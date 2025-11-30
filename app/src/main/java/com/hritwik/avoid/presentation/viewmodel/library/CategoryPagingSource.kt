package com.hritwik.avoid.presentation.viewmodel.library

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.usecase.search.GetItemsByCategoryUseCase

class CategoryPagingSource(
    private val getItemsByCategoryUseCase: GetItemsByCategoryUseCase,
    private val userId: String,
    private val accessToken: String,
    private val filters: Map<String, String>,
    private val sortBy: List<String>,
    private val sortOrder: LibrarySortDirection
) : PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val page = params.key ?: 0
        val startIndex = page * params.loadSize
        return when (val result = getItemsByCategoryUseCase(
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
                val nextKey = if (result.data.isEmpty()) null else page + 1
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

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { pos ->
            state.closestPageToPosition(pos)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(pos)?.nextKey?.minus(1)
        }
    }
}
