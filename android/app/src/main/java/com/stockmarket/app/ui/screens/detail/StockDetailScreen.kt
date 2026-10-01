package com.stockmarket.app.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.stockmarket.app.data.model.CandlePoint
import com.stockmarket.app.data.model.OrderBookEntry
import com.stockmarket.app.data.model.StockQuote
import com.stockmarket.app.data.model.TickTransaction
import com.stockmarket.app.ui.components.CandlestickChart
import com.stockmarket.app.ui.components.ChartType
import com.stockmarket.app.ui.components.TimeframeSelector
import com.stockmarket.app.ui.components.formatVolumeInLots
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
    var selectedSubTab by remember { mutableStateOf(0) } // 0: 五档, 1: 资金, 2: 简况, 3: 明细

    // Interactive Dialog states
    var showTradeDialog by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var showDiagnosisDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedKLineCandle by remember { mutableStateOf<CandlePoint?>(null) }

    // Market trading status string (A-Share CST: 09:30-11:30, 13:00-15:00)
    val marketStatus = remember {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val timeVal = hour * 100 + minute
        val timeStr = String.format(Locale.US, "%02d:%02d:%02d", hour, minute, second)

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            "休市 (周末)"
        } else if (timeVal in 930..1130 || timeVal in 1300..1500) {
            "交易中 $timeStr"
        } else if (timeVal in 915 until 930) {
            "盘前竞价 $timeStr"
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
                            // Exchange Badge (深A / 沪A / 京A)
                            val badge = quote?.exchangeBadge ?: "沪A"
                            val badgeBg = when {
                                badge.startsWith("沪") -> EastMoneyRed.copy(alpha = 0.2f)
                                badge.startsWith("深") -> Color(0xFF2563EB).copy(alpha = 0.2f)
                                badge.startsWith("京") || badge.startsWith("北") -> Color(0xFF059669).copy(alpha = 0.2f)
                                badge.startsWith("港") -> Color(0xFF9333EA).copy(alpha = 0.2f)
                                else -> Color(0xFF4B5563).copy(alpha = 0.2f)
                            }
                            val badgeColor = when {
                                badge.startsWith("沪") -> Color(0xFFF87171)
                                badge.startsWith("深") -> Color(0xFF60A5FA)
                                badge.startsWith("京") || badge.startsWith("北") -> Color(0xFF34D399)
                                badge.startsWith("港") -> Color(0xFFC084FC)
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
                            Spacer(modifier = Modifier.width(6.dp))
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
                                    .clip(CircleShape)
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
                            tint = if (state.isWatchlisted) EastMoneyYellow else TextSecondary
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
            // East Money Signature 4-Item Sticky Action Bar
            Surface(
                color = EastMoneySurface,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EastMoneyBorder, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. 自选
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { viewModel.toggleWatchlist() }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = if (state.isWatchlisted) EastMoneyYellow else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (state.isWatchlisted) "已在自选" else "加自选",
                            fontSize = 10.sp,
                            color = if (state.isWatchlisted) EastMoneyYellow else TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 2. 设预警
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { showAlertDialog = true }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "设预警",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 3. 智能诊股
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { showDiagnosisDialog = true }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = EastMoneyOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "智能诊股",
                            fontSize = 10.sp,
                            color = EastMoneyOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 4. 下单 / 买入 (East Money Iconic Red Button)
                    Button(
                        onClick = { showTradeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EastMoneyRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .padding(start = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShowChart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "买入 / 交易",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
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

                // East Money 4-Column Metric Grid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EastMoneyCard),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EastMoneyBorder)
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

                        // Expandable Rows (Financial, Market Cap, Outer/Inner Disk)
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    CompactMetric("每股净资产", quote.bps?.let { "${String.format(Locale.US, "%.2f", it)}元" } ?: "--", TextPrimary, Modifier.weight(1f))
                                    CompactMetric("净资产收益率", quote.roe?.let { "${String.format(Locale.US, "%.2f%%", it)}" } ?: "--", TextPrimary, Modifier.weight(1f))
                                    CompactMetric("外盘(买盘)", quote.formattedOuterDisk, stockColors.upColor, Modifier.weight(1f))
                                    CompactMetric("内盘(卖盘)", quote.formattedInnerDisk, stockColors.downColor, Modifier.weight(1f))
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
                                color = EastMoneyOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (isGridExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = EastMoneyOrange,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else if (state.isLoadingQuote) {
                Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EastMoneyRed, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timeframe Selector Tabs (分时 | 五日 | 日K | 周K | 月K | 全部)
            TimeframeSelector(
                selectedRange = state.selectedRange,
                onRangeSelected = {
                    selectedKLineCandle = null
                    viewModel.loadHistory(it)
                }
            )

            // When in 1d intraday mode, show East Money Historical Date Navigation Bar
            if (state.selectedRange == "1d") {
                Spacer(modifier = Modifier.height(8.dp))
                HistoricalIntradayDateBar(
                    selectedDate = state.selectedIntradayDate,
                    availableDates = state.availableIntradayDates,
                    isHistorical = state.isHistoricalIntraday,
                    historicalPrevClose = state.historicalPreviousClose,
                    candles = state.historicalData?.candles ?: emptyList(),
                    previousKLineRange = state.previousKLineRange,
                    onPreviousDay = { viewModel.stepDate(-1) },
                    onNextDay = { viewModel.stepDate(1) },
                    onSelectDateClick = { showDatePickerDialog = true },
                    onResetToday = { viewModel.resetToToday() },
                    onReturnToKLine = if (state.previousKLineRange != null) { { viewModel.returnToKLine() } } else null
                )
            } else {
                // When in K-Line mode (日K/周K/月K), show East Money K-Line to Intraday Linkage Bar
                Spacer(modifier = Modifier.height(8.dp))
                KLineIntradayLinkBar(
                    selectedCandle = selectedKLineCandle ?: state.historicalData?.candles?.lastOrNull(),
                    allCandles = state.historicalData?.candles ?: emptyList(),
                    availableDates = state.availableIntradayDates,
                    onViewIntraday = { dateStr ->
                        viewModel.viewIntradayFromKLine(dateStr, fromRange = state.selectedRange)
                    }
                )
            }

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
                        .background(EastMoneySurface)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (state.chartType == ChartType.LINE) EastMoneyBorder else Color.Transparent)
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
                            .background(if (state.chartType == ChartType.CANDLESTICK) EastMoneyBorder else Color.Transparent)
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
                        color = if (state.showMA) EastMoneyOrange else TextMuted,
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
                        .background(EastMoneySurface),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EastMoneyRed)
                }
            } else {
                val effectivePreviousClose = if (state.selectedRange == "1d" && state.isHistoricalIntraday && state.historicalPreviousClose != null) {
                    state.historicalPreviousClose
                } else {
                    quote?.previousClose
                }
                CandlestickChart(
                    candles = state.historicalData?.candles ?: emptyList(),
                    chartType = state.chartType,
                    previousClose = effectivePreviousClose,
                    showMA = state.showMA,
                    onCandleSelected = { candle ->
                        selectedKLineCandle = candle
                    },
                    onViewIntradayForDate = { dateStr ->
                        viewModel.viewIntradayFromKLine(dateStr, fromRange = state.selectedRange)
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // East Money Lower Sub-views: 4 Tabs Switcher
            // 【五档盘口】 【主力资金】 【个股简况】 【分笔明细】
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EastMoneySurface)
                    .border(1.dp, EastMoneyBorder, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val subTabs = listOf("五档盘口", "主力资金", "个股简况", "分笔明细")
                subTabs.forEachIndexed { index, title ->
                    val isSelected = selectedSubTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) EastMoneyRed else Color.Transparent)
                            .clickable { selectedSubTab = index }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-view Tab Contents
            if (quote != null) {
                when (selectedSubTab) {
                    0 -> OrderBookCard(quote = quote, stockColors = stockColors)
                    1 -> CapitalFlowCard(quote = quote, stockColors = stockColors)
                    2 -> F10ProfileCard(quote = quote)
                    3 -> TickByTickCard(quote = quote, stockColors = stockColors)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Dialog 1: 模拟交易委托下单 (East Money Style)
    if (showTradeDialog && quote != null) {
        TradeDialog(
            quote = quote,
            onDismiss = { showTradeDialog = false },
            onConfirmOrder = { isBuy, orderPrice, lots ->
                showTradeDialog = false
                scope.launch {
                    val action = if (isBuy) "买入" else "卖出"
                    snackbarHostState.showSnackbar("模拟委托已成功提交: $action ${quote.name} ${lots}手, 委托价: ¥${String.format(Locale.US, "%.2f", orderPrice)}")
                }
            }
        )
    }

    // Modal Dialog 2: 智能行情预警设置
    if (showAlertDialog && quote != null) {
        PriceAlertDialog(
            quote = quote,
            onDismiss = { showAlertDialog = false },
            onSaveAlert = { upTarget, downTarget ->
                showAlertDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("预警已生效: 上涨至 ¥$upTarget / 下跌至 ¥$downTarget 时将实时提醒")
                }
            }
        )
    }

    // Modal Dialog 3: 东方财富智能诊股
    if (showDiagnosisDialog && quote != null) {
        StockDiagnosisDialog(
            quote = quote,
            stockColors = stockColors,
            onDismiss = { showDiagnosisDialog = false }
        )
    }

    // Modal Dialog 4: 历史分时交易日选择器
    if (showDatePickerDialog) {
        HistoricalDatePickerDialog(
            availableDates = state.availableIntradayDates,
            selectedDate = state.selectedIntradayDate,
            onDateSelected = { date ->
                viewModel.selectHistoricalDate(date)
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
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

// Tab 0: 买卖五档盘口
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
    val outerDisk = quote.computedOuterDisk
    val innerDisk = quote.computedInnerDisk
    val totalDisk = (outerDisk + innerDisk).coerceAtLeast(1L)
    val outerRatio = (outerDisk.toFloat() / totalDisk).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EastMoneyCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EastMoneyBorder)
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
                    .background(EastMoneySurface)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
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

            Spacer(modifier = Modifier.height(10.dp))

            // 外盘 (主动买盘) vs 内盘 (主动卖盘) Comparison Bar
            HorizontalDivider(color = EastMoneyBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "外盘(买): ${quote.formattedOuterDisk}",
                    color = stockColors.upColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "内盘(卖): ${quote.formattedInnerDisk}",
                    color = stockColors.downColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Dual-color progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(stockColors.downColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = outerRatio)
                        .background(stockColors.upColor)
                )
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

// Tab 1: 主力资金动向
@Composable
private fun CapitalFlowCard(
    quote: StockQuote,
    stockColors: StockColors
) {
    val flow = remember(quote.symbol, quote.price, quote.volume) { quote.getResolvedCapitalFlow() }
    val isNetPositive = flow.mainNetInflowWan >= 0
    val mainNetColor = if (isNetPositive) stockColors.upColor else stockColors.downColor
    val prefix = if (isNetPositive) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EastMoneyCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EastMoneyBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日主力资金动向",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "超大单+大单净额",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Net Inflow Big Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EastMoneySurface)
                    .padding(12.dp)
            ) {
                Column {
                    Text(text = "今日主力净流入", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$prefix${String.format(Locale.US, "%.2f", flow.mainNetInflowWan)} 万元",
                        color = mainNetColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Tiers Breakdown: 超大单, 大单, 中单, 小单
            Text(text = "各档资金净流向分布", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            val maxAbs = maxOf(
                abs(flow.superLargeNetWan),
                abs(flow.largeNetWan),
                abs(flow.mediumNetWan),
                abs(flow.smallNetWan),
                1.0
            )

            CapitalTierRow("超大单 (>100万)", flow.superLargeNetWan, maxAbs, stockColors)
            Spacer(modifier = Modifier.height(6.dp))
            CapitalTierRow("大单 (20~100万)", flow.largeNetWan, maxAbs, stockColors)
            Spacer(modifier = Modifier.height(6.dp))
            CapitalTierRow("中单 (4~20万)", flow.mediumNetWan, maxAbs, stockColors)
            Spacer(modifier = Modifier.height(6.dp))
            CapitalTierRow("小单 (<4万 散户)", flow.smallNetWan, maxAbs, stockColors)

            Spacer(modifier = Modifier.height(12.dp))

            // Evaluation Box
            HorizontalDivider(color = EastMoneyBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = EastMoneyOrange,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = flow.evaluation,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun CapitalTierRow(
    label: String,
    netWan: Double,
    maxAbs: Double,
    stockColors: StockColors
) {
    val isPos = netWan >= 0
    val color = if (isPos) stockColors.upColor else stockColors.downColor
    val ratio = (abs(netWan) / maxAbs).toFloat().coerceIn(0.08f, 1f)
    val prefix = if (isPos) "+" else ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.width(105.dp)
        )
        // Ratio Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(EastMoneySurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = ratio)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "$prefix${String.format(Locale.US, "%.1f", netWan)}万",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.End
        )
    }
}

// Tab 2: F10 个股简况
@Composable
private fun F10ProfileCard(quote: StockQuote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EastMoneyCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EastMoneyBorder)
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
                    color = EastMoneyOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Industry
            quote.industry?.let { ind ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("所属行业: ", color = TextMuted, fontSize = 11.sp)
                    Text(ind, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Main Business
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
                                .background(EastMoneySurface)
                                .border(0.8.dp, EastMoneyBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(tag, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Key Financial Metrics Grid
            HorizontalDivider(color = EastMoneyBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                CompactMetric("每股收益", quote.eps?.let { "${String.format(Locale.US, "%.2f", it)}元" } ?: "--", TextPrimary, Modifier.weight(1f))
                CompactMetric("每股净资产", quote.bps?.let { "${String.format(Locale.US, "%.2f", it)}元" } ?: "--", TextPrimary, Modifier.weight(1f))
                CompactMetric("净资产收益率", quote.roe?.let { "${String.format(Locale.US, "%.2f%%", it)}" } ?: "--", TextPrimary, Modifier.weight(1f))
            }
        }
    }
}

// Tab 3: 分笔明细流水
@Composable
private fun TickByTickCard(
    quote: StockQuote,
    stockColors: StockColors
) {
    val ticks = remember(quote.symbol, quote.price) { quote.getResolvedTicks() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EastMoneyCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EastMoneyBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "实时逐笔成交流水",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "单位: 手 / 盘向",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(EastMoneySurface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("时间", color = TextMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text("价格", color = TextMuted, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("手数", color = TextMuted, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("性质", color = TextMuted, fontSize = 10.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tick Rows
            ticks.forEach { tick ->
                val typeColor = when (tick.type) {
                    "B" -> stockColors.upColor
                    "S" -> stockColors.downColor
                    else -> TextSecondary
                }
                val typeText = when (tick.type) {
                    "B" -> "买盘 B"
                    "S" -> "卖盘 S"
                    else -> "平盘 -"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tick.time, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text(
                        String.format(Locale.US, "%.2f", tick.price),
                        color = typeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text("${tick.volume}", color = TextPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(typeText, color = typeColor, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = EastMoneyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

// Dialog 1: 模拟交易下单 (East Money Simulation Trade)
@Composable
private fun TradeDialog(
    quote: StockQuote,
    onDismiss: () -> Unit,
    onConfirmOrder: (isBuy: Boolean, price: Double, lots: Long) -> Unit
) {
    var isBuy by remember { mutableStateOf(true) }
    var orderPriceText by remember { mutableStateOf(String.format(Locale.US, "%.2f", quote.price)) }
    var lotsText by remember { mutableStateOf("10") } // 默认 10手 (1000股)
    val availableFunds = 100_000.0 // 模拟资金 10万元

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EastMoneyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, EastMoneyBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "模拟实盘委托", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${quote.name} (${quote.symbol.substringBefore(".")})", color = TextSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Buy / Sell Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(EastMoneySurface)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isBuy) EastMoneyRed else Color.Transparent)
                            .clickable { isBuy = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("买入", color = if (isBuy) Color.White else TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isBuy) Color(0xFF2563EB) else Color.Transparent)
                            .clickable { isBuy = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("卖出", color = if (!isBuy) Color.White else TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price Input with Stepper
                Text("委托价格 (元)", color = TextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val cur = orderPriceText.toDoubleOrNull() ?: quote.price
                            orderPriceText = String.format(Locale.US, "%.2f", (cur - 0.01).coerceAtLeast(0.01))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(42.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("-", fontSize = 18.sp, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = orderPriceText,
                        onValueChange = { orderPriceText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EastMoneyRed,
                            unfocusedBorderColor = EastMoneyBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = {
                            val cur = orderPriceText.toDoubleOrNull() ?: quote.price
                            orderPriceText = String.format(Locale.US, "%.2f", cur + 0.01)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(42.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+", fontSize = 18.sp, color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quantity Input
                Text("委托数量 (手, 1手=100股)", color = TextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = lotsText,
                    onValueChange = { lotsText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EastMoneyRed,
                        unfocusedBorderColor = EastMoneyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Ratio Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val p = orderPriceText.toDoubleOrNull() ?: quote.price
                    val maxLots = if (p > 0) ((availableFunds / (p * 100)).toLong()).coerceAtLeast(1L) else 10L
                    listOf(
                        "1/4仓" to (maxLots / 4).coerceAtLeast(1L),
                        "半仓" to (maxLots / 2).coerceAtLeast(1L),
                        "3/4仓" to (maxLots * 3 / 4).coerceAtLeast(1L),
                        "全仓" to maxLots
                    ).forEach { (label, lots) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(EastMoneySurface)
                                .border(0.8.dp, EastMoneyBorder, RoundedCornerShape(4.dp))
                                .clickable { lotsText = "$lots" }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calculation Summary
                val p = orderPriceText.toDoubleOrNull() ?: quote.price
                val l = lotsText.toLongOrNull() ?: 10L
                val totalCost = p * l * 100
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("模拟可用资金: ¥100,000.00", color = TextMuted, fontSize = 11.sp)
                    Text("预估金额: ¥${String.format(Locale.US, "%.2f", totalCost)}", color = EastMoneyOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Button
                Button(
                    onClick = {
                        val parsedP = orderPriceText.toDoubleOrNull() ?: quote.price
                        val parsedL = lotsText.toLongOrNull() ?: 10L
                        onConfirmOrder(isBuy, parsedP, parsedL)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isBuy) EastMoneyRed else Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = if (isBuy) "确认委托买入" else "确认委托卖出",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Dialog 2: 智能预警设置
@Composable
private fun PriceAlertDialog(
    quote: StockQuote,
    onDismiss: () -> Unit,
    onSaveAlert: (up: Double, down: Double) -> Unit
) {
    var upTargetText by remember { mutableStateOf(String.format(Locale.US, "%.2f", quote.price * 1.05)) }
    var downTargetText by remember { mutableStateOf(String.format(Locale.US, "%.2f", quote.price * 0.95)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EastMoneyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, EastMoneyBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "设置行情预警", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "当前现价: ¥${String.format(Locale.US, "%.2f", quote.price)}", color = TextSecondary, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(14.dp))

                Text("股价上涨至预警价 (元)", color = TextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = upTargetText,
                    onValueChange = { upTargetText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EastMoneyRed,
                        unfocusedBorderColor = EastMoneyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("股价下跌至预警价 (元)", color = TextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = downTargetText,
                    onValueChange = { downTargetText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EastMoneyRed,
                        unfocusedBorderColor = EastMoneyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val up = upTargetText.toDoubleOrNull() ?: (quote.price * 1.05)
                        val down = downTargetText.toDoubleOrNull() ?: (quote.price * 0.95)
                        onSaveAlert(up, down)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EastMoneyRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("保存预警设置", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Dialog 3: 东方财富智能诊股
@Composable
private fun StockDiagnosisDialog(
    quote: StockQuote,
    stockColors: StockColors,
    onDismiss: () -> Unit
) {
    val seed = abs(quote.symbol.hashCode())
    val score = 7.5 + ((seed % 20) / 10.0) // 7.5 ~ 9.4
    val supportPrice = quote.price * 0.96
    val resistancePrice = quote.price * 1.06

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EastMoneyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, EastMoneyBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = EastMoneyOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "东财 AI 智能诊股", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Score Badge Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(EastMoneySurface)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("综合健康评分", color = TextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", score),
                                    color = EastMoneyOrange,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(" / 10分", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(stockColors.upColor.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("建议: 多头增持", color = stockColors.upColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dimensions
                Text("维度剖析", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                DiagnosisItem("技术面", "均线呈多头排列，量价配合健康，短期具备向上突破动能。")
                Spacer(modifier = Modifier.height(6.dp))
                DiagnosisItem("资金面", "主力资金持续流入，超大单机构建仓意愿明确。")
                Spacer(modifier = Modifier.height(6.dp))
                DiagnosisItem("基本面", "所属 ${quote.industry ?: "行业"} 景气度持续向好，估值处于合理中枢区间。")

                Spacer(modifier = Modifier.height(12.dp))

                // Support and Resistance Levels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(EastMoneySurface)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("压力位", color = TextMuted, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("¥${String.format(Locale.US, "%.2f", resistancePrice)}", color = stockColors.downColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(EastMoneyBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("支撑位", color = TextMuted, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("¥${String.format(Locale.US, "%.2f", supportPrice)}", color = stockColors.upColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EastMoneyRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("完成", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DiagnosisItem(title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(EastMoneySurface)
                .border(0.8.dp, EastMoneyBorder, RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(title, color = EastMoneyOrange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

private fun formatDisplayDateWithWeekday(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }
        val date = sdf.parse(dateStr) ?: return dateStr
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).apply { time = date }
        val weekDay = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            Calendar.SUNDAY -> "周日"
            else -> ""
        }
        if (weekDay.isNotEmpty()) "$dateStr ($weekDay)" else dateStr
    } catch (_: Exception) {
        dateStr
    }
}

@Composable
private fun KLineIntradayLinkBar(
    selectedCandle: CandlePoint?,
    allCandles: List<CandlePoint>,
    availableDates: List<String>,
    onViewIntraday: (String) -> Unit
) {
    val stockColors = LocalStockColors.current
    val candle = selectedCandle ?: allCandles.lastOrNull()
    if (candle == null) return

    val sdfYmd = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }
    }
    val dateKey = remember(candle.timestamp) { sdfYmd.format(Date(candle.timestamp)) }
    val displayDate = remember(dateKey) { formatDisplayDateWithWeekday(dateKey) }
    val isUp = candle.close >= candle.open
    val color = if (isUp) stockColors.upColor else stockColors.downColor
    val deltaPct = if (candle.open > 0) ((candle.close - candle.open) / candle.open) * 100 else 0.0
    val prefix = if (isUp) "+" else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(EastMoneySurface)
            .border(1.dp, EastMoneyBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = EastMoneyOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "收: ${String.format(Locale.US, "%.2f", candle.close)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$prefix${String.format(Locale.US, "%.2f%%", deltaPct)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                    }
                    Text(
                        text = "高: ${String.format(Locale.US, "%.2f", candle.high)}  低: ${String.format(Locale.US, "%.2f", candle.low)}  量: ${formatVolumeInLots(candle.volume)}",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button: View Intraday for this K-line!
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(EastMoneyOrange.copy(alpha = 0.2f))
                    .border(1.dp, EastMoneyOrange.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .clickable { onViewIntraday(dateKey) }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "查看当日分时",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EastMoneyOrange
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = EastMoneyOrange,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "💡 拖动或长按十字光标可选定任意日K，双击K线可直接穿透查看该日分时",
            color = TextMuted.copy(alpha = 0.8f),
            fontSize = 9.5.sp
        )
    }
}

@Composable
private fun HistoricalIntradayDateBar(
    selectedDate: String?,
    availableDates: List<String>,
    isHistorical: Boolean,
    historicalPrevClose: Double?,
    candles: List<CandlePoint>,
    previousKLineRange: String? = null,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectDateClick: () -> Unit,
    onResetToday: () -> Unit,
    onReturnToKLine: (() -> Unit)? = null
) {
    val stockColors = LocalStockColors.current
    val currentDate = selectedDate ?: availableDates.lastOrNull() ?: "今日"
    val currentIndex = if (selectedDate != null) availableDates.indexOf(selectedDate) else availableDates.lastIndex
    val canGoPrev = availableDates.size > 1 && currentIndex > 0
    val canGoNext = availableDates.size > 1 && currentIndex in 0 until (availableDates.size - 1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(EastMoneySurface)
            .border(1.dp, if (isHistorical) EastMoneyOrange.copy(alpha = 0.5f) else EastMoneyBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Day Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (canGoPrev) Color(0xFF1E2433) else Color.Transparent)
                    .clickable(enabled = canGoPrev) { onPreviousDay() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "前一交易日",
                    tint = if (canGoPrev) TextSecondary else TextMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "前一日",
                    fontSize = 11.sp,
                    color = if (canGoPrev) TextSecondary else TextMuted.copy(alpha = 0.3f)
                )
            }

            // Center Date Selector Pill Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isHistorical) EastMoneyOrange.copy(alpha = 0.15f) else Color(0xFF222838))
                    .border(
                        1.dp,
                        if (isHistorical) EastMoneyOrange.copy(alpha = 0.6f) else EastMoneyBorder,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectDateClick() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = if (isHistorical) EastMoneyOrange else EastMoneyYellow,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = formatDisplayDateWithWeekday(currentDate),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHistorical) EastMoneyOrange else TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isHistorical) EastMoneyOrange else TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Right Group: Next Day Button, Return to Today Button, and Return to K-Line Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (canGoNext) Color(0xFF1E2433) else Color.Transparent)
                        .clickable(enabled = canGoNext) { onNextDay() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "后一日",
                        fontSize = 11.sp,
                        color = if (canGoNext) TextSecondary else TextMuted.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "后一交易日",
                        tint = if (canGoNext) TextSecondary else TextMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isHistorical) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EastMoneyRed.copy(alpha = 0.15f))
                            .border(1.dp, EastMoneyRed.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable { onResetToday() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "今日",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EastMoneyRed
                        )
                    }
                }

                if (onReturnToKLine != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EastMoneyOrange.copy(alpha = 0.18f))
                            .border(1.dp, EastMoneyOrange.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .clickable { onReturnToKLine() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = EastMoneyOrange,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "回K线",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EastMoneyOrange
                            )
                        }
                    }
                }
            }
        }

        // Historical Day Summary Row (when reviewing past date)
        if (isHistorical && candles.isNotEmpty()) {
            val lastClose = candles.last().close
            val refClose = historicalPrevClose ?: candles.first().open
            val delta = lastClose - refClose
            val deltaPct = if (refClose > 0) (delta / refClose) * 100 else 0.0
            val isUp = delta >= 0
            val color = if (isUp) stockColors.upColor else stockColors.downColor
            val sign = if (isUp) "+" else ""
            val high = candles.maxOfOrNull { it.high } ?: lastClose
            val low = candles.minOfOrNull { it.low } ?: lastClose

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F1420))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modeLabel = if (onReturnToKLine != null) "【日K穿透分时】" else "【历史分时】"
                Text(
                    text = "${modeLabel}昨收: ${String.format(Locale.US, "%.2f", refClose)}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Text(
                    text = "收盘: ${String.format(Locale.US, "%.2f", lastClose)} ($sign${String.format(Locale.US, "%.2f%%", deltaPct)})",
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "高: ${String.format(Locale.US, "%.2f", high)} 低: ${String.format(Locale.US, "%.2f", low)}",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun HistoricalDatePickerDialog(
    availableDates: List<String>,
    selectedDate: String?,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val latestDate = availableDates.lastOrNull()
    val activeDate = selectedDate ?: latestDate

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = EastMoneySurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, EastMoneyBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "选择分时交易日",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "支持回放近30个交易日完整5分钟分时走势",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (availableDates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无可用历史交易日", color = TextMuted, fontSize = 13.sp)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Reverse so latest dates appear at top
                        availableDates.reversed().forEach { dateStr ->
                            val isSelected = dateStr == activeDate
                            val isLatest = dateStr == latestDate

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF222B42) else Color(0xFF141926))
                                    .border(
                                        1.dp,
                                        if (isSelected) EastMoneyOrange else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onDateSelected(dateStr) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = null,
                                        tint = if (isSelected) EastMoneyOrange else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatDisplayDateWithWeekday(dateStr),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) EastMoneyOrange else TextPrimary
                                    )
                                    if (isLatest) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(EastMoneyRed.copy(alpha = 0.2f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "今日/最新",
                                                color = EastMoneyRed,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "已选中",
                                        tint = EastMoneyOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = "查看分时",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263045)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("关闭", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}
