package com.hritwik.avoid.presentation.ui.screen.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.LoadState
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.model.library.LibraryType
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.ContentGrid
import com.hritwik.avoid.presentation.ui.components.common.DiskContentGrid
import com.hritwik.avoid.presentation.ui.components.common.SectionedContentGrid
import com.hritwik.avoid.presentation.ui.components.common.ErrorDisplay
import com.hritwik.avoid.presentation.ui.components.common.ScreenHeader
import com.hritwik.avoid.presentation.ui.components.common.states.EmptyLibraryState
import com.hritwik.avoid.presentation.ui.components.common.states.EmptyState
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
import com.hritwik.avoid.presentation.ui.common.focusToSideNavigationOnLeftEdge
import com.hritwik.avoid.presentation.ui.state.LibraryGenresState
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun LibraryScreen(
    libraryId: String,
    libraryName: String,
    studio: String? = null,
    libraryIds: List<String> = listOf(libraryId),
    libraryType: LibraryType? = null,
    onBackClick: () -> Unit = {},
    onMediaItemClick: (MediaItem) -> Unit = {},
    onMediaItemFocus: (MediaItem) -> Unit = {},
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    sideNavigationFocusRequester: FocusRequester? = null,
    preserveCache: Boolean = false,
    onCacheHandled: () -> Unit = {},
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val pagerCacheVersion by libraryViewModel.pagerCacheVersion.collectAsStateWithLifecycle()
    val itemsState by libraryViewModel.itemsState.collectAsStateWithLifecycle()
    val effectiveLibraryIds = remember(libraryId, libraryIds) {
        libraryIds.filter { it.isNotBlank() }.ifEmpty { listOf(libraryId) }
    }
    val includeItemTypes = remember(libraryType) {
        when (libraryType) {
            LibraryType.MOVIES -> ApiConstants.ITEM_TYPE_MOVIE
            LibraryType.TV_SHOWS -> ApiConstants.ITEM_TYPE_SERIES
            else -> null
        }
    }
    val gridState = rememberSaveable(libraryId, studio, effectiveLibraryIds, saver = LazyGridState.Saver) {
        LazyGridState()
    }
    val genresState by libraryViewModel.genresState.collectAsStateWithLifecycle()
    val currentGenresState = genresState[libraryId] ?: LibraryGenresState()
    var reloadTrigger by remember { mutableStateOf(0) }

    var selectedSortId by rememberSaveable(libraryId, studio, effectiveLibraryIds) { mutableStateOf(LibrarySortOptions.SortName.id) }
    val selectedSortOption = remember(selectedSortId) {
        LibrarySortOptions.All.firstOrNull { it.id == selectedSortId } ?: LibrarySortOptions.SortName
    }
    var sortDirectionName by rememberSaveable(libraryId, studio, effectiveLibraryIds) { mutableStateOf(selectedSortOption.defaultDirection.name) }
    LaunchedEffect(selectedSortOption.id) {
        sortDirectionName = selectedSortOption.defaultDirection.name
    }
    val sortDirection = runCatching { LibrarySortDirection.valueOf(sortDirectionName) }
        .getOrDefault(selectedSortOption.defaultDirection)
    var selectedGenre by rememberSaveable(libraryId, studio, effectiveLibraryIds) { mutableStateOf<String?>(null) }

    LaunchedEffect(libraryId, authState.authSession) {
        authState.authSession?.let { session ->
            libraryViewModel.loadLibraryGenres(
                userId = session.userId.id,
                libraryId = libraryId,
                accessToken = session.accessToken
            )
        }
    }

    
    
    LaunchedEffect(libraryId, authState.authSession, selectedSortOption, sortDirection, selectedGenre, studio, reloadTrigger) {
        if (reloadTrigger == 0) return@LaunchedEffect
        authState.authSession?.let { session ->
            libraryViewModel.loadLibraryItems(
                userId = session.userId.id,
                libraryId = libraryId,
                accessToken = session.accessToken,
                sortBy = selectedSortOption.sortFields,
                sortOrder = sortDirection,
                supportsAlphaScroller = selectedSortOption.supportsAlphaScroller,
                genre = selectedGenre,
                studio = studio,
                skipCacheRefresh = false
            )
        }
    }

    var previousSortId by remember { mutableStateOf(selectedSortId) }
    var previousSortDirection by remember { mutableStateOf(sortDirectionName) }
    var previousGenre by remember { mutableStateOf(selectedGenre) }
    var previousStudio by remember { mutableStateOf(studio) }
    var hasRecordedInitialSelection by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSortId, sortDirectionName, selectedGenre, studio) {
        if (hasRecordedInitialSelection) {
            val hasSelectionChanged =
                selectedSortId != previousSortId ||
                        sortDirectionName != previousSortDirection ||
                        selectedGenre != previousGenre ||
                        studio != previousStudio

            if (hasSelectionChanged) {
                gridState.scrollToItem(0)
                reloadTrigger++
            }
        } else {
            hasRecordedInitialSelection = true
        }

        previousSortId = selectedSortId
        previousSortDirection = sortDirectionName
        previousGenre = selectedGenre
        previousStudio = studio
    }

    LaunchedEffect(libraryId, studio, preserveCache, effectiveLibraryIds) {
        if (preserveCache) {
            onCacheHandled()
        } else {
            libraryViewModel.clearLibraryPagerCache()
            reloadTrigger++
            onCacheHandled()
        }
    }

    val useDiskGrid = selectedSortOption.supportsAlphaScroller

    val pagingItems = authState.authSession?.let { session ->
        if (useDiskGrid) {
            null
        } else {
            val pager = remember(
                libraryId,
                effectiveLibraryIds,
                session.userId.id,
                session.accessToken,
                selectedSortOption.id,
                sortDirectionName,
                selectedGenre,
                studio,
                includeItemTypes,
                pagerCacheVersion
            ) {
                libraryViewModel.libraryItemsPager(
                    userId = session.userId.id,
                    libraryId = libraryId,
                    libraryIds = effectiveLibraryIds,
                    accessToken = session.accessToken,
                    sortBy = selectedSortOption.sortFields,
                    sortOrder = sortDirection,
                    genre = selectedGenre,
                    studio = studio,
                    includeItemTypes = includeItemTypes
                )
            }
            pager.collectAsLazyPagingItems()
        }
    }

    val baseLibraryName = if (effectiveLibraryIds.size > 1) {
        when (libraryType) {
            LibraryType.MOVIES -> "Movies"
            LibraryType.TV_SHOWS -> "Shows"
            else -> libraryName
        }
    } else {
        libraryName
    }
    val headerTitle = if (studio.isNullOrBlank()) {
        baseLibraryName
    } else {
        "$baseLibraryName • $studio"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = calculateRoundedValue(80).sdp)
            .focusToSideNavigationOnLeftEdge(sideNavigationFocusRequester)
    ) {
        ScreenHeader(
            title = headerTitle,
            showBackButton = false,
            onBackClick = onBackClick,
            showSortBar = true,
            sortOptions = LibrarySortOptions.All,
            selectedSort = selectedSortOption,
            sortDirection = sortDirection,
            onSortSelected = { option -> selectedSortId = option.id },
            onSortDirectionChange = { direction -> sortDirectionName = direction.name },
            genres = currentGenresState.genres,
            selectedGenre = selectedGenre,
            onGenreSelected = { genre -> selectedGenre = genre },
            isGenresLoading = currentGenresState.isLoading,
            genreError = currentGenresState.error,
            onRetryGenres = {
                authState.authSession?.let { session ->
                    libraryViewModel.loadLibraryGenres(
                        userId = session.userId.id,
                        libraryId = libraryId,
                        accessToken = session.accessToken,
                        forceRefresh = true
                    )
                }
            }
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            if (useDiskGrid) {
                when {
                    itemsState.isLoading -> LoadingState()
                    itemsState.totalCount == 0 -> {
                        EmptyLibraryState(libraryName)
                    }
                    else -> {
                        DiskContentGrid(
                            totalCount = itemsState.totalCount,
                            windowCache = itemsState.windowCache,
                            requestWindow = { index -> libraryViewModel.requestWindow(index) },
                            gridState = gridState,
                            serverUrl = authState.authSession?.server?.url ?: "",
                            precomputedIndexMap = itemsState.sectionHeaderIndices,
                            onMediaItemClick = onMediaItemClick,
                            onMediaItemFocus = onMediaItemFocus
                        )
                    }
                }
            } else {
                when {
                    pagingItems == null || pagingItems.loadState.refresh is LoadState.Loading -> {
                        LoadingState()
                    }
                    pagingItems.itemCount == 0 -> {
                        if (!studio.isNullOrBlank()) {
                            val contentLabel = when (libraryType) {
                                LibraryType.MOVIES -> "movies"
                                LibraryType.TV_SHOWS -> "series"
                                else -> "titles"
                            }
                            val scopeLabel = if (effectiveLibraryIds.size > 1) {
                                "across your libraries"
                            } else {
                                "in $libraryName"
                            }
                            EmptyState(
                                title = "No ${contentLabel.replaceFirstChar { it.uppercase() }} found",
                                description = "We couldn't find any $contentLabel from $studio $scopeLabel.",
                            )
                        } else {
                            EmptyLibraryState(libraryName)
                        }
                    }
                    else -> {
                        ContentGrid(
                            items = pagingItems,
                            gridState = gridState,
                            serverUrl = authState.authSession?.server?.url ?: "",
                            allTitles = itemsState.allTitles,  
                            precomputedIndexMap = itemsState.sectionHeaderIndices,
                            totalCount = itemsState.totalCount,
                            onMediaItemClick = onMediaItemClick,
                            onMediaItemFocus = onMediaItemFocus
                        )
                    }
                }
            }
        }

        if (!useDiskGrid && pagingItems != null && pagingItems.loadState.refresh is LoadState.Error) {
            val message = (pagingItems.loadState.refresh as LoadState.Error).error.message ?: ""
            ErrorDisplay(
                error = message,
                onDismiss = { pagingItems.retry() }
            )
        }
    }
}
