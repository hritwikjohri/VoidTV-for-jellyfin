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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.HeroBackground
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.extensions.getBackdropUrl
import com.hritwik.avoid.utils.extensions.getImageUrl

@Composable
fun BackgroundSection(
    currentItem: MediaItem,
    serverUrl: String
) {
    var lastSuccessfulUrl by remember { mutableStateOf<String?>(null) }
    AnimatedContent(
        targetState = currentItem,
        transitionSpec = {
            fadeIn(animationSpec = tween(150)) togetherWith
                    fadeOut(animationSpec = tween(150))
        },
        label = "background_transition"
    ) { item ->
        val backdropTag = item.backdropImageTags.firstOrNull()
        val primaryTag = item.primaryImageTag
        val heroUrl = when {
            backdropTag != null -> item.getBackdropUrl(serverUrl)
            primaryTag != null -> item.getImageUrl(
                serverUrl = serverUrl,
                imageType = ApiConstants.IMAGE_TYPE_PRIMARY,
                imageTag = primaryTag
            )
            else -> null
        }
        val heroBlurHash = when {
            backdropTag != null -> item.getBlurHash(ApiConstants.IMAGE_TYPE_BACKDROP, backdropTag)
            primaryTag != null -> item.getBlurHash(ApiConstants.IMAGE_TYPE_PRIMARY, primaryTag)
            else -> null
        }
        HeroBackground(
            imageUrl = heroUrl,
            blurHash = heroBlurHash,
            placeholderUrl = lastSuccessfulUrl,
            onImageLoaded = { lastSuccessfulUrl = it },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
        }
    }
}
