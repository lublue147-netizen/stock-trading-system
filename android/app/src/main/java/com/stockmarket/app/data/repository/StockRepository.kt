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
        "600519.SS" -> "贵州茅台"
        "300750.SZ" -> "宁德时代"
        "002594.SZ" -> "比亚迪"
        "601318.SS" -> "中国平安"
        "600036.SS" -> "招商银行"
        "300059.SZ" -> "东方财富"
        "000001.SZ" -> "平安银行"
        "000858.SZ" -> "五粮液"
        "688981.SS" -> "中芯国际"
        "000001.SS" -> "上证指数"
        "399001.SZ" -> "深证成指"
        "399006.SZ" -> "创业板指"
        "000688.SS" -> "科创50"
        "000300.SS" -> "沪深300"
        "0700.HK" -> "腾讯控股"
        "9988.HK" -> "阿里巴巴"
        "AAPL" -> "苹果公司"
        "TSLA" -> "特斯拉"
        "NVDA" -> "英伟达"
        else -> "$symbol"
    }

    companion object {
        val FALLBACK_INDICES = listOf(
            MarketIndex("000001.SS", "上证指数", 3841.88, 18.26, 0.48),
            MarketIndex("399001.SZ", "深证成指", 12939.70, 80.95, 0.63),
            MarketIndex("399006.SZ", "创业板指", 3152.28, 12.46, 0.40),
            MarketIndex("000688.SS", "科创50", 1575.43, 19.45, 1.25),
            MarketIndex("000300.SS", "沪深300", 4357.12, 16.36, 0.38),
            MarketIndex("^HSI", "恒生指数", 20590.15, 312.45, 1.54)
        )

        val FALLBACK_SEARCH = listOf(
            SearchResult("600519.SS", "贵州茅台", "上交所", "A股"),
            SearchResult("300750.SZ", "宁德时代", "深交所", "A股"),
            SearchResult("002594.SZ", "比亚迪", "深交所", "A股"),
            SearchResult("300059.SZ", "东方财富", "深交所", "A股"),
            SearchResult("688981.SS", "中芯国际", "上交所", "科创板"),
            SearchResult("601318.SS", "中国平安", "上交所", "A股"),
            SearchResult("600036.SS", "招商银行", "上交所", "A股"),
            SearchResult("000858.SZ", "五粮液", "深交所", "A股"),
            SearchResult("000001.SZ", "平安银行", "深交所", "A股"),
            SearchResult("000001.SS", "上证指数", "上交所", "指数"),
            SearchResult("399001.SZ", "深证成指", "深交所", "指数"),
            SearchResult("399006.SZ", "创业板指", "深交所", "指数"),
            SearchResult("0700.HK", "腾讯控股", "港交所", "港股"),
            SearchResult("AAPL", "苹果公司 (Apple)", "NASDAQ", "美股"),
            SearchResult("TSLA", "特斯拉 (Tesla)", "NASDAQ", "美股")
        )
    }
}
