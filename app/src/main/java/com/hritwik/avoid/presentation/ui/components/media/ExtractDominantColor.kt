package com.hritwik.avoid.presentation.ui.components.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


private val colorCache = LruCache<String, Color>(50)









suspend fun extractDominantColor(
    imageUrl: String?,
    context: Context? = null
): Color? {
    if (imageUrl.isNullOrEmpty()) return null

    
    colorCache.get(imageUrl)?.let { return it }

    
    
    if (context == null) return null

    return withContext(Dispatchers.IO) {
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
                bitmap?.let {
                    val color = extractDominantColorFromBitmap(it)
                    
                    if (color != null) {
                        colorCache.put(imageUrl, color)
                    }
                    color
                }
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ExtractDominantColor", "Error extracting color from URL: $imageUrl", e)
            null
        }
    }
}




suspend fun extractDominantColor(
    context: Context,
    imageUrl: String?
): Color? = extractDominantColor(imageUrl, context)




private fun extractDominantColorFromBitmap(bitmap: Bitmap): Color? {
    return try {
        val palette = Palette.from(bitmap)
            .maximumColorCount(16) 
            .generate()

        
        val dominantColor = palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.darkVibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.lightMutedSwatch?.rgb
            ?: palette.darkMutedSwatch?.rgb
            ?: palette.dominantSwatch?.rgb

        dominantColor?.let { Color(it) }
    } catch (e: Exception) {
        android.util.Log.e("ExtractDominantColor", "Error extracting palette colors", e)
        null
    }
}




fun clearColorCache() {
    colorCache.evictAll()
}
