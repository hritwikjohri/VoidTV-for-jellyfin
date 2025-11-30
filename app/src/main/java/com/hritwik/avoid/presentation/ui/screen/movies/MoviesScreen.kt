package com.hritwik.avoid.presentation.ui.screen.movies

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.domain.model.library.LibraryType
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.Studio
import com.hritwik.avoid.presentation.ui.common.focusToSideNavigationOnLeftEdge
import com.hritwik.avoid.presentation.ui.components.common.states.EmptyState
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.media.MediaCardType
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.presentation.ui.components.media.StudioCard
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.extensions.homeContentFocusProperties
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun MoviesScreen(
    onMediaItemClick: (MediaItem) -> Unit = {},
    onMediaItemFocus: (MediaItem) -> Unit = {},
    onStudioClick: (
        libraryId: String,
        libraryName: String,
        additionalLibraryIds: List<String>,
        libraryType: LibraryType,
        studio: Studio
    ) -> Unit = { _, _, _, _, _ -> },
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel,
    sideNavigationFocusRequester: FocusRequester
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.libraryState.collectAsStateWithLifecycle()

    val session = authState.authSession
    val sessionUserId = session?.userId?.id
    val sessionToken = session?.accessToken
    val serverUrl = session?.server?.url ?: ""

    LaunchedEffect(sessionUserId, sessionToken) {
        if (sessionUserId != null && sessionToken != null) {
            libraryViewModel.loadLibraries(sessionUserId, sessionToken)
        }
    }

    val continueWatching = remember(libraryState.resumeItems) {
        libraryState.resumeItems.filter { item ->
            item.type == ApiConstants.ITEM_TYPE_MOVIE
        }
    }
    val recommendedMovies = remember(libraryState.recommendedItems) {
        libraryState.recommendedItems.filter { it.type == ApiConstants.ITEM_TYPE_MOVIE }.take(20)
    }
    val recentlyAdded = remember(libraryState.latestMovies) {
        libraryState.latestMovies.take(20)
    }
    val recentlyReleased = remember(libraryState.recentlyReleasedMovies) {
        libraryState.recentlyReleasedMovies.take(20)
    }
    val studios = remember(libraryState.movieStudios) { libraryState.movieStudios }
    val movieLibraries = remember(libraryState.libraries) {
        libraryState.libraries.filter { it.type == LibraryType.MOVIES }
    }
    val primaryMovieLibrary = movieLibraries.firstOrNull()

    val hasContent = continueWatching.isNotEmpty() ||
            recommendedMovies.isNotEmpty() ||
            recentlyAdded.isNotEmpty() ||
            recentlyReleased.isNotEmpty() ||
            (studios.isNotEmpty() && primaryMovieLibrary != null)

    when {
        libraryState.isLoading && !hasContent -> {
            LoadingState()
        }

        !libraryState.isLoading && !hasContent -> {
            EmptyState(
                title = "No movies available",
                description = "New films will appear here once they're added to your library."
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = calculateRoundedValue(80).sdp)
                    .focusToSideNavigationOnLeftEdge(sideNavigationFocusRequester),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(32).sdp),
                contentPadding = PaddingValues(vertical = calculateRoundedValue(32).sdp)
            ) {
                if (continueWatching.isNotEmpty()) {
                    item(key = "movies_continue_watching") {
                        BackdropRow(
                            title = "Continue Watching",
                            items = continueWatching,
                            serverUrl = serverUrl,
                            sideNavigationFocusRequester = sideNavigationFocusRequester,
                            onMediaItemClick = onMediaItemClick,
                            onMediaItemFocus = onMediaItemFocus,
                            showProgress = true
                        )
                    }
                }

                if (recommendedMovies.isNotEmpty()) {
                    item(key = "movies_recommended") {
                        BackdropRow(
                            title = "Recommended",
                            items = recommendedMovies,
                            serverUrl = serverUrl,
                            sideNavigationFocusRequester = sideNavigationFocusRequester,
                            onMediaItemClick = onMediaItemClick,
                            onMediaItemFocus = onMediaItemFocus,
                            showProgress = false
                        )
                    }
                }

                if (studios.isNotEmpty() && primaryMovieLibrary != null) {
                    item(key = "movies_studios") {
                        StudioRow(
                            title = "Studios",
                            studios = studios,
                            serverUrl = serverUrl,
                            sideNavigationFocusRequester = sideNavigationFocusRequester,
                            onStudioClick = { studio ->
                                val additionalLibraryIds = movieLibraries
                                    .drop(1)
                                    .map { it.id }
                                onStudioClick(
                                    primaryMovieLibrary.id,
                                    primaryMovieLibrary.name,
                                    additionalLibraryIds,
                                    LibraryType.MOVIES,
                                    studio
                                )
                            }
                        )
                    }
                }

                if (recentlyAdded.isNotEmpty()) {
                    item(key = "movies_recently_added") {
                        PosterRow(
                            title = "Recently Added",
                            items = recentlyAdded,
                            serverUrl = serverUrl,
                            sideNavigationFocusRequester = sideNavigationFocusRequester,
                            onMediaItemClick = onMediaItemClick,
                            onMediaItemFocus = onMediaItemFocus
                        )
                    }
                }

                if (recentlyReleased.isNotEmpty()) {
                    item(key = "movies_recently_released") {
                        PosterRow(
                            title = "Recently Released",
                            items = recentlyReleased,
                            serverUrl = serverUrl,
                            sideNavigationFocusRequester = sideNavigationFocusRequester,
                            onMediaItemClick = onMediaItemClick,
                            onMediaItemFocus = onMediaItemFocus
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackdropRow(
    title: String,
    items: List<MediaItem>,
    serverUrl: String,
    sideNavigationFocusRequester: FocusRequester?,
    onMediaItemClick: (MediaItem) -> Unit,
    onMediaItemFocus: (MediaItem) -> Unit,
    showProgress: Boolean
) {
    SectionHeader(title = title) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(24).sdp),
            contentPadding = PaddingValues(
                start = calculateRoundedValue(20).sdp,
                end = calculateRoundedValue(20).sdp
            )
        ) {
            itemsIndexed(items, key = { _, item -> "${title}_${item.id}" }) { index, mediaItem ->
                val itemModifier = if (index == 0) {
                    Modifier.homeContentFocusProperties(sideNavigationFocusRequester)
                } else {
                    Modifier
                }

                MediaItemCard(
                    modifier = itemModifier,
                    mediaItem = mediaItem,
                    serverUrl = serverUrl,
                    cardType = MediaCardType.THUMBNAIL,
                    showProgress = showProgress,
                    onClick = onMediaItemClick,
                    onFocus = onMediaItemFocus
                )
            }
        }
    }
}

@Composable
private fun PosterRow(
    title: String,
    items: List<MediaItem>,
    serverUrl: String,
    sideNavigationFocusRequester: FocusRequester?,
    onMediaItemClick: (MediaItem) -> Unit,
    onMediaItemFocus: (MediaItem) -> Unit
) {
    SectionHeader(title = title) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(24).sdp),
            contentPadding = PaddingValues(
                start = calculateRoundedValue(20).sdp,
                end = calculateRoundedValue(20).sdp
            )
        ) {
            itemsIndexed(items, key = { _, item -> "${title}_${item.id}" }) { index, mediaItem ->
                val itemModifier = if (index == 0) {
                    Modifier.homeContentFocusProperties(sideNavigationFocusRequester)
                } else {
                    Modifier
                }

                MediaItemCard(
                    modifier = itemModifier,
                    mediaItem = mediaItem,
                    serverUrl = serverUrl,
                    cardType = MediaCardType.POSTER,
                    showProgress = false,
                    onClick = onMediaItemClick,
                    onFocus = onMediaItemFocus
                )
            }
        }
    }
}

@Composable
private fun StudioRow(
    title: String,
    studios: List<Studio>,
    serverUrl: String,
    sideNavigationFocusRequester: FocusRequester?,
    onStudioClick: (Studio) -> Unit,
) {
    SectionHeader(title = title) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(24).sdp),
            contentPadding = PaddingValues(
                start = calculateRoundedValue(10).sdp,
                end = calculateRoundedValue(20).sdp
            )
        ) {
            itemsIndexed(studios, key = { _, studio -> "${title}_${studio.id}" }) { index, studio ->
                val itemModifier = if (index == 0) {
                    Modifier.homeContentFocusProperties(sideNavigationFocusRequester)
                } else {
                    Modifier
                }

                StudioCard(
                    modifier = itemModifier,
                    studio = studio,
                    serverUrl = serverUrl,
                    onClick = onStudioClick
                )
            }
        }
    }
}
