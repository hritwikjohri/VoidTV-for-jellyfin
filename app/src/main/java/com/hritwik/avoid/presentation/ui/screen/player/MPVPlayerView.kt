package com.hritwik.avoid.presentation.ui.screen.player

import android.content.Context
import android.media.AudioManager
import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.playback.DecoderMode
import com.hritwik.avoid.domain.model.playback.DisplayMode
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
import com.hritwik.avoid.utils.MpvConfig
import com.hritwik.avoid.utils.RuntimeConfig
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.constants.PreferenceConstants.SKIP_PROMPT_FLOATING_DURATION_MS
import com.hritwik.avoid.utils.extensions.findActivity
import com.hritwik.avoid.utils.extensions.getSubtitleUrl
import com.hritwik.avoid.utils.extensions.toSubtitleFileExtension
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import dev.marcelsoftware.mpvcompose.DefaultLogObserver
import dev.marcelsoftware.mpvcompose.MPVLib
import dev.marcelsoftware.mpvcompose.MPVPlayer
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(UnstableApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun MpvPlayerView(
    mediaItem: MediaItem,
    playerState: VideoPlaybackState,
    decoderMode: DecoderMode,
    displayMode: DisplayMode,
    userId: String,
    accessToken: String,
    serverUrl: String,
    autoSkipSegments: Boolean,
    gesturesEnabled: Boolean,
    onBackClick: () -> Unit,
    videoPlaybackViewModel: VideoPlaybackViewModel,
    userDataViewModel: UserDataViewModel
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val mpvConfigOptions = remember(context) { MpvConfig.readOptions(context) }
    val mpvConfigKeys = remember(mpvConfigOptions) { mpvConfigOptions.keys.map { it.lowercase() }.toSet() }
    val currentMediaItem = playerState.mediaItem ?: mediaItem
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val audioFocusRequest = rememberAudioFocusRequest(audioManager)
    val scope = rememberCoroutineScope()
    val screenAspectRatio = remember {
        val metrics = context.resources.displayMetrics
        metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat()
    }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var resumeOnStart by remember { mutableStateOf(false) }
    var playbackDuration by remember { mutableLongStateOf(1L) }
    var playbackProgress by remember { mutableLongStateOf(playerState.startPositionMs / 1000) }
    var volume by remember { mutableLongStateOf(playerState.volume) }
    var isBuffering by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var isMpvInitialized by remember { mutableStateOf(false) }
    val window = activity?.window
    var brightness by remember {
        mutableFloatStateOf(window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f)
    }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var showSubtitleOffsetDialog by remember { mutableStateOf(false) }
    var showSubtitleSizeDialog by remember { mutableStateOf(false) }
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showDisplayDialog by remember { mutableStateOf(false) }
    val subtitleSize by userDataViewModel.subtitleSize.collectAsStateWithLifecycle()

    val audioStreams = playerState.availableAudioStreams
    val subtitleStreams = playerState.availableSubtitleStreams
    val currentAudioTrack = playerState.audioStreamIndex
    val currentSubtitleTrack = playerState.subtitleStreamIndex

    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }

    var skipLabel by remember { mutableStateOf("") }
    var currentSkipSegmentId by remember { mutableStateOf<String?>(null) }
    var skipSegmentEndMs by remember { mutableLongStateOf(0L) }
    var showOverlaySkipButton by remember { mutableStateOf(false) }
    var showFloatingSkipButton by remember { mutableStateOf(false) }
    var skipPromptDeadlineMs by remember { mutableLongStateOf(0L) }
    var autoSkipProgress by remember { mutableFloatStateOf(0f) }
    var isAutoSkipActive by remember { mutableStateOf(false) }
    var autoSkipCancelled by remember { mutableStateOf(false) }
    val initialSubtitleLanguage = subtitleStreams.firstOrNull { it.index == currentSubtitleTrack }?.language
        ?: playerState.preferredSubtitleLanguage

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
        MPVLib.setPropertyInt("volume", percent.roundToInt())
        videoPlaybackViewModel.updateVolume(volume)
        gestureFeedback = GestureFeedback.Volume(volume.toInt())
    }

    fun cancelAutoSkip() {
        if (isAutoSkipActive) {
            isAutoSkipActive = false
            autoSkipProgress = 0f
            autoSkipCancelled = true
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
                videoPlaybackViewModel.skipSegment()
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
        skipPromptDeadlineMs,
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

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && activity?.isInPictureInPictureMode == true) {
                showControls = false
            }
            if (event == Lifecycle.Event.ON_RESUME && activity?.isInPictureInPictureMode == false) {
                showControls = true
            }
            if ((event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) && activity?.isInPictureInPictureMode == false) {
                resumeOnStart = isPlaying
                MPVLib.setPropertyBoolean("pause", true)
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
                isPlaying = false
            }
            if ((event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) && activity?.isInPictureInPictureMode == false) {
                if (resumeOnStart) {
                    val focusResult = audioManager.requestAudioFocus(audioFocusRequest)
                    if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        MPVLib.setPropertyBoolean("pause", false)
                        isPlaying = true
                    }
                    resumeOnStart = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isRemoteSource = playerState.videoUrl?.let { it.toUri().scheme != "file" } ?: false

    var pendingTrackChangeEvent by remember { mutableStateOf<TrackChangeEvent?>(null) }

    fun getSubtitleFont(language: String?): String =
        when (language?.take(2)?.lowercase()) {
            "ar" -> "Noto Sans Arabic"
            "hi" -> "Noto Sans Devanagari"
            else -> "Noto Sans"
        }

    fun disableMpvSubtitles() {
        MPVLib.setPropertyString("sid", "no")
        MPVLib.setPropertyBoolean("sub-visibility", false)
        MPVLib.command(arrayOf("sub-remove", "all"))
    }

    fun applyMpvSubtitleSelection(subtitleIndex: Int?) {
        val language = subtitleStreams.firstOrNull { it.index == subtitleIndex }?.language
            ?: playerState.preferredSubtitleLanguage
        if ("sub-font" !in mpvConfigKeys) {
            MPVLib.setPropertyString("sub-font", getSubtitleFont(language))
        }

        if (subtitleIndex == null) {
            disableMpvSubtitles()
            return
        }

        val stream = subtitleStreams.firstOrNull { it.index == subtitleIndex }
        if (stream?.isExternal == true) {
            val extension = stream.codec.toSubtitleFileExtension()
            val localFile = getLocalSubtitleFile(context, currentMediaItem.id, subtitleIndex, extension)
            val filePath = localFile?.path
                ?: currentMediaItem.getSubtitleUrl(serverUrl, accessToken, subtitleIndex)
            MPVLib.command(arrayOf("sub-remove", "all"))
            MPVLib.command(arrayOf("sub-add", filePath, "select"))
            MPVLib.setPropertyBoolean("sub-visibility", true)
        } else {
            MPVLib.command(arrayOf("sub-remove", "all"))
            val mpvSubtitleIndex = subtitleIndex - audioStreams.size
            if (mpvSubtitleIndex >= 0) {
                MPVLib.setPropertyString("sid", mpvSubtitleIndex.toString())
                MPVLib.setPropertyBoolean("sub-visibility", true)
            } else {
                disableMpvSubtitles()
            }
        }
    }

    LaunchedEffect(Unit) {
        videoPlaybackViewModel.trackChangeEvents.collect { event ->
            pendingTrackChangeEvent = event
        }
    }

    val handleAudioTrackChange: (Int) -> Unit = { newAudioIndex ->
        videoPlaybackViewModel.updateAudioStream(
            newAudioIndex,
            currentMediaItem,
            serverUrl,
            accessToken,
            shouldRebuildUrl = isRemoteSource,
            startPositionMs = playbackProgress * 1000
        )
        MPVLib.setPropertyString("aid", newAudioIndex.toString())
    }

    val handleSubtitleTrackChange: (Int?) -> Unit = { newSubtitleIndex ->
        videoPlaybackViewModel.updateSubtitleStream(
            newSubtitleIndex,
            currentMediaItem,
            serverUrl,
            accessToken,
            shouldRebuildUrl = isRemoteSource,
            startPositionMs = playbackProgress * 1000
        )
        val lang = subtitleStreams.firstOrNull { it.index == newSubtitleIndex }?.language
            ?: playerState.preferredSubtitleLanguage
        MPVLib.setPropertyString("sub-font", getSubtitleFont(lang))

        if (newSubtitleIndex == null) {
            MPVLib.setPropertyString("sid", "no")
            MPVLib.setPropertyBoolean("sub-visibility", false)
            MPVLib.command(arrayOf("sub-remove", "all"))
        } else {
            val stream = subtitleStreams.firstOrNull { it.index == newSubtitleIndex }
            if (stream?.isExternal == true) {
                val extension = stream.codec.toSubtitleFileExtension()
                val localFile = getLocalSubtitleFile(context, currentMediaItem.id, newSubtitleIndex, extension)
                val filePath = localFile?.path
                    ?: currentMediaItem.getSubtitleUrl(serverUrl, accessToken, newSubtitleIndex)
                MPVLib.command(arrayOf("sub-add", filePath, "select"))
            } else {
                val audioStreamCount = audioStreams.size
                val mpvSubtitleIndex = newSubtitleIndex - audioStreamCount
                if (mpvSubtitleIndex >= 0) {
                    MPVLib.setPropertyString("sid", mpvSubtitleIndex.toString())
                    MPVLib.setPropertyBoolean("sub-visibility", true)
                } else {
                    MPVLib.setPropertyString("sid", "no")
                    MPVLib.setPropertyBoolean("sub-visibility", false)
                }
            }
        }
        MPVLib.setPropertyDouble("sub-delay", playerState.subtitleOffsetMs / 1000.0)
    }
    val handleDisplayModeChange: (DisplayMode) -> Unit = { newDisplayMode ->
        userDataViewModel.setDisplayMode(newDisplayMode)
        when (newDisplayMode) {
            DisplayMode.FIT_SCREEN -> {
                MPVLib.setPropertyString("panscan", "0")
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("video-aspect-override", "no")
            }
            DisplayMode.CROP -> {
                MPVLib.setPropertyString("panscan", "1")
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("video-aspect-override", "no")
            }
            DisplayMode.STRETCH -> {
                MPVLib.setPropertyString("panscan", "0")
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("video-aspect-override", screenAspectRatio.toString())
            }
            DisplayMode.ORIGINAL -> {
                MPVLib.setPropertyString("panscan", "0")
                MPVLib.setPropertyString("video-unscaled", "yes")
                MPVLib.setPropertyString("video-aspect-override", "no")
            }
        }
    }

    val resolvedVideoUrl = remember(playerState.videoUrl) {
        playerState.videoUrl?.let { url ->
            val uri = url.toUri()
            if (uri.scheme == "file") uri.path else url
        }
    }

    val windowInsetsController = remember(activity) {
        activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }

    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(1000)
            gestureFeedback = null
        }
    }

    DisposableEffect(audioManager) {
        audioManager.requestAudioFocus(audioFocusRequest)
        onDispose { audioManager.abandonAudioFocusRequest(audioFocusRequest) }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
            val focusResult = audioManager.requestAudioFocus(audioFocusRequest)
            if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                MPVLib.setPropertyBoolean("pause", true)
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
        onDispose {
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(isPlaying, userId, accessToken, currentMediaItem.id) {
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying) {
            if (!isSeeking && playbackProgress > 0) {
                videoPlaybackViewModel.savePlaybackPosition(
                    mediaId = currentMediaItem.id,
                    userId = userId,
                    accessToken = accessToken,
                    positionSeconds = playbackProgress
                )
            }
            delay(1000L)
        }
    }

    LaunchedEffect(resolvedVideoUrl, isMpvInitialized) {
        if (!isMpvInitialized) return@LaunchedEffect

        resolvedVideoUrl?.let {
            MPVLib.setOptionString("start", playbackProgress.toString())
            MPVLib.command(arrayOf("loadfile", it, "replace"))
            MPVLib.command(arrayOf("seek", playbackProgress.toString(), "absolute", "exact"))
            MPVLib.setPropertyBoolean("pause", false)
        }

        currentAudioTrack?.let { MPVLib.setPropertyString("aid", it.toString()) }
        applyMpvSubtitleSelection(currentSubtitleTrack)
    }

    LaunchedEffect(pendingTrackChangeEvent, isMpvInitialized) {
        val event = pendingTrackChangeEvent
        if (!isMpvInitialized || event == null) return@LaunchedEffect

        event.audioIndex?.let { index ->
            MPVLib.setPropertyString("aid", index.toString())
        }
        applyMpvSubtitleSelection(event.subtitleIndex)
        pendingTrackChangeEvent = null
    }

    BackHandler {
        videoPlaybackViewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
        videoPlaybackViewModel.reportPlaybackStop(
            mediaId = currentMediaItem.id,
            userId = userId,
            accessToken = accessToken,
            positionSeconds = playbackProgress,
            isCompleted = playbackProgress >= playbackDuration - 5
        )
        onBackClick()
    }

    DisposableEffect(Unit) {
        videoPlaybackViewModel.reportPlaybackStart(currentMediaItem.id, userId, accessToken)
        onDispose {
            videoPlaybackViewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
            videoPlaybackViewModel.reportPlaybackStop(
                mediaId = currentMediaItem.id,
                userId = userId,
                accessToken = accessToken,
                positionSeconds = playbackProgress,
                isCompleted = playbackProgress >= playbackDuration - 5
            )
        }
    }

    LaunchedEffect(displayMode, isMpvInitialized) {
        if (isMpvInitialized) {
            handleDisplayModeChange(displayMode)
        }
    }

    LaunchedEffect(playerState.subtitleOffsetMs, isMpvInitialized) {
        if (!isMpvInitialized) return@LaunchedEffect
        MPVLib.setPropertyDouble("sub-delay", playerState.subtitleOffsetMs / 1000.0)
    }

    LaunchedEffect(subtitleSize, isMpvInitialized) {
        if (!isMpvInitialized) return@LaunchedEffect
        val fontSize = when (subtitleSize) {
            PreferenceConstants.SUBTITLE_SIZE_SMALL -> 36
            PreferenceConstants.SUBTITLE_SIZE_MEDIUM -> 48
            PreferenceConstants.SUBTITLE_SIZE_LARGE -> 60
            PreferenceConstants.SUBTITLE_SIZE_EXTRA_LARGE -> 72
            else -> 48
        }
        MPVLib.setPropertyInt("sub-font-size", fontSize)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val mpvView = LocalView.current
        DisposableEffect(mpvView) {
            mpvView.keepScreenOn = true
            onDispose { mpvView.keepScreenOn = false }
        }
        MPVPlayer(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    showControls = !showControls
                },
            onInitialized = {
                MPVLib.addLogObserver(DefaultLogObserver())
                mpvConfigOptions.forEach { (option, value) ->
                    MPVLib.setOptionString(option, value)
                }
                setMpvOptionIfMissing("android-page-size", RuntimeConfig.pageSize.toString(), mpvConfigKeys)
                isMpvInitialized = true
                handleDisplayModeChange(displayMode)

                when (decoderMode) {
                    DecoderMode.HARDWARE_ONLY -> {
                        setMpvOptionIfMissing("hwdec", "mediacodec", mpvConfigKeys)
                        setMpvOptionIfMissing("vo", "gpu-next", mpvConfigKeys)
                        setMpvOptionIfMissing("vd-lavc-software-fallback", "no", mpvConfigKeys)
                    }
                    DecoderMode.SOFTWARE_ONLY -> {
                        setMpvOptionIfMissing("hwdec", "no", mpvConfigKeys)
                        setMpvOptionIfMissing("vd", "lavc", mpvConfigKeys)
                        setMpvOptionIfMissing("vo", "gpu", mpvConfigKeys)
                        setMpvOptionIfMissing("gpu-api", "opengl", mpvConfigKeys)
                    }
                    else -> {
                        setMpvOptionIfMissing("hwdec", "mediacodec", mpvConfigKeys)
                        setMpvOptionIfMissing("vo", "gpu-next", mpvConfigKeys)
                        setMpvOptionIfMissing("vd-lavc-software-fallback", "yes", mpvConfigKeys)
                    }
                }

                setMpvOptionIfMissing("vd-lavc-assume-old-x264", "yes", mpvConfigKeys)
                setMpvOptionIfMissing("codec-profile", "custom", mpvConfigKeys)
                setMpvOptionIfMissing("vd-lavc-check-hw-profile", "no", mpvConfigKeys)
                setMpvOptionIfMissing(
                    "vd-lavc-codec-whitelist",
                    "h264,hevc,vp8,vp9,av1,mpeg2video,mpeg4",
                    mpvConfigKeys
                )

                setMpvOptionIfMissing("sub-codepage", "auto", mpvConfigKeys)
                setMpvOptionIfMissing("sub-fix-timing", "yes", mpvConfigKeys)
                setMpvOptionIfMissing("blend-subtitles", "yes", mpvConfigKeys)
                setMpvOptionIfMissing("sub-forced-only", "no", mpvConfigKeys)

                currentAudioTrack?.let { MPVLib.setOptionString("aid", it.toString()) }

                val initialSubtitleStream = subtitleStreams.firstOrNull { it.index == currentSubtitleTrack }
                val initialMpvSubtitleIndex = currentSubtitleTrack?.let { it - audioStreams.size }
                if (
                    initialSubtitleStream?.isExternal == true ||
                    initialMpvSubtitleIndex == null ||
                    initialMpvSubtitleIndex < 0
                ) {
                    MPVLib.setOptionString("sid", "no")
                    MPVLib.setOptionString("sub-visibility", "no")
                } else {
                    MPVLib.setOptionString("sid", initialMpvSubtitleIndex.toString())
                    MPVLib.setOptionString("sub-visibility", "yes")
                }

                configureSubtitleFonts(
                    subtitleStreams.firstOrNull()?.codec,
                    context,
                    getSubtitleFont(initialSubtitleLanguage),
                    mpvConfigKeys
                )

                MPVLib.setPropertyBoolean("keep-open", true)
                MPVLib.setPropertyInt("volume", volume.toInt())
            },
            observedProperties = {
                long("duration")
                long("time-pos")
                boolean("eof-reached")
                boolean("pause")
                boolean("paused-for-cache")
                int("volume")
                double("speed")
                boolean("seeking")
                boolean("sub-visibility")
            },
            propertyObserver = {
                long("duration") { duration ->
                    playbackDuration = duration
                }
                long("time-pos") { timePos ->
                    if (!isSeeking) {
                        playbackProgress = timePos
                        videoPlaybackViewModel.updatePlaybackPosition(timePos * 1000)
                    }
                }
                boolean("eof-reached") { eofReached ->
                    if (eofReached) {
                        scope.launch {
                            videoPlaybackViewModel.reportPlaybackStop(
                                mediaId = currentMediaItem.id,
                                userId = userId,
                                accessToken = accessToken,
                                positionSeconds = playbackDuration,
                                isCompleted = true
                            )
                            videoPlaybackViewModel.markAsWatched(currentMediaItem.id, userId)
                            videoPlaybackViewModel.handlePlaybackEnded(userId, accessToken)
                        }
                    }
                }
                boolean("pause") { paused ->
                    isPlaying = !paused
                    videoPlaybackViewModel.updatePausedState(paused)
                }
                boolean("paused-for-cache") { pausedForCache ->
                    isBuffering = pausedForCache
                    videoPlaybackViewModel.updateBufferingState(pausedForCache)
                }
                long("volume") { vol ->
                    volume = vol
                    videoPlaybackViewModel.updateVolume(vol)
                }
                boolean("seeking") { seeking ->
                    isSeeking = seeking
                }
                boolean("sub-visibility") { visible ->
                    Log.d("SubtitleVisibility", "Subtitle visibility: $visible")
                }
            }
        )

        LaunchedEffect(playerState.startPositionMs, playerState.startPositionUpdateCount) {
            MPVLib.command(
                arrayOf(
                    "seek",
                    (playerState.startPositionMs / 1000).toString(),
                    "absolute",
                    "exact"
                )
            )
            playbackProgress = playerState.startPositionMs / 1000
        }

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
                if (isPlaying) {
                    videoPlaybackViewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
                }
                isPlaying = !isPlaying
                MPVLib.setPropertyBoolean("pause", !isPlaying)
            },
            onSeek = { position ->
                isSeeking = true
                playbackProgress = position
            },
            onSeekComplete = { position ->
                MPVLib.command(arrayOf("seek", position.toString(), "absolute"))
                isSeeking = false
                videoPlaybackViewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, position)
            },
            onBackClick = {
                videoPlaybackViewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
                videoPlaybackViewModel.reportPlaybackStop(
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
                MPVLib.command(arrayOf("seek", "-10", "relative"))
            },
            onSkipForward = {
                MPVLib.command(arrayOf("seek", "10", "relative"))
            },
            skipButtonVisible = showOverlaySkipButton,
            skipButtonLabel = skipLabel,
            onSkipButtonClick = {
                videoPlaybackViewModel.skipSegment()
                currentSkipSegmentId = null
                skipSegmentEndMs = 0L
                skipPromptDeadlineMs = 0L
                showOverlaySkipButton = false
                showFloatingSkipButton = false
                showControls = false
            },
            floatingSkipButtonVisible = showFloatingSkipButton && skipLabel.isNotEmpty(),
            onFloatingSkipButtonClick = {
                videoPlaybackViewModel.skipSegment()
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
                    videoPlaybackViewModel.savePlaybackPosition(
                        currentMediaItem.id,
                        userId,
                        accessToken,
                        playbackProgress
                    )
                    videoPlaybackViewModel.reportPlaybackStop(
                        mediaId = currentMediaItem.id,
                        userId = userId,
                        accessToken = accessToken,
                        positionSeconds = playbackProgress,
                        isCompleted = playbackProgress >= playbackDuration - 5
                    )
                    videoPlaybackViewModel.playPreviousEpisode()
                }
            } else {
                null
            },
            onPlayNext = if (playerState.hasNextEpisode) {
                {
                    videoPlaybackViewModel.savePlaybackPosition(
                        currentMediaItem.id,
                        userId,
                        accessToken,
                        playbackProgress
                    )
                    videoPlaybackViewModel.reportPlaybackStop(
                        mediaId = currentMediaItem.id,
                        userId = userId,
                        accessToken = accessToken,
                        positionSeconds = playbackProgress,
                        isCompleted = playbackProgress >= playbackDuration - 5
                    )
                    videoPlaybackViewModel.playNextEpisode()
                }
            } else {
                null
            },
            onAudioClick = { showAudioDialog = true },
            onSubtitleClick = { showSubtitleDialog = true },
            onSubtitleOptionsClick = { showSubtitleMenu = true },
            videoQualityLabel = playerState.playbackTranscodeOption.label,
            onVideoClick = { videoPlaybackViewModel.showVideoQualityDialog() }
        )


        if (showAudioDialog) {
            AudioTrackDialog(
                audioStreams = audioStreams,
                selectedAudioStream = audioStreams.firstOrNull { it.index == currentAudioTrack },
                onAudioSelected = { stream ->
                    handleAudioTrackChange(stream.index)
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
                    handleSubtitleTrackChange(stream?.index)
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
                onOffsetChange = { offset -> videoPlaybackViewModel.updateSubtitleOffset(offset) },
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
                    val currentPositionMs = playbackProgress * 1000
                    videoPlaybackViewModel.selectTranscodeOption(option, startPositionMs = currentPositionMs)
                },
                onDismiss = { videoPlaybackViewModel.hideVideoQualityDialog() }
            )
        }

        if (showDisplayDialog) {
            DisplayModeSelectionDialog(
                currentMode = displayMode,
                onModeSelected = { mode ->
                    userDataViewModel.setDisplayMode(mode)
                    handleDisplayModeChange(mode)
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
                    val pos = playbackProgress
                    when (decoderMode) {
                        DecoderMode.HARDWARE_ONLY -> {
                            setMpvOptionIfMissing("hwdec", "mediacodec", mpvConfigKeys)
                            setMpvOptionIfMissing("vo", "gpu", mpvConfigKeys)
                            setMpvOptionIfMissing("vd-lavc-software-fallback", "no", mpvConfigKeys)
                        }
                        DecoderMode.SOFTWARE_ONLY -> {
                            setMpvOptionIfMissing("hwdec", "no", mpvConfigKeys)
                            setMpvOptionIfMissing("vd", "lavc", mpvConfigKeys)
                            setMpvOptionIfMissing("vo", "gpu", mpvConfigKeys)
                            setMpvOptionIfMissing("gpu-api", "opengl", mpvConfigKeys)
                        }
                        else -> {
                            setMpvOptionIfMissing("hwdec", "mediacodec", mpvConfigKeys)
                            setMpvOptionIfMissing("vd-lavc-software-fallback", "yes", mpvConfigKeys)
                        }
                    }
                    resolvedVideoUrl?.let { url ->
                        MPVLib.command(arrayOf("loadfile", url, "replace"))
                        MPVLib.command(arrayOf("seek", pos.toString(), "absolute", "exact"))
                        MPVLib.setPropertyBoolean("pause", !isPlaying)
                    }
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
                    videoPlaybackViewModel.replayCurrentVideo()
                    MPVLib.command(arrayOf("seek", "0", "absolute", "exact"))
                    MPVLib.setPropertyBoolean("pause", false)
                },
                onPlayNext = {
                    videoPlaybackViewModel.playNextEpisode()
                },
                onPlayItem = { mediaItem ->
                    videoPlaybackViewModel.playSpecificItem(mediaItem)
                },
                onDismiss = {
                    videoPlaybackViewModel.dismissEndScreen()
                    onBackClick()
                }
            )
        }
    }
}

