package com.hritwik.avoid.utils.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) : ConnectivityObserver {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private data class ConnectionStatus(
        val isConnected: Boolean,
        val isOnWifi: Boolean
    )

    private val initialStatus = currentStatus()
    private val _isConnected = MutableStateFlow(initialStatus.isConnected)
    private val _isOnWifi = MutableStateFlow(initialStatus.isOnWifi)

    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    override val isOnWifi: StateFlow<Boolean> = _isOnWifi.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateStatus(network = network)
        }

        override fun onLost(network: Network) {
            updateStatus()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateStatus(capabilities = networkCapabilities)
        }

        override fun onUnavailable() {
            _isConnected.tryEmit(false)
            _isOnWifi.tryEmit(false)
        }
    }

    init {
        registerCallback()
    }

    private fun registerCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(callback)
            } else {
                connectivityManager.registerNetworkCallback(request, callback)
            }
        } catch (securityException: SecurityException) {
            connectivityManager.registerNetworkCallback(request, callback)
        }
        updateStatus()
    }

    private fun updateStatus(
        network: Network? = null,
        capabilities: NetworkCapabilities? = null
    ) {
        val status = when {
            capabilities != null -> capabilities.asStatus()
            network != null -> network.asStatus()
            else -> currentStatus()
        }
        _isConnected.tryEmit(status.isConnected)
        _isOnWifi.tryEmit(status.isConnected && status.isOnWifi)
    }

    private fun currentStatus(): ConnectionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.let { info ->
                return ConnectionStatus(info.isConnected, info.type == ConnectivityManager.TYPE_WIFI)
            }
        }

        connectivityManager.activeNetwork?.let { active ->
            val status = connectivityManager.getNetworkCapabilities(active)?.asStatus()
            if (status != null && status.isConnected) return status
        }

        return ConnectionStatus(false, false)
    }

    private fun Network.asStatus(): ConnectionStatus {
        val capabilities = connectivityManager.getNetworkCapabilities(this)
        return capabilities?.asStatus() ?: ConnectionStatus(false, false)
    }

    private fun NetworkCapabilities.asStatus(): ConnectionStatus {
        val notSuspended = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED) ||
                hasCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND)
        } else {
            true
        }

        val hasUsableTransport = hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val isConnected = hasUsableTransport && notSuspended
        val onWifi = hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        return ConnectionStatus(isConnected, onWifi)
    }
}
