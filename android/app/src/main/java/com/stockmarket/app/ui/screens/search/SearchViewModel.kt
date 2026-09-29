package com.stockmarket.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockmarket.app.data.model.SearchResult
import com.stockmarket.app.data.repository.StockRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val trendingStocks: List<SearchResult> = StockRepository.FALLBACK_SEARCH,
    val watchlistedSymbols: Set<String> = emptySet()
)

class SearchViewModel(
    private val repository: StockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        updateWatchlistState()
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }

        searchJob?.cancel()
        if (newQuery.trim().isEmpty()) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _uiState.update { it.copy(isSearching = true) }
            val res = repository.searchStocks(newQuery)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    results = res.getOrDefault(emptyList())
                )
            }
        }
    }

    fun toggleWatchlist(symbol: String) {
        repository.toggleWatchlist(symbol)
        updateWatchlistState()
    }

    fun isWatchlisted(symbol: String): Boolean {
        return repository.isWatchlisted(symbol)
    }

    private fun updateWatchlistState() {
        val symbols = StockRepository.FALLBACK_SEARCH.map { it.symbol }
            .filter { repository.isWatchlisted(it) }
            .toSet()
        _uiState.update { it.copy(watchlistedSymbols = symbols) }
    }
}
