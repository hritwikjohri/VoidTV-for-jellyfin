package com.hritwik.avoid.presentation.ui.navigation

import androidx.compose.ui.focus.FocusRequester
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.screen.home.HomeScreen
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel

fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel,
    sideNavigationFocusRequester: FocusRequester
) {
    composable(Routes.HOME) {
        HomeScreen(
            onMediaItemClick = { mediaItem: MediaItem ->
                if (mediaItem.type == "Episode") {
                    mediaItem.seriesId?.let { seriesId ->
                        navController.navigate(
                            Routes.mediaDetail(
                                seriesId,
                                seasonNumber = mediaItem.parentIndexNumber,
                                episodeId = mediaItem.id
                            )
                        )
                    }
                } else {
                    navController.navigate(Routes.mediaDetail(mediaItem.id))
                }
            },
            authViewModel = authViewModel,
            libraryViewModel = libraryViewModel,
            sideNavigationFocusRequester = sideNavigationFocusRequester
        )
    }
}
