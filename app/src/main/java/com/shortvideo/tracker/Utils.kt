package com.shortvideo.tracker

/** 秒數轉成「X小時X分」或「X分X秒」，滿一小時就不顯示秒 */
fun formatTime(seconds: Long): String {
    if (seconds <= 0) return "0 秒"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return buildString {
        if (h > 0) append("${h}小時")
        if (m > 0) append("${m}分")
        if (h == 0L) append("${s}秒")
    }
}

/** 圖表用的短格式：1h5m、12m、30s */
fun formatTimeShort(seconds: Long): String {
    if (seconds <= 0) return "0"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 -> "${h}h${m}m"
        m > 0 -> "${m}m"
        else -> "${seconds}s"
    }
}
