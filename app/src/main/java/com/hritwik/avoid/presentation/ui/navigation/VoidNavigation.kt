package com.hritwik.avoid.presentation.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hritwik.avoid.presentation.ui.components.common.SplashScreen
import com.hritwik.avoid.presentation.ui.components.navigation.SideNavigationBar
import com.hritwik.avoid.presentation.ui.components.visual.AmbientBackground
import com.hritwik.avoid.presentation.ui.state.InitializationState
import com.hritwik.avoid.presentation.ui.state.NavigationState
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import kotlinx.coroutines.delay

@Composable
fun VoidNavigation(navigator: Navigator = rememberNavigator()) {
    val navController = navigator.navController
    val authViewModel: AuthServerViewModel = hiltViewModel()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    var navigationState by remember { mutableStateOf<NavigationState>(NavigationState.Loading) }
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val startDestination = if (authState.isAuthenticated) Routes.HOME else Routes.SERVER_SETUP
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val sideBarRoutes = listOf(Routes.SEARCH, Routes.HOME, Routes.MOVIES, Routes.SHOWS, Routes.LIBRARY, Routes.PROFILE)
    val selectedIndex = when (currentRoute) {
        Routes.SEARCH -> 0
        Routes.HOME -> 1
        Routes.MOVIES -> 2
        Routes.SHOWS -> 3
        Routes.LIBRARY -> 4
        Routes.PROFILE -> 5
        else -> null
    }
    val sideNavigationFocusRequester = remember { FocusRequester() }

    LaunchedEffect(currentRoute) {
        val authRoutes = listOf(
            Routes.SERVER_SETUP,
            Routes.LOGIN,
            Routes.QUICK_CONNECT,
            Routes.QUICK_CONNECT_AUTHORIZE
        )
        if (currentRoute in authRoutes && authState.isAuthenticated) {
            authViewModel.resetAuthState()
        }
    }

    LaunchedEffect(authState.initializationState, authState.isAuthenticated) {
        when {
            authState.initializationState != InitializationState.Initialized -> navigationState = NavigationState.Loading
            authState.initializationState == InitializationState.Initialized && navigationState is NavigationState.Loading -> {
                navigationState = NavigationState.ShowingSplash
                delay(2000)
                navigationState = NavigationState.ReadyToNavigate
            }
            navigationState is NavigationState.ReadyToNavigate -> {
                val targetRoute = if (authState.isAuthenticated) Routes.HOME else Routes.SERVER_SETUP
                navController.navigate(targetRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
                navigationState = NavigationState.NavigatedTo(targetRoute)
            }
            navigationState is NavigationState.NavigatedTo -> {
                val currentNavigatedRoute = (navigationState as NavigationState.NavigatedTo).route
                val targetRoute = if (authState.isAuthenticated) Routes.HOME else Routes.SERVER_SETUP
                if (currentNavigatedRoute != targetRoute) {
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                    navigationState = NavigationState.NavigatedTo(targetRoute)
                }
            }
        }
    }

    when (navigationState) {
        is NavigationState.Loading,
        is NavigationState.ShowingSplash -> {
            SplashScreen()
            return
        }
        else -> Unit
    }

    AmbientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
            ) {
                authGraph(navController, authViewModel)
                homeGraph(navController, authViewModel, libraryViewModel, sideNavigationFocusRequester)
                mediaGraph(navController, authViewModel, libraryViewModel, sideNavigationFocusRequester)
                profileGraph(navController, authViewModel, sideNavigationFocusRequester)
            }

            if (authState.isAuthenticated && currentRoute != Routes.VIDEO_PLAYER) {
                SideNavigationBar(
                    selectedItem = selectedIndex,
                    onItemSelected = { index ->
                        navController.navigate(sideBarRoutes[index]) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart),
                    focusRequester = sideNavigationFocusRequester
                )
            }
        }
    }
}
