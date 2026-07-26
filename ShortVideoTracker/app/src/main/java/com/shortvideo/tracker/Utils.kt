package com.shortvideo.tracker

/** 完整格式，例如「1小時23分45秒」，用在文字顯示 */
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

/** 簡短格式，例如「1h23m」或「45s」，用在圖表座標軸 */
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
