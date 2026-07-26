package com.shortvideo.tracker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 核心計時服務。
 *
 * 運作方式：
 * 1. 監聽 TYPE_WINDOW_STATE_CHANGED，取得目前前景 App 的 packageName。
 * 2. 如果是我們關心的三個 App 之一，進入「追蹤模式」。
 * 3. 用一個每秒執行一次的 Handler tick：
 *    - TikTok：只要前景是 TikTok 就算數（整個 App 幾乎都是短影音）。
 *    - YouTube / Instagram：每次 tick 都重新掃描畫面節點，確認目前是否在
 *      Shorts / Reels 播放介面（用 resource-id 關鍵字比對），符合才計秒。
 * 4. 累積滿 1 秒就呼叫 TrackingData.addSeconds() 寫入。
 *
 * 注意：YouTube / Instagram 的 resource-id 是對方 App 內部命名，
 * 未來對方更新 App 有可能改變，屆時偵測可能需要更新關鍵字。
 */
class ShortVideoAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var currentPackage: String? = null
    private var isTicking = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == currentPackage) return
        currentPackage = pkg

        if (platformOf(pkg) != null) {
            startTicking()
        } else {
            stopTicking()
        }
    }

    private fun startTicking() {
        if (isTicking) return
        isTicking = true
        handler.postDelayed(tickRunnable, TICK_INTERVAL_MS)
    }

    private fun stopTicking() {
        isTicking = false
        handler.removeCallbacks(tickRunnable)
    }

    /** 每秒執行：確認目前狀態是否該計時，如果是就寫入 1 秒 */
    private fun tick() {
        val pkg = currentPackage ?: return
        val platform = platformOf(pkg) ?: return

        val shouldCount = when (platform) {
            "tt" -> true // TikTok 整個 App 都算
            "yt" -> isViewingShortsOrReels(YT_SHORTS_ID_KEYWORDS)
            "ig" -> isViewingShortsOrReels(IG_REELS_ID_KEYWORDS)
            else -> false
        }

        if (shouldCount) {
            TrackingData.addSeconds(this, platform, 1L)
        }
    }

    /** 掃描目前畫面上的節點，看是否存在符合關鍵字的 resource-id */
    private fun isViewingShortsOrReels(keywords: List<String>): Boolean {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return false
        return try {
            containsMatchingId(root, keywords, depth = 0)
        } catch (e: Exception) {
            false
        }
    }

    private fun containsMatchingId(
        node: AccessibilityNodeInfo,
        keywords: List<String>,
        depth: Int
    ): Boolean {
        if (depth > MAX_SCAN_DEPTH) return false
        val id = node.viewIdResourceName
        if (id != null && keywords.any { id.contains(it, ignoreCase = true) }) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = containsMatchingId(child, keywords, depth + 1)
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun platformOf(pkg: String): String? = when (pkg) {
        "com.google.android.youtube" -> "yt"
        "com.instagram.android" -> "ig"
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> "tt"
        else -> null
    }

    override fun onInterrupt() {
        stopTicking()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTicking()
    }

    companion object {
        private const val TICK_INTERVAL_MS = 1000L
        private const val MAX_SCAN_DEPTH = 25

        // YouTube Shorts 播放介面常見 resource-id 關鍵字（可能隨版本更新變動）
        private val YT_SHORTS_ID_KEYWORDS = listOf(
            "reel_player",
            "shorts_player",
            "reel_recycler",
            "shorts_ui"
        )

        // Instagram Reels 播放介面常見 resource-id 關鍵字（可能隨版本更新變動）
        private val IG_REELS_ID_KEYWORDS = listOf(
            "clips_viewer",
            "reels_viewer",
            "clips_tab",
            "reel_viewer_"
        )
    }
}
