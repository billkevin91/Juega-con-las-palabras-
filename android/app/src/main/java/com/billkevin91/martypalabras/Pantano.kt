package com.billkevin91.martypalabras

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/* Pantanal (planeta Anfibia): adivina la palabra tocando nenúfares, con 6 vidas. */

private const val SW_P = "M320 198C400 205 560 280 610 330C560 400 400 466 320 470C240 466 80 400 30 330C80 280 240 205 320 198Z"
private val PAD_TOP = Color(0xFFC0F870)
private val PAD_MID = Color(0xFF52C654)
private val PAD_DARK = Color(0xFF1F8A3E)

private class Ripple(val x: Float, val y: Float, val bad: Boolean, val at: Float)
private class FrogColors(val b: Color, val d: Color, val l: Color, val s: Color, val a: Color, val eyes: Int)

private data class Pad(val c: String, val x: Float, val y: Float, val rot: Float, val per: Float, val ph: Float, val accent: Boolean)

private val PADS: List<Pad> = run {
    val rows = listOf(5, 7, 8, 7, 6)
    val ys = listOf(54f, 61.5f, 69f, 76.5f, 84f)
    val out = mutableListOf<Pad>()
    var idx = 0
    rows.forEachIndexed { r, n ->
        for (j in 0 until n) {
            val i = idx++
            val x = 50f + (j - (n - 1) / 2f) * 9.4f + (((i * 37) % 11) - 5) / 5f * .7f
            val y = ys[r] + (((i * 53) % 7) - 3) / 3f * .6f
            out += Pad(KEYS[i], x, y, ((((i * 47) % 9) - 4) * 3).toFloat(), 2.6f + (i % 5) * .35f, (i % 7) * .4f, i > 26)
        }
    }
    out
}

