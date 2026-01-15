package com.hritwik.avoid.presentation.ui.screen.player

import android.content.Context
import android.graphics.Typeface
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Log
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ffmpeg.FilteringExtractorsFactory
import androidx.media3.exoplayer.text.SubtitleDecoderFactory
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ffmpeg.FfmpegFilteringMode
import androidx.media3.extractor.text.SubtitleDecoder
import androidx.media3.extractor.text.SubtitleInputBuffer
import androidx.media3.extractor.text.SubtitleOutputBuffer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.playback.DecoderMode
import com.hritwik.avoid.domain.model.playback.DisplayMode
import com.hritwik.avoid.domain.model.playback.HdrFormatPreference
import com.hritwik.avoid.presentation.ui.components.common.rememberAudioFocusRequest
import com.hritwik.avoid.presentation.ui.components.dialogs.AudioTrackDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.DecoderSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.DisplayModeSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.SubtitleDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.SubtitleMenuDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.SubtitleOptionsDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.SubtitleSizeDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.VideoQualityDialog
import com.hritwik.avoid.utils.constants.PreferenceConstants
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.presentation.ui.state.TrackChangeEvent
import com.hritwik.avoid.presentation.ui.state.VideoPlaybackState
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.constants.PreferenceConstants.SKIP_PROMPT_FLOATING_DURATION_MS
import com.hritwik.avoid.utils.extensions.getSubtitleUrl
import com.hritwik.avoid.utils.extensions.showToast
import com.hritwik.avoid.utils.extensions.toSubtitleFileExtension
import com.hritwik.avoid.utils.extensions.findActivity
import com.hritwik.avoid.utils.helpers.CodecDetector
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import com.hritwik.avoid.utils.helpers.FrameRateHelper
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.delay
import java.io.File
import java.util.ArrayList
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.media3.common.MediaItem as ExoMediaItem