private fun setMpvOptionIfMissing(option: String, value: String, overriddenOptions: Set<String>) {
    if (option.lowercase() !in overriddenOptions) {
        MPVLib.setOptionString(option, value)
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

internal fun configureSubtitleFonts(
    codec: String?,
    context: Context,
    fallbackFont: String,
    overriddenOptions: Set<String> = emptySet()
) {
    setMpvOptionIfMissing("embeddedfonts", "yes", overriddenOptions)
    setMpvOptionIfMissing("sub-fonts-dir", File(context.filesDir, "fonts").absolutePath, overriddenOptions)
    setMpvOptionIfMissing("sub-font-provider", "file", overriddenOptions)
    setMpvOptionIfMissing("sub-font", fallbackFont, overriddenOptions)

    if (codec?.lowercase() !in listOf("ass", "ssa")) {
        setMpvOptionIfMissing("sub-ass", "yes", overriddenOptions)
        setMpvOptionIfMissing("sub-ass-override", "no", overriddenOptions)
        setMpvOptionIfMissing("sub-font-size", "48", overriddenOptions)
        setMpvOptionIfMissing("sub-color", "#FFFFFFFF", overriddenOptions)
        setMpvOptionIfMissing("sub-shadow-color", "#80000000", overriddenOptions)
        setMpvOptionIfMissing("sub-shadow-offset", "1", overriddenOptions)
        setMpvOptionIfMissing("sub-margin-y", "20", overriddenOptions)
        setMpvOptionIfMissing("sub-align-y", "bottom", overriddenOptions)
    }
}
