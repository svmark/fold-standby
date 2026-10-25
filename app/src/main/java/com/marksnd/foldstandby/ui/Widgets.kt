package com.marksnd.foldstandby.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.marksnd.foldstandby.data.AgendaState
import com.marksnd.foldstandby.data.Sky
import com.marksnd.foldstandby.data.WeatherState
import com.marksnd.foldstandby.data.skyLabel
import com.marksnd.foldstandby.data.skyOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HM = DateTimeFormatter.ofPattern("HH:mm")
private fun hm(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(HM)

/**
 * 날씨 보기: 화면 전체를 쓴다.
 * 위: 지역·업데이트 시각 / 가운데: 하늘 그림·큰 기온·상태·최고최저·체감·습도·바람 / 아래: 시간별 예보.
 */
@Composable
fun WeatherPage(state: WeatherState) {
    val p = LocalPalette.current
    val korean = LocalKorean.current
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val u = minOf(maxHeight, maxWidth * 0.62f)
        when (state) {
            WeatherState.Loading -> Label(if (korean) "날씨 불러오는 중…" else "Loading weather…", u * 0.07f, p.dim)
            WeatherState.Failed -> Label(if (korean) "날씨를 불러오지 못했어요" else "Weather unavailable", u * 0.07f, p.dim)
            is WeatherState.Ready -> if (maxWidth < maxHeight * 1.2f) {
                CompactWeather(state)
            } else {
                val w = state.weather
                val sky = skyOf(w.code)
                Column(Modifier.fillMaxSize().padding(horizontal = maxWidth * 0.05f, vertical = maxHeight * 0.06f)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Label(w.place, u * 0.075f, p.accent)
                        Spacer(Modifier.weight(1f))
                        Label(
                            (if (korean) "업데이트 " else "Updated ") + w.fetchedAt.format(HM),
                            u * 0.045f, p.dim, weight = FontWeight.Medium,
                        )
                    }
                    Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        SkyIcon(sky, w.isDay, Modifier.fillMaxHeight(0.78f).aspectRatio(1f))
                        Spacer(Modifier.width(u * 0.03f))
                        Label("${w.temp}°", u * 0.34f, p.primary, weight = FontWeight.Light)
                        Spacer(Modifier.width(u * 0.05f))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Label(skyLabel(sky, korean), u * 0.075f, p.primary, weight = FontWeight.SemiBold, align = TextAlign.End, maxLines = 2)
                            // 한국어는 "최고 · 최저"가 길어 한 줄에 들어가지 않으므로 두 줄로 나눈다.
                            if (korean) {
                                Label("최고 ${w.high}°", u * 0.06f, p.secondary, weight = FontWeight.Medium)
                                Label("최저 ${w.low}°", u * 0.06f, p.secondary, weight = FontWeight.Medium)
                            } else {
                                Label("H ${w.high}° · L ${w.low}°", u * 0.06f, p.secondary, weight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(u * 0.03f))
                            Label(if (korean) "체감 ${w.feelsLike}°" else "Feels ${w.feelsLike}°", u * 0.05f, p.dim, weight = FontWeight.Medium)
                            Label(if (korean) "습도 ${w.humidity}%" else "Humidity ${w.humidity}%", u * 0.05f, p.dim, weight = FontWeight.Medium)
                            Label(if (korean) "바람 ${w.windMs}m/s" else "Wind ${w.windMs} m/s", u * 0.05f, p.dim, weight = FontWeight.Medium)
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(u * 0.28f)) {
                        w.hourly.forEachIndexed { i, h ->
                            Column(
                                Modifier.weight(1f).fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Label(
                                    if (i == 0) (if (korean) "지금" else "Now")
                                    else if (korean) "${h.time.hour}시" else h.time.format(HM),
                                    u * 0.045f, p.dim, weight = FontWeight.Medium,
                                )
                                SkyIcon(skyOf(h.code), h.isDay, Modifier.size(u * 0.11f))
                                Label("${h.temp}°", u * 0.06f, p.primary, weight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 세로로 긴 칸(펼친 화면 오른쪽 스택)용 날씨: 위에서 아래로 쌓는다. */
@Composable
private fun CompactWeather(state: WeatherState.Ready) {
    val p = LocalPalette.current
    val korean = LocalKorean.current
    val w = state.weather
    val sky = skyOf(w.code)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val u = minOf(maxWidth, maxHeight * 0.75f)
        Column(Modifier.fillMaxSize().padding(horizontal = maxWidth * 0.07f, vertical = maxHeight * 0.05f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(w.place, u * 0.08f, p.accent)
                Spacer(Modifier.weight(1f))
                Label(w.fetchedAt.format(HM), u * 0.05f, p.dim, weight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                SkyIcon(sky, w.isDay, Modifier.size(u * 0.36f))
                Spacer(Modifier.width(u * 0.04f))
                Label("${w.temp}°", u * 0.3f, p.primary, weight = FontWeight.Light)
            }
            Label(skyLabel(sky, korean), u * 0.075f, p.primary, weight = FontWeight.SemiBold)
            Label(
                if (korean) "최고 ${w.high}° · 최저 ${w.low}°" else "H ${w.high}° · L ${w.low}°",
                u * 0.06f, p.secondary, weight = FontWeight.Medium,
            )
            Spacer(Modifier.height(u * 0.02f))
            Label(
                if (korean) "체감 ${w.feelsLike}° · 습도 ${w.humidity}% · 바람 ${w.windMs}m/s"
                else "Feels ${w.feelsLike}° · ${w.humidity}% · ${w.windMs} m/s",
                u * 0.048f, p.dim, weight = FontWeight.Medium,
            )
            Spacer(Modifier.height(u * 0.06f))
            Row(Modifier.fillMaxWidth().height(u * 0.3f)) {
                w.hourly.take(5).forEachIndexed { i, h ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Label(
                            if (i == 0) (if (korean) "지금" else "Now") else if (korean) "${h.time.hour}시" else h.time.format(HM),
                            u * 0.05f, p.dim, weight = FontWeight.Medium,
                        )
                        SkyIcon(skyOf(h.code), h.isDay, Modifier.size(u * 0.11f))
                        Label("${h.temp}°", u * 0.065f, p.primary, weight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun SkyIcon(sky: Sky, isDay: Boolean, modifier: Modifier) {
    val p = LocalPalette.current
    Canvas(modifier) {
        val s = size.minDimension
        val sunC = Offset(s * 0.38f, s * 0.36f)
        fun cloud(color: Color, dy: Float = 0f) {
            drawCircle(color, s * 0.17f, Offset(s * 0.42f, s * 0.55f + dy))
            drawCircle(color, s * 0.22f, Offset(s * 0.6f, s * 0.48f + dy))
            drawRoundRect(color, Offset(s * 0.25f, s * 0.55f + dy), Size(s * 0.6f, s * 0.17f), CornerRadius(s * 0.09f))
        }
        // 밤에는 해 대신 초승달. 배경이 검정이라 검은 원을 겹쳐 깎는다.
        fun moon(c: Offset, r: Float) {
            drawCircle(p.secondary, r, c)
            drawCircle(Color.Black, r * 0.85f, Offset(c.x + r * 0.45f, c.y - r * 0.3f))
        }
        when (sky) {
            Sky.Clear -> if (!isDay) moon(Offset(s / 2, s / 2), s * 0.3f) else {
                drawCircle(p.accent, s * 0.22f, Offset(s / 2, s / 2))
                for (i in 0 until 8) {
                    val a = (i * Math.PI / 4).toFloat()
                    drawLine(
                        p.accent,
                        Offset(s / 2 + kotlin.math.cos(a) * s * 0.31f, s / 2 + kotlin.math.sin(a) * s * 0.31f),
                        Offset(s / 2 + kotlin.math.cos(a) * s * 0.42f, s / 2 + kotlin.math.sin(a) * s * 0.42f),
                        strokeWidth = s * 0.045f, cap = StrokeCap.Round,
                    )
                }
            }
            Sky.PartlyCloudy -> {
                if (isDay) drawCircle(p.accent, s * 0.18f, sunC) else moon(sunC, s * 0.2f)
                cloud(p.primary, s * 0.08f)
            }
            Sky.Cloudy -> cloud(p.primary)
            Sky.Fog -> repeat(3) { i ->
                drawLine(p.primary, Offset(s * 0.18f, s * (0.38f + i * 0.14f)), Offset(s * 0.82f, s * (0.38f + i * 0.14f)),
                    strokeWidth = s * 0.06f, cap = StrokeCap.Round)
            }
            Sky.Rain -> {
                cloud(p.primary, -s * 0.1f)
                repeat(3) { i ->
                    val x = s * (0.36f + i * 0.15f)
                    drawLine(p.secondary, Offset(x, s * 0.72f), Offset(x - s * 0.05f, s * 0.88f), strokeWidth = s * 0.045f, cap = StrokeCap.Round)
                }
            }
            Sky.Snow -> {
                cloud(p.primary, -s * 0.1f)
                repeat(3) { i -> drawCircle(p.secondary, s * 0.035f, Offset(s * (0.36f + i * 0.15f), s * (0.78f + (i % 2) * 0.08f))) }
            }
            Sky.Storm -> {
                cloud(p.primary, -s * 0.12f)
                val bolt = Path().apply {
                    moveTo(s * 0.55f, s * 0.62f); lineTo(s * 0.43f, s * 0.8f); lineTo(s * 0.53f, s * 0.8f)
                    lineTo(s * 0.46f, s * 0.97f); lineTo(s * 0.64f, s * 0.74f); lineTo(s * 0.54f, s * 0.74f); close()
                }
                drawPath(bolt, p.accent)
            }
        }
    }
}

/** 펼친 화면·반접기 아래쪽 띠: 다음 일정 몇 개를 한 줄로. */
@Composable
fun AgendaStrip(agenda: AgendaState, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val korean = LocalKorean.current
    BoxWithConstraints(modifier) {
        val u = minOf(maxHeight * 0.32f, maxWidth * 0.03f)
        Row(
            Modifier.fillMaxSize().padding(horizontal = maxWidth * 0.04f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u * 1.6f),
        ) {
            val upcoming = agenda.events.filter { !it.allDay }.take(3)
            if (upcoming.isEmpty()) {
                Label(
                    if (!agenda.granted) (if (korean) "캘린더 권한을 허용하면 일정이 여기 보여요" else "Allow calendar access to see events here")
                    else (if (korean) "오늘 남은 일정 없음" else "No more events today"),
                    u, p.dim, weight = FontWeight.Medium,
                )
            }
            upcoming.forEach { e ->
                Row(Modifier.weight(1f, fill = false).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Label(hm(e.begin), u, p.accent)
                    Label("  " + e.title, u, p.primary, weight = FontWeight.Medium)
                }
            }
        }
    }
}
