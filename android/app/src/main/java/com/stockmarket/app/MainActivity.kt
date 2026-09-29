package com.stockmarket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.stockmarket.app.ui.navigation.AppNavigation
import com.stockmarket.app.ui.theme.StockTradingAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as StockApp

        setContent {
            val colorScheme = app.preferences.getColorScheme()
            StockTradingAppTheme(colorSchemeMode = colorScheme) {
                AppNavigation(
                    preferences = app.preferences,
                    repository = app.repository
                )
            }
        }
    }
}
