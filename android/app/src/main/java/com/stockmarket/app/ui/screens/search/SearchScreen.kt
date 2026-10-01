package com.stockmarket.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
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
import com.stockmarket.app.data.model.SearchResult
import com.stockmarket.app.ui.theme.*

private val POPULAR_RECOMMENDATIONS = listOf(
    SearchResult("BK1638", "最近多板", "板块", "SECTOR"),
    SearchResult("BK1050", "昨日涨停_含一字", "板块", "SECTOR"),
    SearchResult("BK1715", "趋势股", "板块", "SECTOR"),
    SearchResult("BK1036", "半导体", "板块", "SECTOR"),
    SearchResult("BK1166", "低空经济", "板块", "SECTOR"),
    SearchResult("002579.SZ", "中京电子", "深交所", "EQUITY"),
    SearchResult("600519.SS", "贵州茅台", "上交所", "EQUITY"),
    SearchResult("300750.SZ", "宁德时代", "深交所", "EQUITY"),
    SearchResult("002594.SZ", "比亚迪", "深交所", "EQUITY")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onStockClick: (String) -> Unit,
    onSectorClick: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("输入股票/板块代码或名称 (如 BK1638, 最近多板, 半导体, 002579)", fontSize = 13.sp, color = TextMuted) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除", tint = TextSecondary)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        containerColor = BgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Trending stocks & sectors chip recommendations
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "热门推荐 (包含特色板块与精选个股)",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(POPULAR_RECOMMENDATIONS) { item ->
                        val isSector = item.type == "SECTOR" || item.symbol.startsWith("BK")
                        SuggestionChip(
                            onClick = { viewModel.onQueryChange(if (isSector) item.symbol else item.symbol.substringBefore('.')) },
                            label = {
                                Text(
                                    text = if (isSector) "【板块】${item.name}" else "${item.name} (${item.symbol.substringBefore('.')})",
                                    fontSize = 12.sp,
                                    color = if (isSector) EastMoneyOrange else TextPrimary
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = SurfaceCard,
                                labelColor = TextPrimary
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isSector) EastMoneyOrange.copy(alpha = 0.5f) else SurfaceBorder
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)

            // Results List
            if (state.isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                val displayList = if (state.results.isNotEmpty()) state.results else if (state.query.isEmpty()) POPULAR_RECOMMENDATIONS else emptyList()

                if (displayList.isEmpty() && state.query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("未找到相关股票或板块", color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayList) { stock ->
                            val isSector = stock.type == "SECTOR" || stock.symbol.startsWith("BK")
                            SearchResultItem(
                                stock = stock,
                                isSector = isSector,
                                isWatchlisted = viewModel.isWatchlisted(stock.symbol),
                                onClick = {
                                    if (isSector) {
                                        onSectorClick(stock.symbol, stock.name)
                                    } else {
                                        onStockClick(stock.symbol)
                                    }
                                },
                                onToggleWatchlist = { viewModel.toggleWatchlist(stock.symbol) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    stock: SearchResult,
    isSector: Boolean,
    isWatchlisted: Boolean,
    onClick: () -> Unit,
    onToggleWatchlist: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.symbol,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSector) EastMoneyOrange.copy(alpha = 0.2f) else SurfaceBorder)
                            .border(
                                width = if (isSector) 0.8.dp else 0.dp,
                                color = if (isSector) EastMoneyOrange.copy(alpha = 0.7f) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isSector) "板块" else stock.exchange,
                            color = if (isSector) EastMoneyOrange else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stock.name,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // Watchlist toggle button
            IconButton(
                onClick = onToggleWatchlist,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isWatchlisted) SurfaceBorder else PrimaryBlue.copy(alpha = 0.2f))
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = if (isWatchlisted) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (isWatchlisted) "已在自选" else "加入自选",
                    tint = if (isWatchlisted) TextSecondary else PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
