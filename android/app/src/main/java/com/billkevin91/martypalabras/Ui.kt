package com.billkevin91.martypalabras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.runtime.getValue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Colores y tipografías del concurso espacial, y piezas de interfaz comunes. */

object C {
    val space = Color(0xFF150F3A)
    val space2 = Color(0xFF241B5E)
    val card = Color(0xFF2B2270)
    val ink = Color(0xFFF6F1FF)
    val muted = Color(0xFFB9B0EC)
    val line = Color(0xFF44399A)
    val sun = Color(0xFFFFC93C)
    val berry = Color(0xFFFF6B8B)
    val sky = Color(0xFF4CC3FF)
    val leaf = Color(0xFF5ED38B)
    val grape = Color(0xFFA98BFF)
    val orange = Color(0xFFFF9F4A)
    val onColor = Color(0xFF1B1448)
    val bad = Color(0xFFFF8087)
    val paper = Color(0xFFFFF4CF)
    val paperInk = Color(0xFF4A3200)
    val gold = Color(0xFFD69A00)
    val tiles = listOf(sun, berry, sky, leaf, grape)
}

val Baloo = FontFamily(Font(R.font.baloo_extrabold, FontWeight.ExtraBold))
val Nunito = FontFamily(Font(R.font.nunito_bold, FontWeight.Bold), Font(R.font.nunito_extrabold, FontWeight.ExtraBold))

fun display(size: Int, color: Color = C.ink) =
    TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = size.sp, lineHeight = (size * 1.1f).sp, color = color)

fun body(size: Int, color: Color = C.ink, weight: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = Nunito, fontWeight = weight, fontSize = size.sp, lineHeight = (size * 1.35f).sp, color = color)

/** Clic sin la sombra gris de Material. */
@Composable
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clickable(remember { MutableInteractionSource() }, null, enabled) { onClick() }

/**
 * Relieve de botón tipo caramelo: cara con degradado, canto más oscuro abajo, brillo arriba y sombra.
 * Cuando [pressed] la cara baja hasta el canto, como un botón de verdad.
 */
fun Modifier.candy(color: Color, pressed: Boolean, radius: Dp, depth: Dp = 5.dp): Modifier = drawBehind {
    val d = depth.toPx()
    val r = minOf(radius.toPx(), (size.height - d) / 2)
    val cr = CornerRadius(r, r)
    val faceH = size.height - d
    val off = if (pressed) d * .75f else 0f
    // Botón de caricatura: cara de color plano, canto oscuro, contorno grueso y un brillo blanco nítido.
    val line = 2.5.dp.toPx()
    drawRoundRect(Ink, Offset(0f, d), Size(size.width, faceH), cr)
    drawRoundRect(colorLerp(color, Ink, .35f), Offset(line, d), Size(size.width - line * 2, faceH - line), cr)
    drawRoundRect(color, Offset(0f, off), Size(size.width, faceH), cr)
    val g = 4.dp.toPx()
    val hr = minOf(r * .6f, faceH * .12f)
    drawRoundRect(Color.White.copy(alpha = .55f), Offset(g * 2.2f, off + g), Size((size.width - g * 4.4f) * .55f, faceH * .16f), CornerRadius(hr, hr))
    drawRoundRect(Ink, Offset(0f, off), Size(size.width, faceH), cr, style = Stroke(line))
}

/** Caja que se pulsa con relieve de caramelo; el contenido baja junto con la cara del botón. */
@Composable
fun CandyBox(
    color: Color, radius: Dp, modifier: Modifier = Modifier, enabled: Boolean = true, depth: Dp = 5.dp,
    padding: PaddingValues = PaddingValues(0.dp), onClick: () -> Unit, content: @Composable BoxScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Box(
        modifier.candy(color, pressed && enabled, radius, depth).clickable(src, null, enabled) { onClick() }.padding(bottom = depth),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.offset(y = if (pressed && enabled) depth * .75f else 0.dp).padding(padding), contentAlignment = Alignment.Center) { content() }
    }
}

