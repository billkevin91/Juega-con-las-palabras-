package com.billkevin91.martypalabras

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/** Sacudida corta cada vez que cambia [key] (como .shake en el HTML). */
@Composable
fun Modifier.shake(key: Int): Modifier {
    val a = remember { Animatable(1f) }
    LaunchedEffect(key) {
        if (key > 0) {
            a.snapTo(0f)
            a.animateTo(1f, tween(400))
        }
    }
    return graphicsLayer { rotationZ = -8f * sin(2f * PI.toFloat() * a.value) }
}

@Composable
fun PicCircle(text: String) {
    Box(
        Modifier.size(132.dp).clip(CircleShape).background(C.space2).border(4.dp, C.sun, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = TextStyle(fontSize = 70.sp, color = C.muted, fontFamily = Baloo)) }
}

/** Casilla de letra: 0 vacía, 1 llena (verde), 2 hueco a completar. */
@Composable
fun Slot(text: String, kind: Int) {
    val shape = RoundedCornerShape(14.dp)
    val m = when (kind) {
        1 -> Modifier.clip(shape).background(C.leaf)
        2 -> Modifier.clip(shape).border(3.dp, C.berry, shape)
        else -> Modifier.clip(shape).border(3.dp, C.muted, shape)
    }
    Box(m.size(46.dp, 56.dp), contentAlignment = Alignment.Center) {
        Text(text.uppercase(), style = display(28, if (kind == 1) C.onColor else if (kind == 2) C.berry else C.ink))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CenterFlow(content: @Composable () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

/* Letra perdida: elige la letra que falta. */
@Composable
fun FaltaGame(g: Game, s: Session) {
    val word = remember { letters(s.word.text.uppercase()) }
    val gap = remember { word.indices.random() }
    val miss = word[gap]
    val opts = remember {
        val set = linkedSetOf(miss)
        val base = norm(miss)
        if (base != miss) set.add(base)
        val abc = letters("ABCDEFGHILMNOPRSTUV")
        while (set.size < 3) set.add(abc.random())
        set.toList().shuffled()
    }
    var filled by remember { mutableStateOf(false) }
    val shakes = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(Unit) { g.say("¿Qué letra falta? Fíjate bien en las tildes.") }
    QFrame(g, s, hear = true) {
        PicCircle(s.word.pic)
        CenterFlow {
            word.forEachIndexed { j, c ->
                if (j == gap && !filled) Slot("?", 2) else Slot(c, 1)
            }
        }
        CenterFlow {
            opts.forEachIndexed { i, c ->
                Box(Modifier.shake(shakes[c] ?: 0)) {
                    OptTile(c, C.tiles[(i + 1) % 5]) {
                        if (s.locked) return@OptTile
                        if (c == miss) {
                            filled = true
                            g.win(!s.tried)
                        } else {
                            shakes[c] = (shakes[c] ?: 0) + 1
                            g.wrong()
                        }
                    }
                }
            }
        }
    }
}

/* Lectura: toca la palabra que corresponde al dibujo. */
@Composable
fun LeeGame(g: Game, s: Session) {
    val opts = remember {
        (listOf(s.word.text) + WORDS.filter { it.text != s.word.text }.shuffled().take(2).map { it.text }).shuffled()
    }
    var marks by remember { mutableStateOf(mapOf<String, Int>()) }
    var shake by remember { mutableIntStateOf(0) }
    var shaken by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { g.say("Lee y toca la palabra del dibujo.") }
    QFrame(g, s, hear = true) {
        PicCircle(s.word.pic)
        CenterFlow {
            opts.forEachIndexed { i, x ->
                Box(Modifier.shake(if (shaken == x) shake else 0)) {
                    OptTile(x, C.tiles[(i + 2) % 5], state = marks[x] ?: 0, wide = true) {
                        if (s.locked) return@OptTile
                        if (x == s.word.text) {
                            marks = marks + (x to 1)
                            g.win(!s.tried)
                        } else {
                            shaken = x
                            shake++
                            g.wrong()
                        }
                    }
                }
            }
        }
    }
}
