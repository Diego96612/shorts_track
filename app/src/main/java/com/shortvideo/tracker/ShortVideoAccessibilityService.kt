package com.shortvideo.tracker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * 無障礙服務：判斷使用者現在是不是在看短影音，並把觀看秒數記進 [TrackingData]。
 *
 * - 每次切換視窗會觸發一次判斷，另外每 1 秒輪詢一次（滑下一支影片不一定會觸發事件）
 * - YouTube / Instagram：畫面上找得到 Shorts / Reels 專用元件才算
 * - TikTok：只要 TikTok 在前景就算
 */
class ShortVideoAccessibilityService : AccessibilityService() {

    companion object {
        private const val PLATFORM_YOUTUBE = "yt"
        private const val PLATFORM_INSTAGRAM = "ig"
        private const val PLATFORM_TIKTOK = "tt"
        private const val POLL_INTERVAL_MS = 1000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var currentPackage: String? = null
    private var activePlatform: String? = null
    private var sessionStartElapsedMs = 0L

    private val pollRunnable = object : Runnable {
        override fun run() {
            evaluate()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 注意：這裡沒有設定 packageNames，所以會覆蓋 XML 設定，改成接收所有 App 的視窗事件，
        // 這樣離開短影音 App（例如回到桌面）時也收得到事件，計時才會停。
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

    private fun evaluate() {
        val pkg = currentPackage
        val platform = pkg?.let { platformOf(it) }
        val watching = when (platform) {
            null -> false
            PLATFORM_TIKTOK -> true
            else -> isViewingReel(pkg)
        }
        updateSession(if (watching) platform else null)
    }

    private fun platformOf(pkg: String): String? = when (pkg) {
        "com.google.android.youtube" -> PLATFORM_YOUTUBE
        "com.instagram.android" -> PLATFORM_INSTAGRAM
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> PLATFORM_TIKTOK
        else -> null
    }

    private fun isViewingReel(pkg: String): Boolean {
        val data = ReelAppConfig.reelData[pkg] ?: return false
        val root = rootInActiveWindow ?: return false
        return try {
            val node = NodeFinder.findFirst(root, data.viewId) ?: return false
            NodeFinder.recycle(node)
            data.requiresPresent.all { NodeFinder.exists(root, it) }
        } catch (e: Exception) {
            false
        }
    }

    /** 平台有變化時，把上一段的秒數存起來，再開始新的一段 */
    private fun updateSession(platform: String?) {
        val now = SystemClock.elapsedRealtime()
        if (platform == null) {
            if (activePlatform != null) {
                flushSession(now)
                activePlatform = null
            }
        } else if (activePlatform != platform) {
            flushSession(now)
            activePlatform = platform
            sessionStartElapsedMs = now
        }
    }

    private fun flushSession(now: Long) {
        val platform = activePlatform ?: return
        val seconds = (now - sessionStartElapsedMs) / 1000
        if (seconds > 0) {
            TrackingData.addSeconds(applicationContext, platform, seconds)
        }
        sessionStartElapsedMs = now
    }
}
