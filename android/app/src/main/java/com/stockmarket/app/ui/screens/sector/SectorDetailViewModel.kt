package com.stockmarket.app.ui.screens.sector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockmarket.app.data.model.CandlePoint
import com.stockmarket.app.data.model.StockQuote
import com.stockmarket.app.data.model.ThematicStockItem
import com.stockmarket.app.data.repository.StockRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SectorDetailUiState(
    val bkCode: String,
    val sectorName: String,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val quote: StockQuote? = null,
    val trendCandles: List<CandlePoint> = emptyList(),
    val constituents: List<ThematicStockItem> = emptyList(),
    val totalConstituents: Int = 0,
    val isWatchlisted: Boolean = false,
    val errorMessage: String? = null
)

class SectorDetailViewModel(
    val bkCode: String,
    val sectorName: String,
    private val repository: StockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SectorDetailUiState(
            bkCode = bkCode,
            sectorName = sectorName,
            isLoading = true,
            isWatchlisted = repository.isWatchlisted(bkCode)
        )
    )
    val uiState: StateFlow<SectorDetailUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

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

            // 1. Fetch Sector Quote and Constituents
            val detailRes = repository.getSectorDetail(bkCode)
            // 2. Fetch Sector Intraday Trends (09:15 - 15:00)
            val trendsRes = repository.getHistoricalData(bkCode, "1d")

            _uiState.update { state ->
                val detail = detailRes.getOrNull()
                val q = detail?.quote ?: state.quote
                val name = if (q?.name?.isNotEmpty() == true && q.name != bkCode) q.name else state.sectorName
                val trend = trendsRes.getOrNull()?.candles ?: state.trendCandles

                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    sectorName = name,
                    quote = q,
                    trendCandles = trend,
                    constituents = detail?.constituents ?: state.constituents,
                    totalConstituents = detail?.totalCount ?: state.totalConstituents,
                    isWatchlisted = repository.isWatchlisted(bkCode)
                )
            }
        }
    }

    fun toggleWatchlist() {
        val nowWatchlisted = repository.toggleWatchlist(bkCode)
        _uiState.update { it.copy(isWatchlisted = nowWatchlisted) }
    }

    fun toggleStockWatchlist(symbol: String): Boolean {
        return repository.toggleWatchlist(symbol)
    }

    fun isStockWatchlisted(symbol: String): Boolean {
        return repository.isWatchlisted(symbol)
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                loadData(isInitial = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
