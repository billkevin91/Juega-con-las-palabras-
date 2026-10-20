package com.billkevin91.martypalabras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* Burbujeo (océano alienígena): escucha la palabra y atrapa sus letras en orden. */

private class Bub(val id: Int, val c: String, val x: Float, val dur: Float, val born: Float, val c1: Color, val c2: Color, val sw: Float) {
    var state by mutableIntStateOf(0) // 0 subiendo, 1 reventada, 2 equivocada
    var at = 0f
}

private val BPAL = listOf(
    Color(0x8CFF78C8) to Color(0xF2FF5ABE), Color(0x8078FFE6) to Color(0xF228E6D2), Color(0x8CFFE66E) to Color(0xF2FFBE28),
    Color(0x8CAA8CFF) to Color(0xF28C64FF), Color(0x8082FF96) to Color(0xF23CDC6E),
)

@Composable
fun BurbujaGame(g: Game, s: Session) {
    val word = remember { letters(s.word.text.uppercase()) }
    val sp = remember { g.canSpeak }
    var pos by remember { mutableIntStateOf(0) }
    var clue by remember { mutableStateOf(!sp) }
    var won by remember { mutableStateOf(false) }
    val bubs = remember { mutableStateListOf<Bub>() }
    val time = rememberTime()
    var nextId by remember { mutableIntStateOf(0) }
    val amb = remember { List(9) { floatArrayOf(4 + Random.nextFloat() * 90, 6 + Random.nextFloat() * 12, 5 + Random.nextFloat() * 5, Random.nextFloat() * 6) } }

    fun spawn(frac: Float) {
        if (s.locked || won) return
        if (bubs.count { it.state == 0 } >= 10) return
        val need = word[pos]
        val c = if (Random.nextFloat() < .5f) need else {
            val base = norm(need)
            val pool = word + letters("AEIOUSRNLTCD") + if (base != need) listOf(base, base) else emptyList()
            pool.random()
        }
        val dur = 8f + Random.nextFloat() * 4f
        val pal = BPAL.random()
        val x = 8f + Random.nextInt(7) * 14.2f + Random.nextFloat() * 3f
        bubs += Bub(nextId++, c, x, dur, time.value - frac * dur, pal.first, pal.second, 2f + Random.nextFloat() * 1.5f)
    }

    fun catchB(b: Bub) {
        if (s.locked || b.state != 0) return
        val need = word[pos]
        b.at = time.value
        if (b.c == need) {
            b.state = 1
            pos++
            g.tone(520 + pos * 40)
            if (pos >= word.size) {
                won = true
                g.win(!s.tried)
            } else g.say("¡Esa es! Busca la siguiente letra.")
        } else {
            b.state = 2
            g.wrong()
            g.say(if (norm(b.c) == norm(need)) "¡Casi! Es otra letra: fíjate en la tilde." else "Esa letra no es. Busca la siguiente en la palabra.")
        }
    }

    LaunchedEffect(Unit) {
        g.say(if (sp) "Escucha la palabra y atrapa las burbujas con sus letras, en orden." else "Tu teléfono no puede hablar. Mira el dibujo y atrapa las letras en orden.")
        repeat(6) { spawn((it + Random.nextFloat()) / 6f) }
        if (sp) {
            delay(400)
            if (!s.locked) g.speak(s.word.text)
        }
        while (true) {
            delay(750)
            val now = time.value
            bubs.removeAll { (it.state == 0 && now - it.born > it.dur) || (it.state != 0 && now - it.at > .7f) }
            spawn(0f)
        }
    }

    QFrame(g, s, hear = sp, clue = if (sp) clue else null, onClue = { clue = true }) {
        BoxWithConstraints(Modifier.scene(4f / 3f).background(Color(0xFF0B4A9A))) {
            val W = maxWidth
            val H = maxHeight
            val u = W / 100f
            Canvas(Modifier.fillMaxSize()) {
                val t = time.value
                viewBox(640f, 480f) { ocean(t, won) }
                amb.forEach { (l, sz, per, d) ->
                    val p = ((t + d) / per) % 1f
                    val a = if (p < .12f) p / .12f * .9f else .9f * (1f - (p - .12f) / .88f)
                    val dx = if (p < .5f) lerp(0f, 9f, p * 2) else lerp(9f, -7f, (p - .5f) * 2)
                    val cx = size.width * l / 100f + dx * density
                    val cy = size.height + 14f * density - 230f * density * p
                    circle(cx, cy, sz * density / 2, stroke = Color.White.copy(alpha = .45f), sw = 1.5f * density, alpha = a)
                    circle(cx - sz * density * .12f, cy - sz * density * .12f, sz * density * .2f, Color.White.copy(alpha = .5f), alpha = a)
                }
            }
            // Portilla con el dibujo
            Box(Modifier.at(W, H, 7.2f, 15.4f, u * 12, u * 12), contentAlignment = Alignment.Center) {
                Text(if (clue) s.word.pic else "?", style = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (u * 7).asSp(), color = Color.White))
            }
            // Casillas de la palabra
            Row(
                Modifier.align(Alignment.TopCenter).offset(y = H * .154f - u * 4),
                horizontalArrangement = Arrangement.spacedBy(u * .7f),
            ) {
                word.forEachIndexed { i, c ->
                    val shape = RoundedCornerShape(u * 1.4f)
                    val on = i < pos
                    val cur = i == pos && !won
                    Box(
                        Modifier.size(u * 5.6f, u * 8).graphicsLayer {
                            if (cur) alpha = lerp(.6f, 1f, swing(time.value, .9f))
                        }.clip(shape).background(if (on) Color.White else Color.White.copy(alpha = .18f))
                            .border(u * .4f, if (cur) Color(0xFFFFE36A) else Color.White.copy(alpha = .7f), shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on) Text(c, style = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (u * 4.8f).asSp(), color = Color(0xFF0A4A9A)))
                    }
                }
            }
            // Burbujas con letras
            bubs.forEach { b ->
                key(b.id) {
                    val d = u * 11
                    Box(
                        Modifier.offset(W * (b.x / 100f) - d / 2, 0.dp).size(d).graphicsLayer {
                            val t = time.value
                            val st = b.state
                            val tt = if (st == 0) t else b.at
                            val p = ((tt - b.born) / b.dur).coerceIn(0f, 1f)
                            translationY = H.toPx() * lerp(1.04f, -.16f, p)
                            translationX = u.toPx() * 1.8f * wave(t, b.sw * 2)
                            if (st == 1) {
                                val q = ((t - b.at) / .35f).coerceIn(0f, 1f)
                                val k = lerp(1f, 1.35f, q)
                                scaleX = k; scaleY = k; alpha = 1f - q
                            } else if (st == 2) {
                                val q = ((t - b.at) / .6f).coerceIn(0f, 1f)
                                val k = lerp(1f, .62f, q)
                                scaleX = k; scaleY = k; alpha = lerp(1f, .3f, q)
                            }
                        }.tap(enabled = b.state == 0) { catchB(b) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2
                            val c = center
                            drawCircle(rGrad(c.x, c.y, r, 0f to Color.White.copy(alpha = .06f), .5f to Color.White.copy(alpha = .06f), .82f to b.c1, 1f to b.c2), r, c)
                            drawCircle(b.c2.copy(alpha = .25f), r * 1.15f, c)
                            circle(c.x, c.y, r, stroke = Color.White.copy(alpha = .7f), sw = 2f * density)
                            spin(-28f, c.x - r * .32f, c.y - r * .56f) { ellipse(c.x - r * .32f, c.y - r * .56f, r * .34f, r * .2f, Color.White.copy(alpha = .8f)) }
                            circle(c.x + r * .55f, c.y + r * .5f, r * .13f, Color.White.copy(alpha = .5f))
                            if (b.state == 1) {
                                val q = ((time.value - b.at) / .6f).coerceIn(0f, 1f)
                                for (i in 0 until 10) {
                                    val a = (i * 36f) * PI.toFloat() / 180f
                                    val rr = r * lerp(0f, 1.3f, q)
                                    circle(c.x + sin(a) * rr, c.y - cos(a) * rr, 4.5f * density * (1f - .8f * q), Color.White.copy(alpha = .9f), alpha = 1f - q)
                                }
                            }
                        }
                        Text(
                            b.c, modifier = Modifier.graphicsLayer {
                                if (b.state == 1) {
                                    val q = ((time.value - b.at) / .6f).coerceIn(0f, 1f)
                                    translationY = -26f * density * q
                                    scaleX = 1f + .2f * q; scaleY = 1f + .2f * q
                                }
                            },
                            style = TextStyle(
                                fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (u * 6).asSp(),
                                color = if (b.state == 1) C.leaf else Color.White,
                                shadow = Shadow(Color(0xE6002864), blurRadius = 6f),
                            ),
                        )
                    }
                }
            }
        }
    }
}

