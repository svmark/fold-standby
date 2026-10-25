package com.marksnd.foldstandby.ui

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

/** 사진이 바뀌는 간격. */
private const val PHOTO_INTERVAL_MS = 30_000L

/** 화면에 맞춰 줄여 읽을 사진의 긴 변(px). 펼친 화면 반쪽보다 조금 크게. */
private const val PHOTO_MAX_SIDE = 1800

/**
 * 고른 앨범 사진을 섞어서 30초마다 천천히 바꿔 보여준다. 왼쪽 아래에 지금 시각.
 * 밤에는 사진을 어둡게 한다.
 */
@Composable
fun PhotoFrame(photos: List<Uri>, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val sources = LocalStandbySources.current
    val order = remember(photos) { photos.shuffled() }
    var index by remember(order) { mutableIntStateOf(0) }
    var current by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(order) {
        while (order.size > 1) {
            delay(PHOTO_INTERVAL_MS)
            index = (index + 1) % order.size
        }
    }
    val uri: Uri? = order.getOrNull(index)
    LaunchedEffect(uri) {
        // 읽지 못한 사진(지워졌거나 권한이 바뀐 경우)은 건너뛴다.
        val bitmap = uri?.let { sources.loadPhoto(context, it, PHOTO_MAX_SIDE) }
        if (bitmap != null) current = bitmap else if (order.size > 1) index = (index + 1) % order.size
    }

    val now by sources.now(smooth = false)
    val time = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(timeFormatter())

    // 펼친 화면에서는 오른쪽 위에 전면 카메라가 있다. 사진 윗변을 카메라 아래로 내린다.
    BoxWithConstraints(modifier.avoidInnerCamera()) {
        val radius = minOf(maxWidth, maxHeight) * 0.06f
        val timeSize = with(LocalDensity.current) { (maxHeight * 0.13f).toSp() }
        BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(radius)).background(p.card)) {
            Crossfade(targetState = current, animationSpec = tween(1500), label = "photo") { bitmap ->
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (p.night) 0.3f else 1f,
                    )
                }
            }
            Text(
                text = time,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = maxWidth * 0.06f, bottom = maxHeight * 0.05f),
                color = p.primary,
                fontSize = timeSize,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 2f), blurRadius = 16f)),
            )
        }
    }
}
