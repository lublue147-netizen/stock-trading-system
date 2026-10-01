package com.stockmarket.app.data.model

enum class ThematicSectorType(val title: String, val badge: String, val desc: String) {
    MULTI_BOARD("最近多板", "连板天梯", "近期连板高度龙头及多板梯队"),
    YESTERDAY_LIMIT_UP("昨日涨停-含一字", "超短接力", "昨日涨停及一字板股票今日接力溢价表现"),
    TREND_STOCKS("趋势股", "机构重仓", "均线多头主升浪与中长期上升通道股票"),
    ALL_TIME_HIGH("历史新高", "创历史高", "突破历史最高价或临近历史峰值股票"),
    AVERAGE_PRICE("A股平均股价", "全市场脉搏", "全A指数均价与全市场涨跌情绪透视")
}

data class ThematicStockItem(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val boardCount: Int? = null,
    val tag: String,
    val subDetail: String
)

data class MarketBreadth(
    val averagePrice: Double = 21.85,
    val change: Double = 0.42,
    val changePercent: Double = 1.96,
    val upCount: Int = 4128,
    val downCount: Int = 986,
    val flatCount: Int = 185,
    val limitUpCount: Int = 98,
    val limitDownCount: Int = 3,
    val totalTurnover: String = "1.68万亿",
    val turnoverChange: String = "+2,350亿",
    val sentimentScore: Int = 88,
    val sentimentLabel: String = "情绪极强 · 赚钱效应爆棚"
)
