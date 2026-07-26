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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
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
                RootScreen()
            }
        }
    }
}

private enum class Tab(val label: String) { OVERVIEW("總覽"), STATS("統計") }

@Composable
fun RootScreen() {
    var selectedTab by remember { mutableStateOf(Tab.OVERVIEW) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == Tab.OVERVIEW,
                    onClick = { selectedTab = Tab.OVERVIEW },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(Tab.OVERVIEW.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.STATS,
                    onClick = { selectedTab = Tab.STATS },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                    label = { Text(Tab.STATS.label) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                Tab.OVERVIEW -> OverviewScreen()
                Tab.STATS -> StatsScreen()
            }
        }
    }
}

@Composable
fun OverviewScreen() {
    val context = LocalContext.current
    var ytToday by remember { mutableLongStateOf(0L) }
    var ytWeek by remember { mutableLongStateOf(0L) }
    var igToday by remember { mutableLongStateOf(0L) }
    var igWeek by remember { mutableLongStateOf(0L) }
    var ttToday by remember { mutableLongStateOf(0L) }
    var ttWeek by remember { mutableLongStateOf(0L) }

    fun refresh() {
        ytToday = TrackingData.getTodayTotal(context, "yt")
        ytWeek = TrackingData.getWeeklyTotal(context, "yt")
        igToday = TrackingData.getTodayTotal(context, "ig")
        igWeek = TrackingData.getWeeklyTotal(context, "ig")
        ttToday = TrackingData.getTodayTotal(context, "tt")
        ttWeek = TrackingData.getWeeklyTotal(context, "tt")
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("短影音使用時間", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(4.dp))

        StatCard(title = "YouTube Shorts", today = ytToday, week = ytWeek, color = ColorYouTube)
        StatCard(title = "Instagram Reels", today = igToday, week = igWeek, color = ColorInstagram)
        StatCard(title = "TikTok 抖音", today = ttToday, week = ttWeek, color = ColorTikTok)

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
                val csv = TrackingData.exportCsv(context)
                val clip = ClipData.newPlainText("short_video_data", csv)
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(clip)
                Toast.makeText(context, "已複製到剪貼簿（30天CSV資料）", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("匯出30天資料（複製CSV）")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun StatCard(title: String, today: Long, week: Long, color: androidx.compose.ui.graphics.Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            MiniBarComparison(today = today, week = week, barColor = color)
        }
    }
}

private enum class StatsPeriod { DAILY, WEEKLY }

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    var period by remember { mutableStateOf(StatsPeriod.DAILY) }
    var entries by remember { mutableStateOf(listOf<GroupedBarEntry>()) }

    fun refresh() {
        entries = if (period == StatsPeriod.DAILY) {
            val yt = TrackingData.getDailyBreakdown(context, "yt", 7)
            val ig = TrackingData.getDailyBreakdown(context, "ig", 7)
            val tt = TrackingData.getDailyBreakdown(context, "tt", 7)
            yt.indices.map { i ->
                GroupedBarEntry(label = yt[i].label, yt = yt[i].seconds, ig = ig[i].seconds, tt = tt[i].seconds)
            }
        } else {
            val yt = TrackingData.getWeeklyBreakdown(context, "yt", 6)
            val ig = TrackingData.getWeeklyBreakdown(context, "ig", 6)
            val tt = TrackingData.getWeeklyBreakdown(context, "tt", 6)
            yt.indices.map { i ->
                GroupedBarEntry(label = yt[i].weekLabel, yt = yt[i].seconds, ig = ig[i].seconds, tt = tt[i].seconds)
            }
        }
    }

    LaunchedEffect(period) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("統計", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        SingleChoiceSegment(
            selected = period,
            onSelect = { period = it }
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (period == StatsPeriod.DAILY) "近7天每日比較" else "近6週每週比較",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                GroupedBarChart(entries = entries)
            }
        }

        Button(onClick = { refresh() }, modifier = Modifier.fillMaxWidth()) {
            Text("重新整理")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SingleChoiceSegment(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == StatsPeriod.DAILY,
            onClick = { onSelect(StatsPeriod.DAILY) },
            label = { Text("每日") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selected == StatsPeriod.WEEKLY,
            onClick = { onSelect(StatsPeriod.WEEKLY) },
            label = { Text("每週") },
            modifier = Modifier.weight(1f)
        )
    }
}
