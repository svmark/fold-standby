package com.marksnd.foldstandby.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.marksnd.foldstandby.data.rememberNow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// ---- 날짜 표기 ----

fun weekdayShort(d: LocalDate, korean: Boolean): String =
    if (korean) d.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.KOREAN)
    else d.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.ENGLISH)

fun monthShort(d: LocalDate, korean: Boolean): String =
    if (korean) "${d.monthValue}월" else d.month.getDisplayName(JTextStyle.SHORT, Locale.ENGLISH)

private fun monthTitle(d: LocalDate, korean: Boolean): String =
    if (korean) "${d.monthValue}월" else d.month.getDisplayName(JTextStyle.FULL, Locale.ENGLISH).uppercase()

private fun weekHeaders(korean: Boolean, mondayFirst: Boolean): List<String> {
    val sundayFirst = if (korean) listOf("일", "월", "화", "수", "목", "금", "토") else listOf("S", "M", "T", "W", "T", "F", "S")
    return if (mondayFirst) sundayFirst.drop(1) + sundayFirst.first() else sundayFirst
}

@Composable
fun timeFormatter(): DateTimeFormatter {
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    return DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm")
}

// ---- 1. 달력 + 아날로그 시계 (아이폰 듀오 스탠바이) ----

@Composable
fun AnalogCalendarFace(
    now: State<Long>,
    today: LocalDate,
    showSeconds: Boolean,
    smooth: Boolean,
) {
    // 펼친 안쪽 화면(가장 짧은 변 600dp 이상)에서는 좌우를 반씩 나눈다.
    // 책처럼 살짝 접어 세우면 접힘선 왼쪽은 달력, 오른쪽은 시계가 된다.
    val innerDisplay = LocalConfiguration.current.smallestScreenWidthDp >= 600
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val pad = minOf(maxWidth, maxHeight) * 0.04f
        Split(
            landscape = landscape,
            firstWeight = when {
                landscape && innerDisplay -> 0.5f
                landscape -> 0.4f
                else -> 0.45f
            },
            first = { DatePanel(today, Modifier.fillMaxSize()) },
            second = {
                SquircleClock(
                    now = now,
                    modifier = Modifier.fillMaxSize().padding(pad),
                    showSeconds = showSeconds,
                    smooth = smooth,
                )
            },
        )
    }
}

@Composable
fun DatePanel(today: LocalDate, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val korean = LocalKorean.current
    BoxWithConstraints(modifier) {
        val h = maxHeight
        val w = maxWidth
        // 세로 화면에서는 날짜 영역이 가로로 넓고 낮다. 큰 날짜와 여백을 줄여
        // 달력 칸 높이를 늘리고, 날짜 숫자를 가로 화면과 비슷한 크기로 맞춘다.
        val wide = w > h
        val header = minOf(h * if (wide) 0.07f else 0.085f, w * 0.12f)
        val big = minOf(h * if (wide) 0.22f else 0.32f, w * 0.42f)
        val gridHeight = h * if (wide) 0.56f else 0.42f
        Column(
            Modifier.fillMaxSize().padding(
                start = w * if (wide) 0.06f else 0.1f,
                end = w * 0.04f,
                top = h * if (wide) 0.04f else 0.07f,
                bottom = h * if (wide) 0.02f else 0.05f,
            ),
        ) {
            Row {
                Label(weekdayShort(today, korean) + " ", header, p.accent)
                Label(monthShort(today, korean), header, p.secondary)
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.CenterStart) {
                Label(today.dayOfMonth.toString(), big, p.primary)
            }
            MonthGrid(today, Modifier.fillMaxWidth().height(gridHeight))
        }
    }
}

@Composable
fun MonthGrid(today: LocalDate, modifier: Modifier) {
    val p = LocalPalette.current
    val korean = LocalKorean.current
    val mondayFirst = LocalMondayFirst.current
    val first = today.withDayOfMonth(1)
    val offset = if (mondayFirst) first.dayOfWeek.value - 1 else first.dayOfWeek.value % 7
    val weekendCols = if (mondayFirst) setOf(5, 6) else setOf(0, 6)
    val length = today.lengthOfMonth()
    val weeks = (offset + length + 6) / 7
    BoxWithConstraints(modifier) {
        val cellH = maxHeight / (weeks + 2)
        val cellW = maxWidth / 7
        val fs = minOf(cellH * 0.55f, cellW * 0.42f)
        Column {
            Box(Modifier.height(cellH), contentAlignment = Alignment.CenterStart) {
                Label(monthTitle(today, korean), fs, p.accent)
            }
            Row {
                weekHeaders(korean, mondayFirst).forEachIndexed { col, d ->
                    Box(Modifier.weight(1f).height(cellH), contentAlignment = Alignment.Center) {
                        Label(d, fs, if (col in weekendCols) p.dim else p.primary)
                    }
                }
            }
            for (week in 0 until weeks) {
                Row {
                    for (col in 0 until 7) {
                        val day = week * 7 + col - offset + 1
                        Box(Modifier.weight(1f).height(cellH), contentAlignment = Alignment.Center) {
                            if (day in 1..length) {
                                val isToday = day == today.dayOfMonth
                                if (isToday) {
                                    Box(Modifier.size(minOf(cellH, cellW) * 0.95f).background(p.accent, CircleShape))
                                }
                                Label(
                                    day.toString(), fs,
                                    when {
                                        isToday -> p.onAccent
                                        col in weekendCols -> p.dim
                                        else -> p.primary
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
