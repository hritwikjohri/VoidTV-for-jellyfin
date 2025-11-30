package com.hritwik.avoid.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.presentation.ui.theme.Minsk

private val DefaultFocusOutlineWidth = 2.dp

private fun Modifier.drawFocusOutline(
    isFocused: Boolean,
    shape: Shape,
    focusColor: Color
): Modifier {
    return this
        .graphicsLayer {
            this.shape = shape
            clip = true
        }
        .border(
            width = DefaultFocusOutlineWidth,
            color = if (isFocused) focusColor else Color.Transparent,
            shape = shape
        )
}

fun Modifier.dpadNavigation(
    shape: Shape? = null,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onMoveFocus: ((FocusDirection) -> Boolean)? = null,
    showFocusOutline: Boolean = true,
    applyClickModifier: Boolean = true,
    focusColor: Color = Minsk,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val actualShape = shape ?: MaterialTheme.shapes.small
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by resolvedInteractionSource.collectIsFocusedAsState()

    fun move(direction: FocusDirection): Boolean {
        val handledByCustomMove = onMoveFocus?.invoke(direction) ?: false
        return if (handledByCustomMove) {
            true
        } else {
            focusManager.moveFocus(direction)
        }
    }

    this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { focusState: FocusState ->
            if (!enabled) return@onFocusChanged
            val hasFocus = focusState.hasFocus
            onFocusChange?.invoke(hasFocus)
        }
        .let { base ->
            if (showFocusOutline) {
                base.drawFocusOutline(isFocused, actualShape, focusColor)
            } else {
                base
            }
        }
        .focusable(enabled = enabled, interactionSource = resolvedInteractionSource)
        .onPreviewKeyEvent { event ->
            if (!enabled || event.type != KeyEventType.KeyDown) {
                return@onPreviewKeyEvent false
            }

            when (event.key) {
                Key.DirectionLeft -> move(FocusDirection.Left)
                Key.DirectionRight -> move(FocusDirection.Right)
                Key.DirectionUp -> move(FocusDirection.Up)
                Key.DirectionDown -> move(FocusDirection.Down)
                Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar ->
                    if (onClick != null) {
                        onClick()
                        true
                    } else {
                        false
                    }
                else -> false
            }
        }
        .let { base ->
            if (onClick != null && applyClickModifier) {
                base.clickable(
                    enabled = enabled,
                    indication = null,
                    interactionSource = resolvedInteractionSource
                ) { onClick() }
            } else {
                base
            }
        }
}

fun Modifier.focusAwareIconButton(
    shape: Shape = CircleShape,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onMoveFocus: ((FocusDirection) -> Boolean)? = null,
    applyClickModifier: Boolean = true,
    focusedColor: Color = Minsk.copy(alpha = 0.4f),
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }

    this
        .dpadNavigation(
            shape = shape,
            focusRequester = focusRequester,
            enabled = enabled,
            onClick = onClick,
            onFocusChange = { hasFocus ->
                isFocused = hasFocus
                onFocusChange?.invoke(hasFocus)
            },
            onMoveFocus = onMoveFocus,
            showFocusOutline = false,
            applyClickModifier = applyClickModifier,
            focusColor = focusedColor
        )
        .clip(shape)
        .background(
            color = if (isFocused && enabled) {
                focusedColor
            } else {
                Color.Transparent
            }
        )
}

fun Modifier.focusToSideNavigationOnLeftEdge(
    sideNavigationFocusRequester: FocusRequester?,
    block: FocusProperties.() -> Unit = {},
): Modifier = composed {
    if (sideNavigationFocusRequester == null) {
        this.focusProperties(block)
    } else {
        val focusManager = LocalFocusManager.current
        this
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                    return@onPreviewKeyEvent false
                }

                val moved = focusManager.moveFocus(FocusDirection.Left)
                if (!moved) {
                    sideNavigationFocusRequester.requestFocus()
                }
                true
            }
            .focusProperties(block)
    }
}
