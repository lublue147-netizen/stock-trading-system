package com.stockmarket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.stockmarket.app.ui.theme.*

@Composable
fun TimeframeSelector(
    selectedRange: String,
    onRangeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        "分时" to "1d",
        "五日" to "5d",
        "日K" to "1mo",
        "周K" to "1y",
        "月K" to "5y",
        "全部" to "all"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        options.forEach { (label, value) ->
            val isSelected = selectedRange == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) PrimaryBlue else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onRangeSelected(value) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) androidx.compose.ui.graphics.Color.White else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
