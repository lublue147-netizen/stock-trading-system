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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmarket.app.data.local.WatchlistGroup
import com.stockmarket.app.data.local.WatchlistPreferences
import com.stockmarket.app.ui.components.MarketIndexCard
import com.stockmarket.app.ui.components.StockCard
import com.stockmarket.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onStockClick: (String) -> Unit,
    onSectorClick: (String, String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showManageGroupsDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var createGroupError by remember { mutableStateOf<String?>(null) }

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
                                    MarketIndexCard(
                                        index = index,
                                        onClick = { onStockClick(index.symbol) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Watchlist Groups Bar (自选分组导航栏)
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "自选分组",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { showManageGroupsDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "管理分组",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("管理", fontSize = 12.sp, color = PrimaryBlue)
                                }
                            }
                        }

                        // Scrollable Group Tabs Row
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "全部" Tab
                            val isAllSelected = state.selectedGroup == WatchlistPreferences.GROUP_ALL
                            val totalCount = state.groups.flatMap { it.symbols }.distinct().size
                            item {
                                GroupTabPill(
                                    title = WatchlistPreferences.GROUP_ALL,
                                    count = totalCount,
                                    isSelected = isAllSelected,
                                    onClick = { viewModel.selectGroup(WatchlistPreferences.GROUP_ALL) }
                                )
                            }

                            // Dynamic Group Tabs
                            items(state.groups, key = { it.name }) { group ->
                                val isSelected = state.selectedGroup == group.name
                                GroupTabPill(
                                    title = group.name,
                                    count = group.symbols.size,
                                    isSelected = isSelected,
                                    onClick = { viewModel.selectGroup(group.name) }
                                )
                            }

                            // "+ 新建分组" Button
                            item {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SurfaceBorder.copy(alpha = 0.4f))
                                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                                        .clickable {
                                            newGroupName = ""
                                            createGroupError = null
                                            showCreateGroupDialog = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "新建分组",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "新建分组",
                                        color = PrimaryBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Section Status Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.selectedGroup} (${state.quotes.size})",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.isRefreshing) {
                            Text(
                                text = "正在刷新行情...",
                                color = PrimaryBlue,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 4. Watchlist Items (Stocks and Sectors)
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
                                    text = if (state.selectedGroup == WatchlistPreferences.GROUP_ALL) "自选列表为空" else "「${state.selectedGroup}」分组暂无内容",
                                    color = TextSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "搜索股票或特色板块并加入自选",
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
                                    Text("添加股票/板块")
                                }
                            }
                        }
                    }
                } else {
                    val distinctQuotes = state.quotes.distinctBy { it.symbol }
                    items(distinctQuotes, key = { it.symbol }) { quote ->
                        val isSector = quote.symbol.startsWith("BK")
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            StockCard(
                                quote = quote,
                                onClick = {
                                    if (isSector) {
                                        onSectorClick(quote.symbol, quote.name)
                                    } else {
                                        onStockClick(quote.symbol)
                                    }
                                },
                                onSectorClick = onSectorClick,
                                onRemove = { viewModel.removeSymbol(quote.symbol) }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Dialog: Create New Group ---
    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = {
                Text("新建自选分组", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("输入分组名称 (如: 核心科技、短线接力、我的板块):", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = {
                            newGroupName = it
                            createGroupError = null
                        },
                        singleLine = true,
                        placeholder = { Text("分组名称", color = TextMuted, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (createGroupError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = createGroupError!!, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newGroupName.trim()
                        if (clean.isEmpty()) {
                            createGroupError = "分组名称不能为空"
                        } else if (clean == WatchlistPreferences.GROUP_ALL) {
                            createGroupError = "不能使用系统保留名称"
                        } else {
                            val success = viewModel.createGroup(clean)
                            if (success) {
                                showCreateGroupDialog = false
                            } else {
                                createGroupError = "该分组已存在"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard
        )
    }

    // --- Dialog: Manage Groups ---
    if (showManageGroupsDialog) {
        AlertDialog(
            onDismissRequest = { showManageGroupsDialog = false },
            title = {
                Text("自选分组管理", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("默认分组不可删除，自定义分组可随时增删：", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.groups, key = { it.name }) { group ->
                            val canDelete = group.name != WatchlistPreferences.GROUP_DEFAULT
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceDark)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = group.name,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${group.symbols.size} 个股票/板块",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                if (canDelete) {
                                    IconButton(
                                        onClick = { viewModel.deleteGroup(group.name) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除分组",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "默认",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showManageGroupsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("完成")
                }
            },
            containerColor = SurfaceCard
        )
    }
}

@Composable
private fun GroupTabPill(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) PrimaryBlue else SurfaceCard)
            .border(
                width = 1.dp,
                color = if (isSelected) PrimaryBlue else SurfaceBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else TextSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($count)",
                fontSize = 11.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextMuted
            )
        }
    }
}
