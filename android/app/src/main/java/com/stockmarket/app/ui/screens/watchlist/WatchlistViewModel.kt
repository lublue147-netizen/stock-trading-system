package com.stockmarket.app.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockmarket.app.data.local.WatchlistGroup
import com.stockmarket.app.data.local.WatchlistPreferences
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
    val groups: List<WatchlistGroup> = emptyList(),
    val selectedGroup: String = WatchlistPreferences.GROUP_ALL,
    val errorMessage: String? = null
)

class WatchlistViewModel(
    private val repository: StockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WatchlistUiState(
            isLoading = true,
            groups = repository.preferences.getGroups(),
            selectedGroup = repository.preferences.getSelectedGroup()
        )
    )
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        loadData(isInitial = true)
        startAutoRefresh()

        // Observe groups flow
        viewModelScope.launch {
            repository.preferences.groupsFlow.collect { groups ->
                _uiState.update { it.copy(groups = groups) }
            }
        }
        viewModelScope.launch {
            repository.preferences.selectedGroupFlow.collect { sel ->
                _uiState.update { it.copy(selectedGroup = sel) }
            }
        }
    }

    fun selectGroup(groupName: String) {
        repository.preferences.setSelectedGroup(groupName)
        _uiState.update { it.copy(selectedGroup = groupName) }
        loadData(isInitial = false)
    }

    fun createGroup(name: String): Boolean {
        val success = repository.preferences.createGroup(name)
        if (success) {
            loadData(isInitial = false)
        }
        return success
    }

    fun deleteGroup(name: String): Boolean {
        val success = repository.preferences.deleteGroup(name)
        if (success) {
            loadData(isInitial = false)
        }
        return success
    }

    fun renameGroup(oldName: String, newName: String): Boolean {
        val success = repository.preferences.renameGroup(oldName, newName)
        if (success) {
            loadData(isInitial = false)
        }
        return success
    }

    fun moveSymbol(symbol: String, targetGroup: String): Boolean {
        val success = repository.preferences.moveSymbolToGroup(symbol, targetGroup)
        if (success) {
            loadData(isInitial = false)
        }
        return success
    }

    fun removeSymbol(symbol: String) {
        val currentGroup = _uiState.value.selectedGroup
        val target = if (currentGroup == WatchlistPreferences.GROUP_ALL) null else currentGroup
        repository.preferences.removeSymbolFromGroup(symbol, target)
        loadData(isInitial = false)
    }

    fun loadData(isInitial: Boolean = false) {
        viewModelScope.launch {
            if (isInitial) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            }

            val curGroup = repository.preferences.getSelectedGroup()
            val symbols = repository.preferences.getSymbolsForGroup(curGroup)
            val quotesResult = repository.getWatchlistQuotes(symbols)
            val indicesResult = repository.getMarketIndices()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    quotes = quotesResult.getOrDefault(emptyList()),
                    indices = indicesResult.getOrDefault(emptyList()),
                    groups = repository.preferences.getGroups(),
                    selectedGroup = curGroup,
                    errorMessage = null
                )
            }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000) // auto-refresh every 15s
                loadData(isInitial = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
