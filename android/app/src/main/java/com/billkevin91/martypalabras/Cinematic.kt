package com.billkevin91.martypalabras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * Escenas animadas (como pequeñas películas), dibujadas con el mismo estilo del juego:
 * la intro, el viaje entre planetas, la llegada de Borrón, Marty persiguiendo a Borrón y el gran final.
 * Marty y Borrón hablan con sus voces grabadas; siempre se pueden saltar.
 */

enum class Escena { INTRO, VIAJE, BORRON_LLEGA, BORRON_HUYE, FINAL }

private const val VW = 640f
private const val VH = 400f

private fun at(s: Float, f: () -> Unit): Pair<Float, () -> Unit> = s to f

/** Duración en segundos y lo que pasa en cada momento (frases y sonidos). */
private fun plan(g: Game, c: Screen.Cine): Pair<Float, List<Pair<Float, () -> Unit>>> = when (c.kind) {
    Escena.INTRO -> 15f to listOf(
        at(.1f) { g.sfx(Sfx.WHOOSH) },
        at(.6f) { g.say(Frases.CINE_INTRO_1) },
        at(5f) { g.react(Mood.HAPPY); g.say(Frases.CINE_INTRO_2) },
        at(8.6f) { g.sfx(Sfx.BOING) },
        at(9.8f) { g.say(Frases.CINE_INTRO_3); g.celebrate(false) },
    )
    Escena.VIAJE -> 6.5f to listOf(
        at(.1f) { g.sfx(Sfx.WHOOSH) },
        at(.4f) { g.react(Mood.HAPPY); g.say(Frases.rumbo(c.arg)) },
    )
    Escena.BORRON_LLEGA -> 7f to listOf(
        at(.1f) { g.tone(300, 240, 300, 240) },
        at(1.9f) { g.sfx(Sfx.BOING) },
        at(2.6f) { g.react(Mood.HAPPY); g.say(Frases.CINE_BORRON_LLEGA) },
    )
    Escena.BORRON_HUYE -> 10f to listOf(
        at(.3f) { g.react(Mood.NONE); g.say(Frases.CINE_HUYE_1) },
        at(3.1f) { g.react(Mood.SAD); g.say(Frases.CINE_HUYE_2) },
        at(3.4f) { g.sfx(Sfx.WHOOSH) },
        at(6.9f) { g.react(Mood.HAPPY); g.say(Frases.CINE_HUYE_3); g.celebrate(true) },
    )
    Escena.FINAL -> 9f to listOf(
        at(.3f) { g.react(Mood.HAPPY); g.say(Frases.CINE_FINAL); g.celebrate(true) },
        at(2f) { g.tone(523, 659, 784, 1047) },
        at(5f) { g.celebrate(true) },
    )
}

@Composable
fun CineScreen(g: Game, c: Screen.Cine) {
    val tm = rememberTextMeasurer()
    val time = rememberTime()
    LaunchedEffect(c) {
        val (dur, cues) = plan(g, c)
        var now = 0f
        for ((at, act) in cues) {
            delay(((at - now) * 1000).toLong().coerceAtLeast(0))
            now = at
            act()
        }
        delay(((dur - now) * 1000).toLong().coerceAtLeast(0))
        g.cineDone()
    }
    GameCard {
        Box(Modifier.scene(VW / VH, Ink, 22.dp, 3.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val t = time.value
                viewBox(VW, VH) {
                    when (c.kind) {
                        Escena.INTRO -> introScene(t, tm)
                        Escena.VIAJE -> travelScene(t, tm, planet(c.arg.ifEmpty { RETO.first() }))
                        Escena.BORRON_LLEGA -> borronArrives(t)
                        Escena.BORRON_HUYE -> borronFlees(t)
                        Escena.FINAL -> finale(t)
                    }
                }
            }
        }
        Buttons { Ghost("Saltar ›") { g.cineDone() } }
    }
}

/* ---------- Piezas comunes ---------- */

private fun ease(p: Float): Float {
    val q = p.coerceIn(0f, 1f)
    return 1f - (1f - q) * (1f - q) * (1f - q)
}

private fun phase(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)

