package com.stockmarket.app.ui.screens.search

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onStockClick: (String) -> Unit,
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
                        placeholder = { Text("输入A股代码/拼音/名称 (如 002579, zjdz, 中京电子)", fontSize = 14.sp, color = TextMuted) },
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
            // Trending stocks chip list
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "热门股票推荐",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.trendingStocks.take(8)) { item ->
                        val shortCode = item.symbol.substringBefore('.')
                        SuggestionChip(
                            onClick = { viewModel.onQueryChange(shortCode) },
                            label = { Text("${item.name} ($shortCode)", fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = SurfaceCard,
                                labelColor = TextPrimary
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = SurfaceBorder
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
                val displayList = if (state.results.isNotEmpty()) state.results else if (state.query.isEmpty()) state.trendingStocks else emptyList()

                if (displayList.isEmpty() && state.query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("未找到相关股票", color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayList) { stock ->
                            SearchResultItem(
                                stock = stock,
                                isWatchlisted = viewModel.isWatchlisted(stock.symbol),
                                onStockClick = { onStockClick(stock.symbol) },
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
    isWatchlisted: Boolean,
    onStockClick: () -> Unit,
    onToggleWatchlist: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onStockClick),
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
                    if (stock.exchange.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceBorder)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stock.exchange,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
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
