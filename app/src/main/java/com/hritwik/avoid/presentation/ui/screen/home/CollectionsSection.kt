package com.hritwik.avoid.presentation.ui.screen.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.media.MediaCardType
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.presentation.ui.state.FeatureContentFocusTarget
import com.hritwik.avoid.utils.extensions.homeContentFocusProperties
import com.hritwik.avoid.utils.extensions.resetFeatureOnFocusExit
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun CollectionsSection(
    items: List<MediaItem>,
    serverUrl: String,
    contentFocusRequester: FocusRequester,
    contentFocusTarget: FeatureContentFocusTarget?,
    sideNavigationFocusRequester: FocusRequester?,
    onCollectionClick: (MediaItem) -> Unit,
    onCollectionFocus: (MediaItem) -> Unit,
    onFocusedItemChange: (MediaItem?) -> Unit,
    onViewAll: (() -> Unit)?
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    var hasBeenFocused by remember { mutableStateOf(false) }

    SectionHeader(
        title = "Collections",
        actionButton = {
            onViewAll?.let {
                TextButton(
                    onClick = it,
                    modifier = Modifier.homeContentFocusProperties(sideNavigationFocusRequester)
                ) {
                    Text(text = "View all")
                }
            }
        }
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .focusGroup()
                .onFocusChanged { state ->
                    if (state.hasFocus && !hasBeenFocused) {
                        hasBeenFocused = true
                        firstItemFocusRequester.requestFocus()
                    }
                }
                .resetFeatureOnFocusExit { onFocusedItemChange(null) },
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(24).sdp),
            contentPadding = PaddingValues(
                start = calculateRoundedValue(20).sdp,
                end = calculateRoundedValue(20).sdp
            )
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> "collection_${item.id}" }
            ) { index, mediaItem ->
                val focusRequester = when {
                    index != 0 -> null
                    contentFocusTarget == FeatureContentFocusTarget.Resume -> contentFocusRequester
                    else -> firstItemFocusRequester
                }
                val itemModifier = if (index == 0) {
                    Modifier.homeContentFocusProperties(
                        sideNavigationFocusRequester = sideNavigationFocusRequester,
                        initialFocusRequester = focusRequester
                    )
                } else {
                    Modifier
                }

                MediaItemCard(
                    modifier = itemModifier,
                    mediaItem = mediaItem,
                    serverUrl = serverUrl,
                    cardType = MediaCardType.POSTER,
                    showProgress = false,
                    badgeNumber = mediaItem.childCount,
                    onClick = onCollectionClick,
                    onFocus = {
                        onCollectionFocus(it)
                        onFocusedItemChange(it)
                    },
                    focusRequester = focusRequester
                )
            }
        }
    }
}
