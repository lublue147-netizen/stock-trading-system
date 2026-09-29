package com.stockmarket.app.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmarket.app.data.model.OrderBookEntry
import com.stockmarket.app.data.model.StockQuote
import com.stockmarket.app.ui.components.CandlestickChart
import com.stockmarket.app.ui.components.ChartType
import com.stockmarket.app.ui.components.TimeframeSelector
import com.stockmarket.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: StockDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val stockColors = LocalStockColors.current
    val quote = state.quote
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isGridExpanded by remember { mutableStateOf(false) }

    // Market trading status string (A-Share CST: 09:30-11:30, 13:00-15:00)
    val marketStatus = remember {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeVal = hour * 100 + minute

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            "休市 (周末)"
        } else if (timeVal in 930..1130 || timeVal in 1300..1500) {
            "交易中"
        } else if (timeVal in 915 until 930) {
            "盘前竞价"
        } else {
            "已收盘 15:00"
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = quote?.name?.ifEmpty { state.symbol } ?: state.symbol,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Exchange Badge
                            val badge = quote?.exchangeBadge ?: "沪"
                            val badgeBg = when (badge) {
                                "沪" -> Color(0xFFDC2626).copy(alpha = 0.2f)
                                "深" -> Color(0xFF2563EB).copy(alpha = 0.2f)
                                "北" -> Color(0xFF059669).copy(alpha = 0.2f)
                                "港" -> Color(0xFF9333EA).copy(alpha = 0.2f)
                                else -> Color(0xFF4B5563).copy(alpha = 0.2f)
                            }
                            val badgeColor = when (badge) {
                                "沪" -> Color(0xFFF87171)
                                "深" -> Color(0xFF60A5FA)
                                "北" -> Color(0xFF34D399)
                                "港" -> Color(0xFFC084FC)
                                else -> Color(0xFF9CA3AF)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(badge, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = state.symbol.substringBefore("."),
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotColor = if (marketStatus.startsWith("交易中")) stockColors.upColor else TextMuted
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$marketStatus · 人民币 CNY",
                                color = TextSecondary,
                                fontSize = 11.sp
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
                    IconButton(onClick = {
                        viewModel.loadQuote()
                        viewModel.loadHistory(state.selectedRange)
                        scope.launch {
                            snackbarHostState.showSnackbar("行情数据已刷新")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "刷新",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        bottomBar = {
            // Sticky Bottom Action Bar (East Money Style)
            Surface(
                color = SurfaceDark,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Watchlist Button
                    Button(
                        onClick = { viewModel.toggleWatchlist() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isWatchlisted) Color(0xFF1E293B) else PrimaryBlue
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(
                            imageVector = if (state.isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = if (state.isWatchlisted) Color(0xFFFBBF24) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isWatchlisted) "已在自选" else "加自选",
                            fontSize = 13.sp,
                            color = if (state.isWatchlisted) TextPrimary else Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Alert Button
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("已成功设置 ${quote?.name ?: state.symbol} 涨跌预警")
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("设预警", fontSize = 13.sp, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Refresh Button
                    IconButton(
                        onClick = {
                            viewModel.loadQuote()
                            viewModel.loadHistory(state.selectedRange)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "刷新",
                            tint = TextSecondary
                        )
                    }
                }
            }
        },
        containerColor = BgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // Mega Price & Limit Badges (East Money Top Header)
            if (quote != null) {
                val isPositive = quote.change >= 0
                val priceColor = if (isPositive) stockColors.upColor else stockColors.downColor
                val prefix = if (isPositive) "+" else ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val currSign = when (quote.currency) {
                            "CNY" -> "¥"
                            "HKD" -> "HK$"
                            "点" -> ""
                            else -> "$"
                        }
                        Text(
                            text = "$currSign${String.format(Locale.US, "%.2f", quote.price)}",
                            color = priceColor,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${prefix}${String.format(Locale.US, "%.2f", quote.change)}",
                                color = priceColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${prefix}${String.format(Locale.US, "%.2f%%", quote.changePercent)}",
                                color = priceColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Limit Up / Limit Down Cards
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("涨停 ", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = String.format(Locale.US, "%.2f", quote.computedLimitUp),
                                color = stockColors.upColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("跌停 ", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = String.format(Locale.US, "%.2f", quote.computedLimitDown),
                                color = stockColors.downColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // East Money 盘口指标网格 (12-Metric Grid)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Row 1
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val openColor = when {
                                quote.open > quote.previousClose -> stockColors.upColor
                                quote.open < quote.previousClose -> stockColors.downColor
                                else -> TextPrimary
                            }
                            CompactMetric("今开", String.format(Locale.US, "%.2f", quote.open), openColor, Modifier.weight(1f))
                            CompactMetric("最高", String.format(Locale.US, "%.2f", quote.high), stockColors.upColor, Modifier.weight(1f))
                            CompactMetric("涨停", String.format(Locale.US, "%.2f", quote.computedLimitUp), stockColors.upColor, Modifier.weight(1f))
                            CompactMetric("换手", "${String.format(Locale.US, "%.2f", quote.computedTurnoverRate)}%", TextPrimary, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Row 2
                        Row(modifier = Modifier.fillMaxWidth()) {
                            CompactMetric("昨收", String.format(Locale.US, "%.2f", quote.previousClose), TextSecondary, Modifier.weight(1f))
                            CompactMetric("最低", String.format(Locale.US, "%.2f", quote.low), stockColors.downColor, Modifier.weight(1f))
                            CompactMetric("跌停", String.format(Locale.US, "%.2f", quote.computedLimitDown), stockColors.downColor, Modifier.weight(1f))
                            CompactMetric("成交量", quote.formattedVolume, TextPrimary, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Row 3
                        Row(modifier = Modifier.fillMaxWidth()) {
                            CompactMetric("成交额", quote.formattedTurnoverAmount, TextPrimary, Modifier.weight(1f))
                            CompactMetric("振幅", "${String.format(Locale.US, "%.2f", quote.computedAmplitude)}%", TextPrimary, Modifier.weight(1f))
                            CompactMetric("市盈(动)", quote.peRatio?.let { String.format(Locale.US, "%.2f", it) } ?: "--", TextPrimary, Modifier.weight(1f))
                            CompactMetric("市净率", quote.pbRatio?.let { String.format(Locale.US, "%.2f", it) } ?: "--", TextPrimary, Modifier.weight(1f))
                        }

                        // Expandable Row 4 (Financial & Market Cap Details)
                        AnimatedVisibility(visible = isGridExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    CompactMetric("总市值", quote.formattedMarketCap, TextPrimary, Modifier.weight(1f))
                                    CompactMetric("流通值", quote.formattedFloatCap, TextPrimary, Modifier.weight(1f))
                                    CompactMetric("每股收益", quote.eps?.let { "${String.format(Locale.US, "%.2f", it)}元" } ?: "--", TextPrimary, Modifier.weight(1f))
                                    val weibi = quote.getResolvedWeibi()
                                    val weibiColor = if (weibi >= 0) stockColors.upColor else stockColors.downColor
                                    CompactMetric("委比", "${if (weibi >= 0) "+" else ""}${String.format(Locale.US, "%.2f%%", weibi)}", weibiColor, Modifier.weight(1f))
                                }
                            }
                        }

                        // Toggle Expand Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isGridExpanded = !isGridExpanded }
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isGridExpanded) "收起盘口指标" else "展开更多盘口指标",
                                color = PrimaryBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (isGridExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else if (state.isLoadingQuote) {
                Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timeframe Selector Tabs (分时 | 五日 | 日K | 周K | 月K | 全部)
            TimeframeSelector(
                selectedRange = state.selectedRange,
                onRangeSelected = { viewModel.loadHistory(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chart Indicator Sub-bar (K-Line vs Intraday, MA Toggle)
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
                            .background(if (state.chartType == ChartType.LINE) SurfaceBorder else Color.Transparent)
                            .clickable { viewModel.setChartType(ChartType.LINE) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "分时走势",
                            color = if (state.chartType == ChartType.LINE) TextPrimary else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (state.chartType == ChartType.CANDLESTICK) SurfaceBorder else Color.Transparent)
                            .clickable { viewModel.setChartType(ChartType.CANDLESTICK) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "K线图",
                            color = if (state.chartType == ChartType.CANDLESTICK) TextPrimary else TextMuted,
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
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "均线指标 (MA)",
                        color = if (state.showMA) PrimaryBlue else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Interactive Canvas Chart Area
            if (state.isLoadingChart && state.historicalData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
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
                    previousClose = quote?.previousClose,
                    showMA = state.showMA
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // East Money Hallmark: 买卖五档盘口 (Five-Level Order Book)
            if (quote != null) {
                OrderBookCard(quote = quote, stockColors = stockColors)
                Spacer(modifier = Modifier.height(14.dp))
            }

            // F10 简况与资讯卡片 (Company Profile & Financial Highlights)
            if (quote != null) {
                F10ProfileCard(quote = quote)
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun CompactMetric(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, color = TextMuted, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun OrderBookCard(
    quote: StockQuote,
    stockColors: StockColors
) {
    val asks = quote.getResolvedAsks()
    val bids = quote.getResolvedBids()
    val weibi = quote.getResolvedWeibi()
    val weicha = quote.getResolvedWeicha()
    val weibiColor = if (weibi >= 0) stockColors.upColor else stockColors.downColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "买卖五档盘口",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Level-1 实时深度",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "单位: 手",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Asks (卖五 ~ 卖一)
            asks.forEach { item ->
                OrderBookRow(
                    entry = item,
                    priceColor = stockColors.downColor,
                    barColor = stockColors.downColor.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(3.dp))
            }

            // Divider & 委比/委差 Center Bar
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceBorder.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "委比: ${if (weibi >= 0) "+" else ""}${String.format(Locale.US, "%.2f%%", weibi)}",
                    color = weibiColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "委差: ${if (weicha >= 0) "+" else ""}${weicha}手",
                    color = weibiColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Bids (买一 ~ 买五)
            bids.forEach { item ->
                OrderBookRow(
                    entry = item,
                    priceColor = stockColors.upColor,
                    barColor = stockColors.upColor.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}

@Composable
private fun OrderBookRow(
    entry: OrderBookEntry,
    priceColor: Color,
    barColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(3.dp))
    ) {
        // Visual Depth Bar (right-aligned)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = entry.percent)
                .align(Alignment.CenterEnd)
                .background(barColor)
        )

        // Text Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.level,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format(Locale.US, "%.2f", entry.price),
                color = priceColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${entry.volume}",
                color = TextPrimary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun F10ProfileCard(quote: StockQuote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "F10 资料与公司简况",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = quote.exchange.ifEmpty { "A股主板" },
                    color = PrimaryBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Industry & Main Business
            quote.industry?.let { ind ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("所属行业: ", color = TextMuted, fontSize = 11.sp)
                    Text(ind, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            quote.mainBusiness?.let { biz ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("主营业务: ", color = TextMuted, fontSize = 11.sp)
                    Text(
                        biz,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Concepts Chips Flow
            if (quote.conceptTags.isNotEmpty()) {
                Text("概念题材:", color = TextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quote.conceptTags.take(4).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceBorder.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(tag, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Key Financial Metrics Grid
            HorizontalDivider(color = SurfaceBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                CompactMetric("每股收益", quote.eps?.let { "${String.format(Locale.US, "%.2f", it)}元" } ?: "--", TextPrimary, Modifier.weight(1f))
                CompactMetric("每股净资产", quote.bps?.let { "${String.format(Locale.US, "%.2f", it)}元" } ?: "--", TextPrimary, Modifier.weight(1f))
                CompactMetric("净资产收益率", quote.roe?.let { "${String.format(Locale.US, "%.2f%%", it)}" } ?: "--", TextPrimary, Modifier.weight(1f))
            }
        }
    }
}

