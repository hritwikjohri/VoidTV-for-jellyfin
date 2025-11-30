package com.hritwik.avoid.presentation.ui.components.media

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.EmptyItem
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.LocalImageHelper
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp


private val FOCUS_BORDER_WIDTH = 2.dp
private val FOCUS_BORDER_COLOR = Color.White

@Composable
fun MediaItemCard(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    serverUrl: String = "",
    badgeNumber: Int? = null,
    cardType: MediaCardType = MediaCardType.POSTER,
    showProgress: Boolean = true,
    showTitle: Boolean = true,
    onClick: (MediaItem) -> Unit = {},
    onFocus: (MediaItem) -> Unit = {},
    focusRequester: FocusRequester? = null
) {
    val imageHelper = LocalImageHelper.current

    val aspectRatio = remember(cardType) {
        when (cardType) {
            MediaCardType.POSTER -> 2f / 3.15f
            MediaCardType.THUMBNAIL -> 16f / 9f
            MediaCardType.SQUARE -> 1f
        }
    }

    val defaultWidth = remember(cardType) {
        when (cardType) {
            MediaCardType.POSTER -> calculateRoundedValue(120)
            MediaCardType.THUMBNAIL -> calculateRoundedValue(240)
            MediaCardType.SQUARE -> calculateRoundedValue(120)
        }
    }.sdp

    var isFocused by remember { mutableStateOf(false) }

    
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "focus_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isFocused) calculateRoundedValue(16).dp else calculateRoundedValue(4).dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "focus_elevation"
    )

    
    val cardBorder = if (isFocused) {
        BorderStroke(calculateRoundedValue(FOCUS_BORDER_WIDTH.value.toInt()).dp, FOCUS_BORDER_COLOR)
    } else {
        null
    }
    val focusShape = remember(cardType) {
        when (cardType) {
            MediaCardType.THUMBNAIL -> RoundedCornerShape(calculateRoundedValue(16).dp)
            MediaCardType.POSTER -> RoundedCornerShape(calculateRoundedValue(16).dp)
            MediaCardType.SQUARE -> RoundedCornerShape(calculateRoundedValue(12).dp)
        }
    }

    Column(
        modifier = modifier
            .width(defaultWidth)
            .dpadNavigation(
                shape = focusShape,
                focusRequester = focusRequester,
                onClick = { onClick(mediaItem) },
                showFocusOutline = false,
                onFocusChange = { focused ->
                    isFocused = focused
                    if (focused) onFocus(mediaItem)
                }
            )
    ) {
        Box(
            modifier = Modifier
                .width(defaultWidth)
                .zIndex(if (isFocused) 1f else 0f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    clip = false
                }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio),
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                    shape = RoundedCornerShape(calculateRoundedValue(12).dp),
                    border = cardBorder
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (mediaItem.primaryImageTag != null) {
                            
                            val imageUrl = remember(mediaItem.id, mediaItem.primaryImageTag) {
                                if (cardType == MediaCardType.THUMBNAIL) {
                                    imageHelper.createBackdropUrl(
                                        serverUrl,
                                        mediaItem.id,
                                        mediaItem.backdropImageTags.firstOrNull()
                                    ) ?: createMediaImageUrl(serverUrl, mediaItem.id, mediaItem.primaryImageTag)
                                } else {
                                    createMediaImageUrl(serverUrl, mediaItem.id, mediaItem.primaryImageTag)
                                }
                            }

                            NetworkImage(
                                data = imageUrl,
                                contentDescription = mediaItem.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        } else {
                            EmptyItem()
                        }

                        if (showProgress && mediaItem.userData?.playbackPositionTicks != null &&
                            mediaItem.runTimeTicks != null && mediaItem.runTimeTicks > 0) {
                            val progress by remember(mediaItem.userData.playbackPositionTicks, mediaItem.runTimeTicks) {
                                derivedStateOf {
                                    mediaItem.userData.playbackPositionTicks.toFloat() /
                                            mediaItem.runTimeTicks.toFloat()
                                }
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(calculateRoundedValue(4).dp)
                                    .align(Alignment.BottomCenter),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        badgeNumber?.let { number ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(calculateRoundedValue(4).dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(calculateRoundedValue(4).dp)
                                    )
                                    .padding(
                                        horizontal = calculateRoundedValue(6).dp,
                                        vertical = calculateRoundedValue(2).dp
                                    )
                            ) {
                                Text(
                                    text = number.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (showTitle) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = calculateRoundedValue(8).dp)
                    ) {
                        if (mediaItem.type == ApiConstants.ITEM_TYPE_EPISODE && mediaItem.seriesName != null) {
                            Text(
                                text = mediaItem.seriesName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val seasonNumber = mediaItem.parentIndexNumber
                            val episodeNumber = mediaItem.indexNumber
                            if (seasonNumber != null && episodeNumber != null) {
                                Text(
                                    text = "S$seasonNumber . E$episodeNumber",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = mediaItem.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun createMediaImageUrl(
    serverUrl: String,
    itemId: String,
    imageTag: String
): String {
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$itemId/Images/Primary?tag=$imageTag&quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}&maxWidth=${ApiConstants.THUMBNAIL_MAX_WIDTH}"
}