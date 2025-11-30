package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import jp.wasabeef.transformers.coil.BlurTransformation

@Composable
fun HeroBackground(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    featherHeight: Dp = 64.dp,
    featherWidth: Dp = 64.dp,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize().then(modifier)) {
        
        
        Box(Modifier.matchParentSize().background(Color(0xFF0A0A0A)))

        if (imageUrl != null) {
            val context = LocalContext.current

            
            val blurredImageRequest = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(150) 
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(128)
                    .transformations(
                        BlurTransformation(
                            context = context,
                            radius = 25,  
                            sampling = 4  
                        )
                    )
                    .build()
            }

            AsyncImage(
                model = blurredImageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )

            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f)))

            Canvas(Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xCC000000), Color(0x66000000), Color.Transparent),
                    ),
                )
            }

            
            val unblurredImageRequest = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(150)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(CoilSize.ORIGINAL)
                    .build()
            }

            Box(
                Modifier
                    .fillMaxSize(0.70f)
                    .align(Alignment.TopEnd)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            ) {
                AsyncImage(
                    model = unblurredImageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )

                Canvas(Modifier.matchParentSize()) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black,
                            ),
                            startX = 0f,
                            endX = featherWidth.toPx(),
                        ),
                        size = Size(width = size.width, height = size.height),
                        blendMode = BlendMode.DstIn,
                    )
                }

                Canvas(Modifier.matchParentSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black,
                                Color.Transparent,
                            ),
                            startY = size.height - featherHeight.toPx(),
                            endY = size.height,
                        ),
                        size = size,
                        blendMode = BlendMode.DstIn,
                    )
                }
            }
        }

        content()
    }
}
