package com.hritwik.avoid.presentation.ui.components.media

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.hritwik.avoid.domain.model.library.Library
import com.hritwik.avoid.domain.model.library.LibraryType
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun LibraryGridCard(
    library: Library,
    serverUrl: String,
    modifier: Modifier = Modifier,
    onClick: (Library) -> Unit = {},
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        label = "library_focus"
    )
    val elevation by animateDpAsState(
        targetValue = if (isFocused) {
            calculateRoundedValue(12).sdp
        } else {
            calculateRoundedValue(6).sdp
        },
        label = "library_elevation"
    )
    val cardShape = RoundedCornerShape(calculateRoundedValue(12).sdp)

    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .dpadNavigation(
                    shape = cardShape,
                    focusRequester = focusRequester,
                    onFocusChange = { hasFocus -> isFocused = hasFocus },
                    onClick = { onClick(library) },
                    focusColor = Color.White
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            shape = cardShape
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (library.primaryImageTag != null) {
                    val imageUrl = remember(serverUrl, library.id, library.primaryImageTag) {
                        createLibraryImageUrl(serverUrl, library.id, library.primaryImageTag)
                    }
                    NetworkImage(
                        data = imageUrl,
                        contentDescription = library.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getLibraryIcon(library.type),
                            contentDescription = null,
                            modifier = Modifier.size(calculateRoundedValue(48).sdp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Text(
            text = library.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = calculateRoundedValue(8).sdp)
        )
    }
}

private fun getLibraryIcon(type: LibraryType): ImageVector {
    return when (type) {
        LibraryType.MOVIES -> Icons.Default.Movie
        LibraryType.TV_SHOWS -> Icons.Default.Tv
        LibraryType.MUSIC -> Icons.Default.MusicNote
        LibraryType.PHOTOS -> Icons.Default.Photo
        else -> Icons.Default.Movie
    }
}

private fun createLibraryImageUrl(
    serverUrl: String,
    itemId: String,
    imageTag: String
): String {
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$itemId/Images/Primary?tag=$imageTag&quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}&maxWidth=${ApiConstants.POSTER_MAX_WIDTH}"
}