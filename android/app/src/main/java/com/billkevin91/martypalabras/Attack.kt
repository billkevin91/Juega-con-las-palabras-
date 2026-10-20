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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.random.Random

/** Mancha de tinta de Borrón: posición en % de la tarjeta y tamaño en dp. */
private data class InkBlot(val l: Float, val t: Float, val s: Float)

/* Ataque de las palabras: 60 segundos, 3 vidas, elegir la palabra bien escrita. */
@Composable
fun AttackScreen(g: Game) {
    val queue = remember { WORDS.filter { it.wrongs.size >= 2 }.shuffled() }
    var idx by remember { mutableIntStateOf(0) }
    var left by remember { mutableFloatStateOf(60f) }
    var lives by remember { mutableIntStateOf(3) }
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
    val opts = remember(idx) { listOf(cur.text, cur.wrongs.random()).shuffled() }

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
                Text("♥".repeat(3 - lives), style = display(20, C.berry.copy(alpha = .3f)), modifier = Modifier.weight(1f))
                Text("${ceil(left.coerceAtLeast(0f)).toInt()} s", style = display(20), modifier = Modifier.weight(1f))
                Text("$score pts", style = display(20))
            }
            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(8.dp)).background(C.line)) {
                Box(Modifier.fillMaxWidth((left / 60f).coerceIn(0f, 1f)).height(12.dp).background(C.sun))
            }
            Text(if (combo >= 3) "¡Racha de $combo!" else " ", style = body(16, C.sun, FontWeight.ExtraBold))
            PicCircle(cur.pic)
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
