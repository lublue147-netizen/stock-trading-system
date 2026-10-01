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
        .readTimeout(12, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
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
