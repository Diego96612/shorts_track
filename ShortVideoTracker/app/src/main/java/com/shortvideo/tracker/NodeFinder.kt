package com.shortvideo.tracker

import android.view.accessibility.AccessibilityNodeInfo

/**
 * 在無障礙節點樹裡尋找符合條件的節點。
 *
 * 支援三種選擇器語法：
 * - "id:完整resource-id"，例如 "id:com.instagram.android:id/clips_viewer_view_pager"
 *   → 用 AccessibilityNodeInfo.findAccessibilityNodeInfosByViewId 比對
 * - "desc:內容描述文字"，例如 "desc:Spotlight"
 *   → 遞迴搜尋 contentDescription 完全相符的節點
 * - "path:Class[idx]>Class[idx]>..."，例如 "path:android.widget.FrameLayout[0]>android.widget.TextView[1]"
 *   → 從根節點開始，依序找「第 idx 個是這個類別的子節點」
 */
object NodeFinder {

    private const val MAX_DEPTH = 60

    fun findFirst(root: AccessibilityNodeInfo, selector: String): AccessibilityNodeInfo? {
        return try {
            when {
                selector.startsWith("id:") -> findById(root, selector.removePrefix("id:"))
                selector.startsWith("desc:") -> findByDesc(root, selector.removePrefix("desc:"))
                selector.startsWith("path:") -> findByPath(root, selector.removePrefix("path:"))
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 是否存在符合條件的節點（用完即丟，不保留參照） */
    fun exists(root: AccessibilityNodeInfo, selector: String): Boolean {
        val node = findFirst(root, selector)
        val found = node != null
        recycle(node)
        return found
    }

    fun recycle(node: AccessibilityNodeInfo?) {
        try {
            @Suppress("DEPRECATION")
            node?.recycle()
        } catch (e: Exception) {
            // 新版 Android 已經不需要手動 recycle，呼叫失敗直接忽略
        }
    }

    private fun findById(root: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        val list = root.findAccessibilityNodeInfosByViewId(id)
        if (list.isEmpty()) return null
        val first = list[0]
        for (i in 1 until list.size) recycle(list[i])
        return first
    }

    private fun findByDesc(root: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? =
        searchByDesc(root, desc, 0)

    private fun searchByDesc(node: AccessibilityNodeInfo, desc: String, depth: Int): AccessibilityNodeInfo? {
        if (depth > MAX_DEPTH) return null
        val cd = node.contentDescription?.toString()
        if (cd != null && cd == desc) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = searchByDesc(child, desc, depth + 1)
            if (found != null) return found
            recycle(child)
        }
        return null
    }

    private fun findByPath(root: AccessibilityNodeInfo, path: String): AccessibilityNodeInfo? {
        val segments = path.split(">")
        var current: AccessibilityNodeInfo = root
        val pattern = Regex("""^(.+)\[(\d+)]$""")

        for (segment in segments) {
            val match = pattern.find(segment.trim()) ?: return null
            val className = match.groupValues[1]
            val targetIndex = match.groupValues[2].toIntOrNull() ?: return null
            val next = findChildByClassIndex(current, className, targetIndex) ?: return null
            current = next
        }
        return current
    }

    private fun findChildByClassIndex(
        node: AccessibilityNodeInfo,
        className: String,
        targetIndex: Int
    ): AccessibilityNodeInfo? {
        var matchCount = 0
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (child.className?.toString() == className) {
                if (matchCount == targetIndex) return child
                matchCount++
            }
        }
        return null
    }
}
