plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    // ❌ kotlin.compose 플러그인 없음 (Kotlin 1.9.x에서는 불필요/미지원)
}