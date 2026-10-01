package com.stockmarket.app.ui.screens.sector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmarket.app.data.model.ThematicStockItem
import com.stockmarket.app.ui.components.CandlestickChart
import com.stockmarket.app.ui.components.ChartType
import com.stockmarket.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectorDetailScreen(
    viewModel: SectorDetailViewModel,
    onStockClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val stockColors = LocalStockColors.current

    val quote = state.quote
    val isUp = (quote?.change ?: 0.0) >= 0.0
    val quoteColor = if (isUp) stockColors.upColor else stockColors.downColor
    val prefix = if (isUp) "+" else ""

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.sectorName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EastMoneyOrange.copy(alpha = 0.2f))
                                    .border(0.8.dp, EastMoneyOrange.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "板块",
                                    color = EastMoneyOrange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = state.bkCode,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    // Watchlist Toggle Button
                    IconButton(onClick = { viewModel.toggleWatchlist() }) {
                        Icon(
                            imageVector = if (state.isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (state.isWatchlisted) "已在自选" else "加入自选",
                            tint = if (state.isWatchlisted) Color(0xFFFFB300) else TextPrimary
                        )
                    }
                    // Refresh Button
                    IconButton(onClick = { viewModel.loadData(isInitial = false) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = if (state.isRefreshing) PrimaryBlue else TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BgDark
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // 1. Sector Top Quote Metric Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val priceStr = quote?.let { String.format(Locale.US, "%.2f", it.price) } ?: "--"
                                Text(
                                    text = priceStr,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = quoteColor
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val chgStr = quote?.let { "$prefix${String.format(Locale.US, "%.2f", it.change)}" } ?: "--"
                                    val pctStr = quote?.let { "$prefix${String.format(Locale.US, "%.2f%%", it.changePercent)}" } ?: "--"
                                    Text(text = chgStr, color = quoteColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = pctStr, color = quoteColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Quick Watchlist Status Pill
                            Button(
                                onClick = { viewModel.toggleWatchlist() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isWatchlisted) SurfaceBorder else PrimaryBlue
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isWatchlisted) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (state.isWatchlisted) TextSecondary else Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.isWatchlisted) "已在自选" else "加入自选",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (state.isWatchlisted) TextSecondary else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Grid Metrics: 今开, 最高, 最低, 昨收, 成交额, 换手率
                        quote?.let { q ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                MetricCell("今开", String.format(Locale.US, "%.2f", q.open))
                                MetricCell("最高", String.format(Locale.US, "%.2f", q.high), quoteColor)
                                MetricCell("最低", String.format(Locale.US, "%.2f", q.low))
                                MetricCell("昨收", String.format(Locale.US, "%.2f", q.previousClose))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val turnoverStr = formatAmount(q.turnoverAmount)
                                MetricCell("成交额", turnoverStr)
                                MetricCell("换手率", String.format(Locale.US, "%.2f%%", q.turnoverRate))
                                MetricCell("成分股", "${state.constituents.size} 只")
                                MetricCell("市场类型", "A股板块")
                            }
                        }
                    }
                }

                // 2. Sector Intraday Trend Chart
                item {
                    if (state.trendCandles.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "板块分时走势",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                            CandlestickChart(
                                candles = state.trendCandles,
                                chartType = ChartType.LINE,
                                previousClose = quote?.previousClose,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 3. Constituent Stocks Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(SurfaceDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "成分股",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(共 ${state.constituents.size} 只)",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "按涨跌幅降序",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Column headers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceBorder.copy(alpha = 0.3f))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "股票名称 / 代码", color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1.3f))
                            Text(text = "最新价", color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                            Text(text = "涨跌幅", color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                            Text(text = "加自选", color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.End)
                        }
                    }
                }

                // 4. Constituent Stock Items
                if (state.constituents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "暂无成分股数据", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    itemsIndexed(state.constituents, key = { _, stock -> stock.symbol }) { index, stock ->
                        ConstituentStockRow(
                            rank = index + 1,
                            stock = stock,
                            isWatchlisted = viewModel.isStockWatchlisted(stock.symbol),
                            onClick = { onStockClick(stock.symbol) },
                            onToggleWatchlist = { viewModel.toggleStockWatchlist(stock.symbol) }
                        )
                        HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ConstituentStockRow(
    rank: Int,
    stock: ThematicStockItem,
    isWatchlisted: Boolean,
    onClick: () -> Unit,
    onToggleWatchlist: () -> Unit
) {
    val stockColors = LocalStockColors.current
    val isUp = stock.changePercent >= 0.0
    val color = if (isUp) stockColors.upColor else stockColors.downColor
    val prefix = if (isUp) "+" else ""

    var inWatchlist by remember(isWatchlisted) { mutableStateOf(isWatchlisted) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stock Name & Code + Rank
        Row(modifier = Modifier.weight(1.3f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (rank <= 3) EastMoneyOrange.copy(alpha = 0.25f) else SurfaceBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) EastMoneyOrange else TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stock.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.symbol,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    if (stock.tag.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(color.copy(alpha = 0.15f))
                                .border(0.6.dp, color.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stock.tag,
                                color = color,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Latest Price
        Text(
            text = String.format(Locale.US, "%.2f", stock.price),
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.9f),
            textAlign = TextAlign.End
        )

        // Change % Pill
        Box(
            modifier = Modifier
                .weight(0.9f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$prefix${String.format(Locale.US, "%.2f%%", stock.changePercent)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Add to watchlist button
        Box(
            modifier = Modifier.weight(0.5f),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(
                onClick = {
                    onToggleWatchlist()
                    inWatchlist = !inWatchlist
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (inWatchlist) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (inWatchlist) "已在自选" else "加入自选",
                    tint = if (inWatchlist) TextMuted else PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return when {
        amount >= 1e8 -> String.format(Locale.US, "%.2f亿", amount / 1e8)
        amount >= 1e4 -> String.format(Locale.US, "%.2f万", amount / 1e4)
        amount > 0 -> String.format(Locale.US, "%.2f", amount)
        else -> "--"
    }
}