/* ---------- Dibujo del océano ---------- */

private class Fish(val b: Color, val d: Color, val l: Color, val s: Color, val a: Color)

private fun DrawScope.ofish(x: Float, y: Float, s: Float, c: Fish, flip: Boolean, t: Float) = group(x, y, if (flip) -s else s, s) {
    group(y = lerp(-8f, 8f, swing(t, 3f, x % 3f))) {
        spin(lerp(-14f, 14f, swing(t, .5f)), -28f, 0f) { path("M-28 0L-58 -20Q-48 0 -58 20Z", c.d) }
        path("M-6 -18Q8 -38 22 -16Z", c.d)
        ellipse(0f, 0f, 34f, 20f, c.b); ellipse(4f, 8f, 24f, 10f, c.l)
        circle(-14f, -4f, 4f, c.s); circle(-4f, -10f, 3f, c.s); circle(-8f, 6f, 3f, c.s)
        path("M20 -16L24 -36", stroke = c.d, sw = 2.5f)
        circle(24f, -37f, 4.5f, c.a, alpha = lerp(.35f, 1f, swing(t, .8f)))
        circle(18f, -6f, 8f, Color.White); circle(20f, -6f, 3.6f, Color(0xFF10224F))
        circle(6f, -13f, 5f, Color.White); circle(7f, -13f, 2.4f, Color(0xFF10224F))
        path("M24 6Q30 11 34 5", stroke = Color(0xFF10224F), sw = 2.5f)
    }
}

