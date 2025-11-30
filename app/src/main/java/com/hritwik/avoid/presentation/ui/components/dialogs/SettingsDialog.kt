package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun SettingsDialog(
    onVoidSettings: () -> Unit,
    onPlaybackSettings: () -> Unit,
    onPersonalizationSettings: () -> Unit,
    onServerSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Settings",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            SelectionItem(
                title = "Void settings",
                subtitle = null,
                isSelected = false,
                onClick = {
                    onVoidSettings()
                    onDismiss()
                }
            )
            SelectionItem(
                title = "Playback settings",
                subtitle = null,
                isSelected = false,
                onClick = {
                    onPlaybackSettings()
                    onDismiss()
                }
            )
            SelectionItem(
                title = "Personalization settings",
                subtitle = null,
                isSelected = false,
                onClick = {
                    onPersonalizationSettings()
                    onDismiss()
                }
            )
            SelectionItem(
                title = "Server settings",
                subtitle = null,
                isSelected = false,
                onClick = {
                    onServerSettings()
                    onDismiss()
                }
            )
        }
    }
}
