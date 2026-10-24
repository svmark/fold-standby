package com.marksnd.foldstandby.ui

import android.app.KeyguardManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.marksnd.foldstandby.service.IncomingNotification
import com.marksnd.foldstandby.service.NotificationBus
import kotlinx.coroutines.delay

private const val SHOW_MS = 6_000L

/**
 * 새 알림을 위쪽에 크게 띄웠다가 6초 뒤 내린다.
 * 잠겨 있으면 앱 이름만 보여주고 제목·내용은 숨긴다.
 */
@Composable
fun NotificationBanner(maxWidth: Dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val korean = LocalKorean.current
    val context = LocalContext.current
    val latest by NotificationBus.latest.collectAsState()
    val startedAt = remember { System.currentTimeMillis() }
    var visible by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf<IncomingNotification?>(null) }
    var locked by remember { mutableStateOf(true) }

    LaunchedEffect(latest) {
        val n = latest ?: return@LaunchedEffect
        if (n.postedAt < startedAt) return@LaunchedEffect // 화면이 켜지기 전 알림은 띄우지 않는다
        locked = context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked ?: true
        shown = n
        visible = true
        delay(SHOW_MS)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val n = shown ?: return@AnimatedVisibility
        val icon = remember(n.packageName) {
            runCatching { context.packageManager.getApplicationIcon(n.packageName).toBitmap(128, 128).asImageBitmap() }.getOrNull()
        }
        Row(
            Modifier
                .padding(top = 16.dp)
                .widthIn(max = maxWidth * 0.7f)
                .clip(RoundedCornerShape(28.dp))
                .background(p.card)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Image(icon, contentDescription = null, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(14.dp))
            }
            Column {
                Label("${n.appLabel} · ${if (korean) "지금" else "now"}", 14.dp, p.dim, weight = FontWeight.Medium)
                if (locked) {
                    Label(if (korean) "새 알림" else "New notification", 20.dp, p.primary)
                } else {
                    Label(n.title, 20.dp, p.primary)
                    if (n.text.isNotBlank()) Label(n.text, 16.dp, p.secondary, weight = FontWeight.Normal, maxLines = 2)
                }
            }
        }
    }
}
