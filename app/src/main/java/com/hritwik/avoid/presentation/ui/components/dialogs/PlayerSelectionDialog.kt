package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import com.hritwik.avoid.domain.model.playback.PlayerType
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun PlayerSelectionDialog(
    currentPlayer: PlayerType,
    onPlayerSelected: (PlayerType) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Preferred Player",
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            items(PlayerType.entries, key = { it.name }) { player ->
                val isSelected = currentPlayer == player
                val focusRequester = remember { FocusRequester() }

                SelectionItem(
                    title = player.value,
                    isSelected = isSelected,
                    onClick = {
                        onPlayerSelected(player)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null
                )
            }
        }
    }
}
