package com.hritwik.avoid.presentation.ui.screen.media

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil.Coil
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.playback.PlaybackInfo
import com.hritwik.avoid.presentation.ui.components.common.HeroBackground
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.media.EpisodeActionsRow
import com.hritwik.avoid.presentation.ui.components.media.EpisodeThumbnailCard
import com.hritwik.avoid.presentation.ui.components.media.MediaActionButtons
import com.hritwik.avoid.presentation.ui.components.media.MediaCardType
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.presentation.ui.components.media.PersonCard
import com.hritwik.avoid.presentation.ui.components.media.SeasonSelectionBar
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.extensions.getBackdropUrl
import com.hritwik.avoid.utils.extensions.getImageUrl
import com.hritwik.avoid.utils.extensions.resolveSubtitleOffIndex
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@OptIn(UnstableApi::class)
@Composable
fun MediaDetailContent(
    modifier: Modifier = Modifier,
    mediaType: MediaType,
    mediaItem: MediaItem,
    serverUrl: String,
    episodes: List<MediaItem> = emptyList(),
    seasons: List<MediaItem> = emptyList(),
    specialFeatures: List<MediaItem> = emptyList(),
    initialSeasonId: String? = null,
    initialEpisodeIndex: Int = 0,
    shouldAutoFocusEpisode: Boolean = false,
    focusedEpisode: MediaItem? = null,
    onPlayClick: (PlaybackInfo) -> Unit = {},
    onEpisodeClick: (PlaybackInfo) -> Unit = {},
    onEpisodeFocused: (MediaItem) -> Unit = {},
    onSpecialFeatureFocused: (MediaItem) -> Unit = {},
    onEpisodeUnfocused: (MediaItem) -> Unit = {},
    onSeasonSelected: (String) -> Unit = {},
    sideNavigationFocusRequester: FocusRequester? = null,
    videoPlaybackViewModel: VideoPlaybackViewModel = hiltViewModel()
) {
    val playbackState by videoPlaybackViewModel.state.collectAsStateWithLifecycle()
    var seasonAccentColorOverride by remember(mediaItem.id) {
        mutableStateOf<Color?>(null)
    }
    val seasonAccentColor = seasonAccentColorOverride ?: MaterialTheme.colorScheme.primary
    fun buildEpisodePlaybackInfo(episode: MediaItem, startPositionMs: Long): PlaybackInfo {
        val playbackOptions = playbackState.playbackOptions
        val selectedSourceId = playbackOptions.selectedMediaSource?.id
            ?: playbackState.mediaSourceId
            ?: episode.mediaSources.firstOrNull()?.id
        val selectedAudioIndex = playbackOptions.selectedAudioStream?.index
            ?: playbackState.audioStreamIndex
        val episodeSource = episode.mediaSources.firstOrNull { it.id == selectedSourceId }
            ?: episode.mediaSources.firstOrNull()
        val audioStreams = episodeSource?.audioStreams
            ?: playbackOptions.selectedMediaSource?.audioStreams
            ?: playbackState.availableAudioStreams
        val subtitleStreams = episodeSource?.subtitleStreams
            ?: playbackOptions.selectedMediaSource?.subtitleStreams
            ?: playbackState.availableSubtitleStreams
        val resolvedSubtitleIndex = playbackOptions.selectedSubtitleStream?.index
            ?: playbackState.subtitleStreamIndex
            ?: episode.resolveSubtitleOffIndex(
                selectedSourceId,
                audioStreams,
                subtitleStreams
            )

        return PlaybackInfo(
            mediaItem = episode,
            mediaSourceId = selectedSourceId,
            audioStreamIndex = selectedAudioIndex,
            subtitleStreamIndex = resolvedSubtitleIndex,
            startPosition = startPositionMs,
            maxBitrate = playbackOptions.selectedMediaSource?.bitrate
        )
    }
    fun buildSpecialFeaturePlaybackInfo(feature: MediaItem): PlaybackInfo {
        val source = feature.mediaSources.firstOrNull()
        val startPositionMs = (feature.userData?.playbackPositionTicks ?: 0L) / 10_000
        return PlaybackInfo(
            mediaItem = feature,
            mediaSourceId = source?.id,
            audioStreamIndex = source?.defaultAudioStream?.index,
            subtitleStreamIndex = source?.defaultSubtitleStream?.index,
            startPosition = startPositionMs,
            maxBitrate = source?.bitrate
        )
    }
    val handleEpisodeClick = remember(playbackState) {
        { episode: MediaItem ->
            val playbackOptions = playbackState.playbackOptions
            val startPositionMs = when {
                playbackOptions.startFromBeginning -> 0L
                playbackOptions.resumePositionTicks > 0L -> playbackOptions.resumePositionTicks / 10_000
                playbackState.startPositionMs > 0L -> playbackState.startPositionMs
                else -> (episode.userData?.playbackPositionTicks ?: 0L) / 10_000
            }
            val playbackInfo = buildEpisodePlaybackInfo(episode, startPositionMs)
            onEpisodeClick(playbackInfo)
        }
    }
    val handleSpecialFeatureClick = remember {
        { feature: MediaItem ->
            val playbackInfo = buildSpecialFeaturePlaybackInfo(feature)
            onEpisodeClick(playbackInfo)
        }
    }
    val isMovie = mediaType == MediaType.MOVIE
    val mediaTechnicalMetadata = remember(mediaItem.id, mediaItem.mediaSources) {
        mediaItem.getPrimaryTechnicalMetadata()
    }
    val mediaTechnicalValues = remember(mediaTechnicalMetadata) {
        mediaTechnicalMetadata?.displayValues ?: emptyList()
    }
    val specialFeaturesSorted = remember(specialFeatures) {
        specialFeatures.sortedWith(
            compareBy<MediaItem> { it.indexNumber ?: Int.MAX_VALUE }
                .thenBy { it.name.lowercase() }
        )
    }
    val extrasList = remember(specialFeaturesSorted) {
        specialFeaturesSorted
    }
    val seriesIdForExtras = remember(mediaItem.id, mediaItem.seriesId, mediaType) {
        if (mediaType == MediaType.SEASON) mediaItem.seriesId ?: mediaItem.id else mediaItem.id
    }
    val extrasSeasonId = remember(seriesIdForExtras) { "extras_$seriesIdForExtras" }
    val hasExtras = mediaType == MediaType.SHOW && extrasList.isNotEmpty()
    val displaySeasons = remember(seasons, hasExtras, extrasSeasonId, extrasList.size) {
        if (!hasExtras) seasons
        else buildList {
            add(
                mediaItem.copy(
                    id = extrasSeasonId,
                    name = "Extras",
                    title = "Extras",
                    type = "Season",
                    overview = mediaItem.overview,
                    primaryImageTag = mediaItem.primaryImageTag,
                    backdropImageTags = mediaItem.backdropImageTags,
                    genres = mediaItem.genres,
                    isFolder = true,
                    childCount = extrasList.size,
                    seasonId = null,
                    seasonName = null,
                    seasonPrimaryImageTag = null,
                    parentIndexNumber = null,
                    indexNumber = Int.MIN_VALUE 
                )
            )
            addAll(seasons)
        }
    }
    val showSeasonBar = mediaType == MediaType.SHOW && displaySeasons.isNotEmpty()
    val playFocusRequester = remember(mediaItem.id) { FocusRequester() }
    var selectedSeasonId by remember(mediaItem.id, mediaType, seasons, displaySeasons, initialSeasonId) {
        mutableStateOf(
            if (mediaType == MediaType.SEASON) {
                mediaItem.id
            } else {
                val preferredSeasonId = initialSeasonId?.takeIf { id -> seasons.any { it.id == id } }
                preferredSeasonId ?: seasons.firstOrNull()?.id ?: displaySeasons.firstOrNull()?.id
            }
        )
    }

    val showLogoUrl = mediaItem.getLogoUrl(serverUrl)
    val logoUrl = if (mediaType == MediaType.SHOW) {
        val seasonLogoUrl = displaySeasons
            .firstOrNull { it.id == selectedSeasonId && it.id != extrasSeasonId }
            ?.getLogoUrl(serverUrl)
        seasonLogoUrl ?: showLogoUrl
    } else {
        showLogoUrl
    }
    val seasonFocusRequesters = remember(displaySeasons) { List(displaySeasons.size) { FocusRequester() } }
    val activeEpisodes = remember(selectedSeasonId, extrasSeasonId, extrasList, episodes) {
        if (selectedSeasonId == extrasSeasonId) extrasList else episodes
    }
    val episodeFocusRequesters = remember(activeEpisodes) { List(activeEpisodes.size) { FocusRequester() } }
    val episodeListState = rememberLazyListState()
    var selectedEpisode by remember(activeEpisodes) { mutableStateOf<MediaItem?>(null) }
    var episodeActionsSourceId by remember(activeEpisodes) { mutableStateOf<String?>(null) }
    var currentEpisodeFocusId by remember { mutableStateOf<String?>(null) }
    var isActionsRowFocused by remember { mutableStateOf(false) }
    var isReturningToEpisodes by remember { mutableStateOf(false) }

    fun clearSelectedEpisode() {
        val episode = selectedEpisode ?: return
        selectedEpisode = null
        isReturningToEpisodes = false
        onEpisodeUnfocused(episode)
        episodeActionsSourceId = null
    }

    LaunchedEffect(activeEpisodes) {
        val selectedId = selectedEpisode?.id
        if (selectedId != null && activeEpisodes.none { it.id == selectedId }) {
            clearSelectedEpisode()
            currentEpisodeFocusId = null
            isActionsRowFocused = false
            isReturningToEpisodes = false
        }
        val storedId = episodeActionsSourceId
        if (storedId != null && activeEpisodes.none { it.id == storedId }) {
            episodeActionsSourceId = null
        }
    }

    val actionsFocusRequester = remember(selectedEpisode?.id) { FocusRequester() }
    val selectedSeasonIndex = displaySeasons.indexOfFirst { it.id == selectedSeasonId }.takeIf { it >= 0 } ?: 0
    var seasonBarAutoFocused by remember(mediaItem.id, displaySeasons.size) { mutableStateOf(false) }
    var episodeRowAutoFocused by remember(mediaItem.id, activeEpisodes.size, initialEpisodeIndex, selectedSeasonId) { mutableStateOf(false) }
    val targetEpisodeIndex = remember(initialEpisodeIndex, activeEpisodes, selectedSeasonId) {
        if (activeEpisodes.isEmpty()) null else initialEpisodeIndex.coerceIn(0, activeEpisodes.lastIndex)
    }
    val tagline = mediaItem.getPrimaryTagline()
    val requestSelectedSeasonFocus = remember(seasonFocusRequesters, selectedSeasonIndex, showSeasonBar) {
        {
            if (showSeasonBar && seasonFocusRequesters.isNotEmpty()) {
                val targetIndex = selectedSeasonIndex.coerceIn(0, seasonFocusRequesters.lastIndex)
                seasonFocusRequesters[targetIndex].requestFocus()
            }
        }
    }

    LaunchedEffect(mediaItem.id) {
        if (isMovie) {
            playFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(showSeasonBar) {
        if (!showSeasonBar) {
            seasonBarAutoFocused = false
            episodeRowAutoFocused = false
        }
    }

    LaunchedEffect(showSeasonBar, seasonFocusRequesters, selectedSeasonIndex, shouldAutoFocusEpisode) {
        if (
            showSeasonBar &&
            seasonFocusRequesters.isNotEmpty() &&
            !seasonBarAutoFocused &&
            !shouldAutoFocusEpisode
        ) {
            val targetIndex = selectedSeasonIndex.coerceIn(0, seasonFocusRequesters.lastIndex)
            seasonFocusRequesters[targetIndex].requestFocus()
            seasonBarAutoFocused = true
        }
    }

    LaunchedEffect(showSeasonBar, activeEpisodes, targetEpisodeIndex, shouldAutoFocusEpisode) {
        if (activeEpisodes.isNotEmpty() && episodeFocusRequesters.isNotEmpty() && !episodeRowAutoFocused) {
            val targetIndex = targetEpisodeIndex ?: 0
            if (!showSeasonBar || shouldAutoFocusEpisode) {
                episodeListState.scrollToItem(targetIndex)
                snapshotFlow {
                    episodeListState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
                }.filter { it }.first()
                episodeFocusRequesters[targetIndex].requestFocus()
                episodeRowAutoFocused = true
            }
        }
    }

    val heroBackgroundUrl = remember(
        focusedEpisode?.id,
        focusedEpisode?.primaryImageTag,
        focusedEpisode?.backdropImageTags,
        mediaItem.id,
        serverUrl
    ) {
        if (focusedEpisode != null) {
            val episodePrimaryTag = focusedEpisode.primaryImageTag
            val episodeBackdropTag = focusedEpisode.backdropImageTags.firstOrNull()

            when {
                episodePrimaryTag != null -> focusedEpisode.getImageUrl(
                    serverUrl = serverUrl,
                    imageType = ApiConstants.IMAGE_TYPE_PRIMARY,
                    imageTag = episodePrimaryTag
                )
                episodeBackdropTag != null -> focusedEpisode.getBackdropUrl(serverUrl)
                mediaItem.backdropImageTags.firstOrNull() != null -> mediaItem.getBackdropUrl(serverUrl)
                mediaItem.primaryImageTag != null -> mediaItem.getImageUrl(
                    serverUrl = serverUrl,
                    imageType = ApiConstants.IMAGE_TYPE_PRIMARY,
                    imageTag = mediaItem.primaryImageTag
                )

                else -> null
            }
        } else {
            when {
                mediaItem.backdropImageTags.firstOrNull() != null -> mediaItem.getBackdropUrl(serverUrl)
                mediaItem.primaryImageTag != null -> mediaItem.getImageUrl(
                    serverUrl = serverUrl,
                    imageType = ApiConstants.IMAGE_TYPE_PRIMARY,
                    imageTag = mediaItem.primaryImageTag
                )
                else -> null
            }
        }
    }

    val heroBlurHash = remember(
        focusedEpisode?.id,
        focusedEpisode?.primaryImageTag,
        focusedEpisode?.backdropImageTags,
        mediaItem.id
    ) {
        if (focusedEpisode != null) {
            val episodePrimaryTag = focusedEpisode.primaryImageTag
            val episodeBackdropTag = focusedEpisode.backdropImageTags.firstOrNull()
            when {
                episodePrimaryTag != null -> focusedEpisode.getBlurHash(ApiConstants.IMAGE_TYPE_PRIMARY, episodePrimaryTag)
                episodeBackdropTag != null -> focusedEpisode.getBlurHash(ApiConstants.IMAGE_TYPE_BACKDROP, episodeBackdropTag)
                else -> null
            }
        } else {
            val backdropTag = mediaItem.backdropImageTags.firstOrNull()
            when {
                backdropTag != null -> mediaItem.getBlurHash(ApiConstants.IMAGE_TYPE_BACKDROP, backdropTag)
                mediaItem.primaryImageTag != null -> mediaItem.getBlurHash(ApiConstants.IMAGE_TYPE_PRIMARY, mediaItem.primaryImageTag)
                else -> null
            }
        }
    }

    val context = LocalContext.current
    var lastSuccessfulHeroUrl by remember { mutableStateOf<String?>(null) }
    var lastHeroWindowKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(focusedEpisode?.id, activeEpisodes, serverUrl) {
        val baseEpisode = focusedEpisode ?: return@LaunchedEffect
        val currentIndex = activeEpisodes.indexOfFirst { it.id == baseEpisode.id }
        if (currentIndex == -1) return@LaunchedEffect

        val imageLoader = Coil.imageLoader(context)
        val windowStart = (currentIndex - 6).coerceAtLeast(0)
        val windowEnd = (currentIndex + 6).coerceAtMost(activeEpisodes.lastIndex)
        val window = activeEpisodes.subList(windowStart, windowEnd + 1)
        val memoryCache = imageLoader.memoryCache

        withContext(Dispatchers.IO) {
            val newKeys = mutableSetOf<String>()
            window.forEach { episode ->
                val url = when {
                    episode.primaryImageTag != null -> episode.getImageUrl(
                        serverUrl = serverUrl,
                        imageType = ApiConstants.IMAGE_TYPE_PRIMARY,
                        imageTag = episode.primaryImageTag
                    )
                    episode.backdropImageTags.firstOrNull() != null -> episode.getBackdropUrl(serverUrl)
                    else -> null
                } ?: return@forEach

                val fullKey = "hero_full:$url"
                newKeys += fullKey

                val fullRequest = ImageRequest.Builder(context)
                    .data(url)
                    .memoryCacheKey(fullKey)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(CoilSize.ORIGINAL)
                    .build()
                imageLoader.execute(fullRequest)
            }
            val evictKeys = lastHeroWindowKeys - newKeys
            evictKeys.forEach { key -> memoryCache?.remove(MemoryCache.Key(key)) }
            lastHeroWindowKeys = newKeys
        }
    }

    HeroBackground(
        imageUrl = heroBackgroundUrl,
        blurHash = heroBlurHash,
        placeholderUrl = lastSuccessfulHeroUrl,
        onImageLoaded = { lastSuccessfulHeroUrl = it }
    ) {
        Column(
            modifier = Modifier.padding(start = calculateRoundedValue(80).sdp)
        ) {
            if (showSeasonBar) {
                SeasonSelectionBar(
                    seasons = displaySeasons,
                    selectedSeasonId = selectedSeasonId,
                    onSeasonSelected = { season ->
                        selectedSeasonId = season.id
                        if (season.id != extrasSeasonId) {
                            onSeasonSelected(season.id)
                        } else {
                            selectedEpisode = null
                            currentEpisodeFocusId = null
                        }
                    },
                    serverUrl = serverUrl,
                    focusRequesters = seasonFocusRequesters,
                    onRequestNavFocus = { sideNavigationFocusRequester?.requestFocus() },
                    mediaItem = mediaItem,
                    dominantColor = seasonAccentColor,
                    onAccentColorChanged = { seasonAccentColorOverride = it }
                )
            }

            Spacer(modifier.height(calculateRoundedValue(18).sdp))

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(
                        min = calculateRoundedValue(200).sdp,
                        max = calculateRoundedValue(400).sdp
                    )
                    .padding(horizontal = calculateRoundedValue(20).sdp)
            ) {
                if (focusedEpisode != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(140.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (logoUrl != null) {
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = mediaItem.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = mediaItem.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val episodeOverview = focusedEpisode.overview ?: mediaItem.overview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(calculateRoundedValue(110).sdp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = focusedEpisode.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!episodeOverview.isNullOrBlank()) {
                                Text(
                                    text = episodeOverview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = modifier.height(calculateRoundedValue(12).sdp))

                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(140.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (logoUrl != null) {
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = mediaItem.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = mediaItem.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier.height(calculateRoundedValue(12).sdp))

                    val primaryGenre = mediaItem.genres.firstOrNull()
                    val yearText = mediaItem.year?.toString()
                    if (primaryGenre != null || yearText != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            primaryGenre?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            if (primaryGenre != null && yearText != null) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            yearText?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    Spacer(modifier.height(calculateRoundedValue(8).sdp))

                    if (isMovie){
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(calculateRoundedValue(140).sdp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (!tagline.isNullOrBlank()) {
                                    Text(
                                        text = tagline,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Spacer(modifier.height(calculateRoundedValue(8).sdp))
                                }

                                mediaItem.overview?.let { overview ->
                                    Text(
                                        text = overview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        maxLines = 6,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier.height(calculateRoundedValue(8).sdp))

                                if (mediaTechnicalValues.isNotEmpty()) {
                                    TechnicalMetadataRow(
                                        values = mediaTechnicalValues,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        if (!tagline.isNullOrBlank()) {
                            Text(
                                text = tagline,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        Spacer(modifier.height(calculateRoundedValue(8).sdp))

                        mediaItem.overview?.let { overview ->
                            Text(
                                text = overview,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier.height(calculateRoundedValue(8).sdp))

                        if (mediaTechnicalValues.isNotEmpty()) {
                            TechnicalMetadataRow(
                                values = mediaTechnicalValues,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier.height(calculateRoundedValue(8).sdp))
                    }
                }
            }

            if(isMovie){
                Spacer(modifier.height(calculateRoundedValue(20).sdp))

                MediaActionButtons(
                    modifier = Modifier.padding(horizontal = calculateRoundedValue(20).sdp),
                    mediaItem = mediaItem,
                    serverUrl = serverUrl,
                    onPlayClick = onPlayClick,
                    playButtonFocusRequester = playFocusRequester
                )
            }

            LazyColumn (
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
            ) {
                if (activeEpisodes.isNotEmpty()) {
                    val currentSeason = displaySeasons.firstOrNull { it.id == selectedSeasonId }
                    val seasonBackdropTag = currentSeason?.backdropImageTags?.firstOrNull()
                    item {
                        SectionHeader (
                            title = if (selectedSeasonId == extrasSeasonId) "Extras" else "Episodes"
                        ){
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(24).sdp),
                                contentPadding = PaddingValues(horizontal = calculateRoundedValue(20).sdp),
                                state = episodeListState
                            ) {
                                itemsIndexed(activeEpisodes) { index, episode ->
                                    val episodeNumber = episode.indexNumber
                                    EpisodeThumbnailCard(
                                        modifier = Modifier.onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                                                requestSelectedSeasonFocus()
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                        episode = episode,
                                        serverUrl = serverUrl,
                                        episodeNumber = episodeNumber,
                                        seasonId = currentSeason?.id,
                                        seasonBackdropTag = seasonBackdropTag,
                                        highlightColor = seasonAccentColor,
                                        onClick = handleEpisodeClick,
                                        focusRequester = episodeFocusRequesters[index],
                                        onFocused = { focused ->
                                            currentEpisodeFocusId = focused.id
                                            selectedEpisode = focused
                                            isReturningToEpisodes = false
                                            onEpisodeFocused(focused)
                                        },
                                        onUnfocused = { unfocused ->
                                            if (currentEpisodeFocusId == unfocused.id) {
                                                currentEpisodeFocusId = null
                                            }
                                            if (!isActionsRowFocused && selectedEpisode?.id == unfocused.id) {
                                                clearSelectedEpisode()
                                            }
                                        },
                                        onRequestActionsFocus = {
                                            if (selectedEpisode != null) {
                                                episodeActionsSourceId = selectedEpisode?.id
                                                isActionsRowFocused = true
                                                isReturningToEpisodes = false
                                                actionsFocusRequester.requestFocus()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if(isMovie){
                    if (specialFeaturesSorted.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Special Features"
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(24).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(20).sdp)
                                ) {
                                    itemsIndexed(specialFeaturesSorted) { _, feature ->
                                        MediaItemCard(
                                            mediaItem = feature,
                                            serverUrl = serverUrl,
                                            cardType = MediaCardType.THUMBNAIL,
                                            onClick = { handleSpecialFeatureClick(feature) },
                                            onFocus = { onSpecialFeatureFocused(feature) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (mediaItem.people.isNotEmpty()) {
                        item {
                            SectionHeader (
                                title = "Cast & Crew"
                            ){
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(24).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    itemsIndexed(mediaItem.people) { index, person ->
                                        PersonCard(
                                            person = person,
                                            serverUrl = serverUrl
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = calculateRoundedValue(80).sdp),
            contentAlignment = Alignment.BottomStart
        ) {
            val episodeForActions = selectedEpisode
            if (episodeForActions != null) {
                EpisodeActionsRow(
                    serverUrl = serverUrl,
                    episode = episodeForActions,
                    focusRequester = actionsFocusRequester,
                    onPlayClick = onPlayClick,
                    buildEpisodePlaybackInfo = { episode, startPositionMs ->
                        buildEpisodePlaybackInfo(episode, startPositionMs)
                    },
                    onRequestEpisodeFocus = {
                        val storedId = episodeActionsSourceId
                        val targetIndex = storedId?.let { id ->
                            activeEpisodes.indexOfFirst { it.id == id }
                        }?.takeIf { it >= 0 }
                        val fallbackIndex = activeEpisodes.indexOfFirst { it.id == episodeForActions.id }
                            .takeIf { it >= 0 }
                        val index = targetIndex ?: fallbackIndex
                        if (index != null && index in episodeFocusRequesters.indices) {
                            isReturningToEpisodes = true
                            episodeFocusRequesters[index].requestFocus()
                        }
                    },
                    onFocusChanged = { isFocused ->
                        if (isFocused) episodeActionsSourceId = episodeForActions.id
                        isActionsRowFocused = isFocused
                        if (!isFocused) {
                            if (currentEpisodeFocusId == null && selectedEpisode != null && !isReturningToEpisodes) {
                                clearSelectedEpisode()
                            }
                            isReturningToEpisodes = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    videoPlaybackViewModel = videoPlaybackViewModel,
                    seasonAccentColor = seasonAccentColor
                )
            }
        }
    }
}

@Composable
private fun TechnicalMetadataRow(
    values: List<String>,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    if (values.isEmpty()) return

    Text(
        text = values.joinToString(" • "),
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.85f),
        textAlign = textAlign,
        modifier = modifier
    )
}
