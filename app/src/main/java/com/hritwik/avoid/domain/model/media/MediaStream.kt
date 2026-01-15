package com.hritwik.avoid.domain.model.media

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.MediaStreamType

@Serializable
@Parcelize
data class MediaStream(
    val index: Int,
    val type: MediaStreamType,
    val codec: String?,
    val language: String?,
    val displayLanguage: String?,
    val title: String?,
    val displayTitle: String?,
    val isDefault: Boolean,
    val isForced: Boolean,
    val isExternal: Boolean,
    val bitRate: Int?,
    val width: Int?,
    val height: Int?,
    val aspectRatio: String?,
    val frameRate: Float?,
    val channels: Int?,
    val sampleRate: Int?,
    val channelLayout: String?,
    val videoRange: String? = null,
    val videoRangeType: String? = null,
    val videoDoViTitle: String? = null,
    val dvProfile: Int? = null,
    val profile: String? = null,
    val bitDepth: Int? = null
) : Parcelable {
    
    val resolution: String?
        get() = if (width != null && height != null) "${width}x${height}" else null

    val videoQuality: VideoQuality?
        get() = when {
            width != null -> when {
                width >= 3840 -> VideoQuality.UHD_4K
                width >= 2560 -> VideoQuality.QHD_2K
                width >= 1920 -> VideoQuality.FHD_1080P
                width >= 1280 -> VideoQuality.HD_720P
                width >= 854 -> VideoQuality.SD_480P
                width > 0 -> VideoQuality.SD_480P
                else -> VideoQuality.SD_480P
            }
            height != null -> when {
                height >= 2160 -> VideoQuality.UHD_4K
                height >= 1440 -> VideoQuality.QHD_2K
                height >= 1080 -> VideoQuality.FHD_1080P
                height >= 720 -> VideoQuality.HD_720P
                height >= 480 -> VideoQuality.SD_480P
                else -> VideoQuality.SD_480P
            }
            else -> VideoQuality.SD_480P
        }

    val videoRangeLabel: String?
        get() {
            val doViTitle = videoDoViTitle
            if (doViTitle != null && doViTitle.contains("dolby", ignoreCase = true)) {
                return "Dolby Vision"
            }
            fun map(value: String?): String? {
                val cleaned = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
                return when (cleaned.uppercase()) {
                    "SDR" -> "SDR"
                    "HDR" -> "HDR"
                    "HDR10" -> "HDR10"
                    "HDR10PLUS", "HDR10_PLUS" -> "HDR10+"
                    "DOLBYVISION", "DOLBY_VISION", "DOVI" -> "Dolby Vision"
                    "HLG" -> "HLG"
                    else -> cleaned.replace('_', ' ').uppercase()
                }
            }

            return map(videoRangeType) ?: map(videoRange)
        }

    
    val audioDescription: String
        get() = buildString {
            displayTitle?.let { append(it) } ?: run {
                codec?.let { append(it.uppercase()) }
                channels?.let {
                    if (isNotEmpty()) append(" ")
                    append("${it}ch")
                }
                displayLanguage?.let {
                    if (isNotEmpty()) append(" ")
                    append("($it)")
                }
            }
        }.ifEmpty { "Unknown Audio" }

    
    val subtitleDescription: String
        get() = buildString {
            displayTitle?.let { append(it) } ?: run {
                displayLanguage?.let { append(it) }
                if (isForced) {
                    if (isNotEmpty()) append(" ")
                    append("(Forced)")
                }
                if (isExternal) {
                    if (isNotEmpty()) append(" ")
                    append("(External)")
                }
            }
        }.ifEmpty { "Unknown Subtitle" }
}
