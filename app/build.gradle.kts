plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// 정식 서명: 키는 ~/.android-keys/foldstandby-release.jks, 비밀번호는 macOS 키체인
// ("fold-standby-release-keystore"). release 작업을 실행할 때만 읽는다.
// 비밀번호가 설정 캐시에 남지 않도록 release 빌드는 --no-configuration-cache 로 실행한다.
val releaseKeystore = file("${System.getProperty("user.home")}/.android-keys/foldstandby-release.jks")
val wantsRelease = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

fun keychainPassword(): String {
    val out = providers.exec {
        commandLine("security", "find-generic-password", "-a", System.getProperty("user.name"),
            "-s", "fold-standby-release-keystore", "-w")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
    if (out.isEmpty()) throw GradleException("키체인에서 서명 비밀번호를 찾지 못했습니다. scripts/create-release-key.sh 를 먼저 실행하세요.")
    return out
}

android {
    namespace = "com.marksnd.foldstandby"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.marksnd.foldstandby"
        minSdk = 31
        targetSdk = 37
        versionCode = 4
        versionName = "1.1.0-beta.2"
    }

    signingConfigs {
        if (wantsRelease) {
            if (!releaseKeystore.exists()) {
                throw GradleException("서명키가 없습니다: $releaseKeystore (scripts/create-release-key.sh)")
            }
            create("release") {
                storeFile = releaseKeystore
                storePassword = keychainPassword()
                keyAlias = "foldstandby"
                keyPassword = storePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Robolectric는 JDK 17+에서 java.base 내부 API 접근이 필요하다.
                it.jvmArgs(
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                )
            }
        }
    }
}

// Android Lint 도구도 따로 가져오는 라이브러리 중 보안 문제가 알려진 것을 고친 버전으로 올린다(루트 build.gradle.kts와 같은 목록).
// 앱(APK)에는 들어가지 않는다.
configurations.matching { it.name == "androidLintTool" }.configureEach {
    resolutionStrategy.force(
        "org.bouncycastle:bcprov-jdk18on:1.85",
        "org.bouncycastle:bcpkix-jdk18on:1.85",
        "org.bouncycastle:bcutil-jdk18on:1.85",
        "org.apache.commons:commons-lang3:3.18.0",
        "org.apache.httpcomponents:httpclient:4.5.14",
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.window)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
