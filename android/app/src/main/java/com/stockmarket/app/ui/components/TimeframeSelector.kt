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

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            options.forEach { (label, value) ->
                val isSelected = selectedRange == value
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onRangeSelected(value) }
                        .padding(top = 10.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) EastMoneyRed else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(EastMoneyRed)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.5.dp))
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(SurfaceBorder.copy(alpha = 0.5f))
        )
    }
}
