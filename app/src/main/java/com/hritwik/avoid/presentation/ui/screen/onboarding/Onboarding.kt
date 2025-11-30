package com.hritwik.avoid.presentation.ui.screen.onboarding

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.visual.AmbientBackground
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.launch

@Composable
fun OnboardingPageView(page: OnboardingPage, modifier: Modifier = Modifier) {
    Row (
        modifier = modifier.fillMaxSize().padding(calculateRoundedValue(16).sdp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box (
            modifier = modifier.weight(1f).fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
            ) {
                page.imageRes?.let { res ->
                    Image(
                        painter = painterResource(id = res),
                        contentDescription = null,
                        modifier = Modifier.size(calculateRoundedValue(150).sdp)
                    )
                }
                Text(
                    text = page.headline,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Box (
            modifier = modifier.weight(1f).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = page.subheading,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start,
                )
                Text(
                    text = page.body,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                )
                page.keyPoints.forEach { point ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = point.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = point.text,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val ctaFocusRequester = remember { FocusRequester() }
    var ctaReady by remember { mutableStateOf(false) }

    LaunchedEffect(ctaReady, pagerState.currentPage) {
        if (ctaReady) {
            ctaFocusRequester.requestFocus()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val denied = permissions.filterValues { !it }.keys
        if (Manifest.permission.POST_NOTIFICATIONS in denied) {
            Toast.makeText(
                context,
                "Notifications are disabled.",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (denied.any {
                it == Manifest.permission.READ_MEDIA_VIDEO ||
                        it == Manifest.permission.READ_MEDIA_AUDIO ||
                        it == Manifest.permission.READ_MEDIA_IMAGES ||
                        it == Manifest.permission.READ_EXTERNAL_STORAGE
            }
        ) {
            Toast.makeText(
                context,
                "Media access is unavailable without required permissions.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    AmbientBackground {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                OnboardingPageView(
                    page = onboardingPages[page],
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(onboardingPages.size) { index ->
                        val color = if (pagerState.currentPage == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                                .background(color, CircleShape),
                        )
                    }
                }

                val isLast = pagerState.currentPage == onboardingPages.lastIndex
                val currentPage = onboardingPages[pagerState.currentPage]
                val handleCtaClick: () -> Unit = {
                    if (isLast) {
                        val permissionsToRequest = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest += listOf(
                                Manifest.permission.POST_NOTIFICATIONS,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO,
                                Manifest.permission.READ_MEDIA_IMAGES
                            )
                        } else {
                            permissionsToRequest += Manifest.permission.READ_EXTERNAL_STORAGE
                        }

                        if (permissionsToRequest.isNotEmpty()) {
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                        }

                        onFinished()
                    } else {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                }
                Button(
                    onClick = handleCtaClick,
                    modifier = Modifier
                        .focusRequester(ctaFocusRequester)
                        .onGloballyPositioned { ctaReady = true }
                        .focusable()
                        .pressToClick { handleCtaClick() }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionLeft -> {
                                        val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                                        coroutineScope.launch { pagerState.animateScrollToPage(target) }
                                        true
                                    }
                                    Key.DirectionRight -> {
                                        val target = (pagerState.currentPage + 1).coerceAtMost(onboardingPages.lastIndex)
                                        coroutineScope.launch { pagerState.animateScrollToPage(target) }
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        }
                ) {
                    Text(text = currentPage.ctaText)
                }
            }
        }
    }
}

private val onboardingPages = listOf(
    OnboardingPage(
        imageRes = R.drawable.void_icon,
        headline = "Welcome",
        subheading = "Discover your media library",
        body = "Void is a Native android client for Jellyfin.",
        ctaText = "Get Started",
        keyPoints = listOf(
            FeaturePoint(Icons.Filled.VideoLibrary, "Browse and stream your collection"),
            FeaturePoint(Icons.Filled.PlayCircle, "Fast, reliable playback"),
            FeaturePoint(Icons.Filled.Download, "Save items for offline viewing"),
        ),
    ),
    OnboardingPage(
        imageRes = R.drawable.void_personalize,
        headline = "Personalize",
        subheading = "Make Void yours",
        body = "Adjust Font size, color theme and more to create your perfect streaming experience.",
        ctaText = "Connect to Server",
        keyPoints = listOf(
            FeaturePoint(Icons.Filled.Language, "Support for 30+ Languages"),
            FeaturePoint(Icons.Filled.TextFormat, "Choose your own font size"),
            FeaturePoint(Icons.Filled.InvertColors, "Support for colorblind people"),
        ),
    )
)

private fun Modifier.pressToClick(onClick: () -> Unit): Modifier =
    onPreviewKeyEvent { e ->
        if (e.type == KeyEventType.KeyDown &&
            (e.key == Key.Enter ||
                e.key == Key.NumPadEnter ||
                e.key == Key.DirectionCenter ||
                e.key == Key.Spacebar)
        ) {
            onClick()
            true
        } else {
            false
        }
    }
