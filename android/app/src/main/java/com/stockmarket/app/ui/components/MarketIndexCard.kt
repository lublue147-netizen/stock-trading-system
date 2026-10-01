package com.stockmarket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmarket.app.data.model.MarketIndex
import androidx.compose.foundation.clickable
import com.stockmarket.app.ui.theme.*
import java.util.Locale

@Composable
fun MarketIndexCard(
    index: MarketIndex,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val stockColors = LocalStockColors.current
    val isPositive = index.change >= 0
    val color = if (isPositive) stockColors.upColor else stockColors.downColor
    val prefix = if (isPositive) "+" else ""

    Box(
        modifier = modifier
            .width(130.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = index.name,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "%.2f", index.price),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${prefix}${String.format(Locale.US, "%.2f", index.change)}",
                    color = color,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${prefix}${String.format(Locale.US, "%.2f%%", index.changePercent)})",
                    color = color,
                    fontSize = 11.sp
                )
            }
        }
    }
}
