package com.marksnd.foldstandby.data

import android.os.Build

/*
 * 화면 크기·비율로는 알 수 없는 기기별 사실을 모아 둔다. 화면 배치 규칙은 ui/LayoutRules.kt에 있다.
 * 새 기기는 보통 [KNOWN]에 한 줄을 더하면 된다.
 */

/**
 * 시스템이 화면 가림(cutout)으로 알려주지 않는 화면 아래 카메라(UDC).
 * 자연 방향 해상도 [displayWidth]×[displayHeight] 기준 픽셀 위치.
 */
data class HiddenCamera(
    val displayWidth: Int,
    val displayHeight: Int,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

data class DeviceProfile(
    val name: String,
    /** 펼친 안쪽 화면의 화면 아래 카메라. 없으면 null. */
    val innerCamera: HiddenCamera? = null,
)

object DeviceProfiles {
    val GENERIC = DeviceProfile("일반 안드로이드")

    /** 삼성 모델 번호 앞부분 → 기기 정보. 같은 모델의 지역별 번호(SM-F971N 등)를 함께 잡는다. */
    private val KNOWN = listOf(
        // 실기기에서 읽은 값: adb shell dumpsys window displays → mDisplayId=0 udcCutout
        "SM-F971" to DeviceProfile(
            name = "Galaxy Z Fold8 와이드",
            innerCamera = HiddenCamera(displayWidth = 2448, displayHeight = 1848, left = 1823, top = 18, right = 1901, bottom = 96),
        ),
    )

    fun forDevice(manufacturer: String, model: String): DeviceProfile {
        if (!manufacturer.equals("samsung", ignoreCase = true)) return GENERIC
        return KNOWN.firstOrNull { (prefix, _) -> model.startsWith(prefix) }?.second ?: GENERIC
    }

    /** 이 폰의 기기 정보. */
    val current: DeviceProfile by lazy { forDevice(Build.MANUFACTURER, Build.MODEL) }
}
