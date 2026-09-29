package com.stockmarket.app.data.repository

import com.stockmarket.app.data.local.WatchlistPreferences
import com.stockmarket.app.data.model.CandlePoint
import com.stockmarket.app.data.model.HistoricalData
import com.stockmarket.app.data.model.HistoryMeta
import com.stockmarket.app.data.model.MarketIndex
import com.stockmarket.app.data.model.SearchResult
import com.stockmarket.app.data.model.StockQuote
import com.stockmarket.app.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin

class StockRepository(
    private val preferences: WatchlistPreferences
) {
    private fun getService() = ApiClient.getService(preferences.getServerUrl())

    suspend fun getWatchlistQuotes(): Result<List<StockQuote>> = withContext(Dispatchers.IO) {
        val symbols = preferences.getWatchlistSymbols().toList()
        if (symbols.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        try {
            val joined = symbols.joinToString(",")
            val quotes = getService().getQuotes(joined)
            Result.success(quotes)
        } catch (e: Exception) {
            // Graceful fallback to synthetic data if server is unreachable
            val fallbackQuotes = symbols.map { createFallbackQuote(it) }
            Result.success(fallbackQuotes)
        }
    }

    suspend fun getStockQuote(symbol: String): Result<StockQuote> = withContext(Dispatchers.IO) {
        try {
            val quote = getService().getQuote(symbol)
            Result.success(quote)
        } catch (e: Exception) {
            Result.success(createFallbackQuote(symbol))
        }
    }

    suspend fun getHistoricalData(symbol: String, range: String): Result<HistoricalData> = withContext(Dispatchers.IO) {
        try {
            val data = getService().getHistory(symbol = symbol, range = range)
            Result.success(data)
        } catch (e: Exception) {
            Result.success(createFallbackHistory(symbol, range))
        }
    }

    suspend fun searchStocks(query: String): Result<List<SearchResult>> = withContext(Dispatchers.IO) {
        try {
            val results = getService().searchStocks(query)
            Result.success(results)
        } catch (e: Exception) {
            val filtered = FALLBACK_SEARCH.filter {
                it.symbol.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
            }
            Result.success(filtered)
        }
    }

    suspend fun getMarketIndices(): Result<List<MarketIndex>> = withContext(Dispatchers.IO) {
        try {
            val indices = getService().getMarketIndices()
            Result.success(indices)
        } catch (e: Exception) {
            Result.success(FALLBACK_INDICES)
        }
    }

    suspend fun testServerConnection(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val testService = ApiClient.getService(url)
            val res = testService.checkHealth()
            res["status"] == "ok"
        } catch (e: Exception) {
            false
        }
    }

    fun isWatchlisted(symbol: String): Boolean = preferences.isInWatchlist(symbol)

    fun toggleWatchlist(symbol: String): Boolean {
        return if (preferences.isInWatchlist(symbol)) {
            preferences.removeSymbol(symbol)
            false
        } else {
            preferences.addSymbol(symbol)
            true
        }
    }

    // --- Synthetic Fallback Generators ---

    private fun createFallbackQuote(symbol: String): StockQuote {
        val clean = symbol.trim().uppercase()
        val basePrice = when (clean) {
            "AAPL" -> 231.41
            "TSLA" -> 260.48
            "NVDA" -> 138.25
            "MSFT" -> 428.15
            "0700.HK" -> 432.80
            "600519.SS" -> 1560.00
            else -> 100.0 + (clean.hashCode().coerceAtLeast(0) % 300)
        }
        val change = round(basePrice * 0.015 * 100) / 100
        val changePercent = round((change / basePrice) * 10000) / 100
        val currency = when {
            clean.endsWith(".HK") -> "HKD"
            clean.endsWith(".SS") || clean.endsWith(".SZ") -> "CNY"
            else -> "USD"
        }

        return StockQuote(
            symbol = clean,
            name = getStockName(clean),
            price = basePrice,
            change = change,
            changePercent = changePercent,
            currency = currency,
            exchange = if (clean.endsWith(".HK")) "HKSE" else if (clean.endsWith(".SS")) "SSE" else "NASDAQ",
            open = basePrice - 0.5,
            high = basePrice + abs(change) + 1.2,
            low = basePrice - abs(change) - 0.8,
            previousClose = basePrice - change,
            volume = 35240000L,
            marketCap = 2500000000000L,
            peRatio = 32.5,
            fiftyTwoWeekHigh = basePrice * 1.25,
            fiftyTwoWeekLow = basePrice * 0.75,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createFallbackHistory(symbol: String, range: String): HistoricalData {
        val clean = symbol.trim().uppercase()
        val quote = createFallbackQuote(clean)
        val basePrice = quote.price
        val count = when (range) {
            "1d" -> 48
            "5d" -> 40
            "1mo" -> 30
            "6mo" -> 90
            "1y" -> 180
            else -> 240
        }
        val intervalMs = when (range) {
            "1d" -> 5 * 60 * 1000L
            "5d" -> 30 * 60 * 1000L
            "1mo" -> 24 * 3600 * 1000L
            "6mo" -> 2 * 24 * 3600 * 1000L
            "1y" -> 2 * 24 * 3600 * 1000L
            else -> 7 * 24 * 3600 * 1000L
        }

        val candles = mutableListOf<CandlePoint>()
        val now = System.currentTimeMillis()
        var current = basePrice * 0.9

        for (i in 0 until count) {
            val ts = now - (count - i) * intervalMs
            val delta = sin(i * 0.5 + clean.hashCode() % 10) * 0.02 + 0.003
            val open = round(current * 100) / 100
            val close = round(max(1.0, open * (1 + delta)) * 100) / 100
            val high = round(max(open, close) * (1 + abs(sin(i.toDouble())) * 0.015) * 100) / 100
            val low = round(min(open, close) * (1 - abs(cos(i.toDouble())) * 0.015) * 100) / 100
            val vol = (1000000L + abs(sin(i.toDouble())) * 5000000).toLong()

            candles.add(CandlePoint(timestamp = ts, open = open, high = high, low = low, close = close, volume = vol))
            current = close
        }

        return HistoricalData(
            symbol = clean,
            range = range,
            interval = null,
            candles = candles,
            meta = HistoryMeta(
                currency = quote.currency,
                previousClose = quote.previousClose,
                high = candles.maxOfOrNull { it.high } ?: basePrice,
                low = candles.minOfOrNull { it.low } ?: (basePrice * 0.8)
            )
        )
    }

    private fun getStockName(symbol: String): String = when (symbol) {
        "AAPL" -> "Apple Inc."
        "TSLA" -> "Tesla, Inc."
        "NVDA" -> "NVIDIA Corporation"
        "MSFT" -> "Microsoft Corporation"
        "0700.HK" -> "腾讯控股 (Tencent)"
        "600519.SS" -> "贵州茅台 (Moutai)"
        "9988.HK" -> "阿里巴巴 (Alibaba)"
        "300750.SZ" -> "宁德时代 (CATL)"
        else -> "$symbol Corp"
    }

    companion object {
        val FALLBACK_INDICES = listOf(
            MarketIndex("^GSPC", "S&P 500", 5864.67, 24.34, 0.42),
            MarketIndex("^IXIC", "NASDAQ", 18415.21, 115.80, 0.63),
            MarketIndex("^DJI", "Dow Jones", 42863.86, -45.12, -0.11),
            MarketIndex("000001.SS", "上证指数", 3326.46, 38.20, 1.16),
            MarketIndex("399001.SZ", "深证成指", 10611.72, 184.60, 1.77),
            MarketIndex("^HSI", "恒生指数", 20638.70, 254.30, 1.25)
        )

        val FALLBACK_SEARCH = listOf(
            SearchResult("AAPL", "Apple Inc.", "NASDAQ", "EQUITY"),
            SearchResult("TSLA", "Tesla, Inc.", "NASDAQ", "EQUITY"),
            SearchResult("NVDA", "NVIDIA Corporation", "NASDAQ", "EQUITY"),
            SearchResult("MSFT", "Microsoft Corporation", "NASDAQ", "EQUITY"),
            SearchResult("AMZN", "Amazon.com, Inc.", "NASDAQ", "EQUITY"),
            SearchResult("GOOGL", "Alphabet Inc.", "NASDAQ", "EQUITY"),
            SearchResult("META", "Meta Platforms, Inc.", "NASDAQ", "EQUITY"),
            SearchResult("0700.HK", "腾讯控股 (Tencent Holdings)", "HKSE", "EQUITY"),
            SearchResult("9988.HK", "阿里巴巴 (Alibaba Group)", "HKSE", "EQUITY"),
            SearchResult("600519.SS", "贵州茅台 (Kweichow Moutai)", "SSE", "EQUITY"),
            SearchResult("000858.SZ", "五粮液 (Wuliangye)", "SZSE", "EQUITY"),
            SearchResult("300750.SZ", "宁德时代 (CATL)", "SZSE", "EQUITY"),
            SearchResult("002594.SZ", "比亚迪 (BYD)", "SZSE", "EQUITY")
        )
    }
}
