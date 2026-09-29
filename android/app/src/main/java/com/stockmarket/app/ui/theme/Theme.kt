package com.stockmarket.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class StockColors(
    val upColor: Color,
    val downColor: Color
)

val LocalStockColors = compositionLocalOf {
    StockColors(upColor = UpRed, downColor = DownGreen)
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = AccentCyan,
    onSecondary = Color.Black,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary
)

@Composable
fun StockTradingAppTheme(
    colorSchemeMode: String = "CN",
    content: @Composable () -> Unit
) {
    val stockColors = if (colorSchemeMode == "US") {
        StockColors(upColor = UpGreen, downColor = DownRed)
    } else {
        StockColors(upColor = UpRed, downColor = DownGreen)
    }

    CompositionLocalProvider(LocalStockColors provides stockColors) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
