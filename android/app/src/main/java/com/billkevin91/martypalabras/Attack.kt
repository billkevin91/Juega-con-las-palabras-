package com.billkevin91.martypalabras

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.random.Random

/** Mancha de tinta de Borrón: posición en % de la tarjeta y tamaño en dp. */
private data class InkBlot(val l: Float, val t: Float, val s: Float)

/** Segundos del Ataque según la dificultad. */
fun attackSeconds(l: Level) = when (l) { Level.FACIL -> 75; Level.NORMAL -> 60; Level.DIFICIL -> 45 }

/* Ataque de las palabras: elegir la palabra bien escrita antes de que se acabe el tiempo o las vidas. */
/** Guarida de Borrón: caldero de tinta burbujeante; el dibujo flota en una burbuja. */
@Composable
private fun AttackLair(pic: String, mood: Mood) {
    val time = rememberTime()
    SceneBox(16f / 9f, 640f / 420f, Color(0xFF1A0E3D), Ink, 3.dp) { W, _, tall ->
        val px = W / 640f
        val vh = if (tall) 420f else 360f
        val bubY = if (tall) 170f else 140f
        Canvas(Modifier.fillMaxSize()) { viewBox(640f, vh) { lair(time.value, vh, bubY, mood) } }
        Box(
            Modifier.offset(px * (320f - 70f), px * (bubY - 70f)).size(px * 140f).graphicsLayer {
                translationY = lerp(-8f, 8f, swing(time.value, 1.6f)) * (W.toPx() / 640f)
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(pic, style = androidx.compose.ui.text.TextStyle(fontSize = (px * 70f).asSp()))
        }
    }
}

private fun DrawScope.lair(t: Float, vh: Float, bubY: Float, mood: Mood) {
    rect(0f, 0f, 640f, vh, brush = vGrad(0f, vh, 0f to Color(0xFF120830), .6f to Color(0xFF2B1A5C), 1f to Color(0xFF3A2378)))
    // manchas en las paredes
    for ((x, y, r) in listOf(Triple(60f, 90f, 40f), Triple(590f, 70f, 34f), Triple(110f, 250f, 28f), Triple(560f, 230f, 30f))) {
        circle(x, y, r, Color(0xFF0B0824), alpha = .7f)
        circle(x + r * .8f, y - r * .5f, r * .3f, Color(0xFF0B0824), alpha = .7f)
    }
    // techo que gotea
    path("M0 0H640V26Q600 44 560 26T480 30T400 22T320 34T240 24T160 32T80 22T0 30Z", Color(0xFF0B0824))
    for (k in 0 until 7) {
        val x = 40f + k * 92f
        val p = ((t / 2.2f + k * .37f) % 1f)
        val y = 30f + p * (vh - 60f)
        ellipse(x, y, 5f, 8f, Color(0xFF5B49C9), alpha = 1f - p * .8f)
    }
    // letras manchadas flotando
    listOf("A", "Ñ", "B", "V", "H", "Ü").forEachIndexed { k, _ ->
        val x = 70f + k * 100f
        val y = 70f + 20f * sin(t * .8f + k) + (k % 2) * 40f
        circle(x, y, 13f, Color(0xFF8C7BF0), alpha = .18f)
    }
    // burbuja con el dibujo
    val by = bubY + lerp(-8f, 8f, swing(t, 1.6f))
    circle(320f, by, 78f, Color(0xFFB9A6FF), alpha = .18f)
    circle(320f, by, 70f, Color(0xFFFFFFFF), alpha = .14f)
    circle(320f, by, 70f, stroke = Color(0xFFD9CCFF), sw = 4f, alpha = .9f)
    spin(-28f, 290f, by - 40f) { ellipse(290f, by - 40f, 20f, 10f, Color.White, alpha = .7f) }
    // caldero
    val cy = vh - 70f
    for (k in 0 until 4) {
        val p = ((t / 1.4f + k * .25f) % 1f)
        circle(270f + k * 34f, cy - 30f - 70f * p, 6f + 6f * p, Color(0xFF8C7BF0), alpha = .8f * (1f - p))
    }
    ellipse(320f, cy - 18f, 120f, 22f, Color(0xFF6B57D6))
    for (k in 0 until 5) {
        val p = ((t / .9f + k * .2f) % 1f)
        circle(230f + k * 45f, cy - 22f, 8f * sin(p * PI.toFloat()), Color(0xFF8C7BF0))
    }
    path("M196 ${cy - 18}Q190 ${cy + 60} 320 ${cy + 62}Q450 ${cy + 60} 444 ${cy - 18}Z", Color(0xFF2A2350), Ink, 4f)
    ellipse(320f, cy - 18f, 126f, 16f, stroke = Color(0xFF3F3670), sw = 10f)
    for (x in listOf(250f, 390f)) ellipse(x, cy + 64f, 18f, 10f, Color(0xFF2A2350), Ink, 3f)
    // fuego
    for (k in 0 until 5) {
        val f = lerp(.7f, 1.1f, swing(t + k * .3f, .25f))
        zoom(1f, f, 250f + k * 35f, vh) { path("M${238 + k * 35} ${vh}Q${250 + k * 35} ${vh - 40} ${262 + k * 35} ${vh}Z", Color(0xFFFF9A3C)) }
    }
    // Borrón, a un lado
    group(500f, vh - 150f, 1.25f) { borron(t, mood) }
}

@Composable
fun AttackScreen(g: Game) {
    val queue = remember { WORDS.filter { it.wrongs.size >= 2 }.shuffled() }
    var idx by remember { mutableIntStateOf(0) }
    val level = remember { g.level }
    val total = attackSeconds(level).toFloat()
    val maxLives = if (level == Level.FACIL) 4 else 3
    var left by remember { mutableFloatStateOf(total) }
    var lives by remember { mutableIntStateOf(maxLives) }
    var score by remember { mutableIntStateOf(0) }
    var ok by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var marks by remember { mutableStateOf(mapOf<String, Int>()) }
    val inks = remember { mutableStateListOf<InkBlot>() }
    val splat = remember { Animatable(1f) }
    val jolt = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var over by remember { mutableStateOf(false) }

    val cur = queue[idx % queue.size]
    // Difícil: dos trampas en lugar de una.
    val opts = remember(idx) { (listOf(cur.text) + cur.wrongs.shuffled().take(if (level == Level.DIFICIL) 2 else 1)).shuffled() }

    fun finish() {
        if (over) return
        over = true
        g.attackEnd(score, ok)
    }

    LaunchedEffect(Unit) {
        while (left > 0f && !over) {
            delay(100)
            left -= .1f
        }
        finish()
    }

    fun answer(o: String) {
        if (locked || over) return
        locked = true
        if (o == cur.text) {
            marks = mapOf(o to 1)
            ok++; combo++
            score += 10 + if (combo >= 3) 5 else 0
            g.tone(600, 800)
            g.react(Mood.HAPPY)
            g.say("¡Bien! ${cur.text}")
            scope.launch {
                delay(400)
                marks = emptyMap(); idx++; locked = false
            }
        } else {
            marks = mapOf(o to 2, cur.text to 1)
            lives--; combo = 0
            g.tone(200, 150)
            inks.add(InkBlot(Random.nextInt(5, 65).toFloat(), Random.nextInt(5, 65).toFloat(), Random.nextInt(90, 170).toFloat()))
            scope.launch {
                splat.snapTo(0f)
                splat.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            scope.launch {
                jolt.snapTo(1f)
                jolt.animateTo(0f, tween(260))
            }
            g.react(Mood.HAPPY)
            g.say("¡Jajaja! Se escribe ${cur.text}. ¡Una mancha más!", Who.BORRON)
            scope.launch {
                delay(900)
                if (lives <= 0) finish() else {
                    marks = emptyMap(); idx++; locked = false
                }
            }
        }
    }

    Box(Modifier.graphicsLayer { translationX = 5f * density * jolt.value * sin(jolt.value * 20f) }) {
        GameCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("♥".repeat(lives), style = display(20, C.berry))
                Text("♥".repeat(maxLives - lives), style = display(20, C.berry.copy(alpha = .3f)), modifier = Modifier.weight(1f))
                Text("${ceil(left.coerceAtLeast(0f)).toInt()} s", style = display(20), modifier = Modifier.weight(1f))
                Text("$score pts", style = display(20))
            }
            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(8.dp)).background(C.line)) {
                Box(Modifier.fillMaxWidth((left / total).coerceIn(0f, 1f)).height(12.dp).background(C.sun))
            }
            Text(if (combo >= 3) "¡Racha de $combo!" else " ", style = body(16, C.sun, FontWeight.ExtraBold))
            AttackLair(cur.pic, g.mood)
            Text("¿Cómo se escribe?", style = body(16, C.muted, FontWeight.ExtraBold))
            CenterFlow {
                opts.forEachIndexed { i, o ->
                    OptTile(o, C.tiles[(i + 1) % 5], state = marks[o] ?: 0, wide = true) { answer(o) }
                }
            }
        }
        Canvas(Modifier.matchParentSize()) {
            inks.forEachIndexed { i, k ->
                val sc = if (i == inks.lastIndex) splat.value else 1f
                val w = k.s * density
                val h = w * .8f
                val x = size.width * k.l / 100f + w / 2
                val y = size.height * k.t / 100f + h / 2
                zoom(sc, sc, x, y) {
                    val inkC = Color(0xE60B0824)
                    ellipse(x, y, w / 2, h / 2, inkC)
                    circle(x + w * .5f, y - h * .3f, w * .12f, inkC)
                    rect(x - w * .1f, y + h * .2f, w * .14f, h * .55f * sc, w * .07f, inkC)
                    circle(x - w * .42f, y + h * .32f, w * .07f, inkC)
                }
            }
        }
    }
}
