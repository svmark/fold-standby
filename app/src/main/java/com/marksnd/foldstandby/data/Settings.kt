package com.marksnd.foldstandby.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class NightMode { Auto, On, Off }

data class Settings(
    val accent: Int = 0,
    val korean: Boolean = false,
    val nightMode: NightMode = NightMode.Auto,
    val mondayFirst: Boolean = true,
    val alarmGlow: Boolean = true,
    val wakeOnMotion: Boolean = true,
    /** 펼친 화면 오른쪽에 시계 대신 사진을 보여줄지. */
    val unfoldedPhotos: Boolean = true,
    /** 고른 앨범(MediaStore 버킷). 고르지 않았으면 null. */
    val photoAlbumId: Long? = null,
    val photoAlbumName: String = "",
    /** 펼친 화면 오른쪽 스택에서 마지막으로 본 카드. */
    val stackCard: com.marksnd.foldstandby.ui.StackCard = com.marksnd.foldstandby.ui.StackCard.Photos,
)

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("standby", Context.MODE_PRIVATE)

    fun read() = Settings(
        accent = prefs.getInt(ACCENT, 0),
        korean = prefs.getBoolean(KOREAN, false),
        nightMode = NightMode.entries.getOrElse(prefs.getInt(NIGHT, 0)) { NightMode.Auto },
        mondayFirst = prefs.getBoolean(MONDAY_FIRST, true),
        alarmGlow = prefs.getBoolean(ALARM_GLOW, true),
        wakeOnMotion = prefs.getBoolean(WAKE_ON_MOTION, true),
        unfoldedPhotos = prefs.getBoolean(UNFOLDED_PHOTOS, true),
        photoAlbumId = prefs.getLong(PHOTO_ALBUM_ID, 0L).takeIf { prefs.contains(PHOTO_ALBUM_ID) },
        photoAlbumName = prefs.getString(PHOTO_ALBUM_NAME, "") ?: "",
        stackCard = com.marksnd.foldstandby.ui.StackCard.entries.firstOrNull { it.name == prefs.getString(STACK_CARD, null) }
            ?: com.marksnd.foldstandby.ui.StackCard.Photos,
    )

    fun update(block: SharedPreferences.Editor.() -> Unit) = prefs.edit().apply(block).apply()

    /** 설정 화면을 한 번이라도 열었는지. 처음 실행 때 설정을 자동으로 보여주는 데 쓴다. */
    val setupSeen: Boolean get() = prefs.getBoolean(SETUP_SEEN, false)

    fun flow(): Flow<Settings> = callbackFlow {
        trySend(read())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        const val ACCENT = "accent"
        const val KOREAN = "korean"
        const val NIGHT = "night"
        const val MONDAY_FIRST = "mondayFirst"
        const val ALARM_GLOW = "alarmGlow"
        const val WAKE_ON_MOTION = "wakeOnMotion"
        const val UNFOLDED_PHOTOS = "unfoldedPhotos"
        const val PHOTO_ALBUM_ID = "photoAlbumId"
        const val PHOTO_ALBUM_NAME = "photoAlbumName"
        const val STACK_CARD = "stackCard"
        const val SETUP_SEEN = "setupSeen"
    }
}

@Composable
fun rememberSettingsStore(): SettingsStore {
    val context = LocalContext.current
    return remember { SettingsStore(context) }
}

@Composable
fun rememberSettings(store: SettingsStore): State<Settings> {
    val flow = remember(store) { store.flow() }
    return flow.collectAsState(store.read())
}
