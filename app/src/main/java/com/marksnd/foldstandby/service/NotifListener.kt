package com.marksnd.foldstandby.service

import android.app.Notification
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class IncomingNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long,
)

/** 알림 리스너가 받은 최신 알림을 화면 쪽에 넘긴다. */
object NotificationBus {
    private val latestFlow = MutableStateFlow<IncomingNotification?>(null)
    val latest: StateFlow<IncomingNotification?> = latestFlow.asStateFlow()

    fun post(n: IncomingNotification) {
        val prev = latestFlow.value
        // 같은 알림이 내용 그대로 다시 올라오면(진행 상황 갱신 등) 무시한다.
        if (prev != null && prev.key == n.key && prev.title == n.title && prev.text == n.text) return
        latestFlow.value = n
    }

    private val ringingFlow = MutableStateFlow<String?>(null)

    /** 지금 울리고 있는 알람 알림의 키. 없으면 null. */
    val ringingAlarm: StateFlow<String?> = ringingFlow.asStateFlow()

    fun alarmStarted(key: String) {
        ringingFlow.value = key
    }

    fun alarmStopped(key: String) {
        if (ringingFlow.value == key) ringingFlow.value = null
    }
}

/** 시계 앱이 알람을 울릴 때 띄우는 알림: 알람 분류이고 전체 화면으로 뜨거나 계속 떠 있다. */
private fun isRingingAlarm(sbn: StatusBarNotification): Boolean =
    sbn.notification.category == Notification.CATEGORY_ALARM &&
        (sbn.notification.fullScreenIntent != null || sbn.isOngoing)

/**
 * 세 가지 용도: 미디어 세션을 읽을 권한의 근거(MediaSessionManager가 이 컴포넌트를 요구),
 * 새로 울린 알림을 스탠바이 화면에 크게 띄우기, 알람이 울리는 동안 은은한 빛을 유지하기.
 */
class NotifListener : NotificationListenerService() {

    override fun onListenerConnected() {
        activeNotifications?.firstOrNull(::isRingingAlarm)?.let { NotificationBus.alarmStarted(it.key) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotificationBus.alarmStopped(sbn.key)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isRingingAlarm(sbn)) {
            NotificationBus.alarmStarted(sbn.key)
            return
        }
        val n = sbn.notification
        if (sbn.packageName == packageName || sbn.isOngoing) return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.category == Notification.CATEGORY_TRANSPORT || n.category == Notification.CATEGORY_SERVICE) return
        if (n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return

        val ranking = Ranking()
        if (currentRanking?.getRanking(sbn.key, ranking) == true &&
            ranking.importance < NotificationManager.IMPORTANCE_DEFAULT
        ) return

        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)

        NotificationBus.post(IncomingNotification(sbn.key, sbn.packageName, label, title, text, sbn.postTime))
    }
}
