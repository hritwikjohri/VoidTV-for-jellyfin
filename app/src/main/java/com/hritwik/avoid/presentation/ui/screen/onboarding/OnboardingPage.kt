package com.hritwik.avoid.presentation.ui.screen.onboarding

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class OnboardingPage(
    val headline: String,
    val subheading: String,
    val body: String,
    val ctaText: String,
    val keyPoints: List<FeaturePoint>,
    val imageRes: Int? = null,
    val iconRes: ImageVector? = null,
    val backgroundGradient: List<Color>? = null,
)