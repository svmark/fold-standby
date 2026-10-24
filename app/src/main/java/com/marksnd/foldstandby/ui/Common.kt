package com.marksnd.foldstandby.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.marksnd.foldstandby.R
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

/** 화면 전체가 쓰는 색. 야간에는 모든 요소가 어두운 빨강으로 바뀐다. */
data class Palette(
    val accent: Color,
    val primary: Color,
    val secondary: Color,
    val dim: Color,
    val tick: Color,
    val card: Color,
    val onAccent: Color,
    val night: Boolean,
)

// 첫 번째가 기본값. 아이폰 듀오 스탠바이 사진의 주황.
val Accents = listOf(Color(0xFFF59E2E), Color(0xFFF5C542), Color(0xFFFFFFFF), Color(0xFF6EE7C8), Color(0xFFFF8FB1))
val AccentNames = listOf("주황", "노랑", "흰색", "민트", "분홍")

fun dayPalette(accent: Color) = Palette(
    accent = accent,
    primary = Color(0xFFF0EDF6),
    secondary = Color(0xFFA9A5B4),
    dim = Color(0xFF6E6B76),
    tick = Color(0xFFBDB9C6),
    card = Color(0xFF1C1C1E),
    onAccent = if (accent.luminance() > 0.6f) Color.Black else Color.White,
    night = false,
)

val NightPalette = Palette(
    accent = Color(0xFFB0241B),
    primary = Color(0xFFC8322A),
    secondary = Color(0xFFC8322A),
    dim = Color(0xFF5C1410),
    tick = Color(0xFF7A1A14),
    card = Color(0xFF1A0605),
    onAccent = Color.Black,
    night = true,
)

/** 디지털·달력 스타일에 쓰는 둥근 굵은 글꼴(Nunito, SIL OFL). 한글은 시스템 글꼴로 대신 그린다. */
@OptIn(ExperimentalTextApi::class)
val Rounded = FontFamily(
    Font(R.font.nunito, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    Font(R.font.nunito, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

val LocalPalette = staticCompositionLocalOf { dayPalette(Accents[0]) }
val LocalKorean = staticCompositionLocalOf { false }
val LocalMondayFirst = staticCompositionLocalOf { true }

/** 글자 크기를 dp로 받아 화면 크기에 비례하게 맞춘다(시스템 글꼴 배율 무시). */
@Composable
fun Label(
    text: String,
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Bold,
    align: TextAlign? = null,
    maxLines: Int = 1,
    family: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    val sp = with(LocalDensity.current) { size.toSp() }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = sp,
        fontWeight = weight,
        fontFamily = family,
        letterSpacing = letterSpacing,
        textAlign = align,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        lineHeight = sp * 1.12f,
        style = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        ),
    )
}

/** 가로면 좌우, 세로면 위아래로 두 칸을 나눈다. */
@Composable
fun Split(
    landscape: Boolean,
    firstWeight: Float,
    modifier: Modifier = Modifier,
    first: @Composable BoxScope.() -> Unit,
    second: @Composable BoxScope.() -> Unit,
) {
    if (landscape) {
        Row(modifier.fillMaxSize()) {
            Box(Modifier.weight(firstWeight).fillMaxHeight(), content = first)
            Box(Modifier.weight(1f - firstWeight).fillMaxHeight(), content = second)
        }
    } else {
        Column(modifier.fillMaxSize()) {
            Box(Modifier.weight(firstWeight).fillMaxWidth(), content = first)
            Box(Modifier.weight(1f - firstWeight).fillMaxWidth(), content = second)
        }
    }
}
