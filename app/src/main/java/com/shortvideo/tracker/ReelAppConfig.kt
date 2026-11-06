package com.shortvideo.tracker

import android.view.accessibility.AccessibilityEvent

/**
 * 一個 App 的「短影音畫面」辨識規則。
 *
 * @param viewId 短影音畫面一定會出現的元件，格式見 [NodeFinder.findFirst]
 * @param requiresPresent 另外必須同時存在的元件（全部都要找得到才算）
 * @param eventType 觸發檢查的無障礙事件類型（目前沒有用到，保留原設計）
 */
data class ReelAppData(
    val viewId: String,
    val requiresPresent: List<String> = emptyList(),
    val eventType: Int = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
)

/**
 * 各 App 的短影音辨識規則，key 是 App 套件名稱。
 * TikTok 不在這裡：只要 TikTok 在前景就直接算作觀看短影音。
 */
object ReelAppConfig {
    val reelData: Map<String, ReelAppData> = mapOf(
        "com.instagram.android" to ReelAppData(
            viewId = "id:com.instagram.android:id/clips_viewer_view_pager",
            requiresPresent = listOf("id:com.instagram.android:id/clips_ufi_component")
        ),
        "com.google.android.youtube" to ReelAppData(
            viewId = "id:com.google.android.youtube:id/reel_recycler",
            requiresPresent = emptyList()
        )
    )
}
