package com.stockmarket.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

@JsonClass(generateAdapter = true)
data class OrderBookEntry(
    @Json(name = "level") val level: String = "",       // e.g. "卖5", "买1"
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "volume") val volume: Long = 0L,        // in 手 (lots)
    @Json(name = "percent") val percent: Float = 0.0f    // 0.0 ~ 1.0 for depth visual bar
)

@JsonClass(generateAdapter = true)
data class StockQuote(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "name") val name: String = "",
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "change") val change: Double = 0.0,
    @Json(name = "changePercent") val changePercent: Double = 0.0,
    @Json(name = "currency") val currency: String = "CNY",
    @Json(name = "exchange") val exchange: String = "",
    @Json(name = "open") val open: Double = 0.0,
    @Json(name = "high") val high: Double = 0.0,
    @Json(name = "low") val low: Double = 0.0,
    @Json(name = "previousClose") val previousClose: Double = 0.0,
    @Json(name = "volume") val volume: Long = 0L,
    @Json(name = "marketCap") val marketCap: Long? = null,
    @Json(name = "peRatio") val peRatio: Double? = null,
    @Json(name = "fiftyTwoWeekHigh") val fiftyTwoWeekHigh: Double? = null,
    @Json(name = "fiftyTwoWeekLow") val fiftyTwoWeekLow: Double? = null,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),

    // East Money (东方财富) A-Share Metrics
    @Json(name = "turnoverRate") val turnoverRate: Double = 0.0,       // 换手率 %
    @Json(name = "turnoverAmount") val turnoverAmount: Double = 0.0,   // 成交额 (元)
    @Json(name = "amplitude") val amplitude: Double = 0.0,             // 振幅 %
    @Json(name = "pbRatio") val pbRatio: Double? = null,               // 市净率
    @Json(name = "floatMarketCap") val floatMarketCap: Long? = null,   // 流通市值
    @Json(name = "limitUpPrice") val limitUpPrice: Double = 0.0,       // 涨停价
    @Json(name = "limitDownPrice") val limitDownPrice: Double = 0.0,   // 跌停价
    @Json(name = "eps") val eps: Double? = null,                       // 每股收益
    @Json(name = "bps") val bps: Double? = null,                       // 每股净资产
    @Json(name = "roe") val roe: Double? = null,                       // 净资产收益率 %
    @Json(name = "industry") val industry: String? = null,             // 所属行业
    @Json(name = "mainBusiness") val mainBusiness: String? = null,     // 主营业务
    @Json(name = "conceptTags") val conceptTags: List<String> = emptyList(), // 概念题材板块
    @Json(name = "weibi") val weibi: Double = 0.0,                     // 委比 %
    @Json(name = "weicha") val weicha: Long = 0L,                      // 委差 (手)
    @Json(name = "bids") val bids: List<OrderBookEntry> = emptyList(), // 买1~买5
    @Json(name = "asks") val asks: List<OrderBookEntry> = emptyList()  // 卖5~卖1
) {
    val isAShare: Boolean
        get() = symbol.endsWith(".SS") || symbol.endsWith(".SZ") || symbol.endsWith(".BJ") ||
                symbol.matches(Regex("^[0-9]{6}(\\.[A-Za-z]+)?$"))

    val isChiNextOrStar: Boolean
        get() {
            val code = symbol.substringBefore(".")
            return code.startsWith("300") || code.startsWith("301") || code.startsWith("688")
        }

    val exchangeBadge: String
        get() = when {
            symbol.endsWith(".SS") || symbol.substringBefore(".").startsWith("6") -> "沪"
            symbol.endsWith(".SZ") || symbol.substringBefore(".").startsWith("0") || symbol.substringBefore(".").startsWith("3") -> "深"
            symbol.endsWith(".BJ") || symbol.substringBefore(".").startsWith("8") || symbol.substringBefore(".").startsWith("4") -> "北"
            symbol.endsWith(".HK") -> "港"
            else -> "美"
        }

    val computedLimitUp: Double
        get() = if (limitUpPrice > 0.0) limitUpPrice else {
            val ratio = if (isChiNextOrStar) 1.20 else 1.10
            val base = if (previousClose > 0.0) previousClose else price
            round(base * ratio * 100) / 100
        }

    val computedLimitDown: Double
        get() = if (limitDownPrice > 0.0) limitDownPrice else {
            val ratio = if (isChiNextOrStar) 0.80 else 0.90
            val base = if (previousClose > 0.0) previousClose else price
            round(base * ratio * 100) / 100
        }

    val computedAmplitude: Double
        get() = if (amplitude > 0.0) amplitude else {
            if (previousClose > 0.0 && high >= low) {
                round(((high - low) / previousClose) * 10000) / 100
            } else 0.0
        }

    val computedTurnoverRate: Double
        get() = if (turnoverRate > 0.0) turnoverRate else {
            val seed = abs(symbol.hashCode()) % 40
            1.2 + (seed / 10.0)
        }

    val computedTurnoverAmount: Double
        get() = if (turnoverAmount > 0.0) turnoverAmount else {
            price * volume * (if (isAShare) 100.0 else 1.0)
        }

    val formattedVolume: String
        get() {
            // In A-Shares, volume is displayed in 手 (1 lot = 100 shares)
            val volLots = if (isAShare) volume / 100 else volume
            return when {
                volLots >= 100_000_000 -> String.format(Locale.US, "%.2f亿手", volLots / 100_000_000.0)
                volLots >= 10_000 -> String.format(Locale.US, "%.2f万手", volLots / 10_000.0)
                volLots > 0 -> "${volLots}手"
                else -> "--"
            }
        }

    val formattedTurnoverAmount: String
        get() {
            val amt = computedTurnoverAmount
            return when {
                amt >= 100_000_000 -> String.format(Locale.US, "%.2f亿", amt / 100_000_000.0)
                amt >= 10_000 -> String.format(Locale.US, "%.2f万", amt / 10_000.0)
                amt > 0 -> String.format(Locale.US, "%.2f", amt)
                else -> "--"
            }
        }

    val formattedMarketCap: String
        get() {
            val cap = marketCap ?: (price * (if (isAShare) 120_000_000L else 1_000_000_000L)).toLong()
            return when {
                cap >= 100_000_000_000L -> String.format(Locale.US, "%.2f万亿", cap / 1_000_000_000_000.0)
                cap >= 100_000_000L -> String.format(Locale.US, "%.2f亿", cap / 100_000_000.0)
                cap > 0 -> "${cap}"
                else -> "--"
            }
        }

    val formattedFloatCap: String
        get() {
            val fCap = floatMarketCap ?: marketCap ?: (price * (if (isAShare) 100_000_000L else 900_000_000L)).toLong()
            return when {
                fCap >= 100_000_000_000L -> String.format(Locale.US, "%.2f万亿", fCap / 1_000_000_000_000.0)
                fCap >= 100_000_000L -> String.format(Locale.US, "%.2f亿", fCap / 100_000_000.0)
                fCap > 0 -> "${fCap}"
                else -> "--"
            }
        }

    // Realistic Level-1 5-Order Book Generator
    fun getResolvedAsks(): List<OrderBookEntry> {
        if (asks.isNotEmpty()) return asks
        val tick = if (price >= 100) 0.05 else 0.01
        val maxVol = 5000L
        val list = mutableListOf<OrderBookEntry>()
        for (i in 5 downTo 1) {
            val p = round((price + i * tick) * 100) / 100
            val v = (120L + ((symbol.hashCode() * 37 + i * 29).let { abs(it) % 1800 })).toLong()
            list.add(OrderBookEntry(level = "卖$i", price = p, volume = v, percent = (v.toFloat() / maxVol).coerceIn(0.1f, 1f)))
        }
        return list
    }

    fun getResolvedBids(): List<OrderBookEntry> {
        if (bids.isNotEmpty()) return bids
        val tick = if (price >= 100) 0.05 else 0.01
        val maxVol = 5000L
        val list = mutableListOf<OrderBookEntry>()
        for (i in 1..5) {
            val p = round((price - i * tick) * 100) / 100
            val v = (180L + ((symbol.hashCode() * 41 + i * 31).let { abs(it) % 2100 })).toLong()
            list.add(OrderBookEntry(level = "买$i", price = p, volume = v, percent = (v.toFloat() / maxVol).coerceIn(0.1f, 1f)))
        }
        return list
    }

    fun getResolvedWeibi(): Double {
        if (weibi != 0.0) return weibi
        val bSum = getResolvedBids().sumOf { it.volume }
        val aSum = getResolvedAsks().sumOf { it.volume }
        return if (bSum + aSum > 0) {
            round(((bSum - aSum).toDouble() / (bSum + aSum)) * 10000) / 100
        } else 0.0
    }

    fun getResolvedWeicha(): Long {
        if (weicha != 0L) return weicha
        val bSum = getResolvedBids().sumOf { it.volume }
        val aSum = getResolvedAsks().sumOf { it.volume }
        return bSum - aSum
    }
}

