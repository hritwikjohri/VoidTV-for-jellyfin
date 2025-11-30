package com.hritwik.avoid.presentation.viewmodel.library

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.usecase.library.GetLibraryItemsUseCase
import com.hritwik.avoid.domain.usecase.search.GetItemsByCategoryUseCase

class LibraryItemsPagingSource(
    private val getLibraryItemsUseCase: GetLibraryItemsUseCase,
    private val getItemsByCategoryUseCase: GetItemsByCategoryUseCase?,
    private val userId: String,
    private val libraryId: String,
    private val libraryIds: List<String>,
    private val accessToken: String,
    private val sortBy: List<String>,
    private val sortOrder: LibrarySortDirection,
    private val genre: String?,
    private val studio: String?,
    private val includeItemTypes: String?
) : PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val page = params.key ?: 0
        val startIndex = page * params.loadSize
        val shouldUseCategory = libraryIds.size > 1 && !includeItemTypes.isNullOrBlank() && getItemsByCategoryUseCase != null
        return if (shouldUseCategory) {
            val filters = mutableMapOf(
                "IncludeItemTypes" to includeItemTypes!!,
                "Recursive" to "true"
            )
            genre?.let { filters["Genres"] = it }
            studio?.let { filters["Studios"] = it }

            val categoryUseCase = getItemsByCategoryUseCase!!
            when (val result = categoryUseCase(
                GetItemsByCategoryUseCase.Params(
                    userId = userId,
                    accessToken = accessToken,
                    filters = filters,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                    startIndex = startIndex,
                    limit = params.loadSize
                )
            )) {
                is NetworkResult.Success -> {
                    
                    val nextKey = if (result.data.size < params.loadSize) null else page + 1
                    LoadResult.Page(
                        data = result.data,
                        prevKey = if (page == 0) null else page - 1,
                        nextKey = nextKey
                    )
                }
                is NetworkResult.Error -> LoadResult.Error(Throwable(result.message))
                is NetworkResult.Loading -> LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
            }
        } else {
            when (val result = getLibraryItemsUseCase(
                GetLibraryItemsUseCase.Params(
                    userId = userId,
                    libraryId = libraryIds.firstOrNull() ?: libraryId,
                    accessToken = accessToken,
                    startIndex = startIndex,
                    limit = params.loadSize,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                    genre = genre,
                    studio = studio
                )
            )) {
                is NetworkResult.Success -> {
                    
                    val nextKey = if (result.data.size < params.loadSize) null else page + 1
                    LoadResult.Page(
                        data = result.data,
                        prevKey = if (page == 0) null else page - 1,
                        nextKey = nextKey
                    )
                }
                is NetworkResult.Error -> LoadResult.Error(Throwable(result.message))
                is NetworkResult.Loading -> LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(position)?.nextKey?.minus(1)
        }
    }
}
