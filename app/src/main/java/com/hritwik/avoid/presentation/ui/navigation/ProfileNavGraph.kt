package com.hritwik.avoid.presentation.ui.navigation

import androidx.compose.ui.focus.FocusRequester
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.hritwik.avoid.presentation.ui.screen.profile.Profile
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel

fun NavGraphBuilder.profileGraph(
    navController: NavHostController,
    authViewModel: AuthServerViewModel,
    sideNavigationFocusRequester: FocusRequester
) {
    composable(Routes.PROFILE) {
        Profile(
            authViewModel = authViewModel,
            onSwitchUser = {
                authViewModel.switchUser()
                navController.navigate(Routes.LOGIN)
            },
            sideNavigationFocusRequester = sideNavigationFocusRequester,
        )
    }
}
