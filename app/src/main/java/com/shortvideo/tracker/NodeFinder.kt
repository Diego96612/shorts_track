package com.shortvideo.tracker

import android.view.accessibility.AccessibilityNodeInfo

/**
 * 在無障礙節點樹裡找元件。支援三種寫法：
 * - `id:套件名稱:id/元件ID`：用 view ID 找
 * - `desc:文字`：找 contentDescription 完全相同的元件
 * - `path:類別[索引]>類別[索引]>...`：照階層路徑一層一層往下找
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

    fun exists(root: AccessibilityNodeInfo, selector: String): Boolean {
        val node = findFirst(root, selector)
        val found = node != null
        recycle(node)
        return found
    }

    @Suppress("DEPRECATION")
    fun recycle(node: AccessibilityNodeInfo?) {
        if (node == null) return
        try {
            node.recycle()
        } catch (e: Exception) {
            // 已回收過就忽略
        }
    }

    private fun findById(root: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        val nodes = root.findAccessibilityNodeInfosByViewId(id)
        if (nodes.isEmpty()) return null
        val first = nodes[0]
        for (i in 1 until nodes.size) recycle(nodes[i])
        return first
    }

    private fun findByDesc(root: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? =
        searchByDesc(root, desc, 0)

    private fun searchByDesc(node: AccessibilityNodeInfo, desc: String, depth: Int): AccessibilityNodeInfo? {
        if (depth > MAX_DEPTH) return null
        val nodeDesc = node.contentDescription?.toString()
        if (nodeDesc != null && nodeDesc == desc) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = searchByDesc(child, desc, depth + 1)
            if (result != null) return result
            recycle(child)
        }
        return null
    }

    private fun findByPath(root: AccessibilityNodeInfo, path: String): AccessibilityNodeInfo? {
        val segments = path.split(">")
        val segmentRegex = Regex("^(.+)\\[(\\d+)]$")
        var current = root
        for (segment in segments) {
            val match = segmentRegex.find(segment.trim()) ?: return null
            val className = match.groupValues[1]
            val index = match.groupValues[2].toIntOrNull() ?: return null
            current = findChildByClassIndex(current, className, index) ?: return null
        }
        return current
    }

    private fun findChildByClassIndex(
        parent: AccessibilityNodeInfo,
        className: String,
        index: Int
    ): AccessibilityNodeInfo? {
        var count = 0
        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i) ?: continue
            if (child.className?.toString() == className) {
                if (count == index) return child
                count++
            }
        }
        return null
    }
}
