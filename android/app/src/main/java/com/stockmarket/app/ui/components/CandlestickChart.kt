package com.stockmarket.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmarket.app.data.model.CandlePoint
import com.stockmarket.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

enum class ChartType {
    CANDLESTICK,
    LINE
}

@Composable
fun CandlestickChart(
    candles: List<CandlePoint>,
    symbol: String? = null,
    chartType: ChartType = ChartType.CANDLESTICK,
    previousClose: Double? = null,
    modifier: Modifier = Modifier,
    showMA: Boolean = true,
    onCandleSelected: ((CandlePoint?) -> Unit)? = null,
    onViewIntradayForDate: ((String) -> Unit)? = null
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(SurfaceDark, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("暂无行情分时/K线数据", color = TextSecondary, fontSize = 14.sp)
        }
        return
    }

    val limitRate: Double = remember(symbol, previousClose) {
        val s = symbol?.uppercase() ?: ""
        when {
            s.contains("ST") -> 0.05
            s.startsWith("30") || s.startsWith("68") || s.contains("300") || s.contains("688") -> 0.20
            s.endsWith(".BJ") || s.startsWith("8") || s.startsWith("4") || s.startsWith("920") -> 0.30
            s.startsWith("^") || s.startsWith("000001.SS") || s.startsWith("399") -> 0.10
            else -> 0.10
        }
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val stockColors = LocalStockColors.current

    val ma5 = remember(candles) { calculateMA(candles, 5) }
    val ma10 = remember(candles) { calculateMA(candles, 10) }
    val ma20 = remember(candles) { calculateMA(candles, 20) }

    val vol5 = remember(candles) { calculateVolMA(candles, 5) }
    val vol10 = remember(candles) { calculateVolMA(candles, 10) }
    val vwap = remember(candles) { calculateVWAP(candles) }

    val auctionIndex: Int? = remember(candles, chartType) {
        if (chartType == ChartType.LINE && candles.isNotEmpty()) {
            val sdfHHmm = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT+8")
            }
            val idx = candles.indexOfFirst {
                val t = sdfHHmm.format(Date(it.timestamp))
                t >= "09:30"
            }
            if (idx > 0) idx else null
        } else null
    }

    val activeIndex = selectedIndex ?: (candles.size - 1).coerceAtLeast(0)
    val activeCandle = candles.getOrNull(activeIndex)
    val activeMA5 = ma5.getOrNull(activeIndex)
    val activeMA10 = ma10.getOrNull(activeIndex)
    val activeMA20 = ma20.getOrNull(activeIndex)
    val activeVWAP = vwap.getOrNull(activeIndex)

    val isAuction = remember(activeCandle, chartType) {
        if (chartType == ChartType.LINE && activeCandle != null) {
            val t = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT+8")
            }.format(Date(activeCandle.timestamp))
            t < "09:30"
        } else false
    }

    val activeVol5 = vol5.getOrNull(activeIndex)
    val activeVol10 = vol10.getOrNull(activeIndex)

    val baseClose = previousClose ?: candles.firstOrNull()?.open ?: 100.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(10.dp)
    ) {
        // Top Info & Indicator Legend (East Money Style)
        Column(modifier = Modifier.fillMaxWidth()) {
            if (activeCandle != null) {
                LaunchedEffect(activeCandle) {
                    onCandleSelected?.invoke(activeCandle)
                }

                val dateFormat = remember {
                    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("GMT+8")
                    }
                }
                val dateStr = dateFormat.format(Date(activeCandle.timestamp))
                val ymdFormat = remember {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("GMT+8")
                    }
                }
                val candleYmd = ymdFormat.format(Date(activeCandle.timestamp))

                val prevRef = if (chartType == ChartType.LINE && previousClose != null && previousClose > 0) previousClose else activeCandle.open
                val candleDelta = activeCandle.close - prevRef
                val candleDeltaPercent = if (prevRef > 0) (candleDelta / prevRef) * 100 else 0.0
                val isUp = candleDelta >= 0
                val candleColor = if (isUp) stockColors.upColor else stockColors.downColor
                val prefix = if (isUp) "+" else ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateStr,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        if (isAuction) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(EastMoneyOrange.copy(alpha = 0.2f))
                                    .border(1.dp, EastMoneyOrange.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "集合竞价",
                                    color = EastMoneyOrange,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isAuction) "竞价: ${String.format(Locale.US, "%.2f", activeCandle.close)}" else "现价: ${String.format(Locale.US, "%.2f", activeCandle.close)}",
                            color = candleColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$prefix${String.format(Locale.US, "%.2f%%", candleDeltaPercent)}",
                            color = candleColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // East Money Style: Direct shortcut button to view intraday from K-Line!
                        if (chartType == ChartType.CANDLESTICK && onViewIntradayForDate != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EastMoneyOrange.copy(alpha = 0.18f))
                                    .border(1.dp, EastMoneyOrange.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .clickable { onViewIntradayForDate(candleYmd) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "当日分时",
                                    color = EastMoneyOrange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowRight,
                                    contentDescription = "查看分时",
                                    tint = EastMoneyOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                if (chartType == ChartType.LINE) {
                    // Intraday Legend (Price, VWAP, Change, Volume)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            activeVWAP?.let {
                                Text("均价: ${String.format(Locale.US, "%.2f", it)}", color = VwapYellow, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                            previousClose?.let {
                                Text("昨收: ${String.format(Locale.US, "%.2f", it)}", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                        Text("量: ${formatVolumeInLots(activeCandle.volume)}", color = TextMuted, fontSize = 10.sp)
                    }
                } else {
                    // K-Line Legend (Open, High, Low, Volume)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("开: ${String.format(Locale.US, "%.2f", activeCandle.open)}", color = TextMuted, fontSize = 10.sp)
                        Text("高: ${String.format(Locale.US, "%.2f", activeCandle.high)}", color = TextMuted, fontSize = 10.sp)
                        Text("低: ${String.format(Locale.US, "%.2f", activeCandle.low)}", color = TextMuted, fontSize = 10.sp)
                        Text("量: ${formatVolumeInLots(activeCandle.volume)}", color = TextMuted, fontSize = 10.sp)
                    }

                    if (showMA) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            activeMA5?.let { Text("MA5: ${String.format(Locale.US, "%.2f", it)}", color = MA5Color, fontSize = 10.sp) }
                            activeMA10?.let { Text("MA10: ${String.format(Locale.US, "%.2f", it)}", color = MA10Color, fontSize = 10.sp) }
                            activeMA20?.let { Text("MA20: ${String.format(Locale.US, "%.2f", it)}", color = MA20Color, fontSize = 10.sp) }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Chart Canvas Container with Fixed Limit Up / Limit Down Coordinate Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(candles) {
                        detectTapGestures(
                            onPress = { offset ->
                                selectedIndex = calculateCandleIndex(offset.x, size.width.toFloat(), candles.size)
                            },
                            onTap = { offset ->
                                selectedIndex = calculateCandleIndex(offset.x, size.width.toFloat(), candles.size)
                            },
                            onDoubleTap = { offset ->
                                val idx = calculateCandleIndex(offset.x, size.width.toFloat(), candles.size)
                                selectedIndex = idx
                                if (chartType == ChartType.CANDLESTICK && onViewIntradayForDate != null) {
                                    candles.getOrNull(idx)?.let { c ->
                                        val ymd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                            timeZone = TimeZone.getTimeZone("GMT+8")
                                        }.format(Date(c.timestamp))
                                        onViewIntradayForDate(ymd)
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(candles) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selectedIndex = calculateCandleIndex(offset.x, size.width.toFloat(), candles.size)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                selectedIndex = calculateCandleIndex(change.position.x, size.width.toFloat(), candles.size)
                            },
                            onDragEnd = {
                                // keep selected
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height

                val priceChartHeight = height * 0.70f
                val volumeChartHeight = height * 0.22f
                val volumeChartTop = height * 0.78f

                val minPrice: Double
                val maxPrice: Double
                val priceSpan: Double
                val adjustedMinPrice: Double

                if (chartType == ChartType.LINE && previousClose != null && previousClose > 0) {
                    // Fixed limit-up and limit-down coordinates (固定涨跌停坐标)
                    val nominalDev = previousClose * limitRate
                    val actualMaxDev = candles.maxOfOrNull { abs(it.close - previousClose) } ?: 0.0
                    val effectiveDev = max(nominalDev, actualMaxDev)
                    minPrice = previousClose - effectiveDev
                    maxPrice = previousClose + effectiveDev
                    priceSpan = maxPrice - minPrice
                    adjustedMinPrice = minPrice
                } else {
                    val rawMin = candles.minOf { it.low }
                    val rawMax = candles.maxOf { it.high }
                    priceSpan = max(0.01, rawMax - rawMin) * 1.08
                    adjustedMinPrice = rawMin - (priceSpan * 0.04)
                    minPrice = adjustedMinPrice
                    maxPrice = adjustedMinPrice + priceSpan
                }

                val candleCount = candles.size
                val candleWidth = width / candleCount
                val barWidth = max(2f, candleWidth * 0.72f)
                val maxVolume = max(1L, candles.maxOf { it.volume }).toFloat()
                val auctionX = if (chartType == ChartType.LINE && auctionIndex != null) (auctionIndex * candleWidth) else null

                // Draw Background Grid
                drawEastMoneyGrid(
                    width = width,
                    priceHeight = priceChartHeight,
                    volTop = volumeChartTop,
                    volHeight = volumeChartHeight,
                    chartType = chartType,
                    hasPrevClose = chartType == ChartType.LINE && previousClose != null,
                    auctionX = auctionX
                )

            // Draw Candlesticks or Line Chart
            if (chartType == ChartType.CANDLESTICK) {
                candles.forEachIndexed { i, candle ->
                    val x = i * candleWidth + (candleWidth / 2f)
                    val isUp = candle.close >= candle.open
                    val color = if (isUp) stockColors.upColor else stockColors.downColor

                    val openY = priceChartHeight - ((candle.open - adjustedMinPrice) / priceSpan * priceChartHeight).toFloat()
                    val closeY = priceChartHeight - ((candle.close - adjustedMinPrice) / priceSpan * priceChartHeight).toFloat()
                    val highY = priceChartHeight - ((candle.high - adjustedMinPrice) / priceSpan * priceChartHeight).toFloat()
                    val lowY = priceChartHeight - ((candle.low - adjustedMinPrice) / priceSpan * priceChartHeight).toFloat()

                    // Draw Wick (High - Low)
                    drawLine(
                        color = color,
                        start = Offset(x, highY),
                        end = Offset(x, lowY),
                        strokeWidth = 2f
                    )

                    // Draw Body (Open - Close)
                    val bodyTop = min(openY, closeY)
                    val bodyBottom = max(openY, closeY)
                    val bodyHeight = max(2f, bodyBottom - bodyTop)

                    drawRect(
                        color = color,
                        topLeft = Offset(x - barWidth / 2f, bodyTop),
                        size = Size(barWidth, bodyHeight)
                    )

                    // Draw Volume Bar
                    val volHeight = (candle.volume.toFloat() / maxVolume) * volumeChartHeight
                    drawRect(
                        color = color.copy(alpha = 0.85f),
                        topLeft = Offset(x - barWidth / 2f, height - volHeight),
                        size = Size(barWidth, volHeight)
                    )
                }

                // Draw MA Curves
                if (showMA) {
                    drawMALine(ma5, candleWidth, adjustedMinPrice, priceSpan, priceChartHeight, MA5Color)
                    drawMALine(ma10, candleWidth, adjustedMinPrice, priceSpan, priceChartHeight, MA10Color)
                    drawMALine(ma20, candleWidth, adjustedMinPrice, priceSpan, priceChartHeight, MA20Color)
                }

                // Draw Volume MAVOL Curves
                drawMALine(vol5.map { it?.let { (it / maxVolume) * volumeChartHeight } }, candleWidth, 0.0, volumeChartHeight.toDouble(), volumeChartHeight, MA5Color, yOffset = volumeChartTop)
                drawMALine(vol10.map { it?.let { (it / maxVolume) * volumeChartHeight } }, candleWidth, 0.0, volumeChartHeight.toDouble(), volumeChartHeight, MA10Color, yOffset = volumeChartTop)

            } else {
                // INTRADAY LINE CHART with East Money smooth gradient & VWAP line
                val linePath = Path()
                val fillPath = Path()

                candles.forEachIndexed { i, candle ->
                    val x = i * candleWidth + (candleWidth / 2f)
                    val y = priceChartHeight - ((candle.close - adjustedMinPrice) / priceSpan * priceChartHeight).toFloat()

                    if (i == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, priceChartHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }

                    // Volume Bar in Intraday: color based on close >= prev tick
                    val prevPrice = if (i > 0) candles[i - 1].close else (previousClose ?: candle.open)
                    val isUp = candle.close >= prevPrice
                    val color = if (isUp) stockColors.upColor else stockColors.downColor
                    val volHeight = (candle.volume.toFloat() / maxVolume) * volumeChartHeight
                    drawRect(
                        color = color.copy(alpha = 0.85f),
                        topLeft = Offset(x - barWidth / 2f, height - volHeight),
                        size = Size(barWidth, volHeight)
                    )
                }

                val lastX = (candles.size - 1) * candleWidth + (candleWidth / 2f)
                fillPath.lineTo(lastX, priceChartHeight)
                fillPath.close()

                // Draw gradient under price line
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color(0xFF38BDF8).copy(alpha = 0.02f)),
                        startY = 0f,
                        endY = priceChartHeight
                    )
                )

                // Draw main price line (White-Blue)
                drawPath(
                    path = linePath,
                    color = Color(0xFF38BDF8),
                    style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                )

                // Draw VWAP (分时均价线 - Yellow) - East Money classic!
                drawMALine(vwap, candleWidth, adjustedMinPrice, priceSpan, priceChartHeight, VwapYellow)
            }

            // Crosshair overlay if selected
            selectedIndex?.let { idx ->
                if (idx in candles.indices) {
                    val candle = candles[idx]
                    val crosshairX = idx * candleWidth + (candleWidth / 2f)
                    val crosshairY = priceChartHeight - ((candle.close - adjustedMinPrice) / priceSpan * priceChartHeight).toFloat()

                    // Vertical Line
                    drawLine(
                        color = TextSecondary.copy(alpha = 0.8f),
                        start = Offset(crosshairX, 0f),
                        end = Offset(crosshairX, height),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    // Horizontal Line
                    drawLine(
                        color = TextSecondary.copy(alpha = 0.8f),
                        start = Offset(0f, crosshairY),
                        end = Offset(width, crosshairY),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    // Center dot
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(crosshairX, crosshairY)
                    )
                    drawCircle(
                        color = PrimaryBlue,
                        radius = 2.5f,
                        center = Offset(crosshairX, crosshairY)
                    )
                }
            }
        }

        // Fixed Limit Up / Limit Down Y-Axis overlay labels (East Money style)
        if (chartType == ChartType.LINE && previousClose != null && previousClose > 0) {
            val effectiveDev = max(previousClose * limitRate, candles.maxOfOrNull { abs(it.close - previousClose) } ?: 0.0)
            val topPrice = previousClose + effectiveDev
            val botPrice = previousClose - effectiveDev
            val topPct = if (previousClose > 0) (topPrice - previousClose) / previousClose * 100 else 0.0
            val botPct = if (previousClose > 0) (previousClose - botPrice) / previousClose * 100 else 0.0

            // Intraday price area takes top 70% of 250dp = 175dp
            val priceAreaHeight = 175.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(priceAreaHeight)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                // Left labels: 涨停, 昨收, 跌停
                Text(
                    text = "涨停 ${String.format(Locale.US, "%.2f", topPrice)}",
                    color = stockColors.upColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                Text(
                    text = "昨收 ${String.format(Locale.US, "%.2f", previousClose)}",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = "跌停 ${String.format(Locale.US, "%.2f", botPrice)}",
                    color = stockColors.downColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.BottomStart)
                )

                // Right labels: +XX.XX%, 0.00%, -XX.XX%
                Text(
                    text = "+${String.format(Locale.US, "%.2f%%", topPct)}",
                    color = stockColors.upColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                Text(
                    text = "0.00%",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
                Text(
                    text = "-${String.format(Locale.US, "%.2f%%", botPct)}",
                    color = stockColors.downColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }

        // Sub-chart Vol Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("成交量(VOL): ${activeCandle?.let { formatVolumeInLots(it.volume) } ?: "--"}", color = TextMuted, fontSize = 9.sp)
            if (chartType == ChartType.CANDLESTICK) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeVol5?.let { Text("VOL5: ${formatVolumeInLots(it.toLong())}", color = MA5Color, fontSize = 9.sp) }
                    activeVol10?.let { Text("VOL10: ${formatVolumeInLots(it.toLong())}", color = MA10Color, fontSize = 9.sp) }
                }
            }
        }

        // Time Axis Markers (East Money style: 09:30, 10:30, 11:30/13:00, 14:00, 15:00)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (chartType == ChartType.LINE) {
                if (auctionIndex != null) {
                    Text("09:15(竞价)", color = EastMoneyOrange, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    Text("09:30", color = TextSecondary, fontSize = 9.sp)
                    Text("11:30/13:00", color = TextMuted, fontSize = 9.sp)
                    Text("14:00", color = TextMuted, fontSize = 9.sp)
                    Text("15:00", color = TextMuted, fontSize = 9.sp)
                } else {
                    Text("09:30", color = TextMuted, fontSize = 9.sp)
                    Text("10:30", color = TextMuted, fontSize = 9.sp)
                    Text("11:30/13:00", color = TextMuted, fontSize = 9.sp)
                    Text("14:00", color = TextMuted, fontSize = 9.sp)
                    Text("15:00", color = TextMuted, fontSize = 9.sp)
                }
            } else {
                val dayFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
                val startDay = candles.firstOrNull()?.let { dayFormat.format(Date(it.timestamp)) } ?: ""
                val midDay = candles.getOrNull(candles.size / 2)?.let { dayFormat.format(Date(it.timestamp)) } ?: ""
                val endDay = candles.lastOrNull()?.let { dayFormat.format(Date(it.timestamp)) } ?: ""
                Text(startDay, color = TextMuted, fontSize = 9.sp)
                Text(midDay, color = TextMuted, fontSize = 9.sp)
                Text(endDay, color = TextMuted, fontSize = 9.sp)
            }
        }
    }
}

private fun DrawScope.drawEastMoneyGrid(
    width: Float,
    priceHeight: Float,
    volTop: Float,
    volHeight: Float,
    chartType: ChartType,
    hasPrevClose: Boolean,
    auctionX: Float? = null
) {
    val gridColor = SurfaceBorder.copy(alpha = 0.5f)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))

    // Middle reference line (for Intraday, this is previous close)
    val midY = priceHeight / 2f
    drawLine(
        color = if (hasPrevClose) Color(0xFF6B7280).copy(alpha = 0.8f) else gridColor,
        start = Offset(0f, midY),
        end = Offset(width, midY),
        strokeWidth = 1.2f,
        pathEffect = dashEffect
    )

    // Top & bottom quarter lines
    val topY = priceHeight * 0.25f
    val botY = priceHeight * 0.75f
    drawLine(color = gridColor, start = Offset(0f, topY), end = Offset(width, topY), strokeWidth = 1f, pathEffect = dashEffect)
    drawLine(color = gridColor, start = Offset(0f, botY), end = Offset(width, botY), strokeWidth = 1f, pathEffect = dashEffect)

    // Vertical auction delimiter (09:15-09:25 vs 09:30)
    if (auctionX != null && auctionX > 0f) {
        val auctionDash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        drawLine(
            color = EastMoneyOrange.copy(alpha = 0.6f),
            start = Offset(auctionX, 0f),
            end = Offset(auctionX, priceHeight),
            strokeWidth = 1.2f,
            pathEffect = auctionDash
        )
        drawLine(
            color = EastMoneyOrange.copy(alpha = 0.4f),
            start = Offset(auctionX, volTop),
            end = Offset(auctionX, volTop + volHeight),
            strokeWidth = 1f,
            pathEffect = auctionDash
        )
    }

    // Divider between price and volume
    drawLine(
        color = SurfaceBorder,
        start = Offset(0f, priceHeight + 4f),
        end = Offset(width, priceHeight + 4f),
        strokeWidth = 1.5f
    )
}

private fun DrawScope.drawMALine(
    maValues: List<Double?>,
    candleWidth: Float,
    minPrice: Double,
    priceSpan: Double,
    chartHeight: Float,
    color: Color,
    yOffset: Float = 0f
) {
    val path = Path()
    var started = false

    maValues.forEachIndexed { i, ma ->
        if (ma != null) {
            val x = i * candleWidth + (candleWidth / 2f)
            val y = yOffset + (chartHeight - ((ma - minPrice) / priceSpan * chartHeight).toFloat())
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
    }

    if (started) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.6f, cap = StrokeCap.Round)
        )
    }
}

