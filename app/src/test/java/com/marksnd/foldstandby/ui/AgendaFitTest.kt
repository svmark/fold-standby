package com.marksnd.foldstandby.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AgendaFitTest {
    // 쉬운 셈을 위해 단위 높이: 오늘 제목 10, 일정 18 + 사이 간격 2(일정 n개 = 20n - 2), +N 5, 내일 제목 10, 내일 한 줄 8, 빈 오늘 10
    private val h = AgendaHeights(todayHead = 10f, empty = 10f, event = 18f, eventGap = 2f, more = 5f, tomorrowHead = 10f, line = 8f)

    @Test fun everythingFits() =
        assertEquals(AgendaFit(3, 2, false), fitAgenda(100f, 3, 2, 4, h)) // 10 + 58 + 10 + 16 = 94

    @Test fun todayComesBeforeTomorrowsSecondLine() =
        // 10 + 10 + 8 = 28 잡고 남은 60: 오늘 3개(58), 내일 둘째 줄 자리 없음 → 마지막 줄 끝에 +1
        assertEquals(AgendaFit(3, 1, false), fitAgenda(88f, 3, 2, 4, h))

    @Test fun noGapAfterLastEvent() =
        // 남은 58이면 사이 간격만 세어 오늘 3개가 딱 들어간다
        assertEquals(AgendaFit(3, 1, false), fitAgenda(86f, 3, 2, 4, h))

    @Test fun hiddenTodayLeavesRoomForMoreLine() =
        // 28 잡고 남은 42: 오늘 2개(38)에 +N 줄(5)까지는 안 들어가므로 1개 + "+2 more", 남은 19로 내일 둘째 줄
        assertEquals(AgendaFit(1, 2, false), fitAgenda(70f, 3, 2, 4, h))

    @Test fun tomorrowMoreLineWhenRoomLeft() =
        // 오늘 1개(18) 뒤 남은 54: 내일 둘째 줄(8) 다음 +N 줄(5)
        assertEquals(AgendaFit(1, 2, true), fitAgenda(100f, 1, 4, 4, h))

    @Test fun atLeastOneTodayEvent() =
        assertEquals(AgendaFit(1, 1, false), fitAgenda(30f, 3, 3, 4, h))

    @Test fun noTomorrow() =
        assertEquals(AgendaFit(4, 0, false), fitAgenda(200f, 6, 0, 4, h))

    @Test fun noTodayEvents() =
        // 10 + 10(빈 오늘) + 10 + 8 = 38, 남은 12에 둘째 줄(8), 남은 4라 +N 줄은 안 됨
        assertEquals(AgendaFit(0, 2, false), fitAgenda(50f, 0, 3, 4, h))
}
