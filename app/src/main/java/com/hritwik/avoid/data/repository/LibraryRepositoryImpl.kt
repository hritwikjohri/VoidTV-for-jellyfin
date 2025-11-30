package com.hritwik.avoid.data.repository

import android.content.Context
import android.provider.Settings.Secure.ANDROID_ID
import android.provider.Settings.System.getString
import com.hritwik.avoid.data.common.BaseRepository
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.common.NetworkResult.Success
import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.LibraryDao
import com.hritwik.avoid.data.local.database.dao.MediaItemDao
import com.hritwik.avoid.data.local.database.dao.PendingActionDao
import com.hritwik.avoid.data.local.database.entities.LibraryEntity
import com.hritwik.avoid.data.local.database.entities.MediaItemEntity
import com.hritwik.avoid.data.local.database.entities.PendingActionEntity
import com.hritwik.avoid.data.network.PriorityDispatcher
import com.hritwik.avoid.data.remote.JellyfinApiService
import com.hritwik.avoid.data.remote.dto.library.BaseItemDto
import com.hritwik.avoid.data.remote.dto.library.StudioDto
import com.hritwik.avoid.data.remote.dto.library.UserDataDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackCodecProfileDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackDirectPlayProfileDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackDeviceProfileDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackInfoMediaSourceDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackInfoRequestDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackProgressRequest
import com.hritwik.avoid.data.remote.dto.playback.PlaybackStartRequest
import com.hritwik.avoid.data.remote.dto.playback.PlaybackStopRequest
import com.hritwik.avoid.data.remote.dto.playback.PlaybackSubtitleProfileDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackTranscodingProfileDto
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.mapper.PlaybackMapper
import com.hritwik.avoid.domain.model.auth.User
import com.hritwik.avoid.domain.model.library.HomeScreenData
import com.hritwik.avoid.domain.model.library.Library
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.model.library.LibraryType
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.PendingAction
import com.hritwik.avoid.domain.model.library.Person
import com.hritwik.avoid.domain.model.library.Studio
import com.hritwik.avoid.domain.model.library.UserData
import com.hritwik.avoid.domain.model.playback.PlaybackStreamInfo
import com.hritwik.avoid.domain.model.playback.PlaybackTranscodeOption
import com.hritwik.avoid.domain.model.playback.Segment
import com.hritwik.avoid.domain.model.playback.TranscodeRequestParameters
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.repository.RelatedResources
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.extensions.extractTvdbId
import com.hritwik.avoid.utils.helpers.CodecDetector
import android.util.Log
import com.hritwik.avoid.utils.helpers.NetworkMonitor
import com.hritwik.avoid.utils.helpers.normalizeUuid
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val THRESHOLD = 10_000_000L
private const val DEFAULT_NEXT_UP_LIMIT = 20

