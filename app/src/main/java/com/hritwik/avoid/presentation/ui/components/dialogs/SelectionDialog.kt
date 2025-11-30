package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun SelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    VoidAlertDialog(
        visible = true,
        title = title,
        icon = icon,
        onDismissRequest = onDismiss,
        content = content
    )
}

@Composable
fun SelectionItem(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    showRadio: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    leadingContent: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(calculateRoundedValue(16).sdp)

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        isFocused -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val internalFocusRequester = remember { FocusRequester() }
    val effectiveFocusRequester = focusRequester ?: internalFocusRequester

    LaunchedEffect(isSelected, effectiveFocusRequester) {
        if (isSelected) {
            effectiveFocusRequester.requestFocus()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .onFocusChanged { isFocused = it.hasFocus }
            .selectable(
                selected = isSelected && showRadio,
                onClick = onClick,
                role = if (showRadio) Role.RadioButton else Role.Button
            )
            .focusRequester(effectiveFocusRequester)
            .focusable()
            .border(
                width = calculateRoundedValue(1).sdp,
                color = borderColor,
                shape = shape
            )
            .background(backgroundColor, shape)
            .padding(
                horizontal = calculateRoundedValue(12).sdp,
                vertical = calculateRoundedValue(8).sdp
            ),
        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showRadio) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledSelectedColor = MaterialTheme.colorScheme.primary,
                    disabledUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        leadingContent?.let { it() }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(calculateRoundedValue(4).sdp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        else -> "%.1f KB".format(kb)
    }
}
