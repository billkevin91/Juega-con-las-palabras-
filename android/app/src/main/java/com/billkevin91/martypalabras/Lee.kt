package com.billkevin91.martypalabras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* Lectura: festival de barriletes (cometas gigantes, como los de Sumpango). La palabra correcta vuela. */

private const val TALL_FEST = 800f

private val KITE_PALETTES = listOf(
    listOf(Color(0xFFFF4F9A), Color(0xFFFFE36A), Color(0xFF34D1E0), Color(0xFF7EE06A)),
    listOf(Color(0xFFFF8A3C), Color(0xFFA06BFF), Color(0xFFFF7FC2), Color(0xFF2EC4A6)),
    listOf(Color(0xFFE8443A), Color(0xFFFFC93C), Color(0xFF3F7CFF), Color(0xFFB4F05A)),
)

/** Posición (x, y) y radio de cada barrilete, en unidades del dibujo. */
private fun kiteSpots(n: Int, tall: Boolean): List<Triple<Float, Float, Float>> = when {
    tall && n == 2 -> listOf(Triple(190f, 380f, 125f), Triple(450f, 560f, 125f))
    tall -> listOf(Triple(170f, 330f, 108f), Triple(468f, 455f, 108f), Triple(190f, 625f, 104f))
    n == 2 -> listOf(Triple(200f, 285f, 96f), Triple(440f, 285f, 96f))
    else -> listOf(Triple(112f, 300f, 88f), Triple(320f, 262f, 88f), Triple(528f, 300f, 88f))
}

@Composable
fun LeeGame(g: Game, s: Session) {
    val opts = remember {
        val others = WORDS.filter { it.text != s.word.text }.shuffled().map { it.text }
        when (s.level) {
            Level.FACIL -> (listOf(s.word.text) + others.take(1)).shuffled()
            Level.NORMAL -> (listOf(s.word.text) + others.take(2)).shuffled()
            // Difícil: las otras opciones son la misma palabra mal escrita. ¡Hay que leer con cuidado!
            Level.DIFICIL -> (listOf(s.word.text) + (s.word.wrongs.shuffled() + others).take(2)).shuffled()
        }
    }
    var winner by remember { mutableStateOf(-1) }
    var winAt by remember { mutableFloatStateOf(-9f) }
    val shakes = remember { mutableStateMapOf<Int, Float>() }
    val time = rememberTime()

    LaunchedEffect(Unit) { g.say(Frases.LEE_INICIO) }

    fun choose(i: Int) {
        if (s.locked) return
        if (opts[i] == s.word.text) {
            winner = i
            winAt = time.value
            g.sfx(Sfx.WHOOSH)
            g.win(!s.tried)
        } else {
            shakes[i] = time.value
            g.wrong()
        }
    }

    QFrame(g, s, hear = true) {
        SceneBox(4f / 3f, 640f / TALL_FEST, Color(0xFFFF7E5F)) { W, H, tall ->
            val px = W / 640f
            val vh = if (tall) TALL_FEST else 480f
            val spots = kiteSpots(opts.size, tall)
            val sun = if (tall) Triple(320f, 128f, 70f) else Triple(320f, 92f, 56f)
            Canvas(Modifier.fillMaxSize()) {
                val t = time.value
                viewBox(640f, vh) {
                    festival(t, vh, sun)
                    // hilos de los barriletes hasta el suelo
                    spots.forEachIndexed { i, (x, y, r) ->
                        val fly = if (winner == i) ((t - winAt) / 1f).coerceIn(0f, 1f) else 0f
                        val ky = y + lerp(-6f, 6f, swing(t, 2.4f, i * .3f)) - 90f * fly
                        path("M$x ${ky + r * .9f}Q${x + 40f} ${(ky + vh) / 2} ${x + 20f} ${vh - 30f}", stroke = Color(0xFFFFF4E0), sw = 1.6f, alpha = .8f)
                    }
                }
            }
            Box(Modifier.offset(px * (sun.first - sun.third), px * (sun.second - sun.third)).size(px * sun.third * 2), contentAlignment = Alignment.Center) {
                Text(s.word.pic, style = TextStyle(fontSize = (px * sun.third * .95f).asSp()))
            }
            opts.forEachIndexed { i, word ->
                val (x, y, r) = spots[i]
                val pal = KITE_PALETTES[i % KITE_PALETTES.size]
                Box(
                    Modifier.offset(px * (x - r), px * (y - r)).size(px * r * 2).graphicsLayer {
                        val t = time.value
                        val k = W.toPx() / 640f
                        transformOrigin = TransformOrigin(.5f, .9f)
                        translationY = lerp(-6f, 6f, swing(t, 2.4f, i * .3f)) * k
                        rotationZ = lerp(-4f, 4f, swing(t, 3.1f, i * .4f))
                        if (winner == i) {
                            val p = ((t - winAt) / 1f).coerceIn(0f, 1f)
                            translationY -= 90f * k * p
                            val sc = 1f + .12f * sin(PI.toFloat() * p)
                            scaleX = sc; scaleY = sc
                        }
                        val sh = t - (shakes[i] ?: -9f)
                        if (sh in 0f..0.8f) {
                            val f = sin(sh / .8f * PI.toFloat())
                            rotationZ += 28f * f
                            translationY += 24f * k * f
                        }
                    }.tap(enabled = !s.locked) { choose(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val t = time.value
                        val glow = if (winner == i) ((t - winAt) / .4f).coerceIn(0f, 1f) else 0f
                        group(size.width / 2, size.height / 2, size.width / 200f) { barrilete(pal, t + i, glow) }
                    }
                    val fs = min(r * .3f, r * 1.75f / (word.length * .62f))
                    val shape = RoundedCornerShape(px * 12)
                    Box(
                        Modifier.clip(shape).background(Color(0xFFFFFDF2)).border(px * 3, Ink, shape).padding(horizontal = px * 10, vertical = px * 2),
                    ) {
                        Text(
                            word, textAlign = TextAlign.Center, maxLines = 1,
                            style = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (px * fs).asSp(), color = Color(0xFF3A1F7A)),
                        )
                    }
                }
            }
        }
    }
}

