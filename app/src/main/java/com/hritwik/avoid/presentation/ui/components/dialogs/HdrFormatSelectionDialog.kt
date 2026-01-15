package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import com.hritwik.avoid.domain.model.playback.HdrFormatPreference
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun HdrFormatSelectionDialog(
    currentPreference: HdrFormatPreference,
    onPreferenceSelected: (HdrFormatPreference) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "HDR Format",
        icon = Icons.Default.HdrOn,
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            items(HdrFormatPreference.entries, key = { it.name }) { preference ->
                val isSelected = currentPreference == preference
                val focusRequester = remember { FocusRequester() }
                val subtitle = when (preference) {
                    HdrFormatPreference.AUTO -> "Let the device decide"
                    HdrFormatPreference.HDR10_PLUS -> "Strip Dolby Vision metadata"
                    HdrFormatPreference.DOLBY_VISION -> "Strip HDR10+ metadata"
                }

                SelectionItem(
                    title = preference.displayName,
                    subtitle = subtitle,
                    isSelected = isSelected,
                    onClick = {
                        onPreferenceSelected(preference)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null
                )
            }
        }
    }
}