/** Fondo de espacio con estrellas; con [speed] > 0 las estrellas se estiran como en un viaje rápido. */
private fun DrawScope.spaceBg(t: Float, speed: Float, top: Color = Color(0xFF0B0828), bottom: Color = Color(0xFF2B1F78)) {
    rect(0f, 0f, VW, VH, brush = vGrad(0f, VH, 0f to top, 1f to bottom))
    for (k in 0 until 46) {
        val depth = .3f + (k % 5) * .17f
        val x0 = (k * 137f) % VW
        val y = (k * 89f) % VH
        val x = ((x0 - t * speed * depth * 260f) % VW + VW) % VW
        val len = speed * depth * 46f
        val a = lerp(.35f, 1f, swing(t, 1.3f, k * .11f))
        if (len > 2f) line(x, y, x + len, y, Color.White, 1.2f + depth * 1.4f, a * .8f)
        else circle(x, y, .8f + depth * 1.6f, Color.White, alpha = a)
    }
}

/** Escenario del concurso: piso, luces de colores y reflectores que se mueven. */
private fun DrawScope.stage(t: Float, alpha: Float = 1f) {
    if (alpha <= 0f) return
    for ((i, x) in listOf(120f, 520f).withIndex()) {
        val ang = 18f * sin(t * .9f + i * 2f)
        spin(ang, x, -20f) {
            path("M${x - 26} -20L${x + 26} -20L${x + 110} 400L${x - 110} 400Z", brush = vGrad(-20f, 400f, 0f to Color(0x55FFF6D8), 1f to Color.Transparent), alpha = alpha)
        }
    }
    ellipse(320f, 372f, 300f, 48f, Color(0xFF4A2FA0), Ink, 4f, alpha = alpha)
    ellipse(320f, 366f, 280f, 36f, Color(0xFF5B3FC0), alpha = alpha)
    for (i in 0 until 15) {
        val a = PI.toFloat() * (.06f + i * .063f)
        val x = 320f - cos(a) * 290f
        val y = 372f + sin(a) * 44f
        val on = ((t / .7f + (i % 2) * .5f) % 1f) < .5f
        circle(x, y, 5f, if (i % 2 == 1) Color(0xFFFFE36A) else Color(0xFFFF7FC2), Ink, 1.5f, alpha = alpha * if (on) 1f else .35f)
    }
}

/** Marty de cuerpo entero con los pies en (fx, fy). */
private fun DrawScope.martyAt(t: Float, fx: Float, fy: Float, k: Float, rot: Float = 0f, squash: Float = 1f) {
    zoom(1f + (1f - squash) * .6f, squash, fx, fy) {
        spin(rot, fx, fy) { group(fx - 60f * k, fy - 214f * k, k) { martyFull(t) } }
    }
}

/** Borrón con la base en (bx, by). */
private fun DrawScope.borronAt(t: Float, bx: Float, by: Float, k: Float, mood: Mood, talking: Boolean = false) =
    group(bx - 50f * k, by - 92f * k, k) { borron(t, mood, talking) }

/** La nave de Marty centrada en (cx, cy). */
private fun DrawScope.shipAt(t: Float, tm: TextMeasurer, cx: Float, cy: Float, k: Float, rot: Float = 0f) =
    spin(rot, cx, cy) { group(cx - 505f * k, cy - 160f * k, k) { balloonShip(t, tm) } }

/** Nubecitas de polvo detrás de alguien que corre. */
private fun DrawScope.dust(t: Float, x: Float, y: Float, dir: Float) {
    for (i in 0 until 4) {
        val q = ((t * 2.2f + i * .25f) % 1f)
        circle(x - dir * q * 60f, y - q * 16f, 6f + q * 14f, Color.White, alpha = .5f * (1f - q))
    }
}

/** Un fuego artificial que estalla en (x, y) a los [at] segundos. */
private fun DrawScope.firework(t: Float, at: Float, x: Float, y: Float, col: Color) {
    val dt = t - at
    if (dt < 0f) return
    if (dt < .5f) { // el cohete sube
        val q = dt / .5f
        circle(x, VH - (VH - y) * ease(q), 3f, Color(0xFFFFF3B0))
        return
    }
    val q = (dt - .5f) / 1.4f
    if (q > 1f) return
    for (i in 0 until 16) {
        val a = i * 2f * PI.toFloat() / 16
        val r = ease(q) * 80f
        val px = x + cos(a) * r
        val py = y + sin(a) * r + q * q * 30f
        line(px - cos(a) * 10f, py - sin(a) * 10f, px, py, col, 3f, 1f - q)
        circle(px, py, 2.6f, Color.White, alpha = 1f - q)
    }
}

