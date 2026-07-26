package com.shortvideo.tracker

import android.view.accessibility.AccessibilityEvent

/**
 * 描述「某個 App 要如何判斷目前是否在看 Shorts/Reels」的規則。
 *
 * @param viewId 主要判斷依據，例如 Shorts/Reels 播放器容器的 id。
 *               支援三種格式："id:完整resource-id"、"desc:內容描述"、"path:Class[idx]>Class[idx]..."
 * @param requiresPresent 除了 viewId 找得到之外，畫面上還必須同時存在的其他元件（用來避免誤判）
 * @param eventType 這個 App 主要靠哪種 AccessibilityEvent 類型來觸發重新檢查（目前實作用輪詢，這個欄位保留供未來擴充）
 */
data class ReelAppData(
    val viewId: String,
    val requiresPresent: List<String> = emptyList(),
    val eventType: Int = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
)
