package com.hritwik.avoid.domain.model.library

import android.os.Parcelable
import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.TechnicalMetadata
import com.hritwik.avoid.domain.model.media.VideoQuality
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.extensions.formatBitrate
import com.hritwik.avoid.utils.extensions.formatFileSize
import com.hritwik.avoid.utils.extensions.formatFrameRate
import com.hritwik.avoid.utils.extensions.formatRuntime
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
@Parcelize
data class MediaItem(
    val id: String,
    val name: String,
    val title: String? = null,
    val type: String,
    val overview: String?,
    val year: Int?,
    val communityRating: Double?,
    val runTimeTicks: Long?,
    val primaryImageTag: String?,
    val thumbImageTag: String? = null,
    val logoImageTag: String?,
    val backdropImageTags: List<String>,
    val genres: List<String>,
    val isFolder: Boolean,
    val childCount: Int?,
    val userData: UserData?,
    val taglines: List<String> = emptyList(),
    val people: List<Person> = emptyList(),
    val mediaSources: List<MediaSource> = emptyList(),
    val hasSubtitles: Boolean = false,
    val versionName: String? = null,
    val seriesName: String? = null,
    val seriesId: String? = null,
    val seriesPrimaryImageTag: String? = null,
    val seasonId: String? = null,
    val seasonName: String? = null,
    val seasonPrimaryImageTag: String? = null,
    val parentIndexNumber: Int? = null,
    val indexNumber: Int? = null,
    val tvdbId: String? = null,
    val imageBlurHashes: Map<String, Map<String, String>>? = null
) : Parcelable {
    fun getLogoUrl(serverUrl: String): String? {
        val tag = logoImageTag ?: return null
        val file = java.io.File(tag)
        if (file.isAbsolute) return file.toURI().toString()
        val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        return "${baseUrl}Items/$id/Images/Logo?tag=$tag&quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}&maxWidth=${ApiConstants.POSTER_MAX_WIDTH}"
    }

    fun getBlurHash(imageType: String, imageTag: String?): String? {
        if (imageTag == null) return null
        return imageBlurHashes?.get(imageType)?.get(imageTag)
    }

    fun getPrimaryTagline(): String? {
        return taglines.firstOrNull()  
    }

    
    fun getPrimaryMediaSource(): MediaSource? {
        return mediaSources.firstOrNull()
    }
    
    fun getAvailableVideoQualities(): List<VideoQuality> {
        return mediaSources
            .flatMap { it.availableVideoQualities }
            .distinct()
            .sortedByDescending { it.height }
    }
    
    fun hasPlaybackDataLoaded(): Boolean {
        return mediaSources.any { it.mediaStreams.isNotEmpty() }
    }

    fun getPrimaryTechnicalMetadata(): TechnicalMetadata? {
        val primarySource = getPrimaryMediaSource() ?: return null

        val runtimeTicks = primarySource.runTimeTicks ?: runTimeTicks
        val runtime = runtimeTicks?.takeIf { it > 0 }?.formatRuntime()

        val bitrate = primarySource.bitrate?.takeIf { it > 0 }?.formatBitrate()

        val frameRate = primarySource.defaultVideoStream?.frameRate
            ?.takeIf { it > 0f }
            ?.formatFrameRate()

        val audioLayout = primarySource.defaultAudioStream?.let { audioStream ->
            val layout = audioStream.channelLayout
                ?.takeIf { it.isNotBlank() }
                ?.replace('_', ' ')
                ?.trim()
                ?.uppercase(Locale.ROOT)
            layout ?: audioStream.channels?.takeIf { it > 0 }?.let { "${it}ch" }
        }

        val container = primarySource.container
            ?.takeIf { it.isNotBlank() }
            ?.uppercase(Locale.ROOT)

        val fileSize = primarySource.size?.takeIf { it > 0L }?.formatFileSize()

        if (listOf(runtime, bitrate, frameRate, audioLayout, container, fileSize).all { it == null }) {
            return null
        }

        return TechnicalMetadata(
            runtime = runtime,
            bitrate = bitrate,
            frameRate = frameRate,
            audioLayout = audioLayout,
            container = container,
            fileSize = fileSize
        )
    }
}
