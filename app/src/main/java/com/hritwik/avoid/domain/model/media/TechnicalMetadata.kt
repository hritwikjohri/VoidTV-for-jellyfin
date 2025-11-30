package com.hritwik.avoid.domain.model.media

data class TechnicalMetadata(
    val runtime: String?,
    val bitrate: String?,
    val frameRate: String?,
    val audioLayout: String?,
    val container: String?,
    val fileSize: String?
) {
    val displayValues: List<String>
        get() = listOfNotNull(runtime, bitrate, frameRate, audioLayout, container, fileSize)
}

