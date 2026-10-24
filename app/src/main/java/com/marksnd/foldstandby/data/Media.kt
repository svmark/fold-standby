package com.marksnd.foldstandby.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.marksnd.foldstandby.service.NotifListener
import kotlinx.coroutines.delay

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val appLabel: String,
    val playing: Boolean,
    val position: Long,
    val positionUpdatedAt: Long,
    val speed: Float,
    val duration: Long,
    val controller: MediaController,
) {
    /** 마지막 갱신 이후 흐른 시간을 더한 현재 재생 위치. */
    fun positionNow(): Long {
        if (!playing) return position
        val elapsed = SystemClock.elapsedRealtime() - positionUpdatedAt
        val pos = position + (elapsed * speed).toLong()
        return if (duration > 0) pos.coerceIn(0, duration) else pos
    }
}

fun notificationAccessGranted(context: Context) =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/** 활성 미디어 세션 중 재생 중인 것을 골라 따라간다. 알림 접근 권한이 있어야 동작한다. */
private class MediaTracker(
    private val context: Context,
    private val onChange: (NowPlaying?) -> Unit,
) : MediaController.Callback(), MediaSessionManager.OnActiveSessionsChangedListener {

    private val msm = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, NotifListener::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var current: MediaController? = null

    fun start(): Boolean = try {
        msm.addOnActiveSessionsChangedListener(this, component, handler)
        onActiveSessionsChanged(msm.getActiveSessions(component))
        true
    } catch (_: SecurityException) {
        false
    }

    fun stop() {
        runCatching { msm.removeOnActiveSessionsChangedListener(this) }
        current?.unregisterCallback(this)
        current = null
    }

    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        val list = controllers.orEmpty()
        val best = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        if (best?.sessionToken != current?.sessionToken) {
            current?.unregisterCallback(this)
            current = best
            best?.registerCallback(this, handler)
        }
        emit()
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) = emit()

    override fun onPlaybackStateChanged(state: PlaybackState?) {
        // 다른 앱이 재생을 시작했을 수 있으니 다시 고른다.
        onActiveSessionsChanged(runCatching { msm.getActiveSessions(component) }.getOrNull())
    }

    override fun onSessionDestroyed() {
        current?.unregisterCallback(this)
        current = null
        onActiveSessionsChanged(runCatching { msm.getActiveSessions(component) }.getOrNull())
    }

    private fun emit() = onChange(current?.let(::snapshot))

    private fun snapshot(c: MediaController): NowPlaying? {
        val m = c.metadata ?: return null
        val title = m.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: return null
        val artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val art = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val pm = context.packageManager
        val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString() }
            .getOrDefault(c.packageName)
        val s = c.playbackState
        return NowPlaying(
            title = title,
            artist = artist,
            art = art,
            appLabel = label,
            playing = s?.state == PlaybackState.STATE_PLAYING,
            position = s?.position ?: 0,
            positionUpdatedAt = s?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = s?.playbackSpeed ?: 1f,
            duration = m.getLong(MediaMetadata.METADATA_KEY_DURATION),
            controller = c,
        )
    }
}

sealed interface MediaState {
    data object NoAccess : MediaState
    data object Idle : MediaState
    data class Playing(val now: NowPlaying) : MediaState
}

@Composable
fun rememberNowPlaying(): State<MediaState> {
    val context = LocalContext.current
    return produceState<MediaState>(MediaState.Idle) {
        // 권한은 설정 앱에서 켜므로, 켜질 때까지 가끔 확인한다.
        while (!notificationAccessGranted(context)) {
            value = MediaState.NoAccess
            delay(5_000)
        }
        val tracker = MediaTracker(context) { np -> value = np?.let { MediaState.Playing(it) } ?: MediaState.Idle }
        if (!tracker.start()) value = MediaState.NoAccess
        awaitDispose { tracker.stop() }
    }
}
