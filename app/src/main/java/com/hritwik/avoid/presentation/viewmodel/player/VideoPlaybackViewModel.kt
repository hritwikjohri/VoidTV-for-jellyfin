package com.hritwik.avoid.presentation.viewmodel.player

import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.MediaItemDao
import com.hritwik.avoid.data.local.model.PlaybackPreferences
import com.hritwik.avoid.data.network.MtlsProxyServer
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.media.PlaybackOptions
import com.hritwik.avoid.domain.model.media.VideoQuality
import com.hritwik.avoid.domain.model.playback.HdrFormatPreference
import com.hritwik.avoid.domain.model.playback.PlaybackStreamInfo
import com.hritwik.avoid.domain.model.playback.PlaybackTranscodeOption
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.library.GetMediaDetailUseCase
import com.hritwik.avoid.presentation.ui.state.TrackChangeEvent
import com.hritwik.avoid.presentation.ui.state.VideoPlaybackState
import com.hritwik.avoid.presentation.viewmodel.BaseViewModel
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.extensions.getStreamContainer
import com.hritwik.avoid.utils.helpers.ConnectivityObserver
import com.hritwik.avoid.utils.helpers.CodecDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.MediaStreamType
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class VideoPlaybackViewModel @Inject constructor(
    private val mediaItemDao: MediaItemDao,
    private val preferencesManager: PreferencesManager,
    private val libraryRepository: LibraryRepository,
    private val getMediaDetailUseCase: GetMediaDetailUseCase,
    private val mtlsProxyServer: MtlsProxyServer,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel(connectivityObserver) {
    companion object {
        private const val MIN_SUBTITLE_OFFSET_MS = -5000L
        private const val MAX_SUBTITLE_OFFSET_MS = 5000L
    }

    private val _state = MutableStateFlow(VideoPlaybackState())
    val state: StateFlow<VideoPlaybackState> = _state.asStateFlow()
    private val _trackChangeEvents = MutableSharedFlow<TrackChangeEvent>()
    val trackChangeEvents: SharedFlow<TrackChangeEvent> = _trackChangeEvents.asSharedFlow()
    private var currentMediaSourceId: String? = null
    private var currentAudioStreamIndex: Int? = null
    private var currentSubtitleStreamIndex: Int? = null
    private var savedUserId: String? = null
    private var savedAccessToken: String? = null
    private var savedServerUrl: String? = null
    private var activeTranscodeJob: Job? = null
    private var initializeOptionsJob: Job? = null

    private fun proxiedUrl(url: String?, token: String?): String? {
        if (url.isNullOrBlank()) return url
        return mtlsProxyServer.proxiedUrlFor(url) ?: url
    }

    private data class ResolvedStreamSelection(
        val audioStreams: List<MediaStream>,
        val subtitleStreams: List<MediaStream>,
        val selectedAudio: MediaStream?,
        val selectedSubtitle: MediaStream?,
        val selectedVideo: MediaStream?
    )

    private fun parseDolbyVisionProfile(value: String?): Int? {
        val text = value ?: return null
        val match = Regex("""profile\s+(\d+)""", RegexOption.IGNORE_CASE).find(text)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun getDolbyVisionProfile(stream: MediaStream): Int? {
        return stream.dvProfile
            ?: parseDolbyVisionProfile(stream.videoDoViTitle)
            ?: parseDolbyVisionProfile(stream.displayTitle)
    }

    private fun isDolbyVisionStream(stream: MediaStream): Boolean {
        val doViTitle = stream.videoDoViTitle
        if (doViTitle?.contains("dolby", ignoreCase = true) == true) return true
        val displayTitle = stream.displayTitle
        if (displayTitle?.contains("dolby", ignoreCase = true) == true) return true
        val rangeType = stream.videoRangeType
        return rangeType?.contains("dovi", ignoreCase = true) == true ||
            rangeType?.contains("dolby", ignoreCase = true) == true
    }

    private fun isHdrStream(stream: MediaStream): Boolean {
        return stream.videoRangeType?.contains("HDR", ignoreCase = true) == true ||
            stream.videoRange?.contains("HDR", ignoreCase = true) == true
    }

    private fun getSupportedDolbyVisionProfiles(): Set<Int> {
        return CodecDetector.getDolbyVisionProfiles()
            .mapNotNull { parseDolbyVisionProfile(it) }
            .toSet()
    }

    private fun pickBestStream(streams: List<MediaStream>): MediaStream? {
        return streams.firstOrNull { it.isDefault }
            ?: streams.maxByOrNull { it.height ?: 0 }
            ?: streams.firstOrNull()
    }

    private fun pickBestHdrStream(streams: List<MediaStream>): MediaStream? {
        val hdrStreams = streams.filter { isHdrStream(it) }
        val candidates = if (hdrStreams.isNotEmpty()) hdrStreams else streams
        return candidates.maxByOrNull { it.height ?: 0 } ?: candidates.firstOrNull()
    }

    private fun selectVideoStream(
        videoStreams: List<MediaStream>,
        preferredQuality: VideoQuality?,
        hdrPreference: HdrFormatPreference,
        fallback: MediaStream?
    ): MediaStream? {
        val qualityCandidates = preferredQuality?.let { q ->
            videoStreams.filter { it.videoQuality == q }
        }.orEmpty()
        val candidates = if (qualityCandidates.isNotEmpty()) qualityCandidates else videoStreams
        if (candidates.isEmpty()) return fallback

        if (hdrPreference != HdrFormatPreference.AUTO) {
            return pickBestStream(candidates) ?: fallback
        }

        val supportedDvProfiles = getSupportedDolbyVisionProfiles()
        val supportsDolbyVision = supportedDvProfiles.isNotEmpty()

        fun isDvAllowed(stream: MediaStream): Boolean {
            val profile = getDolbyVisionProfile(stream)
            if (profile == 7) return false
            if (!supportsDolbyVision) return false
            return profile == null || supportedDvProfiles.contains(profile)
        }

        val dvStreams = candidates.filter { isDolbyVisionStream(it) }
        val nonDvStreams = candidates.filterNot { isDolbyVisionStream(it) }
        val hdrCandidate = pickBestHdrStream(nonDvStreams)

        if (dvStreams.isEmpty()) {
            return pickBestStream(candidates) ?: fallback
        }

        val allowedDvStreams = dvStreams.filter { isDvAllowed(it) }
        if (allowedDvStreams.isNotEmpty()) {
            return pickBestStream(allowedDvStreams) ?: hdrCandidate ?: fallback
        }

        val hdrFallback = hdrCandidate
            ?: pickBestHdrStream(videoStreams.filterNot { isDolbyVisionStream(it) })
        return hdrFallback ?: pickBestStream(candidates) ?: fallback
    }

    private fun resolveStreamsFromPlaybackInfo(
        playbackStreams: List<MediaStream>,
        fallbackAudioStreams: List<MediaStream>,
        fallbackSubtitleStreams: List<MediaStream>,
        fallbackVideoStream: MediaStream?,
        audioIndex: Int?,
        subtitleIndex: Int?
    ): ResolvedStreamSelection {
        if (playbackStreams.isEmpty()) {
            return ResolvedStreamSelection(
                audioStreams = fallbackAudioStreams,
                subtitleStreams = fallbackSubtitleStreams,
                selectedAudio = audioIndex?.let { idx ->
                    fallbackAudioStreams.firstOrNull { it.index == idx }
                },
                selectedSubtitle = subtitleIndex?.let { idx ->
                    fallbackSubtitleStreams.firstOrNull { it.index == idx }
                },
                selectedVideo = fallbackVideoStream
            )
        }

        val audioStreams = playbackStreams.filter { it.type == MediaStreamType.AUDIO }
        val subtitleStreams = playbackStreams.filter { it.type == MediaStreamType.SUBTITLE }
        val videoStreams = playbackStreams.filter { it.type == MediaStreamType.VIDEO }

        val selectedAudio = audioIndex?.let { idx ->
            audioStreams.firstOrNull { it.index == idx }
        } ?: audioStreams.firstOrNull { it.isDefault } ?: audioStreams.firstOrNull()

        val selectedSubtitle = subtitleIndex?.let { idx ->
            subtitleStreams.firstOrNull { it.index == idx }
        } ?: subtitleStreams.firstOrNull { it.isDefault }

        val selectedVideo = selectVideoStream(
            videoStreams = videoStreams,
            preferredQuality = _state.value.preferredVideoQuality,
            hdrPreference = _state.value.hdrFormatPreference,
            fallback = fallbackVideoStream
        )

        val resolvedAudioStreams = if (audioStreams.isNotEmpty()) audioStreams else fallbackAudioStreams
        val resolvedSubtitleStreams = if (subtitleStreams.isNotEmpty()) subtitleStreams else fallbackSubtitleStreams

        val resolvedSelectedAudio = if (audioStreams.isNotEmpty()) {
            selectedAudio
        } else {
            audioIndex?.let { idx -> fallbackAudioStreams.firstOrNull { it.index == idx } }
                ?: fallbackAudioStreams.firstOrNull { it.isDefault }
                ?: fallbackAudioStreams.firstOrNull()
        }

        val resolvedSelectedSubtitle = if (subtitleStreams.isNotEmpty()) {
            selectedSubtitle
        } else {
            subtitleIndex?.let { idx -> fallbackSubtitleStreams.firstOrNull { it.index == idx } }
                ?: fallbackSubtitleStreams.firstOrNull { it.isDefault }
        }

        return ResolvedStreamSelection(
            audioStreams = resolvedAudioStreams,
            subtitleStreams = resolvedSubtitleStreams,
            selectedAudio = resolvedSelectedAudio,
            selectedSubtitle = resolvedSelectedSubtitle,
            selectedVideo = selectedVideo ?: fallbackVideoStream
        )
    }

    init {
        viewModelScope.launch {
            preferencesManager.getHdrFormatPreference().collectLatest { preference ->
                _state.update { it.copy(hdrFormatPreference = preference) }
            }
        }
    }

    fun initializeVideoOptions(
        mediaItem: MediaItem,
        userId: String,
        accessToken: String
    ) {
        savedUserId = userId
        savedAccessToken = accessToken
        initializeOptionsJob?.cancel()
        initializeOptionsJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            if (!mediaItem.hasPlaybackDataLoaded()) {
                val isOffline = !isConnected.value
                when (val result = getMediaDetailUseCase(
                    GetMediaDetailUseCase.Params(mediaItem.id, userId, accessToken)
                )) {
                    is NetworkResult.Success -> {
                        setupVideoOptions(result.data)
                    }
                    is NetworkResult.Error -> {
                        val errorMessage = if (isOffline) "No network connection" else result.message
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                    is NetworkResult.Loading -> {}
                }
            } else {
                setupVideoOptions(mediaItem)
            }
        }.also { job ->
            job.invokeOnCompletion {
                if (initializeOptionsJob == job) {
                    initializeOptionsJob = null
                }
            }
        }
    }

    private suspend fun setupVideoOptions(mediaItem: MediaItem) {
        val primarySource = mediaItem.getPrimaryMediaSource()

        val preferredAudioLanguage = preferencesManager.getAudioTrackLanguage().first()
        val preferredSubtitleLanguage = preferencesManager.getSubtitleLanguage().first()
        val savedPrefs = preferencesManager.getPlaybackPreferences(mediaItem.id).first()
        val savedTranscodeOption = savedPrefs?.transcodeOption?.let { option ->
            runCatching { PlaybackTranscodeOption.valueOf(option) }.getOrNull()
        } ?: PlaybackTranscodeOption.ORIGINAL

        val selectedMediaSource = savedPrefs?.mediaSourceId?.let { id ->
            mediaItem.mediaSources.firstOrNull { it.id == id }
        } ?: primarySource

        val subtitleStreams = selectedMediaSource?.subtitleStreams ?: emptyList()

        val preferredQuality = savedPrefs?.videoQuality?.let { qualityStr ->
            runCatching { VideoQuality.valueOf(qualityStr) }.getOrNull()
        }
        val selectedVideoStream = selectVideoStream(
            videoStreams = selectedMediaSource?.videoStreams.orEmpty(),
            preferredQuality = preferredQuality,
            hdrPreference = _state.value.hdrFormatPreference,
            fallback = selectedMediaSource?.defaultVideoStream
        )

        val selectedAudioStream = savedPrefs?.audioIndex?.let { idx ->
            selectedMediaSource?.audioStreams?.firstOrNull { it.index == idx }
        } ?: selectedMediaSource?.audioStreams?.firstOrNull {
            it.language?.equals(preferredAudioLanguage, true) == true
        } ?: selectedMediaSource?.defaultAudioStream

        val selectedSubtitleStream = savedPrefs?.subtitleIndex?.let { idx ->
            subtitleStreams.firstOrNull { it.index == idx }
        } ?: subtitleStreams.firstOrNull {
            it.language?.equals(preferredSubtitleLanguage, true) == true
        } ?: selectedMediaSource?.defaultSubtitleStream
        val savedSubtitleOffsetMs = savedPrefs?.subtitleOffsetMs ?: 0L

        val initialPlaybackOptions = PlaybackOptions(
            selectedMediaSource = selectedMediaSource,
            selectedVideoStream = selectedVideoStream,
            selectedAudioStream = selectedAudioStream,
            selectedSubtitleStream = selectedSubtitleStream,
            resumePositionTicks = mediaItem.userData?.playbackPositionTicks ?: 0L
        )

        _state.value = _state.value.copy(
            isLoading = false,
            mediaItem = mediaItem,
            playbackOptions = initialPlaybackOptions,
            availableVersions = mediaItem.mediaSources,
            availableVideoQualities = mediaItem.getAvailableVideoQualities(),
            availableAudioStreams = selectedMediaSource?.audioStreams ?: emptyList(),
            availableSubtitleStreams = subtitleStreams,
            preferredAudioLanguage = preferredAudioLanguage,
            preferredSubtitleLanguage = preferredSubtitleLanguage,
            preferredVideoQuality = preferredQuality,
            playbackTranscodeOption = savedTranscodeOption,
            transcodingSessionId = null,
            subtitleOffsetMs = savedSubtitleOffsetMs
        )
    }

    fun showVersionDialog() { _state.value = _state.value.copy(showVersionDialog = true) }
    fun hideVersionDialog() { _state.value = _state.value.copy(showVersionDialog = false) }
    fun showVideoQualityDialog() { _state.value = _state.value.copy(showVideoQualityDialog = true) }
    fun hideVideoQualityDialog() { _state.value = _state.value.copy(showVideoQualityDialog = false) }
    fun showAudioDialog() { _state.value = _state.value.copy(showAudioDialog = true) }
    fun hideAudioDialog() { _state.value = _state.value.copy(showAudioDialog = false) }
    fun showSubtitleDialog() { _state.value = _state.value.copy(showSubtitleDialog = true) }
    fun hideSubtitleDialog() { _state.value = _state.value.copy(showSubtitleDialog = false) }

    private fun persistPlaybackPreferences() {
        val mediaId = _state.value.mediaItem?.id ?: return
        val options = _state.value.playbackOptions
        val prefs = PlaybackPreferences(
            mediaSourceId = options.selectedMediaSource?.id,
            audioIndex = options.selectedAudioStream?.index,
            subtitleIndex = options.selectedSubtitleStream?.index,
            videoQuality = options.selectedVideoStream?.videoQuality?.name,
            transcodeOption = _state.value.playbackTranscodeOption.name,
            subtitleOffsetMs = _state.value.subtitleOffsetMs
        )
        viewModelScope.launch {
            preferencesManager.savePlaybackPreferences(mediaId, prefs)
        }
    }

    fun selectVersion(mediaSource: MediaSource) {
        viewModelScope.launch {
            var updatedSource = mediaSource
            if (mediaSource.mediaStreams.isEmpty()) {
                val mediaId = _state.value.mediaItem?.id
                val userId = savedUserId
                val accessToken = savedAccessToken
                if (mediaId != null && userId != null && accessToken != null) {
                    when (val result = libraryRepository.getMediaItemDetail(userId, mediaId, accessToken)) {
                        is NetworkResult.Success -> {
                            val item = result.data
                            updatedSource = item.mediaSources.firstOrNull { it.id == mediaSource.id } ?: mediaSource
                            _state.value = _state.value.copy(
                                mediaItem = item,
                                availableVersions = item.mediaSources
                            )
                        }
                        is NetworkResult.Error -> {
                            _state.value = _state.value.copy(
                                error = result.message,
                                showVersionDialog = false
                            )
                            return@launch
                        }
                        else -> return@launch
                    }
                }
            }

            currentMediaSourceId = updatedSource.id
            currentAudioStreamIndex = updatedSource.defaultAudioStream?.index
            currentSubtitleStreamIndex = updatedSource.defaultSubtitleStream?.index
            val availableVideoQualities = updatedSource.availableVideoQualities
            val updatedOptions = _state.value.playbackOptions.copy(
                selectedMediaSource = updatedSource,
                selectedVideoStream = selectVideoStream(
                    videoStreams = updatedSource.videoStreams,
                    preferredQuality = _state.value.preferredVideoQuality,
                    hdrPreference = _state.value.hdrFormatPreference,
                    fallback = updatedSource.defaultVideoStream
                ),
                selectedAudioStream = updatedSource.defaultAudioStream,
                selectedSubtitleStream = updatedSource.defaultSubtitleStream
            )

            _state.value = _state.value.copy(
                playbackOptions = updatedOptions,
                showVersionDialog = false,
                mediaSourceId = currentMediaSourceId,
                audioStreamIndex = currentAudioStreamIndex,
                subtitleStreamIndex = currentSubtitleStreamIndex,
                availableVideoQualities = availableVideoQualities,
                availableAudioStreams = updatedSource.audioStreams,
                availableSubtitleStreams = updatedSource.subtitleStreams,
                preferredAudioLanguage = updatedOptions.selectedAudioStream?.language,
                preferredSubtitleLanguage = updatedOptions.selectedSubtitleStream?.language,
                playbackTranscodeOption = PlaybackTranscodeOption.ORIGINAL,
                transcodingSessionId = null
            )
            persistPlaybackPreferences()
        }
    }

    fun selectVideoQuality(quality: VideoQuality) {
        val currentSource = _state.value.playbackOptions.selectedMediaSource ?: return
        val videoStream = currentSource.videoStreams.firstOrNull { it.videoQuality == quality }

        if (videoStream != null) {
            val updatedOptions = _state.value.playbackOptions.copy(
                selectedVideoStream = videoStream
            )

            _state.value = _state.value.copy(
                playbackOptions = updatedOptions,
                showVideoQualityDialog = false,
                preferredVideoQuality = quality
            )
        }
        persistPlaybackPreferences()
    }

    fun selectTranscodeOption(option: PlaybackTranscodeOption, startPositionMs: Long? = null) {
        val mediaItem = _state.value.mediaItem ?: return
        val serverUrl = savedServerUrl ?: return
        val accessToken = savedAccessToken ?: return
        val userId = savedUserId ?: return
        val mediaSourceId = currentMediaSourceId
            ?: _state.value.playbackOptions.selectedMediaSource?.id
            ?: mediaItem.mediaSources.firstOrNull()?.id
            ?: mediaItem.id
        val audioIndex = currentAudioStreamIndex
        val subtitleIndex = currentSubtitleStreamIndex

        
        val selectedSource = _state.value.playbackOptions.selectedMediaSource
            ?: mediaItem.mediaSources.firstOrNull { it.id == mediaSourceId }
        val selectedVideoStream = _state.value.playbackOptions.selectedVideoStream
            ?: selectedSource?.defaultVideoStream
        val videoCodec = selectedVideoStream?.codec
        val videoRange = selectedVideoStream?.videoRange
        val videoRangeType = selectedVideoStream?.videoRangeType
        val profile = selectedVideoStream?.profile
        val bitDepth = selectedVideoStream?.bitDepth

        activeTranscodeJob?.cancel()
        activeTranscodeJob = viewModelScope.launch {
            val directPlayEnabled = preferencesManager.getDirectPlayEnabled().first()
            _state.value = _state.value.copy(isTranscoding = !option.isOriginal, error = null)
            when (
                val result = fetchStreamInfo(
                    mediaItem = mediaItem,
                    serverUrl = serverUrl,
                    accessToken = accessToken,
                    userId = userId,
                    mediaSourceId = mediaSourceId,
                    directPlayEnabled = directPlayEnabled,
                    videoCodec = videoCodec,
                    videoRange = videoRange,
                    videoRangeType = videoRangeType,
                    profile = profile,
                    bitDepth = bitDepth,
                    audioStreamIndex = audioIndex,
                    subtitleStreamIndex = subtitleIndex,
                    option = option,
                    startPositionMs = startPositionMs
                )
            ) {
                is NetworkResult.Success -> {
                    val streamInfo = result.data
                    val resolvedUrl = proxiedUrl(streamInfo.url, accessToken)
                    val shouldUpdateStart = startPositionMs != null
                    val nextUpdateCount = if (shouldUpdateStart) {
                        _state.value.startPositionUpdateCount + 1
                    } else {
                        _state.value.startPositionUpdateCount
                    }
                    _state.value = _state.value.copy(
                        videoUrl = resolvedUrl,
                        playbackTranscodeOption = option,
                        transcodingSessionId = streamInfo.playSessionId,
                        isTranscoding = false,
                        showVideoQualityDialog = false,
                        mediaSourceId = mediaSourceId,
                        audioStreamIndex = audioIndex,
                        subtitleStreamIndex = subtitleIndex,
                        exoMediaItem = null,
                        cacheDataSourceFactory = null,
                        startPositionMs = startPositionMs ?: _state.value.startPositionMs,
                        startPositionUpdateCount = nextUpdateCount
                    )
                    persistPlaybackPreferences()
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message,
                        isTranscoding = false,
                        showVideoQualityDialog = false
                    )
                }
                else -> Unit
            }
        }
    }

    fun selectAudioStream(audioStream: MediaStream) {
        val updatedOptions = _state.value.playbackOptions.copy(
            selectedAudioStream = audioStream
        )

        _state.value = _state.value.copy(
            playbackOptions = updatedOptions,
            showAudioDialog = false,
            preferredAudioLanguage = audioStream.language
        )

        viewModelScope.launch {
            preferencesManager.saveAudioTrackLanguage(audioStream.language)
        }
        persistPlaybackPreferences()
    }

    fun selectSubtitleStream(subtitleStream: MediaStream?) {
        val updatedOptions = _state.value.playbackOptions.copy(
            selectedSubtitleStream = subtitleStream
        )

        _state.value = _state.value.copy(
            playbackOptions = updatedOptions,
            showSubtitleDialog = false,
            preferredSubtitleLanguage = subtitleStream?.language
        )

        viewModelScope.launch {
            preferencesManager.saveSubtitleLanguage(subtitleStream?.language)
        }
        persistPlaybackPreferences()
    }

    fun selectResumePlayback(resumePositionTicks: Long) {
        val updatedOptions = _state.value.playbackOptions.copy(
            startFromBeginning = false,
            resumePositionTicks = resumePositionTicks
        )
        _state.value = _state.value.copy(playbackOptions = updatedOptions)
    }

    fun selectStartFromBeginning() {
        val updatedOptions = _state.value.playbackOptions.copy(
            startFromBeginning = true,
            resumePositionTicks = 0L
        )
        _state.value = _state.value.copy(playbackOptions = updatedOptions)
    }

    
    
    

    fun initializePlayer(
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        userId: String,
        mediaSourceId: String? = null,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        startPositionMs: Long = 0
    ) {
        savedServerUrl = serverUrl
        savedUserId = userId
        savedAccessToken = accessToken
        _state.value = _state.value.copy(mediaItem = mediaItem)
        viewModelScope.launch {
            try {
                
                val directPlayEnabled = preferencesManager.getDirectPlayEnabled().first()
                val isOffline = !isConnected.value
                var resolvedStartMs = startPositionMs
                if (resolvedStartMs == 0L) {
                    val localTicks = preferencesManager.getPlaybackPosition(mediaItem.id).first()
                    resolvedStartMs = localTicks?.div(10_000) ?: 0L
                    if (resolvedStartMs == 0L && !isOffline) {
                        resolvedStartMs = when (val result = libraryRepository.getPlaybackPosition(userId, mediaItem.id, accessToken)) {
                            is NetworkResult.Success -> result.data / 10_000
                            else -> 0L
                        }
                    }
                    if (resolvedStartMs == 0L) {
                        _state.value = _state.value.copy(error = "Unable to restore playback progress")
                    }
                }
                if (resolvedStartMs == 0L) {
                    _state.value = _state.value.copy(error = "Unable to restore playback progress")
                }
                val savedPrefs = if (
                    mediaSourceId == null || audioStreamIndex == null || subtitleStreamIndex == null
                ) {
                    preferencesManager.getPlaybackPreferences(mediaItem.id).first()
                } else null

                val subtitleOffsetMs = savedPrefs?.subtitleOffsetMs ?: _state.value.subtitleOffsetMs
                _state.value = _state.value.copy(subtitleOffsetMs = subtitleOffsetMs)

                currentMediaSourceId = mediaSourceId
                    ?: currentMediaSourceId
                            ?: savedPrefs?.mediaSourceId
                            ?: mediaItem.mediaSources.firstOrNull()?.id
                            ?: mediaItem.id
                currentAudioStreamIndex = audioStreamIndex
                    ?: currentAudioStreamIndex
                            ?: savedPrefs?.audioIndex
                            ?: mediaItem.mediaSources.firstOrNull { it.id == currentMediaSourceId }?.defaultAudioStream?.index
                currentSubtitleStreamIndex = subtitleStreamIndex
                    ?: currentSubtitleStreamIndex
                            ?: savedPrefs?.subtitleIndex
                            ?: mediaItem.mediaSources.firstOrNull { it.id == currentMediaSourceId }?.defaultSubtitleStream?.index

                val selectedSource = mediaItem.mediaSources.firstOrNull { it.id == currentMediaSourceId }
                var availableAudioStreams = selectedSource?.audioStreams ?: emptyList()
                var availableSubtitleStreams = selectedSource?.subtitleStreams ?: emptyList()

                val selectedAudioStream = currentAudioStreamIndex?.let { idx ->
                    availableAudioStreams.firstOrNull { it.index == idx }
                }
                val selectedSubtitleStream = currentSubtitleStreamIndex?.let { idx ->
                    availableSubtitleStreams.firstOrNull { it.index == idx }
                }

                
                val preferredQuality = savedPrefs?.videoQuality?.let { qualityStr ->
                    runCatching { VideoQuality.valueOf(qualityStr) }.getOrNull()
                }
                val selectedVideoStream = selectVideoStream(
                    videoStreams = selectedSource?.videoStreams.orEmpty(),
                    preferredQuality = preferredQuality,
                    hdrPreference = _state.value.hdrFormatPreference,
                    fallback = selectedSource?.defaultVideoStream
                )


                if (!isOffline) {
                    when (val segmentResult = libraryRepository.getItemSegments(
                        userId = userId,
                        mediaId = mediaItem.id,
                        accessToken = accessToken
                    )) {
                        is NetworkResult.Success -> {
                            _state.value = _state.value.copy(segments = segmentResult.data)
                            updatePlaybackPosition(resolvedStartMs)
                        }
                        else -> {
                            val errorMessage = if (segmentResult is NetworkResult.Error) {
                                segmentResult.message
                            } else {
                                "Unknown error"
                            }
                            Log.e(
                                "VideoPlaybackViewModel",
                                "Failed to load item segments: $errorMessage",
                                (segmentResult as? NetworkResult.Error)?.exception
                            )
                            _state.value = _state.value.copy(
                                segments = emptyList(),
                                activeSegment = null,
                                error = errorMessage
                            )
                        }
                    }
                }



                val savedTranscodeOption = savedPrefs?.transcodeOption?.let { option ->
                    runCatching { PlaybackTranscodeOption.valueOf(option) }.getOrNull()
                }

                val transcodeOption = savedTranscodeOption ?: _state.value.playbackTranscodeOption

                val mediaSourceIdentifier = currentMediaSourceId
                    ?: mediaItem.mediaSources.firstOrNull()?.id
                    ?: mediaItem.id

                
                val streamForParams = selectedVideoStream ?: selectedSource?.defaultVideoStream
                val videoCodec = streamForParams?.codec
                val videoRange = streamForParams?.videoRange
                val videoRangeType = streamForParams?.videoRangeType
                val profile = streamForParams?.profile
                val bitDepth = streamForParams?.bitDepth

                val streamResult = fetchStreamInfo(
                    mediaItem = mediaItem,
                    serverUrl = serverUrl,
                    accessToken = accessToken,
                    userId = userId,
                    mediaSourceId = mediaSourceIdentifier,
                    directPlayEnabled = directPlayEnabled,
                    videoCodec = videoCodec,
                    videoRange = videoRange,
                    videoRangeType = videoRangeType,
                    profile = profile,
                    bitDepth = bitDepth,
                    audioStreamIndex = currentAudioStreamIndex,
                    subtitleStreamIndex = currentSubtitleStreamIndex,
                    option = transcodeOption,
                    startPositionMs = resolvedStartMs,
                    startPositionTicks = resolvedStartMs.takeIf { it > 0 }?.times(10_000)
                )

                if (streamResult is NetworkResult.Error) {
                    _state.value = _state.value.copy(
                        error = streamResult.message,
                        playbackTranscodeOption = transcodeOption,
                        transcodingSessionId = null,
                        isInitialized = false
                    )
                    return@launch
                }

                val streamInfo = (streamResult as? NetworkResult.Success)?.data
                    ?: return@launch
                val resolvedUrl = proxiedUrl(streamInfo.url, accessToken)
                val resolvedStreams = resolveStreamsFromPlaybackInfo(
                    playbackStreams = streamInfo.mediaStreams,
                    fallbackAudioStreams = availableAudioStreams,
                    fallbackSubtitleStreams = availableSubtitleStreams,
                    fallbackVideoStream = selectedVideoStream,
                    audioIndex = currentAudioStreamIndex,
                    subtitleIndex = currentSubtitleStreamIndex
                )

                val nextUpdateCount = _state.value.startPositionUpdateCount + 1

                _state.value = _state.value.copy(
                    videoUrl = resolvedUrl,
                    playbackTranscodeOption = transcodeOption,
                    transcodingSessionId = streamInfo.playSessionId,
                    exoMediaItem = null,
                    cacheDataSourceFactory = null,
                    mediaSourceId = mediaSourceIdentifier,
                    audioStreamIndex = currentAudioStreamIndex,
                    subtitleStreamIndex = currentSubtitleStreamIndex,
                    startPositionMs = resolvedStartMs,
                    startPositionUpdateCount = nextUpdateCount,
                    isInitialized = true,
                    availableAudioStreams = resolvedStreams.audioStreams,
                    availableSubtitleStreams = resolvedStreams.subtitleStreams,
                    preferredVideoQuality = preferredQuality,
                    playbackOptions = _state.value.playbackOptions.copy(
                        selectedMediaSource = selectedSource,
                        selectedVideoStream = resolvedStreams.selectedVideo,
                        selectedAudioStream = resolvedStreams.selectedAudio ?: selectedAudioStream,
                        selectedSubtitleStream = resolvedStreams.selectedSubtitle ?: selectedSubtitleStream
                    ),
                    isTranscoding = !transcodeOption.isOriginal
                )
                if (mediaItem.type.equals(ApiConstants.ITEM_TYPE_EPISODE, true)) {
                    updateEpisodeNavigation(mediaItem, userId, accessToken)
                } else {
                    _state.value = _state.value.copy(
                        siblingEpisodes = emptyList(),
                        currentEpisodeIndex = -1
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to initialize player: ${e.message}"
                )
            }
        }
    }

    fun playNextEpisode() {
        val nextIndex = _state.value.currentEpisodeIndex + 1
        val episodes = _state.value.siblingEpisodes
        if (nextIndex in episodes.indices) {
            viewModelScope.launch {
                playEpisodeAtIndex(episodes[nextIndex], nextIndex)
            }
        }
    }

    fun playPreviousEpisode() {
        val previousIndex = _state.value.currentEpisodeIndex - 1
        val episodes = _state.value.siblingEpisodes
        if (previousIndex in episodes.indices) {
            viewModelScope.launch {
                playEpisodeAtIndex(episodes[previousIndex], previousIndex)
            }
        }
    }

    fun playSpecificItem(mediaItem: MediaItem) {
        viewModelScope.launch {
            val episodes = _state.value.siblingEpisodes
            val targetIndex = episodes.indexOfFirst { it.id == mediaItem.id }
            if (targetIndex != -1) {
                playEpisodeAtIndex(episodes[targetIndex], targetIndex)
            } else {
                
                playEpisodeAtIndex(mediaItem, -1)
            }
        }
    }

    private suspend fun playEpisodeAtIndex(targetEpisode: MediaItem, targetIndex: Int) {
        val userId = savedUserId ?: return
        val accessToken = savedAccessToken ?: return
        val serverUrl = savedServerUrl ?: return
        val currentSourcePath = _state.value.playbackOptions.selectedMediaSource?.path
        val previousAudioSelection = _state.value.playbackOptions.selectedAudioStream
        val previousSubtitleSelection = _state.value.playbackOptions.selectedSubtitleStream
        val preferredAudioLanguage = previousAudioSelection?.language ?: _state.value.preferredAudioLanguage
        val preferredSubtitleLanguage = previousSubtitleSelection?.language ?: _state.value.preferredSubtitleLanguage
        val subtitleWasOff = previousSubtitleSelection == null

        val detailedEpisode = when (
            val result = getMediaDetailUseCase(
                GetMediaDetailUseCase.Params(targetEpisode.id, userId, accessToken)
            )
        ) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> targetEpisode
            is NetworkResult.Loading -> targetEpisode
        }

        setupVideoOptions(detailedEpisode)

        val preferredSource = selectPreferredMediaSource(currentSourcePath, detailedEpisode)
            ?: detailedEpisode.getPrimaryMediaSource()
        val preferredAudio = selectAudioForNextEpisode(
            previousAudio = previousAudioSelection,
            preferredLanguage = preferredAudioLanguage,
            newStreams = preferredSource?.audioStreams ?: emptyList(),
            defaultStream = preferredSource?.defaultAudioStream
        )
        val preferredSubtitle = selectSubtitleForNextEpisode(
            previousSubtitle = previousSubtitleSelection,
            preferredLanguage = preferredSubtitleLanguage,
            newStreams = preferredSource?.subtitleStreams ?: emptyList(),
            defaultStream = preferredSource?.defaultSubtitleStream,
            keepOff = subtitleWasOff
        )

        val updatedOptions = _state.value.playbackOptions.copy(
            selectedMediaSource = preferredSource ?: _state.value.playbackOptions.selectedMediaSource,
            selectedAudioStream = preferredAudio
                ?: preferredSource?.defaultAudioStream
                ?: _state.value.playbackOptions.selectedAudioStream,
            selectedSubtitleStream = preferredSubtitle,
            startFromBeginning = true,
            resumePositionTicks = 0L
        )

        val updatedEpisodes = _state.value.siblingEpisodes.mapIndexed { index, item ->
            if (index == targetIndex) detailedEpisode else item
        }

        _state.value = _state.value.copy(
            mediaItem = detailedEpisode,
            playbackOptions = updatedOptions,
            availableAudioStreams = preferredSource?.audioStreams
                ?: updatedOptions.selectedMediaSource?.audioStreams.orEmpty(),
            availableSubtitleStreams = preferredSource?.subtitleStreams
                ?: updatedOptions.selectedMediaSource?.subtitleStreams.orEmpty(),
            preferredAudioLanguage = updatedOptions.selectedAudioStream?.language
                ?: _state.value.preferredAudioLanguage,
            preferredSubtitleLanguage = updatedOptions.selectedSubtitleStream?.language
                ?: _state.value.preferredSubtitleLanguage,
            currentEpisodeIndex = targetIndex,
            siblingEpisodes = updatedEpisodes,
            isInitialized = false,
            error = null,
            showEndScreen = false,
            hasEnded = false
        )

        persistPlaybackPreferences()

        initializePlayer(
            mediaItem = detailedEpisode,
            serverUrl = serverUrl,
            accessToken = accessToken,
            userId = userId,
            mediaSourceId = preferredSource?.id,
            audioStreamIndex = preferredAudio?.index
                ?: preferredSource?.defaultAudioStream?.index,
            subtitleStreamIndex = preferredSubtitle?.index,
            startPositionMs = 0
        )
    }

    private fun selectPreferredMediaSource(
        currentSourcePath: String?,
        episode: MediaItem
    ): MediaSource? {
        val currentParent = currentSourcePath?.let { extractParentPath(it) } ?: return null
        return episode.mediaSources.firstOrNull { source ->
            val candidateParent = source.path?.let { extractParentPath(it) }
            candidateParent != null && candidateParent.equals(currentParent, ignoreCase = true)
        }
    }

    private fun extractParentPath(path: String): String? {
        val normalized = path.trim().replace('\\', '/').trimEnd('/')
        val lastSeparator = normalized.lastIndexOf('/')
        return if (lastSeparator > 0) normalized.substring(0, lastSeparator) else null
    }

    private fun normalizeTitle(title: String?): String? {
        return title?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    }

    private fun selectAudioForNextEpisode(
        previousAudio: MediaStream?,
        preferredLanguage: String?,
        newStreams: List<MediaStream>,
        defaultStream: MediaStream?
    ): MediaStream? {
        if (newStreams.isEmpty()) return null
        val targetLanguage = (previousAudio?.language ?: preferredLanguage)?.trim()?.lowercase()
        val previousTitle = normalizeTitle(previousAudio?.displayTitle ?: previousAudio?.title)

        val languageMatches = newStreams.filter { stream ->
            val streamLang = stream.language?.trim()?.lowercase()
            targetLanguage != null && streamLang == targetLanguage
        }

        val exactTitleMatch = languageMatches.firstOrNull { stream ->
            val streamTitle = normalizeTitle(stream.displayTitle ?: stream.title)
            previousTitle != null && streamTitle == previousTitle
        }
        if (exactTitleMatch != null) return exactTitleMatch

        if (languageMatches.isNotEmpty()) {
            return languageMatches.sortedWith(
                compareByDescending<MediaStream> { it.isDefault }
                    .thenBy { it.index }
            ).first()
        }

        return defaultStream ?: newStreams.firstOrNull()
    }

    private fun selectSubtitleForNextEpisode(
        previousSubtitle: MediaStream?,
        preferredLanguage: String?,
        newStreams: List<MediaStream>,
        defaultStream: MediaStream?,
        keepOff: Boolean
    ): MediaStream? {
        if (keepOff) return null
        if (newStreams.isEmpty()) return null

        val targetLanguage = (previousSubtitle?.language ?: preferredLanguage)?.trim()?.lowercase()
        val previousTitle = normalizeTitle(previousSubtitle?.displayTitle ?: previousSubtitle?.title)
        val wasForced = previousSubtitle?.isForced ?: false
        val enforceNonForced = previousSubtitle != null && !wasForced

        val languageMatches = newStreams.filter { stream ->
            val streamLang = stream.language?.trim()?.lowercase()
            targetLanguage != null && streamLang == targetLanguage
        }

        val prioritizedMatches = when {
            enforceNonForced -> languageMatches.filterNot { it.isForced }.ifEmpty { languageMatches }
            else -> languageMatches
        }

        if (prioritizedMatches.isNotEmpty()) {
            val sorted = prioritizedMatches.sortedWith(
                compareByDescending<MediaStream> { stream ->
                    val streamTitle = normalizeTitle(stream.displayTitle ?: stream.title)
                    previousTitle != null && streamTitle == previousTitle
                }.thenByDescending { stream -> stream.isForced == wasForced }
                    .thenByDescending { it.isDefault }
                    .thenBy { it.index }
            )
            return sorted.first()
        }

        return defaultStream ?: newStreams.firstOrNull()
    }

    private suspend fun updateEpisodeNavigation(
        mediaItem: MediaItem,
        userId: String,
        accessToken: String
    ) {
        if (!mediaItem.type.equals(ApiConstants.ITEM_TYPE_EPISODE, true)) {
            _state.value = _state.value.copy(siblingEpisodes = emptyList(), currentEpisodeIndex = -1)
            return
        }

        val seriesId = mediaItem.seriesId ?: run {
            _state.value = _state.value.copy(siblingEpisodes = emptyList(), currentEpisodeIndex = -1)
            return
        }

        val seasonsResult = libraryRepository.getSeasons(userId, seriesId, accessToken)
        val seasonId = when (seasonsResult) {
            is NetworkResult.Success -> {
                val seasons = seasonsResult.data
                val preferredSeason = mediaItem.parentIndexNumber?.let { index ->
                    seasons.firstOrNull { it.indexNumber == index }
                } ?: seasons.firstOrNull()
                preferredSeason?.id
            }
            else -> null
        }

        if (seasonId == null) {
            _state.value = _state.value.copy(siblingEpisodes = emptyList(), currentEpisodeIndex = -1)
            return
        }

        when (val episodesResult = libraryRepository.getEpisodes(userId, seasonId, accessToken)) {
            is NetworkResult.Success -> {
                val episodes = episodesResult.data
                val resolvedEpisodes = episodes.map { episode ->
                    if (episode.id == mediaItem.id) mediaItem else episode
                }
                val index = resolvedEpisodes.indexOfFirst { it.id == mediaItem.id }
                _state.value = _state.value.copy(
                    siblingEpisodes = resolvedEpisodes,
                    currentEpisodeIndex = index
                )
            }
            else -> {
                val existingIndex = _state.value.siblingEpisodes.indexOfFirst { it.id == mediaItem.id }
                if (existingIndex != -1) {
                    _state.value = _state.value.copy(currentEpisodeIndex = existingIndex)
                } else {
                    _state.value = _state.value.copy(
                        siblingEpisodes = emptyList(),
                        currentEpisodeIndex = -1
                    )
                }
            }
        }
    }

    private fun buildVideoUrl(
        serverUrl: String,
        accessToken: String,
        mediaItem: MediaItem,
        mediaSourceId: String,
        container: String = "mkv",
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null
    ): String {
        return buildString {
            append(serverUrl.removeSuffix("/"))
            append("/Videos/")
            append(mediaItem.id)
            append("/stream")
            append("?static=true&container=")
            append(container)
            append("&MediaSourceId=")
            append(mediaSourceId)
            audioStreamIndex?.let { append("&AudioStreamIndex=$it") }
            subtitleStreamIndex?.let { append("&SubtitleStreamIndex=$it") }
            append("&api_key=")
            append(accessToken)
        }
    }

    fun getExternalPlaybackUrl(
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        mediaSourceId: String? = null,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null
    ): String {
        val sourceId = mediaSourceId ?: mediaItem.mediaSources.firstOrNull()?.id ?: mediaItem.id
        val container = mediaItem.getStreamContainer(sourceId) ?: "mkv"
        val url = buildVideoUrl(
            serverUrl = serverUrl,
            accessToken = accessToken,
            mediaItem = mediaItem,
            mediaSourceId = sourceId,
            container = container,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex
        )
        return proxiedUrl(url, accessToken) ?: url
    }

    fun updateAudioStream(
        audioStreamIndex: Int,
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        shouldRebuildUrl: Boolean = true,
        startPositionMs: Long? = null
    ) {
        currentAudioStreamIndex = audioStreamIndex
        val selectedStream = _state.value.availableAudioStreams
            .firstOrNull { it.index == audioStreamIndex }

        _state.value = _state.value.copy(
            playbackOptions = _state.value.playbackOptions.copy(
                selectedAudioStream = selectedStream
            ),
            audioStreamIndex = currentAudioStreamIndex,
            preferredAudioLanguage = selectedStream?.language
        )

        viewModelScope.launch {
            preferencesManager.saveAudioTrackLanguage(selectedStream?.language)
            persistPlaybackPreferences()
        }

        if (shouldRebuildUrl) {
            rebuildStream(mediaItem, serverUrl, accessToken, startPositionMs)
        }
        viewModelScope.launch {
            _trackChangeEvents.emit(
                TrackChangeEvent(currentAudioStreamIndex, currentSubtitleStreamIndex)
            )
        }
    }

    fun updateSubtitleStream(
        subtitleStreamIndex: Int?,
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        shouldRebuildUrl: Boolean = true,
        startPositionMs: Long? = null
    ) {
        currentSubtitleStreamIndex = subtitleStreamIndex
        val selectedSubtitleStream = subtitleStreamIndex?.let { index ->
            _state.value.availableSubtitleStreams.firstOrNull { it.index == index }
        }

        val updatedOptions = _state.value.playbackOptions.copy(
            selectedSubtitleStream = selectedSubtitleStream
        )

        _state.value = _state.value.copy(
            playbackOptions = updatedOptions,
            subtitleStreamIndex = currentSubtitleStreamIndex,
            preferredSubtitleLanguage = selectedSubtitleStream?.language
        )

        viewModelScope.launch {
            preferencesManager.saveSubtitleLanguage(selectedSubtitleStream?.language)
            persistPlaybackPreferences()
        }

        if (shouldRebuildUrl) {
            rebuildStream(mediaItem, serverUrl, accessToken, startPositionMs)
        }
        viewModelScope.launch {
            _trackChangeEvents.emit(
                TrackChangeEvent(currentAudioStreamIndex, currentSubtitleStreamIndex)
            )
        }
    }

    private suspend fun fetchStreamInfo(
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        userId: String,
        mediaSourceId: String,
        directPlayEnabled: Boolean,
        videoCodec: String?,
        videoRange: String?,
        videoRangeType: String?,
        profile: String?,
        bitDepth: Int?,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        option: PlaybackTranscodeOption,
        startPositionMs: Long? = null,
        startPositionTicks: Long? = null,
    ): NetworkResult<PlaybackStreamInfo> {
        val effectiveStartTicks = startPositionTicks
            ?: startPositionMs?.takeIf { it > 0 }?.times(10_000)
            ?: _state.value.startPositionMs.takeIf { it > 0 }?.times(10_000)
            ?: _state.value.playbackOptions.resumePositionTicks.takeIf { it > 0 }

        
        return libraryRepository.requestPlaybackInfo(
            itemId = mediaItem.id,
            userId = userId,
            accessToken = accessToken,
            mediaSourceId = mediaSourceId,
            directPlayEnabled = directPlayEnabled,
            videoCodec = videoCodec,
            videoRange = videoRange,
            videoRangeType = videoRangeType,
            profile = profile,
            bitDepth = bitDepth,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
            startTimeTicks = effectiveStartTicks,
            transcodeOption = option
        )
    }

    private fun rebuildStream(
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        startPositionMs: Long? = null
    ) {
        val userId = savedUserId ?: return
        val option = _state.value.playbackTranscodeOption
        val mediaSourceId = currentMediaSourceId
            ?: _state.value.playbackOptions.selectedMediaSource?.id
            ?: mediaItem.mediaSources.firstOrNull()?.id
            ?: mediaItem.id
        val audioIndex = currentAudioStreamIndex
        val subtitleIndex = currentSubtitleStreamIndex

        
        val selectedSource = _state.value.playbackOptions.selectedMediaSource
            ?: mediaItem.mediaSources.firstOrNull { it.id == mediaSourceId }
        val selectedVideoStream = _state.value.playbackOptions.selectedVideoStream
            ?: selectedSource?.defaultVideoStream
        val videoCodec = selectedVideoStream?.codec
        val videoRange = selectedVideoStream?.videoRange
        val videoRangeType = selectedVideoStream?.videoRangeType
        val profile = selectedVideoStream?.profile
        val bitDepth = selectedVideoStream?.bitDepth

        activeTranscodeJob?.cancel()
        activeTranscodeJob = viewModelScope.launch {
            val directPlayEnabled = preferencesManager.getDirectPlayEnabled().first()
            _state.value = _state.value.copy(isTranscoding = !option.isOriginal)
            when (
                val result = fetchStreamInfo(
                    mediaItem = mediaItem,
                    serverUrl = serverUrl,
                    accessToken = accessToken,
                    userId = userId,
                    mediaSourceId = mediaSourceId,
                    directPlayEnabled = directPlayEnabled,
                    videoCodec = videoCodec,
                    videoRange = videoRange,
                    videoRangeType = videoRangeType,
                    profile = profile,
                    bitDepth = bitDepth,
                    audioStreamIndex = audioIndex,
                    subtitleStreamIndex = subtitleIndex,
                    option = option,
                    startPositionMs = startPositionMs
                )
                ) {
                is NetworkResult.Success -> {
                    val streamInfo = result.data
                    val resolvedUrl = proxiedUrl(streamInfo.url, accessToken)
                    val resolvedStreams = resolveStreamsFromPlaybackInfo(
                        playbackStreams = streamInfo.mediaStreams,
                        fallbackAudioStreams = _state.value.availableAudioStreams,
                        fallbackSubtitleStreams = _state.value.availableSubtitleStreams,
                        fallbackVideoStream = _state.value.playbackOptions.selectedVideoStream,
                        audioIndex = audioIndex,
                        subtitleIndex = subtitleIndex
                    )
                    val shouldUpdateStart = startPositionMs != null
                    val nextUpdateCount = if (shouldUpdateStart) {
                        _state.value.startPositionUpdateCount + 1
                    } else {
                        _state.value.startPositionUpdateCount
                    }
                    _state.value = _state.value.copy(
                        videoUrl = resolvedUrl,
                        transcodingSessionId = streamInfo.playSessionId,
                        mediaSourceId = mediaSourceId,
                        audioStreamIndex = audioIndex,
                        subtitleStreamIndex = subtitleIndex,
                        isTranscoding = false,
                        exoMediaItem = null,
                        cacheDataSourceFactory = null,
                        startPositionMs = startPositionMs ?: _state.value.startPositionMs,
                        startPositionUpdateCount = nextUpdateCount,
                        availableAudioStreams = resolvedStreams.audioStreams,
                        availableSubtitleStreams = resolvedStreams.subtitleStreams,
                        playbackOptions = _state.value.playbackOptions.copy(
                            selectedVideoStream = resolvedStreams.selectedVideo
                                ?: _state.value.playbackOptions.selectedVideoStream,
                            selectedAudioStream = resolvedStreams.selectedAudio
                                ?: _state.value.playbackOptions.selectedAudioStream,
                            selectedSubtitleStream = resolvedStreams.selectedSubtitle
                                ?: _state.value.playbackOptions.selectedSubtitleStream
                        )
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message,
                        isTranscoding = false
                    )
                }
                else -> Unit
            }
        }
    }

    fun updatePlaybackPosition(positionMs: Long) {
        val positionTicks = positionMs * 10_000
        val newSegment = _state.value.segments.firstOrNull {
            positionTicks >= it.startPositionTicks && positionTicks < it.endPositionTicks
        }
        if (newSegment?.id != _state.value.activeSegment?.id) {
            _state.value = _state.value.copy(activeSegment = newSegment)
        }
    }

    fun skipSegment() {
        val segment = _state.value.activeSegment ?: return
        val endMs = segment.endPositionTicks / 10_000
        val nextUpdateCount = _state.value.startPositionUpdateCount + 1
        _state.value = _state.value.copy(
            startPositionMs = endMs,
            startPositionUpdateCount = nextUpdateCount,
            activeSegment = null
        )
    }

    fun savePlaybackPosition(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionSeconds: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val positionTicks = positionSeconds * 10_000_000
                mediaItemDao.updatePlaybackPosition(mediaId, userId, positionTicks)
                preferencesManager.savePlaybackPosition(mediaId, positionTicks)
                val isOnline = isConnected.value
                val result = if (isOnline) {
                    libraryRepository.reportPlaybackProgress(
                        mediaId = mediaId,
                        userId = userId,
                        accessToken = accessToken,
                        positionTicks = positionTicks,
                        playSessionId = _state.value.transcodingSessionId
                    )
                } else {
                    NetworkResult.Error("No network connection")
                }

                _state.value = _state.value.copy(lastSavedPosition = positionSeconds)
            } catch (e: Exception) {
                println("Error saving playback position: ${e.message}")
            }
        }
    }

    fun markAsWatched(
        mediaId: String,
        userId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val accessToken = savedAccessToken ?: preferencesManager.getAccessToken().first()

                if (accessToken != null) {
                    when (val result = libraryRepository.markAsPlayed(userId, mediaId, accessToken, true)) {
                        is NetworkResult.Success -> Unit
                        is NetworkResult.Error -> libraryRepository.setPlayedLocal(userId, mediaId, true)
                        is NetworkResult.Loading -> Unit
                    }
                } else {
                    libraryRepository.setPlayedLocal(userId, mediaId, true)
                }

                preferencesManager.clearPlaybackPosition(mediaId)
                _state.value = _state.value.copy(isMarkedAsWatched = true)
            } catch (e: Exception) {
                println("Error marking as watched: ${e.message}")
            }
        }
    }

    fun reportPlaybackStart(mediaId: String, userId: String, accessToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                libraryRepository.reportPlaybackStart(
                    mediaId = mediaId,
                    userId = userId,
                    accessToken = accessToken,
                    playSessionId = _state.value.transcodingSessionId
                )
                _state.value = _state.value.copy(playbackStartReported = true)
            } catch (e: Exception) {
                println("Error reporting playback start: ${e.message}")
            }
        }
    }

    fun reportPlaybackStop(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionSeconds: Long,
        isCompleted: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val positionTicks = positionSeconds * 10_000_000
                val finalTicks = if (isCompleted) 0L else positionTicks

                if (isCompleted) {
                    mediaItemDao.updatePlaybackPosition(mediaId, userId, 0)
                    preferencesManager.clearPlaybackPosition(mediaId)
                } else {
                    mediaItemDao.updatePlaybackPosition(mediaId, userId, finalTicks)
                    preferencesManager.savePlaybackPosition(mediaId, finalTicks)
                }

                libraryRepository.reportPlaybackStop(
                    mediaId = mediaId,
                    userId = userId,
                    accessToken = accessToken,
                    positionTicks = finalTicks,
                    playSessionId = _state.value.transcodingSessionId
                )

                _state.value = _state.value.copy(playbackStopReported = true)
            } catch (e: Exception) {
                println("Error reporting playback stop: ${e.message}")
            }
        }
    }

    fun updateBufferingState(isBuffering: Boolean) {
        _state.value = _state.value.copy(isBuffering = isBuffering)
    }

    fun updatePlaybackSpeed(speed: Float) {
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    fun updateVolume(volume: Long) {
        _state.value = _state.value.copy(volume = volume.coerceIn(0, 100))
    }

    fun updateSubtitleOffset(offsetMs: Long) {
        val clamped = offsetMs.coerceIn(MIN_SUBTITLE_OFFSET_MS, MAX_SUBTITLE_OFFSET_MS)
        if (_state.value.subtitleOffsetMs == clamped) return
        _state.value = _state.value.copy(subtitleOffsetMs = clamped)
        persistPlaybackPreferences()
    }

    fun updatePausedState(isPaused: Boolean) {
        _state.value = _state.value.copy(isPaused = isPaused)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun reset() {
        _state.value = VideoPlaybackState()
        currentMediaSourceId = null
        currentAudioStreamIndex = null
        currentSubtitleStreamIndex = null
    }

    fun handlePlaybackEnded(userId: String, accessToken: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                hasEnded = true,
                showEndScreen = true
            )

            
            fetchOnDeckItems(userId, accessToken)
        }
    }

    private suspend fun fetchOnDeckItems(userId: String, accessToken: String) {
        try {
            when (val result = libraryRepository.getResumeItems(userId, accessToken, limit = 6)) {
                is NetworkResult.Success -> {
                    
                    val currentItemId = _state.value.mediaItem?.id
                    val filteredItems = result.data.filter { it.id != currentItemId }
                    _state.value = _state.value.copy(onDeckItems = filteredItems)
                }
                else -> {
                    
                    _state.value = _state.value.copy(onDeckItems = emptyList())
                }
            }
        } catch (e: Exception) {
            println("Error fetching on deck items: ${e.message}")
            _state.value = _state.value.copy(onDeckItems = emptyList())
        }
    }

    fun dismissEndScreen() {
        _state.value = _state.value.copy(
            showEndScreen = false,
            hasEnded = false
        )
    }

    fun replayCurrentVideo() {
        _state.value = _state.value.copy(
            hasEnded = false,
            showEndScreen = false,
            startPositionMs = 0,
            startPositionUpdateCount = _state.value.startPositionUpdateCount + 1
        )
    }
}
