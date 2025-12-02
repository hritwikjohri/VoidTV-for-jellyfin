package com.hritwik.avoid.presentation.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.domain.model.auth.AuthSession
import com.hritwik.avoid.domain.model.auth.User
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.provider.AuthSessionProvider
import com.hritwik.avoid.domain.usecase.search.GetSearchSuggestionsUseCase
import com.hritwik.avoid.domain.usecase.search.SearchItemsUseCase
import com.hritwik.avoid.presentation.ui.state.SearchCategory
import com.hritwik.avoid.presentation.ui.state.SearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchItemsUseCase: SearchItemsUseCase,
    private val authSessionProvider: AuthSessionProvider,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(SearchCategory.TopResults)
    val selectedCategory: StateFlow<SearchCategory> = _selectedCategory.asStateFlow()
    private var lastSearchedQuery: String = ""
    private var authSession: AuthSession? = null

    private val searchDebounceMs = 300L

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults = combine(_searchQuery, _selectedCategory) { query, category ->
        query to category
    }
        .debounce(searchDebounceMs)
        .filter { (query, _) -> query.isNotBlank() && query.length >= 2 }
        .distinctUntilChanged()
        .flatMapLatest { (query, category) ->
            val session = authSession ?: return@flatMapLatest emptyFlow()
            Pager(PagingConfig(pageSize = 50)) {
                SearchPagingSource(
                    searchItemsUseCase = searchItemsUseCase,
                    userId = session.userId,
                    accessToken = session.accessToken,
                    query = query,
                    itemTypes = category.toItemTypes()
                )
            }.flow
        }
        .cachedIn(viewModelScope)

    init {
        authSessionProvider.authSession
            .onEach { session ->
                authSession = session
            }
            .launchIn(viewModelScope)

        preferencesManager.getRecentSearches()
            .onEach { searches ->
                _searchState.value = _searchState.value.copy(recentSearches = searches)
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _searchState.value = _searchState.value.copy(
            searchQuery = query,
            isSearchActive = query.length >= 2,
            suggestions = if (query.isBlank()) emptyList() else _searchState.value.suggestions,
            suggestionsError = null
        )
        if (query.isBlank()) {
            lastSearchedQuery = ""
        }
    }

    fun fetchSearchSuggestions(query: String, userId: String, accessToken: String, limit: Int = 10) {
        if (query.length < 2) {
            _searchState.value = _searchState.value.copy(suggestions = emptyList(), suggestionsError = null)
            return
        }
        viewModelScope.launch {
            when (val result = getSearchSuggestionsUseCase(
                GetSearchSuggestionsUseCase.Params(
                    userId = userId,
                    accessToken = accessToken,
                    query = query,
                    limit = limit
                )
            )) {
                is NetworkResult.Success -> {
                    _searchState.value = _searchState.value.copy(
                        suggestions = result.data,
                        suggestionsError = null
                    )
                }
                is NetworkResult.Error -> {
                    _searchState.value = _searchState.value.copy(
                        suggestions = emptyList(),
                        suggestionsError = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun performImmediateSearch(query: String) {
        _searchQuery.value = query
        _searchState.value = _searchState.value.copy(
            searchQuery = query,
            isSearchActive = query.length >= 2,
            suggestions = emptyList(),
            suggestionsError = null
        )
        val trimmed = query.trim()
        if (trimmed.length >= 3 && trimmed != lastSearchedQuery) {
            addToRecentSearches(trimmed)
            lastSearchedQuery = trimmed
        }
    }

    fun updateSelectedCategory(category: SearchCategory) {
        _selectedCategory.value = category
        _searchState.value = _searchState.value.copy(selectedCategory = category)
        if (_searchQuery.value.isNotBlank()) {
            _searchQuery.value = _searchQuery.value
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        lastSearchedQuery = ""
        _searchState.value = _searchState.value.copy(
            searchQuery = "",
            suggestions = emptyList(),
            suggestionsError = null,
            isSearchActive = false,
            error = null
        )
    }

    fun dismissSearch() {
        _searchQuery.value = ""
        lastSearchedQuery = ""
        _searchState.value = SearchState(
            recentSearches = _searchState.value.recentSearches
        )
    }

    fun clearError() {
        _searchState.value = _searchState.value.copy(error = null)
    }

    fun selectRecentSearch(query: String) {
        updateSearchQuery(query)
        performImmediateSearch(query)
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            preferencesManager.clearRecentSearches()
        }
        _searchState.value = _searchState.value.copy(recentSearches = emptyList())
    }

    fun deactivateSearch() {
        if (_searchState.value.searchQuery.isBlank()) {
            _searchState.value = _searchState.value.copy(
                isSearchActive = false,
                error = null
            )
        }
    }

    fun activateSearch() {
        if (_searchState.value.searchQuery.isNotBlank()) {
            _searchState.value = _searchState.value.copy(isSearchActive = true)
        }
    }

    private fun addToRecentSearches(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 3) return

        val currentSearches = _searchState.value.recentSearches.toMutableList()
        currentSearches.removeAll { it.equals(trimmedQuery, ignoreCase = true) }
        currentSearches.add(0, trimmedQuery)
        if (currentSearches.size > 24) {
            currentSearches.subList(24, currentSearches.size).clear()
        }
        _searchState.value = _searchState.value.copy(recentSearches = currentSearches)
        viewModelScope.launch {
            preferencesManager.saveRecentSearches(currentSearches)
        }
    }
}

private class SearchPagingSource(
    private val searchItemsUseCase: SearchItemsUseCase,
    private val userId: User,
    private val accessToken: String,
    private val query: String,
    private val itemTypes: List<String>
) : androidx.paging.PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val page = params.key ?: 0
        val startIndex = page * params.loadSize
        return when (val result = searchItemsUseCase(
            SearchItemsUseCase.Params(
                userId = userId,
                accessToken = accessToken,
                searchTerm = query,
                includeItemTypes = itemTypes,
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

    override fun getRefreshKey(state: androidx.paging.PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(position)?.nextKey?.minus(1)
        }
    }
}

private fun SearchCategory.toItemTypes(): List<String> = when (this) {
    SearchCategory.TopResults -> listOf(
        "Movie",
        "Series"
    )
    SearchCategory.Movies -> listOf("Movie")
    SearchCategory.Shows -> listOf("Series")
    SearchCategory.Episodes -> listOf("Episode")
}
