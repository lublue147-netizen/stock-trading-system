package com.stockmarket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import java.util.Locale

@Composable
fun IndustryTagPill(
    industryName: String,
    bkCode: String?,
    changePercent: Double?,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val stockColors = LocalStockColors.current
    val chg = changePercent ?: 0.0
    val isUp = chg >= 0.0
    val chgColor = if (isUp) stockColors.upColor else stockColors.downColor
    val prefix = if (isUp) "+" else ""
    val hasChange = changePercent != null

    val isClickable = onClick != null && !bkCode.isNullOrEmpty()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(SurfaceBorder.copy(alpha = 0.5f))
            .border(0.6.dp, SurfaceBorder, RoundedCornerShape(3.dp))
            .then(if (isClickable) Modifier.clickable(onClick = onClick!!) else Modifier)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = industryName,
            color = PrimaryBlue,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        if (hasChange) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$prefix${String.format(Locale.US, "%.2f%%", chg)}",
                color = chgColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (isClickable) {
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "›",
                color = PrimaryBlue.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
