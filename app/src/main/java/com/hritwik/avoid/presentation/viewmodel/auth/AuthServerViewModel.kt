package com.hritwik.avoid.presentation.viewmodel.auth

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hritwik.avoid.R
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.connection.RemoteConfigServer
import com.hritwik.avoid.data.connection.RemoteServerConfigPayload
import com.hritwik.avoid.data.connection.ServerConnectionEvent
import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.network.MtlsCertificateProvider
import com.hritwik.avoid.presentation.ui.state.AuthServerState
import com.hritwik.avoid.presentation.ui.state.InitializationState
import com.hritwik.avoid.presentation.ui.state.PasswordChangeState
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.model.auth.LoginCredentials
import com.hritwik.avoid.domain.model.auth.Server
import com.hritwik.avoid.domain.provider.AuthSessionProvider
import com.hritwik.avoid.domain.usecase.auth.AuthenticateUserUseCase
import com.hritwik.avoid.domain.usecase.auth.AuthenticateWithTokenUseCase
import com.hritwik.avoid.domain.usecase.auth.AuthorizeQuickConnectUseCase
import com.hritwik.avoid.domain.usecase.auth.AuthorizeQuickConnectWithTokenUseCase
import com.hritwik.avoid.domain.usecase.auth.ChangePasswordUseCase
import com.hritwik.avoid.domain.usecase.auth.GetSavedAuthUseCase
import com.hritwik.avoid.domain.usecase.auth.LogoutUseCase
import com.hritwik.avoid.domain.usecase.auth.ClearAuthDataUseCase
import com.hritwik.avoid.domain.usecase.auth.ConnectToServerUseCase
import com.hritwik.avoid.domain.usecase.auth.GetSavedServerUseCase
import com.hritwik.avoid.domain.usecase.auth.InitiateQuickConnectUseCase
import com.hritwik.avoid.domain.usecase.auth.PollQuickConnectUseCase
import com.hritwik.avoid.domain.usecase.auth.SaveServerConfigUseCase
import com.hritwik.avoid.domain.usecase.auth.ValidateSessionUseCase
import com.hritwik.avoid.presentation.ui.state.QuickConnectState
import com.hritwik.avoid.utils.helpers.getLocalIpAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthServerViewModel @Inject constructor(
    private val authenticateUserUseCase: AuthenticateUserUseCase,
    private val getSavedAuthUseCase: GetSavedAuthUseCase,
    private val validateSessionUseCase: ValidateSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val clearAuthDataUseCase: ClearAuthDataUseCase,
    private val authSessionProvider: AuthSessionProvider,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val saveServerConfigUseCase: SaveServerConfigUseCase,
    private val getSavedServerUseCase: GetSavedServerUseCase,
    private val authenticateWithTokenUseCase: AuthenticateWithTokenUseCase,
    private val authorizeQuickConnectUseCase: AuthorizeQuickConnectUseCase,
    private val authorizeQuickConnectWithTokenUseCase: AuthorizeQuickConnectWithTokenUseCase,
    private val initiateQuickConnectUseCase: InitiateQuickConnectUseCase,
    private val pollQuickConnectUseCase: PollQuickConnectUseCase,
    private val serverConnectionManager: ServerConnectionManager,
    private val preferencesManager: PreferencesManager,
    private val mtlsCertificateProvider: MtlsCertificateProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AuthServerState())
    val state: StateFlow<AuthServerState> = _state.asStateFlow()
    val connectionEvents: SharedFlow<ServerConnectionEvent> = serverConnectionManager.events

    private val _passwordChangeState = MutableStateFlow<PasswordChangeState>(PasswordChangeState.Idle)
    val passwordChangeState: StateFlow<PasswordChangeState> = _passwordChangeState.asStateFlow()

    private var quickConnectJob: Job? = null
    private var configServer: RemoteConfigServer? = null
    private var pendingMtlsPassword: String? = null

    init {
        initializeRemoteConfigServer()
        state.onEach { authSessionProvider.updateSession(it.authSession) }.launchIn(viewModelScope)
        viewModelScope.launch {
            serverConnectionManager.state.collect { connectionState ->
                val current = _state.value
                val updatedServer = current.server?.let { server ->
                    server.copy(
                        url = connectionState.activeMethod?.url ?: server.url,
                        connectionMethods = connectionState.methods,
                        activeConnection = connectionState.activeMethod
                    )
                }
                val updatedAuthSession = current.authSession?.let { session ->
                    val serverForSession = updatedServer ?: session.server.copy(
                        url = connectionState.activeMethod?.url ?: session.server.url,
                        connectionMethods = connectionState.methods,
                        activeConnection = connectionState.activeMethod
                    )
                    session.copy(server = serverForSession)
                }
                val updatedState = current.copy(
                    connectionMethods = connectionState.methods,
                    activeConnectionMethod = connectionState.activeMethod,
                    isOfflineMode = connectionState.isOffline,
                    isWifiConnected = connectionState.isWifiConnected,
                    localConnectionUrls = connectionState.methods.filter { it.isLocal }.map { it.url },
                    remoteConnectionUrls = connectionState.methods.filterNot { it.isLocal }.map { it.url },
                    serverUrl = connectionState.activeMethod?.url ?: updatedServer?.url ?: current.serverUrl,
                    server = updatedServer,
                    authSession = updatedAuthSession
                )
                val ipAddress = getLocalIpAddress(context)
                _state.value = if (!ipAddress.isNullOrBlank() && updatedState.localConfigServerIp != ipAddress) {
                    updatedState.copy(localConfigServerIp = ipAddress)
                } else {
                    updatedState
                }
            }
        }
        viewModelScope.launch {
            combine(
                preferencesManager.isMtlsEnabled(),
                preferencesManager.getMtlsCertificateName(),
                preferencesManager.getMtlsCertificatePassword(),
            ) { enabled, certificateName, password ->
                Triple(enabled, certificateName, password)
            }.collect { (enabled, certificateName, password) ->
                val pendingPassword = pendingMtlsPassword
                _state.update { current ->
                    current.copy(
                        isMtlsEnabled = enabled,
                        mtlsCertificateName = certificateName,
                        mtlsCertificatePassword = pendingPassword ?: password.orEmpty()
                    )
                }
            }
        }
        viewModelScope.launch {
            serverConnectionManager.ensureActiveConnection()
        }
        loadSavedServer()
        checkSavedAuth()
    }

    private fun initializeRemoteConfigServer() {
        configServer?.stop()
        configServer = null

        val ipAddress = getLocalIpAddress(context)
        val primaryResult = startRemoteConfigServer(CONFIG_SERVER_PORT)
        if (primaryResult.isSuccess) {
            configServer = primaryResult.getOrThrow()
            _state.update { current ->
                current.copy(
                    localConfigServerIp = ipAddress,
                    localConfigServerPort = CONFIG_SERVER_PORT,
                    configServerWarning = null,
                    configServerError = null
                )
            }
            return
        }

        val primaryError = primaryResult.exceptionOrNull()
        val fallbackResult = startRemoteConfigServer(CONFIG_SERVER_FALLBACK_PORT)
        if (fallbackResult.isSuccess) {
            configServer = fallbackResult.getOrThrow()
            val warningMessageRes = if (isPermissionDenied(primaryError)) {
                R.string.server_setup_port_permission_warning
            } else {
                R.string.server_setup_port_unavailable_warning
            }
            val warningMessage = context.getString(
                warningMessageRes,
                CONFIG_SERVER_PORT,
                CONFIG_SERVER_FALLBACK_PORT
            )
            _state.update { current ->
                current.copy(
                    localConfigServerIp = ipAddress,
                    localConfigServerPort = CONFIG_SERVER_FALLBACK_PORT,
                    configServerWarning = warningMessage,
                    configServerError = null
                )
            }
        } else {
            val failureReason = fallbackResult.exceptionOrNull()?.message?.takeUnless { it.isNullOrBlank() }
                ?: primaryError?.message?.takeUnless { it.isNullOrBlank() }
            val failureMessage = failureReason?.let { reason ->
                context.getString(R.string.server_setup_config_server_failed_with_reason, reason)
            } ?: context.getString(R.string.server_setup_config_server_failed)
            _state.update { current ->
                current.copy(
                    localConfigServerIp = ipAddress,
                    localConfigServerPort = null,
                    configServerWarning = null,
                    configServerError = failureMessage
                )
            }
        }
    }

    private fun startRemoteConfigServer(port: Int): Result<RemoteConfigServer> {
        val server = RemoteConfigServer(port) { payload ->
            handleRemoteConfigPayload(payload)
        }
        val startResult = runCatching { server.start(SERVER_SOCKET_TIMEOUT, false) }
        return if (startResult.isSuccess) {
            Result.success(server)
        } else {
            server.stop()
            Result.failure(startResult.exceptionOrNull() ?: IllegalStateException("Unable to start server"))
        }
    }

    private fun isPermissionDenied(error: Throwable?): Boolean {
        val message = error?.message?.lowercase() ?: return false
        return "permission denied" in message || "eacces" in message
    }

    private fun handleRemoteConfigPayload(payload: RemoteServerConfigPayload) {
        viewModelScope.launch {
            val token = payload.quickConnectToken?.takeIf { it.isNotBlank() }
                ?: payload.mediaToken?.takeIf { it.isNotBlank() }
            val targetUrl = payload.quickConnectUrl?.takeIf { it.isNotBlank() }
                ?: resolvePayloadUrl(payload)

            if (token != null && !targetUrl.isNullOrBlank()) {
                prepareQuickConnectAuthorization(targetUrl, token)
                return@launch
            }

            if (token != null) {
                _state.update { current -> current.copy(receivedMediaToken = token) }
            }

            if (!targetUrl.isNullOrBlank()) {
                _state.update { current -> current.copy(serverUrl = targetUrl) }
                connectToServer(targetUrl)
            }
        }
    }

    private fun prepareQuickConnectAuthorization(serverUrl: String, token: String) {
        quickConnectJob?.cancel()
        quickConnectJob = null
        _state.update { current ->
            current.copy(
                serverUrl = serverUrl,
                quickConnectState = QuickConnectState(
                    authorizationToken = token,
                    authorizationUrl = serverUrl,
                    shouldNavigateToAuthorization = true
                ),
                receivedMediaToken = token
            )
        }
        connectToServer(serverUrl)
    }

    private fun resolvePayloadUrl(payload: RemoteServerConfigPayload): String? {
        payload.fullUrl?.takeIf { it.isNotBlank() }?.let { return it }

        val rawAddress = payload.serverUrl?.takeIf { it.isNotBlank() } ?: return null
        val address = rawAddress.trim()
        val schemeProvided = address.startsWith("http://", ignoreCase = true) ||
            address.startsWith("https://", ignoreCase = true)
        val scheme = when (payload.connectionType?.lowercase()) {
            "https" -> "https"
            else -> "http"
        }

        val portSegment = payload.port?.let { ":$it" } ?: ""
        return if (schemeProvided) {
            if (portSegment.isNotEmpty() && !address.substringAfter("://").contains(":")) {
                "$address$portSegment"
            } else {
                address
            }
        } else {
            "$scheme://$address$portSegment"
        }
    }

    fun saveLocalConnections(urls: List<String>) {
        viewModelScope.launch {
            serverConnectionManager.setLocalAddresses(urls)
        }
    }

    fun clearLocalConnections() {
        viewModelScope.launch {
            serverConnectionManager.setLocalAddresses(emptyList())
        }
    }

    fun saveRemoteConnections(urls: List<String>) {
        viewModelScope.launch {
            serverConnectionManager.setRemoteAddresses(urls)
        }
    }

    suspend fun resetServerConfiguration() {
        serverConnectionManager.clearConnectionState()
        preferencesManager.clearServerConfiguration()
        resetAuthState()
    }

    fun clearRemoteConnections() {
        viewModelScope.launch {
            serverConnectionManager.setRemoteAddresses(emptyList())
        }
    }

    fun setMtlsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setMtlsEnabled(enabled)
            if (!enabled) {
                _state.update { current -> current.copy(mtlsError = null) }
            }
        }
    }

    fun clearMtlsError() {
        _state.update { current -> current.copy(mtlsError = null) }
    }

    fun importMtlsCertificate(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isMtlsImporting = true, mtlsError = null) }
            try {
                val certificateBytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes()
                    } ?: throw IllegalStateException("Unable to open selected certificate")
                }
                if (certificateBytes.isEmpty()) {
                    throw IllegalArgumentException("Selected certificate is empty")
                }
                val displayName = getDisplayName(context, uri) ?: uri.lastPathSegment ?: "mtls_certificate"
                preferencesManager.saveMtlsCertificate(certificateBytes, displayName)
                preferencesManager.setMtlsEnabled(true)
                preferencesManager.setMtlsCertificatePassword("")
                pendingMtlsPassword = null
                _state.update {
                    it.copy(
                        isMtlsImporting = false,
                        isMtlsEnabled = true,
                        mtlsCertificateName = displayName,
                        mtlsCertificatePassword = "",
                        mtlsError = null
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isMtlsImporting = false,
                        mtlsError = error.message ?: "Failed to import certificate"
                    )
                }
            }
        }
    }

    fun updateMtlsCertificatePassword(password: String) {
        pendingMtlsPassword = password
        _state.update { it.copy(mtlsCertificatePassword = password) }
    }

    fun removeMtlsCertificate() {
        viewModelScope.launch {
            _state.update { it.copy(isMtlsImporting = true, mtlsError = null) }
            runCatching {
                preferencesManager.clearMtlsCertificate()
                preferencesManager.setMtlsEnabled(false)
                pendingMtlsPassword = null
            }.onSuccess {
                _state.update {
                    it.copy(
                        isMtlsImporting = false,
                        mtlsCertificateName = null,
                        mtlsCertificatePassword = "",
                        isMtlsEnabled = false
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isMtlsImporting = false,
                        mtlsError = error.message ?: "Failed to remove certificate"
                    )
                }
            }
        }
    }

    fun showMtlsError(message: String) {
        _state.update { it.copy(mtlsError = message) }
    }

    fun retryConnectionEvaluation() {
        viewModelScope.launch {
            serverConnectionManager.refreshActiveConnection(messageOnSwitch = "Rechecked server connection")
        }
    }

    fun connectToServer(serverUrl: String, mediaToken: String? = null) {
        viewModelScope.launch {
            if (_state.value.isMtlsEnabled && _state.value.mtlsCertificateName.isNullOrBlank()) {
                _state.update {
                    it.copy(
                        mtlsError = "Upload an mTLS certificate before connecting.",
                        isLoading = false,
                        isConnected = false
                    )
                }
                return@launch
            }
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                isConnected = false,
                mtlsError = null
            )

            val cleanUrl = cleanServerUrl(serverUrl)
            val useMtls = _state.value.isMtlsEnabled && !_state.value.mtlsCertificateName.isNullOrBlank()
            val mtlsPassword = _state.value.mtlsCertificatePassword

            val result = try {
                if (useMtls) {
                    mtlsCertificateProvider.withTemporaryPassword(mtlsPassword) {
                        connectToServerUseCase(cleanUrl)
                    }
                } else {
                    connectToServerUseCase(cleanUrl)
                }
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isConnected = false,
                    server = null,
                    mtlsError = if (useMtls) "Unable to connect" else _state.value.mtlsError,
                    error = error.message ?: "Unable to connect"
                )
                return@launch
            }

            when (result) {
                is NetworkResult.Success -> {
                    if (useMtls) {
                        preferencesManager.setMtlsCertificatePassword(mtlsPassword)
                        pendingMtlsPassword = null
                    }
                    saveServerConfigUseCase(result.data)
                    val connectionState = serverConnectionManager.state.value
                    val resolvedMethods = connectionState.methods.ifEmpty { result.data.connectionMethods }
                    val activeMethod = connectionState.activeMethod ?: result.data.activeConnection
                    val resolvedServer = result.data.copy(
                        connectionMethods = resolvedMethods,
                        activeConnection = activeMethod,
                        url = activeMethod?.url ?: result.data.url
                    )

                    if (!mediaToken.isNullOrBlank()) {
                        _state.update { current ->
                            current.copy(
                                server = resolvedServer,
                                serverUrl = resolvedServer.url,
                                connectionMethods = resolvedMethods,
                                activeConnectionMethod = activeMethod,
                                isOfflineMode = connectionState.isOffline,
                                isWifiConnected = connectionState.isWifiConnected,
                                localConnectionUrls = resolvedMethods.filter { it.isLocal }.map { it.url },
                                remoteConnectionUrls = resolvedMethods.filterNot { it.isLocal }.map { it.url },
                                isConnected = true,
                                receivedMediaToken = mediaToken,
                                error = null,
                                mtlsError = null
                            )
                        }
                        authenticateWithMediaToken(resolvedServer, mediaToken)
                    } else {
                        _state.update { current ->
                            current.copy(
                                isLoading = false,
                                isConnected = true,
                                server = resolvedServer,
                                serverUrl = resolvedServer.url,
                                connectionMethods = resolvedMethods,
                                activeConnectionMethod = activeMethod,
                                isOfflineMode = connectionState.isOffline,
                                isWifiConnected = connectionState.isWifiConnected,
                                localConnectionUrls = resolvedMethods.filter { it.isLocal }.map { it.url },
                                remoteConnectionUrls = resolvedMethods.filterNot { it.isLocal }.map { it.url },
                                error = null,
                                mtlsError = null
                            )
                        }
                    }
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isConnected = false,
                        server = null,
                        error = result.message,
                        mtlsError = if (useMtls) "Unable to connect" else _state.value.mtlsError
                    )
                }
                else -> Unit
            }
        }
    }

    
    
    
    

    private fun authenticateWithMediaToken(server: Server, mediaToken: String) {
        viewModelScope.launch {
            val params = AuthenticateWithTokenUseCase.Params(server, mediaToken)
            when (val result = authenticateWithTokenUseCase(params)) {
                is NetworkResult.Success -> {
                    val session = result.data
                    val connectionState = serverConnectionManager.state.value
                    val resolvedMethods = connectionState.methods.ifEmpty { session.server.connectionMethods }
                    val activeMethod = connectionState.activeMethod ?: session.server.activeConnection
                    val resolvedServer = session.server.copy(
                        connectionMethods = resolvedMethods,
                        activeConnection = activeMethod,
                        url = activeMethod?.url ?: session.server.url
                    )

                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            isConnected = true,
                            isAuthenticated = true,
                            authSession = session.copy(server = resolvedServer),
                            server = resolvedServer,
                            serverUrl = resolvedServer.url,
                            connectionMethods = resolvedMethods,
                            activeConnectionMethod = activeMethod,
                            isOfflineMode = connectionState.isOffline,
                            isWifiConnected = connectionState.isWifiConnected,
                            localConnectionUrls = resolvedMethods.filter { it.isLocal }.map { it.url },
                            remoteConnectionUrls = resolvedMethods.filterNot { it.isLocal }.map { it.url },
                            receivedMediaToken = mediaToken,
                            error = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            error = result.message
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    fun login(username: String, password: String) {
        val url = _state.value.server?.url ?: _state.value.serverUrl
        if (url.isNullOrBlank()) {
            _state.value = _state.value.copy(error = "Server URL not set")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val credentials = LoginCredentials(username, password)
            val params = AuthenticateUserUseCase.Params(url, credentials)
            when (val result = authenticateUserUseCase(params)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        authSession = result.data,
                        error = null
                    )
                }
                is NetworkResult.Error -> {
                    val errorMessage = if (result.message.contains("timed out", ignoreCase = true)) {
                        LOGIN_TIMEOUT_MESSAGE
                    } else {
                        result.message
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        error = errorMessage
                    )
                }
                else -> Unit
            }
        }
    }

    fun initiateQuickConnect() {
        val quickState = _state.value.quickConnectState
        val url = quickState.authorizationUrl
            ?: _state.value.server?.url
            ?: _state.value.serverUrl
        if (url.isNullOrBlank()) {
            _state.update { current ->
                current.copy(
                    quickConnectState = current.quickConnectState.copy(
                        error = "Server URL not set"
                    )
                )
            }
            return
        }
        if (quickState.authorizationInitiated && quickState.code != null) {
            return
        }
        viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    quickConnectState = current.quickConnectState.copy(
                        isPolling = false,
                        error = null,
                        code = null,
                        secret = null,
                        authorizationInitiated = true,
                        authorizationCompleted = false,
                        authorizationRequestInFlight = false,
                        authorizationAttempted = false
                    )
                )
            }
            when (val result = initiateQuickConnectUseCase(InitiateQuickConnectUseCase.Params(url))) {
                is NetworkResult.Success -> {
                    _state.update { current ->
                        current.copy(
                            quickConnectState = current.quickConnectState.copy(
                                code = result.data.code,
                                secret = result.data.secret,
                                error = null
                            )
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            quickConnectState = current.quickConnectState.copy(
                                error = result.message,
                                authorizationInitiated = false
                            )
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    fun acknowledgeQuickConnectNavigation() {
        _state.update { current ->
            current.copy(
                quickConnectState = current.quickConnectState.copy(
                    shouldNavigateToAuthorization = false
                )
            )
        }
    }

    fun authorizeQuickConnectWithToken(code: String) {
        val quickState = _state.value.quickConnectState
        val token = quickState.authorizationToken ?: return
        val serverUrl = quickState.authorizationUrl
            ?: _state.value.server?.url
            ?: _state.value.serverUrl
            ?: return

        if (quickState.authorizationRequestInFlight || quickState.authorizationCompleted) {
            return
        }

        viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    quickConnectState = current.quickConnectState.copy(
                        authorizationRequestInFlight = true,
                        authorizationAttempted = true,
                        error = null
                    )
                )
            }

            when (val result = authorizeQuickConnectWithTokenUseCase(
                AuthorizeQuickConnectWithTokenUseCase.Params(serverUrl, code, token)
            )) {
                is NetworkResult.Success -> {
                    _state.update { current ->
                        current.copy(
                            quickConnectState = current.quickConnectState.copy(
                                authorizationRequestInFlight = false,
                                authorizationCompleted = true
                            )
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            quickConnectState = current.quickConnectState.copy(
                                authorizationRequestInFlight = false,
                                error = result.message
                            )
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    fun pollQuickConnect(timeoutMillis: Long = 60000L) {
        val secret = _state.value.quickConnectState.secret ?: return
        quickConnectJob?.cancel()
        quickConnectJob = viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    quickConnectState = current.quickConnectState.copy(
                        isPolling = true,
                        error = null
                    )
                )
            }
            val startTime = System.currentTimeMillis()
            val fallbackAuthorizeDelay = 10_000L
            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                when (val stateResult = pollQuickConnectUseCase(PollQuickConnectUseCase.Params(secret))) {
                    is NetworkResult.Success -> {
                        val elapsed = System.currentTimeMillis() - startTime
                        val shouldAttemptAuthorization =
                            stateResult.data.authenticated || elapsed >= fallbackAuthorizeDelay

                        if (shouldAttemptAuthorization) {
                            when (val authResult = authorizeQuickConnectUseCase(AuthorizeQuickConnectUseCase.Params(secret))) {
                                is NetworkResult.Success -> {
                                    _state.update { current ->
                                        current.copy(
                                            isAuthenticated = true,
                                            authSession = authResult.data,
                                            quickConnectState = QuickConnectState()
                                        )
                                    }
                                    return@launch
                                }
                                is NetworkResult.Error -> {
                                    val message = authResult.message
                                    val isPending = message.equals("Quick Connect authorization pending", ignoreCase = true) ||
                                            message.equals("Server not found", ignoreCase = true)

                                    if (!isPending) {
                                        _state.update { current ->
                                            current.copy(
                                                quickConnectState = current.quickConnectState.copy(
                                                    isPolling = false,
                                                    error = message
                                                )
                                            )
                                        }
                                        return@launch
                                    }
                                }
                                else -> Unit
                            }
                        }
                        delay(2000)
                    }
                    is NetworkResult.Error -> {
                        delay(2000)
                    }
                    else -> Unit
                }
            }
            _state.update { current ->
                current.copy(
                    quickConnectState = current.quickConnectState.copy(
                        isPolling = false,
                        error = "Quick Connect timed out",
                    )
                )
            }
        }
    }

    fun resetQuickConnectState() {
        quickConnectJob?.cancel()
        quickConnectJob = null
        _state.value = _state.value.copy(quickConnectState = QuickConnectState())
    }

    fun logout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (logoutUseCase()) {
                is NetworkResult.Success -> {
                    resetAuthState()
                }
                is NetworkResult.Error -> {
                    clearLocalAuthData()
                }
                is NetworkResult.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }

    fun updatePassword(current: String, new: String) {
        viewModelScope.launch {
            val params = ChangePasswordUseCase.Params(current, new)
            _passwordChangeState.value = PasswordChangeState.Loading
            when (val result = changePasswordUseCase(params)) {
                is NetworkResult.Success -> {
                    _passwordChangeState.value = PasswordChangeState.Success
                }
                is NetworkResult.Error -> {
                    _passwordChangeState.value = PasswordChangeState.Error(result.message)
                }
                else -> Unit
            }
        }
    }

    fun switchUser() {
        viewModelScope.launch {
            clearAuthDataUseCase()
            _state.value = _state.value.copy(
                isAuthenticated = false,
                authSession = null
            )
        }
    }

    private suspend fun clearLocalAuthData() {
        clearAuthDataUseCase()
        resetAuthState()
    }

    fun resetAuthState() {
        _state.value = AuthServerState(initializationState = InitializationState.Initialized)
    }

    private fun checkSavedAuth() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = getSavedAuthUseCase()) {
                is NetworkResult.Success -> {
                    val savedSession = result.data
                    if (savedSession != null) {
                        val connectionState = serverConnectionManager.state.value
                        val resolvedMethods = connectionState.methods.ifEmpty { savedSession.server.connectionMethods }
                        val updatedServer = savedSession.server.copy(
                            connectionMethods = resolvedMethods,
                            activeConnection = connectionState.activeMethod ?: savedSession.server.activeConnection,
                            url = connectionState.activeMethod?.url ?: savedSession.server.url
                        )
                        val updatedSession = savedSession.copy(server = updatedServer)

                        val validationResult = validateSessionUseCase()
                        val isSessionValid = validationResult is NetworkResult.Success && validationResult.data
                        val isAuthError = validationResult is NetworkResult.Error && validationResult.error is AppError.Auth
                        val shouldInvalidate =
                            (validationResult is NetworkResult.Success && !validationResult.data) || isAuthError
                        val isOfflineError = connectionState.isOffline ||
                                (validationResult is NetworkResult.Error && !isAuthError)

                        if (shouldInvalidate) {
                            runCatching {
                                preferencesManager.invalidateSession()
                                preferencesManager.clearAuthData()
                            }
                        }

                        val updatedState = when {
                            isSessionValid -> {
                                _state.value.copy(
                                    isLoading = false,
                                    isAuthenticated = true,
                                    authSession = updatedSession,
                                    server = updatedServer,
                                    serverUrl = updatedServer.url,
                                    connectionMethods = resolvedMethods,
                                    activeConnectionMethod =
                                        connectionState.activeMethod ?: savedSession.server.activeConnection,
                                    isOfflineMode = connectionState.isOffline,
                                    isWifiConnected = connectionState.isWifiConnected,
                                    localConnectionUrls = resolvedMethods.filter { it.isLocal }.map { it.url },
                                    remoteConnectionUrls = resolvedMethods.filterNot { it.isLocal }.map { it.url },
                                    error = null
                                )
                            }
                            isOfflineError -> {
                                _state.value.copy(
                                    isLoading = false,
                                    isAuthenticated = true,
                                    authSession = updatedSession,
                                    server = updatedServer,
                                    serverUrl = updatedServer.url,
                                    connectionMethods = resolvedMethods,
                                    activeConnectionMethod =
                                        connectionState.activeMethod ?: savedSession.server.activeConnection,
                                    isOfflineMode = true,
                                    isWifiConnected = connectionState.isWifiConnected,
                                    localConnectionUrls = resolvedMethods.filter { it.isLocal }.map { it.url },
                                    remoteConnectionUrls = resolvedMethods.filterNot { it.isLocal }.map { it.url },
                                    error = (validationResult as? NetworkResult.Error)?.message
                                        ?: connectionState.lastMessage
                                )
                            }
                            else -> {
                                val failureMessage = when (validationResult) {
                                    is NetworkResult.Error -> validationResult.message
                                    is NetworkResult.Success -> "Session expired. Please log in again."
                                    else -> null
                                }
                                _state.value.copy(
                                    isLoading = false,
                                    isAuthenticated = false,
                                    authSession = null,
                                    server = updatedServer,
                                    serverUrl = updatedServer.url,
                                    connectionMethods = resolvedMethods,
                                    activeConnectionMethod =
                                        connectionState.activeMethod ?: savedSession.server.activeConnection,
                                    isOfflineMode = false,
                                    isWifiConnected = connectionState.isWifiConnected,
                                    localConnectionUrls = resolvedMethods.filter { it.isLocal }.map { it.url },
                                    remoteConnectionUrls = resolvedMethods.filterNot { it.isLocal }.map { it.url },
                                    error = failureMessage
                                )
                            }
                        }
                        _state.value = updatedState
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isAuthenticated = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        error = result.message
                    )
                }
                is NetworkResult.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
            _state.value = _state.value.copy(initializationState = InitializationState.Initialized)
        }
    }

    private fun loadSavedServer() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = getSavedServerUseCase()) {
                is NetworkResult.Success -> {
                    if (result.data != null) {
                        val connectionState = serverConnectionManager.state.value
                        val resolvedMethods = connectionState.methods.ifEmpty { result.data.connectionMethods }
                        val updatedServer = result.data.copy(
                            connectionMethods = resolvedMethods,
                            activeConnection = connectionState.activeMethod ?: result.data.activeConnection,
                            url = connectionState.activeMethod?.url ?: result.data.url
                        )
                        _state.value = _state.value.copy(
                            server = updatedServer,
                            serverUrl = updatedServer.url,
                            connectionMethods = resolvedMethods,
                            activeConnectionMethod = connectionState.activeMethod ?: result.data.activeConnection,
                            isOfflineMode = connectionState.isOffline,
                            isWifiConnected = connectionState.isWifiConnected,
                            localConnectionUrls = resolvedMethods.filter { it.isLocal }.map { it.url },
                            remoteConnectionUrls = resolvedMethods.filterNot { it.isLocal }.map { it.url },
                            isConnected = false,
                            isLoading = false
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false)
                    }
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is NetworkResult.Loading -> {
                    
                }
            }
        }
    }

    private fun getDisplayName(context: Context, uri: Uri): String? {
        return context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1 && cursor.moveToFirst()) {
                    cursor.getString(index)
                } else {
                    null
                }
            }
    }

    private fun cleanServerUrl(url: String): String = serverConnectionManager.normalizeUrl(url)

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetConnectionState() {
        _state.value = _state.value.copy(
            isConnected = false,
            isLoading = false,
            error = null,
            mtlsError = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        configServer?.stop()
        configServer = null
    }

    companion object {
        private const val LOGIN_TIMEOUT_MESSAGE = "Login timed out. Please try again."
        private const val CONFIG_SERVER_PORT = 8000
        private const val CONFIG_SERVER_FALLBACK_PORT = 8097
        private const val SERVER_SOCKET_TIMEOUT = 10_000
    }
}
