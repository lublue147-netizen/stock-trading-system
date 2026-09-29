package com.stockmarket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmarket.app.data.model.StockQuote
import com.stockmarket.app.ui.theme.*
import java.util.Locale

@Composable
fun StockCard(
    quote: StockQuote,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val stockColors = LocalStockColors.current
    val isPositive = quote.change >= 0
    val color = if (isPositive) stockColors.upColor else stockColors.downColor
    val prefix = if (isPositive) "+" else ""

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Symbol, Exchange & Company Name
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = quote.symbol,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (quote.exchange.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceBorder)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = quote.exchange,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = quote.name,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // Right: Price & Change Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    val currSign = when (quote.currency) {
                        "CNY" -> "¥"
                        "HKD" -> "HK$"
                        "点" -> ""
                        else -> "$"
                    }
                    Text(
                        text = "$currSign${String.format(Locale.US, "%.2f", quote.price)}",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${prefix}${String.format(Locale.US, "%.2f", quote.change)}",
                        color = color,
                        fontSize = 12.sp
                    )
                }

                // Change % Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${prefix}${String.format(Locale.US, "%.2f%%", quote.changePercent)}",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onRemove != null) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除自选",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