@OptIn(UnstableApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun ExoPlayerView(
    mediaItem: MediaItem,
    playerState: VideoPlaybackState,
    decoderMode: DecoderMode,
    displayMode: DisplayMode,
    userId: String,
    accessToken: String,
    serverUrl: String,
    autoSkipSegments: Boolean,
    audioPassthroughEnabled: Boolean,
    hdrFormatPreference: HdrFormatPreference,
    gesturesEnabled: Boolean,
    onBackClick: () -> Unit,
    viewModel: VideoPlaybackViewModel,
    userDataViewModel: UserDataViewModel
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val currentMediaItem = playerState.mediaItem ?: mediaItem
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val audioFocusRequest = rememberAudioFocusRequest(audioManager)
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var resumeOnStart by remember { mutableStateOf(false) }
    var playbackDuration by remember { mutableLongStateOf(1L) }
    var playbackProgress by remember { mutableLongStateOf(0L) }
    var volume by remember { mutableLongStateOf(playerState.volume) }
    var isBuffering by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var exoPlayerRef by remember { mutableStateOf<ExoPlayer?>(null) }
    var lastAppliedFrameRate by remember { mutableStateOf<Float?>(null) }
    val contentFrameRate = playerState.playbackOptions.selectedVideoStream?.frameRate
    val window = activity?.window
    var brightness by remember {
        mutableFloatStateOf(window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f)
    }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var showSubtitleOffsetDialog by remember { mutableStateOf(false) }
    var showSubtitleSizeDialog by remember { mutableStateOf(false) }
    val subtitleSize by userDataViewModel.subtitleSize.collectAsStateWithLifecycle()
    val audioStreams = playerState.availableAudioStreams
    val subtitleStreams = playerState.availableSubtitleStreams
    val currentAudioTrack = playerState.audioStreamIndex
    val currentSubtitleTrack = playerState.subtitleStreamIndex
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showDisplayDialog by remember { mutableStateOf(false) }
    var pendingTrackChangeEvent by remember { mutableStateOf<TrackChangeEvent?>(null) }
    var restorePositionMs by remember(currentMediaItem.id) {
        mutableLongStateOf(playerState.startPositionMs)
    }

    var skipLabel by remember { mutableStateOf("") }
    var currentSkipSegmentId by remember { mutableStateOf<String?>(null) }
    var skipSegmentEndMs by remember { mutableLongStateOf(0L) }
    var showOverlaySkipButton by remember { mutableStateOf(false) }
    var showFloatingSkipButton by remember { mutableStateOf(false) }
    var skipPromptDeadlineMs by remember { mutableLongStateOf(0L) }
    var autoSkipProgress by remember { mutableFloatStateOf(0f) }
    var isAutoSkipActive by remember { mutableStateOf(false) }
    var autoSkipCancelled by remember { mutableStateOf(false) }

    fun adjustBrightness(delta: Float) {
        val newBrightness = (brightness + delta).coerceIn(0f, 1f)
        brightness = newBrightness
        window?.let { w ->
            val lp = w.attributes
            lp.screenBrightness = newBrightness
            w.attributes = lp
        }
        gestureFeedback = GestureFeedback.Brightness((newBrightness * 100f).roundToInt())
    }

    fun adjustVolume(stepDelta: Int) {
        val maxVolumeSteps = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val currentSteps = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val targetSteps = (currentSteps + stepDelta).coerceIn(0, maxVolumeSteps)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetSteps, 0)
        val percent = targetSteps / maxVolumeSteps.toFloat() * 100f
        volume = percent.roundToLong()
        exoPlayerRef?.volume = (targetSteps / maxVolumeSteps.toFloat()).coerceIn(0f, 1f)
        viewModel.updateVolume(volume)
        gestureFeedback = GestureFeedback.Volume(volume.toInt())
    }

    fun cancelAutoSkip() {
        if (isAutoSkipActive) {
            isAutoSkipActive = false
            autoSkipProgress = 0f
            autoSkipCancelled = true
        }
    }

    DisposableEffect(audioManager) {
        audioManager.requestAudioFocus(audioFocusRequest)
        onDispose { audioManager.abandonAudioFocusRequest(audioFocusRequest) }
    }

    LaunchedEffect(audioStreams.size) {
        if (audioStreams.isEmpty()) {
            context.showToast("No audio tracks available")
        }
    }

    LaunchedEffect(subtitleStreams.size) {
        if (subtitleStreams.isEmpty()) {
            context.showToast("No subtitles available")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.trackChangeEvents.collect { event ->
            pendingTrackChangeEvent = event
        }
    }

    LaunchedEffect(playerState.activeSegment, autoSkipSegments, autoSkipCancelled) {
        val segment = playerState.activeSegment
        if (segment != null && (
                    segment.type.equals("Intro", true) ||
                            segment.type.equals("Outro", true) ||
                            segment.type.equals("Credits", true)
                    )
        ) {
            skipLabel = when {
                segment.type.equals("Intro", true) -> "Skip Intro"
                segment.type.equals("Outro", true) -> "Skip Outro"
                else -> "Skip Credits"
            }
            if (autoSkipSegments && !autoSkipCancelled) {
                
                val startMs = segment.startPositionTicks / 10_000
                val endMs = segment.endPositionTicks / 10_000
                currentSkipSegmentId = segment.id
                skipSegmentEndMs = endMs
                skipPromptDeadlineMs = 0L
                showFloatingSkipButton = true
                showOverlaySkipButton = true
                showControls = false
                isAutoSkipActive = true
                autoSkipProgress = 0f
            } else {
                
                val startMs = segment.startPositionTicks / 10_000
                val endMs = segment.endPositionTicks / 10_000
                currentSkipSegmentId = segment.id
                skipSegmentEndMs = endMs
                skipPromptDeadlineMs = 0L
                showFloatingSkipButton = true
                showOverlaySkipButton = true
                showControls = false
                isAutoSkipActive = false
                autoSkipProgress = 0f
            }
        } else {
            skipLabel = ""
            currentSkipSegmentId = null
            skipSegmentEndMs = 0L
            skipPromptDeadlineMs = 0L
            showOverlaySkipButton = false
            showFloatingSkipButton = false
            isAutoSkipActive = false
            autoSkipProgress = 0f
            autoSkipCancelled = false
        }
    }

    
    LaunchedEffect(isAutoSkipActive, currentSkipSegmentId) {
        if (isAutoSkipActive && currentSkipSegmentId != null) {
            val autoSkipDurationMs = 4000L
            val updateIntervalMs = 50L
            val steps = autoSkipDurationMs / updateIntervalMs
            var currentStep = 0

            while (isAutoSkipActive && currentStep < steps) {
                delay(updateIntervalMs)
                currentStep++
                autoSkipProgress = (currentStep.toFloat() / steps.toFloat()).coerceIn(0f, 1f)
            }

            
            if (isAutoSkipActive && autoSkipProgress >= 1f) {
                viewModel.skipSegment()
                currentSkipSegmentId = null
                skipSegmentEndMs = 0L
                skipPromptDeadlineMs = 0L
                showOverlaySkipButton = false
                showFloatingSkipButton = false
                isAutoSkipActive = false
                autoSkipProgress = 0f
            }
        }
    }
    LaunchedEffect(
        playbackProgress,
        currentSkipSegmentId,
        skipSegmentEndMs,
        playerState.activeSegment
    ) {
        val segmentId = currentSkipSegmentId ?: return@LaunchedEffect
        val currentMs = playbackProgress * 1000

        val endMs = skipSegmentEndMs
        val activeId = playerState.activeSegment?.id
        if ((endMs > 0 && currentMs >= endMs) || activeId != segmentId) {
            currentSkipSegmentId = null
            skipSegmentEndMs = 0L
            skipPromptDeadlineMs = 0L
            showOverlaySkipButton = false
            showFloatingSkipButton = false
        }
    }

    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(1000)
            gestureFeedback = null
        }
    }

    LaunchedEffect(playbackProgress) {
        restorePositionMs = playbackProgress * 1000
    }


    val windowInsetsController = remember(activity) {
        activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }

    LaunchedEffect(isPlaying, exoPlayerRef) {
        if (isPlaying) {
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
            val focusResult = audioManager.requestAudioFocus(audioFocusRequest)
            if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                exoPlayerRef?.pause()
                isPlaying = false
            }
        } else {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose { windowInsetsController?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    LaunchedEffect(isPlaying, userId, accessToken, currentMediaItem.id) {
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying) {
            if (!isSeeking) {
                viewModel.savePlaybackPosition(
                    mediaId = currentMediaItem.id,
                    userId = userId,
                    accessToken = accessToken,
                    positionSeconds = playbackProgress
                )
            }
            delay(1000L)
        }
    }

    BackHandler {
        viewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
        viewModel.reportPlaybackStop(
            mediaId = currentMediaItem.id,
            userId = userId,
            accessToken = accessToken,
            positionSeconds = playbackProgress,
            isCompleted = playbackProgress >= playbackDuration - 5
        )
        onBackClick()
    }

    DisposableEffect(Unit) {
        viewModel.reportPlaybackStart(currentMediaItem.id, userId, accessToken)
        onDispose {
            viewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
            viewModel.reportPlaybackStop(
                mediaId = currentMediaItem.id,
                userId = userId,
                accessToken = accessToken,
                positionSeconds = playbackProgress,
                isCompleted = playbackProgress >= playbackDuration - 5
            )
        }
    }

    DisposableEffect(lifecycleOwner, activity, exoPlayerRef) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && activity?.isInPictureInPictureMode == true) {
                showControls = false
            }
            if (event == Lifecycle.Event.ON_RESUME && activity?.isInPictureInPictureMode == false) {
                showControls = true
            }
            if ((event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) && activity?.isInPictureInPictureMode == false) {
                resumeOnStart = isPlaying
                if (resumeOnStart) {
                    exoPlayerRef?.pause()
                    isPlaying = false
                }
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
            }
            if ((event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) && activity?.isInPictureInPictureMode == false) {
                if (resumeOnStart) {
                    val focusResult = audioManager.requestAudioFocus(audioFocusRequest)
                    if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        exoPlayerRef?.play()
                        isPlaying = true
                    }
                    resumeOnStart = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val isLocalFile = remember(playerState.videoUrl) {
            playerState.videoUrl?.let {
                val uri = it.toUri()
                uri.scheme == "file" || File(uri.path ?: "").exists()
            } ?: false
        }
        val baseItem = remember(playerState.videoUrl, playerState.exoMediaItem) {
            playerState.exoMediaItem ?: playerState.videoUrl?.let { ExoMediaItem.fromUri(it) }
        }

        val selectedSubtitleStream = remember(playerState.subtitleStreamIndex, subtitleStreams) {
            subtitleStreams.firstOrNull { it.index == playerState.subtitleStreamIndex }
        }

        val effectiveHdrPreference = remember(
            hdrFormatPreference,
            playerState.playbackOptions.selectedVideoStream
        ) {
            resolveHdrPreferenceForStream(
                preference = hdrFormatPreference,
                videoStream = playerState.playbackOptions.selectedVideoStream
            )
        }

        val mediaItemWithSubtitles = remember(baseItem, selectedSubtitleStream, isLocalFile) {
            baseItem?.let { mediaItemBase ->
                when {
                    selectedSubtitleStream == null && !isLocalFile -> {
                        mediaItemBase.buildUpon()
                            .setSubtitleConfigurations(emptyList())
                            .build()
                    }

                    selectedSubtitleStream?.isExternal == true -> {
                        val config = buildSubtitleConfiguration(
                            context = context,
                            mediaItem = currentMediaItem,
                            stream = selectedSubtitleStream,
                            isLocalFile = isLocalFile,
                            serverUrl = serverUrl,
                            accessToken = accessToken
                        )
                        mediaItemBase.buildUpon()
                            .setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
                            .build()
                    }

                    else -> mediaItemBase
                }
            }
        }

        val exoPlayer = remember(
            decoderMode,
            playerState.cacheDataSourceFactory,
            audioPassthroughEnabled,
            effectiveHdrPreference
        ) {
            val filteringMode = when (effectiveHdrPreference) {
                HdrFormatPreference.HDR10_PLUS -> FfmpegFilteringMode.KEEP_HDR10_BASE
                HdrFormatPreference.DOLBY_VISION -> FfmpegFilteringMode.KEEP_DOLBY_VISION
                else -> FfmpegFilteringMode.AUTO
            }
            val extractorsFactory = FilteringExtractorsFactory(DefaultExtractorsFactory(), filteringMode)
            val mediaSourceFactory = playerState.cacheDataSourceFactory?.let { cacheFactory ->
                DefaultMediaSourceFactory(cacheFactory, extractorsFactory)
            } ?: DefaultMediaSourceFactory(context, extractorsFactory)
            val renderersFactory = createRenderersFactory(
                context = context,
                decoderMode = decoderMode,
                enableAudioPassthrough = audioPassthroughEnabled,
                hdrFormatPreference = effectiveHdrPreference
            ) { viewModel.state.value.subtitleOffsetMs }
            val builder = ExoPlayer.Builder(context, renderersFactory)
            builder.setMediaSourceFactory(mediaSourceFactory)
            builder.build().apply {
                playWhenReady = true
                volume = (playerState.volume.toFloat() / 100f).coerceIn(0f, 1f).toLong()
                setPlaybackSpeed(playerState.playbackSpeed)
            }
        }

        mediaItemWithSubtitles?.let { resolvedMediaItem ->
            LaunchedEffect(resolvedMediaItem, exoPlayer) {
                if (pendingTrackChangeEvent != null) {
                    return@LaunchedEffect
                }
                val shouldPlay = !playerState.isPaused
                exoPlayer.setMediaItem(resolvedMediaItem)
                exoPlayer.prepare()
                exoPlayer.seekTo(restorePositionMs)
                exoPlayer.playWhenReady = shouldPlay
            }

            LaunchedEffect(pendingTrackChangeEvent, isLocalFile, resolvedMediaItem) {
                val event = pendingTrackChangeEvent ?: return@LaunchedEffect
                val currentPositionMs = exoPlayer.currentPosition
                
                
                val wasPlaying = exoPlayer.playWhenReady
                restorePositionMs = currentPositionMs

                if (isLocalFile) {
                    event.audioIndex?.let { index ->
                        exoPlayer.applyLocalAudioSelection(index)
                    }
                    val updatedItem = exoPlayer.applyLocalSubtitleSelection(
                        targetIndex = event.subtitleIndex,
                        subtitleStreams = subtitleStreams,
                        mediaItem = currentMediaItem,
                        context = context,
                        serverUrl = serverUrl,
                        accessToken = accessToken,
                        isLocalFile = true
                    )
                    updatedItem?.let { item ->
                        exoPlayer.setMediaItem(item)
                        exoPlayer.prepare()
                    }
                } else {
                    
                    event.audioIndex?.let { audioIndex ->
                        val audioStream = audioStreams.firstOrNull { it.index == audioIndex }
                        exoPlayer.configureAudioTrackPreferences(audioStream)
                    }

                    val baseItem = playerState.videoUrl?.let { ExoMediaItem.fromUri(it) }
                    val updatedItem = when {
                        event.subtitleIndex == null -> {
                            exoPlayer.configureTextTrackPreferences(
                                stream = null,
                                subtitleStreams = subtitleStreams,
                                enabled = false
                            )
                            baseItem?.buildUpon()
                                ?.setSubtitleConfigurations(emptyList())
                                ?.build()
                        }

                        else -> {
                            val stream = subtitleStreams.firstOrNull { it.index == event.subtitleIndex }
                            when {
                                stream == null -> {
                                    exoPlayer.configureTextTrackPreferences(
                                        stream = null,
                                        subtitleStreams = subtitleStreams,
                                        enabled = false
                                    )
                                    baseItem?.buildUpon()
                                        ?.setSubtitleConfigurations(emptyList())
                                        ?.build()
                                }

                                stream.isExternal -> {
                                    exoPlayer.configureTextTrackPreferences(
                                        stream = stream,
                                        subtitleStreams = subtitleStreams,
                                        enabled = true
                                    )
                                    val config = buildSubtitleConfiguration(
                                        context = context,
                                        mediaItem = currentMediaItem,
                                        stream = stream,
                                        isLocalFile = false,
                                        serverUrl = serverUrl,
                                        accessToken = accessToken
                                    )
                                    baseItem?.buildUpon()
                                        ?.setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
                                        ?.build()
                                }
                                else -> {
                                    exoPlayer.configureTextTrackPreferences(
                                        stream = stream,
                                        subtitleStreams = subtitleStreams,
                                        enabled = true
                                    )
                                    baseItem
                                }
                            }
                        }
                    }
                    updatedItem?.let { item ->
                        exoPlayer.setMediaItem(item)
                        exoPlayer.prepare()
                    }
                }

                exoPlayer.seekTo(currentPositionMs)
                exoPlayer.playWhenReady = wasPlaying
                pendingTrackChangeEvent = null
            }

            val playbackListener = remember(exoPlayer) {
                object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            viewModel.reportPlaybackStop(
                                mediaId = currentMediaItem.id,
                                userId = userId,
                                accessToken = accessToken,
                                positionSeconds = playbackDuration,
                                isCompleted = true
                            )
                            viewModel.markAsWatched(currentMediaItem.id, userId)
                            viewModel.handlePlaybackEnded(userId, accessToken)
                            isPlaying = false
                            audioManager.abandonAudioFocusRequest(audioFocusRequest)
                            viewModel.updatePausedState(true)
                            viewModel.updateBufferingState(false)
                        }
                        if (state == Player.STATE_BUFFERING) {
                            isBuffering = true
                            viewModel.updateBufferingState(true)
                        } else if (state == Player.STATE_READY || state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                            isBuffering = false
                            viewModel.updateBufferingState(false)
                        }
                    }

                    override fun onIsPlayingChanged(isPlayingState: Boolean) {
                        isPlaying = isPlayingState
                        viewModel.updatePausedState(!isPlayingState)
                    }
                }
            }

            var playerView by remember { mutableStateOf<PlayerView?>(null) }

            LaunchedEffect(playerView, contentFrameRate) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@LaunchedEffect
                val rate = contentFrameRate?.takeIf { it > 0f } ?: return@LaunchedEffect
                if (lastAppliedFrameRate == rate) return@LaunchedEffect
                val applied = FrameRateHelper.requestFrameRate(
                    surfaceProvider = { playerView?.videoSurfaceView as? SurfaceView },
                    frameRate = rate
                )
                if (applied) {
                    lastAppliedFrameRate = rate
                    FrameRateHelper.showFrameRateToast(context, rate)
                }
            }

            DisposableEffect(playerView) {
                playerView?.keepScreenOn = true
                onDispose { playerView?.keepScreenOn = false }
            }

            var isPlayerReleased by remember { mutableStateOf(false) }

            DisposableEffect(exoPlayer) {
                exoPlayerRef = exoPlayer
                exoPlayer.addListener(playbackListener)
                isPlayerReleased = false
                onDispose {
                    restorePositionMs = exoPlayer.currentPosition
                    exoPlayer.removeListener(playbackListener)
                    if (exoPlayerRef === exoPlayer) {
                        exoPlayerRef = null
                    }
                    audioManager.abandonAudioFocusRequest(audioFocusRequest)
                    isPlayerReleased = true
                    exoPlayer.release()
                }
            }

            LaunchedEffect(playerState.startPositionMs, playerState.startPositionUpdateCount) {
                restorePositionMs = playerState.startPositionMs
                exoPlayer.seekTo(playerState.startPositionMs)
                playbackProgress = playerState.startPositionMs / 1000
            }

            LaunchedEffect(exoPlayer) {
                var lastPausedState: Boolean? = null
                var lastBufferingState: Boolean? = null
                while (!isPlayerReleased) {
                    val durationMs = exoPlayer.duration
                    if (durationMs > 0) {
                        playbackDuration = durationMs / 1000
                    }
                    if (!isSeeking) {
                        playbackProgress = exoPlayer.currentPosition / 1000
                    }
                    val currentlyPlaying = exoPlayer.isPlaying
                    if (isPlaying != currentlyPlaying) {
                        isPlaying = currentlyPlaying
                    }
                    val pausedState = !currentlyPlaying
                    if (lastPausedState != pausedState) {
                        viewModel.updatePausedState(pausedState)
                        lastPausedState = pausedState
                    }
                    val bufferingState = exoPlayer.playbackState == Player.STATE_BUFFERING
                    if (isBuffering != bufferingState) {
                        isBuffering = bufferingState
                    }
                    if (lastBufferingState != bufferingState) {
                        viewModel.updateBufferingState(bufferingState)
                        lastBufferingState = bufferingState
                    }
                    if (isPlayerReleased) {
                        break
                    }
                    delay(500)
                }
            }

            LaunchedEffect(isPlaying) {
                if (!isPlaying) return@LaunchedEffect
                while (isPlaying && !isPlayerReleased) {
                    viewModel.updatePlaybackPosition(exoPlayer.currentPosition)
                    if (isPlayerReleased) {
                        break
                    }
                    delay(500L)
                }
            }

            LaunchedEffect(playerState.subtitleOffsetMs) {
                val player = exoPlayerRef ?: return@LaunchedEffect
                val position = player.currentPosition
                if (position >= 0) {
                    
                    val seekBackPosition = (position - 100).coerceAtLeast(0)
                    player.seekTo(seekBackPosition)
                    delay(50)
                    player.seekTo(position)
                }
            }

            LaunchedEffect(exoPlayerRef, selectedSubtitleStream, isLocalFile) {
                val player = exoPlayerRef ?: return@LaunchedEffect
                when {
                    selectedSubtitleStream == null -> {
                        player.configureTextTrackPreferences(
                            stream = null,
                            subtitleStreams = subtitleStreams,
                            enabled = false
                        )
                    }

                    selectedSubtitleStream.isExternal -> {
                        player.configureTextTrackPreferences(
                            stream = selectedSubtitleStream,
                            subtitleStreams = subtitleStreams,
                            enabled = true
                        )
                    }

                    else -> {
                        player.configureTextTrackPreferences(
                            stream = selectedSubtitleStream,
                            subtitleStreams = subtitleStreams,
                            enabled = true
                        )
                    }
                }
            }

            fun updateExoDisplayMode(playerView: PlayerView, mode: DisplayMode) {
                when (mode) {
                    DisplayMode.FIT_SCREEN -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                    }

                    DisplayMode.CROP -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                    }

                    DisplayMode.STRETCH -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                    }

                    DisplayMode.ORIGINAL -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    }
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showControls = !showControls
                    },
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        subtitleView?.apply {
                            setApplyEmbeddedStyles(true)
                            setApplyEmbeddedFontSizes(true)
                            val captionStyle = CaptionStyleCompat(
                                Color.White.toArgb(),
                                Color.Transparent.toArgb(),
                                Color.Transparent.toArgb(),
                                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                                Color.Black.toArgb(), Typeface.SANS_SERIF
                            )
                            setStyle(captionStyle)
                            val textSize = when (subtitleSize) {
                                PreferenceConstants.SUBTITLE_SIZE_SMALL -> 0.04f
                                PreferenceConstants.SUBTITLE_SIZE_MEDIUM -> 0.0533f
                                PreferenceConstants.SUBTITLE_SIZE_LARGE -> 0.0666f
                                PreferenceConstants.SUBTITLE_SIZE_EXTRA_LARGE -> 0.08f
                                else -> 0.0533f
                            }
                            setFractionalTextSize(textSize)
                            visibility = View.VISIBLE
                            z = Float.MAX_VALUE
                        }

                        updateExoDisplayMode(this, displayMode)
                    }.also { playerView = it }
                },
                update = { view ->
                    updateExoDisplayMode(view, displayMode)
                    view.subtitleView?.apply {
                        visibility = if (selectedSubtitleStream != null) View.VISIBLE else View.GONE
                        z = Float.MAX_VALUE
                        val textSize = when (subtitleSize) {
                            PreferenceConstants.SUBTITLE_SIZE_SMALL -> 0.04f
                            PreferenceConstants.SUBTITLE_SIZE_MEDIUM -> 0.0533f
                            PreferenceConstants.SUBTITLE_SIZE_LARGE -> 0.0666f
                            PreferenceConstants.SUBTITLE_SIZE_EXTRA_LARGE -> 0.08f
                            else -> 0.0533f
                        }
                        setFractionalTextSize(textSize)
                    }
                }
            )

            VideoControlsOverlay(
                mediaTitle = currentMediaItem.name,
                seasonNumber = currentMediaItem.parentIndexNumber,
                episodeNumber = currentMediaItem.indexNumber,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                duration = playbackDuration,
                currentPosition = playbackProgress,
                isVisible = showControls,
                onPlayPauseClick = {
                    if (exoPlayer.isPlaying) {
                        viewModel.savePlaybackPosition(
                            currentMediaItem.id,
                            userId,
                            accessToken,
                            playbackProgress
                        )
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                onSeek = { position ->
                    cancelAutoSkip()
                    isSeeking = true
                    playbackProgress = position
                },
                onSeekComplete = { position ->
                    cancelAutoSkip()
                    exoPlayer.seekTo(position * 1000)
                    isSeeking = false
                    viewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, position)
                },
                onBackClick = {
                    viewModel.savePlaybackPosition(
                        currentMediaItem.id,
                        userId,
                        accessToken,
                        playbackProgress
                    )
                    viewModel.reportPlaybackStop(
                        mediaId = currentMediaItem.id,
                        userId = userId,
                        accessToken = accessToken,
                        positionSeconds = playbackProgress,
                        isCompleted = playbackProgress >= playbackDuration - 5
                    )
                    onBackClick()
                },
                onDismissControls = { showControls = false },
                onShowControls = { showControls = true },
                onSkipBackward = {
                    cancelAutoSkip()
                    exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                },
                onSkipForward = {
                    cancelAutoSkip()
                    exoPlayer.seekTo(exoPlayer.currentPosition + 10_000)
                },
                skipButtonVisible = showOverlaySkipButton,
                skipButtonLabel = skipLabel,
                onSkipButtonClick = {
                    viewModel.skipSegment()
                    currentSkipSegmentId = null
                    skipSegmentEndMs = 0L
                    skipPromptDeadlineMs = 0L
                    showOverlaySkipButton = false
                    showFloatingSkipButton = false
                    showControls = false
                },
                floatingSkipButtonVisible = showFloatingSkipButton && skipLabel.isNotEmpty(),
                onFloatingSkipButtonClick = {
                    viewModel.skipSegment()
                    currentSkipSegmentId = null
                    skipSegmentEndMs = 0L
                    skipPromptDeadlineMs = 0L
                    showOverlaySkipButton = false
                    showFloatingSkipButton = false
                    showControls = false
                },
                autoSkipProgress = autoSkipProgress,
                isAutoSkipActive = isAutoSkipActive,
                onCancelAutoSkip = { cancelAutoSkip() },
                onPlayPrevious = if (playerState.hasPreviousEpisode) {
                    {
                        viewModel.savePlaybackPosition(
                            currentMediaItem.id,
                            userId,
                            accessToken,
                            playbackProgress
                        )
                        viewModel.reportPlaybackStop(
                            mediaId = currentMediaItem.id,
                            userId = userId,
                            accessToken = accessToken,
                            positionSeconds = playbackProgress,
                            isCompleted = playbackProgress >= playbackDuration - 5
                        )
                        viewModel.playPreviousEpisode()
                    }
                } else {
                    null
                },
                onPlayNext = if (playerState.hasNextEpisode) {
                    {
                        viewModel.savePlaybackPosition(
                            currentMediaItem.id,
                            userId,
                            accessToken,
                            playbackProgress
                        )
                        viewModel.reportPlaybackStop(
                            mediaId = currentMediaItem.id,
                            userId = userId,
                            accessToken = accessToken,
                            positionSeconds = playbackProgress,
                            isCompleted = playbackProgress >= playbackDuration - 5
                        )
                        viewModel.playNextEpisode()
                    }
                } else {
                    null
                },
                onAudioClick = if (audioStreams.isNotEmpty()) {
                    { showAudioDialog = true }
                } else {
                    null
                },
                onSubtitleClick = if (subtitleStreams.isNotEmpty()) {
                    { showSubtitleDialog = true }
                } else {
                    null
                },
                onSubtitleOptionsClick = if (subtitleStreams.isNotEmpty()) {
                    { showSubtitleMenu = true }
                } else {
                    null
                },
                videoQualityLabel = playerState.playbackTranscodeOption.label,
                onVideoClick = { viewModel.showVideoQualityDialog() }
            )


            if (showAudioDialog) {
                AudioTrackDialog(
                    audioStreams = audioStreams,
                    selectedAudioStream = audioStreams.firstOrNull { it.index == currentAudioTrack },
                    onAudioSelected = { stream ->
                        val currentPosition = exoPlayer.currentPosition
                        viewModel.updateAudioStream(
                            audioStreamIndex = stream.index,
                            mediaItem = currentMediaItem,
                            serverUrl = serverUrl,
                            accessToken = accessToken,
                            shouldRebuildUrl = !isLocalFile,
                            startPositionMs = currentPosition
                        )
                        restorePositionMs = currentPosition
                        if (isLocalFile) {
                            exoPlayer.applyLocalAudioSelection(stream.index)
                            exoPlayer.seekTo(currentPosition)
                            exoPlayer.playWhenReady = true
                        } else {
                            
                            exoPlayer.configureAudioTrackPreferences(stream)

                            val baseItem = viewModel.state.value.videoUrl?.let { url ->
                                ExoMediaItem.fromUri(url)
                            }
                            val subtitleStream = subtitleStreams.firstOrNull { it.index == currentSubtitleTrack }
                            val mediaItem = if (subtitleStream?.isExternal == true) {
                                val config = buildSubtitleConfiguration(
                                    context = context,
                                    mediaItem = currentMediaItem,
                                    stream = subtitleStream,
                                    isLocalFile = false,
                                    serverUrl = serverUrl,
                                    accessToken = accessToken
                                )
                                baseItem?.buildUpon()
                                    ?.setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
                                    ?.build()
                            } else {
                                baseItem
                            }
                            mediaItem?.let { item ->
                                exoPlayer.setMediaItem(item)
                                exoPlayer.prepare()
                                exoPlayer.seekTo(currentPosition)
                                exoPlayer.playWhenReady = true
                            }
                        }
                        showAudioDialog = false
                    },
                    onDismiss = { showAudioDialog = false }
                )
            }

            if (showSubtitleDialog) {
                SubtitleDialog(
                    subtitleStreams = subtitleStreams,
                    selectedSubtitleStream = subtitleStreams.firstOrNull { it.index == currentSubtitleTrack },
                    onSubtitleSelected = { stream ->
                        val currentPosition = exoPlayer.currentPosition
                        viewModel.updateSubtitleStream(
                            subtitleStreamIndex = stream?.index,
                            mediaItem = currentMediaItem,
                            serverUrl = serverUrl,
                            accessToken = accessToken,
                            shouldRebuildUrl = !isLocalFile,
                            startPositionMs = currentPosition
                        )
                        restorePositionMs = currentPosition
                        if (isLocalFile) {
                            val updatedItem = exoPlayer.applyLocalSubtitleSelection(
                                targetIndex = stream?.index,
                                subtitleStreams = subtitleStreams,
                                mediaItem = currentMediaItem,
                                context = context,
                                serverUrl = serverUrl,
                                accessToken = accessToken,
                                isLocalFile = true
                            )
                            updatedItem?.let { item ->
                                exoPlayer.setMediaItem(item)
                                exoPlayer.prepare()
                            }
                            exoPlayer.seekTo(currentPosition)
                            exoPlayer.playWhenReady = true
                        } else {
                            val baseItem = viewModel.state.value.videoUrl?.let { url ->
                                ExoMediaItem.fromUri(url)
                            }
                            val finalItem = when {
                                stream == null -> {
                                    exoPlayer.configureTextTrackPreferences(
                                        stream = null,
                                        subtitleStreams = subtitleStreams,
                                        enabled = false
                                    )
                                    baseItem?.buildUpon()
                                        ?.setSubtitleConfigurations(emptyList())
                                        ?.build()
                                }

                                stream.isExternal -> {
                                    exoPlayer.configureTextTrackPreferences(
                                        stream = stream,
                                        subtitleStreams = subtitleStreams,
                                        enabled = true
                                    )
                                    val config = buildSubtitleConfiguration(
                                        context = context,
                                        mediaItem = currentMediaItem,
                                        stream = stream,
                                        isLocalFile = false,
                                        serverUrl = serverUrl,
                                        accessToken = accessToken
                                    )
                                    baseItem?.buildUpon()
                                        ?.setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
                                        ?.build()
                                }

                                else -> baseItem
                            }
                            finalItem?.let { item ->
                                exoPlayer.setMediaItem(item)
                                exoPlayer.prepare()
                            }
                            exoPlayer.seekTo(currentPosition)
                            exoPlayer.playWhenReady = true
                        }
                        showSubtitleDialog = false
                    },
                    onDismiss = { showSubtitleDialog = false }
                )
            }

            if (showSubtitleMenu) {
                SubtitleMenuDialog(
                    onSubtitleOffsetClick = {
                        showSubtitleMenu = false
                        showSubtitleOffsetDialog = true
                    },
                    onSubtitleSizeClick = {
                        showSubtitleMenu = false
                        showSubtitleSizeDialog = true
                    },
                    onDismiss = { showSubtitleMenu = false }
                )
            }

            if (showSubtitleOffsetDialog) {
                SubtitleOptionsDialog(
                    currentOffsetMs = playerState.subtitleOffsetMs,
                    onOffsetChange = { offset -> viewModel.updateSubtitleOffset(offset) },
                    onDismiss = { showSubtitleOffsetDialog = false }
                )
            }

            if (showSubtitleSizeDialog) {
                SubtitleSizeDialog(
                    currentSize = subtitleSize,
                    onSizeChange = { size -> userDataViewModel.setSubtitleSize(size) },
                    onDismiss = { showSubtitleSizeDialog = false }
                )
            }

            if (playerState.showVideoQualityDialog) {
                VideoQualityDialog(
                    selectedOption = playerState.playbackTranscodeOption,
                    onSelect = { option ->
                        val currentPosition = exoPlayer.currentPosition
                        viewModel.selectTranscodeOption(option, startPositionMs = currentPosition)
                    },
                    onDismiss = { viewModel.hideVideoQualityDialog() }
                )
            }

            if (showDisplayDialog) {
                DisplayModeSelectionDialog(
                    currentMode = displayMode,
                    onModeSelected = { mode ->
                        userDataViewModel.setDisplayMode(mode)
                        showDisplayDialog = false
                    },
                    onDismiss = { showDisplayDialog = false }
                )
            }

            if (showDecoderDialog) {
                DecoderSelectionDialog(
                    currentMode = decoderMode,
                    onModeSelected = { mode ->
                        userDataViewModel.setDecoderMode(mode)
                        context.showToast("Decoder mode set to ${mode.name}")
                        showDecoderDialog = false
                    },
                    onDismiss = { showDecoderDialog = false }
                )
            }

            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Minsk
                )
            }
        }

        GestureFeedbackOverlay(
            feedback = gestureFeedback,
            modifier = Modifier.align(Alignment.Center)
        )

        
        if (playerState.showEndScreen) {
            PlaybackEndScreen(
                currentMediaItem = currentMediaItem,
                nextEpisode = playerState.nextEpisode,
                onDeckItems = playerState.onDeckItems,
                serverUrl = serverUrl,
                accessToken = accessToken,
                autoPlayDelaySeconds = 10,
                onReplay = {
                    viewModel.replayCurrentVideo()
                    exoPlayer.seekTo(0)
                    exoPlayer.playWhenReady = true
                },
                onPlayNext = {
                    viewModel.playNextEpisode()
                },
                onPlayItem = { mediaItem ->
                    viewModel.playSpecificItem(mediaItem)
                },
                onDismiss = {
                    viewModel.dismissEndScreen()
                    onBackClick()
                }
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun createRenderersFactory(
    context: Context,
    decoderMode: DecoderMode,
    enableAudioPassthrough: Boolean,
    hdrFormatPreference: HdrFormatPreference,
    enableFfmpegAudio: Boolean = true,
    subtitleOffsetProvider: () -> Long
): DefaultRenderersFactory {
    val filteringMode = when (hdrFormatPreference) {
        HdrFormatPreference.HDR10_PLUS -> FfmpegFilteringMode.KEEP_HDR10_BASE
        HdrFormatPreference.DOLBY_VISION -> FfmpegFilteringMode.KEEP_DOLBY_VISION
        else -> FfmpegFilteringMode.AUTO
    }
    return OffsetRenderersFactory(
        context = context,
        subtitleOffsetProvider = subtitleOffsetProvider,
        enableAudioPassthrough = enableAudioPassthrough,
        hdrFormatPreference = hdrFormatPreference,
        enableFfmpegAudio = enableFfmpegAudio
    ).apply {
        experimentalSetHdrFilteringMode(filteringMode)
        when (decoderMode) {
            DecoderMode.HARDWARE_ONLY -> {
                setEnableDecoderFallback(false)
            }

            DecoderMode.SOFTWARE_ONLY -> {
                setEnableDecoderFallback(true)
            }

            DecoderMode.AUTO -> {
                setEnableDecoderFallback(true)
            }
        }
    }
}

private fun ExoPlayer.findTrackOverride(
    trackType: Int,
    stream: MediaStream,
    streams: List<MediaStream>
): TrackSelectionOverride? {
    val streamPosition = streams.indexOfFirst { it.index == stream.index }
    return currentTracks.groups.firstNotNullOfOrNull { group ->
        if (group.type != trackType) return@firstNotNullOfOrNull null
        val trackGroup = group.mediaTrackGroup
        (0 until group.length).firstNotNullOfOrNull { trackIndex ->
            val format = group.getTrackFormat(trackIndex)
            if (format.matchesStream(stream, trackIndex, streamPosition)) {
                TrackSelectionOverride(trackGroup, trackIndex)
            } else {
                null
            }
        }
    }
}

private fun ExoPlayer.findTrackOverride(
    trackType: Int,
    targetIndex: Int
): TrackSelectionOverride? {
    return currentTracks.groups.firstNotNullOfOrNull { group ->
        if (group.type == trackType) {
            val trackGroup = group.mediaTrackGroup
            (0 until group.length).firstNotNullOfOrNull { trackIndex ->
                val format = group.getTrackFormat(trackIndex)
                if (format.id?.toIntOrNull() == targetIndex) {
                    TrackSelectionOverride(trackGroup, trackIndex)
                } else {
                    null
                }
            }
        } else {
            null
        }
    }
}

private fun ExoPlayer.applyLocalAudioSelection(targetIndex: Int) {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)

    findTrackOverride(C.TRACK_TYPE_AUDIO, targetIndex)?.let { builder.addOverride(it) }
    trackSelectionParameters = builder.build()
}

private fun buildSubtitleConfiguration(
    context: Context,
    mediaItem: MediaItem,
    stream: MediaStream,
    isLocalFile: Boolean,
    serverUrl: String,
    accessToken: String
): ExoMediaItem.SubtitleConfiguration? {
    if (!stream.isExternal) return null

    val extension = stream.codec.toSubtitleFileExtension()
    val localFile = getLocalSubtitleFile(context, mediaItem.id, stream.index, extension)
    val subtitleUri = when {
        isLocalFile -> localFile?.toUri()
        else -> localFile?.toUri()
            ?: mediaItem.getSubtitleUrl(serverUrl, accessToken, stream.index).toUri()
    } ?: return null

    val mimeType = when (extension.lowercase()) {
        "srt" -> MimeTypes.APPLICATION_SUBRIP
        "sup" -> MimeTypes.APPLICATION_PGS
        "sub" -> MimeTypes.APPLICATION_VOBSUB
        "vtt" -> MimeTypes.TEXT_VTT
        "dvb" -> MimeTypes.APPLICATION_DVBSUBS
        "cea608" -> MimeTypes.APPLICATION_CEA608
        "cea708" -> MimeTypes.APPLICATION_CEA708
        "mp4vtt", "tx3g" -> MimeTypes.APPLICATION_MP4VTT
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        "ttml" -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }

    return ExoMediaItem.SubtitleConfiguration.Builder(subtitleUri)
        .setMimeType(mimeType)
        .setLanguage(stream.language)
        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
        .build()
}

private fun ExoPlayer.configureTextTrackPreferences(
    stream: MediaStream?,
    subtitleStreams: List<MediaStream>,
    enabled: Boolean
) {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)

    if (enabled && stream != null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)

        if (stream.isExternal) {
            stream.preferredLanguage()?.let { language ->
                builder.setPreferredTextLanguage(language)
            }
        } else {
            val override = findTrackOverride(C.TRACK_TYPE_TEXT, stream, subtitleStreams)
            if (override != null) {
                builder.addOverride(override)
            } else {
                stream.preferredLanguage()?.let { language ->
                    builder.setPreferredTextLanguage(language)
                }
            }
        }
    } else {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
    }

    trackSelectionParameters = builder.build()
}

