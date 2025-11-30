package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun StorageLocationDialog(
    currentLocation: String,
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val locations = listOf("internal", "external")

    SelectionDialog(
        title = "Storage Location",
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            items(locations, key = { it }) { location ->
                val isSelected = currentLocation == location
                val focusRequester = remember { FocusRequester() }

                SelectionItem(
                    title = if (location == "internal") "Internal" else "External",
                    subtitle = null,
                    isSelected = isSelected,
                    onClick = {
                        onLocationSelected(location)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null
                )
            }
        }
    }
}