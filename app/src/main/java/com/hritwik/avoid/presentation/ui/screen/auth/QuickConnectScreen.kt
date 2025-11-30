package com.hritwik.avoid.presentation.ui.screen.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.common.SubtleShinySignature
import com.hritwik.avoid.presentation.ui.components.visual.AmbientBackground
import com.hritwik.avoid.presentation.ui.theme.focusAwareButtonBorder
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun QuickConnectScreen(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AuthServerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val quickState = state.quickConnectState
    val cancelButtonFocusRequester = remember { FocusRequester() }
    val cancelButtonInteractionSource = remember { MutableInteractionSource() }
    val cancelButtonFocused by cancelButtonInteractionSource.collectIsFocusedAsState()

    LaunchedEffect(Unit) {
        cancelButtonFocusRequester.requestFocus()
    }

    BackHandler {
        viewModel.resetQuickConnectState()
        onCancel()
    }

    LaunchedEffect(Unit) {
        viewModel.resetQuickConnectState()
        viewModel.initiateQuickConnect()
    }

    LaunchedEffect(quickState.code) {
        if (quickState.code != null && !quickState.isPolling && state.authSession == null) {
            viewModel.pollQuickConnect()
        }
    }

    LaunchedEffect(state.authSession) {
        if (state.authSession != null) {
            onSuccess()
        }
    }

    AmbientBackground(
        drawableRes = R.drawable.jellyfin_logo,
        modifier = Modifier.fillMaxSize()
    ) {
        Row (
            modifier = Modifier.fillMaxSize().padding(calculateRoundedValue(16).sdp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box (
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.Center
            ){
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                ) {
                    AsyncImage(
                        model = R.drawable.void_icon,
                        contentDescription = null,
                        modifier = Modifier.size(calculateRoundedValue(100).sdp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Quick Connect",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
                SubtleShinySignature(
                    modifier = Modifier.padding(bottom = calculateRoundedValue(10).sdp)
                )
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(2).sdp)
                ) {
                    quickState.code?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (quickState.isPolling) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(calculateRoundedValue(4).sdp)
        ){
            OutlinedButton(
                onClick = {
                    viewModel.resetQuickConnectState()
                    onCancel()
                },
                enabled = !quickState.isPolling,
                shape = ButtonDefaults.outlinedShape,
                border = focusAwareButtonBorder(cancelButtonFocused),
                interactionSource = cancelButtonInteractionSource,
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.BottomEnd)
                    .focusRequester(cancelButtonFocusRequester)
                    .focusable()
                    .onPreviewKeyEvent {
                        if (it.key == Key.Back && it.type == KeyEventType.KeyDown) {
                            viewModel.resetQuickConnectState()
                            onCancel()
                            true
                        } else {
                            false
                        }
                    }
            ) {
                Text("Cancel")
            }

            quickState.error?.let { error ->
                Card(
                    modifier = Modifier.wrapContentSize().align(Alignment.BottomCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(calculateRoundedValue(16).sdp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}