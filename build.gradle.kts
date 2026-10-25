// AGP 9 built-in Kotlin: KGP 버전을 Compose 플러그인과 맞추기 위해 classpath에 고정한다.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
}
