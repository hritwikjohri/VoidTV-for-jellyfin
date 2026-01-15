package com.hritwik.avoid.presentation.ui.screen.player

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.playback.HdrFormatPreference
import com.hritwik.avoid.domain.model.playback.PlayerType
import com.hritwik.avoid.utils.extensions.findActivity
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel

@SuppressLint("SourceLockedOrientationActivity")
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    mediaItem: MediaItem,
    mediaSourceId: String? = null,
    audioStreamIndex: Int? = null,
    subtitleStreamIndex: Int? = null,
    startPositionMs: Long = 0,
    onBackClick: () -> Unit = {},
    onPlayNextEpisode: (MediaItem) -> Unit = {},
    viewModel: VideoPlaybackViewModel = hiltViewModel(),
    authViewModel: AuthServerViewModel = hiltViewModel(),
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val playerState by viewModel.state.collectAsStateWithLifecycle()
    val playbackSettings by userDataViewModel.playbackSettings.collectAsStateWithLifecycle()
    val playerType = playbackSettings.playerType
    val decoderMode = playbackSettings.decoderMode
    val displayMode = playbackSettings.displayMode
    val audioPassthroughEnabled = playbackSettings.audioPassthroughEnabled
    val hdrFormatPreference = playbackSettings.hdrFormatPreference
    val autoSkipSegments = playbackSettings.autoSkipSegments
    val personalization by userDataViewModel.personalizationSettings.collectAsStateWithLifecycle()
    val gesturesEnabled = personalization.gesturesEnabled
    val userId = authState.authSession?.userId?.id ?: ""
    val accessToken = authState.authSession?.accessToken ?: ""
    val serverUrl = authState.authSession?.server?.url ?: ""
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(activity) {
        val previousOrientation =
            activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = previousOrientation
        }
    }

    LaunchedEffect(mediaItem.id, userId, accessToken, serverUrl) {
        viewModel.initializePlayer(
            mediaItem = mediaItem,
            serverUrl = serverUrl,
            accessToken = accessToken,
            userId = userId,
            mediaSourceId = mediaSourceId,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
            startPositionMs = startPositionMs
        )
    }

    val selectedVideoRangeLabel = playerState.playbackOptions.selectedVideoStream?.videoRangeLabel
    val effectiveVideoRangeLabel =
        if (hdrFormatPreference == HdrFormatPreference.HDR10_PLUS &&
            selectedVideoRangeLabel?.contains("dolby", ignoreCase = true) == true) {
            "HDR10+"
        } else {
            selectedVideoRangeLabel
        }
    val prefersMpv = when (playerType) {
        PlayerType.MPV -> true
        PlayerType.EXOPLAYER -> false
        PlayerType.AUTO -> {
            val videoRange = effectiveVideoRangeLabel ?: "SDR"
            videoRange.equals("SDR", ignoreCase = true)  
        }
    }

    val shouldShowMpv = playerState.isInitialized &&
        prefersMpv &&
        playerState.videoUrl != null

    val hasExoSource = playerState.exoMediaItem != null || playerState.videoUrl != null
    val shouldShowExo = playerState.isInitialized && (!prefersMpv || !shouldShowMpv) && hasExoSource

    when {
        shouldShowMpv -> {
            MpvPlayerView(
                mediaItem = mediaItem,
                playerState = playerState,
                decoderMode = decoderMode,
                displayMode = displayMode,
                userId = userId,
                accessToken = accessToken,
                serverUrl = serverUrl,
                autoSkipSegments = autoSkipSegments,
                gesturesEnabled = gesturesEnabled,
                onBackClick = onBackClick,
                videoPlaybackViewModel = viewModel,
                userDataViewModel = userDataViewModel
            )
        }

        shouldShowExo -> {
            ExoPlayerView(
                mediaItem = mediaItem,
                playerState = playerState,
                displayMode = displayMode,
                userId = userId,
                accessToken = accessToken,
                serverUrl = serverUrl,
                autoSkipSegments = autoSkipSegments,
                audioPassthroughEnabled = audioPassthroughEnabled,
                hdrFormatPreference = hdrFormatPreference,
                gesturesEnabled = gesturesEnabled,
                onBackClick = onBackClick,
                decoderMode = decoderMode,
                viewModel = viewModel,
                userDataViewModel = userDataViewModel
            )
        }

        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Preparing video...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
