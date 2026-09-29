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
    val selectedRange: String = "1mo",
    val chartType: ChartType = ChartType.CANDLESTICK,
    val isWatchlisted: Boolean = false,
    val showMA: Boolean = true,
    val errorMessage: String? = null
)

class StockDetailViewModel(
    private val symbol: String,
    private val repository: StockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StockDetailUiState(
            symbol = symbol,
            isWatchlisted = repository.isWatchlisted(symbol)
        )
    )
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    init {
        loadQuote()
        loadHistory(_uiState.value.selectedRange)
    }

    fun loadQuote() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuote = true) }
            val res = repository.getStockQuote(symbol)
            _uiState.update {
                it.copy(
                    isLoadingQuote = false,
                    quote = res.getOrNull(),
                    isWatchlisted = repository.isWatchlisted(symbol)
                )
            }
        }
    }

    fun loadHistory(range: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChart = true, selectedRange = range) }
            val res = repository.getHistoricalData(symbol, range)
            _uiState.update {
                it.copy(
                    isLoadingChart = false,
                    historicalData = res.getOrNull(),
                    selectedRange = range
                )
            }
        }
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
