package com.hritwik.avoid.presentation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val TextFieldShape = RoundedCornerShape(48.dp)

@Composable
fun voidTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Minsk,
    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
