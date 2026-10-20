package com.billkevin91.martypalabras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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

@Composable
fun GameCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(C.card).border(2.dp, C.line, shape).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun Ghost(text: String, primary: Boolean = false, icon: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.clip(shape).background(if (primary) C.berry else C.space2)
            .border(2.dp, if (primary) C.berry else C.line, shape)
            .tap(onClick = onClick).padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) icon()
        Text(text, style = body(16, if (primary) Color.White else C.ink, FontWeight.ExtraBold))
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
            Canvas(Modifier.size(size)) { group(s = this.size.width / 24f) { path(STAR, if (i < n) C.sun else C.line) } }
        }
    }
}

fun DrawScope.planetBall(a: Color, b: Color, ring: Boolean) {
    val r = size.minDimension / 2
    val c = Offset(size.width / 2, size.height / 2)
    drawCircle(
        androidx.compose.ui.graphics.Brush.radialGradient(
            listOf(a, b), center = Offset(size.width * .32f, size.height * .28f), radius = r * 1.5f,
        ), r, c,
    )
    if (ring) spin(-18f, c.x, c.y) {
        ellipse(c.x, c.y, r * 1.6f, r * .2f, stroke = a, sw = r * .1f, alpha = .9f)
    }
}

@Composable
fun PlanetBall(p: PlanetDef, size: Dp) = PlanetBall(p.a, p.b, p.ring, size)

@Composable
fun PlanetBall(a: Color, b: Color, ring: Boolean, size: Dp) {
    Canvas(Modifier.size(size)) { planetBall(a, b, ring) }
}

@Composable
fun ProgressDots(i: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        repeat(N) { k ->
            Box(
                Modifier.widthIn(max = 56.dp).weight(1f).height(10.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (k < i) C.leaf else if (k == i) C.sun else C.line)
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
    val shape = RoundedCornerShape(16.dp)
    var m = Modifier.clip(shape).background(bg)
    if (state == 1) m = m.border(4.dp, C.leaf, shape)
    if (state == 2) m = m.border(4.dp, C.bad, shape)
    Box(
        m.tap(onClick = onClick).height(58.dp).widthIn(min = if (wide) 118.dp else 54.dp).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (wide) text else text.uppercase(), style = display(if (wide) 24 else 28, C.onColor))
    }
}
