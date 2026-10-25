package com.marksnd.foldstandby.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import com.marksnd.foldstandby.data.AgendaEvent
import com.marksnd.foldstandby.data.AgendaState
import com.marksnd.foldstandby.data.HourlyWeather
import com.marksnd.foldstandby.data.MediaState
import com.marksnd.foldstandby.data.Weather
import com.marksnd.foldstandby.data.WeatherState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** 화면 스냅숏용 고정 값: 2026-10-25(일) 오전 9:41, 서울 맑음, 예시 일정, 팬케이크 사진(CC0). */
class FixedSources(
    private val dark: Boolean = false,
    private val media: MediaState = MediaState.Idle,
) : StandbySources {
    val zone: ZoneId = ZoneId.of("Asia/Seoul")
    val nowTime: ZonedDateTime = ZonedDateTime.of(2026, 10, 25, 9, 41, 7, 0, zone)
    private val nowMs = nowTime.toInstant().toEpochMilli()
    private val day = nowTime.toLocalDate()

    private fun at(d: LocalDate, h: Int, m: Int) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    val agendaState = AgendaState(
        granted = true,
        events = listOf(
            AgendaEvent("Design review", at(day, 10, 30), at(day, 11, 30), false, 0xFF75DCC8.toInt()),
            AgendaEvent("Lunch with Jin", at(day, 13, 0), at(day, 14, 0), false, 0xFFFF8FB1.toInt()),
            AgendaEvent("Gym", at(day, 19, 0), at(day, 20, 0), false, 0xFF00A6A6.toInt()),
        ),
        tomorrow = listOf(
            AgendaEvent("Dentist", at(day.plusDays(1), 9, 30), at(day.plusDays(1), 10, 30), false, 0xFF00A6A6.toInt()),
            AgendaEvent(
                "Product launch",
                day.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
                day.plusDays(2).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
                true, 0xFFFF8FB1.toInt(),
            ),
        ),
    )

    val weatherState = WeatherState.Ready(
        Weather(
            place = "Seoul", temp = 15, code = 0, isDay = true, high = 23, low = 10,
            feelsLike = 14, humidity = 67, windMs = 1,
            hourly = (0 until 7).map { i ->
                HourlyWeather(LocalDateTime.of(day, java.time.LocalTime.of(10 + i, 0)), listOf(13, 16, 19, 21, 22, 23, 23)[i], 0, true)
            },
            fetchedAt = LocalDateTime.of(day, java.time.LocalTime.of(9, 40)),
        ),
    )

    private val photo = Uri.parse("test://photo/pancakes")

    @Composable private fun <T> fixed(value: T): State<T> = remember { mutableStateOf(value) }

    @Composable override fun now(smooth: Boolean) = fixed(nowMs)
    @Composable override fun today() = fixed(day)
    @Composable override fun weather(korean: Boolean): State<WeatherState> = fixed(weatherState)
    @Composable override fun agenda(refreshKey: Int) = fixed(agendaState)
    @Composable override fun nowPlaying() = fixed(media)
    @Composable override fun albumPhotos(albumId: Long?) = fixed(if (albumId != null) listOf(photo) else emptyList())
    @Composable override fun nextAlarm() = fixed<Long?>(null)
    @Composable override fun isDark() = fixed(dark)
    @Composable override fun lastMotion(enabled: Boolean) = fixed(nowMs)
    @Composable override fun pixelShift(stepPx: Int) = fixed(IntOffset.Zero)

    override suspend fun loadPhoto(context: Context, uri: Uri, maxSide: Int): ImageBitmap? =
        javaClass.getResourceAsStream("/sample-photo.jpg")?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }

    override fun currentTimeMillis() = nowMs
}
