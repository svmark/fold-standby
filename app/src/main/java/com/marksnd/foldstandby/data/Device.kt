package com.marksnd.foldstandby.data

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime

data class BatteryInfo(val percent: Int, val plugged: Int) {
    val charging get() = plugged != 0
    val wireless get() = plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
}

private fun Intent.toBattery(): BatteryInfo {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    return BatteryInfo(level * 100 / scale, getIntExtra(BatteryManager.EXTRA_PLUGGED, 0))
}

@Composable
fun rememberBattery(): State<BatteryInfo> {
    val context = LocalContext.current
    return produceState(BatteryInfo(0, 0)) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                value = intent.toBattery()
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )?.let { value = it.toBattery() }
        awaitDispose { context.unregisterReceiver(receiver) }
    }
}

/** 다음 알람 시각(epoch ms). 없으면 null. */
@Composable
fun rememberNextAlarm(): State<Long?> {
    val context = LocalContext.current
    return produceState<Long?>(null) {
        val am = context.getSystemService(AlarmManager::class.java)
        value = am?.nextAlarmClock?.triggerTime
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                value = am?.nextAlarmClock?.triggerTime
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitDispose { context.unregisterReceiver(receiver) }
    }
}

/**
 * 주변이 어두운지. 조도 센서로 판단하고(3 lx 아래면 어둡다, 10 lx 위면 밝다),
 * 센서가 없으면 22시~6시를 어둡다고 본다.
 */
@Composable
fun rememberIsDark(): State<Boolean> {
    val context = LocalContext.current
    return produceState(false) {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (sm == null || sensor == null) {
            var running = true
            while (running) {
                val h = LocalTime.now().hour
                value = h >= 22 || h < 6
                delay(60_000)
                running = isActive
            }
            return@produceState
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val lux = event.values[0]
                if (lux < 3f) value = true else if (lux > 10f) value = false
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitDispose { sm.unregisterListener(listener) }
    }
}
