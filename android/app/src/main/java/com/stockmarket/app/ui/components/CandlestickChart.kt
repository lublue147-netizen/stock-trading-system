package com.stockmarket.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    chartType: ChartType = ChartType.CANDLESTICK,
    previousClose: Double? = null,
    modifier: Modifier = Modifier,
    showMA: Boolean = true
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

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val stockColors = LocalStockColors.current

    val ma5 = remember(candles) { calculateMA(candles, 5) }
    val ma10 = remember(candles) { calculateMA(candles, 10) }
    val ma20 = remember(candles) { calculateMA(candles, 20) }

    val vol5 = remember(candles) { calculateVolMA(candles, 5) }
    val vol10 = remember(candles) { calculateVolMA(candles, 10) }

    val activeCandle = selectedIndex?.let { candles.getOrNull(it) } ?: candles.lastOrNull()
    val activeMA5 = selectedIndex?.let { ma5.getOrNull(it) } ?: ma5.lastOrNull()
    val activeMA10 = selectedIndex?.let { ma10.getOrNull(it) } ?: ma10.lastOrNull()
    val activeMA20 = selectedIndex?.let { ma20.getOrNull(it) } ?: ma20.lastOrNull()

    val activeVol5 = selectedIndex?.let { vol5.getOrNull(it) } ?: vol5.lastOrNull()
    val activeVol10 = selectedIndex?.let { vol10.getOrNull(it) } ?: vol10.lastOrNull()

    val baseClose = previousClose ?: candles.firstOrNull()?.open ?: 100.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(10.dp)
    ) {
        // Top Info & Indicator Legend
        Column(modifier = Modifier.fillMaxWidth()) {
            if (activeCandle != null) {
                val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
                val dateStr = dateFormat.format(Date(activeCandle.timestamp))
                val candleDelta = activeCandle.close - (activeCandle.open)
                val candleDeltaPercent = if (activeCandle.open > 0) (candleDelta / activeCandle.open) * 100 else 0.0
                val isUp = activeCandle.close >= activeCandle.open
                val candleColor = if (isUp) stockColors.upColor else stockColors.downColor
                val prefix = if (isUp) "+" else ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "收: ${String.format(Locale.US, "%.2f", activeCandle.close)}",
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
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("开: ${String.format(Locale.US, "%.2f", activeCandle.open)}", color = TextMuted, fontSize = 10.sp)
                    Text("高: ${String.format(Locale.US, "%.2f", activeCandle.high)}", color = TextMuted, fontSize = 10.sp)
                    Text("低: ${String.format(Locale.US, "%.2f", activeCandle.low)}", color = TextMuted, fontSize = 10.sp)
                    Text("量: ${formatVolumeInLots(activeCandle.volume)}", color = TextMuted, fontSize = 10.sp)
                }
            }

            if (showMA && chartType == ChartType.CANDLESTICK) {
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

        Spacer(modifier = Modifier.height(6.dp))

        // Main Chart Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .pointerInput(candles) {
                    detectTapGestures(
                        onPress = { offset ->
                            selectedIndex = calculateCandleIndex(offset.x, size.width.toFloat(), candles.size)
                        },
                        onTap = { offset ->
                            selectedIndex = calculateCandleIndex(offset.x, size.width.toFloat(), candles.size)
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
                // For Intraday Line, calculate symmetric bounds around previous close
                val maxDev = candles.maxOfOrNull { abs(it.close - previousClose) } ?: (previousClose * 0.02)
                val safeDev = max(maxDev, previousClose * 0.01) * 1.15
                minPrice = previousClose - safeDev
                maxPrice = previousClose + safeDev
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

            val maxVolume = max(1L, candles.maxOf { it.volume }).toFloat()

            // Draw Background Grid
            drawEastMoneyGrid(
                width = width,
                priceHeight = priceChartHeight,
                volTop = volumeChartTop,
                volHeight = volumeChartHeight,
                chartType = chartType,
                hasPrevClose = chartType == ChartType.LINE && previousClose != null
            )

            val candleCount = candles.size
            val candleWidth = width / candleCount
            val barWidth = max(2f, candleWidth * 0.72f)

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
                // INTRADAY LINE CHART with East Money smooth gradient
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

                    // Volume Bar in Intraday: color based on close >= prev close or open
                    val prevPrice = if (i > 0) candles[i - 1].close else candle.open
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

                // Draw gradient under line
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryBlue.copy(alpha = 0.40f), PrimaryBlue.copy(alpha = 0.03f)),
                        startY = 0f,
                        endY = priceChartHeight
                    )
                )

                // Draw line
                drawPath(
                    path = linePath,
                    color = PrimaryBlue,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
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
    }
}

private fun DrawScope.drawEastMoneyGrid(
    width: Float,
    priceHeight: Float,
    volTop: Float,
    volHeight: Float,
    chartType: ChartType,
    hasPrevClose: Boolean
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

private fun formatVolumeInLots(vol: Long): String {
    // 1 lot (手) = 100 shares
    val lots = vol / 100
    return when {
        lots >= 100_000_000 -> String.format(Locale.US, "%.2f亿手", lots / 100_000_000.0)
        lots >= 10_000 -> String.format(Locale.US, "%.2f万手", lots / 10_000.0)
        lots > 0 -> "${lots}手"
        else -> vol.toString()
    }
}

