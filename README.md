# shorts_track
package com.shortvideo.tracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortvideo.tracker.ui.theme.ShortVideoTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShortVideoTrackerTheme {
                TrackerScreen()
            }
        }
    }
}

@Composable
fun TrackerScreen() {
    val context = LocalContext.current
    var ytToday by remember { mutableLongStateOf(0L) }
    var ytWeek by remember { mutableLongStateOf(0L) }
    var igToday by remember { mutableLongStateOf(0L) }
    var igWeek by remember { mutableLongStateOf(0L) }
    var ttToday by remember { mutableLongStateOf(0L) }
    var ttWeek by remember { mutableLongStateOf(0L) }

    fun refresh() {
        ytToday = TrackingService.getTodayTotal(context, "yt")
        ytWeek = TrackingService.getWeeklyTotal(context, "yt")
        igToday = TrackingService.getTodayTotal(context, "ig")
        igWeek = TrackingService.getWeeklyTotal(context, "ig")
        ttToday = TrackingService.getTodayTotal(context, "tt")
        ttWeek = TrackingService.getWeeklyTotal(context, "tt")
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("短影音使用時間", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))

            StatCard(
                title = "YouTube Shorts",
                today = ytToday,
                week = ytWeek
            )

            StatCard(
                title = "Instagram Reels",
                today = igToday,
                week = igWeek
            )

            StatCard(
                title = "TikTok 抖音",
                today = ttToday,
                week = ttWeek
            )

            HorizontalDivider()

            Text("合計", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("今天：${formatTime(ytToday + igToday + ttToday)}", fontSize = 16.sp)
            Text("本週：${formatTime(ytWeek + igWeek + ttWeek)}", fontSize = 16.sp)

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("開啟無障礙服務（第一次必做）")
            }

            Button(
                onClick = { refresh() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("重新整理數據")
            }

            OutlinedButton(
                onClick = {
                    val csv = TrackingService.exportCsv(context)
                    val clip = ClipData.newPlainText("short_video_data", csv)
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(clip)
                    Toast.makeText(context, "已複製到剪貼簿（30天CSV資料）", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("匯出30天資料（複製CSV）")
            }
        }
    }
}

@Composable
fun StatCard(title: String, today: Long, week: Long) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("今天：${formatTime(today)}", fontSize = 15.sp)
            Text("本週：${formatTime(week)}", fontSize = 15.sp)
        }
    }
}

fun formatTime(seconds: Long): String {
    if (seconds <= 0) return "0 秒"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return buildString {
        if (h > 0) append("${h}小時")
        if (m > 0) append("${m}分")
        append("${s}秒")
    }
}