private fun ExoPlayer.configureAudioTrackPreferences(stream: MediaStream?) {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)

    if (stream != null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)

        
        val override = findTrackOverride(C.TRACK_TYPE_AUDIO, stream.index)
        if (override != null) {
            builder.addOverride(override)
        } else {
            stream.preferredLanguage()?.let { language ->
                builder.setPreferredAudioLanguage(language)
            }
        }
    }

    trackSelectionParameters = builder.build()
}

private fun Format.matchesStream(
    stream: MediaStream,
    trackIndex: Int,
    streamPosition: Int
): Boolean {
    val formatId = id?.toIntOrNull()
    if (formatId == stream.index) {
        return true
    }

    val formatLanguage = language?.normalizeLanguageCode()
    if (formatLanguage != null) {
        val streamLanguages = sequenceOf(stream.language, stream.displayLanguage)
            .mapNotNull { it?.normalizeLanguageCode() }
        if (streamLanguages.any { it == formatLanguage }) {
            return true
        }
    }

    val formatLabel = label?.trim()?.lowercase(Locale.ROOT)
    if (!formatLabel.isNullOrEmpty()) {
        val streamLabels = sequenceOf(stream.displayTitle, stream.title)
            .mapNotNull { it?.trim()?.lowercase(Locale.ROOT) }
        if (streamLabels.any { it == formatLabel }) {
            return true
        }
    }

    if (streamPosition != -1 && trackIndex == streamPosition) {
        return true
    }

    return false
}

