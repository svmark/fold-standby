// AGP 9 built-in Kotlin: KGP 버전을 Compose 플러그인과 맞추기 위해 classpath에 고정한다.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
        // 빌드 도구(AGP 등)가 가져오는 라이브러리 중 보안 문제가 알려진 것을 고친 버전으로 올린다.
        // 앱(APK)에는 들어가지 않고 빌드할 때만 쓰인다. 빌드 도구가 새 버전을 쓰게 되면 지워도 된다.
        constraints {
            classpath("org.bouncycastle:bcprov-jdk18on:1.85") { because("보안: Dependabot 알림 6, 7, 8") }
            classpath("org.bouncycastle:bcpkix-jdk18on:1.85") { because("보안: Dependabot 알림 5") }
            classpath("org.bouncycastle:bcutil-jdk18on:1.85") { because("bcprov·bcpkix와 버전 맞춤") }
            classpath("org.bitbucket.b_c:jose4j:0.9.6") { because("보안: Dependabot 알림 4") }
            classpath("org.jdom:jdom2:2.0.6.1") { because("보안: Dependabot 알림 3") }
            classpath("org.apache.commons:commons-lang3:3.18.0") { because("보안: Dependabot 알림 2") }
            classpath("org.apache.httpcomponents:httpclient:4.5.14") { because("보안: Dependabot 알림 1") }
        }
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
}
