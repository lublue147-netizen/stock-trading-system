package com.stockmarket.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.stockmarket.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class WatchlistGroup(
    val name: String,
    val symbols: List<String>
)

class WatchlistPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stock_app_prefs", Context.MODE_PRIVATE)

    companion object {
        const val GROUP_ALL = "全部"
        const val GROUP_DEFAULT = "默认"
        const val GROUP_SECTORS = "热门板块"

        private const val KEY_WATCHLIST = "key_watchlist_symbols"
        private const val KEY_GROUPS = "key_watchlist_groups_json"
        private const val KEY_SELECTED_GROUP = "key_selected_group_name"
        private const val KEY_SERVER_URL = "key_server_url"
        private const val KEY_COLOR_SCHEME = "key_color_scheme" // "CN" or "US"
        private const val KEY_AUTO_REFRESH = "key_auto_refresh_enabled"

        val DEFAULT_SYMBOLS = listOf(
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

        val DEFAULT_SECTOR_SYMBOLS = listOf(
            "BK1638", // 最近多板
            "BK1050", // 昨日涨停-含一字
            "BK1715"  // 趋势股
        )

        fun isValidSymbol(symbol: String): Boolean {
            val s = symbol.trim().uppercase()
            return s.startsWith("BK") ||
                    s == "800005" ||
                    s.endsWith(".SS") || s.endsWith(".SZ") || s.endsWith(".BJ") ||
                    s.matches(Regex("^[0-9]{6}(\\.[A-Za-z]+)?$"))
        }

        private fun computeAllSymbols(groups: List<WatchlistGroup>): List<String> {
            val result = mutableListOf<String>()
            for (g in groups) {
                for (s in g.symbols) {
                    if (!result.contains(s)) {
                        result.add(s)
                    }
                }
            }
            return result
        }
    }

    private val _groupsFlow: MutableStateFlow<List<WatchlistGroup>>
    val groupsFlow: StateFlow<List<WatchlistGroup>>

    private val _selectedGroupFlow: MutableStateFlow<String>
    val selectedGroupFlow: StateFlow<String>

    private val _watchlistFlow: MutableStateFlow<Set<String>>
    val watchlistFlow: StateFlow<Set<String>>

    init {
        val initialGroups = readGroupsFromPrefs()
        _groupsFlow = MutableStateFlow(initialGroups)
        groupsFlow = _groupsFlow.asStateFlow()

        val initialSelected = prefs.getString(KEY_SELECTED_GROUP, GROUP_ALL) ?: GROUP_ALL
        _selectedGroupFlow = MutableStateFlow(initialSelected)
        selectedGroupFlow = _selectedGroupFlow.asStateFlow()

        val allSymbols = computeAllSymbols(initialGroups)
        _watchlistFlow = MutableStateFlow(allSymbols.toSet())
        watchlistFlow = _watchlistFlow.asStateFlow()
    }

    private fun readGroupsFromPrefs(): List<WatchlistGroup> {
        val rawJson = prefs.getString(KEY_GROUPS, null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(rawJson)
                val list = mutableListOf<WatchlistGroup>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name", "")
                    if (name.isBlank()) continue
                    val symArr = obj.optJSONArray("symbols") ?: JSONArray()
                    val syms = mutableListOf<String>()
                    for (j in 0 until symArr.length()) {
                        val sym = symArr.optString(j, "").trim().uppercase()
                        if (isValidSymbol(sym) && !syms.contains(sym)) {
                            syms.add(sym)
                        }
                    }
                    list.add(WatchlistGroup(name, syms))
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {}
        }

        // Migrate from legacy KEY_WATCHLIST if available
        val legacy = prefs.getStringSet(KEY_WATCHLIST, null)
        val defaultStockSymbols = if (!legacy.isNullOrEmpty()) {
            val filtered = legacy.filter { isValidSymbol(it) && !it.startsWith("BK") }
            if (filtered.isNotEmpty()) filtered else DEFAULT_SYMBOLS
        } else {
            DEFAULT_SYMBOLS
        }

        val initial = listOf(
            WatchlistGroup(GROUP_DEFAULT, defaultStockSymbols),
            WatchlistGroup(GROUP_SECTORS, DEFAULT_SECTOR_SYMBOLS)
        )

        // Write directly to SharedPreferences (DO NOT touch StateFlows here!)
        try {
            val arr = JSONArray()
            for (g in initial) {
                val obj = JSONObject()
                obj.put("name", g.name)
                val symArr = JSONArray()
                g.symbols.forEach { symArr.put(it) }
                obj.put("symbols", symArr)
                arr.put(obj)
            }
            prefs.edit().putString(KEY_GROUPS, arr.toString()).apply()
            val all = computeAllSymbols(initial)
            prefs.edit().putStringSet(KEY_WATCHLIST, all.toSet()).apply()
        } catch (_: Exception) {}

        return initial
    }

    fun getGroups(): List<WatchlistGroup> = _groupsFlow.value

    fun saveGroups(groups: List<WatchlistGroup>) {
        try {
            val arr = JSONArray()
            for (g in groups) {
                val obj = JSONObject()
                obj.put("name", g.name)
                val symArr = JSONArray()
                g.symbols.forEach { symArr.put(it) }
                obj.put("symbols", symArr)
                arr.put(obj)
            }
            prefs.edit().putString(KEY_GROUPS, arr.toString()).apply()
        } catch (_: Exception) {}

        _groupsFlow.value = groups
        val allSyms = computeAllSymbols(groups)
        _watchlistFlow.value = allSyms.toSet()
        try {
            prefs.edit().putStringSet(KEY_WATCHLIST, allSyms.toSet()).apply()
        } catch (_: Exception) {}
    }

    fun getSelectedGroup(): String {
        return prefs.getString(KEY_SELECTED_GROUP, GROUP_ALL) ?: GROUP_ALL
    }

    fun setSelectedGroup(name: String) {
        prefs.edit().putString(KEY_SELECTED_GROUP, name).apply()
        _selectedGroupFlow.value = name
    }

    fun createGroup(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty() || clean == GROUP_ALL) return false
        val current = getGroups().toMutableList()
        if (current.any { it.name == clean }) return false
        current.add(WatchlistGroup(clean, emptyList()))
        saveGroups(current)
        setSelectedGroup(clean)
        return true
    }

    fun deleteGroup(name: String): Boolean {
        val clean = name.trim()
        if (clean == GROUP_ALL || clean == GROUP_DEFAULT) return false
        val current = getGroups().toMutableList()
        val index = current.indexOfFirst { it.name == clean }
        if (index == -1) return false
        current.removeAt(index)
        saveGroups(current)
        if (getSelectedGroup() == clean) {
            setSelectedGroup(GROUP_ALL)
        }
        return true
    }

    fun renameGroup(oldName: String, newName: String): Boolean {
        val cleanOld = oldName.trim()
        val cleanNew = newName.trim()
        if (cleanOld == GROUP_ALL || cleanOld == GROUP_DEFAULT) return false
        if (cleanNew.isEmpty() || cleanNew == GROUP_ALL) return false
        val current = getGroups().toMutableList()
        if (current.any { it.name == cleanNew }) return false
        val index = current.indexOfFirst { it.name == cleanOld }
        if (index == -1) return false
        val oldGroup = current[index]
        current[index] = oldGroup.copy(name = cleanNew)
        saveGroups(current)
        if (getSelectedGroup() == cleanOld) {
            setSelectedGroup(cleanNew)
        }
        return true
    }

    fun getSymbolsForGroup(groupName: String): List<String> {
        val clean = groupName.trim()
        if (clean == GROUP_ALL || clean.isEmpty()) {
            return getAllSymbols()
        }
        val group = getGroups().find { it.name == clean }
        return group?.symbols ?: emptyList()
    }

    fun getAllSymbols(): List<String> = computeAllSymbols(getGroups())

    fun addSymbolToGroup(symbol: String, groupName: String? = null): Boolean {
        val clean = symbol.trim().uppercase()
        if (!isValidSymbol(clean)) return false

        val current = getGroups().toMutableList()
        val targetName = when {
            !groupName.isNullOrBlank() && groupName != GROUP_ALL -> groupName
            clean.startsWith("BK") -> {
                if (current.none { it.name == GROUP_SECTORS }) {
                    current.add(WatchlistGroup(GROUP_SECTORS, emptyList()))
                }
                GROUP_SECTORS
            }
            getSelectedGroup() != GROUP_ALL -> getSelectedGroup()
            else -> GROUP_DEFAULT
        }

        var targetIndex = current.indexOfFirst { it.name == targetName }
        if (targetIndex == -1) {
            current.add(WatchlistGroup(targetName, emptyList()))
            targetIndex = current.lastIndex
        }

        val targetGroup = current[targetIndex]
        if (targetGroup.symbols.contains(clean)) return false

        val newSymbols = targetGroup.symbols.toMutableList().apply { add(0, clean) }
        current[targetIndex] = targetGroup.copy(symbols = newSymbols)
        saveGroups(current)
        return true
    }

    fun removeSymbolFromGroup(symbol: String, groupName: String? = null): Boolean {
        val clean = symbol.trim().uppercase()
        val current = getGroups().toMutableList()
        var modified = false

        if (groupName == null || groupName == GROUP_ALL) {
            // Remove from all groups
            for (i in current.indices) {
                val g = current[i]
                if (g.symbols.contains(clean)) {
                    current[i] = g.copy(symbols = g.symbols.filter { it != clean })
                    modified = true
                }
            }
        } else {
            val index = current.indexOfFirst { it.name == groupName }
            if (index != -1) {
                val g = current[index]
                if (g.symbols.contains(clean)) {
                    current[index] = g.copy(symbols = g.symbols.filter { it != clean })
                    modified = true
                }
            }
        }

        if (modified) {
            saveGroups(current)
        }
        return modified
    }

    fun moveSymbolToGroup(symbol: String, targetGroup: String): Boolean {
        val clean = symbol.trim().uppercase()
        if (!isValidSymbol(clean)) return false
        val current = getGroups().toMutableList()
        var targetIndex = current.indexOfFirst { it.name == targetGroup }
        if (targetIndex == -1) {
            current.add(WatchlistGroup(targetGroup, emptyList()))
            targetIndex = current.lastIndex
        }
        val target = current[targetIndex]
        if (!target.symbols.contains(clean)) {
            val newSymbols = target.symbols.toMutableList().apply { add(clean) }
            current[targetIndex] = target.copy(symbols = newSymbols)
            saveGroups(current)
            return true
        }
        return false
    }

    // Legacy compatibility methods
    fun getWatchlistSymbols(): Set<String> = getAllSymbols().toSet()

    fun addSymbol(symbol: String): Boolean = addSymbolToGroup(symbol, null)

    fun removeSymbol(symbol: String): Boolean = removeSymbolFromGroup(symbol, null)

    fun isInWatchlist(symbol: String): Boolean {
        val clean = symbol.trim().uppercase()
        return getAllSymbols().contains(clean)
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
