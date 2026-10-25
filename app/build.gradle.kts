plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
        versionCode = 2
        versionName = "1.0.1"
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

    testImplementation(libs.junit)
}