private fun MediaStream.preferredLanguage(): String? {
    return sequenceOf(language, displayLanguage)
        .mapNotNull { it?.trim() }
        .firstOrNull { it.isNotEmpty() }
}

private fun String.normalizeLanguageCode(): String {
    return trim()
        .lowercase(Locale.ROOT)
        .replace('_', '-')
}

private fun parseDolbyVisionProfile(text: String?): Int? {
    val value = text ?: return null
    val match = Regex("""profile\s+(\d+)""", RegexOption.IGNORE_CASE).find(value)
    return match?.groupValues?.getOrNull(1)?.toIntOrNull()
}

private fun isDolbyVisionStream(stream: MediaStream): Boolean {
    val doViTitle = stream.videoDoViTitle
    if (doViTitle?.contains("dolby", ignoreCase = true) == true) return true
    val displayTitle = stream.displayTitle
    if (displayTitle?.contains("dolby", ignoreCase = true) == true) return true
    val rangeType = stream.videoRangeType
    return rangeType?.contains("dovi", ignoreCase = true) == true ||
        rangeType?.contains("dolby", ignoreCase = true) == true
}

private fun resolveHdrPreferenceForStream(
    preference: HdrFormatPreference,
    videoStream: MediaStream?
): HdrFormatPreference {
    if (preference != HdrFormatPreference.AUTO || videoStream == null) return preference
    if (!isDolbyVisionStream(videoStream)) return preference

    val profile = videoStream.dvProfile
        ?: parseDolbyVisionProfile(videoStream.videoDoViTitle)
        ?: parseDolbyVisionProfile(videoStream.displayTitle)

    val supportedProfiles = CodecDetector.getDolbyVisionProfiles()
        .mapNotNull { parseDolbyVisionProfile(it) }
        .toSet()
    val supportsDolbyVision = supportedProfiles.isNotEmpty()

    val dvAllowed = when {
        profile == 7 -> false
        !supportsDolbyVision -> false
        profile == null -> true
        else -> supportedProfiles.contains(profile)
    }

    return if (dvAllowed) preference else HdrFormatPreference.HDR10_PLUS
}