private fun DrawScope.osquid(x: Float, y: Float, s: Float, t: Float) = group(x, y, s) {
    group(y = lerp(-8f, 8f, swing(t, 3f))) {
        for (i in 0 until 6) {
            val dx = -20f + i * 8
            val sk = lerp(-8f, 8f, swing(t, 2f, i * .1f))
            path("M$dx 14Q${dx - 8 + sk * .5f} 34 ${dx + sk * .7f} 48T${dx + sk} 78", stroke = Color(0xFF8F5BE8), sw = 5f)
        }
        path("M-24 14Q-26 -34 0 -44Q26 -34 24 14Q0 22 -24 14Z", Color(0xFFB07CFF))
        path("M-22 -22L-40 -8L-24 -2ZM22 -22L40 -8L24 -2Z", Color(0xFF7A4FD8))
        circle(-9f, -4f, 8f, Color.White); circle(9f, -4f, 8f, Color.White)
        circle(-8f, -3f, 3.6f, Color(0xFF10224F)); circle(10f, -3f, 3.6f, Color(0xFF10224F))
        path("M-6 8Q0 13 6 8", stroke = Color(0xFF10224F), sw = 2.5f)
        circle(0f, -32f, 3.5f, Color(0xFF7DFFE3), alpha = lerp(.35f, 1f, swing(t, .8f)))
    }
}

