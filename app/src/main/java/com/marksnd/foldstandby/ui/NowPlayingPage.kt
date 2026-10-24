package com.marksnd.foldstandby.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.marksnd.foldstandby.data.MediaState
import com.marksnd.foldstandby.data.NowPlaying

/** 재생 중 보기: 앨범 아트 + 곡 정보 + 진행 막대 + 이전/재생/다음. */
@Composable
fun NowPlayingPage(state: MediaState, tick: State<Long>) {
    val p = LocalPalette.current
    val korean = LocalKorean.current
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // 넓은 화면은 높이 기준, 세로로 긴 스택 칸은 너비 기준으로 글자 크기를 정한다.
        val u = if (maxWidth > maxHeight) minOf(maxHeight * 0.07f, maxWidth * 0.045f) else minOf(maxHeight * 0.06f, maxWidth * 0.07f)
        when (state) {
            MediaState.NoAccess -> Message(
                if (korean) "음악 정보를 보려면 알림 접근을 허용하세요" else "Allow notification access to show music",
                if (korean) "StandBy 설정 앱에서 켤 수 있어요" else "Turn it on in StandBy settings",
                u,
            )
            MediaState.Idle -> Message(
                if (korean) "재생 중인 음악이 없어요" else "Nothing playing",
                if (korean) "음악 앱에서 재생하면 여기 크게 보여요" else "Start music in any app to see it here",
                u,
            )
            is MediaState.Playing -> Player(state.now, tick, maxWidth > maxHeight, u)
        }
    }
}

@Composable
private fun Message(title: String, sub: String, u: Dp) {
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Label(title, u, p.primary, maxLines = 2)
        Spacer(Modifier.height(u * 0.4f))
        Label(sub, u * 0.7f, p.dim, weight = FontWeight.Medium, maxLines = 2)
    }
}

@Composable
private fun Player(np: NowPlaying, tick: State<Long>, landscape: Boolean, u: Dp) {
    val p = LocalPalette.current
    val art = remember(np.art) { np.art?.asImageBitmap() }
    Split(
        landscape = landscape,
        firstWeight = if (landscape) 0.42f else 0.5f,
        modifier = Modifier.padding(u),
        first = {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val shape = RoundedCornerShape(u * 0.8f)
                // 칸의 짧은 변에 맞춘 정사각형. 높이에만 맞추면 커버 화면에서 오른쪽 곡 정보와 겹친다.
                val side = minOf(maxWidth, maxHeight) * 0.92f
                val artModifier = Modifier
                    .size(side)
                    .clip(shape)
                    .alpha(if (p.night) 0.35f else 1f)
                if (art != null) {
                    Image(art, contentDescription = null, modifier = artModifier, contentScale = ContentScale.Crop)
                } else {
                    Box(artModifier.background(Brush.linearGradient(listOf(Color(0xFF2D5BD7), Color(0xFFD7457E)))))
                }
            }
        },
        second = {
            Column(
                Modifier.fillMaxSize().padding(start = u * 0.6f),
                verticalArrangement = Arrangement.Center,
            ) {
                Label(np.title, u * 1.25f, p.primary)
                Label(np.artist, u * 0.9f, p.dim, weight = FontWeight.Medium)
                Spacer(Modifier.height(u * 0.3f))
                Label(np.appLabel, u * 0.6f, p.accent, weight = FontWeight.SemiBold)
                Spacer(Modifier.height(u * 1.1f))
                Progress(np, tick, Modifier.fillMaxWidth().height(u * 0.35f))
                Spacer(Modifier.height(u * 1.2f))
                Controls(np, u * 2.2f)
            }
        },
    )
}

@Composable
private fun Progress(np: NowPlaying, tick: State<Long>, modifier: Modifier) {
    val p = LocalPalette.current
    Canvas(modifier) {
        tick.value // 1초마다 다시 그린다
        val r = CornerRadius(size.height / 2)
        drawRoundRect(p.dim, cornerRadius = r)
        val frac = if (np.duration > 0) np.positionNow().toFloat() / np.duration else 0f
        drawRoundRect(p.primary, size = Size(size.width * frac.coerceIn(0f, 1f), size.height), cornerRadius = r)
    }
}

private enum class Ctl { Prev, PlayPause, Next }

@Composable
private fun Controls(np: NowPlaying, button: Dp) {
    val t = np.controller.transportControls
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        ControlButton(Ctl.Prev, np.playing, button) { t.skipToPrevious() }
        ControlButton(Ctl.PlayPause, np.playing, button * 1.2f) { if (np.playing) t.pause() else t.play() }
        ControlButton(Ctl.Next, np.playing, button) { t.skipToNext() }
    }
}

@Composable
private fun ControlButton(kind: Ctl, playing: Boolean, buttonSize: Dp, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.size(buttonSize).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(buttonSize * 0.5f)) {
            when (kind) {
                Ctl.Prev -> {
                    drawRect(p.primary, Offset(0f, 0f), Size(this.size.width * 0.1f, this.size.height))
                    triangle(p.primary, this.size.width * 0.15f, this.size.width, left = true)
                }
                Ctl.Next -> {
                    triangle(p.primary, 0f, this.size.width * 0.85f, left = false)
                    drawRect(p.primary, Offset(this.size.width * 0.9f, 0f), Size(this.size.width * 0.1f, this.size.height))
                }
                Ctl.PlayPause -> if (playing) {
                    val w = this.size.width * 0.28f
                    drawRect(p.primary, Offset(this.size.width * 0.12f, 0f), Size(w, this.size.height))
                    drawRect(p.primary, Offset(this.size.width * 0.6f, 0f), Size(w, this.size.height))
                } else {
                    triangle(p.primary, this.size.width * 0.12f, this.size.width, left = false)
                }
            }
        }
    }
}

private fun DrawScope.triangle(color: Color, from: Float, to: Float, left: Boolean) {
    val h = size.height
    val path = Path().apply {
        if (left) {
            moveTo(to, 0f); lineTo(from, h / 2); lineTo(to, h)
        } else {
            moveTo(from, 0f); lineTo(to, h / 2); lineTo(from, h)
        }
        close()
    }
    drawPath(path, color)
}
