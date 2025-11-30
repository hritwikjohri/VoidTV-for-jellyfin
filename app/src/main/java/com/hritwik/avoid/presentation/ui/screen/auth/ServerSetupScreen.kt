package com.hritwik.avoid.presentation.ui.screen.auth

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.common.SubtleShinySignature
import com.hritwik.avoid.presentation.ui.components.visual.AmbientBackground
import com.hritwik.avoid.presentation.ui.theme.TextFieldShape
import com.hritwik.avoid.presentation.ui.theme.focusAwareButtonBorder
import com.hritwik.avoid.presentation.ui.theme.voidTextFieldColors
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSetupScreen(
    onServerConnected: () -> Unit,
    onQuickConnectAuthorization: () -> Unit,
    viewModel: AuthServerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val initialUrl = state.serverUrl ?: ""
    var serverUrl by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialUrl,
                selection = TextRange(0, initialUrl.length)
            )
        )
    }
    val textFieldFocusRequester = remember { FocusRequester() }
    val buttonFocusRequester = remember { FocusRequester() }
    var textFieldFocused by remember { mutableStateOf(false) }
    val connectButtonInteractionSource = remember { MutableInteractionSource() }
    val connectButtonFocused by connectButtonInteractionSource.collectIsFocusedAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var mtlsPasswordVisible by remember { mutableStateOf(false) }
    val handleCertificateSelection: (Uri?) -> Unit = remember(context, viewModel) {
        { uri ->
            uri?.let {
                viewModel.clearMtlsError()
                viewModel.importMtlsCertificate(context, it)
                viewModel.setMtlsEnabled(true)
            }
        }
    }
    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> handleCertificateSelection(uri) }
    val legacyContentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> handleCertificateSelection(uri) }
    val hasDocumentPicker = remember(context) {
        isIntentAvailable(context, createDocumentIntent())
    }
    val hasLegacyPicker = remember(context) {
        isIntentAvailable(context, createGetContentIntent())
    }
    val canConnect = !state.isLoading &&
        serverUrl.text.isNotBlank() &&
        !state.isMtlsImporting &&
        (!state.isMtlsEnabled || state.mtlsCertificateName != null)
    val certificateLabel = state.mtlsCertificateName?.let { name ->
        "Selected certificate: $name"
    } ?: if (state.isMtlsEnabled) {
        "mTLS enabled: upload certificate to continue"
    } else {
        "mTLS disabled"
    }
    val density = LocalDensity.current
    val qrSize = calculateRoundedValue(200).sdp
    val displayAddress = remember(state.localConfigServerIp, state.localConfigServerPort) {
        val ip = state.localConfigServerIp
        val port = state.localConfigServerPort
        if (!ip.isNullOrBlank() && port != null) {
            "$ip:$port"
        } else {
            null
        }
    }
    val qrContent = remember(displayAddress) {
        displayAddress?.let { "http://$it" }
    }
    val qrBitmap = remember(qrContent, density) {
        qrContent?.let { content ->
            val sizePx = with(density) { qrSize.roundToPx() }.coerceAtLeast(1)
            generateQrCodeBitmap(content, sizePx)
        }
    }
    val formScrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (serverUrl.text.isBlank()) {
            textFieldFocusRequester.requestFocus()
        } else {
            buttonFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(state.serverUrl) {
        val url = state.serverUrl.orEmpty()
        if (url.isNotBlank() && url != serverUrl.text) {
            serverUrl = TextFieldValue(
                text = url,
                selection = TextRange(url.length)
            )
            buttonFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(state.mtlsCertificateName) {
        mtlsPasswordVisible = false
    }

    LaunchedEffect(state.quickConnectState.shouldNavigateToAuthorization) {
        if (state.quickConnectState.shouldNavigateToAuthorization) {
            onQuickConnectAuthorization()
            viewModel.acknowledgeQuickConnectNavigation()
        }
    }

    LaunchedEffect(textFieldFocused) {
        if (textFieldFocused) {
            formScrollState.animateScrollTo(formScrollState.maxValue)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.resetConnectionState()
    }

    LaunchedEffect(state.isConnected, state.server) {
        if (state.isConnected && state.server != null) {
            onServerConnected()
        }
    }

    AmbientBackground(
        drawableRes = R.drawable.jellyfin_logo,
        modifier = Modifier.fillMaxSize()
    ) {
        Row (
            modifier = Modifier
                .fillMaxSize()
                .padding(calculateRoundedValue(16).sdp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ){
            Box (
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ){
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                ) {
                    AsyncImage(
                        model = R.drawable.void_icon,
                        contentDescription = null,
                        modifier = Modifier.size(calculateRoundedValue(200).sdp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = stringResource(R.string.connect_to_jellyfin),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.enter_your_jellyfin_server_url),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
                SubtleShinySignature(
                    modifier = Modifier.padding(bottom = calculateRoundedValue(10).sdp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val gapXSmall = calculateRoundedValue(4).sdp
                val gapMedium = calculateRoundedValue(12).sdp
                val cardPadding = calculateRoundedValue(8).sdp
                val cardElevation = calculateRoundedValue(2).sdp

                val pendingText = stringResource(R.string.server_setup_qr_pending)
                val unavailableText = stringResource(R.string.server_setup_qr_unavailable)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(formScrollState)
                        .imePadding()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(gapXSmall)
                ) {
                    Card(
                        modifier = Modifier
                            .size(qrSize)
                            .align(Alignment.CenterHorizontally),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
                    ) {
                        val bitmap = qrBitmap
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.server_setup_qr_code_description),
                                modifier = Modifier
                                    .padding(cardPadding)
                                    .fillMaxSize()
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (state.configServerError != null) unavailableText else pendingText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(gapMedium)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(gapMedium))

                    Text(
                        text = stringResource(R.string.connect_to_jellyfin),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    if (displayAddress != null) {
                        Text(
                            text = stringResource(R.string.server_setup_post_instruction, qrContent ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    } else if (state.configServerError == null) {
                        Text(
                            text = stringResource(R.string.server_setup_qr_pending),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                    state.configServerWarning?.let { warningMessage ->
                        Text(
                            text = warningMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFF59D),
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = stringResource(R.string.server_setup_qr_instructions),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    state.receivedMediaToken?.let {
                        Text(
                            text = stringResource(R.string.server_setup_media_token_received),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF81C784),
                            textAlign = TextAlign.Center
                        )
                    }

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = {
                            serverUrl = it
                            if (state.error != null) viewModel.clearError()
                        },
                        label = { Text("Server URL") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val url = serverUrl.text
                                if (url.isNotBlank()) {
                                    viewModel.connectToServer(url)
                                    buttonFocusRequester.requestFocus()
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .widthIn(max = 560.dp)
                            .focusRequester(textFieldFocusRequester)
                            .focusProperties { next = buttonFocusRequester }
                            .onFocusChanged { focusState ->
                                textFieldFocused = focusState.hasFocus
                                if (focusState.hasFocus) {
                                    serverUrl = serverUrl.copy(selection = TextRange(0, serverUrl.text.length))
                                }
                            },
                        enabled = !state.isLoading,
                        singleLine = true,
                        shape = TextFieldShape,
                        colors = voidTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(calculateRoundedValue(12).sdp))

                    Text(
                        text = certificateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.mtlsCertificateName != null) Color.White else Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .widthIn(max = 560.dp)
                    )

                    if (state.isMtlsEnabled && state.mtlsCertificateName != null) {
                        Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))
                        OutlinedTextField(
                            value = state.mtlsCertificatePassword,
                            onValueChange = { viewModel.updateMtlsCertificatePassword(it) },
                            label = { Text("Certificate password (optional)") },
                            visualTransformation = if (mtlsPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { keyboardController?.hide() }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { mtlsPasswordVisible = !mtlsPasswordVisible }) {
                                    Icon(
                                        imageVector = if (mtlsPasswordVisible) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },
                                        contentDescription = if (mtlsPasswordVisible) {
                                            "Hide certificate password"
                                        } else {
                                            "Show certificate password"
                                        },
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            },
                            shape = TextFieldShape,
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .widthIn(max = 560.dp),
                            enabled = !state.isMtlsImporting && !state.isLoading,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

                    val hasCertificate = state.mtlsCertificateName != null
                    val handleCertificateClick = {
                        viewModel.clearMtlsError()
                        if (hasCertificate) {
                            viewModel.removeMtlsCertificate()
                        } else if (hasDocumentPicker) {
                            openDocumentLauncher.launch(
                                arrayOf(
                                    "application/x-pkcs12",
                                    "application/x-pem-file",
                                    "application/x-pkcs7-certificates",
                                    "*/*"
                                )
                            )
                        } else if (hasLegacyPicker) {
                            legacyContentLauncher.launch("*/*")
                        } else {
                            viewModel.showMtlsError("No document picker available on this device.")
                        }
                    }
                    OutlinedButton(
                        onClick = handleCertificateClick,
                        enabled = !state.isMtlsImporting && !state.isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .pressToClick { handleCertificateClick() }
                            .focusable()
                    ) {
                        if (state.isMtlsImporting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(calculateRoundedValue(18).sdp),
                                    strokeWidth = calculateRoundedValue(2).sdp
                                )
                                Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
                                Text(if (hasCertificate) "Removing…" else "Importing…")
                            }
                        } else {
                            Text(if (hasCertificate) "Remove certificate" else "Upload certificate")
                        }
                    }

                    state.mtlsError?.let { error ->
                        Spacer(modifier = Modifier.height(calculateRoundedValue(12).sdp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .widthIn(max = 560.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(calculateRoundedValue(16).sdp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(calculateRoundedValue(4).sdp)
        ){
            val handleConnect = {
                if (canConnect) {
                    viewModel.clearMtlsError()
                    viewModel.connectToServer(serverUrl.text)
                }
            }
            Button(
                onClick = { handleConnect() },
                enabled = canConnect,
                shape = ButtonDefaults.shape,
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.BottomEnd)
                    .focusRequester(buttonFocusRequester)
                    .pressToClick { handleConnect() }
                    .focusProperties {
                        previous = textFieldFocusRequester
                        up = textFieldFocusRequester
                    }
                    .focusable(),
                interactionSource = connectButtonInteractionSource,
                border = focusAwareButtonBorder(
                    isFocused = connectButtonFocused,
                    defaultBorder = null
                )
            ) {
                if (state.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(calculateRoundedValue(20).sdp),
                            strokeWidth = calculateRoundedValue(2).sdp
                        )
                        Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
                        Text("Connecting...")
                    }
                } else {
                    Text("Connect")
                }
            }

            state.error?.let { error ->
                Card(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(calculateRoundedValue(16).sdp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun generateQrCodeBitmap(content: String, size: Int): Bitmap? {
    if (content.isBlank() || size <= 0) return null

    return runCatching {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
        }
        createBitmap(width, height).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }.getOrNull()
}

private fun createDocumentIntent(): Intent =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
    }

private fun createGetContentIntent(): Intent =
    Intent(Intent.ACTION_GET_CONTENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
    }

private fun Modifier.pressToClick(onClick: () -> Unit): Modifier =
    onPreviewKeyEvent { e ->
        if (e.type == KeyEventType.KeyDown &&
            (e.key == Key.Enter ||
                e.key == Key.NumPadEnter ||
                e.key == Key.DirectionCenter ||
                e.key == Key.Spacebar)
        ) {
            onClick()
            true
        } else {
            false
        }
    }

private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
    val resolveInfo = context.packageManager?.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return !resolveInfo.isNullOrEmpty()
}
