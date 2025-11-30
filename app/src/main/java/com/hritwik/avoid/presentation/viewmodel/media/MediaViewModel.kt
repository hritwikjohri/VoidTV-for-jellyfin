package com.hritwik.avoid.presentation.viewmodel.media

import androidx.lifecycle.viewModelScope
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.usecase.library.GetMediaItemDetailLocalUseCase
import com.hritwik.avoid.domain.usecase.media.GetEpisodesUseCase
import com.hritwik.avoid.domain.usecase.media.GetMediaItemDetailUseCase
import com.hritwik.avoid.domain.usecase.media.GetSeasonsUseCase
import com.hritwik.avoid.domain.usecase.media.GetRelatedResourcesBatchUseCase
import com.hritwik.avoid.domain.usecase.media.GetThemeSongsUseCase
import com.hritwik.avoid.data.common.RepositoryCache
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.usecase.library.GetNextUpUseCase
import com.hritwik.avoid.presentation.ui.state.MediaDetailState
import com.hritwik.avoid.utils.helpers.ConnectivityObserver
import com.hritwik.avoid.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val getMediaItemDetailLocalUseCase: GetMediaItemDetailLocalUseCase,
    private val getMediaItemDetailUseCase: GetMediaItemDetailUseCase,
    private val getRelatedResourcesBatchUseCase: GetRelatedResourcesBatchUseCase,
    private val getSeasonsUseCase: GetSeasonsUseCase,
    private val getEpisodesUseCase: GetEpisodesUseCase,
    private val getNextUpUseCase: GetNextUpUseCase,
    private val getThemeSongsUseCase: GetThemeSongsUseCase,
    private val preferencesManager: PreferencesManager,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel(connectivityObserver) {

    private val cache = RepositoryCache()
    private var pendingSeasonNumber: Int? = null
    private var pendingEpisodeId: String? = null
    private var lastRequestedSeasonNumber: Int? = null
    private var lastRequestedEpisodeId: String? = null

    sealed class DetailType {
        object Movie : DetailType()
        object Series : DetailType()
        object Season : DetailType()
        object Generic : DetailType()
    }

    private val _state = MutableStateFlow(MediaDetailState())
    val state: StateFlow<MediaDetailState> = _state.asStateFlow()

    fun loadDetails(
        mediaId: String,
        userId: String,
        accessToken: String,
        type: DetailType = DetailType.Generic,
        initialSeasonNumber: Int? = null,
        initialEpisodeId: String? = null
    ) {
        viewModelScope.launch {
            if (initialSeasonNumber != null || initialEpisodeId != null) {
                pendingSeasonNumber = initialSeasonNumber
                pendingEpisodeId = initialEpisodeId
            }
            if (initialSeasonNumber != null) {
                lastRequestedSeasonNumber = initialSeasonNumber
            }
            if (initialEpisodeId != null) {
                lastRequestedEpisodeId = initialEpisodeId
            }
            _state.value = MediaDetailState(isLoading = true)
            try {
                val isOffline = !isConnected.value
                if (isOffline) {
                    val local = getMediaItemDetailLocalUseCase(
                        GetMediaItemDetailLocalUseCase.Params(userId, mediaId)
                    )
                    if (local != null) {
                        _state.value = _state.value.copy(
                            mediaItem = local,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = AppError.Unknown("Media details unavailable offline")
                        )
                    }
                    return@launch
                }

                val detailResult = cache.get("detail_$mediaId") {
                    getMediaItemDetailUseCase(
                        GetMediaItemDetailUseCase.Params(userId, mediaId, accessToken)
                    )
                }
                when (detailResult) {
                    is NetworkResult.Success -> {
                        _state.value = _state.value.copy(
                            mediaItem = detailResult.data,
                            isLoading = false,
                            error = null
                        )

                        
                        val actualType = if (type == DetailType.Generic) {
                            when (detailResult.data.type.lowercase()) {
                                "movie" -> DetailType.Movie
                                "series" -> DetailType.Series
                                "season" -> DetailType.Season
                                else -> DetailType.Generic
                            }
                        } else {
                            type
                        }

                        
                        launch { loadRelated(mediaId, userId, accessToken) }
                        launch { loadThemeSong(detailResult.data, accessToken) }

                        when (actualType) {
                            DetailType.Series -> {
                                _state.value = _state.value.copy(episodes = null)
                                launch { loadSeasons(mediaId, userId, accessToken) }
                            }
                            DetailType.Season -> {
                                launch { loadEpisodes(mediaId, userId, accessToken) }
                                detailResult.data.seriesId?.let { seriesId ->
                                    launch { loadSeasons(seriesId, userId, accessToken) }
                                }
                            }
                            else -> {}
                        }
                    }
                    is NetworkResult.Error -> {
                        _state.value = _state.value.copy(isLoading = false, error = detailResult.error)
                    }
                    is NetworkResult.Loading -> Unit
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AppError.Unknown("Failed to load media details: ${e.message}")
                )
            }
        }
    }

    private suspend fun loadRelated(mediaId: String, userId: String, accessToken: String) {
        when (val result = cache.get("related_$mediaId") {
            getRelatedResourcesBatchUseCase(
                GetRelatedResourcesBatchUseCase.Params(mediaId, userId, accessToken)
            )
        }) {
            is NetworkResult.Success -> {
                _state.value = _state.value.copy(
                    similarItems = result.data.similar,
                    specialFeatures = result.data.special
                )
            }
            else -> {  }
        }
    }

    private suspend fun loadSeasons(seriesId: String, userId: String, accessToken: String) {
        when (val result = cache.get("seasons_$seriesId") {
            getSeasonsUseCase(
                GetSeasonsUseCase.Params(userId, seriesId, accessToken)
            )
        }) {
            is NetworkResult.Success -> {
                val sorted = result.data.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
                _state.value = _state.value.copy(seasons = sorted)

                val requestedSeasonNumber = pendingSeasonNumber
                val targetSeason = when {
                    requestedSeasonNumber != null -> {
                        sorted.firstOrNull { it.indexNumber == requestedSeasonNumber }
                            ?: sorted.firstOrNull()
                    }
                    _state.value.episodes.isNullOrEmpty() -> sorted.firstOrNull()
                    else -> null
                }

                if (targetSeason != null) {
                    loadEpisodes(targetSeason.id, userId, accessToken)
                }

                pendingSeasonNumber = null
                pendingEpisodeId = null
            }
            else -> {  }
        }
    }

    private suspend fun loadEpisodes(seasonId: String, userId: String, accessToken: String) {
        when (val result = cache.get("episodes_$seasonId") {
            getEpisodesUseCase(
                GetEpisodesUseCase.Params(userId, seasonId, accessToken)
            )
        }) {
            is NetworkResult.Success -> {
                val sorted = result.data.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
                _state.value = _state.value.copy(episodes = sorted)
            }
            else -> {  }
        }
    }

    fun loadEpisodesForSeason(seasonId: String, userId: String, accessToken: String) {
        viewModelScope.launch {
            loadEpisodes(seasonId, userId, accessToken)
        }
    }


    private suspend fun loadThemeSong(mediaItem: MediaItem, accessToken: String) {
        val enabled = try { preferencesManager.getPlayThemeSongs().first() } catch (_: Exception) { false }
        if (!enabled) {
            _state.value = _state.value.copy(themeSongUrl = null)
            return
        }

        val localThemeUrl = when (val result = cache.get("themes_${mediaItem.id}") {
            getThemeSongsUseCase(
                GetThemeSongsUseCase.Params(mediaItem.id, accessToken)
            )
        }) {
            is NetworkResult.Success -> resolveLocalThemeSongUrl(result.data.firstOrNull(), accessToken)
            else -> null
        }

        val fallbackBase = runCatching { preferencesManager.getThemeSongFallbackUrl().first() }.getOrDefault("")
        val resolvedUrl = localThemeUrl ?: buildFallbackThemeSongUrl(mediaItem.tvdbId, fallbackBase)
        _state.value = _state.value.copy(themeSongUrl = resolvedUrl)
    }

    private suspend fun resolveLocalThemeSongUrl(song: MediaItem?, accessToken: String): String? {
        if (song == null) return null
        val serverUrl = preferencesManager.getServerUrl().first() ?: return null
        val mediaSource = song.mediaSources.firstOrNull() ?: return null
        val container = mediaSource.container?.lowercase() ?: "mp3"
        return buildString {
            append(serverUrl.removeSuffix("/"))
            append("/Audio/")
            append(song.id)
            append("/stream.")
            append(container)
            append("?static=true&mediaSourceId=")
            append(mediaSource.id)
            append("&api_key=")
            append(accessToken)
        }
    }

    private fun buildFallbackThemeSongUrl(tvdbId: String?, fallbackBaseUrl: String?): String? {
        val id = tvdbId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val base = fallbackBaseUrl?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: return null
        return "$base/$id.mp3"
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun refresh(mediaId: String, userId: String, accessToken: String, type: DetailType = DetailType.Generic) {
        val seasonNumber = lastRequestedSeasonNumber
        val episodeId = lastRequestedEpisodeId
        pendingSeasonNumber = seasonNumber
        pendingEpisodeId = episodeId
        loadDetails(
            mediaId = mediaId,
            userId = userId,
            accessToken = accessToken,
            type = type,
            initialSeasonNumber = seasonNumber,
            initialEpisodeId = episodeId
        )
    }

    fun clearState() {
        _state.value = MediaDetailState()
    }
}
