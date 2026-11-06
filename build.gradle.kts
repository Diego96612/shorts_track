// 專案層級設定：只宣告 plugin 版本，實際套用在 app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