private const val DEFAULT_MEDIA_IMAGE_TYPES = ApiConstants.DEFAULT_MEDIA_IMAGE_TYPES

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val retrofitBuilder: Retrofit.Builder,
    private val preferencesManager: PreferencesManager,
    private val playbackMapper: PlaybackMapper,
    private val libraryDao: LibraryDao,
    private val mediaItemDao: MediaItemDao,
    private val pendingActionDao: PendingActionDao,
    private val networkMonitor: NetworkMonitor,
    @param:ApplicationContext private val context: Context,
    private val continueWatchingStore: ContinueWatchingStore,
    private val nextUpStore: NextUpStore,
    private val serverConnectionManager: ServerConnectionManager,
    priorityDispatcher: PriorityDispatcher
) : BaseRepository(priorityDispatcher, serverConnectionManager), LibraryRepository {

    companion object {
        private const val TAG = "LibraryRepositoryImpl"
    }

    private val deviceId: String by lazy {
        getString(context.contentResolver, ANDROID_ID) ?: "unknown"
    }

    private val movieDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    override val resumeItemsFlow: StateFlow<List<MediaItem>>
        get() = continueWatchingStore.items
    override val nextUpItemsFlow: StateFlow<List<MediaItem>>
        get() = nextUpStore.items

    private fun createApiService(serverUrl: String): JellyfinApiService {
        val baseUrl = if (!serverUrl.endsWith("/")) "$serverUrl/" else serverUrl
        return retrofitBuilder
            .baseUrl(baseUrl)
            .build()
            .create(JellyfinApiService::class.java)
    }

    override suspend fun getUserLibraries(
        userId: String,
        accessToken: String
    ): NetworkResult<List<Library>> {
        val cached = libraryDao.getAllLibraries(userId).first()
        if (!networkMonitor.isConnected.value) {
            return if (cached.isNotEmpty()) {
                Success(cached.map { it.toDomain() })
            } else {
                NetworkResult.Error(AppError.Network("No network connection"))
            }
        }

        if (cached.isNotEmpty()) {
            return Success(cached.map { it.toDomain() })
        }

        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getUserLibraries(userId, authHeader)

            val libraries = response.items.map { dto ->
                mapToLibrary(dto)
            }

            libraryDao.insertLibraries(libraries.map { it.toEntity(userId) })

            libraries
        }
    }

    override suspend fun getLibraryItems(
        userId: String,
        libraryId: String,
        accessToken: String,
        startIndex: Int,
        limit: Int,
        forceRefresh: Boolean,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection,
        genre: String?,
        studio: String?,
        enableImages: Boolean,
        enableUserData: Boolean,
        enableImageTypes: String?,
        fields: String?
    ): NetworkResult<List<MediaItem>> {
        
        

        if (!networkMonitor.isConnected.value) {
            return NetworkResult.Error(AppError.Network("No network connection"))
        }

        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getLibraryItems(
                userId = userId,
                parentId = libraryId,
                includeItemTypes = listOf(
                    ApiConstants.ITEM_TYPE_MOVIE,
                    ApiConstants.ITEM_TYPE_SERIES
                ).joinToString(","),
                recursive = true,
                startIndex = startIndex,
                limit = limit,
                sortBy = sortBy.joinToString(","),
                sortOrder = sortOrder.toApiValue(),
                genres = genre,
                studios = studio,
                fields = fields ?: ApiConstants.FIELDS_MINIMAL,
                enableImageTypes = enableImageTypes ?: ApiConstants.IMAGE_TYPE_PRIMARY,
                enableImages = enableImages,
                enableUserData = enableUserData,
                authorization = authHeader
            )

            val dedupedItems = response.items
                .filterNot { dto ->
                    dto.type?.equals(ApiConstants.ITEM_TYPE_EPISODE, ignoreCase = true) == true
                }
                .distinctBy { dto -> dto.id }

            if (dedupedItems.size != response.items.size) {
                Log.d(
                    TAG,
                    "getLibraryItems merged duplicate items by Id for library=$libraryId: " +
                        "requested=${response.items.size}, distinct=${dedupedItems.size}"
                )
            }

            dedupedItems.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getLibraryGenres(
        userId: String,
        libraryId: String,
        accessToken: String,
        sortOrder: LibrarySortDirection
    ): NetworkResult<List<String>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getGenres(
                userId = userId,
                parentId = libraryId,
                limit = 200,
                sortBy = "SortName",
                sortOrder = sortOrder.toApiValue(),
                enableImages = false,
                authorization = authHeader
            )

            response.items.mapNotNull { it.name }.distinct().sortedWith(
                if (sortOrder == LibrarySortDirection.ASCENDING) compareBy { it.lowercase() }
                else compareByDescending { it.lowercase() }
            )
        }
    }

    override suspend fun getHomeScreenData(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<HomeScreenData> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val errors = mutableListOf<String>()
            val cachedLibraries = libraryDao.getAllLibraries(userId).first()

            val resultPair = coroutineScope {
                val librariesDeferred = async {
                    try {
                        val response = apiService.getUserLibraries(userId, authHeader)
                        val libraries = response.items.map { dto -> mapToLibrary(dto) }
                        libraryDao.insertLibraries(libraries.map { it.toEntity(userId) })
                        Result.success(libraries)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val latestItemsDeferred = async {
                    try {
                        val items = apiService.getLatestItems(userId, limit, authorization = authHeader)
                            .map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val resumeItemsDeferred = async {
                    try {
                        val items = apiService.getResumeItems(
                            userId = userId,
                            limit = limit,
                            fields = ApiConstants.FIELDS_STANDARD,
                            enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                            authorization = authHeader
                        )
                            .items.map { dto -> mapToMediaItem(dto) }
                        continueWatchingStore.setInitial(items)
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val nextUpItemsDeferred = async {
                    try {
                        val items = apiService.getNextUpItems(
                            userId = userId,
                            limit = limit,
                            authorization = authHeader
                        ).items.filter { it.type == "Episode" }
                            .map { dto -> mapToMediaItem(dto) }
                        nextUpStore.setInitial(items, limit)
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val latestEpisodesDeferred = async {
                    try {
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Episode",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "DateCreated",
                            sortOrder = "Descending",
                            enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val latestMoviesDeferred = async {
                    try {
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Movie",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "DateCreated",
                            sortOrder = "Descending",
                            enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val moviesDeferred = async {
                    try {
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Movie",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "SortName",
                            sortOrder = "Ascending",
                            enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val recentlyReleasedMoviesDeferred = async {
                    try {
                        val minPremiereDate = LocalDate.now().minusMonths(6).format(movieDateFormatter)
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Movie",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "PremiereDate",
                            sortOrder = "Descending",
                            minPremiereDate = minPremiereDate,
                            enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val recentlyReleasedShowsDeferred = async {
                    try {
                        val minPremiereDate = LocalDate.now().minusMonths(3).format(movieDateFormatter)
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Series",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "PremiereDate",
                            sortOrder = "Descending",
                            minPremiereDate = minPremiereDate,
                            enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val showsDeferred = async {
                    try {
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Series",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "SortName",
                            sortOrder = "Ascending",
                            enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val recommendedItemsDeferred = async {
                    try {
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Movie,Series",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "CommunityRating",
                            sortOrder = "Descending",
                            isPlayed = false,
                            minCommunityRating = 7.0,
                            enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val collectionsDeferred = async {
                    try {
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "BoxSet",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "SortName",
                            sortOrder = "Ascending",
                            enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }

                val homeData = HomeScreenData(
                    libraries = emptyList(),
                    latestItems = latestItemsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load latest items: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    resumeItems = resumeItemsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load resume items: ${throwable.message ?: "Unknown error"}"
                        continueWatchingStore.items.value
                    },
                    nextUpItems = nextUpItemsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load next up items: ${throwable.message ?: "Unknown error"}"
                        nextUpStore.snapshot(limit)
                    },
                    latestEpisodes = latestEpisodesDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load latest episodes: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    latestMovies = latestMoviesDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load latest movies: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    movies = moviesDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load movies: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    shows = showsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load shows: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    recentlyReleasedMovies = recentlyReleasedMoviesDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load recently released movies: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    recentlyReleasedShows = recentlyReleasedShowsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load recently released shows: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    recommendedItems = recommendedItemsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load recommended items: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    collections = collectionsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load collections: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    errors = emptyList()
                )

                homeData to librariesDeferred.await()
            }

            val (homeData, librariesResult) = resultPair
            val libraries = if (librariesResult.isSuccess) {
                librariesResult.getOrThrow()
            } else {
                errors += "Failed to load libraries: ${librariesResult.exceptionOrNull()?.message ?: "Unknown error"}"
                cachedLibraries.map { it.toDomain() }
            }

            val aggregatedErrors = errors.toList()
            homeData.copy(
                libraries = libraries,
                errors = aggregatedErrors
            )
        }
    }

    override suspend fun getLatestItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getLatestItems(
                userId = userId,
                limit = limit,
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getResumeItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        val result = safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getResumeItems(
                userId = userId,
                limit = limit,
                fields = ApiConstants.FIELDS_STANDARD,
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
        if (result is Success) {
            continueWatchingStore.setInitial(result.data)
        }
        return result
    }

    override suspend fun getLatestEpisodes(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Episode",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "DateCreated",
                sortOrder = "Descending",
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getLatestMovies(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "DateCreated",
                sortOrder = "Descending",
                fields = ApiConstants.FIELDS_BASIC,
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getMovies(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "SortName",
                sortOrder = "Ascending",
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getShows(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Series",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "SortName",
                sortOrder = "Ascending",
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getRecentlyReleasedMovies(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val minPremiereDate = LocalDate.now().minusMonths(6).format(movieDateFormatter)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "PremiereDate",
                sortOrder = "Descending",
                minPremiereDate = minPremiereDate,
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getRecentlyReleasedShows(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val minPremiereDate = LocalDate.now().minusMonths(3).format(movieDateFormatter)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Series",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "PremiereDate",
                sortOrder = "Descending",
                minPremiereDate = minPremiereDate,
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getRecommendedItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)


            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie,Series",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "CommunityRating",
                sortOrder = "Descending",
                isPlayed = false,
                minCommunityRating = 7.0,
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getCollections(
        userId: String,
        accessToken: String,
        startIndex: Int,
        limit: Int,
        tags: List<String>?
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "BoxSet",
                recursive = true,
                tags = tags?.joinToString(","),
                startIndex = startIndex,
                limit = limit,
                sortBy = "SortName",
                sortOrder = "Ascending",
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getCollectionItems(
        userId: String,
        accessToken: String,
        collectionId: String,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection,
        startIndex: Int,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                parentId = collectionId,
                recursive = true,
                startIndex = startIndex,
                limit = limit,
                sortBy = sortBy.joinToString(","),
                sortOrder = sortOrder.toApiValue(),
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getTrendingItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)


            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie,Series",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "PlayCount",
                sortOrder = "Descending",
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }


    override fun getFavoriteItems(userId: String): Flow<List<MediaItem>> {
        return mediaItemDao.getFavoriteItems(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getWatchlistItems(userId: String): Flow<List<MediaItem>> {
        return mediaItemDao.getWatchlistItems(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPlayedItems(userId: String): Flow<List<MediaItem>> {
        return mediaItemDao.getPlayedItems(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getFavoriteItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = listOf(
                    ApiConstants.ITEM_TYPE_MOVIE,
                    ApiConstants.ITEM_TYPE_SERIES,
                    ApiConstants.ITEM_TYPE_EPISODE
                ).joinToString(","),
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "SortName",
                sortOrder = "Ascending",
                isFavorite = true,
                enableImageTypes = "Primary,Backdrop,Thumb",
                fields = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,IndexNumber,UserData,SeriesName",
                authorization = authHeader
            )

            val items = response.items.map { dto ->
                mapToMediaItem(dto)
            }
            mediaItemDao.insertMediaItems(items.map { it.toEntity(null, userId) })
            items
        }
    }

    override suspend fun getStudios(
        userId: String,
        accessToken: String,
        libraryId: String,
        limit: Int,
        includeItemTypes: String
    ): NetworkResult<List<Studio>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                parentId = libraryId,
                includeItemTypes = includeItemTypes,
                recursive = true,
                limit = limit * 4,
                sortBy = "SortName",
                sortOrder = "Ascending",
                authorization = authHeader
            )

            val random = kotlin.random.Random(System.currentTimeMillis())
            val showsWithStudios = response.items.filter { it.studios.isNotEmpty() }
                .shuffled(random)

            val selectedStudios = mutableListOf<Studio>()
            val seenIds = mutableSetOf<String>()

            for (show in showsWithStudios) {
                val studio = show.studios
                    .asSequence()
                    .mapNotNull { dto -> mapToStudio(dto) }
                    .firstOrNull { candidate -> seenIds.add(candidate.id) }

                if (studio != null) {
                    selectedStudios += studio
                }

                if (selectedStudios.size >= limit) {
                    break
                }
            }

            selectedStudios
        }
    }

    override suspend fun getPlayedItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = listOf(
                    ApiConstants.ITEM_TYPE_MOVIE,
                    ApiConstants.ITEM_TYPE_SERIES,
                    ApiConstants.ITEM_TYPE_EPISODE
                ).joinToString(","),
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "DatePlayed",
                sortOrder = "Descending",
                isPlayed = true,
                enableImageTypes = "Primary,Backdrop,Thumb",
                fields = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,IndexNumber,UserData,SeriesName",
                authorization = authHeader
            )

            val items = response.items.map { dto ->
                mapToMediaItem(dto)
            }
            mediaItemDao.insertMediaItems(items.map { it.toEntity(null, userId) })
            items
        }
    }

    override suspend fun getItemsByCategory(
        userId: String,
        accessToken: String,
        filters: Map<String, String>,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection,
        startIndex: Int,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                parentId = filters["ParentId"],
                includeItemTypes = filters["IncludeItemTypes"],
                genres = filters["Genres"],
                studios = filters["Studios"],
                recursive = filters["Recursive"]?.toBoolean() ?: true,
                startIndex = startIndex,
                limit = limit,
                sortBy = sortBy.joinToString(","),
                sortOrder = sortOrder.toApiValue(),
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun searchItems(
        userId: User,
        accessToken: String,
        searchTerm: String,
        includeItemTypes: String?,
        startIndex: Int,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.searchItems(
                userId = userId.id,
                searchTerm = searchTerm,
                includeItemTypes = includeItemTypes,
                recursive = true,
                startIndex = startIndex,
                limit = limit,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    private fun mapToLibrary(dto: BaseItemDto): Library {
        return Library(
            id = dto.id,
            name = dto.name ?: "Unknown LibrarySection",
            type = LibraryType.fromString(dto.collectionType),
            itemCount = dto.childCount,
            primaryImageTag = dto.imageTags?.primary,
            isFolder = dto.isFolder
        )
    }

    private fun mapToStudio(dto: StudioDto): Studio? {
        val name = dto.name ?: return null
        val id = dto.id ?: name

        return Studio(
            id = id,
            name = name,
            imageTag = dto.imageTags?.thumb ?: dto.imageTags?.primary
        )
    }

    private fun mapToMediaItem(dto: BaseItemDto): MediaItem {
        return MediaItem(
            id = dto.id,
            name = dto.name ?: "Unknown",
            title = dto.title,
            type = dto.type ?: "Unknown",
            overview = dto.overview,
            year = dto.productionYear,
            communityRating = dto.communityRating,
            runTimeTicks = dto.runTimeTicks,
            primaryImageTag = dto.imageTags?.primary,
            thumbImageTag = dto.imageTags?.thumb,
            logoImageTag = dto.imageTags?.logo,
            backdropImageTags = dto.backdropImageTags,
            genres = dto.genres,
            isFolder = dto.isFolder,
            childCount = dto.childCount,
            userData = adjustUserData(mapToUserData(dto.userData), dto.runTimeTicks),
            taglines = dto.taglines,
            people = dto.people.map { personDto ->
                Person(
                    id = personDto.id,
                    name = personDto.name ?: "Unknown",
                    role = personDto.role,
                    type = personDto.type,
                    primaryImageTag = personDto.primaryImageTag
                )
            },
            mediaSources = playbackMapper.mapMediaSourceDtoListToMediaSourceList(dto.mediaSources),
            seriesName = dto.seriesName,
            seriesId = dto.seriesId,
            seriesPrimaryImageTag = dto.seriesPrimaryImageTag,
            seasonId = dto.seasonId,
            seasonName = dto.seasonName,
            seasonPrimaryImageTag = dto.seasonPrimaryImageTag,
            parentIndexNumber = dto.parentIndexNumber,
            indexNumber = dto.indexNumber,
            tvdbId = dto.providerIds.extractTvdbId()
        )
    }

    private fun mapToUserData(dto: UserDataDto?): UserData? {
        return dto?.let {
            UserData(
                isFavorite = it.isFavorite,
                playbackPositionTicks = it.playbackPositionTicks,
                playCount = it.playCount,
                played = it.played,
                lastPlayedDate = it.lastPlayedDate,
                isWatchlist = false,
                pendingFavorite = false,
                pendingPlayed = false,
                pendingWatchlist = false
            )
        }
    }

    private fun adjustUserData(userData: UserData?, runTimeTicks: Long?): UserData? {
        if (userData == null || runTimeTicks == null) return userData
        val adjustedPlayed = userData.playbackPositionTicks >= runTimeTicks - THRESHOLD
        return userData.copy(played = adjustedPlayed)
    }

    private fun LibraryEntity.toDomain(): Library {
        return Library(
            id = id,
            name = name,
            type = runCatching { LibraryType.valueOf(type) }.getOrDefault(LibraryType.UNKNOWN),
            itemCount = itemCount,
            primaryImageTag = primaryImageTag,
            isFolder = isFolder
        )
    }

    private fun Library.toEntity(userId: String): LibraryEntity {
        return LibraryEntity(
            id = id,
            name = name,
            type = type.name,
            itemCount = itemCount,
            primaryImageTag = primaryImageTag,
            isFolder = isFolder,
            userId = userId
        )
    }

    private fun MediaItemEntity.toDomain(): MediaItem {
        val userData = adjustUserData(
            UserData(
                isFavorite = isFavorite,
                playbackPositionTicks = playbackPositionTicks,
                playCount = playCount,
                played = played,
                lastPlayedDate = lastPlayedDate,
                isWatchlist = isWatchlist,
                pendingFavorite = pendingFavorite,
                pendingPlayed = pendingPlayed,
                pendingWatchlist = pendingWatchlist
            ),
            runTimeTicks
        )
        return MediaItem(
            id = id,
            name = name,
            title = title,
            type = type,
            overview = overview,
            year = year,
            communityRating = communityRating,
            runTimeTicks = runTimeTicks,
            primaryImageTag = primaryImageTag,
            thumbImageTag = thumbImageTag,
            logoImageTag = null,
            backdropImageTags = backdropImageTags,
            genres = genres,
            isFolder = isFolder,
            childCount = childCount,
            userData = userData,
            taglines = taglines,
            tvdbId = tvdbId
        )
    }

    private fun MediaItem.toEntity(libraryId: String?, userId: String): MediaItemEntity {
        val adjustedUserData = adjustUserData(userData, runTimeTicks)
        return MediaItemEntity(
            id = id,
            name = name,
            title = title,
            type = type,
            overview = overview,
            year = year,
            communityRating = communityRating,
            runTimeTicks = runTimeTicks,
            primaryImageTag = primaryImageTag,
            thumbImageTag = thumbImageTag,
            tvdbId = tvdbId,
            backdropImageTags = backdropImageTags,
            genres = genres,
            isFolder = isFolder,
            childCount = childCount,
            libraryId = libraryId,
            userId = userId,
            isFavorite = adjustedUserData?.isFavorite ?: false,
            playbackPositionTicks = adjustedUserData?.playbackPositionTicks ?: 0L,
            playCount = adjustedUserData?.playCount ?: 0,
            played = adjustedUserData?.played ?: false,
            lastPlayedDate = adjustedUserData?.lastPlayedDate,
            isWatchlist = adjustedUserData?.isWatchlist ?: false,
            pendingFavorite = adjustedUserData?.pendingFavorite ?: false,
            pendingPlayed = adjustedUserData?.pendingPlayed ?: false,
            pendingWatchlist = adjustedUserData?.pendingWatchlist ?: false,
            taglines = taglines
        )
    }

    private fun PendingActionEntity.toDomain(): PendingAction {
        return PendingAction(
            mediaId = mediaId,
            actionType = actionType,
            newValue = newValue,
            timestamp = timestamp
        )
    }

    private suspend fun getServerUrl(): String {
        val stored = preferencesManager.getServerUrl().first() ?: "http://localhost:8096"
        return serverConnectionManager.normalizeUrl(stored)
    }

    private suspend fun shouldUseLegacyPlaybackApi(): Boolean {
        return preferencesManager.getServerLegacyPlayback().first()
    }

    override suspend fun getMediaItemDetail(
        userId: String,
        mediaId: String,
        accessToken: String
    ): NetworkResult<MediaItem> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemById(
                userId = userId,
                itemId = mediaId,
                authorization = authHeader
            )

            mapToMediaItem(response)
        }
    }

    override suspend fun getMediaItemDetailLocal(
        userId: String,
        mediaId: String
    ): MediaItem? {
        val normalized = normalizeUuid(mediaId)
        mediaItemDao.getMediaItem(normalized, userId)?.toDomain()?.let { return it }
        return mediaItemDao.getMediaItem(mediaId, userId)?.toDomain()
    }

    override suspend fun getSimilarItems(
        mediaId: String,
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getSimilarItems(
                itemId = mediaId,
                userId = userId,
                limit = limit,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getSpecialFeatures(
        mediaId: String,
        userId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getSpecialFeatures(
                itemId = mediaId,
                userId = userId,
                authorization = authHeader
            )

            response.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getMediaCredits(
        userId: String,
        mediaId: String,
        accessToken: String
    ): NetworkResult<List<Person>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val credits = apiService.getItemCredits(
                itemId = mediaId,
                userId = userId,
                authorization = authHeader
            )

            credits.map { personDto ->
                Person(
                    id = personDto.id,
                    name = personDto.name ?: "Unknown",
                    role = personDto.role,
                    type = personDto.type,
                    primaryImageTag = personDto.primaryImageTag
                )
            }
        }
    }

    override suspend fun getRelatedResourcesBatch(
        mediaId: String,
        userId: String,
        accessToken: String
    ): NetworkResult<RelatedResources> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val similar = apiService.getSimilarItems(
                itemId = mediaId,
                userId = userId,
                limit = 20,
                authorization = authHeader
            ).items.map { dto ->
                mapToMediaItem(dto)
            }
            val special = apiService.getSpecialFeatures(
                itemId = mediaId,
                userId = userId,
                authorization = authHeader
            ).map { dto ->
                mapToMediaItem(dto)
            }
            RelatedResources(similar, special)
        }
    }

    override suspend fun getThemeSongs(
        mediaId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getThemeSongs(
                itemId = mediaId,
                inheritFromParent = true,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getThemeSongIds(
        mediaId: String,
        accessToken: String
    ): NetworkResult<List<String>> {
        return safeApiCall {
            val serverUrl = getServerUrl()
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getThemeMedia(
                itemId = mediaId,
                inheritFromParent = true,
                authorization = authHeader
            )

            response.themeSongsResult?.items?.map { it.id } ?: emptyList()
        }
    }

    override suspend fun getItemSegments(
        userId: String,
        mediaId: String,
        accessToken: String
    ): NetworkResult<List<Segment>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemSegments(
                itemId = mediaId,
                userId = userId,
                authorization = authHeader
            )

            playbackMapper.mapSegmentDtoListToSegmentList(response.items)
        }
    }

    override suspend fun updateFavoriteRemote(
        userId: String,
        mediaId: String,
        accessToken: String,
        isFavorite: Boolean
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        return enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (isFavorite) {
                apiService.markAsFavorite(userId, mediaId, authHeader)
            } else {
                apiService.removeFromFavorites(userId, mediaId, authHeader)
            }
        }.await()
    }

    override suspend fun updatePlayedRemote(
        userId: String,
        mediaId: String,
        accessToken: String,
        isPlayed: Boolean
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        val result = enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (isPlayed) {
                apiService.markAsPlayed(userId, mediaId, authHeader)
            } else {
                apiService.markAsUnplayed(userId, mediaId, authHeader)
            }
        }.await()
        if (result is Success) {
            scheduleNextUpRefresh(userId, accessToken, invalidate = true)
        }
        return result
    }

    override suspend fun toggleFavorite(
        userId: String,
        mediaId: String,
        accessToken: String,
        newFavorite: Boolean,
        mediaItem: MediaItem?
    ): NetworkResult<Unit> {
        val previousItem = mediaItemDao.getMediaItem(mediaId, userId)
        setFavoriteLocal(userId, mediaId, newFavorite, mediaItem)

        val serverUrl = getServerUrl()
        val networkResult = enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (newFavorite) {
                apiService.markAsFavorite(userId, mediaId, authHeader)
            } else {
                apiService.removeFromFavorites(userId, mediaId, authHeader)
            }
        }.await()

        return when (networkResult) {
            is Success -> {
                mediaItemDao.updateFavoriteStatus(mediaId, userId, newFavorite, false)
                pendingActionDao.deleteAction(mediaId, "favorite")
                Success(Unit)
            }

            is NetworkResult.Error -> {
                if (previousItem != null) {
                    mediaItemDao.updateMediaItem(previousItem)
                } else {
                    mediaItemDao.getMediaItem(mediaId, userId)?.let { mediaItemDao.deleteMediaItem(it) }
                }
                pendingActionDao.deleteAction(mediaId, "favorite")
                networkResult
            }

            is NetworkResult.Loading<*> -> TODO()
        }
    }

    override suspend fun markAsPlayed(
        userId: String,
        mediaId: String,
        accessToken: String,
        isPlayed: Boolean
    ): NetworkResult<Unit> {
        val previousItem = mediaItemDao.getMediaItem(mediaId, userId)
        setPlayedLocal(userId, mediaId, isPlayed)
        val serverUrl = getServerUrl()
        val apiService = createApiService(serverUrl)
        val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)

        return try {
            if (isPlayed) {
                apiService.markAsPlayed(userId, mediaId, authHeader)
            } else {
                apiService.markAsUnplayed(userId, mediaId, authHeader)
            }
            mediaItemDao.getMediaItem(mediaId, userId)?.let { current ->
                mediaItemDao.updateMediaItem(current.copy(pendingPlayed = false))
            }
            pendingActionDao.deleteAction(mediaId, "played")
            scheduleNextUpRefresh(userId, accessToken, invalidate = true)
            Success(Unit)
        } catch (e: Exception) {
            if (previousItem != null) {
                mediaItemDao.updateMediaItem(previousItem)
            } else {
                mediaItemDao.getMediaItem(mediaId, userId)?.let { mediaItemDao.deleteMediaItem(it) }
            }
            pendingActionDao.deleteAction(mediaId, "played")
            val message = e.localizedMessage ?: "Unknown error"
            NetworkResult.Error(AppError.Unknown(message), e)
        }
    }

    override suspend fun setFavoriteLocal(userId: String, mediaId: String, isFavorite: Boolean, mediaItem: MediaItem?) {
        val existingItem = mediaItemDao.getMediaItem(mediaId, userId)
        if (existingItem == null) {
            var item = mediaItem
            if (item == null) {
                val token = preferencesManager.getAccessToken().first()
                if (token != null) {
                    when (val result = getMediaItemDetail(userId, mediaId, token)) {
                        is Success -> item = result.data
                        else -> Unit
                    }
                }
            }

            val entity = item?.toEntity(null, userId) ?: MediaItemEntity(
                id = mediaId,
                name = "",
                type = "",
                overview = null,
                year = null,
                communityRating = null,
                runTimeTicks = null,
                primaryImageTag = null,
                backdropImageTags = emptyList(),
                genres = emptyList(),
                isFolder = false,
                childCount = null,
                libraryId = null,
                userId = userId,
                isFavorite = false,
                playbackPositionTicks = 0,
                playCount = 0,
                played = false,
                lastPlayedDate = null,
                isWatchlist = false,
                pendingFavorite = false,
                pendingPlayed = false,
                pendingWatchlist = false,
                taglines = emptyList()
            )
            mediaItemDao.insertMediaItem(entity)
        }
        mediaItemDao.updateFavoriteStatus(mediaId, userId, isFavorite, true)
        pendingActionDao.upsert(
            PendingActionEntity(
                mediaId = mediaId,
                actionType = "favorite",
                newValue = isFavorite,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun setPlayedLocal(userId: String, mediaId: String, isPlayed: Boolean) {
        val existingItem = mediaItemDao.getMediaItem(mediaId, userId)
        if (existingItem == null) {
            val placeholder = MediaItemEntity(
                id = mediaId,
                name = "",
                type = "",
                overview = null,
                year = null,
                communityRating = null,
                runTimeTicks = null,
                primaryImageTag = null,
                backdropImageTags = emptyList(),
                genres = emptyList(),
                isFolder = false,
                childCount = null,
                libraryId = null,
                userId = userId,
                isFavorite = false,
                playbackPositionTicks = 0,
                playCount = 0,
                played = false,
                lastPlayedDate = null,
                isWatchlist = false,
                pendingFavorite = false,
                pendingPlayed = false,
                pendingWatchlist = false,
                taglines = emptyList()
            )
            mediaItemDao.insertMediaItem(placeholder)
        }
        mediaItemDao.updatePlayedStatus(mediaId, userId, isPlayed, true)
        pendingActionDao.upsert(
            PendingActionEntity(
                mediaId = mediaId,
                actionType = "played",
                newValue = isPlayed,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun setWatchlistLocal(userId: String, mediaId: String, isWatchlist: Boolean) {
        val existingItem = mediaItemDao.getMediaItem(mediaId, userId)
        if (existingItem == null) {
            val placeholder = MediaItemEntity(
                id = mediaId,
                name = "",
                type = "",
                overview = null,
                year = null,
                communityRating = null,
                runTimeTicks = null,
                primaryImageTag = null,
                backdropImageTags = emptyList(),
                genres = emptyList(),
                isFolder = false,
                childCount = null,
                libraryId = null,
                userId = userId,
                isFavorite = false,
                playbackPositionTicks = 0,
                playCount = 0,
                played = false,
                lastPlayedDate = null,
                isWatchlist = false,
                pendingFavorite = false,
                pendingPlayed = false,
                pendingWatchlist = false,
                taglines = emptyList()
            )
            mediaItemDao.insertMediaItem(placeholder)
        }
        mediaItemDao.updateWatchlistStatus(mediaId, userId, isWatchlist, false)
    }

    override suspend fun getPendingActions(): List<PendingAction> {
        return pendingActionDao.getPendingActions().map { it.toDomain() }
    }

    override suspend fun getSeasons(
        userId: String,
        seriesId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getLibraryItems(
                userId = userId,
                parentId = seriesId,
                recursive = false,  
                startIndex = 0,
                limit = Int.MAX_VALUE,
                sortBy = "SortName",
                sortOrder = "Ascending",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
        }
    }

    override suspend fun getEpisodes(
        userId: String,
        seasonId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getLibraryItems(
                userId = userId,
                parentId = seasonId,
                recursive = false,  
                startIndex = 0,
                limit = Int.MAX_VALUE,
                sortBy = "SortName",
                sortOrder = "Ascending",
                fields = ApiConstants.FIELDS_BASIC,
                enableImageTypes = DEFAULT_MEDIA_IMAGE_TYPES,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
        }
    }

    override suspend fun getNextUpEpisodes(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        return if (nextUpStore.canServe(limit)) {
            Success(nextUpStore.snapshot(limit))
        } else {
            when (val result = fetchNextUpEpisodesRemote(userId, accessToken, limit)) {
                is Success -> {
                    nextUpStore.setInitial(result.data, limit)
                    result
                }

                else -> result
            }
        }
    }

    override suspend fun invalidateNextUp(limit: Int) {
        nextUpStore.invalidate()
    }

    override suspend fun getWatchHistory(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie,Episode",
                isPlayed = true,
                sortBy = "DatePlayed",
                sortOrder = "Descending",
                limit = limit,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    private suspend fun fetchNextUpEpisodesRemote(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getNextUpItems(
                userId = userId,
                limit = limit,
                authorization = authHeader
            )

            response.items.filter { it.type == "Episode" }.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    private suspend fun scheduleNextUpRefresh(
        userId: String,
        accessToken: String,
        limit: Int = DEFAULT_NEXT_UP_LIMIT,
        invalidate: Boolean = false
    ) {
        if (userId.isBlank() || accessToken.isBlank()) return
        if (invalidate) {
            nextUpStore.invalidate()
        }
        nextUpStore.requestRefresh(limit) { requestedLimit ->
            when (val result = fetchNextUpEpisodesRemote(userId, accessToken, requestedLimit)) {
                is Success -> result.data
                else -> null
            }
        }
    }

    override suspend fun getPlaybackPosition(
        userId: String,
        mediaId: String,
        accessToken: String
    ): NetworkResult<Long> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val userData = apiService.getItemUserData(
                itemId = mediaId,
                userId = userId,
                authorization = authHeader
            )
            userData.playbackPositionTicks
        }
    }

    private fun buildDirectStreamUrl(
        serverUrl: String,
        itemId: String,
        mediaSourceId: String,
        container: String?,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        accessToken: String
    ): String {
        val sanitizedBase = serverUrl.removeSuffix("/")
        val containerValue = container?.takeIf { it.isNotBlank() } ?: "mkv"
        return buildString {
            append(sanitizedBase)
            append("/Videos/")
            append(itemId)
            append("/stream")
            append("?static=true&container=")
            append(containerValue)
            append("&MediaSourceId=")
            append(mediaSourceId)
            audioStreamIndex?.let { append("&AudioStreamIndex=$it") }
            subtitleStreamIndex?.let { append("&SubtitleStreamIndex=$it") }
            append("&api_key=")
            append(accessToken)
        }
    }

    private fun mergeServerUrl(serverUrl: String, path: String): String {
        if (path.startsWith("http", ignoreCase = true)) return path
        val sanitizedBase = serverUrl.removeSuffix("/")
        val sanitizedPath = if (path.startsWith("/")) path else "/$path"
        return sanitizedBase + sanitizedPath
    }

    private fun ensureApiKey(url: String, accessToken: String): String {
        val containsToken = url.contains("api_key=", ignoreCase = true) ||
                url.contains("X-Emby-Token", ignoreCase = true) ||
                url.contains("Token=", ignoreCase = true)
        if (containsToken) return url
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}api_key=$accessToken"
    }

    private fun resolvePlaybackUrl(
        serverUrl: String,
        itemId: String,
        accessToken: String,
        mediaSource: PlaybackInfoMediaSourceDto,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?
    ): String {
        mediaSource.transcodingUrl?.takeIf { it.isNotBlank() }?.let { path ->
            val merged = mergeServerUrl(serverUrl, path)
            return ensureApiKey(merged, accessToken)
        }

        if (mediaSource.protocol?.equals("http", ignoreCase = true) == true &&
            !mediaSource.path.isNullOrBlank()
        ) {
            return mediaSource.path
        }

        if (!mediaSource.supportsDirectPlay && !mediaSource.supportsDirectStream) {
            throw IllegalStateException("Server did not return a direct-playable stream")
        }

        val container = mediaSource.container ?: "mkv"
        val sourceId = mediaSource.id ?: itemId
        return buildDirectStreamUrl(
            serverUrl = serverUrl,
            itemId = itemId,
            mediaSourceId = sourceId,
            container = container,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
            accessToken = accessToken
        )
    }

    override suspend fun requestDirectStreamUrl(
        itemId: String,
        userId: String,
        accessToken: String,
        mediaSourceId: String,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        startTimeTicks: Long?,
        maxStreamingBitrate: Int?,
        container: String?
    ): NetworkResult<PlaybackStreamInfo> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val containerValue = container?.takeIf { it.isNotBlank() }
                ?: "mkv,mp4,mov,ts,avi,webm"
            val request = PlaybackInfoRequestDto(
                mediaSourceId = mediaSourceId,
                maxStreamingBitrate = maxStreamingBitrate,
                enableDirectPlay = true,
                enableDirectStream = true,
                enableTranscoding = false,
                allowVideoStreamCopy = true,
                allowAudioStreamCopy = true,
                audioStreamIndex = audioStreamIndex,
                subtitleStreamIndex = subtitleStreamIndex,
                startTimeTicks = startTimeTicks,
                deviceProfile = PlaybackDeviceProfileDto(
                    name = "VoidDirectPlay",
                    maxStreamingBitrate = maxStreamingBitrate,
                    directPlayProfiles = listOf(
                        PlaybackDirectPlayProfileDto(
                            type = "Video",
                            container = containerValue,
                            audioCodec = "aac,flac,opus,vorbis,mp3,ac3,eac3,dts",
                            videoCodec = "h264,hevc,av1,vp9,mpeg4,mpeg2video"
                        )
                    ),
                    transcodingProfiles = emptyList(),
                    codecProfiles = emptyList(),
                    subtitleProfiles = listOf(
                        PlaybackSubtitleProfileDto(format = "srt", method = "External"),
                        PlaybackSubtitleProfileDto(format = "ass", method = "External"),
                        PlaybackSubtitleProfileDto(format = "ssa", method = "External"),
                        PlaybackSubtitleProfileDto(format = "pgs", method = "External")
                    )
                )
            )
            val response = apiService.getPlaybackInfo(
                itemId = itemId,
                userId = userId,
                request = request,
                authorization = authHeader
            )
            val mediaSource = response.mediaSources.firstOrNull { it.id == mediaSourceId }
                ?: response.mediaSources.firstOrNull()
                ?: throw IllegalStateException("No media source returned by server")
            val resolvedUrl = resolvePlaybackUrl(
                serverUrl = serverUrl,
                itemId = itemId,
                accessToken = accessToken,
                mediaSource = mediaSource,
                audioStreamIndex = audioStreamIndex,
                subtitleStreamIndex = subtitleStreamIndex
            )
            PlaybackStreamInfo(
                url = resolvedUrl,
                playSessionId = response.playSessionId
            )
        }
    }


    override suspend fun requestTranscodingUrl(
        itemId: String,
        userId: String,
        accessToken: String,
        mediaSourceId: String,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        startTimeTicks: Long?,
        maxStreamingBitrate: Int?,
        parameters: TranscodeRequestParameters,
    ): NetworkResult<PlaybackStreamInfo> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val defaultBitrate = parameters.videoBitrate
                ?: parameters.maxBitrate
                ?: maxStreamingBitrate
                ?: 8_000_000
            val effectiveBitrate = if (defaultBitrate > 0) defaultBitrate else 8_000_000
            val videoCodecValue = parameters.videoCodec ?: "h264,hevc,av1"
            val audioCodecValue = parameters.audioCodec ?: "aac"
            val request = PlaybackInfoRequestDto(
                mediaSourceId = mediaSourceId,
                maxStreamingBitrate = effectiveBitrate,
                enableDirectPlay = false,
                enableDirectStream = false,
                enableTranscoding = true,
                allowVideoStreamCopy = parameters.allowVideoStreamCopy,
                allowAudioStreamCopy = parameters.allowAudioStreamCopy,
                enableAutoStreamCopy = parameters.enableAutoStreamCopy,
                alwaysBurnInSubtitleWhenTranscoding = subtitleStreamIndex != null,
                audioStreamIndex = audioStreamIndex,
                subtitleStreamIndex = subtitleStreamIndex,
                startTimeTicks = startTimeTicks,
                deviceProfile = PlaybackDeviceProfileDto(
                    name = "VoidMpvProfile",
                    maxStreamingBitrate = effectiveBitrate,
                    transcodingProfiles = listOf(
                        PlaybackTranscodingProfileDto(
                            type = "Video",
                            videoCodec = videoCodecValue,
                            audioCodec = audioCodecValue,
                            profile = parameters.videoCodecProfile,
                            protocol = "hls",
                            context = "Streaming",
                            enableMpegtsM2TsMode = true,
                            transcodeSeekInfo = "Auto",
                            copyTimestamps = true,
                            enableSubtitlesInManifest = true,
                            enableAudioVbrEncoding = true,
                            breakOnNonKeyFrames = false,
                            maxAudioChannels = 6,
                            maxWidth = parameters.maxWidth,
                            maxHeight = parameters.maxHeight,
                            maxBitrate = parameters.maxBitrate ?: effectiveBitrate,
                            videoBitrate = parameters.videoBitrate,
                        )
                    ),
                    codecProfiles = listOf(
                        PlaybackCodecProfileDto(type = "Video", codec = "h264", container = "ts"),
                        PlaybackCodecProfileDto(type = "Video", codec = "hevc", container = "ts"),
                        PlaybackCodecProfileDto(type = "Video", codec = "av1", container = "ts"),
                    ),
                    subtitleProfiles = listOf(
                        PlaybackSubtitleProfileDto(format = "ass", method = "Encode")
                    )
                )
            )
            val response = apiService.getPlaybackInfo(
                itemId = itemId,
                userId = userId,
                request = request,
                authorization = authHeader
            )
            val mediaSource = response.mediaSources.firstOrNull { it.id == mediaSourceId }
                ?: response.mediaSources.firstOrNull()
                ?: throw IllegalStateException("No media source returned by server")
            val transcodingSource = response.mediaSources.firstOrNull { source ->
                source.transcodingUrl?.contains("m3u8", ignoreCase = true) == true
            } ?: mediaSource
            val transcodingUrl = transcodingSource.transcodingUrl
                ?: throw IllegalStateException("Transcoding URL missing or invalid")
            if (!transcodingUrl.contains("m3u8", ignoreCase = true)) {
                throw IllegalStateException("Transcoding URL is not HLS (missing m3u8)")
            }
            PlaybackStreamInfo(
                url = ensureApiKey(mergeServerUrl(serverUrl, transcodingUrl), accessToken),
                playSessionId = response.playSessionId,
            )
        }
    }


    override suspend fun requestPlaybackInfo(
        itemId: String,
        userId: String,
        accessToken: String,
        mediaSourceId: String,
        directPlayEnabled: Boolean,
        videoCodec: String?,
        videoRange: String?,
        videoRangeType: String?,
        profile: String?,
        bitDepth: Int?,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        startTimeTicks: Long?,
        transcodeOption: PlaybackTranscodeOption,
    ): NetworkResult<PlaybackStreamInfo> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)

            
            val codecSupported = if (videoCodec.isNullOrBlank()) {
                true 
            } else {
                CodecDetector.isVideoCodecSupported(videoCodec, videoRange, videoRangeType, profile, bitDepth)
            }

            
            
            
            val useDirectPlayStrategy = directPlayEnabled || codecSupported

            
            val strategyName = if (useDirectPlayStrategy) "Universal Support (Direct Play)" else "Smart Transcoding"
            val reason = when {
                directPlayEnabled -> "Direct Play is enabled (no codec check)"
                codecSupported -> "Direct Play is off, but codec '$videoCodec' is supported"
                else -> "Direct Play is off, codec '$videoCodec' is NOT supported (transcode required)"
            }
            Log.i(TAG, "Playback strategy: $strategyName | Reason: $reason | Transcode Option: ${transcodeOption.label}")

            
            val request = if (useDirectPlayStrategy) {
                
                Log.d(TAG, "Building Universal Support DeviceProfile (wide codec support)")
                PlaybackInfoRequestDto(
                    mediaSourceId = mediaSourceId,
                    maxStreamingBitrate = Int.MAX_VALUE, 
                    enableDirectPlay = true,
                    enableDirectStream = true,
                    enableTranscoding = false,
                    allowVideoStreamCopy = true,
                    allowAudioStreamCopy = true,
                    audioStreamIndex = audioStreamIndex,
                    subtitleStreamIndex = subtitleStreamIndex,
                    startTimeTicks = startTimeTicks,
                    deviceProfile = PlaybackDeviceProfileDto(
                        name = "VoidDirectPlay",
                        maxStreamingBitrate = Int.MAX_VALUE, 
                        directPlayProfiles = listOf(
                            PlaybackDirectPlayProfileDto(
                                type = "Video",
                                container = "mkv,mp4,mov,ts,avi,webm,flv,asf,wmv,m4v,3gp,ogv",
                                audioCodec = "aac,mp3,ac3,eac3,dts,truehd,flac,opus,vorbis,pcm,dca,mlp,wma,wmapro,wmavoice",
                                videoCodec = "h264,hevc,av1,vp9,vp8,mpeg4,mpeg2video,vc1,wmv3,msmpeg4v3,theora"
                            )
                        ),
                        transcodingProfiles = emptyList(),
                        codecProfiles = emptyList(),
                        subtitleProfiles = listOf(
                            PlaybackSubtitleProfileDto(format = "srt", method = "External"),
                            PlaybackSubtitleProfileDto(format = "ass", method = "External"),
                            PlaybackSubtitleProfileDto(format = "ssa", method = "External"),
                            PlaybackSubtitleProfileDto(format = "vtt", method = "External"),
                            PlaybackSubtitleProfileDto(format = "sub", method = "External"),
                            PlaybackSubtitleProfileDto(format = "pgs", method = "External"),
                            PlaybackSubtitleProfileDto(format = "pgssub", method = "External"),
                            PlaybackSubtitleProfileDto(format = "dvdsub", method = "External")
                        )
                    )
                )
            } else {
                
                val supportedVideoCodecs = CodecDetector.getSupportedVideoCodecsForJellyfin()
                Log.d(TAG, "Building Smart Transcoding DeviceProfile | Supported codecs: $supportedVideoCodecs | Option: ${transcodeOption.label}")

                
                val parameters = transcodeOption.resolveParameters(
                    videoCodecOverride = supportedVideoCodecs,
                    audioCodecOverride = "aac,mp3,ac3,eac3,dts,truehd,flac,opus,vorbis,pcm,dca,mlp",
                    allowAudioStreamCopyOverride = true 
                )

                
                val effectiveBitrate = when {
                    parameters.maxBitrate != null -> parameters.maxBitrate
                    parameters.videoBitrate != null -> parameters.videoBitrate
                    else -> null 
                }

                Log.d(TAG, "Transcoding parameters: videoCodec=${parameters.videoCodec}, " +
                        "maxBitrate=$effectiveBitrate, maxWidth=${parameters.maxWidth}, maxHeight=${parameters.maxHeight}")

                PlaybackInfoRequestDto(
                    mediaSourceId = mediaSourceId,
                    maxStreamingBitrate = effectiveBitrate,
                    enableDirectPlay = false,
                    enableDirectStream = false,
                    enableTranscoding = true,
                    allowVideoStreamCopy = false, 
                    allowAudioStreamCopy = true,  
                    enableAutoStreamCopy = false,
                    alwaysBurnInSubtitleWhenTranscoding = true, 
                    audioStreamIndex = audioStreamIndex,
                    subtitleStreamIndex = subtitleStreamIndex,
                    startTimeTicks = startTimeTicks,
                    deviceProfile = PlaybackDeviceProfileDto(
                        name = "VoidSmartTranscode",
                        maxStreamingBitrate = effectiveBitrate,
                        transcodingProfiles = listOf(
                            PlaybackTranscodingProfileDto(
                                type = "Video",
                                videoCodec = parameters.videoCodec ?: supportedVideoCodecs,
                                audioCodec = parameters.audioCodec,
                                profile = parameters.videoCodecProfile,
                                protocol = "hls",
                                context = "Streaming",
                                enableMpegtsM2TsMode = true,
                                transcodeSeekInfo = "Auto",
                                copyTimestamps = true,
                                enableSubtitlesInManifest = false, 
                                enableAudioVbrEncoding = true,
                                breakOnNonKeyFrames = false,
                                maxAudioChannels = null, 
                                maxWidth = parameters.maxWidth,
                                maxHeight = parameters.maxHeight,
                                maxBitrate = parameters.maxBitrate,
                                videoBitrate = parameters.videoBitrate,
                            )
                        ),
                        codecProfiles = buildList {
                            if (supportedVideoCodecs.contains("h264")) {
                                add(PlaybackCodecProfileDto(type = "Video", codec = "h264", container = "ts"))
                            }
                            if (supportedVideoCodecs.contains("hevc")) {
                                add(PlaybackCodecProfileDto(type = "Video", codec = "hevc", container = "ts"))
                            }
                            if (supportedVideoCodecs.contains("av1")) {
                                add(PlaybackCodecProfileDto(type = "Video", codec = "av1", container = "ts"))
                            }
                        },
                        subtitleProfiles = listOf(
                            
                            PlaybackSubtitleProfileDto(format = "ass", method = "Encode"),
                            PlaybackSubtitleProfileDto(format = "ssa", method = "Encode"),
                            PlaybackSubtitleProfileDto(format = "srt", method = "Encode"),
                            PlaybackSubtitleProfileDto(format = "vtt", method = "Encode"),
                            PlaybackSubtitleProfileDto(format = "sub", method = "Encode"),
                            PlaybackSubtitleProfileDto(format = "pgs", method = "Encode"),
                            PlaybackSubtitleProfileDto(format = "pgssub", method = "Encode"),
                            PlaybackSubtitleProfileDto(format = "dvdsub", method = "Encode")
                        )
                    )
                )
            }

            
            Log.d(TAG, "Sending PlaybackInfo request: enableDirectPlay=${request.enableDirectPlay}, " +
                    "enableTranscoding=${request.enableTranscoding}, mediaSourceId=$mediaSourceId")

            val response = apiService.getPlaybackInfo(
                itemId = itemId,
                userId = userId,
                request = request,
                authorization = authHeader
            )

            
            val mediaSource = response.mediaSources.firstOrNull { it.id == mediaSourceId }
                ?: response.mediaSources.firstOrNull()
                ?: throw IllegalStateException("No media source returned by server")

            val resolvedUrl = if (useDirectPlayStrategy) {
                
                Log.d(TAG, "Resolving direct play/stream URL for media source: ${mediaSource.id}")
                resolvePlaybackUrl(
                    serverUrl = serverUrl,
                    itemId = itemId,
                    accessToken = accessToken,
                    mediaSource = mediaSource,
                    audioStreamIndex = audioStreamIndex,
                    subtitleStreamIndex = subtitleStreamIndex
                )
            } else {
                
                val transcodingSource = response.mediaSources.firstOrNull { source ->
                    source.transcodingUrl?.contains("m3u8", ignoreCase = true) == true
                } ?: mediaSource

                val transcodingUrl = transcodingSource.transcodingUrl
                    ?: throw IllegalStateException("Transcoding URL missing - server may not support requested codecs")

                if (!transcodingUrl.contains("m3u8", ignoreCase = true)) {
                    throw IllegalStateException("Transcoding URL is not HLS (missing m3u8)")
                }

                Log.d(TAG, "Resolved transcoding URL: $transcodingUrl")
                ensureApiKey(mergeServerUrl(serverUrl, transcodingUrl), accessToken)
            }

            Log.i(TAG, "Playback URL resolved successfully | Strategy: $strategyName | Session: ${response.playSessionId}")
            PlaybackStreamInfo(
                url = resolvedUrl,
                playSessionId = response.playSessionId
            )
        }
    }

    override suspend fun reportPlaybackStart(
        mediaId: String,
        userId: String,
        accessToken: String,
        playSessionId: String?
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        return enqueue(PriorityDispatcher.Priority.HIGH, serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (shouldUseLegacyPlaybackApi()) {
                apiService.reportLegacyPlaybackStart(
                    itemId = mediaId,
                    userId = userId,
                    canSeek = true,
                    playSessionId = playSessionId,
                    authorization = authHeader
                )
            } else {
                val body = PlaybackStartRequest(
                    itemId = mediaId,
                    canSeek = true,
                    playSessionId = playSessionId,
                )
                apiService.reportPlaybackStart(body, authHeader)
            }
        }.await()
    }

    override suspend fun reportPlaybackProgress(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionTicks: Long,
        playSessionId: String?
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        return enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (shouldUseLegacyPlaybackApi()) {
                apiService.reportLegacyPlaybackProgress(
                    itemId = mediaId,
                    userId = userId,
                    positionTicks = positionTicks,
                    playSessionId = playSessionId,
                    isPaused = false,
                    authorization = authHeader
                )
            } else {
                val body = PlaybackProgressRequest(
                    itemId = mediaId,
                    positionTicks = positionTicks,
                    isPaused = false,
                    playSessionId = playSessionId,
                )
                apiService.reportPlaybackProgress(body, authHeader)
            }
        }.await()
    }

    override suspend fun reportPlaybackStop(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionTicks: Long,
        playSessionId: String?
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        return enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (shouldUseLegacyPlaybackApi()) {
                apiService.reportLegacyPlaybackStop(
                    itemId = mediaId,
                    userId = userId,
                    positionTicks = positionTicks,
                    playSessionId = playSessionId,
                    authorization = authHeader
                )
            } else {
                val body = PlaybackStopRequest(
                    itemId = mediaId,
                    positionTicks = positionTicks,
                    playSessionId = playSessionId,
                )
                apiService.reportPlaybackStop(body, authHeader)
            }
        }.await()
    }

}
