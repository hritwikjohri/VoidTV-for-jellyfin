package com.hritwik.avoid.presentation.ui.screen.category

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.hritwik.avoid.domain.model.category.CategoryProvider
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.common.focusToSideNavigationOnLeftEdge
import com.hritwik.avoid.presentation.ui.components.common.ContentGrid
import com.hritwik.avoid.presentation.ui.components.common.ErrorDisplay
import com.hritwik.avoid.presentation.ui.components.common.ScreenHeader
import com.hritwik.avoid.presentation.ui.components.common.states.EmptyState
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
import com.hritwik.avoid.presentation.ui.screen.library.LibrarySortOptions
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.CategoryViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun CategoryScreen(
    categoryId: String,
    onBackClick: () -> Unit = {},
    onMediaItemClick: (MediaItem) -> Unit = {},
    onMediaItemFocus: (MediaItem) -> Unit = {},
    authViewModel: AuthServerViewModel,
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    sideNavigationFocusRequester: FocusRequester? = null,
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val gridState = rememberSaveable(categoryId, saver = LazyGridState.Saver) { LazyGridState() }

    val category = remember(categoryId) {
        CategoryProvider.getCategories().find { it.id == categoryId }
    }

    val isMoviesCategory = category?.id == "movies"
    val isTvCategory = category?.id == "tv_shows"
    val usesCuratedLayout = isMoviesCategory || isTvCategory

    LaunchedEffect(authState.authSession, usesCuratedLayout) {
        if (!usesCuratedLayout) return@LaunchedEffect

        authState.authSession?.let { session ->
            libraryViewModel.loadLibraries(session.userId.id, session.accessToken)
        }
    }

    var selectedSortId by rememberSaveable(categoryId) { mutableStateOf(LibrarySortOptions.SortName.id) }
    val selectedSortOption = remember(selectedSortId) {
        LibrarySortOptions.All.firstOrNull { it.id == selectedSortId } ?: LibrarySortOptions.SortName
    }
    var sortDirectionName by rememberSaveable(categoryId) { mutableStateOf(selectedSortOption.defaultDirection.name) }

    LaunchedEffect(selectedSortOption.id) {
        sortDirectionName = selectedSortOption.defaultDirection.name
    }

    val sortDirection = runCatching { LibrarySortDirection.valueOf(sortDirectionName) }.getOrDefault(selectedSortOption.defaultDirection)
    var previousSortId by remember { mutableStateOf(selectedSortId) }
    var previousSortDirection by remember { mutableStateOf(sortDirectionName) }
    var hasRecordedInitialSelection by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSortId, sortDirectionName) {
        if (hasRecordedInitialSelection) {
            val hasSelectionChanged =
                selectedSortId != previousSortId ||
                        sortDirectionName != previousSortDirection

            if (hasSelectionChanged) {
                gridState.scrollToItem(0)
            }
        } else {
            hasRecordedInitialSelection = true
        }

        previousSortId = selectedSortId
        previousSortDirection = sortDirectionName
    }

    val pagingItems = authState.authSession?.let { session ->
        category?.let { categoryItem ->
            val pager = remember(
                categoryId,
                session.userId.id,
                session.accessToken,
                selectedSortOption.id,
                sortDirectionName
            ) {
                categoryViewModel.categoryItemsPager(
                    userId = session.userId.id,
                    accessToken = session.accessToken,
                    filters = categoryItem.searchFilters,
                    sortBy = selectedSortOption.sortFields,
                    sortOrder = sortDirection
                )
            }
            pager.collectAsLazyPagingItems()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = calculateRoundedValue(80).sdp)
                .focusToSideNavigationOnLeftEdge(sideNavigationFocusRequester)
        ) {
            ScreenHeader(
                title = category?.name ?: "Category",
                showBackButton = false,
                onBackClick = onBackClick,
                showSortBar = true,
                sortOptions = LibrarySortOptions.All,
                selectedSort = selectedSortOption,
                sortDirection = sortDirection,
                onSortSelected = { option -> selectedSortId = option.id },
                onSortDirectionChange = { direction -> sortDirectionName = direction.name },
                modifier = Modifier.padding(top = calculateRoundedValue(6).sdp)
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    pagingItems == null || pagingItems.loadState.refresh is LoadState.Loading -> {
                        LoadingState()
                    }

                    pagingItems.itemCount == 0 -> {
                        EmptyState(
                            title = category?.name ?: "Category",
                            description = "No content found for this category."
                        )
                    }

                    else -> {
                        ContentGrid(
                            items = pagingItems,
                            gridState = gridState,
                            serverUrl = authState.authSession?.server?.url ?: "",
                            onMediaItemClick = onMediaItemClick,
                            onMediaItemFocus = onMediaItemFocus
                        )
                    }
                }
            }

            if (pagingItems != null && pagingItems.loadState.refresh is LoadState.Error) {
                val message = (pagingItems.loadState.refresh as LoadState.Error).error.message ?: ""
                ErrorDisplay(
                    error = message,
                    onDismiss = { pagingItems.retry() }
                )
            }
        }
    }
}
