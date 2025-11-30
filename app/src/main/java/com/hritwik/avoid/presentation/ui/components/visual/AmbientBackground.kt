package com.hritwik.avoid.presentation.ui.components.visual

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.hritwik.avoid.utils.helpers.extractColorsFromDrawable
import com.hritwik.avoid.utils.helpers.extractColorsFromUrl
import com.hritwik.avoid.utils.helpers.getDefaultAmbientColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    drawableRes: Int? = null,
    baseColors: List<Color> = getDefaultAmbientColors(),
    intensity: Float = 0.7f,
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    var extractedColors by remember { mutableStateOf(baseColors) }

    LaunchedEffect(imageUrl, drawableRes) {
        extractedColors = baseColors
        launch {
            val colors = when {
                imageUrl != null -> extractColorsFromUrl(context, imageUrl)
                drawableRes != null -> extractColorsFromDrawable(context, drawableRes)
                else -> baseColors
            }
            extractedColors = colors.takeIf { it.isNotEmpty() } ?: baseColors
        }
    }

    
    
    val backgroundBrush = remember(extractedColors, intensity) {
        Brush.linearGradient(
            colors = listOf(
                extractedColors.getOrElse(0) { Color(0xFF6366F1) }.copy(alpha = intensity * 0.4f),
                extractedColors.getOrElse(1) { Color(0xFF8B5CF6) }.copy(alpha = intensity * 0.2f),
                Color.Black
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    drawRect(backgroundBrush)
                }
            }
    ) {
        content()
    }
}