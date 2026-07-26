package com.shortvideo.tracker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 負責所有「使用時間資料」的讀寫。
 *
 * 儲存結構（存在 SharedPreferences 裡的一個 JSON 字串）：
 * {
 *   "2026-07-25": { "yt": 120, "ig": 300, "tt": 45 },
 *   "2026-07-24": { "yt": 0,   "ig": 60,  "tt": 900 },
 *   ...
 * }
 *
 * 每一筆數字都是「秒數」。
 * 平台代碼固定為："yt" (YouTube Shorts) / "ig" (Instagram Reels) / "tt" (TikTok)
 */
object TrackingData {

    private const val PREFS_NAME = "short_video_tracker_prefs"
    private const val KEY_DATA = "usage_data_json"

    val PLATFORMS = listOf("yt", "ig", "tt")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun todayKey(): String = dateFormat.format(Date())

    private fun readRoot(context: Context): JSONObject {
        val raw = prefs(context).getString(KEY_DATA, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun writeRoot(context: Context, root: JSONObject) {
        prefs(context).edit().putString(KEY_DATA, root.toString()).apply()
    }

    /**
     * 累加指定平台「今天」的使用秒數。
     * 這個是給 AccessibilityService 呼叫的核心寫入函式。
     * 用「累加」而不是「覆寫」，就算 App 中途被殺掉重開也不會把之前的資料蓋掉。
     */
    @Synchronized
    fun addSeconds(context: Context, platform: String, seconds: Long) {
        if (seconds <= 0) return
        val root = readRoot(context)
        val key = todayKey()
        val dayObj = root.optJSONObject(key) ?: JSONObject().also { root.put(key, it) }
        val current = dayObj.optLong(platform, 0L)
        dayObj.put(platform, current + seconds)
        writeRoot(context, root)
    }

    /** 取得某一天、某平台的秒數 */
    fun getDailySeconds(context: Context, platform: String, dateKey: String): Long {
        val root = readRoot(context)
        val dayObj = root.optJSONObject(dateKey) ?: return 0L
        return dayObj.optLong(platform, 0L)
    }

    /** 今天的秒數 */
    fun getTodayTotal(context: Context, platform: String): Long =
        getDailySeconds(context, platform, todayKey())

    /**
     * 本週秒數：從本週一到今天（包含今天）。
     * 週的定義用 Calendar.MONDAY 當一週的開始，符合台灣一般認知。
     */
    fun getWeeklyTotal(context: Context, platform: String): Long {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        // 移到本週一
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        var total = 0L
        val today = Calendar.getInstance()
        while (!cal.after(today)) {
            val key = dateFormat.format(cal.time)
            total += getDailySeconds(context, platform, key)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return total
    }

    data class DailyPoint(val dateKey: String, val label: String, val seconds: Long)

    /**
     * 取得某平台最近 N 天（含今天）的每日秒數，由舊到新排序。
     * 給折線圖/長條圖使用。
     */
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

    data class WeeklyPoint(val weekLabel: String, val seconds: Long)

    /**
     * 取得某平台最近 N 週（含本週）的每週秒數總和，由舊到新排序。
     */
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
            for (i in 0 until 7) {
                val key = dateFormat.format(cal.time)
                total += getDailySeconds(context, platform, key)
                cal.add(Calendar.DAY_OF_MONTH, 1)
                if (cal.after(Calendar.getInstance())) break
            }
            result.add(WeeklyPoint("${labelFormat.format(weekStart.time)}週", total))
        }
        return result
    }

    /** 匯出最近 30 天的 CSV（日期, YouTube Shorts, Instagram Reels, TikTok, 合計），單位：秒 */
    fun exportCsv(context: Context): String {
        val sb = StringBuilder()
        sb.append("日期,YouTube Shorts(秒),Instagram Reels(秒),TikTok(秒),合計(秒)\n")
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -29)
        repeat(30) {
            val key = dateFormat.format(cal.time)
            val yt = getDailySeconds(context, "yt", key)
            val ig = getDailySeconds(context, "ig", key)
            val tt = getDailySeconds(context, "tt", key)
            sb.append("$key,$yt,$ig,$tt,${yt + ig + tt}\n")
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return sb.toString()
    }

    /** debug 用：清空所有資料 */
    fun clearAll(context: Context) {
        prefs(context).edit().remove(KEY_DATA).apply()
    }
}
