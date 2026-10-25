package com.marksnd.foldstandby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.marksnd.foldstandby.data.AgendaEvent
import com.marksnd.foldstandby.data.AgendaState
import com.marksnd.foldstandby.data.WeatherState
import com.marksnd.foldstandby.data.skyLabel
import com.marksnd.foldstandby.data.skyOf
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// ---- 12시간제 표기 ----

/** "5:26" 과 "AM"(한국어면 "오전"). 이 두 스타일은 폰 설정과 상관없이 12시간제로 보여준다. */
fun twelveHour(t: LocalTime, korean: Boolean): Pair<String, String> {
    val h = (t.hour + 11) % 12 + 1
    val ap = if (t.hour < 12) (if (korean) "오전" else "AM") else (if (korean) "오후" else "PM")
    return "$h:%02d".format(t.minute) to ap
}

private fun eventTime(ms: Long, korean: Boolean): String {
    val (hm, ap) = twelveHour(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime(), korean)
    return if (korean) "$ap $hm" else "$hm $ap"
}

@Composable
private fun rememberClockText(): Pair<String, String> {
    val now = LocalStandbySources.current.now(smooth = false)
    return twelveHour(Instant.ofEpochMilli(now.value).atZone(ZoneId.systemDefault()).toLocalTime(), LocalKorean.current)
}

/** 큰 시각 옆에 오전/오후를 강조색으로, 글자 바닥선을 맞춰 붙인다. */
@Composable
private fun TimeWithAmPm(hm: String, ap: String, size: Dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Label(hm, size, p.primary, Modifier.alignByBaseline(), weight = FontWeight.Black, family = Rounded)
        Spacer(Modifier.width(size * 0.12f))
        Label(ap, size * 0.4f, p.accent, Modifier.alignByBaseline(), weight = FontWeight.Black, family = Rounded)
    }
}

// ---- 2. 디지털 (아이폰 Digital 방식) ----

/**
 * 둥근 굵은 숫자를 크게, 위에서 아래로 흰색 → 강조색 그라디언트. 숫자 옆 오전/오후.
 * 가로: 왼쪽에 날짜·날씨·다음 알람, 오른쪽에 숫자. 세로: 숫자 아래에 정보.
 * 숫자+오전/오후 묶음이 칸보다 넓으면 글자 크기를 줄여 잘리지 않게 한다.
 */
@Composable
fun DigitalFace(today: LocalDate, weather: WeatherState, nextAlarm: Long?) {
    val korean = LocalKorean.current
    val (hm, ap) = rememberClockText()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        if (w > h) {
            Box(Modifier.fillMaxHeight().padding(start = w * 0.06f).width(w * 0.22f), contentAlignment = Alignment.CenterStart) {
                Column(verticalArrangement = Arrangement.spacedBy(h * 0.04f)) {
                    DigitalInfo(today, weather, nextAlarm, h * 0.07f, h * 0.05f, korean, stacked = true)
                }
            }
            Box(Modifier.fillMaxSize().padding(start = w * 0.30f, end = w * 0.07f), contentAlignment = Alignment.Center) {
                BigTime(hm, ap, h * 0.44f, w * 0.63f)
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = w * 0.07f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BigTime(hm, ap, h * 0.2f, w * 0.86f)
                Spacer(Modifier.height(h * 0.04f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(h * 0.015f)) {
                    DigitalInfo(today, weather, nextAlarm, h * 0.035f, h * 0.026f, korean, stacked = false)
                }
            }
        }
    }
}