@Composable
fun PantanoGame(g: Game, s: Session) {
    val word = remember { letters(s.word.text.uppercase()) }
    val got = remember { mutableStateMapOf<String, Boolean>() }
    val used = remember { mutableStateMapOf<String, Int>() }
    var lives by remember { mutableIntStateOf(6) }
    var lost by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }
    var last by remember { mutableStateOf("") }
    var fxKind by remember { mutableIntStateOf(0) }
    var fxAt by remember { mutableFloatStateOf(-9f) }
    val ripples = remember { mutableStateListOf<Ripple>() }
    val time = rememberTime()
    val flies = remember { Fireflies(14, 18f, 92f, Color(0xFFF3FF8A)) }

    LaunchedEffect(Unit) { g.say("Toca los nenúfares para descubrir la palabra. ¡Cuidado con las tildes!") }

    fun hang(p: Pad) {
        val c = p.c
        if (s.locked || used.containsKey(c)) return
        val now = time.value
        if (word.contains(c)) {
            used[c] = 1; got[c] = true; last = c
            g.tone(520)
            ripples += Ripple(p.x, p.y, false, now)
            fxKind = 1; fxAt = now
            if (word.all { got[it] == true }) {
                won = true
                g.win(true)
            }
        } else {
            used[c] = 2
            lives--
            s.tried = true
            g.tone(200, 150)
            val tilde = word.any { it != c && norm(it) == c && got[it] != true }
            g.react(Mood.SAD)
            g.say(if (tilde) "¡Cuidado! Esa letra sí está, pero lleva tilde, diéresis o es otra letra." else "Esa letra no está.")
            ripples += Ripple(p.x, p.y, true, now)
            fxKind = 2; fxAt = now
            if (lives <= 0) {
                s.locked = true
                lost = true
                g.say("¡Jajaja! Era ${s.word.text}.", Who.BORRON)
                g.react(Mood.HAPPY)
                g.later(s, 2400)
            }
        }
        if (ripples.size > 8) ripples.removeAt(0)
    }

    QFrame(g, s) {
        BoxWithConstraints(Modifier.scene(4f / 3f).background(Color(0xFF12402F))) {
            val W = maxWidth
            val H = maxHeight
            val u = W / 100f
            Canvas(Modifier.fillMaxSize()) {
                val t = time.value
                viewBox(640f, 480f) {
                    swamp(t, won, fxKind, t - fxAt)
                    ripples.forEach { r ->
                        val p = (t - r.at) / .8f
                        if (p in 0f..1f) {
                            val sc = lerp(.4f, 1.6f, p)
                            ellipse(r.x * 6.4f, r.y * 4.8f, 38f * sc, 19f * sc, stroke = if (r.bad) Color(0xFF0B2E24) else Color.White.copy(alpha = .75f), sw = 3f, alpha = 1f - p)
                        }
                    }
                }
            }
            // Medalla con el dibujo
            Box(Modifier.at(W, H, 7.2f, 20.8f, u * 11, u * 11), contentAlignment = Alignment.Center) {
                Text(s.word.pic, style = TextStyle(fontSize = (u * 7).asSp()))
            }
            // Vidas
            Row(Modifier.offset(u * 2, u * 2), horizontalArrangement = Arrangement.spacedBy(u * .6f)) {
                repeat(6) { i ->
                    Canvas(Modifier.size(u * 5.2f, u * 2.6f)) {
                        ellipse(size.width / 2, size.height * .6f, size.width * .42f, size.height * .35f, if (i < lives) Color(0xFF8FF04F) else Color(0xFF2F4A3C), alpha = if (i < lives) 1f else .6f)
                    }
                }
            }
            // Letras del cartel
            Row(
                Modifier.offset(y = H * .2167f - u * 4).align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(u * .7f),
            ) {
                word.forEach { c ->
                    val on = got[c] == true
                    val pop by animateFloatAsState(if (on) 1f else .4f, tween(450), label = "pop")
                    val shape = RoundedCornerShape(u)
                    val bg = when {
                        on -> Color(0xFFFFF3C4)
                        lost -> Color(0xFF5A2A52)
                        else -> Color(0xB3281608)
                    }
                    Box(
                        Modifier.size(u * 5.6f, u * 8).graphicsLayer {
                            val k = if (on && c == last) pop else 1f
                            scaleX = k; scaleY = k
                        }.then(if (on) Modifier.shadow(u * 1.4f, shape, ambientColor = Color(0xFFFFF3A0), spotColor = Color(0xFFFFF3A0)) else Modifier)
                            .clip(shape).background(bg).border(u * .35f, if (on) Color(0xFFE8B030) else if (lost) Color(0xFF8A4A7A) else Color(0xFF2A1708), shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on || lost) Text(
                            c, style = TextStyle(
                                fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (u * 4.8f).asSp(),
                                color = if (on) Color(0xFF4A2C12) else Color(0xFFFFB3DA), textAlign = TextAlign.Center,
                            )
                        )
                    }
                }
            }
            // Nenúfares con letras
            PADS.forEach { p ->
                val st = used[p.c] ?: 0
                val sink by animateFloatAsState(if (st == 2) 1f else 0f, tween(800), label = "sink")
                Box(
                    Modifier.at(W, H, p.x, p.y, u * 8.6f, u * 7f).graphicsLayer {
                        val t = time.value
                        val bob = lerp(-.015f, .025f, swing(t + p.ph, p.per))
                        translationY = (bob + .4f * sink) * size.height
                        val k = 1f - .3f * sink
                        scaleX = k; scaleY = k
                        alpha = 1f - .7f * sink
                    }.tap { hang(p) },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        spin(p.rot, size.width / 2, size.height / 2) {
                            group(s = size.width / 100f, sy = size.height / 80f) { padArt(st == 1, p.accent) }
                        }
                    }
                    Text(
                        p.c, modifier = Modifier.offset(y = -u * .4f),
                        style = TextStyle(
                            fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (u * 4.6f).asSp(),
                            color = if (st == 1) Color(0xFFFFF9B0) else Color.White,
                            shadow = androidx.compose.ui.graphics.Shadow(Color(0xE60A3214), blurRadius = 6f),
                        ),
                    )
                }
            }
            Canvas(Modifier.fillMaxSize()) { with(flies) { drawFlies(time.value) } }
        }
    }
}

private fun DrawScope.padArt(good: Boolean, accent: Boolean) {
    if (good) ellipse(50f, 45f, 52f, 33f, Color(0xFFEAFF8A), alpha = .55f)
    ellipse(50f, 50f, 46f, 26f, Color(0xFF0F4A2B))
    ellipse(50f, 43f, 46f, 27f, brush = rGrad(38f, 30f, 70f, 0f to PAD_TOP, .55f to PAD_MID, 1f to PAD_DARK))
    for (a in listOf(30, 75, 120, 165, 210, 255, 300, 345)) {
        val r = a * PI.toFloat() / 180f
        line(50f, 43f, 50f + 38f * cos(r), 43f + 21f * sin(r), Color(0x732D5A28), 1.6f)
    }
    path("M50 43L86 22L94 34Z", Color(0xFF1D5A3C), alpha = .85f)
    ellipse(34f, 31f, 14f, 5f, Color.White, alpha = .25f)
    if (accent) {
        circle(82f, 6f, 10f, Color(0xFFFF7FC2))
        circle(82f, 6f, 4f, Color(0xFFFFF4A8))
    }
}

