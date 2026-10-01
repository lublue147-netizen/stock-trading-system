package com.stockmarket.app.ui.screens.sector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockmarket.app.data.model.CandlePoint
import com.stockmarket.app.data.model.StockQuote
import com.stockmarket.app.data.model.ThematicStockItem
import com.stockmarket.app.data.repository.StockRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

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
    private var isFetching = false

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
        if (isFetching) return
        isFetching = true

        viewModelScope.launch {
            if (isInitial) {
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
                supervisorScope {
                    // 1. Fetch Sector Quote + Constituents
                    launch {
                        try {
                            val detailRes = repository.getSectorDetail(bkCode)
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
                                        isWatchlisted = repository.isWatchlisted(bkCode),
                                        errorMessage = if (detail.constituents.isEmpty()) "未获取到该板块成分股数据，请下拉刷新" else null
                                    )
                                }
                            } else {
                                _uiState.update { state ->
                                    state.copy(
                                        isLoading = false,
                                        isRefreshing = false,
                                        errorMessage = "加载板块详情失败，请重试"
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    errorMessage = "网络连接异常: ${e.localizedMessage ?: "请重试"}"
                                )
                            }
                        }
                    }

                    // 2. Fetch Intraday Trends in parallel (independent from constituents)
                    launch {
                        try {
                            val trendsRes = repository.getHistoricalData(bkCode, "1d")
                            val trend = trendsRes.getOrNull()?.candles
                            if (trend != null) {
                                _uiState.update { state ->
                                    state.copy(trendCandles = trend)
                                }
                            }
                        } catch (_: Exception) {
                            // Trend error will not affect constituents
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = e.localizedMessage) }
            } finally {
                isFetching = false
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
