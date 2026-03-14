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
fun ControlsTimeoutDialog(
    currentSeconds: Int,
    onSecondsSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val minValue = 1
    val maxValue = 30
    val initialValue = (currentSeconds * 2).coerceIn(minValue, maxValue)
    var sliderValue by remember { mutableFloatStateOf(initialValue.toFloat()) }
    val sliderFocusRequester = remember { FocusRequester() }
    var sliderHasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sliderFocusRequester.requestFocus()
    }

    fun formatSeconds(value: Int): String {
        val seconds = value / 2.0f
        return if (seconds == seconds.toLong().toFloat()) {
            "${seconds.toInt()}s"
        } else {
            String.format("%.1fs", seconds)
        }
    }

    fun sliderToSeconds(sliderValue: Float): Int {
        return (sliderValue / 2).roundToInt()
    }

    SelectionDialog(
        title = "Controls Timeout",
        onDismiss = onDismiss
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
        ) {
            val displaySeconds = sliderToSeconds(sliderValue) / 2.0f
            val displayText = if (displaySeconds == 5.0f) {
                "5.0s (Default)"
            } else if (displaySeconds == displaySeconds.toLong().toFloat()) {
                "${displaySeconds.toInt()}s"
            } else {
                String.format("%.1fs", displaySeconds)
            }

            Text(
                text = displayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    sliderValue = value.coerceIn(minValue.toFloat(), maxValue.toFloat())
                },
                valueRange = minValue.toFloat()..maxValue.toFloat(),
                steps = maxValue - minValue - 1,
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
                                sliderValue = (sliderValue - 1).coerceIn(minValue.toFloat(), maxValue.toFloat())
                                true
                            }

                            Key.DirectionRight -> {
                                if (!sliderHasFocus) {
                                    sliderFocusRequester.requestFocus()
                                }
                                sliderValue = (sliderValue + 1).coerceIn(minValue.toFloat(), maxValue.toFloat())
                                true
                            }

                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                if (!sliderHasFocus) {
                                    sliderFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }

                                onSecondsSelected(sliderToSeconds(sliderValue))
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
