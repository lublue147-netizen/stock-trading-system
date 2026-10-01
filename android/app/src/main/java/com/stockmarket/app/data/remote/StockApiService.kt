package com.stockmarket.app.data.remote

import com.stockmarket.app.data.model.HistoricalData
import com.stockmarket.app.data.model.MarketIndex
import com.stockmarket.app.data.model.SearchResult
import com.stockmarket.app.data.model.StockQuote
import retrofit2.http.GET
import retrofit2.http.Query

interface StockApiService {
    @GET("api/quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String
    ): StockQuote

    @GET("api/quotes")
    suspend fun getQuotes(
        @Query("symbols") symbols: String
    ): List<StockQuote>

    @GET("api/history")
    suspend fun getHistory(
        @Query("symbol") symbol: String,
        @Query("range") range: String = "1mo",
        @Query("interval") interval: String? = null,
        @Query("date") date: String? = null
    ): HistoricalData

    @GET("api/search")
    suspend fun searchStocks(
        @Query("q") query: String
    ): List<SearchResult>

    @GET("api/market/indices")
    suspend fun getMarketIndices(): List<MarketIndex>

    @GET("api/health")
    suspend fun checkHealth(): Map<String, Any>
}
