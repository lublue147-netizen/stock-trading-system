package com.stockmarket.app.ui.screens.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmarket.app.data.model.MarketBreadth
import com.stockmarket.app.data.model.ThematicSectorType
import com.stockmarket.app.data.model.ThematicStockItem
import com.stockmarket.app.ui.components.MarketIndexCard
import com.stockmarket.app.ui.components.StockCard
import com.stockmarket.app.ui.theme.*
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onStockClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "A股",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "行情通",
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = TextPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.loadData(isInitial = false) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = if (state.isRefreshing) PrimaryBlue else TextPrimary
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSearchClick,
                containerColor = PrimaryBlue,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加自选"
                )
            }
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
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // 1. Market Indices Section (大盘指数)
                if (state.indices.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)) {
                            Text(
                                text = "大盘指数",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.indices) { index ->
                                    MarketIndexCard(index = index)
                                }
                            }
                        }
                    }
                }

                // 2. Thematic Sectors Section (行情特色板块: 最近多板, 昨日涨停-含一字, 趋势股, 历史新高, A股平均股价)
                item {
                    ThematicSectorsSection(
                        selectedSector = state.selectedThematicSector,
                        thematicSectors = state.thematicSectors,
                        marketBreadth = state.marketBreadth,
                        onSectorSelected = { viewModel.selectThematicSector(it) },
                        onStockClick = onStockClick
                    )
                }

                // 3. Watchlist Section Header (我的自选)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "我的自选 (${state.quotes.size})",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.isRefreshing) {
                            Text(
                                text = "正在刷新...",
                                color = PrimaryBlue,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 4. Watchlist Stock Cards
                if (state.quotes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "自选列表为空",
                                    color = TextSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "点击下方按钮或右上角搜索图标添加股票",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onSearchClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("添加自选股票")
                                }
                            }
                        }
                    }
                } else {
                    items(state.quotes, key = { it.symbol }) { quote ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            StockCard(
                                quote = quote,
                                onClick = { onStockClick(quote.symbol) },
                                onRemove = { viewModel.removeSymbol(quote.symbol) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThematicSectorsSection(
    selectedSector: ThematicSectorType,
    thematicSectors: Map<ThematicSectorType, List<ThematicStockItem>>,
    marketBreadth: MarketBreadth,
    onSectorSelected: (ThematicSectorType) -> Unit,
    onStockClick: (String) -> Unit
) {
    val stockColors = LocalStockColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(EastMoneySurface)
            .border(1.dp, EastMoneyBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = EastMoneyOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "行情特色板块",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = "超短主线雷达",
                fontSize = 11.sp,
                color = EastMoneyOrange,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sector Pill Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ThematicSectorType.values()) { type ->
                val isSelected = type == selectedSector
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) EastMoneyRed else EastMoneyCard)
                        .border(
                            1.dp,
                            if (isSelected) EastMoneyRed else EastMoneyBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSectorSelected(type) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = type.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sector Brief Description
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(EastMoneyCard.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📌 ${selectedSector.desc}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sector Content
        if (selectedSector == ThematicSectorType.AVERAGE_PRICE) {
            MarketBreadthCard(breadth = marketBreadth, stockColors = stockColors)
        } else {
            val stocks = thematicSectors[selectedSector] ?: emptyList()
            if (stocks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = EastMoneyOrange,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stocks.forEachIndexed { index, item ->
                        ThematicStockItemRow(
                            item = item,
                            stockColors = stockColors,
                            onClick = { onStockClick(item.symbol) }
                        )
                        if (index < stocks.size - 1) {
                            HorizontalDivider(
                                color = EastMoneyBorder.copy(alpha = 0.4f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketBreadthCard(
    breadth: MarketBreadth,
    stockColors: StockColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(EastMoneyCard)
            .border(1.dp, EastMoneyBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // Average Price & Turnover
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "全A平均股价",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(EastMoneyOrange.copy(alpha = 0.2f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text("全市场均价", fontSize = 9.sp, color = EastMoneyOrange, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    val isUp = breadth.change >= 0
                    val color = if (isUp) stockColors.upColor else stockColors.downColor
                    val prefix = if (isUp) "+" else ""
                    Text(
                        text = "¥${String.format(Locale.US, "%.2f", breadth.averagePrice)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$prefix${String.format(Locale.US, "%.2f", breadth.change)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "($prefix${String.format(Locale.US, "%.2f%%", breadth.changePercent)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "两市总成交额",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = breadth.totalTurnover,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = breadth.turnoverChange,
                        fontSize = 11.sp,
                        color = if (breadth.turnoverChange.startsWith("+")) stockColors.upColor else stockColors.downColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Market Breadth Progress Bar
        val total = max(1, breadth.upCount + breadth.downCount + breadth.flatCount).toFloat()
        val upWeight = (breadth.upCount / total).coerceIn(0.05f, 0.90f)
        val flatWeight = (breadth.flatCount / total).coerceIn(0.02f, 0.20f)
        val downWeight = (breadth.downCount / total).coerceIn(0.05f, 0.90f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(upWeight)
                    .fillMaxHeight()
                    .background(stockColors.upColor)
            )
            Box(
                modifier = Modifier
                    .weight(flatWeight)
                    .fillMaxHeight()
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
            Box(
                modifier = Modifier
                    .weight(downWeight)
                    .fillMaxHeight()
                    .background(stockColors.downColor)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Breadth counts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "上涨 ${breadth.upCount}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = stockColors.upColor
            )
            Text(
                text = "平盘 ${breadth.flatCount}",
                fontSize = 11.sp,
                color = TextMuted
            )
            Text(
                text = "下跌 ${breadth.downCount}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = stockColors.downColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sentiment & Limit Counts Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(stockColors.upColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "涨停 ${breadth.limitUpCount}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = stockColors.upColor
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(stockColors.downColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "跌停 ${breadth.limitDownCount}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = stockColors.downColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(EastMoneyYellow.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${breadth.sentimentScore}分 · ${breadth.sentimentLabel}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EastMoneyYellow
                )
            }
        }
    }
}

@Composable
private fun ThematicStockItemRow(
    item: ThematicStockItem,
    stockColors: StockColors,
    onClick: () -> Unit
) {
    val isUp = item.change >= 0
    val color = if (isUp) stockColors.upColor else stockColors.downColor
    val prefix = if (isUp) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Name, Tag, Symbol, Theme Subdetail
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                val tagBg = if (item.boardCount != null) EastMoneyOrange else SurfaceBorder
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(tagBg)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.symbol,
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.subDetail,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Column: Price & Change Percent Button
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format(Locale.US, "%.2f", item.price),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$prefix${String.format(Locale.US, "%.2f%%", item.changePercent)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