/** Barrilete de papel de china (centro 0,0; radio 100). [glow] de 0 a 1 cuando es el correcto. */
private fun DrawScope.barrilete(pal: List<Color>, t: Float, glow: Float) {
    if (glow > 0f) circle(0f, 0f, 120f, Color(0xFFFFF36A), alpha = .45f * glow)
    // cola con moños
    for (k in 0 until 3) {
        val y = 100f + k * 20f
        val x = 10f * sin(t * 3f + k)
        path("M${x - 9} ${y - 6}L${x + 9} ${y + 6}M${x - 9} ${y + 6}L${x + 9} ${y - 6}", stroke = pal[k % pal.size], sw = 6f)
    }
    // flecos alrededor
    val sides = 16
    for (k in 0 until sides * 2) {
        val a = k * PI.toFloat() / sides
        val a2 = (k + 1) * PI.toFloat() / sides
        val am = (a + a2) / 2
        val wob = 4f * sin(t * 5f + k)
        path(
            "M${cos(a) * 92} ${sin(a) * 92}L${cos(am) * (108 + wob)} ${sin(am) * (108 + wob)}L${cos(a2) * 92} ${sin(a2) * 92}Z",
            pal[k % pal.size],
        )
    }
    // cuerpo: gajos de colores
    for (k in 0 until sides) {
        val a = k * 2 * PI.toFloat() / sides
        val a2 = (k + 1) * 2 * PI.toFloat() / sides
        path("M0 0L${cos(a) * 94} ${sin(a) * 94}L${cos(a2) * 94} ${sin(a2) * 94}Z", pal[k % pal.size])
    }
    circle(0f, 0f, 94f, stroke = Ink, sw = 4f)
    circle(0f, 0f, 66f, stroke = Color.White, sw = 5f, alpha = .85f)
    // estrella central
    for (k in 0 until 8) {
        val a = k * PI.toFloat() / 4
        path("M0 0L${cos(a - .2f) * 46} ${sin(a - .2f) * 46}L${cos(a) * 60} ${sin(a) * 60}L${cos(a + .2f) * 46} ${sin(a + .2f) * 46}Z", if (k % 2 == 0) Color.White else pal[1])
    }
    circle(0f, 0f, 24f, pal[0], Ink, 3f)
    // varillas
    line(-94f, 0f, 94f, 0f, Color(0xFF8A5A2E), 2f, .5f)
    line(0f, -94f, 0f, 94f, Color(0xFF8A5A2E), 2f, .5f)
}

