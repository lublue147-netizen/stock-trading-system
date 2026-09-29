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
        val isAShare = clean.endsWith(".SS") || clean.endsWith(".SZ") || clean.endsWith(".BJ") || clean.matches(Regex("^[0-9]{6}(\\.[A-Za-z]+)?$"))
        val isChiNextOrStar = clean.startsWith("300") || clean.startsWith("301") || clean.startsWith("688")

        val (basePrice, pe, pb, eps, bps, roe, industry, business, concepts) = when (clean) {
            "600519.SS", "600519" -> Tuple9(
                1560.00, 26.2, 7.85, 58.50, 182.30, 32.1,
                "食品饮料 / 白酒",
                "茅台酒及系列酒的生产与销售，国内高档白酒绝对龙头企业。",
                listOf("白酒龙头", "沪股通", "核心资产", "MSCI中国", "高股息", "大消费")
            )
            "300750.SZ", "300750" -> Tuple9(
                248.50, 25.2, 5.62, 9.85, 44.20, 22.3,
                "电力设备 / 动力电池",
                "全球新能源汽车动力电池与储能系统研发制造龙头。",
                listOf("动力电池", "储能系统", "新能源车", "创业板权重", "深股通")
            )
            "002594.SZ", "002594" -> Tuple9(
                285.60, 27.6, 4.88, 10.35, 58.50, 17.7,
                "汽车整车 / 新能源车",
                "新能源汽车及关键零部件、刀片电池研发与出海制造领军企业。",
                listOf("新能源车", "刀片电池", "深股通", "智能座舱", "出海龙头")
            )
            "300059.SZ", "300059" -> Tuple9(
                22.80, 35.1, 3.25, 0.65, 7.02, 9.25,
                "非银金融 / 证券互联网",
                "以东方财富网为核心的互联网金融服务平台，证券、公募基金代销龙头。",
                listOf("东方财富", "互金龙头", "券商概念", "创业板50", "深股通")
            )
            "601318.SS", "601318" -> Tuple9(
                54.30, 8.76, 1.05, 6.20, 51.70, 12.0,
                "非银金融 / 保险",
                "全牌照综合金融服务集团，涵盖寿险、产险、银行、科技赋能。",
                listOf("保险龙头", "中字头", "高股息", "沪股通", "沪深300")
            )
            "600036.SS", "600036" -> Tuple9(
                38.60, 6.65, 0.88, 5.80, 43.80, 13.2,
                "银行 / 股份制银行",
                "国内领先的零售标杆银行，财富管理与金融科技领跑者。",
                listOf("零售银行王", "高股息", "沪股通", "大金融", "核心资产")
            )
            "000858.SZ", "000858" -> Tuple9(
                138.50, 17.5, 4.12, 7.90, 33.60, 23.5,
                "食品饮料 / 白酒",
                "浓香型白酒典型代表，高端名酒核心企业。",
                listOf("浓香龙头", "深股通", "消费升级", "MSCI中国", "高分红")
            )
            "688981.SS", "688981" -> Tuple9(
                95.20, 85.0, 3.80, 1.12, 25.05, 4.48,
                "半导体 / 芯片制造",
                "中国内地技术最先进、配套最完善、规模最大的集成电路制造企业。",
                listOf("芯片制造", "科创50", "半导体代工", "国产替代", "硬科技")
            )
            "0700.HK" -> Tuple9(
                432.80, 24.3, 3.95, 17.80, 109.50, 16.2,
                "互联网与信息技术",
                "社交网络(微信/QQ)、数字娱乐、金融科技及企业云服务。",
                listOf("港股通", "社交龙头", "手游电竞", "云计算", "腾讯概念")
            )
            "AAPL" -> Tuple9(
                231.41, 34.2, 48.5, 6.76, 4.77, 141.8,
                "消费电子",
                "iPhone、Mac、iPad及可穿戴设备软硬件生态。",
                listOf("美股龙头", "消费电子", "AI手机", "纳斯达克100")
            )
            "TSLA" -> Tuple9(
                260.48, 72.5, 12.8, 3.59, 20.35, 17.6,
                "新能源汽车 / 自动驾驶",
                "电动汽车、储能产品(Powerwall)与全自动驾驶(FSD)技术研发。",
                listOf("特斯拉概念", "机器人Optimus", "自动驾驶", "储能")
            )
            else -> {
                val seed = clean.hashCode().let { if (it < 0) -it else it }
                val p = 20.0 + (seed % 180)
                Tuple9(
                    p, 25.0, 2.5, p / 20.0, p / 3.0, 10.0,
                    if (isAShare) "A股制造业" else "科技创新",
                    "致力于优质产品研发、智能化生产与海内外市场拓展。",
                    listOf("A股精选", "核心资产", "稳健增长")
                )
            }
        }

        val change = round(basePrice * 0.015 * 100) / 100
        val changePercent = round((change / basePrice) * 10000) / 100
        val currency = when {
            clean.endsWith(".HK") -> "HKD"
            clean.endsWith(".SS") || clean.endsWith(".SZ") || clean.endsWith(".BJ") -> "CNY"
            else -> if (isAShare) "CNY" else "USD"
        }
        val exchange = when {
            clean.endsWith(".HK") -> "HKSE"
            clean.endsWith(".SS") || clean.startsWith("6") -> "SSE"
            clean.endsWith(".SZ") || clean.startsWith("0") || clean.startsWith("3") -> "SZSE"
            clean.endsWith(".BJ") || clean.startsWith("8") || clean.startsWith("4") -> "BSE"
            else -> "NASDAQ"
        }

        val limitRatio = if (isChiNextOrStar) 0.20 else 0.10
        val prevClose = basePrice - change
        val limitUp = round(prevClose * (1 + limitRatio) * 100) / 100
        val limitDown = round(prevClose * (1 - limitRatio) * 100) / 100
        val high = basePrice + abs(change) + (basePrice * 0.008)
        val low = basePrice - abs(change) - (basePrice * 0.006)
        val vol = 4520000L + (abs(clean.hashCode()) % 8000000)
        val turnoverRate = 1.25 + ((abs(clean.hashCode()) % 30) / 10.0)
        val turnoverAmt = round(basePrice * vol * 100) / 100

        return StockQuote(
            symbol = clean,
            name = getStockName(clean),
            price = basePrice,
            change = change,
            changePercent = changePercent,
            currency = currency,
            exchange = exchange,
            open = basePrice - (change * 0.3),
            high = round(high * 100) / 100,
            low = round(low * 100) / 100,
            previousClose = round(prevClose * 100) / 100,
            volume = vol,
            marketCap = (basePrice * 1_200_000_000L).toLong(),
            peRatio = pe,
            fiftyTwoWeekHigh = round(basePrice * 1.25 * 100) / 100,
            fiftyTwoWeekLow = round(basePrice * 0.75 * 100) / 100,
            timestamp = System.currentTimeMillis(),
            turnoverRate = round(turnoverRate * 100) / 100,
            turnoverAmount = turnoverAmt,
            amplitude = round(((high - low) / prevClose) * 10000) / 100,
            pbRatio = pb,
            floatMarketCap = (basePrice * 1_050_000_000L).toLong(),
            limitUpPrice = limitUp,
            limitDownPrice = limitDown,
            eps = eps,
            bps = bps,
            roe = roe,
            industry = industry,
            mainBusiness = business,
            conceptTags = concepts
        )
    }

    private data class Tuple9<A, B, C, D, E, F, G, H, I>(
        val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G, val h: H, val i: I
    )

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
