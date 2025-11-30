package com.hritwik.avoid.presentation.ui.screen.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
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
fun QuickConnectAuthorizeScreen(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AuthServerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val quickState = state.quickConnectState
    val cancelButtonFocusRequester = remember { FocusRequester() }
    val cancelButtonInteractionSource = remember { MutableInteractionSource() }
    val cancelButtonFocused by cancelButtonInteractionSource.collectIsFocusedAsState()

    LaunchedEffect(quickState.authorizationInitiated) {
        if (!quickState.authorizationInitiated) {
            viewModel.initiateQuickConnect()
        }
        cancelButtonFocusRequester.requestFocus()
    }

    LaunchedEffect(quickState.code) {
        if (quickState.code != null && !quickState.isPolling && state.authSession == null) {
            viewModel.pollQuickConnect()
        }
    }

    LaunchedEffect(
        quickState.code,
        quickState.authorizationToken,
        quickState.authorizationRequestInFlight,
        quickState.authorizationCompleted,
        quickState.authorizationAttempted
    ) {
        val code = quickState.code
        if (
            code != null &&
            quickState.authorizationToken != null &&
            !quickState.authorizationRequestInFlight &&
            !quickState.authorizationCompleted &&
            !quickState.authorizationAttempted
        ) {
            viewModel.authorizeQuickConnectWithToken(code)
        }
    }

    LaunchedEffect(state.authSession) {
        if (state.authSession != null) {
            onSuccess()
        }
    }

    BackHandler {
        viewModel.resetQuickConnectState()
        onCancel()
    }

    val statusText = when {
        quickState.authorizationRequestInFlight -> stringResource(R.string.quick_connect_authorize_status_authorizing)
        quickState.authorizationCompleted -> stringResource(R.string.quick_connect_authorize_status_submitted)
        quickState.isPolling -> stringResource(R.string.quick_connect_authorize_status_waiting)
        else -> stringResource(R.string.quick_connect_authorize_status_ready)
    }

    val showProgress = quickState.isPolling || quickState.authorizationRequestInFlight

    AmbientBackground(
        drawableRes = R.drawable.jellyfin_logo,
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(calculateRoundedValue(16).sdp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
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
                        text = stringResource(R.string.quick_connect_authorize_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.quick_connect_authorize_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                SubtleShinySignature(
                    modifier = Modifier.padding(bottom = calculateRoundedValue(10).sdp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
                ) {
                    quickState.code?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    if (showProgress) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(calculateRoundedValue(4).sdp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.resetQuickConnectState()
                    onCancel()
                },
                enabled = !showProgress,
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
                Text(text = stringResource(R.string.action_cancel))
            }

            quickState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomCenter),
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
