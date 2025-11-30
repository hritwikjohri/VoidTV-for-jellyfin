package com.hritwik.avoid.utils.helpers

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

fun getLocalIpAddress(context: Context): String? {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    val linkAddress = connectivityManager?.activeNetwork?.let { network ->
        connectivityManager.getLinkProperties(network)?.linkAddresses?.firstOrNull { address ->
            val inetAddress = address.address
            inetAddress is Inet4Address && !inetAddress.isLoopbackAddress
        }
    }

    if (linkAddress != null) {
        return (linkAddress.address as? Inet4Address)?.hostAddress
    }

    return getFallbackIpAddress()
}

private fun getFallbackIpAddress(): String? {
    return try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.flatMap { networkInterface ->
            networkInterface.inetAddresses.toList()
        }?.firstOrNull { inetAddress ->
            inetAddress is Inet4Address && !inetAddress.isLoopbackAddress
        }?.hostAddress
    } catch (_: SocketException) {
        null
    }
}

private fun <T> java.util.Enumeration<T>.toList(): List<T> {
    val result = mutableListOf<T>()
    while (hasMoreElements()) {
        result.add(nextElement())
    }
    return result
}