@Composable
private fun BigTime(hm: String, ap: String, digitSize: Dp, available: Dp) {
    val p = LocalPalette.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val base = TextStyle(
        fontFamily = Rounded,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.02).em,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    // 기준 크기에서 숫자·간격·오전/오후 너비를 재고, 넘치면 같은 비율로 줄인다.
    val scale = with(density) {
        val digitsW = measurer.measure(hm, base.copy(fontSize = digitSize.toSp())).size.width
        // 오전/오후는 Label로 그리므로 자간 없이 잰다(한글 "오전"은 "AM"보다 넓다).
        val apW = measurer.measure(ap, base.copy(fontSize = (digitSize * 0.18f).toSp(), letterSpacing = TextUnit.Unspecified)).size.width
        val total = digitsW + apW + (digitSize * 0.06f).toPx()
        // 화면이 막 뜰 때 칸 크기가 잠깐 0이면 0/0이 되어 멈춘다. 그때는 줄이지 않는다.
        // 줄일 때는 반올림으로 1px 넘쳐 "…"이 붙지 않도록 3% 여유를 둔다.
        val ratio = available.toPx() * 0.97f / total
        if (total > 0f && ratio.isFinite()) minOf(1f, ratio) else 1f
    }
    if (digitSize <= 0.dp || !digitSize.value.isFinite()) return
    val size = digitSize * scale
    val brush = if (p.night) Brush.verticalGradient(listOf(p.primary, p.primary))
    else Brush.verticalGradient(0.15f to Color.White, 1f to lerp(Color.White, p.accent, 0.55f))
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = hm,
            modifier = Modifier.alignByBaseline(),
            style = base.copy(fontSize = with(density) { size.toSp() }, brush = brush),
            maxLines = 1,
        )
        Spacer(Modifier.width(size * 0.06f))
        Label(ap, size * 0.18f, p.accent, Modifier.alignByBaseline(), weight = FontWeight.Black, family = Rounded)
    }
}

/** 날짜 · 날씨 · 다음 알람. stacked면 날짜를 두 줄로(가로 화면 왼쪽 칸), 아니면 한 줄. */
@Composable
private fun DigitalInfo(
    today: LocalDate,
    weather: WeatherState,
    nextAlarm: Long?,
    dateSize: Dp,
    lineSize: Dp,
    korean: Boolean,
    stacked: Boolean,
) {
    val p = LocalPalette.current
    val sep = if (stacked) "\n" else " "
    val date = if (korean) "${weekdayShort(today, true)}$sep${today.monthValue}월 ${today.dayOfMonth}일"
    else "${weekdayShort(today, false)}$sep${monthShort(today, false)} ${today.dayOfMonth}"
    Label(date, dateSize, p.accent, weight = FontWeight.Black, family = Rounded, maxLines = 2)
    if (weather is WeatherState.Ready) {
        val sky = skyOf(weather.weather.code)
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkyIcon(sky, weather.weather.isDay, Modifier.size(lineSize * 1.3f))
            Spacer(Modifier.width(lineSize * 0.3f))
            Label("${weather.weather.temp}° ${skyLabel(sky, korean)}", lineSize, p.secondary, weight = FontWeight.ExtraBold, family = Rounded)
        }
    }
    if (nextAlarm != null) {
        Label("⏰ " + eventTime(nextAlarm, korean), lineSize, p.dim, weight = FontWeight.ExtraBold, family = Rounded)
    }
}

// ---- 3. 달력 (아이폰 캘린더 위젯 방식) ----

/**
 * 가로: 왼쪽에 요일·큰 날짜·월과 맨 아래 시각(화면 높이 11%, 오전/오후 40%),
 * 오른쪽에 이번 주 줄과 오늘·내일 일정. 세로: 큰 날짜 오른쪽에 시각, 아래에 이번 주 줄과 일정.
 * 일정이 없거나 캘린더 권한이 없으면 일정 대신 이번 달 달력.
 */
