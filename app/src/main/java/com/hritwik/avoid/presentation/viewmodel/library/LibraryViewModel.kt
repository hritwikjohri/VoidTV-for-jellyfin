package com.hritwik.avoid.presentation.viewmodel.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.Library
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.usecase.library.GetLatestEpisodesUseCase
import com.hritwik.avoid.domain.usecase.library.GetLatestItemsUseCase
import com.hritwik.avoid.domain.usecase.library.GetLatestMoviesUseCase
import com.hritwik.avoid.domain.usecase.library.GetLibraryItemsUseCase
import com.hritwik.avoid.domain.usecase.library.GetLibraryGenresUseCase
import com.hritwik.avoid.domain.usecase.library.GetRecentlyReleasedMoviesUseCase
import com.hritwik.avoid.domain.usecase.library.GetRecentlyReleasedShowsUseCase
import com.hritwik.avoid.domain.usecase.library.GetNextUpUseCase
import com.hritwik.avoid.domain.usecase.library.GetStudiosUseCase
import com.hritwik.avoid.domain.usecase.library.GetRecommendedItemsUseCase
import com.hritwik.avoid.domain.usecase.library.GetResumeItemsUseCase
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.library.GetUserLibrariesUseCase
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.model.library.LibraryType
import com.hritwik.avoid.domain.usecase.media.GetMediaItemDetailUseCase
import com.hritwik.avoid.domain.usecase.search.GetItemsByCategoryUseCase
import com.hritwik.avoid.domain.usecase.library.GetCollectionsUseCase
import com.hritwik.avoid.presentation.ui.state.LibraryItemsState
import com.hritwik.avoid.presentation.ui.state.LibraryState
import com.hritwik.avoid.presentation.ui.state.LibraryGenresState
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.constants.AppConstants
import com.hritwik.avoid.data.local.database.dao.LibraryAlphaDao
import com.hritwik.avoid.data.local.database.dao.LibraryGridCacheDao
import com.hritwik.avoid.data.local.database.entities.LibraryAlphaIndexEntity
import com.hritwik.avoid.data.local.database.entities.LibraryGridCacheEntity
import com.hritwik.avoid.utils.helpers.normalizeChar
import com.hritwik.avoid.presentation.viewmodel.library.LibraryGridWindowCache
import com.hritwik.avoid.presentation.viewmodel.library.toMediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibraryItemsUseCase: GetLibraryItemsUseCase,
    private val getUserLibrariesUseCase: GetUserLibrariesUseCase,
    private val getLatestItemsUseCase: GetLatestItemsUseCase,
    private val getResumeItemsUseCase: GetResumeItemsUseCase,
    private val getMediaItemDetailUseCase: GetMediaItemDetailUseCase,
    private val getLatestEpisodesUseCase: GetLatestEpisodesUseCase,
    private val getLatestMoviesUseCase: GetLatestMoviesUseCase,
    private val getRecentlyReleasedMoviesUseCase: GetRecentlyReleasedMoviesUseCase,
    private val getRecentlyReleasedShowsUseCase: GetRecentlyReleasedShowsUseCase,
    private val getNextUpUseCase: GetNextUpUseCase,
    private val getStudiosUseCase: GetStudiosUseCase,
    private val getRecommendedItemsUseCase: GetRecommendedItemsUseCase,
    private val getLibraryGenresUseCase: GetLibraryGenresUseCase,
    private val getItemsByCategoryUseCase: GetItemsByCategoryUseCase,
    private val getCollectionsUseCase: GetCollectionsUseCase,
    private val libraryRepository: LibraryRepository,
    private val libraryAlphaDao: LibraryAlphaDao,
    private val libraryGridCacheDao: LibraryGridCacheDao
) : ViewModel() {

    private val _libraryState = MutableStateFlow(LibraryState())
    val libraryState: StateFlow<LibraryState> = _libraryState.asStateFlow()

    private val _itemsState = MutableStateFlow(LibraryItemsState())
    val itemsState: StateFlow<LibraryItemsState> = _itemsState.asStateFlow()

    private val _genresState = MutableStateFlow<Map<String, LibraryGenresState>>(emptyMap())
    val genresState: StateFlow<Map<String, LibraryGenresState>> = _genresState.asStateFlow()

    private val pageSize = 30  
    private data class PagerKey(
        val userId: String,
        val libraryIds: List<String>,
        val accessToken: String,
        val sortBy: List<String>,
        val sortOrder: LibrarySortDirection,
        val genre: String?,
        val studio: String?,
        val includeItemTypes: String?
    )
    private val pagerCache = mutableMapOf<PagerKey, Flow<PagingData<MediaItem>>>()
    private val _pagerCacheVersion = MutableStateFlow(0)
    val pagerCacheVersion: StateFlow<Int> = _pagerCacheVersion.asStateFlow()

    
    private val windowCache = LibraryGridWindowCache(AppConstants.GRID_CACHE_WINDOW)
    private val pageFetchSize = 40_000 

    init {
        viewModelScope.launch {
            libraryRepository.resumeItemsFlow.collect { items ->
                _libraryState.update { current ->
                    current.copy(resumeItems = items)
                }
            }
        }

        viewModelScope.launch {
            libraryRepository.nextUpItemsFlow.collect { items ->
                _libraryState.update { current ->
                    current.copy(nextUpEpisodes = items)
                }
            }
        }
    }

    fun reset() {
        _libraryState.value = LibraryState()
        _itemsState.value = LibraryItemsState()
        _genresState.value = emptyMap()
        pagerCache.clear()
        _pagerCacheVersion.update { it + 1 }
    }

    fun clearLibraryPagerCache() {
        pagerCache.clear()
        _pagerCacheVersion.update { it + 1 }
    }

    private fun buildLibraryKey(
        userId: String,
        libraryId: String,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection,
        genre: String?,
        studio: String?
    ): String {
        return listOf(userId, libraryId, sortBy.joinToString("-"), sortOrder.name, genre.orEmpty(), studio.orEmpty()).joinToString("|")
    }

    private suspend fun buildDiskCache(
        userId: String,
        libraryId: String,
        accessToken: String,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection,
        genre: String?,
        studio: String?,
        libraryKey: String
    ) {
        val cacheFields = listOf(
            "Name",
            "SortName",
            "PrimaryImageAspectRatio",
            "ImageTags"
        ).joinToString(",")
        
        libraryGridCacheDao.deleteByKey(libraryKey)
        libraryAlphaDao.deleteByKey(libraryKey)
        libraryAlphaDao.deleteMetaByKey(libraryKey)
        windowCache.clear()

        val headerEntries = mutableListOf<LibraryAlphaIndexEntity>()
        val headerSeen = mutableSetOf<Char>()
        var offset = 0
        while (true) {
            when (val result = getLibraryItemsUseCase(
                GetLibraryItemsUseCase.Params(
                    userId = userId,
                    libraryId = libraryId,
                    accessToken = accessToken,
                    startIndex = offset,
                    limit = pageFetchSize,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                    genre = genre,
                    studio = studio,
                    forceRefresh = true,
                    enableImages = true, 
                    enableUserData = false, 
                    enableImageTypes = ApiConstants.IMAGE_TYPE_PRIMARY,
                    fields = cacheFields
                )
            )) {
                is NetworkResult.Success -> {
                    val data = result.data
                    if (data.isEmpty()) break
                    val entries = data.mapIndexed { idx, item ->
                        val globalIndex = offset + idx
                        val letter = normalizeChar(item.name?.trim()?.firstOrNull())
                        if (headerSeen.add(letter)) {
                            headerEntries.add(
                                LibraryAlphaIndexEntity(
                                    libraryKey = libraryKey,
                                    letter = letter.toString(),
                                    firstIndex = globalIndex
                                )
                            )
                        }
                        val progress = if (item.userData?.playbackPositionTicks != null && item.runTimeTicks != null && item.runTimeTicks > 0) {
                            item.userData.playbackPositionTicks.toFloat() / item.runTimeTicks.toFloat()
                        } else {
                            0f
                        }
                        LibraryGridCacheEntity(
                            libraryKey = libraryKey,
                            indexInSort = globalIndex,
                            mediaId = item.id,
                            name = item.name,
                            type = item.type,
                            primaryImageTag = item.primaryImageTag,
                            backdropImageTag = item.backdropImageTags.firstOrNull(),
                            progress = progress,
                            runTimeTicks = item.runTimeTicks
                        )
                    }
                    libraryGridCacheDao.insertAll(entries)
                    offset += data.size
                    if (data.size < pageFetchSize) break
                }

                is NetworkResult.Error -> {
                    _itemsState.update { it.copy(isLoading = false, error = result.message) }
                    return
                }

                is NetworkResult.Loading -> {}
            }
        }

        
        val headerMap = headerEntries
            .sortedBy { it.firstIndex }
            .fold(mutableMapOf<Char, Int>()) { acc, entry ->
                val letter = entry.letter.firstOrNull() ?: '#'
                acc.putIfAbsent(letter, entry.firstIndex)
                acc
            }

        libraryAlphaDao.replaceIndex(
            key = libraryKey,
            totalCount = offset,
            entries = headerEntries.map { it.copy(id = 0) } 
        )

        _itemsState.update {
            it.copy(
                isLoading = false,
                error = null,
                sectionHeaderIndices = headerMap,
                totalCount = offset,
                libraryKey = libraryKey,
                windowCache = emptyMap()
            )
        }

        
        prefetchWindow(libraryKey, 0)
    }

    private suspend fun prefetchWindow(libraryKey: String, targetIndex: Int) {
        val stateKey = _itemsState.value.libraryKey
        if (stateKey != libraryKey) return
        val range = windowCache.windowFor(targetIndex)
        val windowEntities = libraryGridCacheDao.getWindow(libraryKey, range.first, range.last)
        if (windowEntities.isNotEmpty()) {
            val mapped = windowEntities.map { it.toMediaItem() }
            windowCache.putAll(range.first, mapped)
            _itemsState.update { it.copy(windowCache = windowCache.snapshot()) }
        }
    }

    fun requestWindow(index: Int) {
        val key = _itemsState.value.libraryKey
        if (key.isEmpty()) return
        viewModelScope.launch {
            prefetchWindow(key, index)
        }
    }

    suspend fun getSeriesDetails(
        seriesId: String,
        userId: String,
        accessToken: String
    ): MediaItem? {
        return when (
            val result = getMediaItemDetailUseCase(
                GetMediaItemDetailUseCase.Params(userId, seriesId, accessToken)
            )
        ) {
            is NetworkResult.Success -> result.data
            else -> null
        }
    }

    fun loadLibraryGenres(
        userId: String,
        libraryId: String,
        accessToken: String,
        forceRefresh: Boolean = false
    ) {
        val current = _genresState.value[libraryId]
        if (!forceRefresh && current != null && current.genres.isNotEmpty()) return

        viewModelScope.launch {
            _genresState.update { state ->
                state + (libraryId to LibraryGenresState(isLoading = true, genres = current?.genres.orEmpty()))
            }

            when (val result = getLibraryGenresUseCase(
                GetLibraryGenresUseCase.Params(
                    userId = userId,
                    libraryId = libraryId,
                    accessToken = accessToken
                )
            )) {
                is NetworkResult.Success -> {
                    _genresState.update { state ->
                        state + (libraryId to LibraryGenresState(genres = result.data))
                    }
                }

                is NetworkResult.Error -> {
                    _genresState.update { state ->
                        state + (libraryId to LibraryGenresState(
                            genres = current?.genres.orEmpty(),
                            error = result.message
                        ))
                    }
                }

                is NetworkResult.Loading -> {
                    
                }
            }
        }
    }

    fun loadLibraries(
        userId: String,
        accessToken: String,
        refreshResume: Boolean = false
    ) {
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(isLoading = true, error = null)

            try {
                
                
                val librariesDeferred = async {
                    getUserLibrariesUseCase(GetUserLibrariesUseCase.Params(userId, accessToken))
                }
                val latestItemsDeferred = async {
                    getLatestItemsUseCase(GetLatestItemsUseCase.Params(userId, accessToken))
                }
                val resumeItemsDeferred = if (refreshResume) {
                    async { getResumeItemsUseCase(GetResumeItemsUseCase.Params(userId, accessToken)) }
                } else null
                val latestMoviesDeferred = async {
                    getLatestMoviesUseCase(GetLatestMoviesUseCase.Params(userId, accessToken))
                }
                val recommendedItemsDeferred = async {
                    getRecommendedItemsUseCase(
                        GetRecommendedItemsUseCase.Params(
                            userId,
                            accessToken
                        )
                    )
                }
                val collectionsDeferred = async {
                    getCollectionsUseCase(
                        GetCollectionsUseCase.Params(
                            userId = userId,
                            accessToken = accessToken,
                            limit = 30
                        )
                    )
                }

                
                val librariesResult = librariesDeferred.await()
                val latestItemsResult = latestItemsDeferred.await()
                val resumeItemsResult = resumeItemsDeferred?.await()
                val latestMoviesResult = latestMoviesDeferred.await()
                val recommendedItemsResult = recommendedItemsDeferred.await()
                val collectionsResult = collectionsDeferred.await()

                
                var showsLibraryId: String? = null
                var moviesLibraryId: String? = null

                
                when (librariesResult) {
                    is NetworkResult.Success -> {
                        _libraryState.value = _libraryState.value.copy(
                            libraries = librariesResult.data
                        )
                        showsLibraryId = librariesResult.data.firstOrNull { it.type == LibraryType.TV_SHOWS }?.id
                        moviesLibraryId = librariesResult.data.firstOrNull { it.type == LibraryType.MOVIES }?.id
                        launch {
                            loadLatestItemsByLibrary(
                                userId = userId,
                                accessToken = accessToken,
                                libraries = librariesResult.data
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _libraryState.value = _libraryState.value.copy(
                            error = "Failed to load libraries: ${librariesResult.message}"
                        )
                    }
                    is NetworkResult.Loading -> {}
                }

                when (latestItemsResult) {
                    is NetworkResult.Success -> {
                        _libraryState.value = _libraryState.value.copy(
                            latestItems = latestItemsResult.data
                        )
                    }
                    is NetworkResult.Error -> {
                        println("Failed to load latest items: ${latestItemsResult.message}")
                    }
                    is NetworkResult.Loading -> {}
                }

                when (resumeItemsResult) {
                    is NetworkResult.Success -> {
                        _libraryState.value = _libraryState.value.copy(
                            resumeItems = resumeItemsResult.data
                        )
                    }
                    is NetworkResult.Error -> {
                        println("Failed to load resume items: ${resumeItemsResult.message}")
                    }
                    is NetworkResult.Loading -> {}
                    null -> Unit
                }

                when (latestMoviesResult) {
                    is NetworkResult.Success -> {
                        _libraryState.value = _libraryState.value.copy(
                            latestMovies = latestMoviesResult.data
                        )
                    }
                    is NetworkResult.Error -> {
                        println("Failed to load latest movies: ${latestMoviesResult.message}")
                    }
                    is NetworkResult.Loading -> {}
                }

                when (recommendedItemsResult) {
                    is NetworkResult.Success -> {
                        _libraryState.value = _libraryState.value.copy(
                            recommendedItems = recommendedItemsResult.data
                        )
                    }
                    is NetworkResult.Error -> {
                        println("Failed to load recommended items: ${recommendedItemsResult.message}")
                    }
                    is NetworkResult.Loading -> {}
                }

                when (collectionsResult) {
                    is NetworkResult.Success -> {
                        _libraryState.value = _libraryState.value.copy(
                            collections = collectionsResult.data
                        )
                    }
                    is NetworkResult.Error -> {
                        println("Failed to load collections: ${collectionsResult.message}")
                    }
                    is NetworkResult.Loading -> {}
                }

                
                _libraryState.value = _libraryState.value.copy(isLoading = false)

                
                
                val latestEpisodesDeferred = async {
                    getLatestEpisodesUseCase(GetLatestEpisodesUseCase.Params(userId, accessToken))
                }
                val recentlyReleasedMoviesDeferred = async {
                    getRecentlyReleasedMoviesUseCase(
                        GetRecentlyReleasedMoviesUseCase.Params(
                            userId,
                            accessToken
                        )
                    )
                }
                val recentlyReleasedShowsDeferred = async {
                    getRecentlyReleasedShowsUseCase(
                        GetRecentlyReleasedShowsUseCase.Params(
                            userId,
                            accessToken
                        )
                    )
                }
                
                val nextUpDeferred = async {
                    getNextUpUseCase(
                        GetNextUpUseCase.Params(
                            userId,
                            accessToken
                        )
                    )
                }

                
                val latestEpisodesResult = latestEpisodesDeferred.await()
                val recentlyReleasedMoviesResult = recentlyReleasedMoviesDeferred.await()
                val recentlyReleasedShowsResult = recentlyReleasedShowsDeferred.await()
                val nextUpResult = nextUpDeferred.await()

                

                
                when (latestEpisodesResult) {
                    is NetworkResult.Success -> {
                        _libraryState.value = _libraryState.value.copy(
                            latestEpisodes = latestEpisodesResult.data
                        )
                    }

                    is NetworkResult.Error -> {
                        println("Failed to load latest episodes: ${latestEpisodesResult.message}")
                    }

                    is NetworkResult.Loading -> {
                        
                    }
                }

                
                when (recentlyReleasedMoviesResult) {
                    is NetworkResult.Success -> {
                        _libraryState.value = _libraryState.value.copy(
                            recentlyReleasedMovies = recentlyReleasedMoviesResult.data
                        )
                    }

                    is NetworkResult.Error -> {
                        println("Failed to load recently released movies: ${recentlyReleasedMoviesResult.message}")
                    }

                    is NetworkResult.Loading -> {
                        
                    }
                }

                
                when (recentlyReleasedShowsResult) {
                    is NetworkResult.Success -> {
                        _libraryState.value = _libraryState.value.copy(
                            recentlyReleasedShows = recentlyReleasedShowsResult.data
                        )
                    }

                    is NetworkResult.Error -> {
                        println("Failed to load recently released shows: ${recentlyReleasedShowsResult.message}")
                    }

                    is NetworkResult.Loading -> {
                        
                    }
                }

                
                when (nextUpResult) {
                    is NetworkResult.Success -> {
                        _libraryState.value = _libraryState.value.copy(
                            nextUpEpisodes = nextUpResult.data
                        )
                    }

                    is NetworkResult.Error -> {
                        println("Failed to load next up episodes: ${nextUpResult.message}")
                    }

                    is NetworkResult.Loading -> {
                        
                    }
                }

                showsLibraryId?.let { libraryId ->
                    when (val studiosResult = getStudiosUseCase(
                        GetStudiosUseCase.Params(
                            userId = userId,
                            accessToken = accessToken,
                            libraryId = libraryId,
                            includeItemTypes = ApiConstants.ITEM_TYPE_SERIES
                        )
                    )) {
                        is NetworkResult.Success -> {
                            _libraryState.value = _libraryState.value.copy(
                                showStudios = studiosResult.data
                            )
                        }

                        is NetworkResult.Error -> {
                            _libraryState.value = _libraryState.value.copy(showStudios = emptyList())
                            println("Failed to load show studios: ${studiosResult.message}")
                        }

                        is NetworkResult.Loading -> {
                            
                        }
                    }
                }
                if (showsLibraryId == null) {
                    _libraryState.value = _libraryState.value.copy(showStudios = emptyList())
                }

                moviesLibraryId?.let { libraryId ->
                    when (val studiosResult = getStudiosUseCase(
                        GetStudiosUseCase.Params(
                            userId = userId,
                            accessToken = accessToken,
                            libraryId = libraryId,
                            includeItemTypes = ApiConstants.ITEM_TYPE_MOVIE
                        )
                    )) {
                        is NetworkResult.Success -> {
                            _libraryState.value = _libraryState.value.copy(
                                movieStudios = studiosResult.data
                            )
                        }

                        is NetworkResult.Error -> {
                            _libraryState.value = _libraryState.value.copy(movieStudios = emptyList())
                            println("Failed to load movie studios: ${studiosResult.message}")
                        }

                        is NetworkResult.Loading -> {
                            
                        }
                    }
                }
                if (moviesLibraryId == null) {
                    _libraryState.value = _libraryState.value.copy(movieStudios = emptyList())
                }

            } catch (e: Exception) {
                _libraryState.value = _libraryState.value.copy(
                    error = "An unexpected error occurred: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private suspend fun loadLatestItemsByLibrary(
        userId: String,
        accessToken: String,
        libraries: List<Library>
    ) {
        if (libraries.isEmpty()) {
            _libraryState.update { state -> state.copy(latestItemsByLibrary = emptyMap()) }
            return
        }

        _libraryState.update { state -> state.copy(latestItemsByLibrary = emptyMap()) }

        val latestByLibrary = coroutineScope {
            libraries.associate { library ->
                library.id to async {
                    when (val result = getLatestItemsUseCase(
                        GetLatestItemsUseCase.Params(
                            userId = userId,
                            accessToken = accessToken,
                            libraryId = library.id
                        )
                    )) {
                        is NetworkResult.Success -> result.data
                        else -> emptyList()
                    }
                }
            }.mapValues { (_, deferred) -> deferred.await() }
        }.filterValues { it.isNotEmpty() }

        _libraryState.update { state ->
            state.copy(latestItemsByLibrary = latestByLibrary)
        }
    }

    fun loadLibraryItems(
        userId: String,
        libraryId: String,
        accessToken: String,
        sortBy: List<String> = listOf("SortName"),
        sortOrder: LibrarySortDirection = LibrarySortDirection.ASCENDING,
        supportsAlphaScroller: Boolean = false,
        genre: String? = null,
        studio: String? = null,
        skipCacheRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            
            
            if (supportsAlphaScroller) {
                val key = buildLibraryKey(userId, libraryId, sortBy, sortOrder, genre, studio)
                if (skipCacheRefresh && _itemsState.value.libraryKey == key && _itemsState.value.totalCount > 0) {
                    return@launch
                }
                _itemsState.value = _itemsState.value.copy(
                    isLoading = true,
                    error = null,
                    allTitles = emptyList(),
                    sectionedItems = emptyList(),
                    sectionHeaderIndices = emptyMap(),
                    totalCount = 0,
                    libraryKey = key,
                    windowCache = emptyMap()
                )
                launch {
                    buildDiskCache(
                        userId = userId,
                        libraryId = libraryId,
                        accessToken = accessToken,
                        sortBy = sortBy,
                        sortOrder = sortOrder,
                        genre = genre,
                        studio = studio,
                        libraryKey = key
                    )
                }
            } else {
                
                _itemsState.value = _itemsState.value.copy(
                    allTitles = emptyList(),
                    sectionedItems = emptyList(),
                    sectionHeaderIndices = emptyMap(),
                    totalCount = 0,
                    libraryKey = "",
                    windowCache = emptyMap()
                )
            }
        }
    }

    fun loadMoreLibraryItems(
        userId: String,
        libraryId: String,
        accessToken: String
    ) {
        val state = _itemsState.value
        if (state.isLoading || !state.hasMorePages) return

        viewModelScope.launch {
            _itemsState.value = state.copy(isLoading = true)
            val startIndex = state.items.size
            when (val result = getLibraryItemsUseCase(
                GetLibraryItemsUseCase.Params(
                    userId = userId,
                    libraryId = libraryId,
                    accessToken = accessToken,
                    startIndex = startIndex,
                    limit = pageSize
                )
            )) {
                is NetworkResult.Success -> {
                    val newItems = state.items + result.data
                    _itemsState.value = state.copy(
                        isLoading = false,
                        items = newItems,
                        hasMorePages = result.data.size == pageSize,
                        currentPage = state.currentPage + 1
                    )
                }
                is NetworkResult.Error -> {
                    _itemsState.value = state.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun loadLatestAdditions(userId: String, accessToken: String) {
        viewModelScope.launch {
            when (val result = getLatestItemsUseCase(GetLatestItemsUseCase.Params(userId, accessToken, 20))) {
                is NetworkResult.Success -> {
                    _libraryState.value = _libraryState.value.copy(
                        latestItems = result.data
                    )
                }
                is NetworkResult.Error -> {
                    println("Failed to refresh latest additions: ${result.message}")
                }
                is NetworkResult.Loading -> {
                    
                }
            }
        }
    }

    fun refreshResumeItems(userId: String, accessToken: String) {
        viewModelScope.launch {
            when (val result = getResumeItemsUseCase(GetResumeItemsUseCase.Params(userId, accessToken))) {
                is NetworkResult.Success -> {
                    _libraryState.value = _libraryState.value.copy(
                        resumeItems = result.data
                    )
                }
                is NetworkResult.Error -> {
                    println("Failed to refresh resume items: ${result.message}")
                }
                is NetworkResult.Loading -> {
                    
                }
            }
        }
    }

    fun clearError() {
        _itemsState.value = _itemsState.value.copy(error = null)
    }

    fun libraryItemsPager(
        userId: String,
        libraryId: String,
        libraryIds: List<String>,
        accessToken: String,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection,
        genre: String?,
        studio: String?,
        includeItemTypes: String?
    ): Flow<PagingData<MediaItem>> {
        val normalizedLibraryIds = libraryIds.filter { it.isNotBlank() }.ifEmpty { listOf(libraryId) }
        val key = PagerKey(userId, normalizedLibraryIds, accessToken, sortBy, sortOrder, genre, studio, includeItemTypes)
        return pagerCache.getOrPut(key) {
            Pager(
                PagingConfig(
                    pageSize = pageSize,
                    initialLoadSize = pageSize,  
                    enablePlaceholders = false
                )
            ) {
                LibraryItemsPagingSource(
                    getLibraryItemsUseCase = getLibraryItemsUseCase,
                    getItemsByCategoryUseCase = getItemsByCategoryUseCase,
                    userId = userId,
                    libraryId = libraryId,
                    libraryIds = normalizedLibraryIds,
                    accessToken = accessToken,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                    genre = genre,
                    studio = studio,
                    includeItemTypes = includeItemTypes
                )
            }.flow.cachedIn(viewModelScope)
        }
    }
}
