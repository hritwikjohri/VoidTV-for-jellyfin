package com.hritwik.avoid.presentation.ui.screen.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.UserData
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import ir.kaaveh.sdpcompose.ssp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch




private fun buildImageUrl(
    serverUrl: String,
    itemId: String,
    accessToken: String,
    maxWidth: Int
): String? {
    
    if (serverUrl.isBlank() || itemId.isBlank() || accessToken.isBlank()) {
        return null
    }

    
    val cleanServerUrl = serverUrl.trimEnd('/')

    return "$cleanServerUrl/Items/$itemId/Images/Primary?api_key=$accessToken&maxWidth=$maxWidth"
}

@Composable
fun PlaybackEndScreen(
    currentMediaItem: MediaItem,
    nextEpisode: MediaItem?,
    onDeckItems: List<MediaItem>,
    serverUrl: String,
    accessToken: String,
    autoPlayDelaySeconds: Int = 10,
    onReplay: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayItem: (MediaItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    
    val validatedDelaySeconds = autoPlayDelaySeconds.coerceIn(1, 60)
    var countdownSeconds by remember { mutableIntStateOf(validatedDelaySeconds) }
    var isCountdownActive by remember { mutableStateOf(nextEpisode != null) }
    val playNextFocusRequester = remember { FocusRequester() }
    val replayFocusRequester = remember { FocusRequester() }
    var hasFocusOnScreen by remember { mutableStateOf(false) }
    val hasOnDeckItems = onDeckItems.isNotEmpty()

    LaunchedEffect(nextEpisode, validatedDelaySeconds) {
        countdownSeconds = validatedDelaySeconds
        isCountdownActive = nextEpisode != null
    }

    
    LaunchedEffect(nextEpisode) {
        delay(100) 
        
        if (isActive) {
            if (nextEpisode != null && isCountdownActive) {
                playNextFocusRequester.requestFocus()
            } else {
                replayFocusRequester.requestFocus()
            }
            hasFocusOnScreen = true
        }
    }

    
    LaunchedEffect(hasFocusOnScreen) {
        if (!hasFocusOnScreen) {
            delay(100)
            if (isActive) {
                if (nextEpisode != null) {
                    playNextFocusRequester.requestFocus()
                } else {
                    replayFocusRequester.requestFocus()
                }
                hasFocusOnScreen = true
            }
        }
    }

    
    LaunchedEffect(isCountdownActive, nextEpisode) {
        if (isCountdownActive && nextEpisode != null) {
            while (countdownSeconds > 0 && isActive) {
                delay(1000)
                countdownSeconds--
            }
            if (countdownSeconds == 0 && isActive) {
                isCountdownActive = false
                onPlayNext()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && isCountdownActive) {
                    isCountdownActive = false
                }
                false
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = calculateRoundedValue(48).sdp)
                .padding(top = calculateRoundedValue(48).sdp, bottom = calculateRoundedValue(32).sdp),
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(32).sdp)
        ) {
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(32).sdp)
            ) {
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
                ) {
                    Text(
                        text = "Just Played",
                        fontSize = calculateRoundedValue(18).ssp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    JustPlayedCard(
                        mediaItem = currentMediaItem,
                        serverUrl = serverUrl,
                        accessToken = accessToken,
                        onReplay = onReplay,
                        modifier = Modifier.fillMaxWidth(),
                        focusRequester = replayFocusRequester,
                        requestFocus = nextEpisode == null,
                        onFocusChanged = { focused ->
                            if (focused) hasFocusOnScreen = true
                        },
                        preventDownNavigation = !hasOnDeckItems,
                        preventRightNavigation = nextEpisode == null
                    )
                }

                
                if (nextEpisode != null) {
                    Column(
                        modifier = Modifier.weight(1.5f),
                        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
                    ) {
                        Text(
                            text = "Up Next",
                            fontSize = calculateRoundedValue(18).ssp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        UpNextCard(
                            mediaItem = nextEpisode,
                            serverUrl = serverUrl,
                            accessToken = accessToken,
                            countdownSeconds = if (isCountdownActive) countdownSeconds else null,
                            onPlay = onPlayNext,
                            onCancelCountdown = { isCountdownActive = false },
                            modifier = Modifier.fillMaxWidth(),
                            playNextFocusRequester = playNextFocusRequester,
                            onFocusChanged = { focused ->
                                if (focused) hasFocusOnScreen = true
                            },
                            preventDownNavigation = !hasOnDeckItems
                        )
                    }
                }
            }

            
            if (onDeckItems.isNotEmpty()) {
                val displayedOnDeckItems = onDeckItems.take(6)
                val onDeckFocusRequesters = remember(displayedOnDeckItems.map { it.id }) {
                    List(displayedOnDeckItems.size) { FocusRequester() }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                ) {
                    Text(
                        text = "Continue Watching",
                        fontSize = calculateRoundedValue(18).ssp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    val listState = rememberLazyListState()
                    var currentFocusedIndex by remember { mutableIntStateOf(0) }
                    val coroutineScope = rememberCoroutineScope()

                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = calculateRoundedValue(24).sdp),
                        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp),
                        modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionRight -> {
                                        if (currentFocusedIndex < displayedOnDeckItems.size - 1) {
                                            currentFocusedIndex++
                                            onDeckFocusRequesters.getOrNull(currentFocusedIndex)?.requestFocus()
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(currentFocusedIndex)
                                            }
                                            true
                                        } else {
                                            
                                            true
                                        }
                                    }

                                    Key.DirectionLeft -> {
                                        if (currentFocusedIndex > 0) {
                                            currentFocusedIndex--
                                            onDeckFocusRequesters.getOrNull(currentFocusedIndex)?.requestFocus()
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(currentFocusedIndex)
                                            }
                                            true
                                        } else {
                                            
                                            true
                                        }
                                    }

                                    Key.DirectionDown -> {
                                        
                                        true
                                    }

                                    else -> false
                                }
                            } else false
                        }
                    ) {
                        itemsIndexed(
                            items = displayedOnDeckItems,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            
                            val focusRequester = onDeckFocusRequesters.getOrNull(index) ?: remember { FocusRequester() }
                            OnDeckCard(
                                mediaItem = item,
                                serverUrl = serverUrl,
                                accessToken = accessToken,
                                onPlay = { onPlayItem(item) },
                                focusRequester = focusRequester,
                                onFocusChanged = { hasFocus ->
                                    if (hasFocus) {
                                        currentFocusedIndex = index
                                        hasFocusOnScreen = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JustPlayedCard(
    mediaItem: MediaItem,
    serverUrl: String,
    accessToken: String,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    requestFocus: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    preventDownNavigation: Boolean = false,
    preventRightNavigation: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "scale")

    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
    }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            delay(100)
            if (isActive) {
                focusRequester.requestFocus()
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .scale(scale)
            .sizeIn(minWidth = 100.dp, minHeight = 56.dp) 
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        
                        Key.DirectionUp -> true
                        
                        Key.DirectionDown -> preventDownNavigation
                        
                        Key.DirectionLeft -> true
                        
                        Key.DirectionRight -> preventRightNavigation
                        else -> false
                    }
                } else {
                    false
                }
            }
            .then(
                if (isFocused) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(calculateRoundedValue(8).sdp)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(calculateRoundedValue(8).sdp))
            .background(Color(0xFF1A1F35))
            .dpadNavigation(
                focusRequester = focusRequester,
                onClick = onReplay,
                showFocusOutline = false,
                interactionSource = interactionSource
            )
    ) {
        
        AsyncImage(
            model = buildImageUrl(serverUrl, mediaItem.id, accessToken, 800),
            contentDescription = mediaItem.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Played",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(calculateRoundedValue(12).sdp)
                .size(calculateRoundedValue(24).sdp)
        )

        
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(calculateRoundedValue(64).sdp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Replay",
                tint = Color.White,
                modifier = Modifier.size(calculateRoundedValue(32).sdp)
            )
        }

        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(calculateRoundedValue(16).sdp)
        ) {
            val displayTitle = (mediaItem.seriesName ?: mediaItem.name)?.takeIf { it.isNotBlank() } ?: "Unknown"
            Text(
                text = displayTitle,
                fontSize = calculateRoundedValue(16).ssp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (mediaItem.type == "Episode") {
                val seasonNum = mediaItem.parentIndexNumber
                val episodeNum = mediaItem.indexNumber
                val episodeName = mediaItem.name?.takeIf { it.isNotBlank() } ?: "Unknown"
                
                if (seasonNum != null && seasonNum > 0 && episodeNum != null && episodeNum > 0) {
                    Text(
                        text = "S$seasonNum • E$episodeNum - $episodeName",
                        fontSize = calculateRoundedValue(12).ssp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun UpNextCard(
    mediaItem: MediaItem,
    serverUrl: String,
    accessToken: String,
    countdownSeconds: Int?,
    onPlay: () -> Unit,
    onCancelCountdown: () -> Unit,
    modifier: Modifier = Modifier,
    playNextFocusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChanged: (Boolean) -> Unit = {},
    preventDownNavigation: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(24).sdp)
    ) {
        
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(16f / 9f)
                .sizeIn(minWidth = 100.dp, minHeight = 56.dp) 
                .clip(RoundedCornerShape(calculateRoundedValue(8).sdp))
                .background(Color(0xFF1A1F35))
        ) {
            AsyncImage(
                model = buildImageUrl(serverUrl, mediaItem.id, accessToken, 800),
                contentDescription = mediaItem.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Queued",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(calculateRoundedValue(12).sdp)
                    .size(calculateRoundedValue(24).sdp)
            )

            
            if (countdownSeconds != null) {
                CountdownCircle(
                    seconds = countdownSeconds,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
        ) {
            
            Column(
                verticalArrangement = Arrangement.spacedBy(0.sdp)
            ) {
                val seriesTitle = mediaItem.seriesName?.takeIf { it.isNotBlank() } ?: "Unknown Series"
                Text(
                    text = seriesTitle,
                    fontSize = calculateRoundedValue(20).ssp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (mediaItem.type == "Episode") {
                    val seasonNum = mediaItem.parentIndexNumber
                    val episodeNum = mediaItem.indexNumber
                    val episodeName = mediaItem.name?.takeIf { it.isNotBlank() } ?: "Unknown"
                    
                    if (seasonNum != null && seasonNum > 0 && episodeNum != null && episodeNum > 0) {
                        Text(
                            text = "S$seasonNum • E$episodeNum - $episodeName",
                            fontSize = calculateRoundedValue(14).ssp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = mediaItem.overview ?: "",
                fontSize = calculateRoundedValue(12).ssp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = calculateRoundedValue(16).ssp
            )

            Spacer(modifier = Modifier.weight(1f))

            
            val buttonShape = RoundedCornerShape(calculateRoundedValue(6).sdp)
            if (countdownSeconds != null) {
                val cancelInteractionSource = remember { MutableInteractionSource() }
                val isCancelFocused by cancelInteractionSource.collectIsFocusedAsState()

                LaunchedEffect(isCancelFocused) {
                    onFocusChanged(isCancelFocused)
                }

                Button(
                    onClick = onCancelCountdown,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = buttonShape,
                    interactionSource = cancelInteractionSource,
                    modifier = Modifier
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    
                                    Key.DirectionUp -> true
                                    
                                    Key.DirectionDown -> preventDownNavigation
                                    
                                    Key.DirectionRight -> true
                                    else -> false
                                }
                            } else {
                                false
                            }
                        }
                        .dpadNavigation(
                            shape = buttonShape,
                            focusRequester = playNextFocusRequester,
                            interactionSource = cancelInteractionSource,
                            applyClickModifier = false,
                            showFocusOutline = false
                        )
                        .then(
                            if (isCancelFocused) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, buttonShape)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = calculateRoundedValue(12).ssp,
                        color = Color.White
                    )
                }
            } else {
                val playInteractionSource = remember { MutableInteractionSource() }
                val isPlayFocused by playInteractionSource.collectIsFocusedAsState()
                val primaryButtonColor = MaterialTheme.colorScheme.primary

                LaunchedEffect(isPlayFocused) {
                    onFocusChanged(isPlayFocused)
                }

                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonColor.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = buttonShape,
                    interactionSource = playInteractionSource,
                    modifier = Modifier
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    
                                    Key.DirectionUp -> true
                                    
                                    Key.DirectionDown -> preventDownNavigation
                                    
                                    Key.DirectionRight -> true
                                    else -> false
                                }
                            } else {
                                false
                            }
                        }
                        .dpadNavigation(
                            shape = buttonShape,
                            focusRequester = playNextFocusRequester,
                            interactionSource = playInteractionSource,
                            applyClickModifier = false,
                            showFocusOutline = false
                        )
                        .then(
                            if (isPlayFocused) {
                                Modifier.border(3.dp, primaryButtonColor, buttonShape)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Text(
                        text = "Play Now",
                        fontSize = calculateRoundedValue(12).ssp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownCircle(
    seconds: Int,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }

    LaunchedEffect(seconds) {
        
        if (seconds > 0 && isActive) {
            progress.snapTo(1f)
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = modifier.size(calculateRoundedValue(80).sdp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress.value },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = calculateRoundedValue(4).sdp,
            trackColor = Color.White.copy(alpha = 0.2f)
        )

        Text(
            text = seconds.toString(),
            fontSize = calculateRoundedValue(28).ssp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun OnDeckCard(
    mediaItem: MediaItem,
    serverUrl: String,
    accessToken: String,
    onPlay: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "scale")

    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
    }

    Column(
        modifier = Modifier
            .width(calculateRoundedValue(240).sdp)
            .scale(scale)
            .sizeIn(minWidth = 120.dp, maxWidth = 400.dp) 
            .dpadNavigation(
                focusRequester = focusRequester,
                onClick = onPlay,
                showFocusOutline = false,
                interactionSource = interactionSource
            ),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp) 
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .sizeIn(minHeight = 68.dp) 
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(calculateRoundedValue(6).sdp)
                        )
                    } else Modifier
                )
                .clip(RoundedCornerShape(calculateRoundedValue(6).sdp))
                .background(Color(0xFF1A1F35))
        ) {
            AsyncImage(
                model = buildImageUrl(serverUrl, mediaItem.id, accessToken, 500),
                contentDescription = mediaItem.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            
            val playbackTicks = mediaItem.userData?.playbackPositionTicks ?: 0L
            val runtimeTicks = mediaItem.runTimeTicks ?: 0L
            val progress = remember(playbackTicks, runtimeTicks) {
                if (runtimeTicks > 0) (playbackTicks.toFloat() / runtimeTicks.toFloat()).coerceIn(0f, 1f) else 0f
            }

            if (progress > 0f && progress < 1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(calculateRoundedValue(4).sdp)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        
        Column(
            verticalArrangement = Arrangement.spacedBy(0.sdp)
        ) {
            val displayTitle = (mediaItem.seriesName ?: mediaItem.name)?.takeIf { it.isNotBlank() } ?: "Unknown"
            Text(
                text = displayTitle,
                fontSize = calculateRoundedValue(13).ssp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (mediaItem.type == "Episode") {
                val seasonNum = mediaItem.parentIndexNumber
                val episodeNum = mediaItem.indexNumber
                val episodeName = mediaItem.name?.takeIf { it.isNotBlank() } ?: "Unknown"
                
                if (seasonNum != null && seasonNum > 0 && episodeNum != null && episodeNum > 0) {
                    Text(
                        text = "S$seasonNum • E$episodeNum - $episodeName",
                        fontSize = calculateRoundedValue(11).ssp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun previewMediaItem(
    id: String,
    name: String,
    seriesName: String,
    overview: String,
    parentIndex: Int,
    episodeIndex: Int,
    playbackPositionMinutes: Int,
    runtimeMinutes: Int
): MediaItem {
    val runtimeTicks = runtimeMinutes * 60 * 10_000_000L
    val playbackTicks = playbackPositionMinutes * 60 * 10_000_000L

    return MediaItem(
        id = id,
        name = name,
        title = name,
        type = "Episode",
        overview = overview,
        year = 2024,
        communityRating = 8.7,
        runTimeTicks = runtimeTicks,
        primaryImageTag = "primary-$id",
        logoImageTag = "logo-$id",
        backdropImageTags = listOf("backdrop-$id"),
        genres = listOf("Sci-Fi", "Adventure"),
        isFolder = false,
        childCount = null,
        userData = UserData(
            playbackPositionTicks = playbackTicks,
            played = playbackTicks > 0
        ),
        seriesName = seriesName,
        seasonName = "Season $parentIndex",
        parentIndexNumber = parentIndex,
        indexNumber = episodeIndex
    )
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0A0E1A,
    widthDp = 1280,
    heightDp = 720
)
@Composable
private fun PlaybackEndScreenPreview() {
    val currentEpisode = previewMediaItem(
        id = "ep5",
        name = "Echoes in the Void",
        seriesName = "Signal Lost",
        overview = "After the jump drive misfires, the crew confronts echoes of their own choices.",
        parentIndex = 2,
        episodeIndex = 5,
        playbackPositionMinutes = 48,
        runtimeMinutes = 52
    )

    val nextEpisode = previewMediaItem(
        id = "ep6",
        name = "Graviton Bloom",
        seriesName = "Signal Lost",
        overview = "A derelict station hides a garden grown from collapsed stars.",
        parentIndex = 2,
        episodeIndex = 6,
        playbackPositionMinutes = 0,
        runtimeMinutes = 50
    )

    val onDeckItems = listOf(
        previewMediaItem(
            id = "deck1",
            name = "The Luminous City",
            seriesName = "Afterlight",
            overview = "A cartographer tracks cities that only appear during eclipses.",
            parentIndex = 1,
            episodeIndex = 2,
            playbackPositionMinutes = 18,
            runtimeMinutes = 46
        ),
        previewMediaItem(
            id = "deck2",
            name = "Ashfall Protocol",
            seriesName = "Afterlight",
            overview = "A hidden code in volcanic ash warns of an old betrayal.",
            parentIndex = 1,
            episodeIndex = 3,
            playbackPositionMinutes = 0,
            runtimeMinutes = 45
        ),
        previewMediaItem(
            id = "deck3",
            name = "Last Transmission",
            seriesName = "Redline Drift",
            overview = "A racer intercepts a distress call from a future version of herself.",
            parentIndex = 3,
            episodeIndex = 1,
            playbackPositionMinutes = 9,
            runtimeMinutes = 49
        )
    )

    MaterialTheme {
        PlaybackEndScreen(
            currentMediaItem = currentEpisode,
            nextEpisode = nextEpisode,
            onDeckItems = onDeckItems,
            serverUrl = "https://demo.jellyfin.org",
            accessToken = "preview_token",
            autoPlayDelaySeconds = 12,
            onReplay = {},
            onPlayNext = {},
            onPlayItem = {},
            onDismiss = {}
        )
    }
}