private fun DrawScope.dpad(x: Float, y: Float, k: Float) = group(x, y, k) {
    ellipse(0f, 6f, 46f, 26f, Color(0xFF0F4A2B))
    ellipse(0f, 0f, 46f, 27f, brush = rGrad(-12f, -13f, 70f, 0f to PAD_TOP, .55f to PAD_MID, 1f to PAD_DARK))
    ellipse(-14f, -10f, 14f, 5f, Color.White, alpha = .25f)
}

private fun DrawScope.lotus(x: Float, y: Float, k: Float) = group(x, y, k) {
    spin(-35f, -9f, -8f) { ellipse(-9f, -8f, 5f, 10f, Color(0xFFFF8FC7)) }
    spin(35f, 9f, -8f) { ellipse(9f, -8f, 5f, 10f, Color(0xFFFF8FC7)) }
    ellipse(0f, -11f, 5f, 12f, Color(0xFFFFB3DA))
    circle(0f, -3f, 3f, Color(0xFFFFE36A))
}

private fun DrawScope.reed(x: Float, y: Float, h: Float) {
    path("M$x ${y}Q${x - 6} ${y - h / 2} ${x + 4} ${y - h}", stroke = Color(0xFF2F7A3A), sw = 3f)
    ellipse(x + 4, y - h + 8, 3.5f, 10f, Color(0xFF6B3F22))
}

private fun DrawScope.frog(x: Float, y: Float, s: Float, c: FrogColors, d: Float, t: Float, fx: Int, fxT: Float) = group(x, y, s) {
    ellipse(0f, 2f, 32f, 6f, Color.Black, alpha = .28f)
    var dy = 0f
    var sx = 1f
    var sy = 1f
    var rot = 0f
    if (fx == 1 && fxT in 0f..0.6f) {
        val p = fxT / .6f
        if (p < .4f) { val f = p / .4f; dy = -14f * f; sx = lerp(1f, .95f, f); sy = lerp(1f, 1.1f, f) } else if (p < .7f) {
            val f = (p - .4f) / .3f; dy = lerp(-14f, 0f, f); sx = lerp(.95f, 1.08f, f); sy = lerp(1.1f, .92f, f)
        } else { val f = (p - .7f) / .3f; sx = lerp(1.08f, 1f, f); sy = lerp(.92f, 1f, f) }
    } else if (fx == 2 && fxT in 0f..0.5f) {
        rot = 6f * sin(fxT / .5f * 2f * PI.toFloat()) * -1f
        sy = .9f
    } else {
        val f = swing(t + d, 1.8f)
        sx = lerp(1f, 1.03f, f); sy = lerp(1f, .97f, f)
    }
    group(y = dy) {
        spin(rot, 0f, 0f) {
            zoom(sx, sy, 0f, 0f) {
                ellipse(-24f, -6f, 13f, 8f, c.d); ellipse(24f, -6f, 13f, 8f, c.d)
                ellipse(0f, -17f, 28f, 18f, c.b); ellipse(0f, -10f, 18f, 10f, c.l)
                circle(-14f, -24f, 4f, c.s); circle(12f, -27f, 5f, c.s); circle(21f, -19f, 3f, c.s)
                val puff = swing(t, 1.2f)
                zoom(lerp(1f, 1.35f, puff), lerp(1f, 1.25f, puff), 0f, -19f) { ellipse(0f, -13f, 9f, 6f, Color(0xFFFF8FB5), alpha = .9f) }
                path("M-6 -42L-14 -64M7 -42L16 -60", stroke = c.d, sw = 2.5f)
                circle(-14f, -65f, 4.5f, c.a, alpha = lerp(.35f, 1f, swing(t, .8f)))
                circle(16f, -61f, 4f, c.a, alpha = lerp(.35f, 1f, swing(t, .8f, .4f)))
                zoom(1f, blink(t + d, 4.2f), 0f, -38f) {
                    circle(-13f, -36f, 8f, Color.White); circle(13f, -36f, 8f, Color.White)
                    circle(-12f, -35f, 3.6f, C.onColor); circle(14f, -35f, 3.6f, C.onColor)
                    if (c.eyes == 3) { circle(0f, -45f, 6.5f, Color.White); circle(0f, -44f, 3f, C.onColor) }
                }
                path("M-14 -22Q0 -14 14 -22", stroke = C.onColor, sw = 2.5f)
                ellipse(-14f, -2f, 9f, 4.5f, c.d); ellipse(14f, -2f, 9f, 4.5f, c.d)
            }
        }
    }
}

