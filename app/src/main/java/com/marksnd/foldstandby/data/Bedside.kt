package com.marksnd.foldstandby.data

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext

/** 알람 몇 분 전부터 빛을 올리기 시작하는지. */
const val GLOW_LEAD_MS = 15 * 60_000L

/** 알림 접근이 없어 울림을 모를 때, 알람 시각 뒤로 빛을 유지하는 시간. */
private const val GLOW_AFTER_MS = 60_000L

/**
 * 알람 전 은은한 빛의 세기(0~1). 빛을 낼 때가 아니면 null.
 * 알람 15분 전부터 천천히 밝아지고, 알람이 울리는 동안은 최대로 유지한다.
 * 알람을 끄면 시계 앱이 다음 알람을 다시 잡으므로 [nextAlarm]이 바뀌어 저절로 꺼진다.
 */
fun glowLevel(now: Long, nextAlarm: Long?, ringing: Boolean): Float? {
    if (ringing) return 1f
    val alarm = nextAlarm ?: return null
    val start = alarm - GLOW_LEAD_MS
    return when {
        now < start -> null
        now < alarm -> {
            val t = (now - start).toFloat() / GLOW_LEAD_MS
            t * t // 처음엔 아주 천천히, 알람에 가까울수록 빠르게
        }
        now < alarm + GLOW_AFTER_MS -> 1f
        else -> null
    }
}

/** 이보다 큰 흔들림(m/s²)을 움직임으로 본다. 폰을 들거나 거치대를 건드리는 정도. */
private const val MOTION_THRESHOLD = 0.35f

/**
 * 마지막으로 움직임을 느낀 시각. [enabled]일 때만 가속도 센서를 듣는다(밤에만 켜서 배터리를 아낀다).
 * 중력을 천천히 따라가는 저역 필터를 빼고 남은 흔들림이 기준을 넘으면 움직임으로 본다.
 */
@Composable
fun rememberLastMotion(enabled: Boolean): State<Long> {
    val context = LocalContext.current
    return produceState(System.currentTimeMillis(), enabled) {
        if (!enabled) return@produceState
        val sm = context.getSystemService(SensorManager::class.java) ?: return@produceState
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return@produceState
        val gravity = FloatArray(3)
        var primed = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!primed) {
                    event.values.copyInto(gravity, endIndex = 3)
                    primed = true
                    return
                }
                var sq = 0f
                for (i in 0..2) {
                    gravity[i] = 0.9f * gravity[i] + 0.1f * event.values[i]
                    val d = event.values[i] - gravity[i]
                    sq += d * d
                }
                if (sq > MOTION_THRESHOLD * MOTION_THRESHOLD) value = System.currentTimeMillis()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitDispose { sm.unregisterListener(listener) }
    }
}
