package com.hritwik.avoid.presentation.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

internal const val OVERVIEW_SECTION_PLACEHOLDER_HEIGHT_DP = 96
internal const val OVERVIEW_SECTION_TOP_PADDING_DP = 8

@Composable
fun OverviewSection(
    currentItem: MediaItem,
    modifier: Modifier = Modifier
) {
    val placeholderHeight = calculateRoundedValue(OVERVIEW_SECTION_PLACEHOLDER_HEIGHT_DP).sdp
    val topPadding = calculateRoundedValue(OVERVIEW_SECTION_TOP_PADDING_DP).sdp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .heightIn(min = placeholderHeight)
    ) {
        currentItem.overview?.let { overview ->
            Text(
                text = overview,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        } ?: Spacer(modifier = Modifier.fillMaxSize())
    }
}