private val POND_CLIP: Path get() = Paths.of(SW_P)

private fun DrawScope.swamp(t: Float, won: Boolean, fx: Int, fxT: Float) {
    rect(0f, 0f, 640f, 215f, brush = vGrad(0f, 215f, 0f to Color(0xFF151F5C), .55f to Color(0xFF6A3FB0), 1f to Color(0xFFF48BB4)))
    listOf(40f to 28f, 96f to 70f, 210f to 22f, 250f to 64f, 330f to 36f, 400f to 18f, 455f to 58f, 610f to 30f, 300f to 92f, 20f to 110f)
        .forEachIndexed { i, (x, y) -> circle(x, y, if (i % 2 == 0) 1.4f else 1f, Color.White, alpha = .85f) }
    group(540f, 70f, rot = -16f) {
        ellipse(0f, 0f, 66f, 14f, stroke = Color(0xFFFFE0A0), sw = 5f, alpha = .9f)
        circle(0f, 0f, 38f, Color(0xFFFF9F6B))
        path("M-38 -6Q0 4 38 -8M-34 14Q0 24 36 12", stroke = Color(0xFFE0603C), sw = 5f, alpha = .6f)
        path("M-66 0A66 14 0 0 0 66 0", stroke = Color(0xFFFFE0A0), sw = 5f)
    }
    circle(150f, 52f, 16f, Color(0xFFE4F6FF)); circle(145f, 48f, 3.5f, Color(0xFFBCD9E8)); circle(156f, 58f, 2.5f, Color(0xFFBCD9E8))
    path("M0 190L0 150Q60 110 130 140T260 130T400 145T530 120T640 150L640 190Z", Color(0xFF3A2C86), alpha = .9f)
    path("M0 205L0 168Q80 140 160 168T320 160T480 170T640 160L640 205Z", Color(0xFF2A2468))
    group(x = 12f * wave(t, 28f)) {
        rect(-20f, 168f, 680f, 40f, brush = vGrad(168f, 208f, 0f to Color(0x00FFD0EC), .5f to Color(0x73FFD0EC), 1f to Color(0x00FFD0EC)))
    }
    path("M0 192Q160 172 320 190T640 188V480H0Z", brush = vGrad(190f, 480f, 0f to Color(0xFF3C8A45), 1f to Color(0xFF1B3D2C)))
    mush(30f, 262f, 1.15f, Color(0xFFE8508A), Color(0xFFFFE8F2)); mush(612f, 248f, 1.05f, Color(0xFF7A4FD8), Color(0xFFE9DDFF))
    mush(96f, 232f, .55f, Color(0xFF27B7D8), Color(0xFFE6FBFF)); mush(560f, 226f, .6f, Color(0xFFFF9A3C), Color(0xFFFFF0D6))
    // estanque
    group(0f, 16f) { path(SW_P, Color(0xFF4A3320)) }
    path(SW_P, Color(0xFF76AD4E), Color(0xFF4D7D36), 8f)
    group(9.6f, 9.9f, .97f) {
        path(SW_P, brush = rGrad(320f, 334f, 350f, 0f to Color(0xFF4B9A52), .6f to Color(0xFF1F6A48), 1f to Color(0xFF0F3A38)))
        clipPath(POND_CLIP) {
            listOf(Triple(220f, 300f, 0f), Triple(440f, 360f, -3f), Triple(300f, 420f, -6f)).forEach { (x, y, d) ->
                ellipse(x + 12f * wave(t + d, 18f), y, 90f, 9f, Color(0xFF9BE07A), alpha = .11f)
            }
            listOf(Triple(180f, 330f, 0f), Triple(450f, 300f, 1.3f), Triple(320f, 432f, 2.1f), Triple(520f, 380f, 3f), Triple(150f, 390f, .7f)).forEach { (x, y, d) ->
                val p = ((t + d) / 4f) % 1f
                ellipse(x, y, 46f * lerp(.2f, 1f, p), 14f * lerp(.2f, 1f, p), stroke = Color.White.copy(alpha = .35f), sw = 1.5f, alpha = 1f - p)
            }
        }
    }
    dpad(72f, 334f, .42f); lotus(72f, 328f, .5f); dpad(572f, 332f, .4f); lotus(572f, 326f, .46f); dpad(258f, 228f, .3f)
    dpad(392f, 228f, .3f); lotus(392f, 224f, .36f); dpad(268f, 454f, .34f); dpad(384f, 456f, .3f); lotus(384f, 452f, .36f)
    // cartel de madera
    val wood = vGrad(46f, 218f, 0f to Color(0xFFB9824A), 1f to Color(0xFF8A5A2E))
    rect(170f, 150f, 22f, 68f, 5f, brush = wood); rect(448f, 150f, 22f, 68f, 5f, brush = wood)
    if (won) rect(92f, 46f, 456f, 112f, 14f, stroke = Color(0xFFFFF3A0), sw = 12f, alpha = .9f)
    rect(92f, 46f, 456f, 112f, 14f, brush = wood)
    rect(92f, 46f, 456f, 112f, 14f, stroke = Color(0xFF4A2C12), sw = 6f)
    path("M98 72H542M98 138H542", stroke = Color(0xFF6D4420), sw = 2.5f, alpha = .7f)
    rect(112f, 72f, 416f, 64f, 10f, Color(0xFF2A1708), alpha = .55f)
    for ((x, y) in listOf(106f to 60f, 534f to 60f, 106f to 144f, 534f to 144f)) circle(x, y, 3.5f, Color(0xFF5A3A1A))
    for (r in 0 until 7) group(150f + r * 56f, 59f) {
        val a = lerp(.3f, 1f, swing(t, 1.2f, r * .15f))
        when (r % 4) {
            0 -> circle(0f, 0f, 5f, stroke = Color(0xFF7DFFE3), sw = 2f, alpha = a)
            1 -> path("M0 -6L6 5H-6Z", stroke = Color(0xFF7DFFE3), sw = 2f, alpha = a)
            2 -> path("M0 -6L6 0L0 6L-6 0Z", stroke = Color(0xFF7DFFE3), sw = 2f, alpha = a)
            else -> path("M-6 0H6M0 -6V6", stroke = Color(0xFF7DFFE3), sw = 2f, alpha = a)
        }
    }
    path("M92 66Q116 96 140 78T190 86", stroke = Color(0xFF2F9A4A), sw = 4f)
    circle(116f, 92f, 5f, Color(0xFF3FBF5A)); circle(150f, 82f, 5f, Color(0xFF3FBF5A)); circle(176f, 84f, 4.5f, Color(0xFF3FBF5A))
    circle(46f, 100f, 36f, Color(0xFFFFF7D6), Color(0xFFE8B030), 6f)
    // ranas alienígenas
    rect(10f, 436f, 104f, 22f, 11f, Color(0xFF6B4524)); ellipse(20f, 447f, 7f, 9f, Color(0xFF8A5A2E)); ellipse(590f, 448f, 40f, 12f, Color(0xFF6D6A86))
    frog(48f, 292f, .9f, FrogColors(Color(0xFFD8479A), Color(0xFF8B2A66), Color(0xFFFFB7DC), Color(0xFFFFE36A), Color(0xFF7DFFE3), 2), 0f, t, fx, fxT)
    frog(598f, 296f, .85f, FrogColors(Color(0xFF27B7D8), Color(0xFF14799A), Color(0xFFBFF4FF), Color.White, Color(0xFFFFE36A), 3), -1.2f, t, fx, fxT)
    frog(64f, 438f, 1.15f, FrogColors(Color(0xFFFF9A3C), Color(0xFFC4601A), Color(0xFFFFE0B0), Color(0xFF7A3FB0), Color(0xFFFF7FC2), 2), -2.1f, t, fx, fxT)
    frog(592f, 444f, .95f, FrogColors(Color(0xFF8F5BE8), Color(0xFF5A34A8), Color(0xFFDCC8FF), Color(0xFF7DFFB0), Color(0xFFFFE36A), 3), -.6f, t, fx, fxT)
    reed(14f, 476f, 70f); reed(30f, 478f, 92f); reed(50f, 476f, 60f); reed(590f, 476f, 64f); reed(610f, 478f, 96f); reed(628f, 476f, 74f)
}
