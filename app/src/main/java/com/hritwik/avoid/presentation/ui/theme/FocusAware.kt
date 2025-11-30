package com.hritwik.avoid.presentation.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun focusAwareButtonBorder(
    isFocused: Boolean,
    defaultBorder: BorderStroke? = ButtonDefaults.outlinedButtonBorder,
    focusedColor: Color = Minsk,
    focusedBorderWidth: Dp = defaultBorder?.width ?: 2.dp
): BorderStroke? {
    return if (isFocused) {
        BorderStroke(focusedBorderWidth, focusedColor)
    } else {
        defaultBorder
    }
}
