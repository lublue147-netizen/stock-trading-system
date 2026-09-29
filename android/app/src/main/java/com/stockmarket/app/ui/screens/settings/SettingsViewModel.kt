package com.stockmarket.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockmarket.app.data.local.WatchlistPreferences
import com.stockmarket.app.data.remote.ApiClient
import com.stockmarket.app.data.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val colorScheme: String = "CN",
    val isAutoRefreshEnabled: Boolean = true,
    val testStatus: String = "idle" // "idle", "testing", "success", "failed"
)

class SettingsViewModel(
    private val preferences: WatchlistPreferences,
    private val repository: StockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            serverUrl = preferences.getServerUrl(),
            colorScheme = preferences.getColorScheme(),
            isAutoRefreshEnabled = preferences.isAutoRefreshEnabled()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onServerUrlChange(newUrl: String) {
        _uiState.update { it.copy(serverUrl = newUrl, testStatus = "idle") }
    }

    fun setColorScheme(scheme: String) {
        preferences.setColorScheme(scheme)
        _uiState.update { it.copy(colorScheme = scheme) }
    }

    fun setAutoRefresh(enabled: Boolean) {
        preferences.setAutoRefreshEnabled(enabled)
        _uiState.update { it.copy(isAutoRefreshEnabled = enabled) }
    }

    fun saveServerUrl() {
        preferences.setServerUrl(_uiState.value.serverUrl)
    }

    fun resetServerUrl() {
        preferences.setServerUrl(ApiClient.DEFAULT_BASE_URL)
        _uiState.update { it.copy(serverUrl = ApiClient.DEFAULT_BASE_URL, testStatus = "idle") }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(testStatus = "testing") }
            val ok = repository.testServerConnection(_uiState.value.serverUrl)
            _uiState.update { it.copy(testStatus = if (ok) "success" else "failed") }
        }
    }
}
