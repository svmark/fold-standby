package com.marksnd.foldstandby.data

import android.Manifest
import android.accounts.Account
import android.content.ContentResolver
import android.os.Bundle
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.CalendarContract.Instances
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.ZoneId

data class AgendaEvent(
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val color: Int,
)

/** [events]는 오늘 아직 끝나지 않은 일정, [tomorrow]는 내일 일정. */
data class AgendaState(
    val granted: Boolean,
    val events: List<AgendaEvent>,
    val tomorrow: List<AgendaEvent> = emptyList(),
)

fun hasCalendarPermission(context: Context) =
    context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

/** 오늘(아직 끝나지 않은 것)과 내일 일정. 종일 일정이 먼저 온다. */
suspend fun queryAgenda(context: Context): AgendaState = withContext(Dispatchers.IO) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = today.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
    val uri = Instances.CONTENT_URI.buildUpon().also {
        ContentUris.appendId(it, start)
        ContentUris.appendId(it, end)
    }.build()
    val projection = arrayOf(Instances.TITLE, Instances.BEGIN, Instances.END, Instances.ALL_DAY, Instances.DISPLAY_COLOR)
    val now = System.currentTimeMillis()
    val all = context.contentResolver.query(
        uri, projection, "${Instances.VISIBLE} = 1", null,
        "${Instances.ALL_DAY} DESC, ${Instances.BEGIN} ASC",
    )?.use { c ->
        buildList {
            while (c.moveToNext()) {
                val e = AgendaEvent(
                    title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "(제목 없음)",
                    begin = c.getLong(1),
                    end = c.getLong(2),
                    allDay = c.getInt(3) == 1,
                    color = c.getInt(4),
                )
                add(e)
            }
        }
    } ?: emptyList()
    // 종일 일정의 시각은 UTC 자정 기준이라 날짜도 UTC로 읽는다.
    fun dateOf(e: AgendaEvent) = java.time.Instant.ofEpochMilli(e.begin).atZone(if (e.allDay) java.time.ZoneOffset.UTC else zone).toLocalDate()
    AgendaState(
        granted = true,
        events = all.filter { dateOf(it) <= today && (it.allDay || it.end > now) },
        tomorrow = all.filter { dateOf(it) == today.plusDays(1) },
    )
}

/**
 * 폰에 있는 구글 등 캘린더 계정에 지금 동기화하라고 요청한다. 웹·다른 기기에서 추가한 일정이
 * 폰에 빨리 들어오게 하려는 것. 동기화가 끝나 캘린더가 바뀌면 바뀜 알림으로 다시 읽힌다.
 */
suspend fun requestCalendarSync(context: Context) = withContext(Dispatchers.IO) {
    if (!hasCalendarPermission(context)) return@withContext
    runCatching {
        val accounts = mutableSetOf<Account>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.ACCOUNT_TYPE),
            "${CalendarContract.Calendars.VISIBLE} = 1", null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0) ?: continue
                val type = c.getString(1) ?: continue
                if (type != CalendarContract.ACCOUNT_TYPE_LOCAL) accounts += Account(name, type)
            }
        }
        val extras = Bundle().apply {
            putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        }
        accounts.forEach { ContentResolver.requestSync(it, CalendarContract.AUTHORITY, extras) }
    }
}

/** 오늘·내일 일정. 캘린더가 바뀌면 바로, 스탠바이를 열 때마다, 그 밖에는 5분마다 다시 읽는다. */
@Composable
fun rememberAgenda(refreshKey: Int = 0): State<AgendaState> {
    val context = LocalContext.current
    // 캘린더가 바뀔 때와 스탠바이를 열 때(refreshKey가 바뀔 때) 같은 통로로 다시 읽기를 알린다.
    val changes = remember { Channel<Unit>(Channel.CONFLATED) }
    LaunchedEffect(refreshKey) {
        requestCalendarSync(context)
        changes.trySend(Unit)
    }
    return produceState(AgendaState(hasCalendarPermission(context), emptyList())) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                changes.trySend(Unit)
            }
        }
        var registered = false
        try {
            while (true) {
                val granted = hasCalendarPermission(context)
                if (granted && !registered) {
                    registered = runCatching {
                        context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
                    }.isSuccess
                }
                value = if (granted) runCatching { queryAgenda(context) }.getOrDefault(AgendaState(true, emptyList())) else AgendaState(false, emptyList())
                withTimeoutOrNull(5 * 60_000L) { changes.receive() }
            }
        } finally {
            if (registered) context.contentResolver.unregisterContentObserver(observer)
        }
    }
}