private fun calculateMA(candles: List<CandlePoint>, period: Int): List<Double?> {
    val result = mutableListOf<Double?>()
    for (i in candles.indices) {
        if (i < period - 1) {
            result.add(null)
        } else {
            val sum = (0 until period).sumOf { candles[i - it].close }
            result.add(sum / period)
        }
    }
    return result
}

private fun calculateVolMA(candles: List<CandlePoint>, period: Int): List<Double?> {
    val result = mutableListOf<Double?>()
    for (i in candles.indices) {
        if (i < period - 1) {
            result.add(null)
        } else {
            val sum = (0 until period).sumOf { candles[i - it].volume }
            result.add(sum.toDouble() / period)
        }
    }
    return result
}

private fun calculateCandleIndex(touchX: Float, width: Float, candleCount: Int): Int {
    if (candleCount <= 0 || width <= 0f) return 0
    val index = (touchX / width * candleCount).toInt()
    return index.coerceIn(0, candleCount - 1)
}

fun formatVolumeInLots(vol: Long): String {
    // 1 lot (手) = 100 shares
    val lots = vol / 100
    return when {
        lots >= 100_000_000 -> String.format(Locale.US, "%.2f亿手", lots / 100_000_000.0)
        lots >= 10_000 -> String.format(Locale.US, "%.2f万手", lots / 10_000.0)
        lots > 0 -> "${lots}手"
        else -> vol.toString()
    }
}

private fun calculateVWAP(candles: List<CandlePoint>): List<Double?> {
    var cumVol = 0.0
    var cumAmt = 0.0
    return candles.map { candle ->
        val vol = candle.volume.toDouble()
        val tp = (candle.open + candle.high + candle.low + candle.close) / 4.0
        if (vol > 0.0) {
            cumVol += vol
            cumAmt += tp * vol
        }
        if (cumVol > 0.0) cumAmt / cumVol else candle.close
    }
}


