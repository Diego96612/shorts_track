package com.shortvideo.tracker

/**
 * YouTube Shorts / Instagram Reels 的畫面偵測規則設定表。
 * key 是 App 的 package name。
 *
 * 這裡的 viewId / requiresPresent 沿用自已驗證過可以正常運作的偵測邏輯，
 * 若未來 Google / Meta 更新 App 版本改了內部元件命名，才需要更新這裡的字串。
 *
 * TikTok 不需要放進這張表——TikTok 整個 App 幾乎都是短影音，
 * 用套件名稱比對就夠了，不需要額外判斷畫面元件。
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