private fun ExoPlayer.applyLocalSubtitleSelection(
    targetIndex: Int?,
    subtitleStreams: List<MediaStream>,
    mediaItem: MediaItem,
    context: Context,
    serverUrl: String,
    accessToken: String,
    isLocalFile: Boolean
): ExoMediaItem? {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)

    if (targetIndex == null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        trackSelectionParameters = builder.build()
        val currentItem = currentMediaItem
        val hasConfigs = currentItem?.localConfiguration?.subtitleConfigurations?.isNotEmpty() == true
        return if (hasConfigs) {
            currentItem.buildUpon()
                .setSubtitleConfigurations(emptyList())
                .build()
        } else {
            null
        }
    }

    val stream = subtitleStreams.firstOrNull { it.index == targetIndex }
    if (stream == null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        trackSelectionParameters = builder.build()
        return null
    }

    builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)

    return if (stream.isExternal) {
        stream.preferredLanguage()?.let { language ->
            builder.setPreferredTextLanguage(language)
        }
        trackSelectionParameters = builder.build()

        val config = buildSubtitleConfiguration(
            context = context,
            mediaItem = mediaItem,
            stream = stream,
            isLocalFile = isLocalFile,
            serverUrl = serverUrl,
            accessToken = accessToken
        )
        currentMediaItem?.buildUpon()
            ?.setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
            ?.build()
    } else {
        val override = findTrackOverride(C.TRACK_TYPE_TEXT, stream, subtitleStreams)
        if (override != null) {
            builder.addOverride(override)
        } else {
            stream.preferredLanguage()?.let { language ->
                builder.setPreferredTextLanguage(language)
            }
        }
        trackSelectionParameters = builder.build()

        val currentItem = currentMediaItem
        val hasConfigs = currentItem?.localConfiguration?.subtitleConfigurations?.isNotEmpty() == true
        if (hasConfigs) {
            currentItem.buildUpon()
                .setSubtitleConfigurations(emptyList())
                .build()
        } else {
            null
        }
    }
}

