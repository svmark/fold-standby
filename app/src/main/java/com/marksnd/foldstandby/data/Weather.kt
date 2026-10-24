package com.marksnd.foldstandby.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** 지금 시각을 포함한 시간별 예보 개수. */
private const val HOURLY_COUNT = 7

data class HourlyWeather(val time: LocalDateTime, val temp: Int, val code: Int, val isDay: Boolean)

data class Weather(
    val place: String,
    val temp: Int,
    val code: Int,
    val isDay: Boolean,
    val high: Int,
    val low: Int,
    val feelsLike: Int,
    val humidity: Int,
    val windMs: Int,
    val hourly: List<HourlyWeather>,
    val fetchedAt: LocalDateTime,
)

sealed interface WeatherState {
    data object Loading : WeatherState
    data object Failed : WeatherState
    data class Ready(val weather: Weather) : WeatherState
}

enum class Sky { Clear, PartlyCloudy, Cloudy, Fog, Rain, Snow, Storm }

/** WMO 날씨 코드 → 하늘 상태. https://open-meteo.com/en/docs */
fun skyOf(code: Int): Sky = when (code) {
    0, 1 -> Sky.Clear
    2 -> Sky.PartlyCloudy
    3 -> Sky.Cloudy
    45, 48 -> Sky.Fog
    in 51..67, in 80..82 -> Sky.Rain
    in 71..77, 85, 86 -> Sky.Snow
    in 95..99 -> Sky.Storm
    else -> Sky.Cloudy
}

fun skyLabel(sky: Sky, korean: Boolean): String = when (sky) {
    Sky.Clear -> if (korean) "맑음" else "Clear"
    Sky.PartlyCloudy -> if (korean) "구름 조금" else "Partly cloudy"
    Sky.Cloudy -> if (korean) "흐림" else "Cloudy"
    Sky.Fog -> if (korean) "안개" else "Fog"
    Sky.Rain -> if (korean) "비" else "Rain"
    Sky.Snow -> if (korean) "눈" else "Snow"
    Sky.Storm -> if (korean) "뇌우" else "Thunderstorm"
}

private const val SEOUL_LAT = 37.5665
private const val SEOUL_LON = 126.9780

private fun hasLocationPermission(context: Context) =
    context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun lastLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val lm = context.getSystemService(LocationManager::class.java) ?: return null
    return try {
        listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    } catch (_: SecurityException) {
        null
    }
}

@Suppress("DEPRECATION")
private fun placeName(context: Context, lat: Double, lon: Double, korean: Boolean): String {
    val locale = if (korean) Locale.KOREAN else Locale.ENGLISH
    val address = runCatching { Geocoder(context, locale).getFromLocation(lat, lon, 1)?.firstOrNull() }.getOrNull()
    return address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: if (korean) "현재 위치" else "Here"
}

/** Open-Meteo는 무료이고 API 키가 필요 없다. 위치 권한이 없으면 서울 날씨를 보여준다. */
suspend fun fetchWeather(context: Context, korean: Boolean): Weather = withContext(Dispatchers.IO) {
    val loc = lastLocation(context)
    val lat = loc?.latitude ?: SEOUL_LAT
    val lon = loc?.longitude ?: SEOUL_LON
    val place = if (loc == null) (if (korean) "서울" else "Seoul") else placeName(context, lat, lon, korean)
    val url = URL(
        "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,is_day,apparent_temperature,relative_humidity_2m,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code,is_day&forecast_hours=$HOURLY_COUNT" +
            "&daily=temperature_2m_max,temperature_2m_min" +
            "&wind_speed_unit=ms&timezone=auto&forecast_days=2",
    )
    val conn = url.openConnection() as HttpURLConnection
    conn.connectTimeout = 10_000
    conn.readTimeout = 10_000
    try {
        val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        val hourly = json.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weather_code")
        val days = hourly.getJSONArray("is_day")
        Weather(
            place = place,
            temp = current.getDouble("temperature_2m").roundToInt(),
            code = current.getInt("weather_code"),
            isDay = current.getInt("is_day") == 1,
            high = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
            low = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
            feelsLike = current.getDouble("apparent_temperature").roundToInt(),
            humidity = current.getDouble("relative_humidity_2m").roundToInt(),
            windMs = current.getDouble("wind_speed_10m").roundToInt(),
            hourly = (0 until times.length()).map {
                HourlyWeather(LocalDateTime.parse(times.getString(it)), temps.getDouble(it).roundToInt(), codes.getInt(it), days.getInt(it) == 1)
            },
            fetchedAt = LocalDateTime.now(),
        )
    } finally {
        conn.disconnect()
    }
}

/** 30분마다 새로 받고, 실패하면 5분 뒤 다시 시도한다. */
@Composable
fun rememberWeather(korean: Boolean): State<WeatherState> {
    val context = LocalContext.current
    return produceState<WeatherState>(WeatherState.Loading, korean) {
        while (true) {
            val result = runCatching { fetchWeather(context, korean) }
            result.onSuccess { value = WeatherState.Ready(it) }
                .onFailure { if (value !is WeatherState.Ready) value = WeatherState.Failed }
            delay(if (result.isSuccess) 30 * 60_000L else 5 * 60_000L)
        }
    }
}
