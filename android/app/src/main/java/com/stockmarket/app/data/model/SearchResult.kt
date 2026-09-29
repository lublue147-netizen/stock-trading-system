package com.stockmarket.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResult(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "name") val name: String,
    @Json(name = "exchange") val exchange: String = "",
    @Json(name = "type") val type: String = "EQUITY"
)
