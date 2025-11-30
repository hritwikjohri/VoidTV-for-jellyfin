package com.hritwik.avoid.presentation.ui.screen.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.common.dpadNavigation
import com.hritwik.avoid.utils.extensions.homeContentFocusProperties
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun ButtonsSection(
    currentItem: MediaItem,
    watchNowFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester,
    hasContentFocusTarget: Boolean,
    sideNavigationFocusRequester: FocusRequester?,
    onMediaItemClick: (MediaItem) -> Unit
) {
    Button(
        onClick = { onMediaItemClick(currentItem) },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(calculateRoundedValue(52).sdp),
        modifier = Modifier
            .height(calculateRoundedValue(76).sdp)
            .dpadNavigation(
                shape = RoundedCornerShape(calculateRoundedValue(52).sdp),
                focusRequester = watchNowFocusRequester,
                onClick = { onMediaItemClick(currentItem) },
                applyClickModifier = false
            )
            .homeContentFocusProperties(sideNavigationFocusRequester) {
                if (hasContentFocusTarget) {
                    right = contentFocusRequester
                    down = contentFocusRequester
                }
            }
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            modifier = Modifier.size(calculateRoundedValue(24).sdp)
        )
        Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
        Text(
            text = "Watch Now",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}