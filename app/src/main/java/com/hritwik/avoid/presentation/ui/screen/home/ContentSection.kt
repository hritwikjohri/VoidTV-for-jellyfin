package com.hritwik.avoid.presentation.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.Library
import com.hritwik.avoid.presentation.ui.screen.home.HomeMediaRowSection
import com.hritwik.avoid.presentation.ui.state.AuthServerState
import com.hritwik.avoid.presentation.ui.state.FeatureContentFocusTarget
import com.hritwik.avoid.presentation.ui.state.LibraryState
import com.hritwik.avoid.utils.extensions.resetFeatureOnFocusExit
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentSection(
    modifier: Modifier,
    items: List<MediaItem>,
    showThumbnails: Boolean,
    serverUrl: String,
    libraryState: LibraryState,
    authState: AuthServerState,
    contentFocusTarget: FeatureContentFocusTarget?,
    contentFocusRequester: FocusRequester,
    sideNavigationFocusRequester: FocusRequester?,
    onItemSelected: (MediaItem) -> Unit,
    onItemFocused: (MediaItem) -> Unit,
    onMediaItemClick: (MediaItem) -> Unit,
    onMediaItemFocus: (MediaItem) -> Unit,
    onFocusedItemChange: (MediaItem?) -> Unit
) {
    val resumeItems = libraryState.resumeItems
    val nextUpItems = libraryState.nextUpEpisodes
    val latestItemsByLibrary = libraryState.latestItemsByLibrary
    val libraries = libraryState.libraries
    val recentMovies = libraryState.latestMovies.take(10)
    val recentShows = libraryState.recentlyReleasedShows
        .ifEmpty { libraryState.latestItems.filter { it.type == "Series" } }
        .take(10)
    val recentEpisodes = libraryState.latestEpisodes
        .ifEmpty { libraryState.latestItems.filter { it.type == "Episode" } }
        .take(10)
    val contentServerUrl = authState.authSession?.server?.url ?: serverUrl

    val lazyListState = rememberLazyListState()
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .resetFeatureOnFocusExit { onFocusedItemChange(null) },
        state = lazyListState,
        flingBehavior = snapFlingBehavior,
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(42).sdp),
        contentPadding = PaddingValues(bottom = calculateRoundedValue(40).sdp)
    ) {
        if (items.size > 1 && showThumbnails) {
            item(key = "thumbnails") {
                ThumbnailRow(
                    items = items,
                    serverUrl = serverUrl,
                    contentFocusTarget = contentFocusTarget,
                    contentFocusRequester = contentFocusRequester,
                    sideNavigationFocusRequester = sideNavigationFocusRequester,
                    onItemSelected = onItemSelected,
                    onItemFocused = onItemFocused,
                    onFocusedItemChange = onFocusedItemChange
                )
            }
        }

        item{
            Spacer(modifier = modifier.height(calculateRoundedValue(8).sdp))
        }

        if (resumeItems.isNotEmpty()) {
            item(key = "continue_watching") {
                HomeMediaRowSection(
                    title = "Continue Watching",
                    keyPrefix = "resume",
                    items = resumeItems,
                    serverUrl = contentServerUrl,
                    contentFocusTarget = contentFocusTarget,
                    contentFocusRequester = contentFocusRequester,
                    sideNavigationFocusRequester = sideNavigationFocusRequester,
                    onMediaItemClick = onMediaItemClick,
                    onMediaItemFocus = onMediaItemFocus,
                    onFocusedItemChange = onFocusedItemChange,
                    focusTargetOverride = FeatureContentFocusTarget.Resume,
                    showProgress = true,
                    showTitle = true
                )
            }
        }

        if (nextUpItems.isNotEmpty()) {
            item(key = "next_up") {
                HomeMediaRowSection(
                    title = "Next up",
                    keyPrefix = "next_up",
                    items = nextUpItems,
                    serverUrl = contentServerUrl,
                    contentFocusTarget = contentFocusTarget,
                    contentFocusRequester = contentFocusRequester,
                    sideNavigationFocusRequester = sideNavigationFocusRequester,
                    onMediaItemClick = onMediaItemClick,
                    onMediaItemFocus = onMediaItemFocus,
                    onFocusedItemChange = onFocusedItemChange,
                    focusTargetOverride = FeatureContentFocusTarget.Resume,
                    showProgress = true,
                    showTitle = true
                )
            }
        }

        if (latestItemsByLibrary.isNotEmpty()) {
            libraries.forEach { library: Library ->
                val itemsForLibrary = latestItemsByLibrary[library.id].orEmpty()
                if (itemsForLibrary.isNotEmpty()) {
                    item(key = "recent_${library.id}") {
                        HomeMediaRowSection(
                            title = "Recently added in ${library.name}",
                            keyPrefix = "recent_${library.id}",
                            items = itemsForLibrary.take(10),
                            serverUrl = contentServerUrl,
                            contentFocusTarget = contentFocusTarget,
                            contentFocusRequester = contentFocusRequester,
                            sideNavigationFocusRequester = sideNavigationFocusRequester,
                            onMediaItemClick = onMediaItemClick,
                            onMediaItemFocus = onMediaItemFocus,
                            onFocusedItemChange = onFocusedItemChange,
                            focusTargetOverride = FeatureContentFocusTarget.Latest,
                            showProgress = true,
                            showTitle = true
                        )
                    }
                }
            }
        } else {
            if (recentMovies.isNotEmpty()) {
                item(key = "Movies") {
                    HomeMediaRowSection(
                        title = "Recently added Movies",
                        keyPrefix = "recent_movie",
                        items = recentMovies,
                        serverUrl = contentServerUrl,
                        contentFocusTarget = contentFocusTarget,
                        contentFocusRequester = contentFocusRequester,
                        sideNavigationFocusRequester = sideNavigationFocusRequester,
                        onMediaItemClick = onMediaItemClick,
                        onMediaItemFocus = onMediaItemFocus,
                        onFocusedItemChange = onFocusedItemChange,
                        focusTargetOverride = FeatureContentFocusTarget.Resume,
                        showProgress = true,
                        showTitle = true
                    )
                }
            }

            if (recentShows.isNotEmpty()) {
                item(key = "Shows"){
                    HomeMediaRowSection(
                        title = "Recently added Shows",
                        keyPrefix = "recent_show",
                        items = recentShows,
                        serverUrl = contentServerUrl,
                        contentFocusTarget = contentFocusTarget,
                        contentFocusRequester = contentFocusRequester,
                        sideNavigationFocusRequester = sideNavigationFocusRequester,
                        onMediaItemClick = onMediaItemClick,
                        onMediaItemFocus = onMediaItemFocus,
                        onFocusedItemChange = onFocusedItemChange,
                        focusTargetOverride = FeatureContentFocusTarget.Resume,
                        showProgress = true,
                        showTitle = true
                    )
                }
            }

            if (recentEpisodes.isNotEmpty()) {
                item(key = "Episodes"){
                    HomeMediaRowSection(
                        title = "Recently added Episodes",
                        keyPrefix = "recent_episode",
                        items = recentEpisodes,
                        serverUrl = contentServerUrl,
                        contentFocusTarget = contentFocusTarget,
                        contentFocusRequester = contentFocusRequester,
                        sideNavigationFocusRequester = sideNavigationFocusRequester,
                        onMediaItemClick = onMediaItemClick,
                        onMediaItemFocus = onMediaItemFocus,
                        onFocusedItemChange = onFocusedItemChange,
                        focusTargetOverride = FeatureContentFocusTarget.Resume,
                        showProgress = true,
                        showTitle = true
                    )
                }
            }
        }

        item{
            Spacer(modifier = modifier.height(calculateRoundedValue(8).sdp))
        }
    }
}
