package com.shortvideo.tracker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 使用時間的儲存與統計。
 *
 * 資料存在 SharedPreferences 的一個 JSON 字串裡，格式：
 * `{"2026-11-06": {"yt": 120, "ig": 30, "tt": 0}, ...}`，單位是秒。
 * 平台代號：yt = YouTube Shorts，ig = Instagram Reels，tt = TikTok。
 */
object TrackingData {
    private const val PREFS_NAME = "short_video_tracker_prefs"
    private const val KEY_DATA = "usage_data_json"
    const val EXPORT_DAYS = 50

    val PLATFORMS = listOf("yt", "ig", "tt")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    data class DailyPoint(val dateKey: String, val label: String, val seconds: Long)
    data class WeeklyPoint(val weekLabel: String, val seconds: Long)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readRoot(context: Context): JSONObject {
        val json = prefs(context).getString(KEY_DATA, null) ?: return JSONObject()
        return try {
            JSONObject(json)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun writeRoot(context: Context, root: JSONObject) {
        prefs(context).edit().putString(KEY_DATA, root.toString()).apply()
    }

    private fun todayKey(): String = dateFormat.format(Date())

    /** 把秒數加到今天、指定平台的累計時間上 */
    @Synchronized
    fun addSeconds(context: Context, platform: String, seconds: Long) {
        if (seconds <= 0) return
        val root = readRoot(context)
        val key = todayKey()
        val day = root.optJSONObject(key) ?: JSONObject().also { root.put(key, it) }
        day.put(platform, day.optLong(platform, 0L) + seconds)
        writeRoot(context, root)
    }

    fun getDailySeconds(context: Context, platform: String, dateKey: String): Long {
        val day = readRoot(context).optJSONObject(dateKey) ?: return 0L
        return day.optLong(platform, 0L)
    }

    fun getTodayTotal(context: Context, platform: String): Long =
        getDailySeconds(context, platform, todayKey())

    /** 本週（從星期一算起到今天）的累計秒數 */
    fun getWeeklyTotal(context: Context, platform: String): Long {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        var total = 0L
        val now = Calendar.getInstance()
        while (!cal.after(now)) {
            total += getDailySeconds(context, platform, dateFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return total
    }

    /** 最近 [days] 天（含今天）每天的秒數，給統計圖用 */
    fun getDailyBreakdown(context: Context, platform: String, days: Int): List<DailyPoint> {
        val result = mutableListOf<DailyPoint>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -(days - 1))
        val labelFormat = SimpleDateFormat("M/d", Locale.TAIWAN)
        repeat(days) {
            val key = dateFormat.format(cal.time)
            val label = labelFormat.format(cal.time)
            result.add(DailyPoint(key, label, getDailySeconds(context, platform, key)))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }

    /** 最近 [weeks] 週（含本週，每週從星期一開始）每週的秒數 */
    fun getWeeklyBreakdown(context: Context, platform: String, weeks: Int): List<WeeklyPoint> {
        val result = mutableListOf<WeeklyPoint>()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        cal.add(Calendar.DAY_OF_MONTH, -7 * (weeks - 1))
        val labelFormat = SimpleDateFormat("M/d", Locale.TAIWAN)
        repeat(weeks) {
            val weekStart = cal.clone() as Calendar
            var total = 0L
            for (d in 0 until 7) {
                total += getDailySeconds(context, platform, dateFormat.format(cal.time))
                cal.add(Calendar.DAY_OF_MONTH, 1)
                if (cal.after(Calendar.getInstance())) break
            }
            result.add(WeeklyPoint("${labelFormat.format(weekStart.time)}週", total))
        }
        return result
    }

    /** 匯出最近 [EXPORT_DAYS] 天的 CSV 文字 */
    fun exportCsv(context: Context): String {
        val sb = StringBuilder()
        sb.append("日期,YouTube Shorts(秒),Instagram Reels(秒),TikTok(秒),合計(秒)\n")
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -(EXPORT_DAYS - 1))
        repeat(EXPORT_DAYS) {
            val key = dateFormat.format(cal.time)
            val yt = getDailySeconds(context, "yt", key)
            val ig = getDailySeconds(context, "ig", key)
            val tt = getDailySeconds(context, "tt", key)
            sb.append("$key,$yt,$ig,$tt,${yt + ig + tt}\n")
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return sb.toString()
    }

    /** 清除所有紀錄（目前畫面上沒有按鈕呼叫） */
    fun clearAll(context: Context) {
        prefs(context).edit().remove(KEY_DATA).apply()
    }
}
