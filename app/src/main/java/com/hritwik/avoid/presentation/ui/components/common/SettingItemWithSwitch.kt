package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun SettingItemWithSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(calculateRoundedValue(16).sdp)
    val toggleChecked = { onCheckedChange(!checked) }

    val focusScale = if (isFocused) 1.04f else 1f
    val containerColor = if (isFocused) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    val glowElevation = if (isFocused) calculateRoundedValue(8).sdp else 0.sdp
    val borderStroke: BorderStroke? = if (isFocused) {
        BorderStroke(
            width = calculateRoundedValue(2).sdp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    Minsk.copy(alpha = 0.9f)
                )
            )
        )
    } else {
        null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(calculateRoundedValue(8).sdp)
            .dpadNavigation(
                shape = shape,
                focusRequester = focusRequester,
                onClick = toggleChecked,
                interactionSource = interactionSource,
                showFocusOutline = false,
                applyClickModifier = false
            )
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            },
        onClick = toggleChecked,
        interactionSource = interactionSource,
        elevation = CardDefaults.cardElevation(defaultElevation = glowElevation),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = borderStroke
    ) {
        if (isFocused) {
            val radialHighlightColor = Color.White.copy(alpha = 0.22f)
            val radialShadowColor = Color.Black.copy(alpha = 0.12f)
            val verticalTopColor = Color.White.copy(alpha = 0.12f)
            val verticalBottomColor = Color.Black.copy(alpha = 0.08f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                radialHighlightColor,
                                radialShadowColor
                            ),
                            radius = 600f
                        ),
                        shape = RoundedCornerShape(calculateRoundedValue(16).sdp)
                    )
                    .blur(radius = calculateRoundedValue(50).sdp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                verticalTopColor,
                                Color.Transparent,
                                verticalBottomColor
                            )
                        ),
                        shape = RoundedCornerShape(calculateRoundedValue(16).sdp)
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(calculateRoundedValue(16).sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(calculateRoundedValue(24).sdp)
            )

            Spacer(modifier = Modifier.width(calculateRoundedValue(16).sdp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    uncheckedTrackColor = Color.Transparent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            )
        }
    }
}