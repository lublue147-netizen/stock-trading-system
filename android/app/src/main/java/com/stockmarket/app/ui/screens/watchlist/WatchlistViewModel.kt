package com.stockmarket.app.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockmarket.app.data.model.MarketIndex
import com.stockmarket.app.data.model.StockQuote
import com.stockmarket.app.data.repository.StockRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WatchlistUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val quotes: List<StockQuote> = emptyList(),
    val indices: List<MarketIndex> = emptyList(),
    val errorMessage: String? = null
)

class WatchlistViewModel(
    private val repository: StockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState(isLoading = true))
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        loadData(isInitial = true)
        startAutoRefresh()
    }

    fun loadData(isInitial: Boolean = false) {
        viewModelScope.launch {
            if (isInitial) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            }

            val quotesResult = repository.getWatchlistQuotes()
            val indicesResult = repository.getMarketIndices()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    quotes = quotesResult.getOrDefault(emptyList()),
                    indices = indicesResult.getOrDefault(emptyList()),
                    errorMessage = null
                )
            }
        }
    }

    fun removeSymbol(symbol: String) {
        repository.toggleWatchlist(symbol)
        loadData(isInitial = false)
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(20_000) // auto-refresh every 20s
                loadData(isInitial = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
