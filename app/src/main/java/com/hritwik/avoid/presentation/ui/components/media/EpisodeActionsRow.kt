package com.hritwik.avoid.presentation.ui.components.media

import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.playback.PlaybackInfo
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.presentation.ui.components.dialogs.AudioTrackDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.SubtitleDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.VersionSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.VideoQualityDialog
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.constants.AppConstants.RESUME_THRESHOLD
import com.hritwik.avoid.utils.extensions.getPlaybackUrl
import com.hritwik.avoid.utils.extensions.launchExternalVideoPlayer
import com.hritwik.avoid.utils.extensions.showToast
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import ir.kaaveh.sdpcompose.ssp

@OptIn(UnstableApi::class)
@Composable
fun EpisodeActionsRow(
    modifier: Modifier = Modifier,
    serverUrl: String,
    episode: MediaItem,
    focusRequester: FocusRequester,
    onPlayClick: (PlaybackInfo) -> Unit,
    buildEpisodePlaybackInfo: (MediaItem, Long) -> PlaybackInfo,
    onRequestEpisodeFocus: () -> Unit,
    onRequestNextSectionFocus: () -> Boolean = { false },
    onFocusChanged: (Boolean) -> Unit = {},
    videoPlaybackViewModel: VideoPlaybackViewModel,
    authViewModel: AuthServerViewModel = hiltViewModel(),
    userDataViewModel: UserDataViewModel = hiltViewModel(),
    seasonAccentColor: Color = Color(0xFF1976D2),
) {
    val playbackState by videoPlaybackViewModel.state.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val playbackSettings by userDataViewModel.playbackSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    
    val buttonColor = seasonAccentColor

    val playbackOptions = playbackState.playbackOptions
    val playbackItemForActions = playbackState.mediaItem ?: episode
    val playbackPositionTicks = playbackItemForActions.userData?.playbackPositionTicks ?: 0L
    val runTimeTicks = playbackItemForActions.runTimeTicks

    val hasResumableProgress = playbackPositionTicks > 0L &&
            (runTimeTicks == null || playbackPositionTicks < runTimeTicks - RESUME_THRESHOLD)

    val resumePositionTicks = when {
        playbackOptions.resumePositionTicks > 0L -> playbackOptions.resumePositionTicks
        hasResumableProgress -> playbackPositionTicks
        playbackState.startPositionMs > 0L -> playbackState.startPositionMs * 10_000
        else -> 0L
    }
    val shouldResume = hasResumableProgress && !playbackOptions.startFromBeginning
    val externalPlayerEnabled = playbackSettings.externalPlayerEnabled

    val handleDirectionalMove: (FocusDirection) -> Boolean = { direction ->
        when (direction) {
            FocusDirection.Up -> {
                onRequestEpisodeFocus()
                true
            }
            FocusDirection.Down -> onRequestNextSectionFocus()
            else -> false
        }
    }

    val selectedVideoStream = playbackOptions.selectedVideoStream
    val videoCodecText = selectedVideoStream?.codec?.let(::formatVideoCodec)
    val videoResolutionWidth = playbackItemForActions
        ?.getPrimaryMediaSource()
        ?.defaultVideoStream
        ?.width
        ?: selectedVideoStream?.width
    val resolutionLabel = videoResolutionWidth?.let { width ->
        when {
            width < 1920 -> "720p"
            width < 3840 -> "FHD"
            else -> "4K"
        }
    }

    val playEpisode = remember(
        playbackState,
        episode,
        resumePositionTicks,
        shouldResume,
        externalPlayerEnabled,
        authState.authSession
    ) {
        {
            if (shouldResume) {
                videoPlaybackViewModel.selectResumePlayback(resumePositionTicks)
            } else {
                videoPlaybackViewModel.selectStartFromBeginning()
            }
            val startPositionMs = if (shouldResume) resumePositionTicks / 10_000 else 0L
            val playbackInfo = buildEpisodePlaybackInfo(episode, startPositionMs)
            if (externalPlayerEnabled) {
                val session = authState.authSession
                if (session == null) {
                    context.showToast("Sign in required for playback")
                } else {
                    val serverUrl = session.server?.url.orEmpty()
                    if (serverUrl.isBlank() || session.accessToken.isBlank()) {
                        context.showToast("Unable to resolve server connection")
                    } else {
                        val startTicks = if (shouldResume && resumePositionTicks > 0L) resumePositionTicks else null
                        val mediaUrl = playbackInfo.mediaItem.getPlaybackUrl(
                            serverUrl = serverUrl,
                            accessToken = session.accessToken,
                            mediaSourceId = playbackInfo.mediaSourceId,
                            audioStreamIndex = playbackInfo.audioStreamIndex,
                            subtitleStreamIndex = playbackInfo.subtitleStreamIndex,
                            startTimeTicks = startTicks,
                            maxBitrate = playbackInfo.maxBitrate
                        )
                        context.launchExternalVideoPlayer(mediaUrl, playbackInfo.mediaItem.name)
                    }
                }
            } else {
                onPlayClick(playbackInfo)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(calculateRoundedValue(8).sdp)
            .heightIn(min = calculateRoundedValue(72).sdp),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(14).sdp)
    ) {
        val chipSpacing = calculateRoundedValue(14).sdp
        val playInteractionSource = remember { MutableInteractionSource() }
        val versionInteractionSource = remember { MutableInteractionSource() }
        val audioInteractionSource = remember { MutableInteractionSource() }
        val subtitleInteractionSource = remember { MutableInteractionSource() }

        val versionFocusRequester = remember { FocusRequester() }
        val audioFocusRequester = remember { FocusRequester() }
        val subtitleFocusRequester = remember { FocusRequester() }

        val isPlayFocused by playInteractionSource.collectIsFocusedAsState()
        val isVersionFocused by versionInteractionSource.collectIsFocusedAsState()
        val isAudioFocused by audioInteractionSource.collectIsFocusedAsState()
        val isSubtitleFocused by subtitleInteractionSource.collectIsFocusedAsState()

        val playButtonSize = calculateRoundedValue(64).sdp
        val haloPadding = calculateRoundedValue(6).sdp
        val haloContainerSize = playButtonSize + haloPadding * 2
        val haloProgress by animateFloatAsState(
            targetValue = if (isPlayFocused) 1f else 0f,
            animationSpec = tween(durationMillis = 100),
            label = "playHaloProgress"
        )
        val haloRadius by animateDpAsState(
            targetValue = if (isPlayFocused) haloContainerSize * 0.95f else playButtonSize * 0.65f,
            animationSpec = tween(durationMillis = 120),
            label = "playHaloRadius"
        )
        val haloColor by animateColorAsState(
            targetValue = if (isPlayFocused) lerp(buttonColor, Color.White, 0.35f) else buttonColor.copy(alpha = 0.25f),
            animationSpec = tween(durationMillis = 100),
            label = "playHaloColor"
        )
        val haloScale by animateFloatAsState(
            targetValue = if (isPlayFocused) 1.05f else 1f,
            animationSpec = tween(durationMillis = 100),
            label = "playHaloScale"
        )
        val haloBorderWidth by animateDpAsState(
            targetValue = if (isPlayFocused) calculateRoundedValue(3).sdp else 0.dp,
            animationSpec = tween(durationMillis = 100),
            label = "playHaloBorderWidth"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = calculateRoundedValue(64).sdp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(10).sdp)
            ) {
                Box(
                    modifier = Modifier
                        .size(haloContainerSize)
                        .graphicsLayer {
                            val scale = 1f + (haloScale - 1f) * haloProgress
                            scaleX = scale
                            scaleY = scale
                        }
                        .drawBehind {
                            if (haloProgress > 0f) {
                                val radius = haloRadius.toPx().coerceAtLeast(size.minDimension / 2f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            haloColor.copy(alpha = 0.55f * haloProgress),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = radius
                                    )
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (haloBorderWidth > 0.dp && haloProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(haloBorderWidth / 2)
                                .border(
                                    width = haloBorderWidth,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.9f * haloProgress),
                                            lerp(buttonColor, Color.White, 0.6f).copy(alpha = haloProgress)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .graphicsLayer { alpha = haloProgress }
                        )
                    }

                    DynamicPlayButton(
                        modifier = Modifier
                            .size(playButtonSize)
                            .dpadNavigation(
                                focusRequester = focusRequester,
                                onClick = playEpisode,
                                onMoveFocus = handleDirectionalMove,
                                interactionSource = playInteractionSource,
                                applyClickModifier = false,
                                showFocusOutline = false
                            ),
                        onClick = playEpisode,
                        dominantColor = buttonColor,
                        buttonSize = playButtonSize,
                        iconSize = playButtonSize / 2,
                        mediaItem = playbackItemForActions,
                        hasResumableProgress = hasResumableProgress
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                ) {
                    EpisodeInfoChip(
                        label = "Resolution",
                        value = resolutionLabel ?: "Unknown",
                        accentColor = buttonColor
                    )
                    EpisodeInfoChip(
                        label = null,
                        value = playbackState.currentVideoRangeText,
                        accentColor = buttonColor
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(chipSpacing * 1.2f)
            ) {
                EpisodeInfoChip(
                    label = "Version",
                    value = playbackState.currentVersionText,
                    onClick = { videoPlaybackViewModel.showVersionDialog() },
                    interactionSource = versionInteractionSource,
                    focusRequester = versionFocusRequester,
                    accentColor = buttonColor,
                    onMoveFocus = { direction ->
                        when (direction) {
                            FocusDirection.Left -> {
                                focusRequester.requestFocus()
                                true
                            }
                            FocusDirection.Right -> {
                                audioFocusRequester.requestFocus()
                                true
                            }
                            else -> handleDirectionalMove(direction)
                        }
                    }
                )
                EpisodeInfoChip(
                    label = "Audio",
                    value = playbackState.currentAudioText,
                    onClick = { videoPlaybackViewModel.showAudioDialog() },
                    interactionSource = audioInteractionSource,
                    focusRequester = audioFocusRequester,
                    accentColor = buttonColor,
                    onMoveFocus = { direction ->
                        when (direction) {
                            FocusDirection.Left -> {
                                versionFocusRequester.requestFocus()
                                true
                            }
                            FocusDirection.Right -> {
                                subtitleFocusRequester.requestFocus()
                                true
                            }
                            else -> handleDirectionalMove(direction)
                        }
                    }
                )
                EpisodeInfoChip(
                    label = "Subtitles",
                    value = playbackState.currentSubtitleText,
                    onClick = { videoPlaybackViewModel.showSubtitleDialog() },
                    interactionSource = subtitleInteractionSource,
                    focusRequester = subtitleFocusRequester,
                    accentColor = buttonColor,
                    onMoveFocus = { direction ->
                        when (direction) {
                            FocusDirection.Left -> {
                                audioFocusRequester.requestFocus()
                                true
                            }
                            else -> handleDirectionalMove(direction)
                        }
                    }
                )
                EpisodeInfoChip(
                    label = "Codec",
                    value = videoCodecText ?: "Unknown",
                    accentColor = buttonColor
                )
            }

            LaunchedEffect(
                isPlayFocused,
                isVersionFocused,
                isAudioFocused,
                isSubtitleFocused
            ) {
                onFocusChanged(
                    isPlayFocused || isVersionFocused || isAudioFocused || isSubtitleFocused
                )
            }
        }
    }

    if (playbackState.showVersionDialog) {
        VersionSelectionDialog(
            versions = playbackState.availableVersions,
            selectedVersion = playbackOptions.selectedMediaSource,
            onVersionSelected = { videoPlaybackViewModel.selectVersion(it) },
            onDismiss = { videoPlaybackViewModel.hideVersionDialog() }
        )
    }

    if (playbackState.showAudioDialog) {
        AudioTrackDialog(
            audioStreams = playbackState.availableAudioStreams,
            selectedAudioStream = playbackOptions.selectedAudioStream,
            onAudioSelected = { videoPlaybackViewModel.selectAudioStream(it) },
            onDismiss = { videoPlaybackViewModel.hideAudioDialog() }
        )
    }

    if (playbackState.showSubtitleDialog) {
        SubtitleDialog(
            subtitleStreams = playbackState.availableSubtitleStreams,
            selectedSubtitleStream = playbackOptions.selectedSubtitleStream,
            onSubtitleSelected = { videoPlaybackViewModel.selectSubtitleStream(it) },
            onDismiss = { videoPlaybackViewModel.hideSubtitleDialog() }
        )
    }

    if (playbackState.showVideoQualityDialog) {
        VideoQualityDialog(
            selectedOption = playbackState.playbackTranscodeOption,
            onSelect = { videoPlaybackViewModel.selectTranscodeOption(it) },
            onDismiss = { videoPlaybackViewModel.hideVideoQualityDialog() }
        )
    }
}

@Composable
fun EpisodeInfoChip(
    label: String?,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    onMoveFocus: ((FocusDirection) -> Boolean)? = null,
    focusRequester: FocusRequester? = null,
    accentColor: Color? = null,
) {
    val shape = RoundedCornerShape(calculateRoundedValue(14).sdp)
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val focusAccentColor = accentColor ?: Minsk

    val chipModifier = if (onClick != null) {
        modifier
            .dpadNavigation(
                shape = shape,
                focusRequester = focusRequester,
                onClick = onClick,
                onMoveFocus = onMoveFocus,
                interactionSource = resolvedInteractionSource,
                applyClickModifier = false,
                showFocusOutline = false
            )
    } else {
        modifier
    }

    val isFocused by resolvedInteractionSource.collectIsFocusedAsState()
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.048f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "chipScale"
    )
    val focusedContainerColor = if (isFocused) {
        if (accentColor != null) {
            
            lerp(focusAccentColor, Color.White, 0.2f).copy(alpha = 0.5f)
        } else {
            Color.White.copy(alpha = 0.2f)
        }
    } else {
        Color.White.copy(alpha = 0.1f)
    }
    val containerColor by animateColorAsState(
        targetValue = focusedContainerColor,
        animationSpec = tween(durationMillis = 80),
        label = "chipContainerColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color.White.copy(alpha = 0.9f),
        animationSpec = tween(durationMillis = 80),
        label = "chipTextColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) calculateRoundedValue(2).sdp else calculateRoundedValue(1).sdp,
        animationSpec = tween(durationMillis = 80),
        label = "chipBorderWidth"
    )
    val borderBrush: Brush = if (isFocused) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                focusAccentColor.copy(alpha = 0.9f)
            )
        )
    } else {
        SolidColor(Color.White.copy(alpha = 0.2f))
    }

    val surfaceModifier = chipModifier.graphicsLayer {
        scaleX = focusScale
        scaleY = focusScale
    }

    val chipContent = @Composable {
        val textContent = label?.let { "$it: $value" } ?: value
        Text(
            text = textContent,
            modifier = Modifier.padding(
                horizontal = calculateRoundedValue(12).sdp,
                vertical = calculateRoundedValue(5).sdp
            ),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontSize = calculateRoundedValue(12).ssp
        )
    }

    if (onClick != null) {
        Surface(
            modifier = surfaceModifier,
            shape = shape,
            onClick = onClick,
            interactionSource = resolvedInteractionSource,
            border = BorderStroke(borderWidth, borderBrush),
            color = containerColor,
        ) {
            chipContent()
        }
    } else {
        Surface(
            modifier = surfaceModifier,
            shape = shape,
            border = BorderStroke(borderWidth, borderBrush),
            color = containerColor,
        ) {
            chipContent()
        }
    }
}

private fun formatVideoCodec(codec: String): String {
    val normalized = codec.trim().lowercase()
    return when {
        normalized.startsWith("h264") || normalized == "avc" -> "H.264"
        normalized.startsWith("h265") ||
            normalized == "hevc" ||
            normalized == "hev1" ||
            normalized == "hvc1" -> "HEVC"
        normalized.startsWith("av1") -> "AV1"
        normalized.startsWith("vp9") -> "VP9"
        normalized.startsWith("vp8") -> "VP8"
        normalized.isNotEmpty() -> normalized.uppercase()
        else -> "Unknown"
    }
}
