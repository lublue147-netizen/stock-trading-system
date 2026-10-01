package com.stockmarket.app.ui.screens.sector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockmarket.app.data.model.CandlePoint
import com.stockmarket.app.data.model.StockQuote
import com.stockmarket.app.data.model.ThematicStockItem
import com.stockmarket.app.data.repository.StockRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    private val _uiState: MutableStateFlow<SectorDetailUiState>
    val uiState: StateFlow<SectorDetailUiState>

    private var refreshJob: Job? = null

    init {
        val initialCached = repository.getCachedSectorDetail(bkCode)
        if (initialCached != null) {
            val name = if (initialCached.quote.name.isNotEmpty() && initialCached.quote.name != bkCode) initialCached.quote.name else sectorName
            _uiState = MutableStateFlow(
                SectorDetailUiState(
                    bkCode = bkCode,
                    sectorName = name,
                    isLoading = false,
                    quote = initialCached.quote,
                    constituents = initialCached.constituents,
                    totalConstituents = initialCached.totalCount,
                    isWatchlisted = repository.isWatchlisted(bkCode)
                )
            )
        } else {
            _uiState = MutableStateFlow(
                SectorDetailUiState(
                    bkCode = bkCode,
                    sectorName = sectorName,
                    isLoading = true,
                    isWatchlisted = repository.isWatchlisted(bkCode)
                )
            )
        }
        uiState = _uiState.asStateFlow()

        loadData(isInitial = initialCached == null)
        startAutoRefresh()
    }

    fun loadData(isInitial: Boolean = false) {
        viewModelScope.launch {
            if (isInitial) {
                // Check if newly cached
                val cached = repository.getCachedSectorDetail(bkCode)
                if (cached != null) {
                    val name = if (cached.quote.name.isNotEmpty() && cached.quote.name != bkCode) cached.quote.name else sectorName
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            sectorName = name,
                            quote = cached.quote,
                            constituents = cached.constituents,
                            totalConstituents = cached.totalCount
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }
            } else {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            }

            try {
                coroutineScope {
                    // Fetch Sector Quote + Constituents AND Intraday Trends in parallel!
                    val detailDeferred = async { repository.getSectorDetail(bkCode) }
                    val trendsDeferred = async { repository.getHistoricalData(bkCode, "1d") }

                    // As soon as sector constituents arrive, IMMEDIATELY update UI and clear loading spinner!
                    val detailRes = detailDeferred.await()
                    val detail = detailRes.getOrNull()
                    if (detail != null) {
                        val q = detail.quote
                        val name = if (q.name.isNotEmpty() && q.name != bkCode) q.name else sectorName
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                                sectorName = name,
                                quote = q,
                                constituents = detail.constituents,
                                totalConstituents = detail.totalCount,
                                isWatchlisted = repository.isWatchlisted(bkCode)
                            )
                        }
                    }

                    // Update trend candles when ready
                    val trendsRes = trendsDeferred.await()
                    val trend = trendsRes.getOrNull()?.candles
                    if (trend != null) {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                                trendCandles = trend
                            )
                        }
                    } else {
                        _uiState.update { state ->
                            state.copy(isLoading = false, isRefreshing = false)
                        }
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
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
