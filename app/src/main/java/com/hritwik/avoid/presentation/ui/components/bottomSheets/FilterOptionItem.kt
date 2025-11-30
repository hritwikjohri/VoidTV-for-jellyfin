package com.hritwik.avoid.presentation.ui.components.bottomSheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.ui.graphics.lerp

@Composable
fun FilterOptionItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = Minsk,
    focusRequester: FocusRequester? = null,
    leadingContent: @Composable (highlighted: Boolean) -> Unit = { highlighted ->
        DefaultLeadingBadge(
            label = title,
            accentColor = accentColor,
            highlighted = highlighted
        )
    },
    trailingContent: @Composable (selected: Boolean) -> Unit = { isSelected ->
        DefaultTrailingCheck(isSelected, accentColor)
    }
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        label = "filterFocusScale"
    )
    val highlightFactor by animateFloatAsState(
        targetValue = when {
            selected -> 1f
            isFocused -> 0.7f
            else -> 0f
        },
        label = "filterHighlight"
    )
    val shape = RoundedCornerShape(calculateRoundedValue(26).sdp)
    val baseBackground = Color.White.copy(alpha = 0.05f)
    val highlightColor = accentColor.copy(alpha = 0.4f * highlightFactor)
    val borderColor = lerp(Color.White.copy(alpha = 0.12f), accentColor.copy(alpha = 0.85f), highlightFactor)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            }
            .clip(shape)
            .background(baseBackground)
            .border(1.dp, borderColor, shape)
            .background(highlightColor, shape)
            .dpadNavigation(
                shape = shape,
                focusRequester = focusRequester,
                onClick = onClick,
                onFocusChange = { isFocused = it },
                showFocusOutline = false
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent(isFocused || selected)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        trailingContent(selected)
    }
}

@Composable
private fun DefaultLeadingBadge(
    label: String,
    accentColor: Color,
    highlighted: Boolean
) {
    val badgeColors = if (highlighted) {
        listOf(
            accentColor.copy(alpha = 0.9f),
            accentColor.copy(alpha = 0.6f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.08f)
        )
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(badgeColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DefaultTrailingCheck(
    selected: Boolean,
    accentColor: Color
) {
    AnimatedVisibility(
        visible = selected,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = Color.White
            )
        }
    }
}
