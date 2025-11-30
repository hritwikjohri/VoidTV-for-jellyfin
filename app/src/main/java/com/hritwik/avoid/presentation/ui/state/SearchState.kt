package com.hritwik.avoid.presentation.ui.state

enum class SearchCategory {
    TopResults,
    Movies,
    Shows,
    Episodes
}

data class SearchState(
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val suggestionsError: String? = null,
    val error: String? = null,
    val isSearchActive: Boolean = false,
    val selectedCategory: SearchCategory = SearchCategory.TopResults
)