@Composable
fun CalendarFace(today: LocalDate, agenda: AgendaState, showTime: Boolean = true) {
    val p = LocalPalette.current
    val korean = LocalKorean.current
    val (hm, ap) = rememberClockText()
    val weekday = today.dayOfWeek.getDisplayName(JTextStyle.FULL, if (korean) Locale.KOREAN else Locale.ENGLISH)
        .let { if (korean) it else it.uppercase() }
    val monthYear = if (korean) "${today.year}년 ${today.monthValue}월"
    else "${today.month.getDisplayName(JTextStyle.FULL, Locale.ENGLISH)} ${today.year}"

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        if (w > h) {
            Column(Modifier.fillMaxHeight().padding(start = w * 0.06f, top = h * 0.09f, bottom = h * 0.09f).width(w * 0.37f)) {
                Label(weekday, h * 0.056f, p.accent, weight = FontWeight.Black, family = Rounded, letterSpacing = 0.1.em)
                Label(today.dayOfMonth.toString(), h * 0.35f, p.primary, weight = FontWeight.Black, family = Rounded, letterSpacing = (-0.04).em)
                Label(monthYear, h * 0.054f, p.secondary, weight = FontWeight.ExtraBold, family = Rounded)
                Spacer(Modifier.weight(1f))
                if (showTime) TimeWithAmPm(hm, ap, h * 0.11f)
            }
            Column(Modifier.fillMaxSize().padding(start = w * 0.47f, end = w * 0.05f, top = h * 0.10f, bottom = h * 0.06f)) {
                AgendaColumn(today, agenda, AgendaSizes(h * 0.04f, h * 0.05f, h * 0.04f, h * 0.05f, h * 0.04f), todayMax = 3)
            }
        } else {
            // 큰 날짜와 시각은 한 줄에 놓인다. 좁고 긴 세로 화면(21:9, 19.5:9)에서는 높이만 따르면
            // 둘을 합친 폭이 화면보다 넓어져 시각이 잘리므로, 폭에 맞춘 크기를 넘지 않게 한다.
            val daySize = minOf(h * 0.19f, w * 0.40f)
            val timeSize = minOf(h * 0.07f, w * 0.12f)
            Column(Modifier.fillMaxSize().padding(start = w * 0.08f, end = w * 0.08f, top = h * 0.06f, bottom = h * 0.04f)) {
                Label(weekday, h * 0.034f, p.accent, weight = FontWeight.Black, family = Rounded, letterSpacing = 0.1.em)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Label(today.dayOfMonth.toString(), daySize, p.primary, weight = FontWeight.Black, family = Rounded, letterSpacing = (-0.04).em)
                    Spacer(Modifier.weight(1f))
                    if (showTime) TimeWithAmPm(hm, ap, timeSize, Modifier.padding(bottom = h * 0.016f))
                }
                Label(monthYear, h * 0.032f, p.secondary, weight = FontWeight.ExtraBold, family = Rounded)
                Spacer(Modifier.height(h * 0.04f))
                AgendaColumn(today, agenda, AgendaSizes(h * 0.024f, h * 0.03f, h * 0.024f, h * 0.03f, h * 0.024f), todayMax = 4)
            }
        }
    }
}

private data class AgendaSizes(val letter: Dp, val number: Dp, val label: Dp, val title: Dp, val sub: Dp)

