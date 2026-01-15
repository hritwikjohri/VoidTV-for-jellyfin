package com.hritwik.avoid.presentation.ui.components.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import com.hritwik.avoid.presentation.ui.common.dpadNavigation

private val ActionFocusBackgroundAlpha = 0.2f

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit = {},
    onClear: () -> Unit = {},
    placeholder: String = "Search...",
    focusRequester: FocusRequester = remember { FocusRequester() },
    clearFocusRequester: FocusRequester = remember { FocusRequester() },
    downFocusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val textFieldFocusRequester = remember { FocusRequester() }

    val requestImeFocus: () -> Unit = {
        textFieldFocusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = modifier
            .dpadNavigation(
                shape = RoundedCornerShape(calculateRoundedValue(24).sdp),
                focusRequester = focusRequester,
                onFocusChange = { focused ->
                    isFocused = focused
                },
                onClick = { requestImeFocus() },
                onMoveFocus = { direction ->
                    if (direction == FocusDirection.Right && query.isNotEmpty()) {
                        clearFocusRequester.requestFocus()
                        true
                    } else {
                        false
                    }
                }
            )
            .border(
                width = calculateRoundedValue(1).sdp,
                color = if (isFocused) Color.White else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(calculateRoundedValue(24).sdp)
            )
            .padding(calculateRoundedValue(16).sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var isClearFocused by remember { mutableStateOf(false) }

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(calculateRoundedValue(20).sdp)
        )

        Spacer(modifier = Modifier.width(calculateRoundedValue(12).sdp))

        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    requestImeFocus()
                }
        ) {
            val textStyle = MaterialTheme.typography.bodyLarge
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = textStyle.merge(
                    TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch(query)
                        keyboardController?.hide()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(textFieldFocusRequester),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            )
        }

        Spacer(modifier = Modifier.width(calculateRoundedValue(12).sdp))

        if (query.isNotEmpty()) {
            Spacer(modifier = Modifier.width(calculateRoundedValue(12).sdp))

            Box(
                modifier = Modifier
                    .size(calculateRoundedValue(36).sdp)
                    .dpadNavigation(
                        shape = RoundedCornerShape(calculateRoundedValue(12).sdp),
                        focusRequester = clearFocusRequester,
                        onFocusChange = { focused -> isClearFocused = focused },
                        onClick = onClear,
                        onMoveFocus = { direction ->
                            when (direction) {
                                FocusDirection.Left -> {
                                    focusRequester.requestFocus()
                                    true
                                }
                                FocusDirection.Down -> {
                                    downFocusRequester?.let {
                                        it.requestFocus()
                                        true
                                    } ?: false
                                }
                                else -> false
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val tint = if (isClearFocused) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                val background = if (isClearFocused) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = ActionFocusBackgroundAlpha)
                } else {
                    Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(calculateRoundedValue(12).sdp))
                        .background(background)
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = tint,
                    modifier = Modifier.size(calculateRoundedValue(20).sdp)
                )
            }
        }
    }
}

@Composable
fun RecentSearches(
    modifier: Modifier = Modifier,
    recentSearches: List<String>,
    onSearchClick: (String) -> Unit,
    onClearAll: () -> Unit = {},
) {
    if (recentSearches.isEmpty()) return

    Column(modifier = modifier) {
        recentSearches.forEach { search ->
            Text(
                text = search,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .dpadNavigation(
                        shape = RoundedCornerShape(calculateRoundedValue(12).sdp),
                        onClick = { onSearchClick(search) }
                    )
                    .padding(vertical = calculateRoundedValue(4).sdp)
            )
        }

        TextButton(
            onClick = onClearAll,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = calculateRoundedValue(8).sdp)
        ) {
            Text("Clear History")
        }
    }
}
