package com.marksnd.foldstandby

import android.content.Intent
import android.provider.Settings
import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.marksnd.foldstandby.ui.Posture
import com.marksnd.foldstandby.ui.StandbyScreen
import com.marksnd.foldstandby.ui.applyBrightness

/**
 * 충전 중 화면 보호기. 시스템이 충전 중 화면이 꺼질 때 띄운다.
 * DreamService는 Activity가 아니라서 Compose가 찾는 Lifecycle·SavedState 주인을 직접 제공한다.
 */
class StandbyDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 터치를 받아야 스와이프와 음악 버튼이 동작한다. 끌 때는 전원 버튼.
        isInteractive = true
        isFullscreen = true

        window?.let { w ->
            w.decorView.setViewTreeLifecycleOwner(this)
            w.decorView.setViewTreeSavedStateRegistryOwner(this)
            WindowCompat.getInsetsController(w, w.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@StandbyDream)
            setViewTreeSavedStateRegistryOwner(this@StandbyDream)
            setContent {
                StandbyScreen(
                    posture = Posture.Flat,
                    onOpenSettings = ::openSettings,
                    onBrightness = { level -> window?.let { applyBrightness(it, level) } },
                )
            }
        }
        setContentView(view)
    }

    /** 화면 보호기를 끝내고 설정 화면을 연다. 잠겨 있으면 잠금 해제 뒤에 보인다. */
    private fun openSettings() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        wakeUp()
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        // 화면 보호기 창은 폰의 회전 잠금을 따르고 앱이 바꿀 수 없다. 회전이 잠겨 있으면
        // 같은 화면을 앱(StandbyActivity, 센서 방향으로 회전)으로 넘기고 화면 보호기는 끝낸다.
        // finish()로 끝내면 화면이 꺼지므로 wakeUp()으로 깨우며 끝낸다.
        if (Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 0) {
            startActivity(
                Intent(this, StandbyActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(StandbyActivity.EXTRA_FROM_DREAM, true),
            )
            wakeUp()
            return
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
