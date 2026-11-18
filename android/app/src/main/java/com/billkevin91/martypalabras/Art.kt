package com.billkevin91.martypalabras

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.fillMaxSize
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/* Piezas compartidas por las escenas de los planetas. */

/** Contenedor de escena con proporción fija, esquinas redondeadas y borde. */
fun Modifier.scene(ratio: Float, border: Color = Color(0xFF44399A), radius: Dp = 22.dp, width: Dp = 2.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return fillMaxWidth().aspectRatio(ratio).shadow(10.dp, shape, ambientColor = Color(0xFF7A5AE0), spotColor = Color(0xFF7A5AE0))
        .clip(shape).sceneLight().border(width, border, shape)
}

/**
 * Acabado de escena, encima de todo lo dibujado: viñeta (bordes en penumbra), luz suave desde arriba
 * y un reflejo de cristal en el borde, para que cada escena tenga profundidad y no se vea plana.
 */
fun Modifier.sceneLight(): Modifier = drawWithContent {
    drawContent()
    val w = size.width
    val h = size.height
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0x5A0A0420)), Offset(w / 2, h * .45f), maxOf(w, h) * .78f))
    drawRect(Brush.verticalGradient(0f to Color(0x24FFFFFF), .22f to Color.Transparent))
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color.Transparent, Color(0x22FFFFFF))),
        cornerRadius = CornerRadius(22.dp.toPx()), style = Stroke(3.dp.toPx()),
    )
}

/** Destellos mágicos que flotan por la escena (pequeñas estrellas de 4 puntas que se encienden y apagan). */
@Composable
fun Sparkles(color: Color = Color.White, count: Int = 14) {
    val time = rememberTime()
    val star = remember { androidx.compose.ui.graphics.Path() }
    val pts = remember { List(count) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), .6f + Random.nextFloat() * .8f, Random.nextFloat() * 6f, 3f + Random.nextFloat() * 3f) } }
    Canvas(Modifier.fillMaxSize()) {
        val t = time.value
        val u = size.width / 100f
        pts.forEach { (x, y, s, ph, per) ->
            val p = ((t + ph) % per) / per
            val a = sin(p * PI.toFloat()).let { it * it }
            if (a < .02f) return@forEach
            val cx = size.width * x
            val cy = size.height * y - u * 4 * p
            val r = u * 1.6f * s * (.5f + .5f * a)
            drawCircle(color.copy(alpha = .22f * a), r * 1.3f, Offset(cx, cy))
            // estrella de cuatro puntas con lados curvos
            val k = r * .18f
            star.reset()
            star.moveTo(cx, cy - r)
            star.quadraticBezierTo(cx + k, cy - k, cx + r, cy)
            star.quadraticBezierTo(cx + k, cy + k, cx, cy + r)
            star.quadraticBezierTo(cx - k, cy + k, cx - r, cy)
            star.quadraticBezierTo(cx - k, cy - k, cx, cy - r)
            star.close()
            drawPath(star, color.copy(alpha = a))
        }
    }
}

/** Coloca un elemento centrado en (x%, y%) de una escena de W×H. */
fun Modifier.at(W: Dp, H: Dp, xp: Float, yp: Float, w: Dp, h: Dp): Modifier =
    offset(W * (xp / 100f) - w / 2, H * (yp / 100f) - h / 2).size(w, h)

/**
 * Escena adaptable. En teléfonos (menos de 480dp de ancho) usa la proporción alta, para aprovechar
 * la pantalla vertical con elementos grandes; en pantallas anchas usa la proporción apaisada.
 */
@Composable
fun SceneBox(
    wide: Float, tall: Float, bg: Color, border: Color = Color(0xFF44399A), borderWidth: Dp = 2.dp,
    content: @Composable BoxScope.(W: Dp, H: Dp, isTall: Boolean) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val isTall = maxWidth < 480.dp
        val W = maxWidth
        val H = W / (if (isTall) tall else wide)
        val shape = RoundedCornerShape(22.dp)
        Box(
            Modifier.size(W, H).shadow(10.dp, shape, ambientColor = Color(0xFF7A5AE0), spotColor = Color(0xFF7A5AE0))
                .clip(shape).background(bg).sceneLight().border(borderWidth, border, shape)
        ) {
            content(W, H, isTall)
            Sparkles()
        }
    }
}

@Composable
fun Dp.asSp(): TextUnit = with(LocalDensity.current) { this@asSp.toSp() }

fun DrawScope.mush(x: Float, y: Float, s: Float, cap: Color, dot: Color) = group(x, y, s) {
    rect(-9f, -46f, 18f, 46f, 8f, Color(0xFFE9DCC3))
    path("M-40 -42Q-36 -92 0 -92Q36 -92 40 -42Q0 -30 -40 -42Z", cap)
    circle(-18f, -66f, 6f, dot); circle(8f, -76f, 7f, dot); circle(24f, -58f, 5f, dot)
    path("M-30 -40Q0 -30 30 -40", stroke = Color.White.copy(alpha = .35f), sw = 3f)
}

fun DrawScope.crystal(x: Float, y: Float, k: Float, a: Color, b: Color, t: Float, d: Float) = group(x, y, k) {
    val al = lerp(.75f, 1f, swing(t, 3f, d))
    path("M-10 0L-6 -50L4 -62L14 -46L10 0Z", a, alpha = al)
    path("M10 0L14 -46L28 -30L22 0Z", b, alpha = al)
    path("M-10 0L-6 -50L-22 -26L-20 0Z", b, alpha = .8f * al)
    path("M-2 -52L4 -60L10 -46", stroke = Color.White, sw = 3f, alpha = .7f)
}

/** Luciérnagas que flotan y brillan (posiciones en % de la escena). */
class Fireflies(n: Int, yMin: Float, yMax: Float, val color: Color, seed: Int = Random.nextInt()) {
    private val r = Random(seed)
    val items = List(n) {
        floatArrayOf(4 + r.nextFloat() * 92, yMin + r.nextFloat() * (yMax - yMin), -26 + r.nextFloat() * 52, -26 + r.nextFloat() * 52, 4 + r.nextFloat() * 4, r.nextFloat() * 6)
    }

    fun DrawScope.drawFlies(t: Float) {
        val u = size.width / 100f
        items.forEach { (x, y, dx, dy, per, ph) ->
            val p = swing(t + ph, per)
            val ox = if (p < .5f) lerp(0f, dx, p * 2) else lerp(dx, -.4f * dx, (p - .5f) * 2)
            val oy = if (p < .5f) lerp(0f, -.5f * dy, p * 2) else lerp(-.5f * dy, dy, (p - .5f) * 2)
            val a = lerp(.25f, 1f, swing(t + ph, .9f))
            val cx = size.width * x / 100f + ox * density * .6f
            val cy = size.height * y / 100f + oy * density * .6f
            circle(cx, cy, u * 1.6f, color, alpha = a * .25f)
            circle(cx, cy, u * .45f, color, alpha = a)
        }
    }
}

private operator fun FloatArray.component6() = this[5]
