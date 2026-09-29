package com.stockmarket.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.stockmarket.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WatchlistPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stock_app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WATCHLIST = "key_watchlist_symbols"
        private const val KEY_SERVER_URL = "key_server_url"
        private const val KEY_COLOR_SCHEME = "key_color_scheme" // "CN" or "US"
        private const val KEY_AUTO_REFRESH = "key_auto_refresh_enabled"

        val DEFAULT_SYMBOLS = setOf(
            "600519.SS", // 贵州茅台
            "300750.SZ", // 宁德时代
            "002594.SZ", // 比亚迪
            "002579.SZ", // 中京电子
            "300059.SZ", // 东方财富
            "601318.SS", // 中国平安
            "600036.SS", // 招商银行
            "000001.SZ", // 平安银行
            "000858.SZ", // 五粮液
            "688981.SS"  // 中芯国际
        )

        private fun isAShare(symbol: String): Boolean {
            val s = symbol.trim().uppercase()
            return s.endsWith(".SS") || s.endsWith(".SZ") || s.endsWith(".BJ") ||
                    s.matches(Regex("^[0-9]{6}(\\.[A-Za-z]+)?$"))
        }
    }

    private val _watchlistFlow = MutableStateFlow(getWatchlistSymbols())
    val watchlistFlow: StateFlow<Set<String>> = _watchlistFlow.asStateFlow()

    fun getWatchlistSymbols(): Set<String> {
        val raw = prefs.getStringSet(KEY_WATCHLIST, null) ?: DEFAULT_SYMBOLS
        val aSharesOnly = raw.filter { isAShare(it) }.toSet()
        return if (aSharesOnly.isNotEmpty()) aSharesOnly else DEFAULT_SYMBOLS
    }

    fun addSymbol(symbol: String): Boolean {
        val current = getWatchlistSymbols().toMutableSet()
        val clean = symbol.trim().uppercase()
        val added = current.add(clean)
        if (added) {
            prefs.edit().putStringSet(KEY_WATCHLIST, current).apply()
            _watchlistFlow.value = current
        }
        return added
    }

    fun removeSymbol(symbol: String): Boolean {
        val current = getWatchlistSymbols().toMutableSet()
        val clean = symbol.trim().uppercase()
        val removed = current.remove(clean)
        if (removed) {
            prefs.edit().putStringSet(KEY_WATCHLIST, current).apply()
            _watchlistFlow.value = current
        }
        return removed
    }

    fun isInWatchlist(symbol: String): Boolean {
        val clean = symbol.trim().uppercase()
        return getWatchlistSymbols().contains(clean)
    }

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, ApiClient.DEFAULT_BASE_URL) ?: ApiClient.DEFAULT_BASE_URL
    }

    fun setServerUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString(KEY_SERVER_URL, trimmed).apply()
    }

    fun getColorScheme(): String {
        return prefs.getString(KEY_COLOR_SCHEME, "CN") ?: "CN" // Default to CN (Red up, Green down)
    }

    fun setColorScheme(scheme: String) {
        prefs.edit().putString(KEY_COLOR_SCHEME, scheme).apply()
    }

    fun isAutoRefreshEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_REFRESH, true)
    }

    fun setAutoRefreshEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REFRESH, enabled).apply()
    }
}
