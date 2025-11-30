package com.hritwik.avoid.presentation.ui.navigation

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hritwik.avoid.domain.model.library.LibraryType
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.screen.library.LibrarySection
import com.hritwik.avoid.presentation.ui.screen.search.Search
import com.hritwik.avoid.presentation.ui.screen.library.LibraryScreen
import com.hritwik.avoid.presentation.ui.screen.media.MediaDetailScreen
import com.hritwik.avoid.presentation.ui.screen.collection.CollectionScreen
import com.hritwik.avoid.presentation.ui.screen.collection.CollectionsScreen
import com.hritwik.avoid.presentation.ui.screen.player.VideoPlayerScreen
import com.hritwik.avoid.presentation.ui.screen.movies.MoviesScreen
import com.hritwik.avoid.presentation.ui.screen.shows.ShowsScreen
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.presentation.viewmodel.media.MediaViewModel

private const val LIBRARY_DETAIL_PRESERVE_CACHE_KEY = "libraryDetail_preserveCache"

fun NavGraphBuilder.mediaGraph(
    navController: NavHostController,
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel,
    sideNavigationFocusRequester: FocusRequester
) {
    composable(Routes.LIBRARY) {
        LibrarySection(
            onLibraryClick = { libraryId, libraryName, libraryType ->
                navController.navigate(
                    Routes.libraryDetail(
                        libraryId = libraryId,
                        libraryName = libraryName,
                        libraryType = libraryType
                    )
                )
            },
            authViewModel = authViewModel,
            libraryViewModel = libraryViewModel,
            sideNavigationFocusRequester = sideNavigationFocusRequester
        )
    }

    composable(Routes.SEARCH) {
        Search(
            onMediaItemClick = { mediaItem: MediaItem ->
                when {
                    mediaItem.type.equals("Episode", ignoreCase = true) -> {
                        mediaItem.seriesId?.let { seriesId ->
                            navController.navigate(
                                Routes.mediaDetail(
                                    seriesId,
                                    seasonNumber = mediaItem.parentIndexNumber,
                                    episodeId = mediaItem.id
                                )
                            )
                        } ?: navController.navigate(Routes.mediaDetail(mediaItem.id))
                    }
                    mediaItem.type.equals("BoxSet", ignoreCase = true) -> {
                        navController.navigate(
                            Routes.collectionDetail(
                                mediaItem.id,
                                mediaItem.name
                            )
                        )
                    }
                    else -> navController.navigate(Routes.mediaDetail(mediaItem.id))
                }
            },
            authViewModel = authViewModel,
            sideNavigationFocusRequester = sideNavigationFocusRequester
        )
    }

    composable(Routes.MOVIES) {
        MoviesScreen(
            onMediaItemClick = { mediaItem: MediaItem ->
                navController.navigate(Routes.mediaDetail(mediaItem.id))
            },
            onStudioClick = { libraryId, libraryName, additionalLibraryIds, libraryType, studio ->
                navController.navigate(
                    Routes.libraryDetail(
                        libraryId = libraryId,
                        libraryName = libraryName,
                        studio = studio.name,
                        additionalLibraryIds = additionalLibraryIds,
                        libraryType = libraryType
                    )
                )
            },
            authViewModel = authViewModel,
            libraryViewModel = libraryViewModel,
            sideNavigationFocusRequester = sideNavigationFocusRequester
        )
    }

    composable(Routes.SHOWS) {
        ShowsScreen(
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
            onStudioClick = { libraryId, libraryName, additionalLibraryIds, libraryType, studio ->
                navController.navigate(
                    Routes.libraryDetail(
                        libraryId = libraryId,
                        libraryName = libraryName,
                        studio = studio.name,
                        additionalLibraryIds = additionalLibraryIds,
                        libraryType = libraryType
                    )
                )
            },
            authViewModel = authViewModel,
            libraryViewModel = libraryViewModel,
            sideNavigationFocusRequester = sideNavigationFocusRequester
        )
    }

    composable(Routes.COLLECTIONS) {
        CollectionsScreen(
            onBackClick = { navController.navigateUp() },
            onCollectionClick = { collection ->
                navController.navigate(
                    Routes.collectionDetail(
                        collection.id,
                        collection.name
                    )
                )
            },
            authViewModel = authViewModel,
            sideNavigationFocusRequester = sideNavigationFocusRequester
        )
    }

    composable(
        route = Routes.LIBRARY_DETAIL,
        arguments = listOf(
            navArgument("libraryId") { type = NavType.StringType },
            navArgument("libraryName") { type = NavType.StringType },
            navArgument("studio") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("additionalLibraryIds") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("libraryType") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val libraryId = backStackEntry.arguments?.getString("libraryId") ?: ""
        val libraryName = backStackEntry.arguments?.getString("libraryName") ?: "LibrarySection"
        val studioName = backStackEntry.arguments?.getString("studio")?.takeIf { it.isNotBlank() }
        val additionalIdsRaw = backStackEntry.arguments
            ?.getString("additionalLibraryIds")
            ?.takeIf { !it.isNullOrBlank() }
        val additionalLibraryIds = additionalIdsRaw
            ?.let { Uri.decode(it) }
            ?.split(",")
            ?.mapNotNull { candidate ->
                Uri.decode(candidate).takeIf { id -> id.isNotBlank() }
            }
            ?.filter { it != libraryId }
            ?: emptyList()
        val libraryTypeName = backStackEntry.arguments
            ?.getString("libraryType")
            ?.takeIf { !it.isNullOrBlank() }
        val libraryType = libraryTypeName
            ?.let { runCatching { LibraryType.valueOf(Uri.decode(it)) }.getOrNull() }
        val allLibraryIds = (listOfNotNull(libraryId.takeIf { it.isNotBlank() }) + additionalLibraryIds)
            .distinct()
        val shouldPreserveCache =
            backStackEntry.savedStateHandle.get<Boolean>(LIBRARY_DETAIL_PRESERVE_CACHE_KEY) == true

        LibraryScreen(
            libraryId = libraryId,
            libraryName = libraryName,
            studio = studioName,
            libraryIds = allLibraryIds,
            libraryType = libraryType,
            onBackClick = { navController.navigateUp() },
            onMediaItemClick = { mediaItem: MediaItem ->
                backStackEntry.savedStateHandle[LIBRARY_DETAIL_PRESERVE_CACHE_KEY] = true
                when {
                    mediaItem.type.equals("Episode", ignoreCase = true) -> {
                        mediaItem.seriesId?.let { seriesId ->
                            navController.navigate(
                                Routes.mediaDetail(
                                    seriesId,
                                    seasonNumber = mediaItem.parentIndexNumber,
                                    episodeId = mediaItem.id
                                )
                            )
                        } ?: navController.navigate(Routes.mediaDetail(mediaItem.id))
                    }

                    mediaItem.type.equals("BoxSet", ignoreCase = true) -> {
                        navController.navigate(
                            Routes.collectionDetail(
                                mediaItem.id,
                                mediaItem.name
                            )
                        )
                    }

                    else -> navController.navigate(Routes.mediaDetail(mediaItem.id))
                }
            },
            authViewModel = authViewModel,
            sideNavigationFocusRequester = sideNavigationFocusRequester,
            preserveCache = shouldPreserveCache,
            onCacheHandled = {
                backStackEntry.savedStateHandle.remove<Boolean>(LIBRARY_DETAIL_PRESERVE_CACHE_KEY)
            }
        )
    }

    composable(
        route = Routes.COLLECTION_DETAIL,
        arguments = listOf(
            navArgument("collectionId") { type = NavType.StringType },
            navArgument("collectionName") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
        val collectionNameArg = backStackEntry.arguments?.getString("collectionName") ?: "Collection"
        val collectionName = Uri.decode(collectionNameArg)

        CollectionScreen(
            collectionId = collectionId,
            collectionName = collectionName,
            onBackClick = { navController.navigateUp() },
            onMediaItemClick = { mediaItem ->
                when {
                    mediaItem.type.equals("Episode", ignoreCase = true) && !mediaItem.seriesId.isNullOrBlank() -> {
                        navController.navigate(
                            Routes.mediaDetail(
                                mediaItem.seriesId!!,
                                seasonNumber = mediaItem.parentIndexNumber,
                                episodeId = mediaItem.id
                            )
                        )
                    }

                    mediaItem.type.equals("Episode", ignoreCase = true) -> {
                        navController.navigate(Routes.mediaDetail(mediaItem.id))
                    }

                    mediaItem.type.equals("BoxSet", ignoreCase = true) -> {
                        navController.navigate(
                            Routes.collectionDetail(
                                mediaItem.id,
                                mediaItem.name
                            )
                        )
                    }

                    else -> navController.navigate(Routes.mediaDetail(mediaItem.id))
                }
            },
            authViewModel = authViewModel,
            sideNavigationFocusRequester = sideNavigationFocusRequester
        )
    }

    composable(
        route = Routes.MEDIA_DETAIL,
        arguments = listOf(
            navArgument("mediaId") { type = NavType.StringType },
            navArgument("seasonNumber") {
                type = NavType.IntType
                defaultValue = -1
            },
            navArgument("episodeId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
        val seasonNumber = backStackEntry.arguments?.getInt("seasonNumber")?.takeIf { it >= 0 }
        val episodeId = backStackEntry.arguments?.getString("episodeId")

        MediaDetailScreen(
            mediaId = mediaId,
            seasonNumber = seasonNumber,
            episodeId = episodeId,
            onBackClick = { navController.navigateUp() },
            onPlayClick = { playbackInfo ->
                navController.navigate(
                    Routes.videoPlayer(
                        mediaId = playbackInfo.mediaItem.id,
                        mediaSourceId = playbackInfo.mediaSourceId,
                        audioStreamIndex = playbackInfo.audioStreamIndex,
                        subtitleStreamIndex = playbackInfo.subtitleStreamIndex,
                        startPosition = playbackInfo.startPosition
                    )
                )
            },
            onEpisodeClick = { playbackInfo ->
                navController.navigate(
                    Routes.videoPlayer(
                        mediaId = playbackInfo.mediaItem.id,
                        mediaSourceId = playbackInfo.mediaSourceId,
                        audioStreamIndex = playbackInfo.audioStreamIndex,
                        subtitleStreamIndex = playbackInfo.subtitleStreamIndex,
                        startPosition = playbackInfo.startPosition
                    )
                )
            },
            authViewModel = authViewModel,
            sideNavigationFocusRequester = sideNavigationFocusRequester,
        )
    }

    composable(
        route = Routes.VIDEO_PLAYER,
        arguments = listOf(
            navArgument("mediaId") { type = NavType.StringType },
            navArgument("mediaSourceId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("audioStreamIndex") {
                type = NavType.IntType
                defaultValue = -1
            },
            navArgument("subtitleStreamIndex") {
                type = NavType.IntType
                defaultValue = -1
            },
            navArgument("startPosition") { type = NavType.LongType }
        )
    ) { backStackEntry ->
        val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
        val mediaSourceId = backStackEntry.arguments?.getString("mediaSourceId")
        val audioStreamIndex = backStackEntry.arguments?.getInt("audioStreamIndex")?.takeIf { it >= 0 }
        val subtitleStreamIndex = backStackEntry.arguments?.getInt("subtitleStreamIndex")?.takeIf { it >= 0 }
        val startPositionMs = backStackEntry.arguments?.getLong("startPosition") ?: 0L

        val mediaDetailViewModel: MediaViewModel = hiltViewModel()
        val detailState by mediaDetailViewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(mediaId, authViewModel.state.collectAsState().value.authSession) {
            if (detailState.mediaItem?.id != mediaId) {
                authViewModel.state.value.authSession?.let { session ->
                    mediaDetailViewModel.loadDetails(
                        mediaId = mediaId,
                        userId = session.userId.id,
                        accessToken = session.accessToken,
                        type = MediaViewModel.DetailType.Generic
                    )
                }
            }
        }

        detailState.mediaItem?.let { mediaItem ->
            VideoPlayerScreen(
                mediaItem = mediaItem,
                mediaSourceId = mediaSourceId,
                audioStreamIndex = audioStreamIndex,
                subtitleStreamIndex = subtitleStreamIndex,
                startPositionMs = startPositionMs,
                onBackClick = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "refreshResumeItems",
                        true
                    )
                    navController.popBackStack()
                },
                onPlayNextEpisode = { next ->
                    navController.navigate(Routes.videoPlayer(next.id)) {
                        popUpTo(Routes.VIDEO_PLAYER) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }
    }
}
