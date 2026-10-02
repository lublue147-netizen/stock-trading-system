package com.stockmarket.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.stockmarket.app.data.local.WatchlistPreferences
import com.stockmarket.app.data.repository.StockRepository

class StockApp : Application() {
    lateinit var preferences: WatchlistPreferences
        private set
    lateinit var repository: StockRepository
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        } catch (_: Exception) {}
        preferences = WatchlistPreferences(this)
        repository = StockRepository(preferences)
    }
}