/* ---------- Intro: la nave llega, se enciende el escenario y Marty salta a saludar ---------- */

private fun DrawScope.introScene(t: Float, tm: TextMeasurer) {
    val warp = 1f - phase(t, 3.5f, 5.5f)
    spaceBg(t, warp * 4f + .1f)
    val lights = phase(t, 5f, 6.5f)
    stage(t, lights)
    // Telón: dos cortinas que se abren al encenderse el escenario.
    val open = ease(phase(t, 5.2f, 7f))
    if (open < 1f) {
        rect(-10f, 0f, 330f * (1f - open) + 10f, VH, brush = vGrad(0f, VH, 0f to Color(0xFFC23A64), 1f to Color(0xFF7A1A3C)), alpha = lights)
        rect(320f + 320f * open, 0f, 330f, VH, brush = vGrad(0f, VH, 0f to Color(0xFFC23A64), 1f to Color(0xFF7A1A3C)), alpha = lights)
    }
    // La nave entra a toda velocidad y luego se estaciona arriba a la derecha.
    val enter = ease(phase(t, 0f, 4.5f))
    val park = ease(phase(t, 6.5f, 8.5f))
    val sx = lerp(lerp(-200f, 320f, enter), 500f, park)
    val sy = lerp(lerp(260f, 190f, enter), 110f, park)
    val sk = lerp(.5f, .32f, park)
    shipAt(t, tm, sx, sy, sk, rot = lerp(12f, 0f, enter))
    // Marty salta desde la nave al escenario.
    if (t >= 8.6f) {
        val j = phase(t, 8.6f, 9.8f)
        val fx = lerp(500f, 320f, j)
        val fy = lerp(150f, 368f, j) - sin(j * PI.toFloat()) * 120f
        val land = phase(t, 9.8f, 10.3f)
        val squash = if (j >= 1f) 1f - .18f * sin(land * PI.toFloat()) else 1f
        martyAt(t, fx, fy, lerp(.5f, 1f, j), rot = if (j < 1f) j * 360f else 0f, squash = squash)
        if (land in .01f..0.99f) for (i in 0 until 8) {
            val a = i / 8f * 2f * PI.toFloat()
            circle(320f + cos(a) * 90f * land, 368f + sin(a) * 14f * land, 10f, Color.White, alpha = .5f * (1f - land))
        }
    }
    // Destellos al final.
    if (t > 10f) for (i in 0 until 10) {
        val q = ((t - 10f) * .7f + i * .1f) % 1f
        val x = 80f + (i * 53f) % 480f
        circle(x, 60f + q * 120f, 3f, if (i % 2 == 0) Color(0xFFFFE36A) else Color(0xFF7DFFE3), alpha = 1f - q)
    }
}

/* ---------- Viaje: la nave cruza el espacio hacia el siguiente planeta ---------- */

private fun DrawScope.travelScene(t: Float, tm: TextMeasurer, p: PlanetDef) {
    val slow = phase(t, 4f, 6.2f)
    spaceBg(t, lerp(5f, .4f, slow))
    // El planeta de destino crece desde la derecha.
    val g = ease(phase(t, .5f, 6.2f))
    val pr = lerp(20f, 150f, g)
    val px = lerp(700f, 470f, g)
    val py = lerp(150f, 230f, g)
    // inset deja un lienzo cuadrado de lado 2·pr, y planetBall se dibuja centrado en él.
    inset(px - pr, py - pr, size.width - px - pr, size.height - py - pr) {
        planetBall(p.a, p.b, p.ring, p.key, t)
    }
    // Estela de humo detrás de la nave.
    val sx = lerp(220f, 260f, slow) + 8f * sin(t * 2f)
    val sy = 190f + 14f * sin(t * 1.6f)
    for (i in 0 until 10) {
        val q = ((t * 1.6f + i * .1f) % 1f)
        circle(sx - 70f - q * 220f, sy + 40f + 10f * sin(q * 9f), 8f + q * 18f, Color.White, alpha = .35f * (1f - q))
    }
    shipAt(t, tm, sx, sy, .42f, rot = lerp(-6f, 0f, slow) + 3f * sin(t * 2.4f))
}

