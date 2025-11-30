package com.hritwik.avoid.presentation.ui.components.navigation

import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val label: Int,
    val icon: ImageVector,
    val route: String,
    val customIcon: Int? = null
)