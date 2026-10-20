package com.billkevin91.martypalabras

import androidx.compose.foundation.border
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
    return fillMaxWidth().aspectRatio(ratio).clip(shape).border(width, border, shape)
}

/** Coloca un elemento centrado en (x%, y%) de una escena de W×H. */
fun Modifier.at(W: Dp, H: Dp, xp: Float, yp: Float, w: Dp, h: Dp): Modifier =
    offset(W * (xp / 100f) - w / 2, H * (yp / 100f) - h / 2).size(w, h)

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
