package com.hritwik.avoid.presentation.viewmodel.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.usecase.library.GetCollectionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val getCollectionsUseCase: GetCollectionsUseCase
) : ViewModel() {

    private val itemsPerPage = 60

    fun collectionsPager(
        userId: String,
        accessToken: String,
        tags: List<String>?
    ): Flow<PagingData<MediaItem>> =
        Pager(
            PagingConfig(
                pageSize = itemsPerPage,
                initialLoadSize = itemsPerPage,
                enablePlaceholders = false
            )
        ) {
            CollectionsPagingSource(
                getCollectionsUseCase = getCollectionsUseCase,
                userId = userId,
                accessToken = accessToken,
                tags = tags
            )
        }.flow.cachedIn(viewModelScope)
}
