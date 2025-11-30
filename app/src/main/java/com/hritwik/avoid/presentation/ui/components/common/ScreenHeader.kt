package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.R
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.presentation.ui.common.focusAwareIconButton
import com.hritwik.avoid.presentation.ui.components.bottomSheets.GenreDropdownIcon
import com.hritwik.avoid.presentation.ui.components.bottomSheets.SortDropdownIcon
import com.hritwik.avoid.presentation.ui.screen.library.LibrarySortOption
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun ScreenHeader(
    modifier: Modifier = Modifier,
    title: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    showSortBar: Boolean = false,
    sortOptions: List<LibrarySortOption> = emptyList(),
    selectedSort: LibrarySortOption? = null,
    sortDirection: LibrarySortDirection = LibrarySortDirection.ASCENDING,
    onSortSelected: (LibrarySortOption) -> Unit = {},
    onSortDirectionChange: (LibrarySortDirection) -> Unit = {},
    genres: List<String> = emptyList(),
    selectedGenre: String? = null,
    onGenreSelected: (String?) -> Unit = {},
    isGenresLoading: Boolean = false,
    genreError: String? = null,
    onRetryGenres: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .align(Alignment.TopStart)
                .padding(
                    horizontal = calculateRoundedValue(4).sdp,
                    vertical = calculateRoundedValue(8).sdp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                val backFocusRequester = remember { FocusRequester() }
                IconButton(
                    onClick = onBackClick,
                    modifier = modifier
                        .focusRequester(backFocusRequester)
                        .semantics { role = Role.Button }
                        .focusable()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        modifier = modifier.size(calculateRoundedValue(28).sdp),
                        tint = Color.White
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = modifier
                    .padding(start = calculateRoundedValue(8).sdp)
                    .weight(1f)
            )

            if (showSortBar && selectedSort != null) {
                if (genres.isNotEmpty() || isGenresLoading || genreError != null) {
                    when {
                        genreError != null && genres.isEmpty() -> {
                            IconButton(
                                onClick = onRetryGenres,
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                                modifier = modifier
                                    .focusAwareIconButton(
                                        shape = CircleShape,
                                        onClick = onRetryGenres
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Retry genres",
                                    tint = Color.White
                                )
                            }
                        }

                        genres.isEmpty() -> {
                            IconButton(
                                onClick = {},
                                enabled = false,
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                            ) {
                                CircularProgressIndicator(
                                    modifier = modifier.size(calculateRoundedValue(24).sdp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            }
                        }

                        else -> {
                            GenreDropdownIcon(
                                genres = genres,
                                selectedGenre = selectedGenre,
                                onGenreSelected = onGenreSelected,
                                onRetry = onRetryGenres,
                                hasError = genreError != null
                            )
                        }
                    }
                }

                SortDropdownIcon(
                    selectedSort = selectedSort,
                    sortOptions = sortOptions,
                    onSortSelected = onSortSelected
                )

                if (selectedSort.allowDirectionToggle) {
                    SortDirectionToggle(
                        direction = sortDirection,
                        onDirectionChange = onSortDirectionChange
                    )
                }
            }
        }
        content()
    }
}

@Composable
private fun SortDirectionToggle(
    direction: LibrarySortDirection,
    onDirectionChange: (LibrarySortDirection) -> Unit
) {
    val toggleDirection = {
        val next = if (direction == LibrarySortDirection.ASCENDING) {
            LibrarySortDirection.DESCENDING
        } else {
            LibrarySortDirection.ASCENDING
        }
        onDirectionChange(next)
    }
    IconButton(
        onClick = toggleDirection,
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
        modifier = Modifier
            .focusAwareIconButton(
                shape = CircleShape,
                onClick = toggleDirection
            )
    ) {
        if (direction == LibrarySortDirection.ASCENDING) {
            Icon(
                imageVector = Icons.Outlined.ArrowUpward,
                contentDescription = "Ascending",
                tint = Color.White
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.ArrowDownward,
                contentDescription = "Descending",
                tint = Color.White
            )
        }
    }
}