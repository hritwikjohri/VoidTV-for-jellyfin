package com.hritwik.avoid.utils.extensions

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.utils.constants.ApiConstants

fun MediaItem.getBackdropUrl(
    serverUrl: String,
    quality: Int = ApiConstants.DEFAULT_IMAGE_QUALITY,
    maxWidth: Int = ApiConstants.BACKDROP_MAX_WIDTH
): String {
    val imageTag = backdropImageTags.firstOrNull()
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$id/Images/Backdrop?tag=$imageTag&quality=$quality&maxWidth=$maxWidth"
}

fun MediaItem.getPosterUrl(
    serverUrl: String,
    quality: Int = ApiConstants.DEFAULT_IMAGE_QUALITY,
    maxWidth: Int = ApiConstants.POSTER_MAX_WIDTH
): String {
    val imageTag = primaryImageTag
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$id/Images/Primary?tag=$imageTag&quality=$quality&maxWidth=$maxWidth"
}

fun MediaItem.getImageUrl(
    serverUrl: String,
    imageType: String,
    imageTag: String?,
    quality: Int = ApiConstants.DEFAULT_IMAGE_QUALITY,
    maxWidth: Int = ApiConstants.BACKDROP_MAX_WIDTH
): String? {
    if (imageTag == null) return null
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$id/Images/$imageType?tag=$imageTag&quality=$quality&maxWidth=$maxWidth"
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.homeContentFocusProperties(
    sideNavigationFocusRequester: FocusRequester?,
    initialFocusRequester: FocusRequester? = null,
    block: FocusProperties.() -> Unit = {}
): Modifier = focusProperties {
    if (sideNavigationFocusRequester != null) {
        left = sideNavigationFocusRequester
    }
    if (initialFocusRequester != null) {
        onEnter = { initialFocusRequester }
    }
    block()
}

fun Modifier.resetFeatureOnFocusExit(reset: () -> Unit) =
    onFocusChanged { state ->
        if (!state.hasFocus) reset()
    }
