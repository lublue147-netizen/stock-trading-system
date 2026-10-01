package com.stockmarket.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HistoryMeta(
    @Json(name = "currency") val currency: String? = null,
    @Json(name = "previousClose") val previousClose: Double? = null,
    @Json(name = "high") val high: Double? = null,
    @Json(name = "low") val low: Double? = null,
    @Json(name = "selectedDate") val selectedDate: String? = null,
    @Json(name = "availableDates") val availableDates: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class HistoricalData(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "range") val range: String = "1mo",
    @Json(name = "interval") val interval: String? = null,
    @Json(name = "candles") val candles: List<CandlePoint> = emptyList(),
    @Json(name = "meta") val meta: HistoryMeta? = null
)
