package com.marksnd.foldstandby.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import com.marksnd.foldstandby.data.AgendaState
import com.marksnd.foldstandby.data.MediaState
import com.marksnd.foldstandby.data.WeatherState
import com.marksnd.foldstandby.data.rememberAgenda
import com.marksnd.foldstandby.data.rememberAlbumPhotos
import com.marksnd.foldstandby.data.rememberIsDark
import com.marksnd.foldstandby.data.rememberLastMotion
import com.marksnd.foldstandby.data.rememberNextAlarm
import com.marksnd.foldstandby.data.rememberNow
import com.marksnd.foldstandby.data.rememberNowPlaying
import com.marksnd.foldstandby.data.rememberPixelShift
import com.marksnd.foldstandby.data.rememberToday
import com.marksnd.foldstandby.data.rememberWeather
import java.time.LocalDate
import com.marksnd.foldstandby.data.loadPhoto as loadPhotoFromStore

/**
 * 스탠바이 화면이 바깥에서 읽는 값(시각, 날씨, 일정, 음악, 사진, 센서).
 * 앱에서는 [LiveSources]가 실제 값을 읽고, 화면 스냅숏 테스트는 고정 값을 넣는다.
 */
interface StandbySources {
    @Composable fun now(smooth: Boolean): State<Long>
    @Composable fun today(): State<LocalDate>
    @Composable fun weather(korean: Boolean): State<WeatherState>
    @Composable fun agenda(refreshKey: Int): State<AgendaState>
    @Composable fun nowPlaying(): State<MediaState>
    @Composable fun albumPhotos(albumId: Long?): State<List<Uri>>
    @Composable fun nextAlarm(): State<Long?>
    @Composable fun isDark(): State<Boolean>
    @Composable fun lastMotion(enabled: Boolean): State<Long>
    @Composable fun pixelShift(stepPx: Int): State<IntOffset>
    suspend fun loadPhoto(context: Context, uri: Uri, maxSide: Int): ImageBitmap?
    fun currentTimeMillis(): Long
}

object LiveSources : StandbySources {
    @Composable override fun now(smooth: Boolean) = rememberNow(smooth)
    @Composable override fun today() = rememberToday()
    @Composable override fun weather(korean: Boolean) = rememberWeather(korean)
    @Composable override fun agenda(refreshKey: Int) = rememberAgenda(refreshKey)
    @Composable override fun nowPlaying() = rememberNowPlaying()
    @Composable override fun albumPhotos(albumId: Long?) = rememberAlbumPhotos(albumId)
    @Composable override fun nextAlarm() = rememberNextAlarm()
    @Composable override fun isDark() = rememberIsDark()
    @Composable override fun lastMotion(enabled: Boolean) = rememberLastMotion(enabled)
    @Composable override fun pixelShift(stepPx: Int) = rememberPixelShift(stepPx)
    override suspend fun loadPhoto(context: Context, uri: Uri, maxSide: Int) = loadPhotoFromStore(context, uri, maxSide)
    override fun currentTimeMillis() = System.currentTimeMillis()
}

val LocalStandbySources = staticCompositionLocalOf<StandbySources> { LiveSources }
