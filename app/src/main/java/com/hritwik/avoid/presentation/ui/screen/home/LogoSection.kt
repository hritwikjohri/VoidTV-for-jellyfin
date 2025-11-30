package com.hritwik.avoid.presentation.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun LogoSection(
    currentItem: MediaItem,
    serverUrl: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(calculateRoundedValue(180).sdp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val logoUrl = remember(currentItem, serverUrl) {
            currentItem.getLogoUrl(serverUrl)
        }

        AnimatedContent(
            targetState = logoUrl to currentItem.name,
            transitionSpec = {
                (slideInHorizontally { width -> width } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally { width -> -width } + fadeOut(tween(300)))
            },
            label = "logo_transition"
        ) { (logo, name) ->
            if (logo != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .heightIn(
                            min = calculateRoundedValue(150).sdp,
                            max = calculateRoundedValue(200).sdp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = logo,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center
                    )
                }
            } else {
                Text(
                    text = name,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}