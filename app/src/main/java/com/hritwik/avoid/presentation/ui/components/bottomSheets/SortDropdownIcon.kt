package com.hritwik.avoid.presentation.ui.components.bottomSheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hritwik.avoid.presentation.ui.common.focusAwareIconButton
import com.hritwik.avoid.presentation.ui.screen.library.LibrarySortOption
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

private val SortAccentPalette = listOf(
    Color(0xFF6C63FF),
    Color(0xFF4DD0E1),
    Color(0xFFFF8A65),
    Color(0xFF7E57C2),
    Color(0xFF26A69A),
    Color(0xFFFFC400)
)

@Composable
fun SortDropdownIcon(
    selectedSort: LibrarySortOption,
    sortOptions: List<LibrarySortOption>,
    onSortSelected: (LibrarySortOption) -> Unit
) {
    var showSelectionPanel by remember { mutableStateOf(false) }
    val handleClick = { showSelectionPanel = true }
    IconButton(
        onClick = handleClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
        modifier = Modifier
            .focusAwareIconButton(
                shape = CircleShape,
                onClick = handleClick
            )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Sort,
            contentDescription = "Sort options",
            tint = Color.White
        )
    }

    if (showSelectionPanel) {
        FilterSelectionDialog(
            title = "Sort your library",
            subtitle = "Highlight a sorting rule and press select to rearrange instantly.",
            icon = Icons.AutoMirrored.Outlined.Sort,
            onDismissRequest = { showSelectionPanel = false },
            accentColor = Minsk,
            supportingContent = {
                Text(
                    text = "Currently applied: ${selectedSort.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            content = { focusRequester ->
                val listState = rememberLazyListState()
                val selectedIndex = remember(selectedSort, sortOptions) {
                    sortOptions.indexOfFirst { it.id == selectedSort.id }.coerceAtLeast(0)
                }

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = calculateRoundedValue(6).sdp)
                ) {
                    itemsIndexed(sortOptions, key = { _, option -> option.id }) { index, option ->
                        val accentColor = remember(option.id) {
                            sortAccentColor(option.id)
                        }
                        val selectSortOption = {
                            onSortSelected(option)
                            showSelectionPanel = false
                        }
                        FilterOptionItem(
                            title = option.label,
                            subtitle = option.sortSubtitle(selectedSortId = selectedSort.id),
                            selected = option.id == selectedSort.id,
                            onClick = selectSortOption,
                            accentColor = accentColor,
                            focusRequester = if (index == selectedIndex) focusRequester else null
                        )
                    }
                }
            }
        )
    }
}

private fun sortAccentColor(id: String): Color {
    val index = id.hashCode().absoluteValue % SortAccentPalette.size
    return SortAccentPalette[index]
}

private fun LibrarySortOption.sortSubtitle(selectedSortId: String): String {
    val descriptor = when (id) {
        "sort_name" -> "Alphabetical by title."
        "name" -> "Uses original metadata titles."
        "date_added" -> "Newest additions lead the list."
        "premiere_date" -> "Premiere dates take priority."
        "production_year" -> "Grouped by release year."
        "runtime" -> "Order by running time."
        "community_rating" -> "Community favorites first."
        "critic_rating" -> "Critics' choice at the top."
        "date_played" -> "Most recently played first."
        "play_count" -> "Most watched items first."
        "random" -> "Shuffle the entire library."
        else -> "Arrange by ${label.lowercase()}."
    }
    return if (id == selectedSortId) {
        "Currently applied • $descriptor"
    } else {
        descriptor
    }
}
