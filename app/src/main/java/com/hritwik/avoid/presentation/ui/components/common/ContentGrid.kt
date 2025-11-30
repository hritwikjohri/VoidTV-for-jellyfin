package com.hritwik.avoid.presentation.ui.components.common

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.paging.compose.LazyPagingItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.presentation.ui.state.LibraryGridItem
import com.hritwik.avoid.utils.constants.AppConstants


private const val POSTER_ASPECT_RATIO = 2f / 3.15f

@Composable
fun ContentGrid(
    items: LazyPagingItems<MediaItem>,
    gridState: LazyGridState,
    serverUrl: String,
    allTitles: List<String> = emptyList(),  
    precomputedIndexMap: Map<Char, Int> = emptyMap(),
    totalCount: Int = 0,
    onMediaItemClick: (MediaItem) -> Unit,
    onMediaItemFocus: (MediaItem) -> Unit = {}
) {
    var overlayLetter by remember { mutableStateOf<Char?>(null) }
    val firstItemFocusRequester = remember { FocusRequester() }
    val alphaScrollerRequester = remember { FocusRequester() }
    var lastFocusedItemId by remember { mutableStateOf<String?>(null) }

    val firstItemId = items.itemSnapshotList.items.firstOrNull()?.id

    
    val showAlphaScroller = allTitles.isNotEmpty() || precomputedIndexMap.isNotEmpty()

    LaunchedEffect(firstItemId) {
        if (firstItemId != null && firstItemId != lastFocusedItemId) {
            firstItemFocusRequester.requestFocus()
            lastFocusedItemId = firstItemId
        }
        if (firstItemId == null) {
            lastFocusedItemId = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val columns = 9

        Row (modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = if (showAlphaScroller) Modifier.weight(0.99f) else Modifier.weight(1f),
                contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
            ) {
                items(items.itemCount) { index ->
                    items[index]?.let { mediaItem ->
                        val isLastColumn = (index + 1) % columns == 0
                        val isLastItem = index == items.itemCount - 1
                        val isCollection = mediaItem.type.equals("BoxSet", ignoreCase = true)
                        val focusModifier = if (showAlphaScroller && (isLastColumn || isLastItem)) {
                            Modifier.focusProperties { right = alphaScrollerRequester }
                        } else {
                            Modifier
                        }

                        MediaItemCard(
                            modifier = focusModifier,
                            mediaItem = mediaItem,
                            showProgress = !isCollection,
                            badgeNumber = if (isCollection) mediaItem.childCount else null,
                            serverUrl = serverUrl,
                            onClick = onMediaItemClick,
                            onFocus = onMediaItemFocus,
                            focusRequester = if (index == 0) firstItemFocusRequester else null
                        )
                    }
                }
            }

            if (showAlphaScroller) {
                AlphaScroller(
                    titles = allTitles,  
                    totalCount = if (allTitles.isNotEmpty()) allTitles.size else totalCount,
                    precomputedIndexMap = if (allTitles.isEmpty()) precomputedIndexMap else emptyMap(),
                    gridState = gridState,
                    modifier = Modifier.weight(0.1f),
                    focusRequester = alphaScrollerRequester,
                    onActiveLetterChange = { overlayLetter = it }
                )
            }
        }



        AnimatedVisibility(
            visible = overlayLetter != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(calculateRoundedValue(24).sdp),
                tonalElevation = calculateRoundedValue(6).sdp
            ) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = calculateRoundedValue(28).sdp,
                            vertical = calculateRoundedValue(16).sdp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = overlayLetter?.toString() ?: "",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }
        }
    }
}