private fun getLocalSubtitleFile(
    context: Context,
    mediaId: String,
    index: Int,
    extension: String
): File? {
    val fileName = "$mediaId-$index.$extension"
    val internal = File(context.filesDir, "downloads/subtitles/$fileName")
    if (internal.exists()) return internal
    val externalDir = context.getExternalFilesDir(null)
    val external = externalDir?.let { File(it, "downloads/subtitles/$fileName") }
    return external?.takeIf { it.exists() }
}

@OptIn(UnstableApi::class)
private class OffsetRenderersFactory(
    context: Context,
    private val subtitleOffsetProvider: () -> Long,
    private val enableAudioPassthrough: Boolean,
    private val hdrFormatPreference: HdrFormatPreference,
    private val enableFfmpegAudio: Boolean = true
) : DefaultRenderersFactory(context) {

    init {
        val extensionMode = when {
            
            enableAudioPassthrough && !enableFfmpegAudio -> EXTENSION_RENDERER_MODE_OFF
            enableAudioPassthrough -> EXTENSION_RENDERER_MODE_ON
            enableFfmpegAudio -> EXTENSION_RENDERER_MODE_PREFER
            else -> EXTENSION_RENDERER_MODE_OFF
        }
        setExtensionRendererMode(extensionMode)
    }
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink? {
        val audioCapabilities = if (enableAudioPassthrough) {
            AudioCapabilities.getCapabilities(context)
        } else {
            AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES
        }
        // Build without context so AudioCapabilitiesReceiver doesn't override the toggle.
        return DefaultAudioSink.Builder()
            .setAudioCapabilities(audioCapabilities)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .build()
    }

    override fun buildTextRenderers(
        context: Context,
        output: androidx.media3.exoplayer.text.TextOutput,
        outputLooper: Looper,
        extensionRendererMode: Int,
        renderersList: ArrayList<Renderer>
    ) {
        val factory = OffsetSubtitleDecoderFactory(SubtitleDecoderFactory.DEFAULT, subtitleOffsetProvider)
        val startIndex = renderersList.size
        super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, renderersList)
        val replacementIndex = (startIndex until renderersList.size)
            .firstOrNull { renderersList[it] is TextRenderer }
        if (replacementIndex != null) {
            renderersList[replacementIndex] = TextRenderer(output, outputLooper, factory)
        } else {
            renderersList.add(TextRenderer(output, outputLooper, factory))
        }
    }

    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        videoRendererEventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        renderersList: ArrayList<Renderer>
    ) {
        val selector = if (hdrFormatPreference == HdrFormatPreference.HDR10_PLUS) {
            FilteringMediaCodecSelector(mediaCodecSelector)
        } else {
            mediaCodecSelector
        }
        super.buildVideoRenderers(
            context,
            extensionRendererMode,
            selector,
            enableDecoderFallback,
            eventHandler,
            videoRendererEventListener,
            allowedVideoJoiningTimeMs,
            renderersList
        )
    }
}

