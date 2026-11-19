package com.billkevin91.martypalabras

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/* Efectos de celebración: confeti, estrellas que aparecen una a una y el aterrizaje en un planeta. */

private val CONFETTI_COLORS = listOf(
    Color(0xFFFFC93C), Color(0xFFFF6B8B), Color(0xFF4CC3FF), Color(0xFF5ED38B), Color(0xFFA98BFF), Color(0xFFFF9F4A), Color.White,
)

private class Piece(
    val x: Float, val y: Float, val vx: Float, val vy: Float, val spin: Float, val wob: Float,
    val w: Float, val h: Float, val color: Color, val round: Boolean,
)

/**
 * Lluvia de confeti sobre toda la pantalla cada vez que cambia [trigger].
 * [big]: celebración grande (fin de planeta, diploma); si no, una ráfaga corta al acertar.
 */
@Composable
fun Confetti(trigger: Int, big: Boolean) {
    if (trigger == 0) return
    key(trigger) {
        var done by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(if (big) 3600 else 2200)
            done = true
        }
        if (!done) ConfettiBurst(big)
    }
}

@Composable
private fun ConfettiBurst(big: Boolean) {
    val time = rememberTime()
    val pieces = remember {
        val r = Random(System.nanoTime())
        List(if (big) 120 else 46) {
            // Las ráfagas grandes caen desde arriba; las pequeñas salen disparadas del centro.
            val fromTop = big && it % 2 == 0
            Piece(
                x = if (fromTop) r.nextFloat() else .5f + (r.nextFloat() - .5f) * .2f,
                y = if (fromTop) -.05f - r.nextFloat() * .2f else .38f,
                vx = if (fromTop) (r.nextFloat() - .5f) * .15f else (r.nextFloat() - .5f) * 1.3f,
                vy = if (fromTop) r.nextFloat() * .1f else -.5f - r.nextFloat() * .6f,
                spin = (r.nextFloat() - .5f) * 900f, wob = r.nextFloat() * 6f,
                w = 6f + r.nextFloat() * 6f, h = 9f + r.nextFloat() * 8f,
                color = CONFETTI_COLORS[r.nextInt(CONFETTI_COLORS.size)], round = r.nextInt(5) == 0,
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val t = time.value
        val life = if (big) 3.4f else 2f
        val fade = ((life - t) / .6f).coerceIn(0f, 1f)
        if (fade <= 0f) return@Canvas
        pieces.forEach { p ->
            val x = (p.x + p.vx * t + .02f * sin(t * 4f + p.wob)) * size.width
            val y = (p.y + p.vy * t + .55f * t * t) * size.height
            if (y > size.height + 20f) return@forEach
            val flip = sin(t * 6f + p.wob)
            spin(p.spin * t, x, y) {
                if (p.round) drawCircle(p.color, p.w * density * .45f, Offset(x, y), alpha = fade)
                else drawRect(
                    p.color, Offset(x - p.w * density / 2, y - p.h * density * flip / 2),
                    androidx.compose.ui.geometry.Size(p.w * density, p.h * density * kotlin.math.abs(flip).coerceAtLeast(.15f)), alpha = fade,
                )
            }
        }
    }
}

/** Estrellas de resultado que aparecen una a una con un rebote y un sonido que sube. */
@Composable
fun AnimatedStars(n: Int, size: Dp, onStar: (Int) -> Unit = {}) {
    val scales = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(n) {
        delay(250)
        for (i in 0 until 3) {
            if (i < n) onStar(i)
            scales[i].animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
            delay(120)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(size / 10)) {
        for (i in 0 until 3) {
            Canvas(Modifier.size(size).graphicsLayer {
                val s = scales[i].value
                scaleX = s; scaleY = s
                rotationZ = (1f - s) * -90f
            }) {
                group(s = this.size.width / 24f) {
                    if (i < n) {
                        circle(12f, 12f, 13f, Color(0xFFFFE36A), alpha = .25f)
                        path(STAR, Color(0xFFFFD84A), Color(0xFFB87A00), 1.4f)
                        path("M8.6 7.6l2.4-4", stroke = Color.White, sw = 1.3f, alpha = .8f)
                    } else path(STAR, Color(0xFF2B3A57), C.line, 1.2f)
                }
            }
        }
    }
}

/** Escena de llegada: la globo-nave de Marty baja desde el cielo hasta el planeta. */
fun androidx.compose.ui.graphics.drawscope.DrawScope.landing(t: Float, p: PlanetDef, tm: TextMeasurer) {
    val w = size.width
    val h = size.height
    drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF0E0A2E), Color(0xFF2B1F78))))
    for (k in 0 until 30) {
        val x = ((k * 97) % 100) / 100f * w
        val y = ((k * 53) % 70) / 100f * h
        drawCircle(Color.White, (1f + (k % 3)) * density * .6f, Offset(x, y), alpha = lerp(.3f, 1f, swing(t, 1.5f, k * .13f)))
    }
    // nebulosa del color del planeta y una luna lejana
    val moon = Offset(w * .84f + w * .02f * sin(t * .3f), h * .22f)
    drawCircle(Ink, h * .068f, moon)
    drawCircle(Color(0xFFF2EEFF), h * .06f, moon)
    drawCircle(Color(0xFF7A70B8), h * .012f, Offset(moon.x + h * .015f, moon.y + h * .01f), alpha = .7f)
    // el planeta, grande, asomando por abajo, con su atmósfera brillando en el horizonte
    val s = w * 1.05f
    val left = (w - s) / 2
    val top = h * .52f
    val pc = Offset(w / 2, top + s / 2)
    drawCircle(p.a.copy(alpha = .25f), s * .56f, pc)
    inset(left, top, w - left - s, h - top - s) { planetBall(p.a, p.b, p.ring, p.key, t) }
    // sombra de la nave sobre el planeta
    val p1 = (t / 2.2f).coerceIn(0f, 1f)
    drawOval(Color.Black.copy(alpha = .25f * p1), Offset(w / 2 - w * .07f * p1, top + h * .05f), androidx.compose.ui.geometry.Size(w * .14f * p1, h * .035f))
    // la globo-nave baja y luego flota
    val k = minOf(w / 640f, h / 360f) * .78f
    val p0 = (t / 2.2f).coerceIn(0f, 1f)
    val ease = 1f - (1f - p0) * (1f - p0) * (1f - p0)
    val y = lerp(-h * 1.1f, -h * .06f, ease) + if (p0 >= 1f) 6f * k * sin(t * 2f) else 0f
    group(w / 2 - 505f * k, y, k) { balloonShip(t, tm) }
    // polvo al llegar
    if (t in 2f..3.2f) {
        val q = (t - 2f) / 1.2f
        for (i in 0 until 8) {
            val a = i / 8f * 2 * PI.toFloat()
            drawCircle(Color.White, (6f + 14f * q) * density, Offset(w / 2 + kotlin.math.cos(a) * w * .25f * q, top + h * .06f), alpha = .35f * (1f - q))
        }
    }
}
