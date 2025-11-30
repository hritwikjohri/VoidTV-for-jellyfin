package com.hritwik.avoid.utils.extensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes




fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}


fun Context.showToast(@StringRes messageRes: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageRes, duration).show()
}

fun Context.launchExternalVideoPlayer(url: String, title: String? = null) {
    val uri = Uri.parse(url)
    val playbackIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/*")
        putExtra(Intent.EXTRA_TITLE, title ?: "Video")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooserTitle = title?.takeIf { it.isNotBlank() } ?: "Open with"
    val chooserIntent = Intent.createChooser(playbackIntent, chooserTitle).apply {
        if (this@launchExternalVideoPlayer !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    try {
        startActivity(chooserIntent)
    } catch (error: ActivityNotFoundException) {
        showToast("No compatible video app found")
    } catch (error: Exception) {
        showToast("Unable to launch external player")
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
            as android.net.ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

    return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
}
