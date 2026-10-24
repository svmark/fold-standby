package com.marksnd.foldstandby.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * 현재 시각(밀리초). smooth면 매 프레임, 아니면 매초 갱신한다.
 * 그리는 단계(Canvas)에서만 읽으면 다시 구성하지 않고 다시 그리기만 한다.
 */
@Composable
fun rememberNow(smooth: Boolean): State<Long> = produceState(System.currentTimeMillis(), smooth) {
    while (true) {
        if (smooth) {
            withFrameMillis { value = System.currentTimeMillis() }
        } else {
            value = System.currentTimeMillis()
            delay(1000 - value % 1000)
        }
    }
}

/** 오늘 날짜. 1분마다 확인해 자정과 시간대 변경을 따라간다. */
@Composable
fun rememberToday(): State<LocalDate> = produceState(LocalDate.now()) {
    while (true) {
        delay(60_000 - System.currentTimeMillis() % 60_000)
        value = LocalDate.now()
    }
}

private val SHIFTS = listOf(0 to 0, 1 to 0, 1 to 1, 0 to 1, -1 to 1, -1 to 0, -1 to -1, 0 to -1, 1 to -1)

/** 번인 방지: 1분마다 화면 전체를 몇 픽셀씩 옮긴다. */
@Composable
fun rememberPixelShift(stepPx: Int): State<IntOffset> = produceState(IntOffset.Zero, stepPx) {
    var i = 0
    while (true) {
        delay(60_000)
        i = (i + 1) % SHIFTS.size
        val (x, y) = SHIFTS[i]
        value = IntOffset(x * stepPx, y * stepPx)
    }
}
