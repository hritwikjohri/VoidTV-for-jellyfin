package com.hritwik.avoid.presentation.ui.components.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.domain.model.library.MediaItem
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.utils.extensions.getPosterUrl

@Composable
fun SeasonSelectionBar(
    seasons: List<MediaItem>,
    selectedSeasonId: String?,
    onSeasonSelected: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    serverUrl: String,
    focusRequesters: List<FocusRequester> = emptyList(),
    onRequestNavFocus: (() -> Unit)? = null,
    mediaItem: MediaItem? = null,
    dominantColor: Color = MaterialTheme.colorScheme.primary,
    onAccentColorChanged: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    var tabColor by remember { mutableStateOf(dominantColor) }

    val selectedTabIndex = seasons.indexOfFirst { it.id == selectedSeasonId }.let {
        if (it == -1) 0 else it
    }

    LaunchedEffect(dominantColor) {
        tabColor = dominantColor
    }

    LaunchedEffect(mediaItem?.id, serverUrl) {
        val imageUrl = mediaItem?.getPosterUrl(serverUrl)
        val color = extractDominantColor(context, imageUrl)
        if (color != null) {
            tabColor = color
        }
    }

    LaunchedEffect(tabColor) {
        onAccentColorChanged(tabColor)
    }

    val focusOutlineColor by animateColorAsState(
        targetValue = tabColor,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "tab_focus_outline"
    )

    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

    val focusContentColor = remember(focusOutlineColor, onBackgroundColor) {
        val luminance = focusOutlineColor.luminance()
        val blendFactor = if (luminance < 0.3f) 0.35f else 0.15f
        lerp(
            start = focusOutlineColor,
            stop = onBackgroundColor,
            fraction = blendFactor
        )
    }

    SecondaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier.wrapContentSize(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        divider = { null },
        edgePadding = 0.dp,
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                color = tabColor
            )
        }
    ) {
        seasons.forEachIndexed { index, season ->
            val focusRequester = focusRequesters.getOrNull(index)
            val interactionSource = remember(season.id) { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            val tabModifier = Modifier
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && index == 0) {
                        onRequestNavFocus?.invoke()
                        true
                    } else {
                        false
                    }
                }
                .dpadNavigation(
                    focusRequester = focusRequester,
                    onClick = { onSeasonSelected(season) },
                    focusColor = focusOutlineColor,
                    interactionSource = interactionSource
                )

            val isSelected = season.id == selectedSeasonId

            Tab(
                selected = isSelected,
                onClick = { onSeasonSelected(season) },
                text = {
                    Text(
                        text = season.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = when {
                            isSelected -> FontWeight.Bold
                            isFocused -> FontWeight.SemiBold
                            else -> FontWeight.Normal
                        },
                        color = when {
                            isSelected -> tabColor
                            isFocused -> focusContentColor
                            else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        }
                    )
                },
                modifier = tabModifier
            )
        }
    }
}