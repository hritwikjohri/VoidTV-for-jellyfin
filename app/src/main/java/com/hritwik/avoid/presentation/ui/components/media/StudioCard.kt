package com.hritwik.avoid.presentation.ui.components.media

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import com.hritwik.avoid.domain.model.library.Studio
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    studio: Studio,
    serverUrl: String,
    onClick: (Studio) -> Unit = {},
    onFocus: (Studio) -> Unit = {},
    focusRequester: FocusRequester? = null
) {
    val defaultWidth = remember { calculateRoundedValue(280).dp }
    val cardShape = remember { RoundedCornerShape(calculateRoundedValue(16).dp) }

    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.08f else 1f, label = "studio_card_focus")

    Column(
        modifier = modifier
            .width(defaultWidth)
            .dpadNavigation(
                shape = cardShape,
                focusRequester = focusRequester,
                onClick = { onClick(studio) },
                showFocusOutline = false,
                onFocusChange = { focused ->
                    isFocused = focused
                    if (focused) onFocus(studio)
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(if (isFocused) 1f else 0f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    clip = false
                }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                shape = cardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(4).dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                val imageUrl = remember(serverUrl, studio.id, studio.name, studio.imageTag) {
                    studio.getThumbUrl(serverUrl)
                }
                NetworkImage(
                    data = imageUrl,
                    contentDescription = studio.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(calculateRoundedValue(10).dp))

        Text(
            text = studio.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = calculateRoundedValue(4).sdp)
        )
    }
}