private fun DrawScope.opuffer(x: Float, y: Float, s: Float, t: Float) = group(x, y, s) {
    group(y = lerp(-8f, 8f, swing(t, 3f, .3f))) {
        for (i in 0 until 12) {
            val a = i * 30 * PI.toFloat() / 180
            val x1 = cos(a) * 24; val y1 = sin(a) * 24; val x2 = cos(a) * 36; val y2 = sin(a) * 36
            val px = -sin(a) * 5; val py = cos(a) * 5
            path("M${x1 + px} ${y1 + py}L$x2 ${y2}L${x1 - px} ${y1 - py}Z", Color(0xFF2F9A4A))
        }
        spin(lerp(-14f, 14f, swing(t, .5f)), -24f, 0f) { path("M-24 0L-46 -12L-46 12Z", Color(0xFF2F9A4A)) }
        circle(0f, 0f, 26f, Color(0xFF7EE06A)); ellipse(2f, 10f, 18f, 10f, Color(0xFFE6FFB8))
        circle(-8f, -8f, 3.5f, Color.White); circle(4f, -14f, 3f, Color.White)
        circle(12f, -4f, 9f, Color.White); circle(14f, -4f, 4f, Color(0xFF10224F))
        circle(-2f, -6f, 6f, Color.White); circle(-1f, -6f, 2.8f, Color(0xFF10224F))
        ellipse(22f, 8f, 3f, 4f, Color(0xFF10224F))
        circle(0f, -30f, 3.5f, Color(0xFFFF7FC2), alpha = lerp(.35f, 1f, swing(t, .8f)))
    }
}

private fun DrawScope.ojelly(x: Float, y: Float, s: Float, b: Color, tc: Color, d: Float, t: Float) = group(x, y, s) {
    val f = swing(t + d, 9f)
    group(lerp(-24f, 26f, f), lerp(18f, -36f, f)) {
        for (i in 0 until 5) {
            val dx = -20f + i * 10
            val sk = lerp(-8f, 8f, swing(t, 2f, i * .12f))
            path("M$dx 4Q${dx + 8 + sk * .4f} 22 ${dx + sk * .6f} 36T${dx + sk} 66", stroke = tc, sw = 4f)
        }
        val bp = swing(t, 1.2f)
        zoom(lerp(1f, 1.07f, bp), lerp(1f, .88f, bp), 0f, -44f) {
            path("M-32 4Q-34 -44 0 -44Q34 -44 32 4Q0 12 -32 4Z", b, alpha = .88f)
            path("M-22 -8Q-22 -32 0 -34", stroke = Color.White, sw = 4f, alpha = .5f)
            circle(-12f, -12f, 3f, tc); circle(10f, -20f, 3.5f, tc); circle(16f, -6f, 2.5f, tc)
            circle(-9f, -4f, 4f, Color.White); circle(9f, -4f, 4f, Color.White)
            circle(-8f, -3f, 2f, Color(0xFF10224F)); circle(10f, -3f, 2f, Color(0xFF10224F))
        }
    }
}

private fun DrawScope.okelp(x: Float, y: Float, h: Float, col: Color, d: Float, t: Float) =
    spin(lerp(-6f, 6f, swing(t + d, 3.4f)), x, y) {
        path("M$x ${y}C${x - 16} ${y - h * .3f} ${x + 16} ${y - h * .6f} $x ${y - h}", stroke = col, sw = 8f)
        circle(x, y - h, 6f, col)
    }

private fun DrawScope.ocoral(x: Float, y: Float, s: Float, c: Color, l: Color) = group(x, y, s) {
    path("M0 0V-34M0 -20L-16 -38M0 -28L14 -48M0 -12L18 -26", stroke = c, sw = 9f)
    circle(0f, -36f, 6f, l); circle(-16f, -40f, 5.5f, l); circle(14f, -50f, 6f, l); circle(18f, -28f, 5f, l)
}

