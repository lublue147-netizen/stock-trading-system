package com.stockmarket.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmarket.app.ui.components.CandlestickChart
import com.stockmarket.app.ui.components.ChartType
import com.stockmarket.app.ui.components.TimeframeSelector
import com.stockmarket.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: StockDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val stockColors = LocalStockColors.current
    val quote = state.quote

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.symbol,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        quote?.name?.takeIf { it.isNotEmpty() }?.let {
                            Text(
                                text = it,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWatchlist() }) {
                        Icon(
                            imageVector = if (state.isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (state.isWatchlisted) "移出自选" else "加入自选",
                            tint = if (state.isWatchlisted) Color(0xFFFBBF24) else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark
                )
            )
        },
        containerColor = BgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Price & Change Overview
            if (quote != null) {
                val isPositive = quote.change >= 0
                val color = if (isPositive) stockColors.upColor else stockColors.downColor
                val prefix = if (isPositive) "+" else ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${quote.currency} ${String.format(Locale.US, "%.2f", quote.price)}",
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${prefix}${String.format(Locale.US, "%.2f", quote.change)}",
                                color = color,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${prefix}${String.format(Locale.US, "%.2f%%", quote.changePercent)})",
                                color = color,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Watchlist status badge button
                    Button(
                        onClick = { viewModel.toggleWatchlist() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isWatchlisted) SurfaceBorder else PrimaryBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (state.isWatchlisted) "已在自选" else "+ 加入自选",
                            fontSize = 12.sp,
                            color = if (state.isWatchlisted) TextSecondary else Color.White
                        )
                    }
                }
            } else if (state.isLoadingQuote) {
                CircularProgressIndicator(
                    color = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timeframe Selector (1D, 5D, 1M, 6M, 1Y, ALL)
            TimeframeSelector(
                selectedRange = state.selectedRange,
                onRangeSelected = { viewModel.loadHistory(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Controls (K-line vs Line, MA Indicator toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chart Type Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (state.chartType == ChartType.CANDLESTICK) SurfaceBorder else Color.Transparent)
                            .clickable { if (state.chartType != ChartType.CANDLESTICK) viewModel.toggleChartType() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "K线图",
                            color = if (state.chartType == ChartType.CANDLESTICK) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (state.chartType == ChartType.LINE) SurfaceBorder else Color.Transparent)
                            .clickable { if (state.chartType != ChartType.LINE) viewModel.toggleChartType() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "分时走势",
                            color = if (state.chartType == ChartType.LINE) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // MA Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.toggleMA() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "均线指标 (MA)",
                        color = if (state.showMA) PrimaryBlue else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Canvas Candlestick & Volume Chart
            if (state.isLoadingChart && state.historicalData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                CandlestickChart(
                    candles = state.historicalData?.candles ?: emptyList(),
                    chartType = state.chartType,
                    showMA = state.showMA
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Key Financial Stats & Overview Grid
            if (quote != null) {
                Text(
                    text = "行情关键指标",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetricItem(label = "今开", value = String.format(Locale.US, "%.2f", quote.open), modifier = Modifier.weight(1f))
                            MetricItem(label = "昨收", value = String.format(Locale.US, "%.2f", quote.previousClose), modifier = Modifier.weight(1f))
                            MetricItem(label = "最高", value = String.format(Locale.US, "%.2f", quote.high), modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetricItem(label = "最低", value = String.format(Locale.US, "%.2f", quote.low), modifier = Modifier.weight(1f))
                            MetricItem(
                                label = "成交量",
                                value = formatVolume(quote.volume),
                                modifier = Modifier.weight(1f)
                            )
                            MetricItem(
                                label = "52周最高",
                                value = quote.fiftyTwoWeekHigh?.let { String.format(Locale.US, "%.2f", it) } ?: "--",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetricItem(
                                label = "52周最低",
                                value = quote.fiftyTwoWeekLow?.let { String.format(Locale.US, "%.2f", it) } ?: "--",
                                modifier = Modifier.weight(1f)
                            )
                            MetricItem(
                                label = "市盈率 (P/E)",
                                value = quote.peRatio?.let { String.format(Locale.US, "%.2f", it) } ?: "--",
                                modifier = Modifier.weight(1f)
                            )
                            MetricItem(
                                label = "交易所",
                                value = quote.exchange.ifEmpty { "--" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatVolume(vol: Long): String {
    return when {
        vol >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", vol / 1_000_000_000.0)
        vol >= 1_000_000 -> String.format(Locale.US, "%.2fM", vol / 1_000_000.0)
        vol >= 1_000 -> String.format(Locale.US, "%.1fK", vol / 1_000.0)
        else -> vol.toString()
    }
}
