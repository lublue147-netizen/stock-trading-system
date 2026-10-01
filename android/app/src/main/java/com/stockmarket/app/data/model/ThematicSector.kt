package com.stockmarket.app.data.model

enum class ThematicSectorType(
    val title: String,
    val bkCode: String,
    val badge: String,
    val desc: String
) {
    MULTI_BOARD("最近多板", "BK1638", "连板天梯", "东方财富最近多板板块(BK1638)最新成分股"),
    YESTERDAY_LIMIT_UP("昨日涨停-含一字", "BK1050", "超短接力", "东方财富昨日涨停_含一字板块(BK1050)最新成分股"),
    TREND_STOCKS("趋势股", "BK1715", "主升浪", "东方财富趋势股板块(BK1715)最新成分股"),
    ALL_TIME_HIGH("历史新高", "BK1675", "突破高点", "东方财富历史新高板块(BK1675)最新成分股"),
    AVERAGE_PRICE("A股平均股价", "800005", "全市场脉搏", "全A指数均价、两市总成交额与涨跌情绪全景")
}

data class ThematicStockItem(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val boardCount: Int? = null,
    val tag: String,
    val subDetail: String,
    val industry: String? = null,
    val industryBkCode: String? = null,
    val industryChangePercent: Double? = null
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
