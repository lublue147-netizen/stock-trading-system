package com.stockmarket.app.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Default placeholder URL (can be customized via settings to user's Cloudflare Worker)
    const val DEFAULT_BASE_URL = "https://stock-trading-worker.lublue147.workers.dev/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val fastOkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(4000, TimeUnit.MILLISECONDS)
        .build()

    val ultraFastOkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(1500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .build()

    /**
     * Dedicated OkHttpClient for EastMoney & domestic financial APIs.
     * Enforces HTTP/1.1 to eliminate buggy HTTP/2 stream multiplexing drops on domestic CDNs/gateways,
     * and enables aggressive retry on connection failures to prevent "unexpected end of stream".
     */
    val eastMoneyOkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .retryOnConnectionFailure(true)
        .connectTimeout(4000, TimeUnit.MILLISECONDS)
        .readTimeout(8000, TimeUnit.MILLISECONDS)
        .connectionPool(okhttp3.ConnectionPool(4, 15, TimeUnit.SECONDS))
        .build()

    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var cachedService: StockApiService? = null

    @Synchronized
    fun getService(baseUrl: String = currentBaseUrl): StockApiService {
        val sanitized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (cachedService != null && currentBaseUrl == sanitized) {
            return cachedService!!
        }

        currentBaseUrl = sanitized
        val retrofit = Retrofit.Builder()
            .baseUrl(sanitized)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val service = retrofit.create(StockApiService::class.java)
        cachedService = service
        return service
    }
}
