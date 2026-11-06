package com.shortvideo.tracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ColorYouTube = Color(0xFFFF5252)
val ColorInstagram = Color(0xFFE1306C)
val ColorTikTok = Color(0xFF00C2CB)

/** 統計圖的一組資料：一個日期（或一週）三個平台的秒數 */
data class GroupedBarEntry(
    val label: String,
    val yt: Long,
    val ig: Long,
    val tt: Long
)

/** 總覽卡片裡「今天 / 本週」兩條橫條 */
@Composable
fun MiniBarComparison(today: Long, week: Long, color: Color) {
    val maxVal = maxOf(today, maxOf(week, 1L))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MiniBarRow("今天", today, maxVal, color)
        MiniBarRow("本週", week, maxVal, color.copy(alpha = 0.55f))
    }
}

@Composable
private fun MiniBarRow(label: String, value: Long, maxVal: Long, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.width(40.dp), fontSize = 12.sp)
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
        ) {
            val ratio = (value.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
            drawRoundRect(
                color = color.copy(alpha = 0.15f),
                size = size,
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = color,
                size = Size(size.width * ratio, size.height),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(formatTimeShort(value), modifier = Modifier.width(48.dp), fontSize = 11.sp)
    }
}

/** 統計頁的分組長條圖：每個日期（或週）一組，三個平台各一根 */
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
            val leftPad = 4.dp.toPx()
            val chartHeight = size.height - 28.dp.toPx()
            val groupWidth = (size.width - leftPad) / entries.size
            val barWidth = groupWidth / 5f

            // 底線
            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(leftPad, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 2f
            )

            entries.forEachIndexed { i, entry ->
                val groupX = leftPad + i * groupWidth
                listOf(
                    entry.yt to ColorYouTube,
                    entry.ig to ColorInstagram,
                    entry.tt to ColorTikTok
                ).forEachIndexed { j, (value, color) ->
                    val barHeight = value.toFloat() / maxVal.toFloat() * chartHeight
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(groupX + (j * 1.3f + 0.5f) * barWidth, chartHeight - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }
        }

        // X 軸標籤
        Row(modifier = Modifier.fillMaxWidth()) {
            entries.forEach { entry ->
                Text(
                    text = entry.label,
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
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
            drawRoundRect(color = color, size = size)
        }
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
