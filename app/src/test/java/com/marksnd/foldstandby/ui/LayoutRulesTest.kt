package com.marksnd.foldstandby.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 기기별 화면 크기(450dpi 기준 dp)로 배치 규칙을 고정해 둔다. 규칙을 바꾸면 여기서 먼저 드러난다. */
class LayoutRulesTest {

    // 폴드8 커버 화면 1972×1248px, 펼친 화면 2448×1848px, 플립8 메인 2520×1080px, 플립8 커버 948×1048px
    private val foldCoverLand = 701.dp to 444.dp
    private val foldCoverPort = 444.dp to 701.dp
    private val foldInnerLand = 870.dp to 657.dp
    private val foldInnerPort = 657.dp to 870.dp
    private val flipLand = 896.dp to 384.dp
    private val flipPort = 384.dp to 896.dp
    private val flipCover = 337.dp to 373.dp

    @Test fun innerDisplayNeedsShortSideOf600dp() {
        assertTrue(isInnerDisplay(657))
        assertTrue(isInnerDisplay(600))
        assertFalse(isInnerDisplay(599))
        assertFalse(isInnerDisplay(444))
        assertFalse(isInnerDisplay(384))
    }

    @Test fun screenLayout() {
        assertEquals(ScreenLayout.Full, screenLayout(foldCoverLand.first, foldCoverLand.second, Posture.Flat))
        assertEquals(ScreenLayout.Full, screenLayout(foldCoverPort.first, foldCoverPort.second, Posture.Flat))
        assertEquals(ScreenLayout.WithAgendaStrip, screenLayout(foldInnerLand.first, foldInnerLand.second, Posture.Flat))
        assertEquals(ScreenLayout.Full, screenLayout(foldInnerPort.first, foldInnerPort.second, Posture.Flat))
        assertEquals(ScreenLayout.Full, screenLayout(flipLand.first, flipLand.second, Posture.Flat))
        assertEquals(ScreenLayout.Full, screenLayout(flipCover.first, flipCover.second, Posture.Flat))
        // 반접기는 화면 모양과 상관없이 위아래 반
        assertEquals(ScreenLayout.Tabletop, screenLayout(flipPort.first, flipPort.second, Posture.Tabletop))
        assertEquals(ScreenLayout.Tabletop, screenLayout(foldInnerPort.first, foldInnerPort.second, Posture.Tabletop))
        assertEquals(ScreenLayout.Tabletop, screenLayout(foldInnerLand.first, foldInnerLand.second, Posture.Tabletop))
    }

    @Test fun agendaStripBoundary() {
        assertEquals(ScreenLayout.WithAgendaStrip, screenLayout(144.dp, 100.dp, Posture.Flat))
        assertEquals(ScreenLayout.Full, screenLayout(145.dp, 100.dp, Posture.Flat))
        // 정사각은 가로가 아니므로 띠 없음
        assertEquals(ScreenLayout.Full, screenLayout(100.dp, 100.dp, Posture.Flat))
    }

    @Test fun clockPageLayout() {
        assertEquals(ClockPageLayout.UnfoldedStack, clockPageLayout(true, foldInnerLand.first, foldInnerLand.second, deskBelow = false))
        assertEquals(ClockPageLayout.Styles, clockPageLayout(true, foldInnerPort.first, foldInnerPort.second, deskBelow = false))
        // 반접기 윗면은 안쪽 화면이어도 시계 스타일
        assertEquals(ClockPageLayout.Styles, clockPageLayout(true, foldInnerLand.first, foldInnerLand.second, deskBelow = true))
        assertEquals(ClockPageLayout.Styles, clockPageLayout(false, foldCoverLand.first, foldCoverLand.second, deskBelow = false))
        assertEquals(ClockPageLayout.Styles, clockPageLayout(false, flipLand.first, flipLand.second, deskBelow = false))
    }

    @Test fun clockStyleCount() {
        assertEquals(3, clockStyleCount(deskBelow = false))
        assertEquals(2, clockStyleCount(deskBelow = true))
    }

    @Test fun dateClockSplit() {
        assertEquals(0.4f, dateClockSplit(false, foldCoverLand.first, foldCoverLand.second))
        assertEquals(0.45f, dateClockSplit(false, foldCoverPort.first, foldCoverPort.second))
        assertEquals(0.5f, dateClockSplit(true, foldInnerLand.first, foldInnerLand.second))
        assertEquals(0.45f, dateClockSplit(true, foldInnerPort.first, foldInnerPort.second))
        assertEquals(0.5f, dateClockSplit(false, flipLand.first, flipLand.second))
        assertEquals(0.45f, dateClockSplit(false, flipCover.first, flipCover.second))
        // 2:1 경계
        assertEquals(0.5f, dateClockSplit(false, 200.dp, 100.dp))
        assertEquals(0.4f, dateClockSplit(false, 199.dp, 100.dp))
    }

    @Test fun albumPhotosOnlyOnInnerDisplay() {
        assertTrue(showsAlbumPhotos(innerDisplay = true, photosEnabled = true))
        assertFalse(showsAlbumPhotos(innerDisplay = true, photosEnabled = false))
        assertFalse(showsAlbumPhotos(innerDisplay = false, photosEnabled = true))
    }
}
