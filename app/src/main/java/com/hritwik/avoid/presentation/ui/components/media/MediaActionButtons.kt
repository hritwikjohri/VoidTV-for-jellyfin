package com.hritwik.avoid.presentation.ui.components.media

import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.PlaybackOptions
import com.hritwik.avoid.domain.model.playback.PlaybackInfo
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.presentation.ui.components.dialogs.AudioTrackDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.SubtitleDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.VersionSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.VideoQualityDialog
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.constants.AppConstants.RESUME_THRESHOLD
import com.hritwik.avoid.utils.extensions.getBackdropUrl
import com.hritwik.avoid.utils.extensions.getPosterUrl
import com.hritwik.avoid.utils.extensions.getPlaybackUrl
import com.hritwik.avoid.utils.extensions.resolveSubtitleOffIndex
import com.hritwik.avoid.utils.extensions.launchExternalVideoPlayer
import com.hritwik.avoid.utils.extensions.showToast
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import ir.kaaveh.sdpcompose.ssp

@OptIn(UnstableApi::class)
@Composable
fun MediaActionButtons(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    serverUrl: String,
    playbackItem: MediaItem? = null,
    onPlayClick: (PlaybackInfo) -> Unit,
    playButtonSize: Int = 48,
    showFavIcon: Boolean = true,
    playButtonFocusRequester: FocusRequester? = null,
    authViewModel: AuthServerViewModel = hiltViewModel(),
    userDataViewModel: UserDataViewModel = hiltViewModel(),
    videoPlaybackViewModel: VideoPlaybackViewModel = hiltViewModel()
) {
    val playbackState by videoPlaybackViewModel.state.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val videoOptionsState by videoPlaybackViewModel.state.collectAsStateWithLifecycle()
    val playbackSettings by userDataViewModel.playbackSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val playbackTarget = playbackItem ?: mediaItem.takeIf { !mediaItem.isFolder }
    var showVersionDialog by remember { mutableStateOf(false) }
    var buttonColor by remember { mutableStateOf(Color(0xFF1976D2)) }
    val isFavorite by userDataViewModel.isFavorite(mediaItem.id).collectAsStateWithLifecycle()
    val dynamicButtonSize = playButtonSize.dp

    val resolveSubtitleIndexForPlayback: (PlaybackOptions) -> Int? = { options ->
        val referenceItem = videoOptionsState.mediaItem ?: playbackTarget
        if (referenceItem == null) {
            null
        } else {
            val source = options.selectedMediaSource
            val offIndex = referenceItem.resolveSubtitleOffIndex(
                source?.id ?: playbackState.mediaSourceId,
                source?.audioStreams ?: playbackState.availableAudioStreams,
                source?.subtitleStreams ?: playbackState.availableSubtitleStreams
            )
            options.selectedSubtitleStream?.index
                ?: offIndex?.takeIf { options.selectedSubtitleStream == null }
        }
    }

    LaunchedEffect(playbackTarget?.id, authState.authSession) {
        val session = authState.authSession ?: return@LaunchedEffect
        val target = playbackTarget ?: return@LaunchedEffect
        videoPlaybackViewModel.initializeVideoOptions(
            mediaItem = target,
            userId = session.userId.id,
            accessToken = session.accessToken
        )
    }

    LaunchedEffect(mediaItem.id, serverUrl) {
        val backdropUrl = mediaItem.backdropImageTags.firstOrNull()?.let {
            mediaItem.getBackdropUrl(serverUrl)
        }
        val posterUrl = mediaItem.primaryImageTag?.let { mediaItem.getPosterUrl(serverUrl) }
        val color = extractDominantColor(context, backdropUrl ?: posterUrl)
        if (color != null) {
            buttonColor = color
        }
    }

    val resolvedPlaybackItem = videoOptionsState.mediaItem ?: playbackTarget
    val playbackPositionTicks = resolvedPlaybackItem?.userData?.playbackPositionTicks ?: 0L
    val runTimeTicks = resolvedPlaybackItem?.runTimeTicks
    val hasResumableProgress = playbackPositionTicks > 0 && (runTimeTicks == null || playbackPositionTicks < runTimeTicks - RESUME_THRESHOLD)
    val playbackItemForActions = resolvedPlaybackItem
    val playbackOptions = playbackState.playbackOptions
    val resumePositionTicks = when {
        playbackOptions.resumePositionTicks > 0L -> playbackOptions.resumePositionTicks
        hasResumableProgress -> playbackItemForActions?.userData?.playbackPositionTicks ?: 0L
        playbackState.startPositionMs > 0L -> playbackState.startPositionMs * 10_000
        else -> 0L
    }
    val shouldResume = hasResumableProgress && resumePositionTicks > 0L && !playbackOptions.startFromBeginning

    val selectedVideoStream = playbackOptions.selectedVideoStream
    val videoResolutionWidth = playbackItemForActions
        ?.mediaSources
        ?.firstOrNull()
        ?.videoStreams
        ?.firstOrNull()
        ?.width
        ?: selectedVideoStream?.width
    val resolutionLabel = videoResolutionWidth?.let { width ->
        when {
            width < 1920 -> "720p"
            width < 3840 -> "FHD"
            else -> "4K"
        }
    }

    val externalPlayerEnabled = playbackSettings.externalPlayerEnabled

    val playMovie: () -> Unit = {
        playbackItemForActions?.let { itemForPlayback ->
            if (shouldResume) {
                videoPlaybackViewModel.selectResumePlayback(resumePositionTicks)
            } else {
                videoPlaybackViewModel.selectStartFromBeginning()
            }

            val latestOptions = videoPlaybackViewModel.state.value.playbackOptions
            val playbackInfo = PlaybackInfo(
                mediaItem = itemForPlayback,
                mediaSourceId = latestOptions.selectedMediaSource?.id,
                audioStreamIndex = latestOptions.selectedAudioStream?.index,
                subtitleStreamIndex = resolveSubtitleIndexForPlayback(latestOptions),
                startPosition = if (shouldResume) resumePositionTicks / 10_000 else 0L,
                maxBitrate = latestOptions.selectedMediaSource?.bitrate
            )
            if (externalPlayerEnabled) {
                val session = authState.authSession
                if (session == null) {
                    context.showToast("Sign in required for playback")
                    return@let
                }
                val serverUrl = session.server?.url.orEmpty()
                if (serverUrl.isBlank() || session.accessToken.isBlank()) {
                    context.showToast("Unable to resolve server connection")
                    return@let
                }
                val startTicks = if (shouldResume && resumePositionTicks > 0L) resumePositionTicks else null
                val mediaUrl = itemForPlayback.getPlaybackUrl(
                    serverUrl = serverUrl,
                    accessToken = session.accessToken,
                    mediaSourceId = playbackInfo.mediaSourceId,
                    audioStreamIndex = playbackInfo.audioStreamIndex,
                    subtitleStreamIndex = playbackInfo.subtitleStreamIndex,
                    startTimeTicks = startTicks,
                    maxBitrate = playbackInfo.maxBitrate
                )
                context.launchExternalVideoPlayer(mediaUrl, itemForPlayback.name)
                return@let
            }
            onPlayClick(playbackInfo)
        }
    }

    val playFocusRequester = playButtonFocusRequester ?: remember { FocusRequester() }
    val versionFocusRequester = remember { FocusRequester() }
    val audioFocusRequester = remember { FocusRequester() }
    val subtitleFocusRequester = remember { FocusRequester() }
    val favoriteFocusRequester = remember(showFavIcon) { if (showFavIcon) FocusRequester() else null }

    val versionInteractionSource = remember { MutableInteractionSource() }
    val audioInteractionSource = remember { MutableInteractionSource() }
    val subtitleInteractionSource = remember { MutableInteractionSource() }
    val favoriteInteractionSource = remember { MutableInteractionSource() }
    val videoCodecText = playbackOptions.selectedVideoStream?.codec
        ?.takeIf { it.isNotBlank() }
        ?.trim()
        ?: "Unknown"
    val versionText = playbackState.currentVersionText.ifBlank { "Auto" }
    val audioText = playbackState.currentAudioText.ifBlank { "Default" }
    val subtitleText = playbackState.currentSubtitleText.ifBlank { "Off" }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(calculateRoundedValue(24).sdp),
        tonalElevation = calculateRoundedValue(6).sdp,
        color = Color.Transparent
    ) {
        Column (
            modifier = Modifier.fillMaxWidth().padding(calculateRoundedValue(8).sdp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            val playInteractionSource = remember { MutableInteractionSource() }
            val isPlayFocused by playInteractionSource.collectIsFocusedAsState()
            val playButtonSize = dynamicButtonSize
            val haloPadding = calculateRoundedValue(6).sdp
            val haloContainerSize = playButtonSize + haloPadding * 2
            val haloProgress by animateFloatAsState(
                targetValue = if (isPlayFocused) 1f else 0f,
                animationSpec = tween(durationMillis = 220),
                label = "moviePlayHaloProgress"
            )
            val haloScale by animateFloatAsState(
                targetValue = if (isPlayFocused) 1.05f else 1f,
                animationSpec = tween(durationMillis = 280),
                label = "moviePlayHaloScale"
            )
            val haloBorderWidth by animateDpAsState(
                targetValue = if (isPlayFocused) calculateRoundedValue(3).sdp else 0.dp,
                animationSpec = tween(durationMillis = 260),
                label = "moviePlayHaloBorderWidth"
            )

            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ){
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(14).sdp)
                ){
                    Box(
                        modifier = Modifier
                            .size(haloContainerSize)
                            .graphicsLayer {
                                val scale = 1f + (haloScale - 1f) * haloProgress
                                scaleX = scale
                                scaleY = scale
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (haloBorderWidth > 0.dp && haloProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .size(playButtonSize + haloBorderWidth * 2)
                                    .border(
                                        width = haloBorderWidth,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.9f * haloProgress),
                                                lerp(buttonColor, Color.White, 0.6f).copy(alpha = haloProgress)
                                            ),
                                            start = Offset(0f, 0f),
                                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                        ),
                                        shape = CircleShape
                                    )
                                    .graphicsLayer { alpha = haloProgress },
                                contentAlignment = Alignment.Center
                            ) {}
                        }

                        DynamicPlayButton(
                            modifier = Modifier
                                .size(playButtonSize)
                                .dpadNavigation(
                                    focusRequester = playFocusRequester,
                                    interactionSource = playInteractionSource,
                                    onClick = playMovie,
                                    onMoveFocus = { direction ->
                                        when (direction) {
                                            FocusDirection.Right -> {
                                                favoriteFocusRequester?.requestFocus()
                                                true
                                            }
                                            else -> false
                                        }
                                    },
                                    applyClickModifier = false,
                                    showFocusOutline = false
                                ),
                            onClick = playMovie,
                            dominantColor = buttonColor,
                            buttonSize = playButtonSize,
                            iconSize = (playButtonSize.value / 2).dp,
                            mediaItem = playbackItemForActions,
                            hasResumableProgress = hasResumableProgress,
                            focusRequester = playFocusRequester
                        )
                    }

                    if (showFavIcon) {
                        val favoriteRequester = favoriteFocusRequester
                        if (favoriteRequester != null) {
                            Box(
                                modifier = Modifier.padding(start = calculateRoundedValue(16).sdp)
                            ) {
                                MediaFavoriteChip(
                                    modifier = Modifier.focusRequester(favoriteRequester),
                                    isFavorite = isFavorite,
                                    accentColor = buttonColor,
                                    interactionSource = favoriteInteractionSource,
                                    onToggle = {
                                        authState.authSession?.let { session ->
                                            userDataViewModel.toggleFavorite(
                                                userId = session.userId.id,
                                                mediaId = mediaItem.id,
                                                accessToken = session.accessToken,
                                                isFavorite = isFavorite,
                                                mediaItem = mediaItem
                                            )
                                        }
                                    },
                                    onMoveFocus = { direction ->
                                        when (direction) {
                                            FocusDirection.Left -> {
                                                playFocusRequester.requestFocus()
                                                true
                                            }
                                            FocusDirection.Right -> {
                                                versionFocusRequester.requestFocus()
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.padding(start = calculateRoundedValue(16).sdp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(10).sdp)
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

                Spacer(modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(14).sdp)
                ) {
                    EpisodeInfoChip(
                        label = "Version",
                        value = versionText,
                        focusRequester = versionFocusRequester,
                        onClick = { videoPlaybackViewModel.showVersionDialog() },
                        interactionSource = versionInteractionSource,
                        accentColor = buttonColor,
                        onMoveFocus = { direction ->
                            when (direction) {
                                FocusDirection.Left -> {
                                    favoriteFocusRequester?.requestFocus()
                                    true
                                }

                                FocusDirection.Right -> {
                                    audioFocusRequester.requestFocus()
                                    true
                                }

                                else -> false
                            }
                        }
                    )

                    EpisodeInfoChip(
                        label = "Audio",
                        value = audioText,
                        focusRequester = audioFocusRequester,
                        onClick = { videoPlaybackViewModel.showAudioDialog() },
                        interactionSource = audioInteractionSource,
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

                                else -> false
                            }
                        }
                    )

                    EpisodeInfoChip(
                        label = "Subtitles",
                        value = subtitleText,
                        focusRequester = subtitleFocusRequester,
                        onClick = { videoPlaybackViewModel.showSubtitleDialog() },
                        interactionSource = subtitleInteractionSource,
                        accentColor = buttonColor,
                        onMoveFocus = { direction ->
                            when (direction) {
                                FocusDirection.Left -> {
                                    audioFocusRequester.requestFocus()
                                    true
                                }
                                else -> false
                            }
                        }
                    )

                    EpisodeInfoChip(
                        label = null,
                        value = videoCodecText,
                        accentColor = buttonColor
                    )
                }
            }
        }
    }

    if (showVersionDialog) {
        VersionSelectionDialog(
            versions = mediaItem.mediaSources,
            selectedVersion = playbackState.playbackOptions.selectedMediaSource,
            onVersionSelected = {
                showVersionDialog = false
            },
            onDismiss = { showVersionDialog = false }
        )
    }

    if (playbackState.showVersionDialog) {
        VersionSelectionDialog(
            versions = playbackState.availableVersions,
            selectedVersion = playbackState.playbackOptions.selectedMediaSource,
            onVersionSelected = { videoPlaybackViewModel.selectVersion(it) },
            onDismiss = { videoPlaybackViewModel.hideVersionDialog() }
        )
    }

    if (playbackState.showAudioDialog) {
        AudioTrackDialog(
            audioStreams = playbackState.availableAudioStreams,
            selectedAudioStream = playbackState.playbackOptions.selectedAudioStream,
            onAudioSelected = { videoPlaybackViewModel.selectAudioStream(it) },
            onDismiss = { videoPlaybackViewModel.hideAudioDialog() }
        )
    }

    if (playbackState.showSubtitleDialog) {
        SubtitleDialog(
            subtitleStreams = playbackState.availableSubtitleStreams,
            selectedSubtitleStream = playbackState.playbackOptions.selectedSubtitleStream,
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
private fun MediaFavoriteChip(
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    accentColor: Color,
    interactionSource: MutableInteractionSource,
    onToggle: () -> Unit,
    onMoveFocus: ((FocusDirection) -> Boolean)?,
) {
    val shape = RoundedCornerShape(calculateRoundedValue(12).sdp)
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "favoriteChipScale"
    )
    val containerStartColor by animateColorAsState(
        targetValue = if (isFocused || isFavorite) {
            lerp(accentColor, Color.White, 0.35f).copy(alpha = 0.6f)
        } else {
            Color.White.copy(alpha = 0.12f)
        },
        animationSpec = tween(durationMillis = 240),
        label = "favoriteChipStartColor"
    )
    val containerEndColor by animateColorAsState(
        targetValue = if (isFocused || isFavorite) {
            Color.White.copy(alpha = 0.22f)
        } else {
            Color.White.copy(alpha = 0.05f)
        },
        animationSpec = tween(durationMillis = 240),
        label = "favoriteChipEndColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) calculateRoundedValue(2).sdp else calculateRoundedValue(1).sdp,
        animationSpec = tween(durationMillis = 220),
        label = "favoriteChipBorder"
    )
    val glowElevation by animateDpAsState(
        targetValue = if (isFocused) calculateRoundedValue(6).sdp else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "favoriteChipGlow"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isFocused || isFavorite) Color.White else Color.White.copy(alpha = 0.85f),
        animationSpec = tween(durationMillis = 200),
        label = "favoriteChipLabelColor"
    )
    val borderBrush: Brush = if (isFocused) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                accentColor.copy(alpha = 0.8f)
            )
        )
    } else {
        SolidColor(Color.White.copy(alpha = 0.2f))
    }

    val statusText = if (isFavorite) "In My List" else "Add to List"
    val statusIcon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder

    Surface(
        modifier = modifier
            .dpadNavigation(
                shape = shape,
                onClick = onToggle,
                onMoveFocus = onMoveFocus,
                interactionSource = interactionSource,
                applyClickModifier = false,
                showFocusOutline = false
            )
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            },
        shape = shape,
        onClick = onToggle,
        interactionSource = interactionSource,
        border = BorderStroke(borderWidth, borderBrush),
        color = Color.Transparent,
        shadowElevation = glowElevation
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(containerStartColor, containerEndColor)
                    ),
                    shape = shape
                )
                .padding(
                    horizontal = calculateRoundedValue(12).sdp,
                    vertical = calculateRoundedValue(5).sdp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = statusText,
                tint = labelColor,
                modifier = Modifier.size(calculateRoundedValue(14).sdp)
            )
            Text(
                text = statusText,
                color = labelColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                fontSize = calculateRoundedValue(10).ssp
            )
        }
    }
}