private fun DrawScope.festival(t: Float, vh: Float, sun: Triple<Float, Float, Float>) {
    val ground = vh - 120f
    rect(0f, 0f, 640f, vh, brush = vGrad(0f, ground, 0f to Color(0xFF3B2A8F), .45f to Color(0xFFFF7E5F), .85f to Color(0xFFFFC371), 1f to Color(0xFFFFE0A0)))
    // sol con rayos (aquí va el dibujo)
    val (sx, sy, sr) = sun
    spin(t * 12f, sx, sy) {
        for (k in 0 until 12) spin(k * 30f, sx, sy) { path("M${sx - 10} ${sy - sr - 6}L$sx ${sy - sr - 30}L${sx + 10} ${sy - sr - 6}Z", Color(0xFFFFE36A)) }
    }
    circle(sx, sy, sr + 6f, Color(0xFFFFC93C), Ink, 3f)
    circle(sx, sy, sr, Color(0xFFFFFAE0))
    // pájaros
    for (k in 0 until 3) {
        val bx = ((t * 18f + k * 230f) % 760f) - 60f
        val by = 70f + k * 40f + 6f * sin(t * 2f + k)
        val f = 6f * sin(t * 8f + k)
        path("M${bx - 10} ${by - f}Q${bx - 5} ${by - 4} $bx ${by}Q${bx + 5} ${by - 4} ${bx + 10} ${by - f}", stroke = Color(0xFF3B2A6F), sw = 2.2f)
    }
    // volcanes
    path("M-20 ${ground}L120 ${ground - 170}Q140 ${ground - 182} 160 ${ground - 170}L320 ${ground}Z", Color(0xFF5B4A9F))
    path("M260 ${ground}L440 ${ground - 210}Q460 ${ground - 222} 480 ${ground - 210}L680 ${ground}Z", Color(0xFF6C5BB5))
    path("M430 ${ground - 206}Q450 ${ground - 214} 490 ${ground - 206}L470 ${ground - 190}Z", Color.White, alpha = .8f)
    for (k in 0 until 3) {
        val p = ((t / 5f + k / 3f) % 1f)
        circle(150f - 20f * p, ground - 178f - 60f * p, 10f + 18f * p, Color(0xFFEDE6FF), alpha = .6f * (1f - p))
    }
    // pueblo con casas de colores e iglesia
    val houses = listOf(Color(0xFFFF6B8B), Color(0xFF4CC3FF), Color(0xFFFFE36A), Color(0xFF7EE06A), Color(0xFFA98BFF), Color(0xFFFF9A3C))
    for (k in 0 until 9) {
        val x = k * 74f - 10f
        val h = 34f + (k * 37 % 5) * 6f
        rect(x, ground - h, 60f, h + 4f, 0f, houses[k % houses.size], Ink, 2f)
        path("M${x - 4} ${ground - h}L${x + 30} ${ground - h - 18}L${x + 64} ${ground - h}Z", Color(0xFFB84A2E), Ink, 2f)
        rect(x + 22f, ground - 18f, 14f, 18f, 3f, Color(0xFF5A3A1A))
    }
    val cx = 320f
    rect(cx - 46f, ground - 96f, 92f, 100f, 0f, Color(0xFFFFE07A), Ink, 3f)
    path("M${cx - 50} ${ground - 96}Q$cx ${ground - 140} ${cx + 50} ${ground - 96}Z", Color(0xFFFFE07A), Ink, 3f)
    rect(cx - 12f, ground - 40f, 24f, 40f, 12f, Color(0xFF8A5A2E), Ink, 2f)
    circle(cx, ground - 74f, 10f, Color.White, Ink, 2f)
    // campo
    rect(0f, ground, 640f, vh - ground, brush = vGrad(ground, vh, 0f to Color(0xFF7EE06A), 1f to Color(0xFF2F9A4A)))
    for (k in 0 until 14) {
        val x = k * 48f + 10f
        path("M$x ${vh - 10}Q${x + 4} ${vh - 28} ${x + 2} ${vh - 40}", stroke = Color(0xFF1F7A3A), sw = 3f)
    }
}
