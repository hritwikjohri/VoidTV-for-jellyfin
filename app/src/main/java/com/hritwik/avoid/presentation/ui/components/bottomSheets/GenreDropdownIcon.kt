package com.hritwik.avoid.presentation.ui.components.bottomSheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
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
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

private val GenreAccentPalette = listOf(
    Color(0xFF8C6CFF),
    Color(0xFF53C7FF),
    Color(0xFFFD6585),
    Color(0xFF2ECC71),
    Color(0xFFFFB74D),
    Color(0xFF7E57C2)
)

@Composable
fun GenreDropdownIcon(
    genres: List<String>,
    selectedGenre: String?,
    onGenreSelected: (String?) -> Unit,
    onRetry: () -> Unit,
    hasError: Boolean
) {
    var showSelectionPanel by remember { mutableStateOf(false) }
    val handleClick = {
        if (genres.isNotEmpty()) {
            showSelectionPanel = true
        } else if (hasError) {
            onRetry()
        }
    }
    IconButton(
        onClick = handleClick,
        enabled = genres.isNotEmpty() || hasError,
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
        modifier = Modifier
            .focusAwareIconButton(
                shape = CircleShape,
                onClick = handleClick,
                enabled = genres.isNotEmpty() || hasError
            )
    ) {
        Icon(
            imageVector = Icons.Outlined.Category,
            contentDescription = "Genre filter",
            tint = Color.White
        )
    }

    if (showSelectionPanel) {
        FilterSelectionDialog(
            title = "Choose a genre",
            subtitle = "Use the remote's D-pad to highlight a genre and press select to apply it.",
            icon = Icons.Outlined.Category,
            onDismissRequest = { showSelectionPanel = false },
            accentColor = Minsk,
            supportingContent = {
                Text(
                    text = "Tip: Press back to close without making changes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            content = { focusRequester ->
                val listState = rememberLazyListState()
                val selectedIndex = remember(selectedGenre, genres) {
                    val genreIndex = selectedGenre?.let { genres.indexOf(it) } ?: -1
                    if (genreIndex >= 0) genreIndex + 1 else 0
                }

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = calculateRoundedValue(6).sdp)
                ) {
                    item {
                        val selectAllGenre = {
                            onGenreSelected(null)
                            showSelectionPanel = false
                        }
                        FilterOptionItem(
                            title = "All genres",
                            subtitle = "Show everything in this library.",
                            selected = selectedGenre == null,
                            onClick = selectAllGenre,
                            accentColor = Minsk,
                            focusRequester = if (selectedIndex == 0) focusRequester else null
                        )
                    }

                    itemsIndexed(genres, key = { _, genre -> genre }) { index, genre ->
                        val accentColor = remember(genre) {
                            genreAccentColor(genre)
                        }
                        val selectGenre = {
                            onGenreSelected(genre)
                            showSelectionPanel = false
                        }
                        FilterOptionItem(
                            title = genre,
                            subtitle = "Only titles tagged \"$genre\".",
                            selected = genre == selectedGenre,
                            onClick = selectGenre,
                            accentColor = accentColor,
                            focusRequester = if (index + 1 == selectedIndex) focusRequester else null
                        )
                    }
                }
            }
        )
    }
}

private fun genreAccentColor(genre: String): Color {
    if (genre.isBlank()) return Minsk
    val index = genre.hashCode().absoluteValue % GenreAccentPalette.size
    return GenreAccentPalette[index]
}
