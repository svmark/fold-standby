package com.marksnd.foldstandby.ui

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.max

/*
 * 폴드8 안쪽 화면의 전면 카메라는 화면 아래 카메라(UDC)라서 시스템이 앱에 화면 가림(cutout)으로 알려주지 않는다.
 * 실기기에서 읽은 값: adb shell dumpsys window displays → mDisplayId=0 udcCutout
 *   자연 방향(가로) 2448×1848px 기준 boundingRect = (1823, 18) – (1901, 96), 오른쪽 위.
 */
private const val INNER_W = 2448f
private const val INNER_H = 1848f
private val INNER_CAMERA = Rect(1823, 18, 1901, 96)

/** 카메라와 사진 사이에 둘 여백. */
private val CAMERA_MARGIN = 12.dp

private fun isFold8() = Build.MANUFACTURER.equals("samsung", ignoreCase = true) && Build.MODEL.startsWith("SM-F971")

/** 지금 회전 상태의 창 좌표로 옮긴 안쪽 화면 카메라 위치. 안쪽 화면이 아니면 null. */
private fun innerCameraInWindow(context: Context): Rect? {
    if (!isFold8()) return null
    val wm = context.getSystemService(WindowManager::class.java) ?: return null
    val bounds = wm.currentWindowMetrics.bounds
    val rotation = context.display?.rotation ?: Surface.ROTATION_0
    val sideways = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
    val naturalW = if (sideways) bounds.height() else bounds.width()
    val naturalH = if (sideways) bounds.width() else bounds.height()
    // 커버 화면(1248×1972)이면 안쪽 화면 카메라는 해당 없음. 해상도 설정을 낮췄을 때를 위해 비율로 맞춘다.
    if (naturalW <= naturalH) return null
    val s = naturalW / INNER_W
    if (kotlin.math.abs(naturalH / s - INNER_H) > 40) return null
    val l = INNER_CAMERA.left * s
    val t = INNER_CAMERA.top * s
    val r = INNER_CAMERA.right * s
    val b = INNER_CAMERA.bottom * s
    val w = naturalW.toFloat()
    val h = naturalH.toFloat()
    return when (rotation) {
        Surface.ROTATION_90 -> Rect(t.toInt(), (w - r).toInt(), b.toInt(), (w - l).toInt())
        Surface.ROTATION_180 -> Rect((w - r).toInt(), (h - b).toInt(), (w - l).toInt(), (h - t).toInt())
        Surface.ROTATION_270 -> Rect((h - b).toInt(), l.toInt(), (h - t).toInt(), r.toInt())
        else -> Rect(l.toInt(), t.toInt(), r.toInt(), b.toInt())
    }
}

/**
 * 이 요소가 안쪽 화면 카메라와 겹치면, 카메라가 붙어 있는 화면 가장자리 쪽에 여백을 넣어
 * 카메라 아래(또는 옆)로 비켜 놓는다. 겹치지 않으면 아무것도 하지 않는다.
 */
@Composable
fun Modifier.avoidInnerCamera(): Modifier {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val camera = remember(config) { innerCameraInWindow(context) } ?: return this
    val marginPx = with(density) { CAMERA_MARGIN.toPx() }
    var pad by remember(camera) { mutableStateOf(floatArrayOf(0f, 0f, 0f, 0f)) } // 왼쪽, 위, 오른쪽, 아래
    return this
        .onGloballyPositioned { coords ->
            val box = coords.boundsInWindow()
            val overlapsX = camera.right + marginPx > box.left && camera.left - marginPx < box.right
            val overlapsY = camera.bottom + marginPx > box.top && camera.top - marginPx < box.bottom
            val next = floatArrayOf(0f, 0f, 0f, 0f)
            if (overlapsX && overlapsY) {
                // 카메라가 위·아래 가장자리에 붙어 있으면 세로로, 왼쪽·오른쪽에 붙어 있으면 가로로 비킨다.
                val win = coords.findRootCoordinates().size
                val dTop = camera.top.toFloat()
                val dBottom = win.height - camera.bottom.toFloat()
                val dLeft = camera.left.toFloat()
                val dRight = win.width - camera.right.toFloat()
                when (minOf(dTop, dBottom, dLeft, dRight)) {
                    dTop -> next[1] = max(0f, camera.bottom + marginPx - box.top)
                    dBottom -> next[3] = max(0f, box.bottom - (camera.top - marginPx))
                    dLeft -> next[0] = max(0f, camera.right + marginPx - box.left)
                    else -> next[2] = max(0f, box.right - (camera.left - marginPx))
                }
            }
            if (!next.contentEquals(pad)) pad = next
        }
        .padding(
            start = with(density) { pad[0].toDp() },
            top = with(density) { pad[1].toDp() },
            end = with(density) { pad[2].toDp() },
            bottom = with(density) { pad[3].toDp() },
        )
}