/* ---------- Borrón llega: alarma, cae una gota de tinta y Borrón sale de la mancha ---------- */

private fun DrawScope.borronArrives(t: Float) {
    val shake = if (t < 2.4f) 6f * sin(t * 60f) * (1f - phase(t, 1.8f, 2.4f)) else 0f
    group(shake, 0f) {
        rect(0f, 0f, VW, VH, brush = vGrad(0f, VH, 0f to Color(0xFF120A2E), 1f to Color(0xFF2A1458)))
        // Estantes de una biblioteca, con libros.
        for (row in 0 until 3) {
            val y = 70f + row * 90f
            rect(30f, y + 52f, 580f, 10f, 3f, Color(0xFF6D4420), Ink, 2f)
            for (b in 0 until 14) {
                val h = 40f + (b * 7 + row * 3) % 14
                val col = listOf(Color(0xFFFF6B8B), Color(0xFF4CC3FF), Color(0xFFFFC93C), Color(0xFF5ED38B), Color(0xFFA98BFF))[(b + row) % 5]
                rect(44f + b * 40f, y + 52f - h, 30f, h, 3f, col, Ink, 2f, alpha = .8f)
            }
        }
        // Luz roja de alarma.
        if (t < 2.4f) rect(0f, 0f, VW, VH, fill = Color(0xFFFF2244), alpha = .22f * swing(t, .25f))
        // La gota de tinta cae.
        val fall = phase(t, .8f, 1.9f)
        if (t in .8f..1.9f) {
            val y = lerp(-40f, 330f, fall * fall)
            path("M320 ${y - 30}Q340 ${y} 320 ${y + 16}Q300 ${y} 320 ${y - 30}Z", Color(0xFF3A2B94), Ink, 2.5f)
        }
        // La mancha se expande.
        if (t >= 1.9f) {
            val sp = ease(phase(t, 1.9f, 2.4f))
            ellipse(320f, 344f, 150f * sp, 26f * sp, Color(0xFF3A2B94), alpha = .95f)
            for (i in 0 until 9) {
                val a = PI.toFloat() * (1f + i / 8f)
                val q = phase(t, 1.9f, 2.8f)
                circle(320f + cos(a) * 170f * q, 344f + sin(a) * 120f * q + q * q * 120f, 7f, Color(0xFF5B49C9), alpha = 1f - q)
            }
            // Borrón crece desde la mancha con un rebote.
            val gr = phase(t, 2.2f, 3f)
            val k = if (gr < 1f) 1.6f * ease(gr) * (1f + .25f * sin(gr * PI.toFloat())) else 1.6f
            if (k > .05f) borronAt(t, 320f, 346f, k, Mood.HAPPY, talking = t in 2.6f..6f)
        }
        // Gotas que escurren por los bordes de la pantalla.
        for (i in 0 until 6) {
            val x = 40f + i * 112f
            val q = phase(t, 2.3f + i * .12f, 6f)
            if (q > 0f) {
                rect(x - 6f, -4f, 12f, 30f + q * 90f, 6f, Color(0xFF3A2B94))
                circle(x, 26f + q * 90f, 9f, Color(0xFF3A2B94))
            }
        }
    }
}

/* ---------- Marty asusta a Borrón con un borrador gigante y lo persigue ---------- */

private fun DrawScope.eraser(x: Float, y: Float, rot: Float) = spin(rot, x, y) {
    rect(x - 30f, y - 70f, 60f, 110f, 12f, Color(0xFFFF9DB4), Ink, 3f)
    rect(x - 30f, y - 10f, 60f, 36f, 0f, Color(0xFF4CC3FF), Ink, 3f)
    path("M${x - 18} ${y - 58}q8-6 16 0", stroke = Color.White, sw = 3f, alpha = .7f)
}

