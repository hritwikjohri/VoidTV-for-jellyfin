package com.hritwik.avoid.presentation.ui.state

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.media.PlaybackOptions
import com.hritwik.avoid.domain.model.playback.Segment
import com.hritwik.avoid.domain.model.media.VideoQuality
import com.hritwik.avoid.domain.model.playback.PlaybackTranscodeOption
import java.util.Locale
import kotlin.text.uppercase

@OptIn(UnstableApi::class)
data class VideoPlaybackState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val mediaItem: MediaItem? = null,
    val siblingEpisodes: List<MediaItem> = emptyList(),
    val currentEpisodeIndex: Int = -1,
    val playbackOptions: PlaybackOptions = PlaybackOptions(),
    val showVersionDialog: Boolean = false,
    val showVideoQualityDialog: Boolean = false,
    val showAudioDialog: Boolean = false,
    val showSubtitleDialog: Boolean = false,
    val availableVersions: List<MediaSource> = emptyList(),
    val availableVideoQualities: List<VideoQuality> = emptyList(),
    val availableAudioStreams: List<MediaStream> = emptyList(),
    val availableSubtitleStreams: List<MediaStream> = emptyList(),
    val preferredAudioLanguage: String? = null,
    val preferredSubtitleLanguage: String? = null,
    val preferredVideoQuality: VideoQuality? = null,
    val playbackTranscodeOption: PlaybackTranscodeOption = PlaybackTranscodeOption.ORIGINAL,
    val transcodingSessionId: String? = null,
    val videoUrl: String? = null,
    val exoMediaItem: ExoMediaItem? = null,
    val cacheDataSourceFactory: CacheDataSource.Factory? = null,
    val mediaSourceId: String? = null,
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null,
    val subtitleOffIndex: Int? = null,
    val subtitleOffsetMs: Long = 0,
    val startPositionMs: Long = 0,
    val startPositionUpdateCount: Long = 0,
    val segments: List<Segment> = emptyList(),
    val activeSegment: Segment? = null,
    val isPaused: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val volume: Long = 100,
    val lastSavedPosition: Long = 0,
    val isInitialized: Boolean = false,
    val isMarkedAsWatched: Boolean = false,
    val playbackStartReported: Boolean = false,
    val playbackStopReported: Boolean = false,
    val isTranscoding: Boolean = false,
    val hasEnded: Boolean = false,
    val showEndScreen: Boolean = false,
    val onDeckItems: List<MediaItem> = emptyList(),
    val preferHdrOverDolbyVision: Boolean = false,
) {
    val hasMultipleVersions: Boolean
        get() = availableVersions.size > 1

    val hasMultipleAudioTracks: Boolean
        get() = availableAudioStreams.size > 1

    val hasSubtitles: Boolean
        get() = availableSubtitleStreams.isNotEmpty()

    val canResume: Boolean
        get() = playbackOptions.shouldShowResume

    val currentVersionText: String
        get() = playbackOptions.selectedMediaSource?.let { source ->
            buildString {
                append(source.versionInfo)
                source.bitrate?.let {
                    if (isNotEmpty()) append(" • ")
                    val mbps = it / 1_000_000.0
                    val formattedMbps = if (mbps >= 10) {
                        String.format(Locale.US, "%.0f Mbps", mbps)
                    } else {
                        String.format(Locale.US, "%.1f Mbps", mbps)
                    }
                    append(formattedMbps)
                }
            }
        } ?: "Auto"

    val currentVideoQualityText: String
        get() = playbackTranscodeOption.label

    val currentVideoRangeText: String
        get() {
            val label = playbackOptions.selectedVideoStream?.videoRangeLabel ?: "SDR"
            return if (preferHdrOverDolbyVision && label.contains("dolby", ignoreCase = true)) {
                "HDR10"
            } else {
                label
            }
        }

    val currentAudioText: String
        get() = playbackOptions.selectedAudioStream?.let { stream ->
            val language = stream.displayLanguage?.uppercase() ?: stream.language?.uppercase()
            val codec = stream.codec?.uppercase()
            val channel = stream.channelLayout?.uppercase()
            when {
                language != null && codec != null && channel != null -> "$language • $codec • $channel"
                language != null -> language
                codec != null -> codec
                channel != null -> channel
                else -> "Unknown"
            }
        } ?: "Default"

    val currentSubtitleText: String
        get() = playbackOptions.selectedSubtitleStream?.let { stream ->
            val language = stream.displayLanguage?.uppercase() ?: stream.language?.uppercase() ?: "Unknown"
            val codec = stream.codec?.uppercase()
            listOfNotNull(language, codec).joinToString(" • ")
        } ?: "Off"

    val hasPreviousEpisode: Boolean
        get() = siblingEpisodes.isNotEmpty() && currentEpisodeIndex > 0

    val hasNextEpisode: Boolean
        get() = siblingEpisodes.isNotEmpty() && currentEpisodeIndex >= 0 && currentEpisodeIndex < siblingEpisodes.lastIndex

    val previousEpisode: MediaItem?
        get() = if (hasPreviousEpisode) siblingEpisodes[currentEpisodeIndex - 1] else null

    val nextEpisode: MediaItem?
        get() = if (hasNextEpisode) siblingEpisodes[currentEpisodeIndex + 1] else null
}
