package com.hritwik.avoid.presentation.ui.components.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.presentation.ui.components.common.EmptyItem
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.extensions.formatRuntime
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import ir.kaaveh.sdpcompose.ssp

@Composable
fun EpisodeThumbnailCard(
    modifier: Modifier = Modifier,
    episode: MediaItem,
    episodeNumber: Int?,
    serverUrl: String,
    seasonId: String? = null,
    seasonBackdropTag: String? = null,
    highlightColor: Color = Minsk,
    onClick: (MediaItem) -> Unit = {},
    focusRequester: FocusRequester? = null,
    onFocused: (MediaItem) -> Unit = {},
    onUnfocused: (MediaItem) -> Unit = {},
    onRequestActionsFocus: () -> Unit = {},
) {
    val cardFocusRequester = focusRequester ?: remember { FocusRequester() }
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardFocused by cardInteractionSource.collectIsFocusedAsState()
    val isEpisodeFocused = isCardFocused
    
    val scale by animateFloatAsState(
        targetValue = if (isEpisodeFocused) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "episode_thumb_focus"
    )
    val focusShape = remember { RoundedCornerShape(calculateRoundedValue(8).dp) }
    val animatedHighlightColor by animateColorAsState(
        targetValue = highlightColor,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "episode_focus_outline"
    )

    
    LaunchedEffect(isEpisodeFocused) {
        if (isEpisodeFocused) {
            
            delay(50)
            
            if (isEpisodeFocused) {
                onFocused(episode)
            }
        } else {
            
            
            delay(50)
            
            if (!isEpisodeFocused) {
                onUnfocused(episode)
            }
        }
    }

    Column(
        modifier = modifier.width(calculateRoundedValue(240).sdp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        val seasonNumber = episode.parentIndexNumber
        val seasonEpisodeLabel = when {
            seasonNumber != null && episodeNumber != null -> "S${seasonNumber}:E${episodeNumber}"
            episodeNumber != null -> "E${episodeNumber}"
            else -> null
        }
        val episodeInfo = listOfNotNull(
            seasonEpisodeLabel,
            episode.runTimeTicks?.formatRuntime()
        ).takeIf { it.isNotEmpty() }?.joinToString(" • ")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .dpadNavigation(
                    shape = focusShape,
                    focusRequester = cardFocusRequester,
                    interactionSource = cardInteractionSource,
                    focusColor = animatedHighlightColor,
                    onClick = { onClick(episode) },
                    onMoveFocus = { direction ->
                        if (direction == FocusDirection.Down) {
                            onRequestActionsFocus()
                            true
                        } else {
                            false
                        }
                    }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(4).sdp),
            shape = focusShape
        ) {
            val thumbnailUrl = remember(
                serverUrl,
                episode.id,
                episode.primaryImageTag,
                seasonId,
                seasonBackdropTag
            ) {
                createEpisodeThumbnailUrl(
                    serverUrl = serverUrl,
                    itemId = episode.id,
                    primaryTag = episode.primaryImageTag,
                    seasonId = seasonId,
                    seasonBackdropTag = seasonBackdropTag
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (thumbnailUrl != null) {
                    NetworkImage(
                        data = thumbnailUrl,
                        contentDescription = episode.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        memoryCacheKey = "hero_full:$thumbnailUrl"
                    )
                } else {
                    EmptyItem()
                }
            }
        }

        Text(
            text = episode.name.orEmpty(),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = calculateRoundedValue(8).sdp),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = calculateRoundedValue(14).ssp
            )
        )

        episodeInfo?.let { info ->
            Text(
                text = info,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Start),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = calculateRoundedValue(12).ssp,
                    lineHeight = calculateRoundedValue(12).ssp
                )
            )
        }
    }
}


private fun createEpisodeThumbnailUrl(
    serverUrl: String,
    itemId: String,
    primaryTag: String?,
    seasonId: String?,
    seasonBackdropTag: String?
): String? {
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return when {
        primaryTag != null -> "${baseUrl}Items/$itemId/Images/Primary?tag=$primaryTag&quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}&maxWidth=${ApiConstants.BACKDROP_MAX_WIDTH}"
        seasonId != null && seasonBackdropTag != null -> "${baseUrl}Items/$seasonId/Images/Backdrop?tag=$seasonBackdropTag&quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}&maxWidth=${ApiConstants.BACKDROP_MAX_WIDTH}"
        else -> null
    }
}