private fun DrawScope.oanem(x: Float, y: Float, s: Float, c: Color, l: Color, t: Float) = group(x, y, s) {
    for (i in 0 until 8) {
        val a = (-70f + i * 20) * PI.toFloat() / 180
        spin(lerp(-6f, 6f, swing(t, 3.4f, i * .07f)), 0f, 0f) {
            line(0f, 0f, sin(a) * 28, -cos(a) * 34, c, 5f)
            circle(sin(a) * 28, -cos(a) * 34, 4f, l)
        }
    }
    path("M-14 4Q-14 -10 0 -10Q14 -10 14 4Z", c)
}

private fun DrawScope.ostar(x: Float, y: Float, s: Float, c: Color) = group(x, y, s) {
    val sb = StringBuilder()
    for (i in 0 until 10) {
        val r = if (i % 2 == 1) 7f else 16f
        val a = (i * 36 - 90) * PI.toFloat() / 180
        sb.append(if (i == 0) "M" else "L").append(cos(a) * r).append(' ').append(sin(a) * r)
    }
    path(sb.append("Z").toString(), c, Color.White.copy(alpha = .4f), 1f)
}

private fun DrawScope.oshell(x: Float, y: Float, s: Float) = group(x, y, s) {
    path("M-16 0Q-18 -26 0 -28Q18 -26 16 0Z", Color(0xFFFFB3DA))
    path("M0 0V-26M-8 0L-9 -24M8 0L9 -24", stroke = Color(0xFFE079AD), sw = 2f)
    rect(-8f, 0f, 16f, 4f, 2f, Color(0xFFE079AD))
}

private fun DrawScope.swim(t: Float, dur: Float, dl: Float, rev: Boolean, block: DrawScope.() -> Unit) {
    val p = ((t + dl) / dur).mod(1f)
    val x = if (rev) lerp(780f, -160f, p) else lerp(-160f, 780f, p)
    group(x) { block() }
}

