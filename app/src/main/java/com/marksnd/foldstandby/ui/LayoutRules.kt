package com.marksnd.foldstandby.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo

/*
 * 화면 배치를 고르는 규칙을 모두 여기 둔다. 기기 이름이 아니라 화면 크기·비율·접힌 자세로 고른다.
 * 화면 크기만으로 알 수 없는 기기별 사실(화면 아래 카메라 위치 등)은 data/DeviceProfile.kt에 있다.
 *
 *   폴드8 커버 화면 가로   1972×1248 (약 1.58:1)  달력+시계 40:60
 *   폴드8 펼친 화면 가로   2448×1848 (약 1.32:1)  왼쪽 달력 + 오른쪽 카드 스택, 아래 일정 띠
 *   플립8 펼쳐서 가로      2520×1080 (약 2.33:1)  달력+시계 50:50
 *   반접기(가로 접힘선)    접힘선에서 반으로: 윗면 시계, 아랫면 날짜와 일정
 */

enum class Posture { Flat, Tabletop }

/** 가로 접힘선으로 반쯤 접혀 있으면 반접기(노트북) 자세다. */
fun postureOf(info: WindowLayoutInfo): Posture {
    val fold = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
    return if (fold?.state == FoldingFeature.State.HALF_OPENED && fold.orientation == FoldingFeature.Orientation.HORIZONTAL) {
        Posture.Tabletop
    } else {
        Posture.Flat
    }
}

/** 가장 짧은 변이 이 이상이면 폴드를 펼친 안쪽(큰) 화면으로 본다. */
const val INNER_DISPLAY_MIN_DP = 600

/** 가로 화면이 이보다 덜 길쭉하면(폴드 펼친 화면 4:3) 남는 아래 공간에 일정 띠를 둔다. */
const val AGENDA_STRIP_MAX_RATIO = 1.45f

/** 가로 화면이 이 이상 길쭉하면(플립 21:9) 달력+시계를 반씩 나눈다. 반쪽이 정사각형에 가까워진다. */
const val HALF_SPLIT_MIN_RATIO = 2f

fun isInnerDisplay(smallestScreenWidthDp: Int) = smallestScreenWidthDp >= INNER_DISPLAY_MIN_DP

/** 지금 창이 폴드를 펼친 안쪽 화면인지. */
@Composable
fun innerDisplay(): Boolean = isInnerDisplay(LocalConfiguration.current.smallestScreenWidthDp)

/** 스탠바이 화면 전체를 어떻게 나눌지. */
enum class ScreenLayout {
    /** 반접기: 접힘선에서 반으로 나눠 윗면은 시계, 아랫면은 날짜와 일정. */
    Tabletop,

    /** 보기 위에 아래 일정 띠(보기 5 : 띠 1). */
    WithAgendaStrip,

    /** 보기만 화면 가득. */
    Full,
}

fun screenLayout(width: Dp, height: Dp, posture: Posture): ScreenLayout = when {
    posture == Posture.Tabletop -> ScreenLayout.Tabletop
    width > height && width / height < AGENDA_STRIP_MAX_RATIO -> ScreenLayout.WithAgendaStrip
    else -> ScreenLayout.Full
}

/** 시계 보기 첫 화면. */
enum class ClockPageLayout {
    /** 펼친 안쪽 화면 가로: 왼쪽 달력 + 오른쪽 위아래로 넘기는 카드 스택(사진·시계·날씨·재생 중). */
    UnfoldedStack,

    /** 위아래로 넘기는 시계 스타일. */
    Styles,
}

/** [deskBelow]: 반접기 윗면이라 날짜·일정은 아랫면에 따로 있다. */
fun clockPageLayout(innerDisplay: Boolean, width: Dp, height: Dp, deskBelow: Boolean): ClockPageLayout =
    if (innerDisplay && width > height && !deskBelow) ClockPageLayout.UnfoldedStack else ClockPageLayout.Styles

/** 시계 스타일 수. 반접기 윗면에서는 날짜·일정이 아랫면에 있으므로 시계만(정사각 아날로그, 디지털). */
fun clockStyleCount(deskBelow: Boolean): Int = if (deskBelow) 2 else 3

/** 달력+시계 스타일에서 달력 쪽이 차지하는 몫. 가로면 왼쪽, 세로면 위쪽. */
fun dateClockSplit(innerDisplay: Boolean, width: Dp, height: Dp): Float {
    val landscape = width > height
    return when {
        landscape && (innerDisplay || width / height >= HALF_SPLIT_MIN_RATIO) -> 0.5f
        landscape -> 0.4f
        else -> 0.45f
    }
}

/** 고른 앨범 사진을 읽을지. 사진 카드는 펼친 안쪽 화면에만 있다. */
fun showsAlbumPhotos(innerDisplay: Boolean, photosEnabled: Boolean) = innerDisplay && photosEnabled