private fun DrawScope.borronFlees(t: Float) {
    spaceBg(t, .1f)
    stage(t)
    // Borrón: primero se ríe, luego tiembla de miedo y sale huyendo a saltos.
    val flee = phase(t, 3.2f, 6.2f)
    val scared = t >= 2.6f
    val trem = if (scared && flee <= 0f) 3f * sin(t * 50f) else 0f
    val bx = lerp(400f, 780f, flee * flee) + trem
    val hop = if (flee > 0f) abs(sin(t * 9f)) * 30f else 0f
    if (bx < 720f) {
        borronAt(t, bx, 368f - hop, 1.5f, if (scared) Mood.SAD else Mood.HAPPY, talking = t in 3.1f..5.5f)
        if (scared) for (i in 0 until 3) { // gotas de sudor
            val q = ((t * 1.5f + i * .33f) % 1f)
            ellipse(bx + 60f + i * 6f, 250f - hop + q * 30f, 4f, 6f, Color(0xFF8FDCFF), alpha = 1f - q)
        }
        if (flee > 0f) for (i in 0 until 5) { // va dejando manchitas
            val q = ((t * 2f + i * .2f) % 1f)
            ellipse(bx - 40f - q * 120f, 372f, 10f * (1f - q), 4f * (1f - q), Color(0xFF3A2B94), alpha = .8f)
        }
    }
    // Marty entra corriendo con el borrador, lo levanta, persigue a Borrón y vuelve a saludar.
    val runIn = ease(phase(t, 0f, 1.8f))
    val chase = phase(t, 3.4f, 6.4f)
    val back = ease(phase(t, 6.4f, 7.8f))
    val mx = when {
        t < 3.4f -> lerp(-120f, 210f, runIn)
        t < 6.4f -> lerp(210f, 760f, chase * chase)
        else -> lerp(760f, 320f, back)
    }
    val running = (t < 1.8f) || (t in 3.4f..7.8f)
    val bounce = if (running) abs(sin(t * 12f)) * 14f else 0f
    val tilt = if (running) (if (t >= 6.4f) -10f else 10f) else 0f
    if (mx < 720f) {
        if (running) dust(t, mx, 368f, if (t >= 6.4f) -1f else 1f)
        martyAt(t, mx, 368f - bounce, .95f, rot = tilt)
        // El borrador: lo lleva en alto y lo sacude cuando asusta a Borrón.
        val raise = if (t in 1.8f..3.4f) 1f else .4f
        val wobble = if (t in 2f..3.4f) 14f * sin(t * 18f) else 0f
        if (t < 6.4f) eraser(mx + 60f, 368f - bounce - 150f - raise * 40f, 20f + wobble)
    }
    // Final: escenario limpio y brillante.
    if (t > 7.6f) for (i in 0 until 12) {
        val q = ((t - 7.6f) * .8f + i * .083f) % 1f
        val a = i * 30f * PI.toFloat() / 180f
        circle(320f + cos(a) * (60f + q * 160f), 250f + sin(a) * (40f + q * 90f), 3.5f, Color(0xFFFFE36A), alpha = 1f - q)
    }
}

/* ---------- Gran final: fuegos artificiales y Marty con el diploma ---------- */

private fun DrawScope.finale(t: Float) {
    spaceBg(t, .05f, Color(0xFF07051C), Color(0xFF2B1F78))
    listOf(
        Triple(.4f, Offset(160f, 110f), Color(0xFFFF6B8B)), Triple(1.1f, Offset(470f, 90f), Color(0xFF4CC3FF)),
        Triple(1.8f, Offset(320f, 70f), Color(0xFFFFC93C)), Triple(2.8f, Offset(110f, 80f), Color(0xFF5ED38B)),
        Triple(3.4f, Offset(520f, 130f), Color(0xFFA98BFF)), Triple(4.4f, Offset(300f, 110f), Color(0xFFFF9F4A)),
        Triple(5.2f, Offset(190f, 70f), Color(0xFF7DFFE3)), Triple(6f, Offset(450f, 80f), Color(0xFFFF7FC2)),
    ).forEach { (at, o, col) -> firework(t, at, o.x, o.y, col) }
    stage(t)
    val jump = abs(sin(t * 3.2f)) * 16f
    martyAt(t, 320f, 368f - jump, 1f)
    // El diploma enrollado, en alto, con su listón.
    val dx = 395f
    val dy = 150f - jump + 6f * sin(t * 4f)
    spin(-15f, dx, dy) {
        rect(dx - 14f, dy - 40f, 28f, 80f, 12f, Color(0xFFFFF4CF), Ink, 3f)
        rect(dx - 15f, dy - 6f, 30f, 12f, 0f, Color(0xFFFF6B8B), Ink, 2f)
        path("M${dx} ${dy + 6}l-8 22l8-6l8 6z", Color(0xFFFF6B8B), Ink, 2f)
    }
}
