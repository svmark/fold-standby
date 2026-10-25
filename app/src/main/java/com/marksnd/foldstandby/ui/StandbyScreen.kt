package com.marksnd.foldstandby.ui

import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.marksnd.foldstandby.data.AgendaState
import com.marksnd.foldstandby.data.NightMode
import com.marksnd.foldstandby.data.MediaState
import com.marksnd.foldstandby.data.Settings
import com.marksnd.foldstandby.data.WeatherState
import com.marksnd.foldstandby.data.SettingsStore
import com.marksnd.foldstandby.data.rememberAlbumPhotos
import com.marksnd.foldstandby.data.rememberAgenda
import com.marksnd.foldstandby.data.glowLevel
import com.marksnd.foldstandby.data.rememberIsDark
import com.marksnd.foldstandby.data.rememberLastMotion
import com.marksnd.foldstandby.data.rememberNextAlarm
import com.marksnd.foldstandby.data.rememberNow
import com.marksnd.foldstandby.data.rememberNowPlaying
import com.marksnd.foldstandby.data.rememberPixelShift
import com.marksnd.foldstandby.data.rememberSettings
import com.marksnd.foldstandby.data.rememberSettingsStore
import com.marksnd.foldstandby.data.rememberToday
import com.marksnd.foldstandby.data.rememberWeather
import com.marksnd.foldstandby.service.NotificationBus
import kotlin.math.roundToInt

/** 화면 밝기를 0~1로 정한다. null이면 시스템 밝기를 따른다. */
fun applyBrightness(window: Window, level: Float?) {
    window.attributes = window.attributes.apply {
        screenBrightness = level ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }
}

/** 이만큼 움직이지 않고 누르고 있으면 설정을 연다. */
private const val LONG_PRESS_MS = 700L

/** 밤에 이만큼 움직임도 터치도 없으면 화면을 거의 끈다. */
private const val SLEEP_AFTER_MS = 60_000L

/** 알람 전 빛의 색. 해 뜨기 전 하늘 같은 호박색. */
private val Amber = Color(0xFFFFA94D)

private const val PAGE_COUNT = 3

/**
 * 스탠바이 화면 전체. Activity와 화면 보호기가 같이 쓴다.
 * 좌우로 밀면 시계 / 날씨 / 재생 중, 시계 보기에서 위아래로 밀면 시계 스타일이 바뀐다.
 */
@Composable
fun StandbyScreen(
    posture: Posture,
    onOpenSettings: () -> Unit,
    onBrightness: (Float?) -> Unit,
    openCount: Int = 0,
) {
    val store = rememberSettingsStore()
    val settings by rememberSettings(store)
    val dark by rememberIsDark()
    val tick by rememberNow(smooth = false)

    // 알람 전 은은한 빛: 빛이 켜지면 야간(빨강)보다 우선한다.
    val nextAlarm by rememberNextAlarm()
    val ringing by NotificationBus.ringingAlarm.collectAsState()
    val glow = if (settings.alarmGlow) glowLevel(tick, nextAlarm, ringing != null) else null

    val night = glow == null && when (settings.nightMode) {
        NightMode.Auto -> dark
        NightMode.On -> true
        NightMode.Off -> false
    }

    // 밤에 가만히 두면 거의 끄고, 움직이거나 만지면 다시 켠다.
    val lastMotion by rememberLastMotion(enabled = settings.wakeOnMotion && night)
    var lastTouch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val nightSince = remember(night) { System.currentTimeMillis() }
    val asleep = settings.wakeOnMotion && night && tick - maxOf(lastMotion, lastTouch, nightSince) > SLEEP_AFTER_MS
    val asleepNow by rememberUpdatedState(asleep)
    val openSettings by rememberUpdatedState(onOpenSettings)
    val haptic = LocalHapticFeedback.current

    val brightness = when {
        glow != null -> 0.05f + 0.95f * (glow * 20).roundToInt() / 20f // 5% 단위로만 바꾼다
        asleep -> 0f
        night -> 0.01f
        else -> null
    }
    LaunchedEffect(brightness) { onBrightness(brightness) }
    val sleepShade by animateFloatAsState(if (asleep) 0.93f else 0f, tween(if (asleep) 2000 else 300), label = "sleep")

    val palette = if (night) NightPalette else dayPalette(Accents[settings.accent.coerceIn(Accents.indices)])
    val shift by rememberPixelShift(with(LocalDensity.current) { 3.dp.roundToPx() })

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalKorean provides settings.korean,
        LocalMondayFirst provides settings.mondayFirst,
    ) {
        val agenda by rememberAgenda(openCount)
        val today by rememberToday()
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            lastTouch = System.currentTimeMillis()
                            // 자는 화면을 깨우는 터치는 넘김·버튼으로 전달하지 않는다.
                            if (asleepNow) event.changes.forEach { it.consume() }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // 화면을 길게 누르면(움직이지 않고) 설정 화면을 연다. 자는 화면을 깨우는 터치는 제외.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        if (asleepNow) return@awaitEachGesture
                        val released = withTimeoutOrNull(LONG_PRESS_MS) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed || event.changes.size > 1 ||
                                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                                ) break
                            }
                        }
                        if (released == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            openSettings()
                            // 손을 뗄 때까지의 움직임은 넘김·버튼으로 보내지 않는다.
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                },
        ) {
            val layout = screenLayout(maxWidth, maxHeight, posture)
            if (layout == ScreenLayout.Tabletop) {
                // 반쯤 접어 세우면 접힘선에서 정확히 반으로 나눈다(화면 가림 여백은 윗면에만).
                // 윗면은 시계, 아랫면은 날짜와 오늘·내일 일정.
                Column(Modifier.fillMaxSize().offset { shift }) {
                    Box(Modifier.fillMaxWidth().weight(1f).windowInsetsPadding(WindowInsets.displayCutout)) {
                        StandbyPages(store, settings, night, openCount, agenda, nextAlarm, deskBelow = true)
                    }
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        CalendarFace(today, agenda, showTime = false)
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .offset { shift },
                ) {
                    val strip = layout == ScreenLayout.WithAgendaStrip
                    Box(Modifier.fillMaxWidth().weight(if (strip) 5f else 1f)) {
                        StandbyPages(store, settings, night, openCount, agenda, nextAlarm, deskBelow = false)
                    }
                    if (strip) {
                        AgendaStrip(agenda, Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
            NotificationBanner(maxWidth, Modifier.align(Alignment.TopCenter))
            if (glow != null) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Amber.copy(alpha = 0.08f + 0.22f * glow), Amber.copy(alpha = 0.2f + 0.45f * glow)),
                        ),
                    ),
                )
            }
            if (sleepShade > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = sleepShade)))
            }
        }
    }
}