private fun DrawScope.ocean(t: Float, won: Boolean) {
    rect(0f, 0f, 640f, 480f, brush = vGrad(0f, 480f, 0f to Color(0xFF8CF0FF), .35f to Color(0xFF37C4F0), .7f to Color(0xFF1B7FD6), 1f to Color(0xFF0C3F93)))
    val ray = vGrad(0f, 480f, 0f to Color.White.copy(alpha = .55f), 1f to Color.Transparent)
    listOf(floatArrayOf(110f, 170f, 300f, 190f, 0f), floatArrayOf(260f, 300f, 420f, 330f, 2f), floatArrayOf(400f, 450f, 560f, 500f, 4f), floatArrayOf(20f, 60f, 170f, 60f, 1.5f)).forEach { r ->
        val f = swing(t + r[4], 7f)
        spin(lerp(-3f, 3f, f), (r[0] + r[1]) / 2, 0f) {
            path("M${r[0]} 0L${r[1]} 0L${r[2]} 480L${r[3]} 480Z", brush = ray, alpha = if (won) 1f else lerp(.55f, 1f, f))
        }
    }
    group(x = 12f * wave(t, 12f)) { path("M-20 10Q20 0 60 10T140 10T220 10T300 10T380 10T460 10T540 10T620 10T700 10", stroke = Color.White, sw = 4f, alpha = .5f) }
    path("M0 400L0 330Q40 290 80 330Q110 300 150 340Q200 280 240 340L270 400Z", Color(0xFF0A3B86), alpha = .35f)
    path("M640 400L640 320Q600 280 560 330Q520 290 480 340Q440 300 400 350L380 400Z", Color(0xFF0A3B86), alpha = .35f)
    swim(t, 30f, 0f, false) { ofish(0f, 190f, .9f, Fish(Color(0xFFFFB13C), Color(0xFFD9701A), Color(0xFFFFE7A8), Color(0xFFC23A64), Color(0xFFFF7FC2)), false, t) }
    swim(t, 38f, 12f, true) { ofish(0f, 300f, 1f, Fish(Color(0xFF35D6C8), Color(0xFF1A8F9C), Color(0xFFC9FFF6), Color.White, Color(0xFFFFE36A)), true, t) }
    swim(t, 46f, 20f, true) { group(s = -1f, sy = 1f) { opuffer(0f, 238f, .8f, t) } }
    swim(t, 56f, 30f, false) { osquid(0f, 350f, .85f, t) }
    swim(t, 34f, 6f, false) {
        listOf(0f to 0f, 22f to -12f, 24f to 14f, 48f to 2f, 56f to -18f, 60f to 18f, 84f to -4f).forEachIndexed { i, (dx, dy) ->
            group(dx, 150f + dy + lerp(-8f, 8f, swing(t, 3f, i * .13f))) {
                path("M0 0Q8 -7 16 0Q8 7 0 0ZM-1 0L-9 -5L-9 5Z", Color(0xFF9BFFE8))
                circle(12f, -1f, 1.8f, Color(0xFF10224F))
            }
        }
    }
    ojelly(80f, 250f, 1f, Color(0xFFFF9FD6), Color(0xFFFF5FB4), 0f, t)
    ojelly(560f, 290f, .8f, Color(0xFF9FD6FF), Color(0xFF4CC3FF), 2f, t)
    path("M0 412Q80 394 170 406T340 402T520 408T640 398V480H0Z", brush = vGrad(398f, 480f, 0f to Color(0xFFF8DFA8), 1f to Color(0xFFC9955A)))
    crystal(46f, 424f, 1f, Color(0xFFFF7FC2), Color(0xFFC04A98), t, 0f); crystal(82f, 434f, .65f, Color(0xFF7DFFE3), Color(0xFF2DB8A8), t, 1f)
    crystal(596f, 426f, 1.05f, Color(0xFF7DFFE3), Color(0xFF2DB8A8), t, .5f); crystal(562f, 436f, .6f, Color(0xFFFFE36A), Color(0xFFE0A400), t, 1.5f)
    okelp(18f, 440f, 120f, Color(0xFF2AA36A), 0f, t); okelp(104f, 440f, 86f, Color(0xFF35C27A), 1f, t); okelp(150f, 444f, 100f, Color(0xFF2AA36A), 2f, t)
    okelp(618f, 442f, 110f, Color(0xFF35C27A), .5f, t); okelp(528f, 440f, 80f, Color(0xFF2AA36A), 1.5f, t)
    ocoral(130f, 438f, 1f, Color(0xFFFF7A5A), Color(0xFFFFC2A8)); ocoral(190f, 432f, .8f, Color(0xFFB07CFF), Color(0xFFE2D0FF))
    ocoral(500f, 438f, 1f, Color(0xFFFF7FC2), Color(0xFFFFD0EC)); ocoral(446f, 434f, .75f, Color(0xFFFFB13C), Color(0xFFFFE7A8)); ocoral(384f, 438f, .6f, Color(0xFF7DFFE3), Color(0xFFD8FFF7))
    oanem(250f, 440f, 1f, Color(0xFFFF9FD6), Color.White, t); oanem(330f, 438f, .8f, Color(0xFF9FD6FF), Color.White, t)
    ostar(224f, 462f, 1f, Color(0xFFFF9A3C)); ostar(420f, 466f, .9f, Color(0xFFFF7FC2)); oshell(160f, 466f, .8f); oshell(476f, 462f, .7f)
    rect(92f, 26f, 456f, 96f, 28f, Color.White.copy(alpha = .2f), Color.White.copy(alpha = .8f), 4f)
    path("M112 38Q320 22 528 38", stroke = Color.White.copy(alpha = .55f), sw = 3f)
    circle(110f, 108f, 5f, stroke = Color.White.copy(alpha = .7f), sw = 2f); circle(530f, 108f, 6f, stroke = Color.White.copy(alpha = .7f), sw = 2f); circle(522f, 94f, 3f, stroke = Color.White.copy(alpha = .7f), sw = 2f)
    circle(46f, 74f, 38f, Color.White.copy(alpha = .3f), Color(0xFFD8A24A), 7f)
    for ((x, y) in listOf(46f to 32f, 46f to 116f, 4f to 74f, 88f to 74f)) circle(x, y, 3f, Color(0xFF8A5A2E))
}
