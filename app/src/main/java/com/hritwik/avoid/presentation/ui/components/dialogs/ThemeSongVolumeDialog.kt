package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlin.math.roundToInt

@Composable
fun ThemeSongVolumeDialog(
    currentVolume: Int,
    onVolumeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialVolume = currentVolume.coerceIn(1, 10)
    var sliderValue by remember { mutableFloatStateOf(initialVolume.toFloat()) }
    val sliderFocusRequester = remember { FocusRequester() }
    var sliderHasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sliderFocusRequester.requestFocus()
    }

    SelectionDialog(
        title = "Theme Song Volume",
        onDismiss = onDismiss
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
        ) {
            Text(
                text = "Volume ${sliderValue.roundToInt()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    sliderValue = value.coerceIn(1f, 10f)
                },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = calculateRoundedValue(4).sdp)
                    .focusRequester(sliderFocusRequester)
                    .onFocusChanged { focusState ->
                        sliderHasFocus = focusState.hasFocus
                    }
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }

                        when (event.key) {
                            Key.DirectionLeft -> {
                                if (!sliderHasFocus) {
                                    sliderFocusRequester.requestFocus()
                                }
                                sliderValue = (sliderValue - 1f).coerceIn(1f, 10f)
                                true
                            }

                            Key.DirectionRight -> {
                                if (!sliderHasFocus) {
                                    sliderFocusRequester.requestFocus()
                                }
                                sliderValue = (sliderValue + 1f).coerceIn(1f, 10f)
                                true
                            }

                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                if (!sliderHasFocus) {
                                    sliderFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }

                                onVolumeSelected(sliderValue.roundToInt().coerceIn(1, 10))
                                onDismiss()
                                true
                            }

                            else -> false
                        }
                    },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            )

            Text(
                text = "Use left/right to adjust, press OK to confirm",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
