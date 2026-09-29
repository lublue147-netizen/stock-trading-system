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
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmarket.app.data.model.CandlePoint
import com.stockmarket.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

enum class ChartType {
    CANDLESTICK,
    LINE
}

@Composable
fun CandlestickChart(
    candles: List<CandlePoint>,
    chartType: ChartType = ChartType.CANDLESTICK,
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
            Text("暂无行情数据", color = TextSecondary, fontSize = 14.sp)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val stockColors = LocalStockColors.current

    val ma5 = remember(candles) { calculateMA(candles, 5) }
    val ma10 = remember(candles) { calculateMA(candles, 10) }
    val ma20 = remember(candles) { calculateMA(candles, 20) }

    val activeCandle = selectedIndex?.let { candles.getOrNull(it) } ?: candles.lastOrNull()
    val activeMA5 = selectedIndex?.let { ma5.getOrNull(it) } ?: ma5.lastOrNull()
    val activeMA10 = selectedIndex?.let { ma10.getOrNull(it) } ?: ma10.lastOrNull()
    val activeMA20 = selectedIndex?.let { ma20.getOrNull(it) } ?: ma20.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(12.dp)
    ) {
        // Top Info & Indicator Legend
        Column(modifier = Modifier.fillMaxWidth()) {
            if (activeCandle != null) {
                val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
                val dateStr = dateFormat.format(Date(activeCandle.timestamp))
                val isUp = activeCandle.close >= activeCandle.open
                val candleColor = if (isUp) stockColors.upColor else stockColors.downColor

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "收: ${String.format(Locale.US, "%.2f", activeCandle.close)}",
                        color = candleColor,
                        fontSize = 13.sp,
                        style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("开: ${String.format(Locale.US, "%.2f", activeCandle.open)}", color = TextMuted, fontSize = 11.sp)
                    Text("高: ${String.format(Locale.US, "%.2f", activeCandle.high)}", color = TextMuted, fontSize = 11.sp)
                    Text("低: ${String.format(Locale.US, "%.2f", activeCandle.low)}", color = TextMuted, fontSize = 11.sp)
                    Text("量: ${formatVolume(activeCandle.volume)}", color = TextMuted, fontSize = 11.sp)
                }
            }

            if (showMA) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    activeMA5?.let { Text("MA5: ${String.format(Locale.US, "%.2f", it)}", color = MA5Color, fontSize = 11.sp) }
                    activeMA10?.let { Text("MA10: ${String.format(Locale.US, "%.2f", it)}", color = MA10Color, fontSize = 11.sp) }
                    activeMA20?.let { Text("MA20: ${String.format(Locale.US, "%.2f", it)}", color = MA20Color, fontSize = 11.sp) }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Chart Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
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
                            // keep last selected or auto reset after a bit
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height

            val priceChartHeight = height * 0.72f
            val volumeChartHeight = height * 0.22f
            val volumeChartTop = height * 0.78f

            val minPrice = candles.minOf { it.low }
            val maxPrice = candles.maxOf { it.high }
            val priceSpan = max(0.01, maxPrice - minPrice) * 1.05
            val adjustedMinPrice = minPrice - (priceSpan * 0.02)

            val maxVolume = max(1L, candles.maxOf { it.volume }).toFloat()

            // Draw Background Grid
            drawGrid(width, priceChartHeight, volumeChartTop, volumeChartHeight)

            val candleCount = candles.size
            val candleWidth = width / candleCount
            val barWidth = max(2f, candleWidth * 0.7f)

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
                        color = color.copy(alpha = 0.8f),
                        topLeft = Offset(x - barWidth / 2f, height - volHeight),
                        size = Size(barWidth, volHeight)
                    )
                }
            } else {
                // LINE CHART with Gradient Fill
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

                    // Volume Bar
                    val isUp = candle.close >= candle.open
                    val color = if (isUp) stockColors.upColor else stockColors.downColor
                    val volHeight = (candle.volume.toFloat() / maxVolume) * volumeChartHeight
                    drawRect(
                        color = color.copy(alpha = 0.8f),
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
                        colors = listOf(PrimaryBlue.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = priceChartHeight
                    )
                )

                // Draw line
                drawPath(
                    path = linePath,
                    color = PrimaryBlue,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }

            // Draw MA Curves
            if (showMA) {
                drawMALine(ma5, candleWidth, adjustedMinPrice, priceSpan, priceChartHeight, MA5Color)
                drawMALine(ma10, candleWidth, adjustedMinPrice, priceSpan, priceChartHeight, MA10Color)
                drawMALine(ma20, candleWidth, adjustedMinPrice, priceSpan, priceChartHeight, MA20Color)
            }

            // Crosshair overlay if selected
            selectedIndex?.let { idx ->
                if (idx in candles.indices) {
                    val candle = candles[idx]
                    val crosshairX = idx * candleWidth + (candleWidth / 2f)
                    val crosshairY = priceChartHeight - ((candle.close - adjustedMinPrice) / priceSpan * priceChartHeight).toFloat()

                    // Vertical Line
                    drawLine(
                        color = TextSecondary.copy(alpha = 0.7f),
                        start = Offset(crosshairX, 0f),
                        end = Offset(crosshairX, height),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )

                    // Horizontal Line
                    drawLine(
                        color = TextSecondary.copy(alpha = 0.7f),
                        start = Offset(0f, crosshairY),
                        end = Offset(width, crosshairY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )

                    // Center dot
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(crosshairX, crosshairY)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(
    width: Float,
    priceHeight: Float,
    volTop: Float,
    volHeight: Float
) {
    val gridColor = SurfaceBorder.copy(alpha = 0.6f)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))

    // 3 Horizontal lines in price chart
    for (i in 1..3) {
        val y = priceHeight * (i / 4f)
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f,
            pathEffect = dashEffect
        )
    }

    // Divider between price and volume
    drawLine(
        color = SurfaceBorder,
        start = Offset(0f, priceHeight + 8f),
        end = Offset(width, priceHeight + 8f),
        strokeWidth = 1.5f
    )
}

private fun DrawScope.drawMALine(
    maValues: List<Double?>,
    candleWidth: Float,
    minPrice: Double,
    priceSpan: Double,
    chartHeight: Float,
    color: Color
) {
    val path = Path()
    var started = false

    maValues.forEachIndexed { i, ma ->
        if (ma != null) {
            val x = i * candleWidth + (candleWidth / 2f)
            val y = chartHeight - ((ma - minPrice) / priceSpan * chartHeight).toFloat()
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
            style = Stroke(width = 2f, cap = StrokeCap.Round)
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

private fun calculateCandleIndex(touchX: Float, width: Float, candleCount: Int): Int {
    if (candleCount <= 0 || width <= 0f) return 0
    val index = (touchX / width * candleCount).toInt()
    return index.coerceIn(0, candleCount - 1)
}

private fun formatVolume(vol: Long): String {
    return when {
        vol >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", vol / 1_000_000_000.0)
        vol >= 1_000_000 -> String.format(Locale.US, "%.2fM", vol / 1_000_000.0)
        vol >= 1_000 -> String.format(Locale.US, "%.1fK", vol / 1_000.0)
        else -> vol.toString()
    }
}
