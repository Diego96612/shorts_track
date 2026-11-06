package com.shortvideo.tracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortvideo.tracker.ui.theme.ShortVideoTrackerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

enum class Tab(val label: String) {
    OVERVIEW("總覽"),
    STATS("統計")
}

enum class StatsPeriod { DAILY, WEEKLY }

/** 底部有「總覽 / 統計」兩個分頁 */
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

/** 總覽：三個平台今天、本週的使用時間，加上設定與匯出按鈕 */
@Composable
fun OverviewScreen() {
    val context = LocalContext.current
    val ytToday = remember { mutableLongStateOf(0L) }
    val ytWeek = remember { mutableLongStateOf(0L) }
    val igToday = remember { mutableLongStateOf(0L) }
    val igWeek = remember { mutableLongStateOf(0L) }
    val ttToday = remember { mutableLongStateOf(0L) }
    val ttWeek = remember { mutableLongStateOf(0L) }

    fun refresh() {
        loadOverview(context, ytToday, ytWeek, igToday, igWeek, ttToday, ttWeek)
    }

    // 讓使用者選擇 CSV 存放位置，再把資料寫進去
    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(TrackingData.exportCsv(context).toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "CSV 檔案已匯出", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "匯出失敗：${e.message}", Toast.LENGTH_SHORT).show()
        }
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

        StatCard("YouTube Shorts", ytToday.longValue, ytWeek.longValue, ColorYouTube)
        StatCard("Instagram Reels", igToday.longValue, igWeek.longValue, ColorInstagram)
        StatCard("TikTok 抖音", ttToday.longValue, ttWeek.longValue, ColorTikTok)

        HorizontalDivider()

        Text("合計", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "今天：${formatTime(ytToday.longValue + igToday.longValue + ttToday.longValue)}",
            fontSize = 16.sp
        )
        Text(
            "本週：${formatTime(ytWeek.longValue + igWeek.longValue + ttWeek.longValue)}",
            fontSize = 16.sp
        )

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
                val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                csvExportLauncher.launch("短影音使用時間_$date.csv")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("匯出${TrackingData.EXPORT_DAYS}天資料（CSV檔案）")
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun loadOverview(
    context: Context,
    ytToday: MutableLongState,
    ytWeek: MutableLongState,
    igToday: MutableLongState,
    igWeek: MutableLongState,
    ttToday: MutableLongState,
    ttWeek: MutableLongState
) {
    ytToday.longValue = TrackingData.getTodayTotal(context, "yt")
    ytWeek.longValue = TrackingData.getWeeklyTotal(context, "yt")
    igToday.longValue = TrackingData.getTodayTotal(context, "ig")
    igWeek.longValue = TrackingData.getWeeklyTotal(context, "ig")
    ttToday.longValue = TrackingData.getTodayTotal(context, "tt")
    ttWeek.longValue = TrackingData.getWeeklyTotal(context, "tt")
}

@Composable
fun StatCard(title: String, today: Long, week: Long, color: Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            MiniBarComparison(today, week, color)
        }
    }
}

/** 統計：近 7 天每日，或近 6 週每週的三平台長條圖 */
@Composable
fun StatsScreen() {
    val context = LocalContext.current
    var period by remember { mutableStateOf(StatsPeriod.DAILY) }
    var entries by remember { mutableStateOf(emptyList<GroupedBarEntry>()) }

    fun refresh() {
        entries = if (period == StatsPeriod.DAILY) {
            val yt = TrackingData.getDailyBreakdown(context, "yt", 7)
            val ig = TrackingData.getDailyBreakdown(context, "ig", 7)
            val tt = TrackingData.getDailyBreakdown(context, "tt", 7)
            yt.indices.map { i ->
                GroupedBarEntry(yt[i].label, yt[i].seconds, ig[i].seconds, tt[i].seconds)
            }
        } else {
            val yt = TrackingData.getWeeklyBreakdown(context, "yt", 6)
            val ig = TrackingData.getWeeklyBreakdown(context, "ig", 6)
            val tt = TrackingData.getWeeklyBreakdown(context, "tt", 6)
            yt.indices.map { i ->
                GroupedBarEntry(yt[i].weekLabel, yt[i].seconds, ig[i].seconds, tt[i].seconds)
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

        SingleChoiceSegment(selected = period, onSelect = { period = it })

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (period == StatsPeriod.DAILY) "近7天每日比較" else "近6週每週比較",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                GroupedBarChart(entries)
            }
        }

        Button(
            onClick = { refresh() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("重新整理")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SingleChoiceSegment(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
