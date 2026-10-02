package com.stockmarket.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockmarket.app.data.model.CandlePoint
import com.stockmarket.app.data.model.HistoricalData
import com.stockmarket.app.data.model.StockQuote
import com.stockmarket.app.data.repository.StockRepository
import com.stockmarket.app.ui.components.ChartType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StockDetailUiState(
    val symbol: String,
    val isLoadingQuote: Boolean = false,
    val isLoadingChart: Boolean = false,
    val quote: StockQuote? = null,
    val historicalData: HistoricalData? = null,
    val selectedRange: String = "1d",
    val chartType: ChartType = ChartType.LINE,
    val isWatchlisted: Boolean = false,
    val showMA: Boolean = true,
    val errorMessage: String? = null,
    val selectedIntradayDate: String? = null,
    val availableIntradayDates: List<String> = emptyList(),
    val isHistoricalIntraday: Boolean = false,
    val historicalPreviousClose: Double? = null,
    val previousKLineRange: String? = null,
    val selectedKLineDate: String? = null
)

class StockDetailViewModel(
    private val symbol: String,
    private val repository: StockRepository
) : ViewModel() {

    private val _uiState: MutableStateFlow<StockDetailUiState>
    val uiState: StateFlow<StockDetailUiState>

    init {
        val cached = repository.getCachedStockQuote(symbol)
        _uiState = MutableStateFlow(
            StockDetailUiState(
                symbol = symbol,
                quote = cached,
                isLoadingQuote = cached == null,
                isWatchlisted = repository.isWatchlisted(symbol)
            )
        )
        uiState = _uiState.asStateFlow()

        loadQuote()
        loadHistory(_uiState.value.selectedRange)
    }

    fun loadQuote() {
        viewModelScope.launch {
            if (_uiState.value.quote == null) {
                _uiState.update { it.copy(isLoadingQuote = true) }
            }
            val res = repository.getStockQuote(symbol)
            _uiState.update {
                it.copy(
                    isLoadingQuote = false,
                    quote = res.getOrNull() ?: it.quote,
                    isWatchlisted = repository.isWatchlisted(symbol)
                )
            }
        }
    }

    fun loadHistory(range: String, date: String? = null) {
        val suggestedType = if (range == "1d" || range == "5d") ChartType.LINE else ChartType.CANDLESTICK
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingChart = it.historicalData == null,
                    selectedRange = range,
                    chartType = suggestedType,
                    selectedIntradayDate = date
                )
            }
            val res = repository.getHistoricalData(symbol, range, date)
            val history = res.getOrNull()
            val availDates = history?.meta?.availableDates ?: _uiState.value.availableIntradayDates
            val selectedDate = history?.meta?.selectedDate ?: date ?: availDates.lastOrNull()
            val latestDate = availDates.lastOrNull()
            val isHistorical = range == "1d" && selectedDate != null && latestDate != null && selectedDate != latestDate
            val prevClose = history?.meta?.previousClose

            _uiState.update {
                it.copy(
                    isLoadingChart = false,
                    historicalData = history,
                    selectedRange = range,
                    selectedIntradayDate = selectedDate,
                    availableIntradayDates = availDates,
                    isHistoricalIntraday = isHistorical,
                    historicalPreviousClose = prevClose
                )
            }
        }
    }

    fun selectHistoricalDate(date: String) {
        loadHistory("1d", date)
    }

    fun viewIntradayFromKLine(date: String, fromRange: String = "1mo") {
        _uiState.update { it.copy(previousKLineRange = fromRange, selectedKLineDate = date) }
        loadHistory("1d", date)
    }

    fun returnToKLine() {
        val targetRange = _uiState.value.previousKLineRange ?: "1mo"
        _uiState.update { it.copy(previousKLineRange = null) }
        loadHistory(targetRange)
    }

    fun setSelectedKLineDate(date: String?) {
        _uiState.update { it.copy(selectedKLineDate = date) }
    }

    fun stepDate(direction: Int) {
        val dates = _uiState.value.availableIntradayDates
        if (dates.isEmpty()) return
        val currentDate = _uiState.value.selectedIntradayDate ?: dates.lastOrNull() ?: return
        val currentIndex = dates.indexOf(currentDate)
        if (currentIndex == -1) return
        val nextIndex = currentIndex + direction
        if (nextIndex in dates.indices) {
            val targetDate = dates[nextIndex]
            if (nextIndex == dates.size - 1) {
                loadHistory("1d", null)
            } else {
                loadHistory("1d", targetDate)
            }
        }
    }

    fun resetToToday() {
        loadHistory("1d", null)
    }

    fun setChartType(type: ChartType) {
        _uiState.update { it.copy(chartType = type) }
    }

    fun toggleChartType() {
        _uiState.update {
            val newType = if (it.chartType == ChartType.CANDLESTICK) ChartType.LINE else ChartType.CANDLESTICK
            it.copy(chartType = newType)
        }
    }

    fun toggleMA() {
        _uiState.update { it.copy(showMA = !it.showMA) }
    }

    fun toggleWatchlist() {
        val newState = repository.toggleWatchlist(symbol)
        _uiState.update { it.copy(isWatchlisted = newState) }
    }
}
