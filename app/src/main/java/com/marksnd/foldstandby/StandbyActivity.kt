package com.marksnd.foldstandby

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.WindowInfoTracker
import com.marksnd.foldstandby.data.SettingsStore
import com.marksnd.foldstandby.data.rememberBattery
import com.marksnd.foldstandby.ui.Posture
import com.marksnd.foldstandby.ui.StandbyScreen
import com.marksnd.foldstandby.ui.applyBrightness
import com.marksnd.foldstandby.ui.postureOf
import kotlinx.coroutines.launch

/** 손으로 여는 스탠바이 화면. 전체화면이고, 충전 중일 때만 화면을 켜 둔다. */
class StandbyActivity : ComponentActivity() {

    private var posture by mutableStateOf(Posture.Flat)

    /** 화면이 다시 보일 때마다 늘어난다. 스탠바이는 이때 달력+시계로 돌아간다. */
    private var openCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        // 화면 보호기가 넘겨준 경우에는 화면 보호기처럼 잠금 화면 위에 보인다.
        if (intent.getBooleanExtra(EXTRA_FROM_DREAM, false)) setShowWhenLocked(true)

        // 아이콘이 하나뿐이라, 처음 실행할 때는 설정(권한·화면 보호기 안내)을 먼저 보여준다.
        if (savedInstanceState == null && !SettingsStore(this).setupSeen) {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@StandbyActivity)
                    .windowLayoutInfo(this@StandbyActivity)
                    .collect { posture = postureOf(it) }
            }
        }

        setContent {
            val battery by rememberBattery()
            var wasCharging by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(battery.charging) {
                // 충전 중이던 폰을 충전기에서 떼면 아이폰 스탠바이처럼 닫는다.
                // 충전 없이 직접 연 경우(처음부터 충전 중 아님)는 그대로 둔다.
                if (wasCharging == true && !battery.charging) finish()
                wasCharging = battery.charging
                if (battery.charging) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            StandbyScreen(
                posture = posture,
                onOpenSettings = { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                onBrightness = { level -> applyBrightness(window, level) },
                openCount = openCount,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        openCount++
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_FROM_DREAM, false)) setShowWhenLocked(true)
    }

    private fun hideSystemBars() {
        // 위 상태 표시줄만 숨기고 아래 내비게이션(제스처 막대)은 둔다.
        // 둘 다 숨기면 첫 번째 위로 밀기는 막대만 꺼내고 두 번째에야 홈으로 나가서 앱이 잘 안 닫힌다.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            show(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        /** 화면 보호기가 회전 때문에 이 화면으로 넘겼을 때 붙는 표시. */
        const val EXTRA_FROM_DREAM = "fromDream"
    }
}
