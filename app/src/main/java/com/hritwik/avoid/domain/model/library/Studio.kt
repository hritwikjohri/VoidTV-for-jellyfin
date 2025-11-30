package com.hritwik.avoid.domain.model.library

import android.net.Uri
import com.hritwik.avoid.utils.constants.ApiConstants

data class Studio(
    val id: String,
    val name: String,
    val imageTag: String? = null
) {
    fun getThumbUrl(serverUrl: String): String {
        val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        val encodedName = Uri.encode(name)

        val params = buildList {
            imageTag?.takeIf { it.isNotBlank() }?.let { add("tag=$it") }
            add("quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}")
        }

        val query = (params + "maxWidth=${ApiConstants.THUMBNAIL_MAX_WIDTH}")
            .joinToString(separator = "&")

        return "$baseUrl${"Studios/$encodedName/Images/Thumb"}?$query"
    }
}
