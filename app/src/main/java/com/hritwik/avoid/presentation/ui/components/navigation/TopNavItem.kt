package com.hritwik.avoid.presentation.ui.components.navigation

import androidx.annotation.StringRes

data class TopNavItem(
    @StringRes val label: Int,
    val option: String,
    val route: String
)