@Composable
private fun ColumnScope.AgendaColumn(today: LocalDate, agenda: AgendaState, s: AgendaSizes, todayMax: Int) {
    val p = LocalPalette.current
    val korean = LocalKorean.current
    WeekStrip(today, s)
    Spacer(Modifier.height(s.label * 1.2f))
    if (!agenda.granted || (agenda.events.isEmpty() && agenda.tomorrow.isEmpty())) {
        // 보여줄 일정이 없으면 이번 달 달력
        MonthGrid(today, Modifier.fillMaxWidth().weight(1f))
        return
    }
    // 남은 높이 안에 들어가게 센다. 내일은 제목과 한 줄을 늘 잡아 두고, 남는 높이는 오늘 일정에 먼저 준다.
    // 못 보여준 일정은 숨기지 않고 +N으로 알린다.
    val line = rememberRoundedLineRatio()
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
        val today = timedFirst(agenda.events)
        val tomorrow = timedFirst(agenda.tomorrow)
        val fit = fitAgenda(
            room = maxHeight.value,
            todayCount = today.size,
            tomorrowCount = tomorrow.size,
            todayMax = todayMax,
            h = AgendaHeights(
                todayHead = (s.label * line + s.label * 0.4f).value,
                empty = (s.title * 0.9f * line).value,
                event = (s.title * line + s.sub * line).value,
                eventGap = (s.sub * 0.6f).value,
                more = (s.sub * line + s.sub * 0.25f).value,
                tomorrowHead = (s.label * 0.6f + s.label * line).value,
                line = (s.sub * 1.1f * line + s.sub * 0.25f).value,
            ),
        )
        Column {
            Label(if (korean) "오늘" else "TODAY", s.label, p.accent, weight = FontWeight.Black, family = Rounded, letterSpacing = 0.06.em)
            Spacer(Modifier.height(s.label * 0.4f))
            if (today.isEmpty()) {
                Label(if (korean) "남은 일정 없음" else "No more events", s.title * 0.9f, p.dim, weight = FontWeight.ExtraBold, family = Rounded)
            }
            today.take(fit.todayShow).forEachIndexed { i, e ->
                // 일정 사이에만 간격을 둔다(마지막 일정 뒤에는 +N이나 내일 제목이 제 간격을 갖고 온다).
                if (i > 0) Spacer(Modifier.height(s.sub * 0.6f))
                EventLine(e, s, korean)
            }
            MoreCount(today.size - fit.todayShow, s, korean)
            if (tomorrow.isNotEmpty()) {
                Spacer(Modifier.height(s.label * 0.6f))
                Label(if (korean) "내일" else "TOMORROW", s.label, p.secondary, weight = FontWeight.Black, family = Rounded, letterSpacing = 0.06.em)
                val hidden = tomorrow.size - fit.tomorrowShow
                tomorrow.take(fit.tomorrowShow).forEachIndexed { i, e ->
                    val time = if (e.allDay) (if (korean) "종일" else "All day") else eventTime(e.begin, korean)
                    // +N 줄을 둘 높이가 없으면 마지막 줄 끝에 붙인다.
                    val inlineMore = if (i == fit.tomorrowShow - 1 && hidden > 0 && !fit.tomorrowMoreLine) "  +$hidden" else ""
                    Label("$time  ${e.title}$inlineMore", s.sub * 1.1f, p.secondary, Modifier.padding(top = s.sub * 0.25f), weight = FontWeight.Bold, family = Rounded)
                }
                if (fit.tomorrowMoreLine) MoreCount(hidden, s, korean)
            }
        }
    }
}

/** 일정 칸 각 부분의 높이(dp). */
internal data class AgendaHeights(
    val todayHead: Float,
    val empty: Float,
    val event: Float,
    val eventGap: Float,
    val more: Float,
    val tomorrowHead: Float,
    val line: Float,
)

/** 오늘 [todayShow]개, 내일 [tomorrowShow]줄을 보여주고, 내일의 나머지는 [tomorrowMoreLine]이면 따로 한 줄, 아니면 마지막 줄 끝에 +N. */
internal data class AgendaFit(val todayShow: Int, val tomorrowShow: Int, val tomorrowMoreLine: Boolean)

/**
 * [room] 높이 안에 들어갈 만큼 센다.
 * 1. 내일 일정이 있으면 내일 제목과 한 줄을 먼저 잡는다.
 * 2. 남는 높이를 오늘 일정에 준다(최대 [todayMax]개). 다 못 보여주면 +N 줄 자리도 뺀다. 오늘 일정이 있으면 적어도 하나는 보여준다.
 * 3. 그래도 남으면 내일 둘째 줄([TOMORROW_MAX]까지), 그다음 내일 +N 줄.
 */
internal fun fitAgenda(room: Float, todayCount: Int, tomorrowCount: Int, todayMax: Int, h: AgendaHeights): AgendaFit {
    var left = room - h.todayHead
    if (tomorrowCount > 0) left -= h.tomorrowHead + h.line
    var todayShow = 0
    if (todayCount == 0) {
        left -= h.empty
    } else {
        val limit = minOf(todayCount, todayMax)
        // n개는 일정 n개 + 사이 간격 n-1개
        fun fits(space: Float) = ((space + h.eventGap) / (h.event + h.eventGap)).toInt().coerceAtLeast(0)
        todayShow = minOf(limit, fits(left))
        if (todayShow < todayCount) todayShow = minOf(todayShow, fits(left - h.more))
        todayShow = todayShow.coerceAtLeast(1)
        left -= h.event * todayShow + h.eventGap * (todayShow - 1) + if (todayShow < todayCount) h.more else 0f
    }
    var tomorrowShow = if (tomorrowCount > 0) 1 else 0
    while (tomorrowShow < minOf(TOMORROW_MAX, tomorrowCount) && left >= h.line) {
        left -= h.line
        tomorrowShow++
    }
    val moreLine = tomorrowCount > tomorrowShow && left >= h.more
    return AgendaFit(todayShow, tomorrowShow, moreLine)
}

