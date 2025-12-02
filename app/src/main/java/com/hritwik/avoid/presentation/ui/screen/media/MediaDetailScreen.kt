package com.hritwik.avoid.presentation.ui.screen.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.playback.PlaybackInfo
import com.hritwik.avoid.presentation.ui.components.common.ErrorMessage
import com.hritwik.avoid.presentation.ui.components.common.ThemeSongPlayer
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
import com.hritwik.avoid.presentation.ui.common.focusToSideNavigationOnLeftEdge
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.media.MediaViewModel
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel

@Composable
fun MediaDetailScreen(
    mediaId: String,
    seasonNumber: Int? = null,
    episodeId: String? = null,
    onBackClick: () -> Unit = {},
    onPlayClick: (PlaybackInfo) -> Unit = {},
    onEpisodeClick: (PlaybackInfo) -> Unit = {},
    authViewModel: AuthServerViewModel,
    mediaViewModel: MediaViewModel = hiltViewModel(),
    userDataViewModel: UserDataViewModel = hiltViewModel(),
    sideNavigationFocusRequester: FocusRequester? = null,
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val detailState by mediaViewModel.state.collectAsStateWithLifecycle()
    val playbackSettings by userDataViewModel.playbackSettings.collectAsStateWithLifecycle()
    val videoPlaybackViewModel: VideoPlaybackViewModel = hiltViewModel()
    var mediaType by remember(mediaId) { mutableStateOf<String?>(null) }
    var initialSeasonHandled by remember(mediaId) { mutableStateOf(false) }
    var focusedEpisode by remember(mediaId) { mutableStateOf<MediaItem?>(null) }

    ThemeSongPlayer(detailState.themeSongUrl, playbackSettings.themeSongVolume)

    LaunchedEffect(mediaId, authState.authSession) {
        authState.authSession?.let { session ->
            userDataViewModel.loadPlayedItems(session.userId.id)
            userDataViewModel.loadFavorites(session.userId.id, session.accessToken)
            val detailType = when (mediaType?.lowercase()) {
                "movie" -> MediaViewModel.DetailType.Movie
                "series" -> MediaViewModel.DetailType.Series
                "season" -> MediaViewModel.DetailType.Season
                else -> MediaViewModel.DetailType.Generic
            }

            mediaViewModel.loadDetails(
                mediaId = mediaId,
                userId = session.userId.id,
                accessToken = session.accessToken,
                type = detailType,
                initialSeasonNumber = seasonNumber,
                initialEpisodeId = episodeId
            )
        }
    }

    LaunchedEffect(detailState.seasons, seasonNumber, authState.authSession) {
        if (!initialSeasonHandled && seasonNumber != null && detailState.seasons != null) {
            val season = detailState.seasons!!.firstOrNull { it.indexNumber == seasonNumber }
            if (season != null) {
                authState.authSession?.let { session ->
                    mediaViewModel.loadEpisodesForSeason(
                        season.id,
                        session.userId.id,
                        session.accessToken
                    )
                    initialSeasonHandled = true
                }
            }
        }
    }

    when {
        detailState.isLoading -> {
            LoadingState()
        }

        detailState.error != null -> {
            ErrorMessage(
                error = detailState.error!!,
                onRetry = {
                    authState.authSession?.let { session ->
                        val detailType = when (mediaType?.lowercase()) {
                            "movie" -> MediaViewModel.DetailType.Movie
                            "series" -> MediaViewModel.DetailType.Series
                            "season" -> MediaViewModel.DetailType.Season
                            else -> MediaViewModel.DetailType.Generic
                        }
                        mediaViewModel.loadDetails(
                            mediaId = mediaId,
                            userId = session.userId.id,
                            accessToken = session.accessToken,
                            type = detailType,
                            initialSeasonNumber = seasonNumber,
                            initialEpisodeId = episodeId
                        )
                    }
                },
                onDismiss = { mediaViewModel.clearError() }
            )
        }

        detailState.mediaItem != null -> {
            val item = detailState.mediaItem!!
            val type = when (item.type.lowercase()) {
                "movie" -> MediaType.MOVIE
                "series" -> MediaType.SHOW
                "season" -> MediaType.SEASON
                else -> MediaType.MOVIE
            }
            val seasons = detailState.seasons ?: emptyList()
            val initialSeasonId = remember(detailState.seasons, seasonNumber) {
                if (seasonNumber != null) {
                    detailState.seasons?.firstOrNull { it.indexNumber == seasonNumber }?.id
                } else {
                    null
                }
            }
            val episodes = detailState.episodes ?: emptyList()
            val initialEpisodeIndex = remember(detailState.episodes, episodeId) {
                val list = detailState.episodes ?: emptyList()
                if (episodeId != null) {
                    val idx = list.indexOfFirst { it.id == episodeId }
                    if (idx >= 0) idx else 0
                } else 0
            }

            LaunchedEffect(episodes) {
                val focusedId = focusedEpisode?.id
                if (focusedId != null && episodes.none { it.id == focusedId }) {
                    focusedEpisode = null
                }
            }

            MediaDetailContent(
                mediaType = type,
                mediaItem = item,
                serverUrl = authState.authSession?.server?.url ?: "",
                episodes = episodes,
                seasons = seasons,
                specialFeatures = detailState.specialFeatures,
                initialSeasonId = initialSeasonId,
                initialEpisodeIndex = initialEpisodeIndex,
                shouldAutoFocusEpisode = episodeId != null,
                focusedEpisode = focusedEpisode,
                modifier = Modifier
                    .focusToSideNavigationOnLeftEdge(sideNavigationFocusRequester),
                onPlayClick = onPlayClick,
                onEpisodeClick = onEpisodeClick,
                onEpisodeFocused = { episode ->
                    focusedEpisode = episode
                    authState.authSession?.let { session ->
                        videoPlaybackViewModel.initializeVideoOptions(
                            mediaItem = episode,
                            userId = session.userId.id,
                            accessToken = session.accessToken
                        )
                    }
                },
                onSpecialFeatureFocused = { _ -> },
                onEpisodeUnfocused = { episode ->
                    if (focusedEpisode?.id == episode.id) {
                        focusedEpisode = null
                    }
                },
                onSeasonSelected = { seasonId ->
                    authState.authSession?.let { session ->
                        mediaViewModel.loadEpisodesForSeason(
                            seasonId,
                            session.userId.id,
                            session.accessToken
                        )
                    }
                },
                sideNavigationFocusRequester = sideNavigationFocusRequester,
                videoPlaybackViewModel = videoPlaybackViewModel
            )
        }
    }
}