/** Fondo de tarjeta de caricatura: color plano, contorno grueso y sombra dura debajo. */
fun Modifier.panel(shape: androidx.compose.ui.graphics.Shape): Modifier =
    drawBehind {
        val o = shape.createOutline(size, layoutDirection, this)
        translate(0f, 5.dp.toPx()) { drawOutline(o, Color(0xCC0A0620)) }
    }.clip(shape).background(C.card).border(3.dp, Ink, shape)

@Composable
fun GameCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier.fillMaxWidth().panel(shape).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
fun Ghost(text: String, primary: Boolean = false, icon: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    CandyBox(
        if (primary) C.berry else Color(0xFF4A3BA8), 50.dp, padding = PaddingValues(horizontal = 18.dp, vertical = 10.dp), onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) icon()
            Text(text, style = body(16, Color.White, FontWeight.ExtraBold).copy(shadow = Shadow(Color(0x66000000), Offset(0f, 2f), 3f)))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Buttons(content: @Composable RowScope.() -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

const val STAR = "M12 2l3.1 6.6 7.2.9-5.3 5 1.4 7.1L12 18l-6.4 3.6L7 14.5l-5.3-5 7.2-.9z"

@Composable
fun StarsRow(n: Int, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(size / 10)) {
        repeat(3) { i ->
            Canvas(Modifier.size(size)) {
                group(s = this.size.width / 24f) {
                    if (i < n) {
                        path(STAR, Color(0xFFFFD84A), Color(0xFFB87A00), 1.4f)
                        path("M8.6 7.6l2.4-4", stroke = Color.White, sw = 1.3f, alpha = .8f)
                    } else path(STAR, Color(0xFF2E2470), C.line, 1.2f)
                }
            }
        }
    }
}

@Composable
fun PlanetBall(p: PlanetDef, size: Dp) {
    val t by rememberTime()
    Canvas(Modifier.size(size)) { planetBall(p.a, p.b, p.ring, p.key, t) }
}

@Composable
fun PlanetBall(a: Color, b: Color, ring: Boolean, size: Dp) {
    Canvas(Modifier.size(size)) { planetBall(a, b, ring) }
}

@Composable
fun ProgressDots(i: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        repeat(N) { k ->
            val c = if (k < i) C.leaf else if (k == i) C.sun else C.line
            Box(
                Modifier.widthIn(max = 56.dp).weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp))
                    .background(Brush.verticalGradient(listOf(colorLerp(c, Color.White, .35f), c, colorLerp(c, Color.Black, .25f))))
                    .border(1.dp, colorLerp(c, Color.Black, .35f), RoundedCornerShape(6.dp))
            )
        }
    }
}

@Composable
fun SpeakerIcon(color: Color = C.ink) {
    Canvas(Modifier.size(20.dp)) {
        group(s = size.width / 24f) {
            path("M3 9v6h4l5 4V5L7 9H3zm13.5 3a4.5 4.5 0 0 0-2.5-4v8a4.5 4.5 0 0 0 2.5-4zM14 3.2v2.1a7 7 0 0 1 0 13.4v2.1a9 9 0 0 0 0-17.6z", color)
        }
    }
}

@Composable
fun Hint(text: String) = Text(text, style = body(15, C.muted), textAlign = TextAlign.Center)

/** Botón de opción con color (letras o palabras). */

@Composable
fun OptTile(text: String, bg: Color, state: Int = 0, wide: Boolean = false, onClick: () -> Unit) {
    val ring = when (state) { 1 -> C.leaf; 2 -> C.bad; else -> Color.Transparent }
    Box(Modifier.border(4.dp, ring, RoundedCornerShape(19.dp)).padding(3.dp)) {
        CandyBox(
            bg, 16.dp, Modifier.height(64.dp).widthIn(min = if (wide) 118.dp else 58.dp),
            padding = PaddingValues(horizontal = 14.dp), onClick = onClick,
        ) {
            Text(if (wide) text else text.uppercase(), style = display(if (wide) 24 else 28, C.onColor).copy(shadow = Shadow(Color(0x55FFFFFF), Offset(0f, 2f), 0f)))
        }
    }
}
