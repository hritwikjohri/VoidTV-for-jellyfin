package com.hritwik.avoid.presentation.viewmodel.user

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.data.cache.CacheManager
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.common.RepositoryCache
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.playback.DecoderMode
import com.hritwik.avoid.domain.model.playback.DisplayMode
import com.hritwik.avoid.domain.model.playback.HdrFormatPreference
import com.hritwik.avoid.domain.model.playback.PlayerType
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.presentation.viewmodel.BaseViewModel
import com.hritwik.avoid.utils.MpvConfig
import com.hritwik.avoid.utils.constants.PreferenceConstants
import com.hritwik.avoid.utils.helpers.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class UserDataViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val preferencesManager: PreferencesManager,
    @param:ApplicationContext private val context: Context,
    private val cacheManager: CacheManager,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel(connectivityObserver) {

    private val cache = RepositoryCache()

    private val _mpvConfig = MutableStateFlow("")
    val mpvConfig: StateFlow<String> = _mpvConfig.asStateFlow()

    private val _favorites = MutableStateFlow<List<MediaItem>>(emptyList())
    val favorites: StateFlow<List<MediaItem>> = _favorites.asStateFlow()
    private val _playedItems = MutableStateFlow<Set<String>>(emptySet())

    fun isFavorite(mediaId: String): StateFlow<Boolean> =
        favorites
            .map { list -> list.any { it.id == mediaId } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)


    data class PlaybackSettings(
        val playThemeSongs: Boolean,
        val themeSongVolume: Int,
        val themeSongFallbackUrl: String,
        val displayMode: DisplayMode,
        val decoderMode: DecoderMode,
        val audioPassthroughEnabled: Boolean,
        val autoSkipSegments: Boolean,
        val externalPlayerEnabled: Boolean,
        val playerType: PlayerType,
        val directPlayEnabled: Boolean,
        val hdrFormatPreference: HdrFormatPreference
    )

    private data class PlaybackSettingsBundle(
        val playThemeSongs: Boolean,
        val themeSongVolume: Int,
        val themeSongFallbackUrl: String,
        val displayMode: DisplayMode,
        val decoderMode: DecoderMode,
        val audioPassthroughEnabled: Boolean,
        val autoSkipSegments: Boolean,
        val externalPlayerEnabled: Boolean = PreferenceConstants.DEFAULT_EXTERNAL_PLAYER_ENABLED,
        val directPlayEnabled: Boolean = PreferenceConstants.DEFAULT_DIRECT_PLAY_ENABLED,
        val hdrFormatPreference: HdrFormatPreference = HdrFormatPreference.AUTO,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _mpvConfig.value = MpvConfig.readConfig(context)
        }
    }

    val playbackSettings: StateFlow<PlaybackSettings> =
        combine(
            preferencesManager.getPlayThemeSongs(),
            preferencesManager.getThemeSongVolume(),
            preferencesManager.getThemeSongFallbackUrl(),
            preferencesManager.getDisplayMode(),
            preferencesManager.getDecoderMode()
        ) { playTheme, volume, fallbackUrl, display, decoder ->
            PlaybackSettingsBundle(
                playThemeSongs = playTheme,
                themeSongVolume = volume,
                themeSongFallbackUrl = fallbackUrl,
                displayMode = display,
                decoderMode = decoder,
                audioPassthroughEnabled = PreferenceConstants.DEFAULT_AUDIO_PASSTHROUGH_ENABLED,
                autoSkipSegments = PreferenceConstants.DEFAULT_AUTO_SKIP_SEGMENTS,
                directPlayEnabled = PreferenceConstants.DEFAULT_DIRECT_PLAY_ENABLED,
                hdrFormatPreference = HdrFormatPreference.fromValue(
                    PreferenceConstants.DEFAULT_HDR_FORMAT_PREFERENCE
                )
            )
        }
            .combine(preferencesManager.getAudioPassthroughEnabled()) { bundle, passthrough ->
                bundle.copy(audioPassthroughEnabled = passthrough)
            }
            .combine(preferencesManager.getAutoSkipSegments()) { bundle, autoSkip ->
                bundle.copy(autoSkipSegments = autoSkip)
            }
            .combine(preferencesManager.getExternalPlayerEnabled()) { bundle, externalPlayer ->
                bundle.copy(externalPlayerEnabled = externalPlayer)
            }
            .combine(preferencesManager.getPreferHdrOverDolbyVision()) { bundle, preferHdr ->
                if (bundle.hdrFormatPreference == HdrFormatPreference.AUTO && preferHdr) {
                    bundle.copy(hdrFormatPreference = HdrFormatPreference.HDR10_PLUS)
                } else {
                    bundle
                }
            }
            .combine(preferencesManager.getHdrFormatPreference()) { bundle, hdrPreference ->
                bundle.copy(hdrFormatPreference = hdrPreference)
            }
            .combine(preferencesManager.getPlayerType()) { bundle, player ->
                bundle.copy(externalPlayerEnabled = bundle.externalPlayerEnabled)
                PlaybackSettings(
                    playThemeSongs = bundle.playThemeSongs,
                    themeSongVolume = bundle.themeSongVolume,
                    themeSongFallbackUrl = bundle.themeSongFallbackUrl,
                    displayMode = bundle.displayMode,
                    decoderMode = bundle.decoderMode,
                    audioPassthroughEnabled = bundle.audioPassthroughEnabled,
                    autoSkipSegments = bundle.autoSkipSegments,
                    externalPlayerEnabled = bundle.externalPlayerEnabled,
                    playerType = player,
                    directPlayEnabled = bundle.directPlayEnabled,
                    hdrFormatPreference = bundle.hdrFormatPreference
                )
            }
            .combine(preferencesManager.getDirectPlayEnabled()) { settings, directPlay ->
                settings.copy(directPlayEnabled = directPlay)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                    PlaybackSettings(
                        false,
                        PreferenceConstants.DEFAULT_THEME_SONG_VOLUME,
                        PreferenceConstants.DEFAULT_THEME_SONG_FALLBACK_URL,
                        DisplayMode.FIT_SCREEN,
                        DecoderMode.fromValue(PreferenceConstants.DEFAULT_DECODER_MODE),
                        PreferenceConstants.DEFAULT_AUDIO_PASSTHROUGH_ENABLED,
                    false,
                    PreferenceConstants.DEFAULT_EXTERNAL_PLAYER_ENABLED,
                    PlayerType.fromValue(PreferenceConstants.DEFAULT_PLAYER_TYPE),
                    PreferenceConstants.DEFAULT_DIRECT_PLAY_ENABLED,
                    HdrFormatPreference.fromValue(PreferenceConstants.DEFAULT_HDR_FORMAT_PREFERENCE)
                )
            )

    data class PersonalizationSettings(
        val themeMode: String,
        val fontScale: Float,
        val language: String,
        val gesturesEnabled: Boolean,
        val highContrast: Boolean,
    )

    val personalizationSettings: StateFlow<PersonalizationSettings> =
        combine(
            preferencesManager.getThemeMode(),
            preferencesManager.getFontScale(),
            preferencesManager.getPreferredLanguage(),
            preferencesManager.getGestureControlsEnabled(),
            preferencesManager.getHighContrastEnabled(),
        ) { theme, scale, lang, gestures, contrast ->
            PersonalizationSettings(theme, scale, lang, gestures, contrast)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PersonalizationSettings(
                PreferenceConstants.DEFAULT_THEME_MODE,
                PreferenceConstants.DEFAULT_FONT_SCALE,
                PreferenceConstants.DEFAULT_PREFERRED_LANGUAGE,
                false,
                highContrast = false,
            )
        )

    val subtitleSize: StateFlow<String> = preferencesManager.getSubtitleSize()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PreferenceConstants.DEFAULT_SUBTITLE_SIZE
        )


    fun setPlayThemeSongs(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.savePlayThemeSongs(enabled) }
    }

    fun setThemeSongVolume(volume: Int) {
        val clamped = volume.coerceIn(1, 10)
        viewModelScope.launch { preferencesManager.saveThemeSongVolume(clamped) }
    }

    fun setThemeSongFallbackUrl(url: String) {
        viewModelScope.launch { preferencesManager.saveThemeSongFallbackUrl(url) }
    }

    fun setDisplayMode(mode: DisplayMode) {
        viewModelScope.launch { preferencesManager.saveDisplayMode(mode) }
    }

    fun setDecoderMode(mode: DecoderMode) {
        viewModelScope.launch { preferencesManager.saveDecoderMode(mode) }
    }

    fun setPlayerType(playerType: PlayerType) {
        viewModelScope.launch { preferencesManager.savePlayerType(playerType) }
    }

    fun setAudioPassthroughEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveAudioPassthroughEnabled(enabled) }
    }

    fun setAutoSkipSegments(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveAutoSkipSegments(enabled) }
    }

    fun setExternalPlayerEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveExternalPlayerEnabled(enabled) }
    }

    fun setDirectPlayEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveDirectPlayEnabled(enabled) }
    }

    fun setPreferHdrOverDolbyVision(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.savePreferHdrOverDolbyVision(enabled) }
    }

    fun setHdrFormatPreference(preference: HdrFormatPreference) {
        viewModelScope.launch { preferencesManager.saveHdrFormatPreference(preference) }
    }

    fun setSubtitleSize(size: String) {
        viewModelScope.launch { preferencesManager.saveSubtitleSize(size) }
    }

    fun refreshMpvConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            _mpvConfig.value = MpvConfig.readConfig(context)
        }
    }

    fun saveMpvConfig(content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _mpvConfig.value = MpvConfig.writeConfig(context, content)
        }
    }





    fun clearCache(): Boolean {
        cacheManager.clearAll()
        return true
    }


    fun clearDownloads(): Boolean {
        return deleteDir(File(context.cacheDir, "downloads"))
    }

    fun loadPlayedItems(userId: String) {
        viewModelScope.launch {
            libraryRepository.getPlayedItems(userId).collectLatest { items ->
                _playedItems.value = items.map { it.id }.toSet()
            }
        }
    }

    fun loadFavorites(userId: String, accessToken: String) {
        viewModelScope.launch {
            libraryRepository.getFavoriteItems(userId).collectLatest { items ->
                _favorites.value = items
            }
        }

        viewModelScope.launch {
            when (cache.get("favorites_$userId") {
                libraryRepository.getFavoriteItems(userId, accessToken, 50)
            }) {
                is NetworkResult.Error -> Unit
                else -> Unit
            }
        }
    }

    fun toggleFavorite(
        userId: String,
        mediaId: String,
        accessToken: String,
        isFavorite: Boolean,
        mediaItem: MediaItem? = null
    ) {
        val newFavorite = !isFavorite

        viewModelScope.launch {
            val result = libraryRepository.toggleFavorite(userId, mediaId, accessToken, newFavorite, mediaItem)
            if (result is NetworkResult.Success) {
                loadFavorites(userId, accessToken)
            }
        }

        viewModelScope.launch {
            val item = mediaItem
                ?: libraryRepository.getMediaItemDetailLocal(userId, mediaId)
                ?: when (val res = cache.get("detail_$mediaId") {
                    libraryRepository.getMediaItemDetail(userId, mediaId, accessToken)
                }) {
                    is NetworkResult.Success -> res.data
                    else -> null
                }

            _favorites.value = if (newFavorite) {
                item?.let { _favorites.value + it } ?: _favorites.value
            } else {
                _favorites.value.filterNot { it.id == mediaId }
            }
        }
    }

    fun clearUserData() {
        viewModelScope.launch {
            preferencesManager.clearAllPreferences()
        }
    }

    companion object {
        fun deleteDir(dir: File): Boolean {
            val result = dir.deleteRecursively()
            return result
        }
    }
}