private class FilteringMediaCodecSelector(
    private val delegate: MediaCodecSelector
) : MediaCodecSelector {
    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean
    ): List<MediaCodecInfo> {
        if (mimeType == MimeTypes.VIDEO_DOLBY_VISION) {
            val hevc = delegate.getDecoderInfos(MimeTypes.VIDEO_H265, requiresSecureDecoder, requiresTunnelingDecoder)
            val avc = delegate.getDecoderInfos(MimeTypes.VIDEO_H264, requiresSecureDecoder, requiresTunnelingDecoder)
            return (hevc + avc).distinctBy { it.name }
        }
        return delegate.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
    }
}

private class OffsetSubtitleDecoderFactory(
    private val delegate: SubtitleDecoderFactory,
    private val subtitleOffsetProvider: () -> Long
) : SubtitleDecoderFactory {
    override fun supportsFormat(format: Format): Boolean = delegate.supportsFormat(format)

    override fun createDecoder(format: Format): SubtitleDecoder {
        val decoder = delegate.createDecoder(format)
        return OffsetSubtitleDecoder(decoder, subtitleOffsetProvider)
    }
}

private class OffsetSubtitleDecoder(
    private val delegate: SubtitleDecoder,
    private val subtitleOffsetProvider: () -> Long
) : SubtitleDecoder {
    override fun getName(): String = delegate.name

    override fun setOutputStartTimeUs(timeUs: Long) {
        delegate.setOutputStartTimeUs(timeUs)
    }

    override fun dequeueInputBuffer(): androidx.media3.extractor.text.SubtitleInputBuffer? {
        return delegate.dequeueInputBuffer()
    }

    override fun queueInputBuffer(inputBuffer: androidx.media3.extractor.text.SubtitleInputBuffer) {
        delegate.queueInputBuffer(inputBuffer)
    }

    override fun dequeueOutputBuffer(): SubtitleOutputBuffer? {
        val buffer = delegate.dequeueOutputBuffer() ?: return null
        applyOffset(buffer)
        return buffer
    }

    override fun flush() {
        delegate.flush()
    }

    override fun release() {
        delegate.release()
    }

    override fun setPositionUs(positionUs: Long) {
        delegate.setPositionUs(positionUs)
    }

    private fun applyOffset(buffer: SubtitleOutputBuffer) {
        val offsetMs = subtitleOffsetProvider().coerceIn(-MAX_USER_OFFSET_MS, MAX_USER_OFFSET_MS)
        if (offsetMs == 0L) {
            return
        }

        val offsetUs = offsetMs * 1000L
        try {
            val baseOffsetUs = SUBSAMPLE_OFFSET_FIELD.getLong(buffer)
            SUBSAMPLE_OFFSET_FIELD.setLong(buffer, safeAdd(baseOffsetUs, offsetUs))
            buffer.timeUs = safeAdd(buffer.timeUs, offsetUs)
            Log.d(TAG, "Applied subtitle offset ${offsetMs}ms")
        } catch (error: Exception) {
            Log.w(TAG, "Failed to apply subtitle offset", error)
        }
    }

    private fun safeAdd(value: Long, delta: Long): Long {
        val result = value + delta
        return when {
            delta > 0 && result < value -> Long.MAX_VALUE
            delta < 0 && result > value -> Long.MIN_VALUE
            else -> result
        }
    }

    companion object {
        private const val TAG = "SubtitleOffset"
        private const val MAX_USER_OFFSET_MS = 5000L
        private val SUBSAMPLE_OFFSET_FIELD = SubtitleOutputBuffer::class.java
            .getDeclaredField("subsampleOffsetUs")
            .apply { isAccessible = true }
    }
}
