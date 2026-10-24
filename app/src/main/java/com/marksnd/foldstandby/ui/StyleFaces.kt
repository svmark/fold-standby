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
import androidx.compose.ui.unit.em
import com.marksnd.foldstandby.data.AgendaEvent
import com.marksnd.foldstandby.data.AgendaState
import com.marksnd.foldstandby.data.WeatherState
import com.marksnd.foldstandby.data.rememberNow
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
    val now = rememberNow(smooth = false)
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
        val apW = measurer.measure(ap, base.copy(fontSize = (digitSize * 0.18f).toSp())).size.width
        val total = digitsW + apW + (digitSize * 0.06f).toPx()
        minOf(1f, available.toPx() / total)
    }
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
fun CalendarFace(today: LocalDate, agenda: AgendaState) {
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
                TimeWithAmPm(hm, ap, h * 0.11f)
            }
            Column(Modifier.fillMaxSize().padding(start = w * 0.47f, end = w * 0.05f, top = h * 0.10f, bottom = h * 0.06f)) {
                AgendaColumn(today, agenda, AgendaSizes(h * 0.04f, h * 0.05f, h * 0.04f, h * 0.05f, h * 0.04f), todayMax = 3)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(start = w * 0.08f, end = w * 0.08f, top = h * 0.06f, bottom = h * 0.04f)) {
                Label(weekday, h * 0.034f, p.accent, weight = FontWeight.Black, family = Rounded, letterSpacing = 0.1.em)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Label(today.dayOfMonth.toString(), h * 0.19f, p.primary, weight = FontWeight.Black, family = Rounded, letterSpacing = (-0.04).em)
                    Spacer(Modifier.weight(1f))
                    TimeWithAmPm(hm, ap, h * 0.07f, Modifier.padding(bottom = h * 0.016f))
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
    // 남은 높이 안에 들어가게 센다: 내일 칸을 먼저 잡아 두고, 오늘 일정은 남는 만큼만 보여주고 나머지는 +N.
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
        val labelH = s.label * 1.15f
        val eventH = s.title * 1.12f + s.sub * 1.12f + s.sub * 0.6f
        val lineH = s.sub * 1.1f * 1.12f + s.sub * 0.25f
        val moreH = s.sub * 1.12f + s.sub * 0.25f
        val today = timedFirst(agenda.events)
        val tomorrow = timedFirst(agenda.tomorrow)
        val tShow = minOf(TOMORROW_MAX, tomorrow.size)
        val tomorrowH = if (tomorrow.isEmpty()) 0f
        else (s.label * 0.6f + labelH + lineH * tShow + (if (tomorrow.size > tShow) moreH else s.sub * 0f)) / s.sub
        val roomForToday = (maxHeight - labelH - s.label * 0.4f) / s.sub - tomorrowH
        var fit = (roomForToday / (eventH / s.sub)).toInt()
        if (today.size > fit) fit = ((roomForToday - moreH / s.sub) / (eventH / s.sub)).toInt()
        val todayShow = minOf(today.size, todayMax, fit.coerceAtLeast(1))
        Column {
            Label(if (korean) "오늘" else "TODAY", s.label, p.accent, weight = FontWeight.Black, family = Rounded, letterSpacing = 0.06.em)
            Spacer(Modifier.height(s.label * 0.4f))
            if (today.isEmpty()) {
                Label(if (korean) "남은 일정 없음" else "No more events", s.title * 0.9f, p.dim, weight = FontWeight.ExtraBold, family = Rounded)
            }
            today.take(todayShow).forEach { e ->
                EventLine(e, s, korean)
                Spacer(Modifier.height(s.sub * 0.6f))
            }
            MoreCount(today.size - todayShow, s, korean)
            if (tomorrow.isNotEmpty()) {
                Spacer(Modifier.height(s.label * 0.6f))
                Label(if (korean) "내일" else "TOMORROW", s.label, p.secondary, weight = FontWeight.Black, family = Rounded, letterSpacing = 0.06.em)
                tomorrow.take(tShow).forEach { e ->
                    val time = if (e.allDay) (if (korean) "종일" else "All day") else eventTime(e.begin, korean)
                    Label("$time  ${e.title}", s.sub * 1.1f, p.secondary, Modifier.padding(top = s.sub * 0.25f), weight = FontWeight.Bold, family = Rounded)
                }
                MoreCount(tomorrow.size - tShow, s, korean)
            }
        }
    }
}

private const val TOMORROW_MAX = 2

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
