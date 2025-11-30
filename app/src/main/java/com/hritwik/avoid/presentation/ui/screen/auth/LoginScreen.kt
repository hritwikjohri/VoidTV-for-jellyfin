package com.hritwik.avoid.presentation.ui.screen.auth

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.common.SubtleShinySignature
import com.hritwik.avoid.presentation.ui.components.visual.AmbientBackground
import com.hritwik.avoid.presentation.ui.theme.TextFieldShape
import com.hritwik.avoid.presentation.ui.theme.focusAwareButtonBorder
import com.hritwik.avoid.presentation.ui.theme.voidTextFieldColors
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onQuickConnect: () -> Unit,
    onBackToServerSetup: () -> Unit,
    viewModel: AuthServerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val serverUrl = state.serverUrl ?: ""
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val usernameFR = remember { FocusRequester() }
    val passwordFR = remember { FocusRequester() }
    val quickConnectFR = remember { FocusRequester() }
    val changeServerFR = remember { FocusRequester() }
    val signInFR = remember { FocusRequester() }

    val quickConnectIS = remember { MutableInteractionSource() }
    val quickConnectFocused by quickConnectIS.collectIsFocusedAsState()
    val changeServerIS = remember { MutableInteractionSource() }
    val changeServerFocused by changeServerIS.collectIsFocusedAsState()
    val signInIS = remember { MutableInteractionSource() }
    val signInFocused by signInIS.collectIsFocusedAsState()

    LaunchedEffect(Unit) { usernameFR.requestFocus() }

    LaunchedEffect(state.authSession) {
        if (state.authSession != null) onLoginSuccess()
    }

    AmbientBackground(
        drawableRes = R.drawable.jellyfin_logo,
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(calculateRoundedValue(16).sdp)
                .focusGroup(),
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
                        modifier = Modifier.size(calculateRoundedValue(200).sdp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = stringResource(R.string.sign_in_to_jellyfin),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        text = serverUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
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
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.username)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFR.requestFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(usernameFR)
                            .onPreviewKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown) {
                                    passwordFR.requestFocus(); true
                                } else false
                            }
                            .focusProperties {
                                down = passwordFR
                                next = passwordFR
                            }
                            .semantics { contentDescription = "Username field" },
                        enabled = !state.isLoading,
                        singleLine = true,
                        shape = TextFieldShape,
                        colors = voidTextFieldColors()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!state.isLoading && username.isNotBlank()) {
                                    viewModel.login(username, password)  
                                } else {
                                    signInFR.requestFocus()
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFR)
                            .onPreviewKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown) {
                                    when (e.key) {
                                        Key.DirectionDown -> { quickConnectFR.requestFocus(); true }
                                        Key.DirectionRight -> true
                                        Key.DirectionUp -> { usernameFR.requestFocus(); true }
                                        else -> false
                                    }
                                } else false
                            }
                            .focusProperties {
                                up = usernameFR
                                down = signInFR
                            }
                            .semantics { contentDescription = "Password field" },
                        enabled = !state.isLoading,
                        singleLine = true,
                        shape = TextFieldShape,
                        colors = voidTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(quickConnectFR)
                                .pressToClick(onQuickConnect)
                                .onPreviewKeyEvent { e ->
                                    if (e.type == KeyEventType.KeyDown) {
                                        when (e.key) {
                                            Key.DirectionDown -> { signInFR.requestFocus(); true }
                                            Key.DirectionRight -> { changeServerFR.requestFocus(); true }
                                            Key.DirectionUp -> { passwordFR.requestFocus(); true }
                                            else -> false
                                        }
                                    } else false
                                }
                                .focusProperties {
                                    down = signInFR
                                    right = changeServerFR
                                    up = passwordFR
                                }
                                .focusable(),
                            shape = ButtonDefaults.outlinedShape,
                            onClick = onQuickConnect,
                            enabled = !state.isLoading,
                            interactionSource = quickConnectIS,
                            border = focusAwareButtonBorder(
                                isFocused = quickConnectFocused,
                                defaultBorder = ButtonDefaults.outlinedButtonBorder(true)
                            )
                        ) {
                            Text("Use Quick Connect")
                        }

                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(changeServerFR)
                                .pressToClick(onBackToServerSetup)
                                .onPreviewKeyEvent { e ->
                                    if (e.type == KeyEventType.KeyDown) {
                                        when (e.key) {
                                            Key.DirectionDown -> { signInFR.requestFocus(); true }
                                            Key.DirectionLeft -> { quickConnectFR.requestFocus(); true }
                                            Key.DirectionRight -> { signInFR.requestFocus(); true }
                                            Key.DirectionUp -> { passwordFR.requestFocus(); true }
                                            else -> false
                                        }
                                    } else false
                                }
                                .focusProperties {
                                    down = signInFR
                                    left = quickConnectFR
                                    right = signInFR
                                    up = passwordFR
                                }
                                .focusable(),
                            shape = ButtonDefaults.outlinedShape,
                            onClick = onBackToServerSetup,
                            enabled = !state.isLoading,
                            interactionSource = changeServerIS,
                            border = focusAwareButtonBorder(
                                isFocused = changeServerFocused,
                                defaultBorder = ButtonDefaults.outlinedButtonBorder(true)
                            )
                        ) {
                            Text("Change Server")
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(calculateRoundedValue(4).sdp)
        ) {
            Button(
                onClick = {
                    if (username.isNotBlank()) {
                        viewModel.login(username, password)
                    }
                },
                enabled = !state.isLoading && username.isNotBlank(),
                shape = ButtonDefaults.shape,
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.BottomEnd)
                    .focusRequester(signInFR)
                    .pressToClick {
                        if (username.isNotBlank() && !state.isLoading) {
                            viewModel.login(username, password)
                        }
                    }
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown) {
                            when (e.key) {
                                Key.DirectionUp -> { passwordFR.requestFocus(); true }
                                Key.DirectionLeft -> { quickConnectFR.requestFocus(); true }
                                Key.DirectionRight -> { changeServerFR.requestFocus(); true }
                                else -> false
                            }
                        } else false
                    }
                    .focusProperties {
                        up = passwordFR
                        left = quickConnectFR
                        right = changeServerFR
                    }
                    .focusable()
                    .semantics { contentDescription = "Sign in" },
                interactionSource = signInIS,
                border = focusAwareButtonBorder(
                    isFocused = signInFocused,
                    defaultBorder = ButtonDefaults.outlinedButtonBorder(true)
                )
            ) {
                if (state.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(calculateRoundedValue(20).sdp),
                            strokeWidth = calculateRoundedValue(2).sdp
                        )
                        Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
                        Text("Signing in...")
                    }
                } else {
                    Text("Sign In")
                }
            }

            state.error?.let { error ->
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

private fun Modifier.pressToClick(onClick: () -> Unit): Modifier =
    onPreviewKeyEvent { e ->
        if (e.type == KeyEventType.KeyDown &&
            (e.key == Key.Enter ||
                    e.key == Key.NumPadEnter ||
                    e.key == Key.DirectionCenter ||
                    e.key == Key.Spacebar)
        ) {
            onClick()
            true
        } else {
            false
        }
    }
