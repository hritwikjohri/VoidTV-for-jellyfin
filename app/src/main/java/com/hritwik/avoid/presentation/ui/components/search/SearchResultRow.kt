package com.hritwik.avoid.presentation.ui.components.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.EmptyItem
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.utils.helpers.LocalImageHelper
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import ir.kaaveh.sdpcompose.sdp

@Composable
fun SearchResultRow(
    mediaItem: MediaItem,
    serverUrl: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    onClick: (MediaItem) -> Unit = {}
) {
    val imageHelper = LocalImageHelper.current
    val isEpisode = mediaItem.type == "Episode"
    val aspectRatio = if (isEpisode) 16f / 9f else 2f / 3f

    val imageUrl = remember(serverUrl, mediaItem.id, mediaItem.primaryImageTag, mediaItem.backdropImageTags) {
        if (isEpisode) {
            imageHelper.createBackdropUrl(
                serverUrl,
                mediaItem.id,
                mediaItem.backdropImageTags.firstOrNull()
            ) ?: imageHelper.createPosterUrl(serverUrl, mediaItem.id, mediaItem.primaryImageTag)
        } else {
            imageHelper.createPosterUrl(serverUrl, mediaItem.id, mediaItem.primaryImageTag)
        }
    }

    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.03f else 1f, label = "search_result_focus")
    val focusModifier = focusRequester?.let {
        Modifier
            .focusRequester(it)
            .focusProperties {
                if (leftFocusRequester != null) {
                    left = leftFocusRequester
                }
                if (upFocusRequester != null) {
                    up = upFocusRequester
                }
            }
    } ?: Modifier


    Row(
        modifier = modifier
            .then(focusModifier)
            .fillMaxWidth()
            .padding(horizontal = calculateRoundedValue(8).sdp, vertical = calculateRoundedValue(4).sdp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .dpadNavigation(
                focusColor = Color.Transparent,
                shape = RoundedCornerShape(calculateRoundedValue(16).sdp),
                onClick = { onClick(mediaItem) },
                onFocusChange = { focused -> isFocused = focused }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(calculateRoundedValue(8).sdp),
            elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(4).sdp),
            modifier = Modifier
                .width(if (isEpisode) calculateRoundedValue(180).sdp else calculateRoundedValue(100).sdp)
                .aspectRatio(aspectRatio)
        ) {
            if (imageUrl != null) {
                NetworkImage(
                    data = imageUrl,
                    contentDescription = mediaItem.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                EmptyItem()
            }
        }

        Spacer(modifier = Modifier.width(calculateRoundedValue(12).sdp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaItem.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val meta = listOfNotNull(
                mediaItem.type.takeIf { it.isNotBlank() },
                mediaItem.year?.toString()
            ).joinToString(" • ")

            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

