package com.stockmarket.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StockQuote(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "name") val name: String = "",
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "change") val change: Double = 0.0,
    @Json(name = "changePercent") val changePercent: Double = 0.0,
    @Json(name = "currency") val currency: String = "USD",
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
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
