package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.theme.DeepBlack

@Composable
fun NetworkImage(
    data: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderResId: Int = R.drawable.void_icon
) {
    val context = LocalContext.current
    val rememberedData = remember(data) { data }
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(rememberedData)
            .placeholder(placeholderResId)
            .error(placeholderResId)
            .size(Size.ORIGINAL)
            .build()
    )

    val painterState = painter.state
    val isShowingPlaceholder = painterState is AsyncImagePainter.State.Loading ||
            painterState is AsyncImagePainter.State.Error ||
            rememberedData == null

    
    
    val backgroundModifier = if (isShowingPlaceholder) {
        Modifier.background(DeepBlack.copy(alpha = 0.8f))
    } else {
        Modifier
    }

    Box(
        modifier = modifier.then(backgroundModifier),
        contentAlignment = Alignment.Center
    ) {
        val imageModifier = if (isShowingPlaceholder) {
            Modifier.fillMaxSize(0.4f)
        } else {
            Modifier.fillMaxSize()
        }

        val imageContentScale = if (isShowingPlaceholder) {
            ContentScale.Fit
        } else {
            contentScale
        }

        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = imageModifier,
            contentScale = imageContentScale
        )
    }
}