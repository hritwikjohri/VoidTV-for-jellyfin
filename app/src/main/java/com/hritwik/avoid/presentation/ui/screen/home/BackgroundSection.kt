package com.hritwik.avoid.presentation.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.HeroBackground
import com.hritwik.avoid.utils.extensions.getBackdropUrl

@Composable
fun BackgroundSection(
    currentItem: MediaItem,
    serverUrl: String
) {
    AnimatedContent(
        targetState = currentItem,
        transitionSpec = {
            fadeIn(animationSpec = tween(150)) togetherWith
                    fadeOut(animationSpec = tween(150))
        },
        label = "background_transition"
    ) { item ->
        HeroBackground(
            imageUrl = item.getBackdropUrl(serverUrl),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        }
    }
}