@Composable
fun DiskContentGrid(
    totalCount: Int,
    windowCache: Map<Int, MediaItem>,
    requestWindow: (Int) -> Unit,
    gridState: LazyGridState,
    serverUrl: String,
    precomputedIndexMap: Map<Char, Int>,
    onMediaItemClick: (MediaItem) -> Unit,
    onMediaItemFocus: (MediaItem) -> Unit = {}
) {
    var overlayLetter by remember { mutableStateOf<Char?>(null) }
    val firstItemFocusRequester = remember { FocusRequester() }
    val alphaScrollerRequester = remember { FocusRequester() }
    var lastRequestedWindow by remember { mutableStateOf<IntRange?>(null) }

    
    LaunchedEffect(gridState, totalCount) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
            .map { infos -> infos.firstOrNull()?.index to infos.lastOrNull()?.index }
            .distinctUntilChanged()
            .collectLatest { (first, last) ->
                val center = when {
                    first != null && last != null -> (first + last) / 2
                    first != null -> first
                    else -> 0
                }
                val windowStart = (center / AppConstants.GRID_CACHE_WINDOW) * AppConstants.GRID_CACHE_WINDOW
                val window = windowStart until (windowStart + AppConstants.GRID_CACHE_WINDOW)
                if (window != lastRequestedWindow) {
                    lastRequestedWindow = window
                    requestWindow(window.first)
                }
            }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val columns = 9

        Row(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier.weight(0.99f),
                contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
            ) {
                items(totalCount) { index ->
                    val mediaItem = windowCache[index]
                    if (mediaItem != null) {
                        val isLastColumn = (index + 1) % columns == 0
                        val isLastItem = index == totalCount - 1
                        val isCollection = mediaItem.type.equals("BoxSet", ignoreCase = true)
                        val focusModifier = if (isLastColumn || isLastItem) {
                            Modifier.focusProperties { right = alphaScrollerRequester }
                        } else {
                            Modifier
                        }
                        MediaItemCard(
                            modifier = focusModifier,
                            mediaItem = mediaItem,
                            showProgress = !isCollection,
                            badgeNumber = if (isCollection) mediaItem.childCount else null,
                            serverUrl = serverUrl,
                            onClick = onMediaItemClick,
                            onFocus = onMediaItemFocus,
                            focusRequester = if (index == 0) firstItemFocusRequester else null
                        )
                    } else {
                        PosterPlaceholder()
                        LaunchedEffect(index) {
                            requestWindow(index)
                        }
                    }
                }
            }

            if (precomputedIndexMap.isNotEmpty()) {
                AlphaScroller(
                    titles = emptyList(),
                    totalCount = totalCount,
                    precomputedIndexMap = precomputedIndexMap,
                    gridState = gridState,
                    modifier = Modifier.weight(0.1f),
                    focusRequester = alphaScrollerRequester,
                    onActiveLetterChange = { overlayLetter = it }
                )
            }
        }

        AnimatedVisibility(
            visible = overlayLetter != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(calculateRoundedValue(24).sdp),
                tonalElevation = calculateRoundedValue(6).sdp
            ) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = calculateRoundedValue(28).sdp,
                            vertical = calculateRoundedValue(16).sdp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = overlayLetter?.toString() ?: "",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PosterPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(POSTER_ASPECT_RATIO),
        shape = RoundedCornerShape(calculateRoundedValue(12).dp)
    ) {
        EmptyItem()
    }
}












@Composable
fun SectionedContentGrid(
    sectionedItems: List<LibraryGridItem>,
    sectionHeaderIndices: Map<Char, Int>,
    gridState: LazyGridState,
    serverUrl: String,
    onMediaItemClick: (MediaItem) -> Unit,
    onMediaItemFocus: (MediaItem) -> Unit = {}
) {
    var overlayLetter by remember { mutableStateOf<Char?>(null) }
    val firstItemFocusRequester = remember { FocusRequester() }
    val alphaScrollerRequester = remember { FocusRequester() }

    
    val showAlphaScroller = sectionedItems.isNotEmpty()

    
    LaunchedEffect(sectionedItems) {
        val firstMediaIndex = sectionedItems.indexOfFirst { it is LibraryGridItem.Media }
        if (firstMediaIndex >= 0) {
            firstItemFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val columns = 9

        Row(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = if (showAlphaScroller) Modifier.weight(0.99f) else Modifier.weight(1f),
                contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
            ) {
                sectionedItems.forEachIndexed { index, gridItem ->
                    when (gridItem) {
                        is LibraryGridItem.Header -> {
                            
                            item(
                                key = gridItem.id,
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                SectionHeader(letter = gridItem.letter)
                            }
                        }

                        is LibraryGridItem.Media -> {
                            
                            item(key = gridItem.id) {
                                val mediaItem = gridItem.item
                                val isLastColumn = (index + 1) % columns == 0
                                val isLastItem = index == sectionedItems.lastIndex
                                val focusModifier = if (showAlphaScroller && (isLastColumn || isLastItem)) {
                                    Modifier.focusProperties { right = alphaScrollerRequester }
                                } else {
                                    Modifier
                                }

                                
                                val isFirstMedia = sectionedItems.take(index + 1)
                                    .count { it is LibraryGridItem.Media } == 1

                                MediaItemCard(
                                    modifier = focusModifier,
                                    mediaItem = mediaItem,
                                    showProgress = false,
                                    serverUrl = serverUrl,
                                    onClick = onMediaItemClick,
                                    onFocus = onMediaItemFocus,
                                    focusRequester = if (isFirstMedia) firstItemFocusRequester else null
                                )
                            }
                        }
                    }
                }
            }

            if (showAlphaScroller) {
                AlphaScrollerWithSections(
                    sectionHeaderIndices = sectionHeaderIndices,
                    gridState = gridState,
                    modifier = Modifier.weight(0.1f),
                    focusRequester = alphaScrollerRequester,
                    onActiveLetterChange = { overlayLetter = it }
                )
            }
        }

        
        AnimatedVisibility(
            visible = overlayLetter != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(calculateRoundedValue(24).sdp),
                tonalElevation = calculateRoundedValue(6).sdp
            ) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = calculateRoundedValue(28).sdp,
                            vertical = calculateRoundedValue(16).sdp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = overlayLetter?.toString() ?: "",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }
        }
    }
}





@Composable
private fun SectionHeader(letter: Char) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = calculateRoundedValue(8).sdp,
                bottom = calculateRoundedValue(4).sdp
            )
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
