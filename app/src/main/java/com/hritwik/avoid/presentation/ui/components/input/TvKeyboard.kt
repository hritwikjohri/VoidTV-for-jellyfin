package com.hritwik.avoid.presentation.ui.components.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun TvKeyboard(
    modifier: Modifier = Modifier,
    onKeyPress: (Char) -> Unit,
    onClear: () -> Unit,
    firstKeyFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester
) {
    val keyboardLayout = remember {
        ('A'..'Z').toList() + ('1'..'9').toList() + listOf('0')
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        contentPadding = PaddingValues(calculateRoundedValue(2).sdp),
        modifier = modifier.padding(calculateRoundedValue(8).sdp),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp),
        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp)
    ) {
        itemsIndexed(keyboardLayout) { index, key ->
            KeyButton(
                key = key,
                onKeyPress = onKeyPress,
                focusRequester = if (index == 0) firstKeyFocusRequester else null,
                upFocusRequester = if (index == 0) upFocusRequester else null
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = calculateRoundedValue(12).sdp),
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp)
            ) {
                BottomKeyButton(
                    key = ' ',
                    label = "SPACE",
                    modifier = Modifier.weight(1f)
                ) { onKeyPress(' ') }

                BottomKeyButton(
                    key = '\b',
                    label = "DEL",
                    modifier = Modifier.weight(1f)
                ) { onKeyPress('\b') }

                BottomKeyButton(
                    key = '\n',
                    label = "ENTER",
                    modifier = Modifier.weight(1f)
                ) { onKeyPress('\n') }

                BottomKeyButton(
                    key = '\u0000',
                    label = "CLEAR",
                    modifier = Modifier.weight(1f)
                ) { onClear() }
            }
        }
    }
}

@Composable
private fun BottomKeyButton(
    key: Char,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Surface(
        modifier = modifier
            .height(calculateRoundedValue(40).sdp)
            .dpadNavigation(
                shape = RoundedCornerShape(calculateRoundedValue(6).sdp),
                focusRequester = focusRequester,
                onClick = onClick
            ),
        shape = RoundedCornerShape(calculateRoundedValue(6).sdp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = calculateRoundedValue(1).sdp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            when (key) {
                ' ' -> {
                    Icon(
                        imageVector = Icons.Default.SpaceBar,
                        contentDescription = "Space",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(calculateRoundedValue(20).sdp)
                    )
                }
                '\b' -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(calculateRoundedValue(18).sdp)
                    )
                }
                '\n' -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(calculateRoundedValue(18).sdp)
                    )
                }
                '\u0000' -> {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(calculateRoundedValue(18).sdp)
                    )
                }
                else -> {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: Char,
    onKeyPress: (Char) -> Unit,
    focusRequester: FocusRequester?,
    upFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier
) {
    val internalFocusRequester = focusRequester ?: remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = modifier
            .height(calculateRoundedValue(40).sdp)
            .dpadNavigation(
                shape = RoundedCornerShape(calculateRoundedValue(6).sdp),
                focusRequester = internalFocusRequester,
                onClick = { onKeyPress(key) },
                onMoveFocus = { direction ->
                    if (direction == FocusDirection.Up && upFocusRequester != null) {
                        upFocusRequester.requestFocus()
                        true
                    } else {
                        focusManager.moveFocus(direction)
                    }
                }
            ),
        shape = RoundedCornerShape(calculateRoundedValue(6).sdp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = null
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = key.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}