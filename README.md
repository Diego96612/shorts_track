# shorts_track（短影音使用時間）

一個 Android App，用「無障礙服務」偵測你是不是正在看 YouTube Shorts、Instagram Reels、TikTok，並統計每天、每週的觀看時間。資料只存在手機本機，不會上傳。

## 功能

### 總覽分頁
- YouTube Shorts、Instagram Reels、TikTok 各一張卡片，用橫條顯示「今天」和「本週」的時間
- 三個平台今天、本週的合計
- 開啟無障礙設定的按鈕
- 重新整理數據
- 匯出最近 50 天的資料成 CSV 檔，可以自己選存放位置，檔名是 `短影音使用時間_日期.csv`

### 統計分頁
- 切換「每日」或「每週」
- 每日：近 7 天，三個平台並排的長條圖
- 每週：近 6 週（每週從星期一算起）的長條圖

### 背景計時
- 切換視窗時判斷一次，另外每 1 秒檢查一次
- YouTube：畫面上有 Shorts 專用的 `reel_recycler` 元件才算
- Instagram：畫面上有 Reels 的 `clips_viewer_view_pager` 和 `clips_ufi_component` 才算
- TikTok（國際版、台灣版）：只要 App 在前景就算
- 離開短影音畫面時，把這段秒數存起來

## 編譯成 APK

需要 Android Studio（2026.1 以上）。

1. Clone 這個 repo，用 Android Studio 開啟 repo 根目錄
2. 等 Gradle Sync 完成（第一次會下載套件）
3. 選單 **Build → Generate App Bundles or APKs → Generate APKs**
4. 產出的檔案在 `app/build/outputs/apk/debug/app-debug.apk`

也可以用命令列：

```bash
./gradlew assembleDebug
```

Windows 用 `gradlew.bat assembleDebug`。

> `gradle.properties` 裡有 `android.overridePathCheck=true`，是為了讓專案放在含中文的路徑下也能在 Windows 編譯，編譯時會出現一行「experimental」警告，可以忽略。

## 安裝後第一次使用

1. 打開 App，按「開啟無障礙服務（第一次必做）」
2. 在系統設定的無障礙清單裡找到「短影音使用時間」，把它開啟
3. 之後正常滑短影音，回到 App 按「重新整理數據」就會看到時間

## 專案結構

```
app/src/main/
├─ AndroidManifest.xml
├─ java/com/shortvideo/tracker/
│  ├─ MainActivity.kt                    畫面：總覽、統計、底部分頁
│  ├─ StatsChart.kt                      橫條、長條圖、圖例、平台顏色
│  ├─ ShortVideoAccessibilityService.kt  無障礙服務，負責偵測與計時
│  ├─ TrackingData.kt                    資料讀寫、每日/每週統計、CSV 匯出
│  ├─ ReelAppConfig.kt                   各 App 的短影音畫面辨識規則
│  ├─ NodeFinder.kt                      在畫面元件樹裡找元件
│  ├─ Utils.kt                           時間格式化
│  └─ ui/theme/                          Material 3 主題
└─ res/
   ├─ xml/accessibility_service_config.xml
   └─ values/strings.xml、themes.xml
```

## 版本

| 項目 | 版本 |
|---|---|
| Android Gradle Plugin | 9.3.1 |
| Gradle | 9.5.0 |
| Kotlin Compose 編譯外掛 | 2.2.20 |
| Compose BOM | 2026.06.01 |
| compileSdk / minSdk / targetSdk | 36 / 26（Android 8.0）/ 34 |

## 已知限制

- YouTube、Instagram 改版後，元件 ID 可能會變，到時候要更新 `ReelAppConfig.kt`
- TikTok 是「在前景就算」，所以在 TikTok 看私訊等非影音頁面也會被計時
- `PACKAGE_USAGE_STATS` 權限有宣告但程式沒用到
- 還沒有單元測試，也還沒在實機上完整測試

## 版本紀錄

- 2026-11-06：原本 repo 只有三個無法編譯的程式片段，改成可以直接編譯的完整 Android Studio 專案（從已編譯的 APK 重建）。舊檔案仍可在 git 歷史中找到。
