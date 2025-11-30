package com.hritwik.avoid.utils.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.hritwik.avoid.presentation.ui.components.visual.PaletteCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val colorPaletteCache = LruCache<String, List<Color>>(20)

suspend fun extractColorsFromUrl(
    context: Context,
    imageUrl: String
): List<Color> = withContext(Dispatchers.IO) {
    PaletteCache.get(imageUrl)?.let { return@withContext it }

    try {
        val imageLoader = Coil.imageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .size(128)
            .scale(Scale.FIT)
            .build()

        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            val colors = bitmap?.let { extractPaletteColors(it) } ?: getDefaultAmbientColors()
            PaletteCache.put(imageUrl, colors)
            colors
        } else {
            getDefaultAmbientColors()
        }
    } catch (e: Exception) {
        getDefaultAmbientColors()
    }
}

suspend fun extractColorsFromDrawable(
    context: Context,
    drawableRes: Int
): List<Color> = withContext(Dispatchers.IO) {
    try {
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        val bitmap = drawable?.let { d ->
            if (d is BitmapDrawable) {
                d.bitmap
            } else {
                val bitmap = createBitmap(
                    d.intrinsicWidth.takeIf { it > 0 } ?: 300,
                    d.intrinsicHeight.takeIf { it > 0 } ?: 300
                )
                val canvas = Canvas(bitmap)
                d.setBounds(0, 0, canvas.width, canvas.height)
                d.draw(canvas)
                bitmap
            }
        }
        bitmap?.let { extractPaletteColors(it) } ?: getDefaultAmbientColors()
    } catch (e: Exception) {
        getDefaultAmbientColors()
    }
}

suspend fun extractPaletteColors(bitmap: Bitmap): List<Color> = withContext(Dispatchers.Default) {
    val palette = Palette.from(bitmap)
        .maximumColorCount(16)  
        .generate()

    val colors = mutableListOf<Color>()
    palette.vibrantSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.lightVibrantSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.darkVibrantSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.dominantSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.mutedSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.lightMutedSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.darkMutedSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.swatches.take(4).forEach { swatch ->
        val color = Color(swatch.rgb)
        if (!colors.contains(color)) {
            colors.add(color)
        }
    }

    if (colors.isEmpty()) getDefaultAmbientColors() else colors.take(6)
}

fun getDefaultAmbientColors(): List<Color> = listOf(
    Color(0xFF6366F1), 
    Color(0xFF8B5CF6), 
    Color(0xFF06B6D4), 
    Color(0xFF10B981), 
    Color(0xFFEF4444), 
    Color(0xFFF59E0B)  
)
