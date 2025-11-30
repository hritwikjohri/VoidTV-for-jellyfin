package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.hritwik.avoid.domain.model.playback.DecoderMode
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun DecoderSelectionDialog(
    currentMode: DecoderMode,
    onModeSelected: (DecoderMode) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Video Decoder",
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            items(DecoderMode.entries, key = { it.name }) { mode ->
                val isSelected = currentMode == mode
                val focusRequester = remember { FocusRequester() }
                val icon = when (mode) {
                    DecoderMode.AUTO -> Icons.Default.SmartToy
                    DecoderMode.HARDWARE_ONLY -> Icons.Default.Speed
                    DecoderMode.SOFTWARE_ONLY -> Icons.Default.Memory
                }

                SelectionItem(
                    title = mode.value,
                    subtitle = mode.description,
                    isSelected = isSelected,
                    onClick = {
                        onModeSelected(mode)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null,
                    leadingContent = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(calculateRoundedValue(20).sdp)
                        )
                    }
                )
            }
        }
    }
}
