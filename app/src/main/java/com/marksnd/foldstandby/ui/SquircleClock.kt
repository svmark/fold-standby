package com.marksnd.foldstandby.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import java.time.Instant
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val SUPERELLIPSE_N = 5.0

/** 각도 a 방향으로 둥근 사각형(슈퍼타원) 가장자리까지의 거리. */
private fun edge(a: Float, hw: Float, hh: Float): Float {
    val c = abs(cos(a)) / hw
    val s = abs(sin(a)) / hh
    return (c.toDouble().pow(SUPERELLIPSE_N) + s.toDouble().pow(SUPERELLIPSE_N)).pow(-1 / SUPERELLIPSE_N).toFloat()
}

private fun angleOf(fraction: Float) = (fraction * 2 * PI - PI / 2).toFloat()

/**
 * 사진 속 아이폰 스탠바이 시계: 둥근 사각 시계판, 가장자리를 따르는 60개 눈금,
 * 12·3·6·9 숫자, 흰 시·분침, 강조색 스윕 초침.
 * [now]는 그리는 단계에서만 읽어서 매 프레임 다시 구성하지 않는다.
 */
@Composable
fun SquircleClock(
    now: State<Long>,
    modifier: Modifier = Modifier,
    zone: ZoneId? = null,
    showSeconds: Boolean = true,
    smooth: Boolean = true,
    showNumbers: Boolean = true,
) {
    val p = LocalPalette.current
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        // 영역이 길쭉해도 시계판은 가로·세로가 같은 정사각 슈퍼타원으로 그린다.
        val hw = min(size.width, size.height) * 0.46f
        val hh = hw
        val base = hw

        for (i in 0 until 60) {
            val a = angleOf(i / 60f)
            val r = edge(a, hw, hh)
            val major = i % 5 == 0
            val inner = r - base * if (major) 0.13f else 0.06f
            drawLine(
                color = if (major) p.primary else p.tick,
                start = Offset(cx + cos(a) * inner, cy + sin(a) * inner),
                end = Offset(cx + cos(a) * r, cy + sin(a) * r),
                strokeWidth = max(1f, base * if (major) 0.018f else 0.009f),
                cap = StrokeCap.Round,
                alpha = if (major) 1f else 0.55f,
            )
        }

        if (showNumbers) {
            val style = TextStyle(color = p.primary, fontSize = (base * 0.24f).toSp(), fontWeight = FontWeight.SemiBold)
            listOf(12 to 0f, 3 to 0.25f, 6 to 0.5f, 9 to 0.75f).forEach { (n, f) ->
                val a = angleOf(f)
                val r = edge(a, hw, hh) * 0.68f
                val layout = measurer.measure(n.toString(), style)
                drawText(
                    layout,
                    topLeft = Offset(
                        cx + cos(a) * r - layout.size.width / 2f,
                        cy + sin(a) * r - layout.size.height / 2f,
                    ),
                )
            }
        }

        val t = Instant.ofEpochMilli(now.value).atZone(zone ?: ZoneId.systemDefault())
        val sec = t.second + if (smooth) t.nano / 1e9f else 0f
        val min = t.minute + sec / 60f
        val hour = (t.hour % 12) + min / 60f

        hand(cx, cy, angleOf(hour / 12f), base * 0.5f, base * 0.045f, base, p.primary)
        val ma = angleOf(min / 60f)
        hand(cx, cy, ma, edge(ma, hw, hh) * 0.9f, base * 0.04f, base, p.primary)

        if (showSeconds) {
            val sa = angleOf(sec / 60f)
            drawLine(
                color = p.accent,
                start = Offset(cx - cos(sa) * base * 0.12f, cy - sin(sa) * base * 0.12f),
                end = Offset(cx + cos(sa) * edge(sa, hw, hh) * 0.95f, cy + sin(sa) * edge(sa, hw, hh) * 0.95f),
                strokeWidth = max(1f, base * 0.01f),
                cap = StrokeCap.Round,
            )
        }
        drawCircle(p.primary, radius = base * 0.035f, center = Offset(cx, cy))
        drawCircle(p.accent, radius = base * 0.02f, center = Offset(cx, cy))
    }
}

/** 중심 쪽은 가는 연결부, 바깥은 굵은 막대인 시계 바늘. */
private fun DrawScope.hand(
    cx: Float, cy: Float, a: Float, length: Float, width: Float, base: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    val joint = Offset(cx + cos(a) * base * 0.1f, cy + sin(a) * base * 0.1f)
    drawLine(color, Offset(cx, cy), joint, strokeWidth = max(1.5f, width * 0.35f), cap = StrokeCap.Round)
    drawLine(color, joint, Offset(cx + cos(a) * length, cy + sin(a) * length), strokeWidth = width, cap = StrokeCap.Round)
}