private const val TOMORROW_MAX = 2

/**
 * 둥근 글꼴 Label 한 줄이 실제로 차지하는 높이 ÷ 글자 크기를 이 기기에서 잰다.
 * Label의 줄 높이(1.12)보다 글꼴 자체 높이가 커서 글꼴 높이를 따른다. 영문(Nunito)은 약 1.37,
 * 한글은 시스템 대체 글꼴이라 약 1.51이고 기기 글꼴에 따라 다르므로, 한글과 영문을 함께 재어 큰 쪽을 쓴다.
 */
@Composable
private fun rememberRoundedLineRatio(): Float {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(measurer, density) {
        val size = 100.dp
        val sp = with(density) { size.toSp() }
        val style = TextStyle(
            fontFamily = Rounded,
            fontWeight = FontWeight.Black,
            fontSize = sp,
            lineHeight = sp * 1.12f,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        )
        with(density) { measurer.measure("오늘 Ag", style).size.height.toDp() } / size
    }
}

/** 시간이 있는 일정을 시작 순으로 먼저, 종일 일정은 뒤로. 종일 일정이 많아도 약속이 밀려나지 않게. */
private fun timedFirst(events: List<AgendaEvent>) = events.sortedWith(compareBy({ it.allDay }, { it.begin }))

/** 화면에 다 못 보여준 일정 수. */
@Composable
private fun MoreCount(hidden: Int, s: AgendaSizes, korean: Boolean) {
    if (hidden <= 0) return
    Label(
        if (korean) "외 ${hidden}개" else "+$hidden more",
        s.sub, LocalPalette.current.dim, Modifier.padding(top = s.sub * 0.25f), weight = FontWeight.ExtraBold, family = Rounded,
    )
}

@Composable
private fun EventLine(e: AgendaEvent, s: AgendaSizes, korean: Boolean) {
    val p = LocalPalette.current
    val bar = if (p.night) p.accent else Color(e.color or 0xFF000000.toInt())
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(s.sub * 0.3f).height(s.title * 1.9f).background(bar, RoundedCornerShape(s.sub)))
        Spacer(Modifier.width(s.sub * 0.6f))
        Column {
            Label(e.title, s.title, p.primary, weight = FontWeight.ExtraBold, family = Rounded)
            Label(
                if (e.allDay) (if (korean) "종일" else "All day") else "${eventTime(e.begin, korean)} – ${eventTime(e.end, korean)}",
                s.sub, p.dim, weight = FontWeight.Bold, family = Rounded,
            )
        }
    }
}

/** 오늘이 들어 있는 한 주. 오늘은 강조색 알약, 주말은 회색. */
@Composable
private fun WeekStrip(today: LocalDate, s: AgendaSizes) {
    val p = LocalPalette.current
    val korean = LocalKorean.current
    val mondayFirst = LocalMondayFirst.current
    val firstDay = if (mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val offset = (today.dayOfWeek.value - firstDay.value + 7) % 7
    val start = today.minusDays(offset.toLong())
    Row(Modifier.fillMaxWidth()) {
        for (i in 0 until 7) {
            val d = start.plusDays(i.toLong())
            val isToday = d == today
            val weekend = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
            val letter = if (korean) d.dayOfWeek.getDisplayName(JTextStyle.NARROW, Locale.KOREAN)
            else d.dayOfWeek.getDisplayName(JTextStyle.NARROW, Locale.ENGLISH)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = s.letter * 0.1f)
                    .background(if (isToday) p.accent else Color.Transparent, RoundedCornerShape(s.number * 0.8f))
                    .padding(vertical = s.letter * 0.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Label(letter, s.letter, if (isToday) p.onAccent else p.dim, weight = FontWeight.ExtraBold, family = Rounded)
                Label(
                    d.dayOfMonth.toString(), s.number,
                    when {
                        isToday -> p.onAccent
                        weekend -> p.dim
                        else -> p.primary
                    },
                    weight = FontWeight.Black, family = Rounded,
                )
            }
        }
    }
}
