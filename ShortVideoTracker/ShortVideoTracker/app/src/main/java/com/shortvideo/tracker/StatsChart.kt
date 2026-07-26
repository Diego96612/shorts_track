package com.shortvideo.tracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ColorYouTube = Color(0xFFFF5252)
val ColorInstagram = Color(0xFFE1306C)
val ColorTikTok = Color(0xFF00C2CB)

/**
 * 總覽卡片裡的小型今天/本週比較長條圖（一組兩條橫向長條）
 */
@Composable
fun MiniBarComparison(today: Long, week: Long, barColor: Color) {
    val maxVal = maxOf(today, week, 1L)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MiniBarRow(label = "今天", value = today, maxVal = maxVal, color = barColor)
        MiniBarRow(label = "本週", value = week, maxVal = maxVal, color = barColor.copy(alpha = 0.55f))
    }
}

@Composable
private fun MiniBarRow(label: String, value: Long, maxVal: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, modifier = Modifier.width(40.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
        ) {
            val fraction = (value.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
            // 背景軌道
            drawRoundRect(
                color = color.copy(alpha = 0.15f),
                size = Size(size.width, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            // 實際數值長條
            drawRoundRect(
                color = color,
                size = Size(size.width * fraction, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(formatTimeShort(value), fontSize = 11.sp, modifier = Modifier.width(48.dp))
    }
}

/** 給統計頁用的一組資料：一個時間標籤（例如日期或週次）+ 三平台的秒數 */
data class GroupedBarEntry(
    val label: String,
    val yt: Long,
    val ig: Long,
    val tt: Long
)

/**
 * 三平台分組長條圖。X 軸是時間（每日或每週），每個時間點有三根不同顏色的長條。
 */
@Composable
fun GroupedBarChart(entries: List<GroupedBarEntry>, modifier: Modifier = Modifier) {
    val maxVal = entries.flatMap { listOf(it.yt, it.ig, it.tt) }.maxOrNull()?.coerceAtLeast(1L) ?: 1L

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            if (entries.isEmpty()) return@Canvas

            val leftPadding = 4.dp.toPx()
            val bottomPadding = 28.dp.toPx()
            val chartHeight = size.height - bottomPadding
            val chartWidth = size.width - leftPadding

            val groupWidth = chartWidth / entries.size
            val barWidth = groupWidth / 5f // 3根長條 + 間距

            // 底部橫軸線
            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(leftPadding, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 2f
            )

            entries.forEachIndexed { index, entry ->
                val groupStart = leftPadding + index * groupWidth
                val bars = listOf(
                    entry.yt to ColorYouTube,
                    entry.ig to ColorInstagram,
                    entry.tt to ColorTikTok
                )
                bars.forEachIndexed { barIndex, (value, color) ->
                    val barHeight = (value.toFloat() / maxVal.toFloat()) * chartHeight
                    val x = groupStart + barWidth * (barIndex * 1.3f + 0.5f)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, chartHeight - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }
        }

        // X 軸標籤
        Row(modifier = Modifier.fillMaxWidth()) {
            entries.forEach { entry ->
                Text(
                    entry.label,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        LegendRow()
    }
}

@Composable
private fun LegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        LegendItem("YouTube", ColorYouTube)
        Spacer(Modifier.width(16.dp))
        LegendItem("Instagram", ColorInstagram)
        Spacer(Modifier.width(16.dp))
        LegendItem("TikTok", ColorTikTok)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawRoundRect(color = color, size = Size(size.width, size.height))
        }
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
