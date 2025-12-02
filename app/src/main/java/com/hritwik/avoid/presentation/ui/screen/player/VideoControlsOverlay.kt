package com.hritwik.avoid.presentation.ui.screen.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.ui.theme.SeekBarBackground
import com.hritwik.avoid.presentation.ui.theme.VoidBrightAzure
import com.hritwik.avoid.utils.extensions.formatTime
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

@Composable
fun VideoControlsOverlay(
    mediaTitle: String,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    isPlaying: Boolean,
    isBuffering: Boolean,
    duration: Long,
    currentPosition: Long,
    isVisible: Boolean = true,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekComplete: (Long) -> Unit,
    onBackClick: () -> Unit,
    onDismissControls: () -> Unit,
    onShowControls: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    skipButtonVisible: Boolean = false,
    skipButtonLabel: String = "",
    onSkipButtonClick: (() -> Unit)? = null,
    floatingSkipButtonVisible: Boolean = false,
    onFloatingSkipButtonClick: (() -> Unit)? = null,
    autoSkipProgress: Float = 0f,
    isAutoSkipActive: Boolean = false,
    onCancelAutoSkip: (() -> Unit)? = null,
    onPlayPrevious: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAudioClick: (() -> Unit)? = null,
    onSubtitleClick: (() -> Unit)? = null,
    onSubtitleOptionsClick: (() -> Unit)? = null,
    videoQualityLabel: String? = null,
    onVideoClick: (() -> Unit)? = null,
) {
    val overlayFocusRequester = remember { FocusRequester() }
    val previousEpisodeFocusRequester = remember { FocusRequester() }
    val skipBackwardFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val skipForwardFocusRequester = remember { FocusRequester() }
    val nextEpisodeFocusRequester = remember { FocusRequester() }
    val skipButtonFocusRequester = remember { FocusRequester() }
    val floatingSkipButtonFocusRequester = remember { FocusRequester() }
    val audioFocusRequester = remember { FocusRequester() }
    val subtitleFocusRequester = remember { FocusRequester() }
    val subtitleOptionsFocusRequester = remember { FocusRequester() }
    val videoFocusRequester = remember { FocusRequester() }
    val progressBarFocusRequester = remember { FocusRequester() }

    val focusShadowElevation = calculateRoundedValue(12).sdp
    val focusHighlightColor = Color(0xCC8F8A7F)
    val pillShape = RoundedCornerShape(percent = 50)

    fun Modifier.focusedShadow(isFocused: Boolean): Modifier =
        if (isFocused) {
            this
                .shadow(focusShadowElevation, CircleShape, clip = false)
                .drawBehind {
                    val radius = size.minDimension / 2f * 0.8f
                    drawCircle(color = focusHighlightColor, radius = radius, center = center)
                }
        } else {
            this
        }

    fun Modifier.focusedPill(isFocused: Boolean): Modifier =
        if (isFocused) {
            this
                .shadow(focusShadowElevation, pillShape, clip = false)
                .background(focusHighlightColor, pillShape)
        } else {
            this
        }

    var previousFocused by remember { mutableStateOf(false) }
    var skipBackwardFocused by remember { mutableStateOf(false) }
    var playPauseFocused by remember { mutableStateOf(false) }
    var skipForwardFocused by remember { mutableStateOf(false) }
    var nextFocused by remember { mutableStateOf(false) }
    var audioFocused by remember { mutableStateOf(false) }
    var subtitleFocused by remember { mutableStateOf(false) }
    var subtitleOptionsFocused by remember { mutableStateOf(false) }
    var videoFocused by remember { mutableStateOf(false) }

    var focusArea by remember { mutableStateOf(VideoControlsFocusArea.CONTROLS) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var isSeeking by remember { mutableStateOf(false) }
    var seekBasePosition by remember { mutableLongStateOf(0L) }
    var seekTargetPosition by remember { mutableLongStateOf(0L) }
    var continuousSeekDirection by remember { mutableIntStateOf(0) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var hiddenHoldDirection by remember { mutableIntStateOf(0) } 
    var hiddenHoldActive by remember { mutableStateOf(false) }
    var isProgressPreviewVisible by remember { mutableStateOf(false) }
    var floatingButtonVisible by remember { mutableStateOf(false) }
    var floatingButtonStartTime by remember { mutableLongStateOf(0L) }
    var floatingSkipButtonFocused by remember { mutableStateOf(false) }
    var hiddenSkipFeedbackDirection by remember { mutableIntStateOf(0) } 
    var hiddenSkipFeedbackVisible by remember { mutableStateOf(false) }
    var hiddenSkipFeedbackTimestamp by remember { mutableLongStateOf(0L) }

    val formattedTitle = remember(mediaTitle, seasonNumber, episodeNumber) {
        val season = seasonNumber?.takeIf { it > 0 }
        val episode = episodeNumber?.takeIf { it > 0 }

        if (season != null && episode != null) {
            "S${season.toString().padStart(2, '0')}:E${episode.toString().padStart(2, '0')} $mediaTitle"
        } else {
            mediaTitle
        }
    }

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000)
        }
    }

    LaunchedEffect(lastInteractionTime, isVisible) {
        if (isVisible) {
            while (true) {
                delay(1000)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastInteractionTime >= 5000) {
                    onDismissControls()
                    break
                }
            }
        }
    }

    fun updateInteractionTime(shouldShowControls: Boolean = true) {
        lastInteractionTime = System.currentTimeMillis()
        if (!isVisible && shouldShowControls) {
            onShowControls()
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(100)
            playPauseFocusRequester.requestFocus()
            lastInteractionTime = System.currentTimeMillis()
            isProgressPreviewVisible = false
            hiddenSkipFeedbackVisible = false
        } else {
            delay(200)
            if (floatingButtonVisible && floatingSkipButtonVisible) {
                floatingSkipButtonFocusRequester.requestFocus()
            } else {
                overlayFocusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(hiddenHoldDirection) {
        if (hiddenHoldDirection != 0) {
            val dir = hiddenHoldDirection
            isProgressPreviewVisible = true
            updateInteractionTime(shouldShowControls = false)
            delay(200)
            if (hiddenHoldDirection == dir) {
                focusArea = VideoControlsFocusArea.ROOT
                continuousSeekDirection = dir
                hiddenHoldActive = true
            }
        } else {
            hiddenHoldActive = false
            isProgressPreviewVisible = false
            if (!isVisible) {
                overlayFocusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(hiddenSkipFeedbackTimestamp) {
        if (hiddenSkipFeedbackTimestamp == 0L) return@LaunchedEffect
        val currentTimestamp = hiddenSkipFeedbackTimestamp
        hiddenSkipFeedbackVisible = true
        delay(1200)
        if (hiddenSkipFeedbackTimestamp == currentTimestamp) {
            hiddenSkipFeedbackVisible = false
        }
    }

    LaunchedEffect(floatingSkipButtonVisible, isVisible) {
        if (floatingSkipButtonVisible) {
            if (!isVisible) {
                floatingButtonVisible = true
                floatingButtonStartTime = System.currentTimeMillis()
            }
        } else {
            floatingButtonVisible = false
        }
    }

    LaunchedEffect(floatingButtonVisible, floatingButtonStartTime, isAutoSkipActive) {
        if (floatingButtonVisible && !isVisible && !isAutoSkipActive) {
            val startTime = floatingButtonStartTime
            delay(5000)
            if (floatingButtonStartTime == startTime) {
                floatingButtonVisible = false
            }
        }
    }

    LaunchedEffect(floatingButtonVisible) {
        if (floatingButtonVisible && !isVisible && floatingSkipButtonVisible) {
            delay(200)
            floatingSkipButtonFocusRequester.requestFocus()
        } else if (!floatingButtonVisible && !isVisible) {
            delay(100)
            overlayFocusRequester.requestFocus()
        }
    }

    fun triggerHiddenSkipFeedback(direction: Int) {
        hiddenSkipFeedbackDirection = direction
        hiddenSkipFeedbackTimestamp = System.currentTimeMillis()
    }

    LaunchedEffect(continuousSeekDirection) {
        if (
            continuousSeekDirection != 0 &&
            (focusArea == VideoControlsFocusArea.PROGRESS_BAR || focusArea == VideoControlsFocusArea.ROOT)
        ) {
            while (continuousSeekDirection != 0) {
                val deltaSeconds = if (continuousSeekDirection > 0) 10L else -10L
                val tentative = seekTargetPosition + deltaSeconds
                val clamped = when {
                    tentative < 0L -> 0L
                    duration > 0 -> tentative.coerceAtMost(duration)
                    else -> tentative
                }
                seekTargetPosition = clamped
                onSeek(clamped)
                if (!isVisible) {
                    triggerHiddenSkipFeedback(continuousSeekDirection)
                }
                lastSeekTime = System.currentTimeMillis()
                updateInteractionTime(shouldShowControls = !isProgressPreviewVisible)
                delay(200)
            }
        }
    }

    LaunchedEffect(isSeeking, seekTargetPosition) {
        if (isSeeking) {
            lastSeekTime = System.currentTimeMillis()
            delay(1000)
            if (isSeeking && continuousSeekDirection == 0 && System.currentTimeMillis() - lastSeekTime >= 1000) {
                onSeekComplete(seekTargetPosition)
                isSeeking = false
                continuousSeekDirection = 0
            }
        }
    }

    fun startSeek(shouldShowControls: Boolean = true) {
        if (!isSeeking) {
            seekBasePosition = currentPosition
            seekTargetPosition = currentPosition
            isSeeking = true
        }
        updateInteractionTime(shouldShowControls = shouldShowControls)
    }

    fun handleSeek(deltaSeconds: Long) {
        startSeek(shouldShowControls = !isProgressPreviewVisible)
        val tentative = seekTargetPosition + deltaSeconds
        val clamped = when {
            tentative < 0L -> 0L
            duration > 0 -> tentative.coerceAtMost(duration)
            else -> tentative
        }
        seekTargetPosition = clamped
        onSeek(clamped)
        lastSeekTime = System.currentTimeMillis()
        updateInteractionTime(shouldShowControls = !isProgressPreviewVisible)
    }

    fun commitSeek() {
        if (isSeeking) {
            onSeekComplete(seekTargetPosition)
            isSeeking = false
            continuousSeekDirection = 0
        }
    }

    val displayedPosition = if (isSeeking) seekTargetPosition else currentPosition
    val progressFraction = if (duration > 0L) {
        displayedPosition.toFloat() / duration.toFloat()
    } else {
        0f
    }

    LaunchedEffect(progressFraction) {
        sliderValue = progressFraction
    }
    val currentTimeColor = if (isSeeking) VoidBrightAzure else Color.White
    val progressColor = if (isSeeking) VoidBrightAzure else Minsk
    val remainingSeconds = (duration - displayedPosition).coerceAtLeast(0L)
    val endTimeText = currentTime.plusSeconds(remainingSeconds).format(timeFormatter)

    @Composable
    fun ProgressSection(
        modifier: Modifier = Modifier,
        interactionShowsControls: Boolean,
        progressOnly: Boolean,
    ) {
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    sliderValue = value
                    if (duration > 0) {
                        val newPosition = (value.toDouble() * duration).roundToLong()
                        startSeek(shouldShowControls = interactionShowsControls)
                        seekTargetPosition = newPosition
                        onSeek(newPosition)
                        lastSeekTime = System.currentTimeMillis()
                    }
                    updateInteractionTime(shouldShowControls = interactionShowsControls)
                },
                onValueChangeFinished = {
                    updateInteractionTime(shouldShowControls = interactionShowsControls)
                    commitSeek()
                },
                valueRange = 0f..1f,
                enabled = duration > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = calculateRoundedValue(8).sdp)
                    .focusable()
                    .focusRequester(progressBarFocusRequester)
                    .focusProperties {
                        up = if (progressOnly) {
                            FocusRequester.Cancel
                        } else {
                            playPauseFocusRequester
                        }
                        down = FocusRequester.Cancel
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
                    }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.DirectionUp -> {
                                    if (!progressOnly) {
                                        playPauseFocusRequester.requestFocus()
                                    }
                                    true
                                }
                                Key.DirectionDown -> {
                                    
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    .onFocusChanged { focusState ->
                        if (focusState.hasFocus) {
                            focusArea = VideoControlsFocusArea.PROGRESS_BAR
                            updateInteractionTime(shouldShowControls = interactionShowsControls)
                        } else {
                            continuousSeekDirection = 0
                            commitSeek()
                        }
                    },
                colors = SliderDefaults.colors(
                    activeTrackColor = progressColor,
                    inactiveTrackColor = SeekBarBackground,
                    thumbColor = progressColor,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(displayedPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = currentTimeColor
                    )

                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(overlayFocusRequester)
            .focusable()
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    focusArea = VideoControlsFocusArea.ROOT
                    updateInteractionTime(shouldShowControls = isVisible)
                }
            }
            .onPreviewKeyEvent { event ->
                
                if (event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.DirectionLeft, Key.DirectionRight -> {
                            
                            val wasHiddenHoldActive = hiddenHoldActive
                            
                            hiddenHoldDirection = 0
                            
                            if (isSeeking) {
                                commitSeek()
                            } else {
                                continuousSeekDirection = 0
                            }
                            
                            if (wasHiddenHoldActive) {
                                progressBarFocusRequester.requestFocus()
                            }
                            true
                        }
                        else -> false
                    }
                } else if (!isVisible && event.type == KeyEventType.KeyDown) {
                    
                    if (isAutoSkipActive && event.key !in listOf(Key.Enter, Key.NumPadEnter, Key.DirectionCenter)) {
                        onCancelAutoSkip?.invoke()
                    }
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (!isSeeking) {
                                seekTargetPosition = currentPosition
                                isSeeking = true
                            }
                            val deltaSeconds = -10L
                            val tentative = seekTargetPosition + deltaSeconds
                            val clamped = when {
                                tentative < 0L -> 0L
                                duration > 0 -> tentative.coerceAtMost(duration)
                                else -> tentative
                            }
                            seekTargetPosition = clamped
                            onSeek(clamped)
                            lastSeekTime = System.currentTimeMillis()
                            hiddenHoldDirection = -1
                            triggerHiddenSkipFeedback(-1)
                            true
                        }

                        Key.DirectionRight -> {
                            if (!isSeeking) {
                                seekTargetPosition = currentPosition
                                isSeeking = true
                            }
                            val deltaSeconds = 10L
                            val tentative = seekTargetPosition + deltaSeconds
                            val clamped = when {
                                tentative < 0L -> 0L
                                duration > 0 -> tentative.coerceAtMost(duration)
                                else -> tentative
                            }
                            seekTargetPosition = clamped
                            onSeek(clamped)
                            lastSeekTime = System.currentTimeMillis()
                            hiddenHoldDirection = 1
                            triggerHiddenSkipFeedback(1)
                            true
                        }

                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            if (floatingButtonVisible && onFloatingSkipButtonClick != null) {
                                onFloatingSkipButtonClick()
                                floatingButtonVisible = false
                                true
                            } else {
                                updateInteractionTime()
                                true
                            }
                        }

                        Key.DirectionUp, Key.DirectionDown,
                        Key.MediaPlayPause, Key.Spacebar -> {
                            updateInteractionTime()
                            true
                        }

                        else -> false
                    }
                } else if (isVisible) {
                    
                    if (isAutoSkipActive && event.type == KeyEventType.KeyDown && event.key !in listOf(Key.Enter, Key.NumPadEnter, Key.DirectionCenter)) {
                        onCancelAutoSkip?.invoke()
                    }
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    when (focusArea) {
                                        VideoControlsFocusArea.PROGRESS_BAR,
                                        VideoControlsFocusArea.ROOT -> {
                                            if (continuousSeekDirection == 0) {
                                                handleSeek(-10)
                                                continuousSeekDirection = -1
                                            }
                                            true
                                        }
                                        else -> {
                                            updateInteractionTime()
                                            false
                                        }
                                    }
                                }

                                Key.DirectionRight -> {
                                    when (focusArea) {
                                        VideoControlsFocusArea.PROGRESS_BAR,
                                        VideoControlsFocusArea.ROOT -> {
                                            if (continuousSeekDirection == 0) {
                                                handleSeek(10)
                                                continuousSeekDirection = 1
                                            }
                                            true
                                        }
                                        else -> {
                                            updateInteractionTime()
                                            false
                                        }
                                    }
                                }

                                Key.DirectionUp, Key.DirectionDown -> {
                                    updateInteractionTime()
                                    false
                                }

                                Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                                    updateInteractionTime()
                                    when (focusArea) {
                                        VideoControlsFocusArea.PROGRESS_BAR -> {
                                            commitSeek()
                                            true
                                        }
                                        VideoControlsFocusArea.ROOT -> {
                                            onPlayPauseClick()
                                            true
                                        }
                                        else -> false
                                    }
                                }

                                Key.MediaPlayPause, Key.Spacebar -> {
                                    updateInteractionTime()
                                    onPlayPauseClick()
                                    true
                                }

                                Key.Back, Key.Escape -> {
                                    if (isSeeking) {
                                        onSeek(seekBasePosition)
                                        onSeekComplete(seekBasePosition)
                                        isSeeking = false
                                        continuousSeekDirection = 0
                                        updateInteractionTime()
                                        true
                                    } else if (isVisible && (
                                        focusArea == VideoControlsFocusArea.ROOT ||
                                        focusArea == VideoControlsFocusArea.CONTROLS ||
                                        focusArea == VideoControlsFocusArea.PROGRESS_BAR ||
                                        focusArea == VideoControlsFocusArea.SKIP ||
                                        focusArea == VideoControlsFocusArea.BOTTOM_OPTIONS
                                        )) {
                                        onDismissControls()
                                        true
                                    } else {
                                        onBackClick()
                                        true
                                    }
                                }

                                else -> false
                            }
                        }
                        KeyEventType.KeyUp -> {
                            when (event.key) {
                                Key.DirectionLeft, Key.DirectionRight -> {
                                    if (
                                        focusArea == VideoControlsFocusArea.PROGRESS_BAR ||
                                        focusArea == VideoControlsFocusArea.ROOT
                                    ) {
                                        continuousSeekDirection = 0
                                        commitSeek()
                                        true
                                    } else false
                                }
                                else -> false
                            }
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                val topPadding = calculateRoundedValue(36).sdp
                val horizontalPadding = calculateRoundedValue(24).sdp

                Text(
                    text = formattedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = topPadding, start = horizontalPadding, end = horizontalPadding)
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = topPadding, end = horizontalPadding)
                ) {
                    Text(
                        text = currentTime.format(timeFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Ends at $endTimeText",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            horizontal = calculateRoundedValue(16).sdp
                        )
                ) {
                    Row (
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                onPlayPrevious?.invoke()
                                updateInteractionTime()
                            },
                            enabled = onPlayPrevious != null,
                            modifier = Modifier
                                .focusedShadow(previousFocused)
                                .focusable()
                                .focusRequester(previousEpisodeFocusRequester)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                    down = progressBarFocusRequester
                                    left = FocusRequester.Cancel
                                    right = skipBackwardFocusRequester
                                }
                                .onFocusChanged { focusState ->
                                    previousFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        focusArea = VideoControlsFocusArea.CONTROLS
                                        updateInteractionTime()
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = if (onPlayPrevious != null) Color.White else Color.Gray,
                                modifier = Modifier.size(calculateRoundedValue(40).sdp),
                            )
                        }

                        VerticalDivider(
                            thickness = calculateRoundedValue(2).sdp,
                            modifier = Modifier.height(calculateRoundedValue(40).sdp)
                        )

                        IconButton(
                            onClick = {
                                onSkipBackward()
                                updateInteractionTime()
                            },
                            modifier = Modifier
                                .focusedShadow(skipBackwardFocused)
                                .focusable()
                                .focusRequester(skipBackwardFocusRequester)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                    down = progressBarFocusRequester
                                    left = if (onPlayPrevious != null) {
                                        previousEpisodeFocusRequester
                                    } else {
                                        FocusRequester.Cancel
                                    }
                                    right = playPauseFocusRequester
                                }
                                .onFocusChanged { focusState ->
                                    skipBackwardFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        focusArea = VideoControlsFocusArea.CONTROLS
                                        updateInteractionTime()
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Skip backward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(calculateRoundedValue(40).sdp),
                            )
                        }

                        IconButton(
                            onClick = {
                                onPlayPauseClick()
                                updateInteractionTime()
                            },
                            modifier = Modifier
                                .focusedShadow(playPauseFocused)
                                .focusable()
                                .focusRequester(playPauseFocusRequester)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                    down = progressBarFocusRequester
                                    left = skipBackwardFocusRequester
                                    right = skipForwardFocusRequester
                                }
                                .onFocusChanged { focusState ->
                                    playPauseFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        focusArea = VideoControlsFocusArea.CONTROLS
                                        updateInteractionTime()
                                    }
                                },
                        ) {
                            when {
                                isBuffering -> { }
                                isPlaying -> {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = "Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(calculateRoundedValue(40).sdp),
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(calculateRoundedValue(40).sdp),
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                onSkipForward()
                                updateInteractionTime()
                            },
                            modifier = Modifier
                                .focusedShadow(skipForwardFocused)
                                .focusable()
                                .focusRequester(skipForwardFocusRequester)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                    down = progressBarFocusRequester
                                    left = playPauseFocusRequester
                                    right = if (onPlayNext != null) {
                                        nextEpisodeFocusRequester
                                    } else if (onVideoClick != null) {
                                        videoFocusRequester
                                    } else if (onAudioClick != null) {
                                        audioFocusRequester
                                    } else if (onSubtitleClick != null) {
                                        subtitleFocusRequester
                                    } else if (onSubtitleOptionsClick != null) {
                                        subtitleOptionsFocusRequester
                                    } else if (skipButtonVisible && onSkipButtonClick != null) {
                                        skipButtonFocusRequester
                                    } else {
                                        FocusRequester.Cancel
                                    }
                                }
                                .onFocusChanged { focusState ->
                                    skipForwardFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        focusArea = VideoControlsFocusArea.CONTROLS
                                        updateInteractionTime()
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Skip forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(calculateRoundedValue(40).sdp),
                            )
                        }

                        VerticalDivider(
                            thickness = calculateRoundedValue(2).sdp,
                            modifier = Modifier.height(calculateRoundedValue(40).sdp)
                        )

                        IconButton(
                            onClick = {
                                onPlayNext?.invoke()
                                updateInteractionTime()
                            },
                            enabled = onPlayNext != null,
                            modifier = Modifier
                                .focusedShadow(nextFocused)
                                .focusable()
                                .focusRequester(nextEpisodeFocusRequester)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                    down = progressBarFocusRequester
                                    left = skipForwardFocusRequester
                                    right = if (onVideoClick != null) {
                                        videoFocusRequester
                                    } else if (onAudioClick != null) {
                                        audioFocusRequester
                                    } else if (onSubtitleClick != null) {
                                        subtitleFocusRequester
                                    } else if (onSubtitleOptionsClick != null) {
                                        subtitleOptionsFocusRequester
                                    } else if (skipButtonVisible && onSkipButtonClick != null) {
                                        skipButtonFocusRequester
                                    } else {
                                        FocusRequester.Cancel
                                    }
                                }
                                .onFocusChanged { focusState ->
                                    nextFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        focusArea = VideoControlsFocusArea.CONTROLS
                                        updateInteractionTime()
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = if (onPlayNext != null) Color.White else Color.Gray,
                                modifier = Modifier.size(calculateRoundedValue(40).sdp),
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (onVideoClick != null) {
                            val label = videoQualityLabel?.ifBlank { "Auto" } ?: "Auto"
                            val displayLabel = label.removePrefix("Quality: ").trim()
                            TextButton(
                                onClick = {
                                    onVideoClick()
                                    updateInteractionTime()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .focusedPill(videoFocused)
                                    .focusable()
                                    .focusRequester(videoFocusRequester)
                                    .focusProperties {
                                        up = FocusRequester.Cancel
                                        down = progressBarFocusRequester
                                        left = if (onPlayNext != null) {
                                            nextEpisodeFocusRequester
                                        } else {
                                            skipForwardFocusRequester
                                        }
                                        right = if (onAudioClick != null) {
                                            audioFocusRequester
                                        } else if (onSubtitleClick != null) {
                                            subtitleFocusRequester
                                        } else if (onSubtitleOptionsClick != null) {
                                            subtitleOptionsFocusRequester
                                        } else if (skipButtonVisible && onSkipButtonClick != null) {
                                            skipButtonFocusRequester
                                        } else {
                                            FocusRequester.Cancel
                                        }
                                    }
                                    .onFocusChanged { focusState ->
                                        videoFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            focusArea = VideoControlsFocusArea.CONTROLS
                                            updateInteractionTime()
                                        }
                                    }
                            ) {
                                Text(text = displayLabel)
                            }
                        }

                        if (onAudioClick != null) {
                            IconButton(
                                onClick = {
                                    onAudioClick()
                                    updateInteractionTime()
                                },
                                modifier = Modifier
                                    .focusedShadow(audioFocused)
                                    .focusable()
                                    .focusRequester(audioFocusRequester)
                                    .focusProperties {
                                        up = FocusRequester.Cancel
                                        down = progressBarFocusRequester
                                        left = if (onVideoClick != null) {
                                            videoFocusRequester
                                        } else if (onPlayNext != null) {
                                            nextEpisodeFocusRequester
                                        } else {
                                            skipForwardFocusRequester
                                        }
                                        right = if (onSubtitleClick != null) {
                                            subtitleFocusRequester
                                        } else if (onSubtitleOptionsClick != null) {
                                            subtitleOptionsFocusRequester
                                        } else if (skipButtonVisible && onSkipButtonClick != null) {
                                            skipButtonFocusRequester
                                        } else {
                                            FocusRequester.Cancel
                                        }
                                    }
                                    .onFocusChanged { focusState ->
                                        audioFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            focusArea = VideoControlsFocusArea.CONTROLS
                                            updateInteractionTime()
                                        }
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Audio",
                                    modifier = Modifier.size(calculateRoundedValue(40).sdp),
                                    tint = Color.White
                                )
                            }
                        }

                        if (onSubtitleClick != null) {
                            IconButton(
                                onClick = {
                                    onSubtitleClick()
                                    updateInteractionTime()
                                },
                                modifier = Modifier
                                    .focusedShadow(subtitleFocused)
                                    .focusable()
                                    .focusRequester(subtitleFocusRequester)
                                    .focusProperties {
                                        up = FocusRequester.Cancel
                                        down = progressBarFocusRequester
                                        left = if (onAudioClick != null) {
                                            audioFocusRequester
                                        } else if (onVideoClick != null) {
                                            videoFocusRequester
                                        } else if (onPlayNext != null) {
                                            nextEpisodeFocusRequester
                                        } else {
                                            skipForwardFocusRequester
                                        }
                                        right = if (onSubtitleOptionsClick != null) {
                                            subtitleOptionsFocusRequester
                                        } else if (skipButtonVisible && onSkipButtonClick != null) {
                                            skipButtonFocusRequester
                                        } else {
                                            FocusRequester.Cancel
                                        }
                                    }
                                    .onFocusChanged { focusState ->
                                        subtitleFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            focusArea = VideoControlsFocusArea.CONTROLS
                                            updateInteractionTime()
                                        }
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "Subtitles",
                                    modifier = Modifier.size(calculateRoundedValue(40).sdp),
                                    tint = Color.White
                                )
                            }
                        }

                        if (onSubtitleOptionsClick != null) {
                            IconButton(
                                onClick = {
                                    onSubtitleOptionsClick()
                                    updateInteractionTime()
                                },
                                modifier = Modifier
                                    .focusedShadow(subtitleOptionsFocused)
                                    .focusable()
                                    .focusRequester(subtitleOptionsFocusRequester)
                                    .focusProperties {
                                        up = FocusRequester.Cancel
                                        down = progressBarFocusRequester
                                        left = if (onSubtitleClick != null) {
                                            subtitleFocusRequester
                                        } else if (onAudioClick != null) {
                                            audioFocusRequester
                                        } else if (onVideoClick != null) {
                                            videoFocusRequester
                                        } else if (onPlayNext != null) {
                                            nextEpisodeFocusRequester
                                        } else {
                                            skipForwardFocusRequester
                                        }
                                        right = if (skipButtonVisible && onSkipButtonClick != null) {
                                            skipButtonFocusRequester
                                        } else {
                                            FocusRequester.Cancel
                                        }
                                    }
                                    .onFocusChanged { focusState ->
                                        subtitleOptionsFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            focusArea = VideoControlsFocusArea.CONTROLS
                                            updateInteractionTime()
                                        }
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Subtitle options",
                                    modifier = Modifier.size(calculateRoundedValue(40).sdp),
                                    tint = Color.White
                                )
                            }
                        }

                        if (skipButtonVisible && onSkipButtonClick != null) {
                            Box {
                                
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(Color.White.copy(alpha = 0.2f))
                                )
                                
                                if (isAutoSkipActive) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(percent = 50))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(autoSkipProgress)
                                                .fillMaxHeight()
                                                .align(Alignment.CenterStart)
                                                .background(Minsk)
                                        )
                                    }
                                }
                                
                                TextButton(
                                    onClick = {
                                        onSkipButtonClick()
                                        updateInteractionTime()
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .focusable()
                                        .focusRequester(skipButtonFocusRequester)
                                        .focusProperties {
                                            up = FocusRequester.Cancel
                                            down = progressBarFocusRequester
                                            left = if (onSubtitleOptionsClick != null) {
                                                subtitleOptionsFocusRequester
                                            } else if (onSubtitleClick != null) {
                                                subtitleFocusRequester
                                            } else if (onAudioClick != null) {
                                                audioFocusRequester
                                            } else if (onVideoClick != null) {
                                                videoFocusRequester
                                            } else if (onPlayNext != null) {
                                                nextEpisodeFocusRequester
                                            } else {
                                                skipForwardFocusRequester
                                            }
                                            right = FocusRequester.Cancel
                                        }
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                focusArea = VideoControlsFocusArea.CONTROLS
                                                updateInteractionTime()
                                            }
                                        }
                                ) {
                                    Text(text = skipButtonLabel)
                                }
                            }
                        }
                    }

                    ProgressSection(
                        interactionShowsControls = true,
                        progressOnly = false
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isVisible && isProgressPreviewVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                ProgressSection(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = calculateRoundedValue(16).sdp),
                    interactionShowsControls = false,
                    progressOnly = true
                )
            }
        }

        AnimatedVisibility(
            visible = !isVisible && hiddenSkipFeedbackVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val isBackward = hiddenSkipFeedbackDirection < 0
                val bubbleAlignment = if (isBackward) Alignment.CenterStart else Alignment.CenterEnd
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(bubbleAlignment)
                        .padding(horizontal = calculateRoundedValue(32).sdp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(
                            horizontal = calculateRoundedValue(16).sdp,
                            vertical = calculateRoundedValue(10).sdp
                        )
                ) {
                    Icon(
                        imageVector = if (isBackward) Icons.Default.Replay10 else Icons.Default.Forward10,
                        contentDescription = if (isBackward) "Skipped back 10 seconds" else "Skipped forward 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(calculateRoundedValue(32).sdp),
                    )
                    Spacer(modifier = Modifier.size(calculateRoundedValue(8).sdp))
                    Text(
                        text = if (isBackward) "-10s" else "+10s",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        
        AnimatedVisibility(
            visible = floatingButtonVisible && floatingSkipButtonVisible && !isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(
                        end = calculateRoundedValue(24).sdp,
                        bottom = calculateRoundedValue(24).sdp
                    )
            ) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                    
                    if (isAutoSkipActive) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(percent = 50))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(autoSkipProgress)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterStart)
                                    .background(Minsk)
                            )
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            onFloatingSkipButtonClick?.invoke()
                            floatingButtonVisible = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .focusedPill(floatingSkipButtonFocused)
                            .focusable()
                            .focusRequester(floatingSkipButtonFocusRequester)
                            .focusProperties {
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                            }
                            .onFocusChanged { focusState ->
                                floatingSkipButtonFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    focusArea = VideoControlsFocusArea.SKIP
                                }
                            }
                    ) {
                        Text(text = skipButtonLabel)
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 800,
    heightDp = 400
)
@Composable
private fun VideoControlsOverlayPreview() {
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }

    val duration = 2 * 60 * 60 * 1000L
    var currentPosition by remember { mutableLongStateOf(35 * 60 * 1000L) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000)
            currentPosition = (currentPosition + 1000L).coerceAtMost(duration)
            if (currentPosition % 30_000L == 0L) {
                isBuffering = true
                delay(500)
                isBuffering = false
            }
        }
    }

    MaterialTheme {
        VideoControlsOverlay(
            mediaTitle = "The Tesseract of Tuesdays",
            seasonNumber = 1,
            episodeNumber = 3,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            duration = duration,
            currentPosition = currentPosition,
            isVisible = isVisible,
            onPlayPauseClick = { isPlaying = !isPlaying },
            onSeek = { pos -> currentPosition = pos },
            onSeekComplete = { pos -> currentPosition = pos },
            onBackClick = { },
            onDismissControls = { },
            onShowControls = { },
            onSkipBackward = {
                currentPosition = (currentPosition - 10_000L).coerceAtLeast(0L)
            },
            onSkipForward = {
                currentPosition = (currentPosition + 10_000L).coerceAtMost(duration)
            },
            skipButtonVisible = true,
            skipButtonLabel = "Skip Intro",
            onSkipButtonClick = {
                currentPosition = (currentPosition + 85_000L).coerceAtMost(duration)
            },
            onPlayPrevious = { },
            onPlayNext = { },
            onAudioClick = { },
            onSubtitleClick = { },
            onSubtitleOptionsClick = { }
        )
    }
}
