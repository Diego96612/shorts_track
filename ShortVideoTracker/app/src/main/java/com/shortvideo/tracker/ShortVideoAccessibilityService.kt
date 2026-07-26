package com.shortvideo.tracker

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService

/**
 * 核心計時服務。
 *
 * 判斷邏輯：
 * - TikTok：整個 App 前景就算數，不需要額外檢查畫面元件。
 * - YouTube / Instagram：透過 [ReelAppConfig] + [NodeFinder]，檢查目前畫面上
 *   是否存在 Shorts/Reels 播放器的元件（沿用已驗證可行的偵測規則）。
 *
 * 計時方式（這是這次修正的重點）：
 * 之前的做法是「每秒 tick 一次、每次 +1 秒」，如果系統偶爾延遲執行 tick
 * （常見於背景省電機制），就會漏秒，導致「看了5分鐘只記錄1分鐘」這種誤差。
 *
 * 現在改成「記錄進入畫面的時間點、離開畫面的時間點，直接算兩者的時間差」：
 * 只要每次檢查時知道「現在還在不在看」，中間偵測有沒有delay都不影響最終算出來的秒數，
 * 因為秒數是靠兩個時間戳記相減得到的，不是靠計算 tick 次數。
 */
class ShortVideoAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private var currentPackage: String? = null

    // 目前是否正在計時中的平台（null = 沒有在計時）
    private var activePlatform: String? = null
    // 這次計時 session 的開始時間點（用 elapsedRealtime，不受系統時間調整影響）
    private var sessionStartElapsedMs: Long = 0L

    private val pollRunnable = object : Runnable {
        override fun run() {
            evaluate()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        currentPackage = pkg
        evaluate()
    }

    /** 重新檢查目前狀態，決定要不要開始/結束計時 session */
    private fun evaluate() {
        val pkg = currentPackage
        val platform = pkg?.let { platformOf(it) }

        val shouldCount = when (platform) {
            PLATFORM_TIKTOK -> true
            PLATFORM_YOUTUBE, PLATFORM_INSTAGRAM -> isViewingReel(pkg!!)
            else -> false
        }

        updateSession(if (shouldCount) platform else null)
    }

    /**
     * @param platform 現在應該要計時的平台；null 代表現在不該計時
     */
    private fun updateSession(platform: String?) {
        val now = SystemClock.elapsedRealtime()

        if (platform != null) {
            if (activePlatform != platform) {
                // 平台切換了（或是從沒在計時 -> 開始計時）：先結算舊的，再開始新的
                flushSession(now)
                activePlatform = platform
                sessionStartElapsedMs = now
            }
            // 如果平台沒變，什麼都不用做，繼續累積中
        } else {
            // 現在不該計時了：如果之前有在計時，結算掉
            if (activePlatform != null) {
                flushSession(now)
                activePlatform = null
            }
        }
    }

    /** 把目前這段 session 累積的時間寫入 TrackingData，並清空 session 起點 */
    private fun flushSession(nowElapsedMs: Long) {
        val platform = activePlatform ?: return
        val elapsedSeconds = (nowElapsedMs - sessionStartElapsedMs) / 1000L
        if (elapsedSeconds > 0) {
            TrackingData.addSeconds(applicationContext, platform, elapsedSeconds)
        }
        sessionStartElapsedMs = nowElapsedMs
    }

    /** 檢查 YouTube / Instagram 目前是否在 Shorts / Reels 播放畫面 */
    private fun isViewingReel(pkg: String): Boolean {
        val data = ReelAppConfig.reelData[pkg] ?: return false
        val root = rootInActiveWindow ?: return false

        return try {
            val mainNode = NodeFinder.findFirst(root, data.viewId) ?: return false
            NodeFinder.recycle(mainNode)
            data.requiresPresent.all { NodeFinder.exists(root, it) }
        } catch (e: Exception) {
            false
        }
    }

    private fun platformOf(pkg: String): String? = when (pkg) {
        "com.google.android.youtube" -> PLATFORM_YOUTUBE
        "com.instagram.android" -> PLATFORM_INSTAGRAM
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> PLATFORM_TIKTOK
        else -> null
    }

    override fun onInterrupt() {
        flushSession(SystemClock.elapsedRealtime())
        activePlatform = null
    }

    override fun onDestroy() {
        super.onDestroy()
        flushSession(SystemClock.elapsedRealtime())
        activePlatform = null
        handler.removeCallbacks(pollRunnable)
    }

    companion object {
        // 輪詢間隔：即使系統偶爾delay執行，也不影響計時準確度（見上方說明），
        // 這個數字只影響「畫面切換後最多延遲多久才被偵測到」，不影響最終秒數精準度。
        private const val POLL_INTERVAL_MS = 1000L

        private const val PLATFORM_YOUTUBE = "yt"
        private const val PLATFORM_INSTAGRAM = "ig"
        private const val PLATFORM_TIKTOK = "tt"
    }
}
