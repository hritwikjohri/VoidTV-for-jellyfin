package com.hritwik.avoid.presentation.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.state.FeatureContentFocusTarget
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun HeroContent(
    currentItem: MediaItem,
    serverUrl: String,
    watchNowFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester,
    sideNavigationFocusRequester: FocusRequester?,
    onMediaItemClick: (MediaItem) -> Unit
) {
    var hasContentFocusTarget by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(
                min = calculateRoundedValue(400).sdp,
                max = calculateRoundedValue(400).sdp
            )
            .padding(calculateRoundedValue(12).sdp)
    ) {
        LogoSection(
            currentItem = currentItem,
            serverUrl = serverUrl
        )

        Spacer(Modifier.height(calculateRoundedValue(16).sdp))

        val overviewPlaceholderHeight = calculateRoundedValue(OVERVIEW_SECTION_PLACEHOLDER_HEIGHT_DP).sdp
        val overviewTopPadding = calculateRoundedValue(OVERVIEW_SECTION_TOP_PADDING_DP).sdp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = overviewPlaceholderHeight + overviewTopPadding)
        ) {
            OverviewSection(
                currentItem = currentItem,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(calculateRoundedValue(32).sdp))

        ButtonsSection(
            currentItem = currentItem,
            watchNowFocusRequester = watchNowFocusRequester,
            contentFocusRequester = contentFocusRequester,
            hasContentFocusTarget = hasContentFocusTarget,
            sideNavigationFocusRequester = sideNavigationFocusRequester,
            onMediaItemClick = onMediaItemClick
        )
    }
}