@Composable
private fun StandbyPages(
    store: SettingsStore,
    settings: Settings,
    night: Boolean,
    openCount: Int,
    agenda: AgendaState,
    nextAlarm: Long?,
    deskBelow: Boolean,
) {
    val korean = LocalKorean.current
    // 열 때마다 시계 보기의 달력+시계로 시작한다(마지막에 본 보기를 기억하지 않는다).
    val pager = rememberPagerState { PAGE_COUNT }
    LaunchedEffect(openCount) { if (pager.currentPage != 0) pager.scrollToPage(0) }

    // 데이터는 보기를 넘겨도 다시 받지 않도록 여기서 한 번만 구독한다.
    val smoothNow = rememberNow(smooth = !night)
    val tick = rememberNow(smooth = false)
    val today by rememberToday()
    val weather by rememberWeather(korean)
    val media by rememberNowPlaying()
    val inner = innerDisplay()
    val photos by rememberAlbumPhotos(settings.photoAlbumId.takeIf { showsAlbumPhotos(inner, settings.unfoldedPhotos) })

    HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
        when (page) {
            0 -> ClockPage(store, openCount, smoothNow, tick, today, night, photos, weather, media, agenda, nextAlarm, deskBelow)
            1 -> WeatherPage(weather)
            else -> NowPlayingPage(media, tick)
        }
    }
}

@Composable
private fun ClockPage(
    store: SettingsStore,
    openCount: Int,
    now: State<Long>,
    tick: State<Long>,
    today: java.time.LocalDate,
    night: Boolean,
    photos: List<android.net.Uri>,
    weather: WeatherState,
    media: MediaState,
    agenda: AgendaState,
    nextAlarm: Long?,
    deskBelow: Boolean,
) {
    val inner = innerDisplay()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (clockPageLayout(inner, maxWidth, maxHeight, deskBelow) == ClockPageLayout.UnfoldedStack) {
            UnfoldedFace(store, now, tick, today, night, photos, weather, media)
        } else {
            ClockStyles(openCount, now, today, night, weather, agenda, nextAlarm, deskBelow)
        }
    }
}

@Composable
private fun ClockStyles(
    openCount: Int,
    now: State<Long>,
    today: java.time.LocalDate,
    night: Boolean,
    weather: WeatherState,
    agenda: AgendaState,
    nextAlarm: Long?,
    deskBelow: Boolean,
) {
    val count = clockStyleCount(deskBelow)
    val styles = rememberPagerState { count }
    LaunchedEffect(openCount) { if (styles.currentPage != 0) styles.scrollToPage(0) }
    Box(Modifier.fillMaxSize()) {
        VerticalPager(styles, Modifier.fillMaxSize()) { style ->
            when (style) {
                0 -> if (deskBelow) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        SquircleClock(
                            now = now,
                            modifier = Modifier.fillMaxSize().padding(minOf(maxWidth, maxHeight) * 0.06f),
                            showSeconds = !night,
                            smooth = !night,
                        )
                    }
                } else {
                    AnalogCalendarFace(now, today, showSeconds = !night, smooth = !night)
                }
                1 -> DigitalFace(today, weather, nextAlarm)
                else -> CalendarFace(today, agenda)
            }
        }
        StyleDots(count, styles.currentPage, Modifier.align(Alignment.CenterEnd))
    }
}

/** 아이폰 듀오처럼 오른쪽 가장자리에 세로로 놓인 페이지 점. */
@Composable
fun StyleDots(count: Int, current: Int, modifier: Modifier) {
    val p = LocalPalette.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val dot = maxHeight * 0.011f
        Column(
            modifier.padding(end = maxWidth * 0.012f),
            verticalArrangement = Arrangement.spacedBy(dot * 1.4f),
        ) {
            repeat(count) { i ->
                Box(Modifier.size(dot).background(if (i == current) p.primary else p.dim, CircleShape))
            }
        }
    }
}
