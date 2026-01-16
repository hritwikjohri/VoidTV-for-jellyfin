package com.hritwik.avoid.presentation.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.state.FeatureContentFocusTarget
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.utils.extensions.getBackdropUrl
import com.hritwik.avoid.utils.extensions.getImageUrl
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

@Composable
fun FeatureHeader(
    modifier: Modifier = Modifier,
    items: List<MediaItem>,
    selected: MediaItem? = null,
    serverUrl: String,
    onItemSelected: (MediaItem) -> Unit,
    onItemFocused: (MediaItem) -> Unit = {},
    autoPlayDelay: Long = 5000L,
    showThumbnails: Boolean = true,
    onMediaItemClick: (MediaItem) -> Unit = {},
    onMediaItemFocus: (MediaItem) -> Unit = {},
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel,
    sideNavigationFocusRequester: FocusRequester? = null
) {
    if (items.isEmpty()) return
    val stableItems = remember(items) { items }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isAutoPlaying by remember { mutableStateOf(true) }
    var focusedItem by remember { mutableStateOf(selected) }
    var displayItemCache by remember { mutableStateOf<Pair<MediaItem?, MediaItem?>>(null to null) }
    var heroBackgroundItem by remember { mutableStateOf<MediaItem?>(null) }
    
    val seriesDetailsCache by remember { mutableStateOf(mutableMapOf<String, MediaItem>()) }
    val watchNowFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        watchNowFocusRequester.requestFocus()
    }

    val carouselItem = remember(currentIndex, stableItems) {
        stableItems.getOrNull(currentIndex) ?: stableItems.first()
    }

    
    
    
    val currentItem = remember(focusedItem, carouselItem, displayItemCache) {
        val focused = focusedItem
        when {
            
            focused != null -> {
                val (cachedFocused, cachedDisplay) = displayItemCache
                
                if (cachedFocused?.id == focused.id && cachedDisplay != null) {
                    cachedDisplay
                } else {
                    focused  
                }
            }
            
            else -> displayItemCache.second ?: carouselItem
        }
    }

    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.libraryState.collectAsStateWithLifecycle()

    
    val resumeItems: List<MediaItem> by remember { derivedStateOf { libraryState.resumeItems } }

    val contentFocusTarget = remember(showThumbnails, stableItems, resumeItems) {
        when {
            showThumbnails && stableItems.isNotEmpty() -> FeatureContentFocusTarget.Latest
            resumeItems.isNotEmpty() -> FeatureContentFocusTarget.Resume
            else -> null
        }
    }

    
    LaunchedEffect(focusedItem, authState.authSession) {
        val item = focusedItem
        if (item == null) {
            isAutoPlaying = true
            displayItemCache = null to null
            return@LaunchedEffect
        }

        isAutoPlaying = false

        
        val seriesId = item.seriesId
        if (item.type.lowercase() == "episode" && seriesId != null) {
            val session = authState.authSession
            if (session != null) {
                
                val seriesDetails = seriesDetailsCache[seriesId] ?: run {
                    libraryViewModel.getSeriesDetails(
                        seriesId = seriesId,
                        userId = session.userId.id,
                        accessToken = session.accessToken
                    )?.also { details ->
                        seriesDetailsCache[seriesId] = details
                    }
                }
                displayItemCache = item to (seriesDetails ?: item)
                when {
                    seriesDetails?.backdropImageTags?.isNotEmpty() == true -> {
                        heroBackgroundItem = seriesDetails
                        val url = seriesDetails.getBackdropUrl(serverUrl)
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .memoryCacheKey("hero_full:$url")
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .size(CoilSize.ORIGINAL)
                            .build()
                        Coil.imageLoader(context).enqueue(request)
                    }
                    item.backdropImageTags.isNotEmpty() -> {
                        heroBackgroundItem = item
                    }
                }
            } else {
                displayItemCache = item to item
                if (item.backdropImageTags.isNotEmpty()) {
                    heroBackgroundItem = item
                }
            }
        } else {
            displayItemCache = item to item
            heroBackgroundItem = item
        }
    }

    LaunchedEffect(currentIndex, isAutoPlaying, stableItems.size, autoPlayDelay) {
        if (isAutoPlaying && stableItems.size > 1) {
            delay(autoPlayDelay)
            if (isAutoPlaying) {
                currentIndex = (currentIndex + 1) % stableItems.size
            }
        }
    }

    LaunchedEffect(currentItem, focusedItem) {
        if (focusedItem == null) {
            heroBackgroundItem = currentItem
        }
    }

    LaunchedEffect(stableItems, serverUrl) {
        val imageLoader = Coil.imageLoader(context)
        stableItems.take(6).forEach { item ->
            val url = when {
                item.backdropImageTags.firstOrNull() != null -> item.getBackdropUrl(serverUrl)
                item.primaryImageTag != null -> item.getImageUrl(
                    serverUrl = serverUrl,
                    imageType = "Primary",
                    imageTag = item.primaryImageTag
                )
                else -> null
            }
            if (url != null) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .memoryCacheKey("hero_full:$url")
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(CoilSize.ORIGINAL)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }
    LaunchedEffect(resumeItems, serverUrl, authState.authSession) {
        if (resumeItems.isEmpty()) return@LaunchedEffect
        val session = authState.authSession ?: return@LaunchedEffect
        val imageLoader = Coil.imageLoader(context)
        withContext(Dispatchers.IO) {
            resumeItems.forEach { item ->
                ensureActive()
                val backdropUrl = when {
                    item.type.lowercase() == "episode" && item.seriesId != null -> {
                        val seriesDetails = seriesDetailsCache[item.seriesId] ?: run {
                            libraryViewModel.getSeriesDetails(
                                seriesId = item.seriesId,
                                userId = session.userId.id,
                                accessToken = session.accessToken
                            )?.also { details ->
                                seriesDetailsCache[item.seriesId] = details
                            }
                        }
                        if (seriesDetails?.backdropImageTags?.isNotEmpty() == true) {
                            seriesDetails.getBackdropUrl(serverUrl)
                        } else {
                            null
                        }
                    }
                    item.backdropImageTags.firstOrNull() != null -> item.getBackdropUrl(serverUrl)
                    else -> null
                }
                if (backdropUrl != null) {
                    val request = ImageRequest.Builder(context)
                        .data(backdropUrl)
                        .memoryCacheKey("hero_full:$backdropUrl")
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .size(CoilSize.ORIGINAL)
                        .build()
                    imageLoader.execute(request)
                }
            }
        }
    }

    val horizontalPadding = calculateRoundedValue(80).sdp
    val sectionSpacing = calculateRoundedValue(6).sdp
    val heroHeight = calculateRoundedValue(460).sdp

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        heroBackgroundItem?.let {
            BackgroundSection(
                currentItem = it,
                serverUrl = serverUrl
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            currentItem?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight),
                    contentAlignment = Alignment.CenterStart
                ) {
                    HeroContent(
                        currentItem = it,
                        serverUrl = serverUrl,
                        watchNowFocusRequester = watchNowFocusRequester,
                        contentFocusRequester = contentFocusRequester,
                        sideNavigationFocusRequester = sideNavigationFocusRequester,
                        onMediaItemClick = onMediaItemClick
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ContentSection(
                    modifier = Modifier.fillMaxSize(),
                    items = stableItems,
                    showThumbnails = showThumbnails,
                    serverUrl = serverUrl,
                    libraryState = libraryState,
                    authState = authState,
                    contentFocusTarget = contentFocusTarget,
                    contentFocusRequester = contentFocusRequester,
                    sideNavigationFocusRequester = sideNavigationFocusRequester,
                    onItemSelected = onItemSelected,
                    onItemFocused = onItemFocused,
                    onMediaItemClick = onMediaItemClick,
                    onMediaItemFocus = onMediaItemFocus,
                    onFocusedItemChange = { focusedItem = it }
                )
            }
        }
    }
}
