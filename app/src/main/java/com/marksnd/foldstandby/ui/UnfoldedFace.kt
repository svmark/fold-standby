package com.marksnd.foldstandby.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.marksnd.foldstandby.data.MediaState
import com.marksnd.foldstandby.data.SettingsStore
import com.marksnd.foldstandby.data.WeatherState
import java.time.LocalDate

/** 펼친 화면 오른쪽 스택에 들어가는 카드. */
enum class StackCard { Photos, Clock, Weather, Music }

/**
 * 펼친 안쪽 화면(가로): 왼쪽 반은 날짜·달력으로 고정, 오른쪽 반은 아이폰 위젯 스택처럼
 * 위아래로 밀어 사진 · 시계 · 날씨 · 재생 중 카드를 넘긴다. 마지막 카드를 기억하고,
 * 음악 재생이 시작되면 재생 중 카드로 알아서 넘어간다.
 */
@Composable
fun UnfoldedFace(
    store: SettingsStore,
    now: State<Long>,
    tick: State<Long>,
    today: LocalDate,
    night: Boolean,
    photos: List<Uri>,
    weather: WeatherState,
    media: MediaState,
) {
    val cards = remember(photos.isNotEmpty()) {
        if (photos.isNotEmpty()) StackCard.entries else StackCard.entries - StackCard.Photos
    }
    val saved = remember { store.read().stackCard }
    val pager = rememberPagerState(initialPage = cards.indexOf(saved).coerceAtLeast(0)) { cards.size }

    // 사진 목록이 늦게 읽혀 카드 순서가 바뀌어도 보던 카드를 유지한다.
    LaunchedEffect(cards) {
        val i = cards.indexOf(store.read().stackCard)
        if (i >= 0 && i != pager.currentPage) pager.scrollToPage(i)
    }
    LaunchedEffect(pager.settledPage, cards) {
        cards.getOrNull(pager.settledPage)?.let { card -> store.update { putString(SettingsStore.STACK_CARD, card.name) } }
    }
    val playing = (media as? MediaState.Playing)?.now?.playing == true
    LaunchedEffect(playing) {
        if (playing) pager.animateScrollToPage(cards.indexOf(StackCard.Music))
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pad = minOf(maxWidth, maxHeight) * 0.04f
        Split(
            landscape = true,
            firstWeight = 0.5f,
            first = { DatePanel(today, Modifier.fillMaxSize()) },
            second = {
                VerticalPager(pager, Modifier.fillMaxSize()) { page ->
                    when (cards[page]) {
                        StackCard.Photos -> PhotoFrame(photos, Modifier.fillMaxSize().padding(pad))
                        StackCard.Clock -> SquircleClock(
                            now = now,
                            modifier = Modifier.fillMaxSize().padding(pad),
                            showSeconds = !night,
                            smooth = !night,
                        )
                        StackCard.Weather -> StackCardBox(pad) { WeatherPage(weather) }
                        StackCard.Music -> StackCardBox(pad) { NowPlayingPage(media, tick) }
                    }
                }
                StyleDots(cards.size, pager.currentPage, Modifier.align(Alignment.CenterEnd))
            },
        )
    }
}

/** 날씨·음악처럼 배경이 없는 카드에 둥근 카드 바탕을 깐다. 카메라와 겹치면 아래로 비킨다. */
@Composable
private fun StackCardBox(pad: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(pad).avoidInnerCamera()) {
        val radius = minOf(maxWidth, maxHeight) * 0.06f
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(radius)).background(p.card)) { content() }
    }
}
