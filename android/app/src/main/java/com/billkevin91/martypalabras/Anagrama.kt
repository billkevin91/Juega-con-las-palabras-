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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* Anagrama: pradera alienígena con islas flotantes; cada conejito lleva una letra. */

private val SIGNC = listOf(Color(0xFFFFE36A), Color(0xFF8FDCFF), Color(0xFFFF9DB4), Color(0xFFA6F7A0), Color(0xFFC9B0FF), Color(0xFFFFC58A))

private fun fluff(cx: Float, cy: Float, rx: Float, ry: Float, n: Int, amp: Float): String {
    fun p(a: Float, k: Float) = "${cx + cos(a) * rx * k} ${cy + sin(a) * ry * k}"
    val sb = StringBuilder("M${p(0f, 1f)}")
    for (i in 1..n) {
        val a = i.toFloat() / n * 2 * PI.toFloat()
        val m = (i - .5f) / n * 2 * PI.toFloat()
        sb.append("Q${p(m, 1 + amp)} ${p(a, 1f)}")
    }
    return sb.append("Z").toString()
}

private val RB_BODY = fluff(50f, 100f, 32f, 26f, 14, .1f)
private val RB_HEAD = fluff(50f, 62f, 27f, 23f, 12, .09f)

/** Conejito esponjoso (unidades 0..100 × 0..130). */
private fun DrawScope.rabbit(i: Int, t: Float, droop: Float) {
    val c = SIGNC[i % SIGNC.size]
    val d = i * .45f
    fun earAngle(phase: Float): Float {
        val p = ((t + d + phase) / 3.2f).mod(1f)
        return when {
            p < .7f -> 0f
            p < .8f -> lerp(0f, -10f, (p - .7f) / .1f)
            p < .9f -> lerp(-10f, 6f, (p - .8f) / .1f)
            else -> lerp(6f, 0f, (p - .9f) / .1f)
        }
    }
    spin(earAngle(0f) - 35f * droop, 43.5f, 47f) {
        path("M40 48C27 30 26 6 34 4C42 2 47 26 47 46Z", Color.White, Ink)
        path("M38 42C32 29 31 14 35 12C39 11 42 28 43 42Z", Color(0xFFFFB3D1))
    }
    spin(earAngle(.5f) + 35f * droop, 56.5f, 47f) {
        path("M60 48C73 30 74 6 66 4C58 2 53 26 53 46Z", Color.White, Ink)
        path("M62 42C68 29 69 14 65 12C61 11 58 28 57 42Z", Color(0xFFFFB3D1))
    }
    line(50f, 38f, 50f, 30f, Ink, 2.5f)
    circle(50f, 27f, 4f, c, Ink, 2f, alpha = lerp(.4f, 1f, swing(t + d, .8f)))
    ellipse(34f, 124f, 11f, 5.5f, Color.White, Ink); ellipse(66f, 124f, 11f, 5.5f, Color.White, Ink)
    path(RB_BODY, Color.White, Ink)
    ellipse(50f, 106f, 20f, 14f, Color(0xFFEFEAFF))
    path(RB_HEAD, Color.White, Ink)
    circle(34f, 68f, 5f, Color(0xFFFFB3D1), alpha = .8f); circle(66f, 68f, 5f, Color(0xFFFFB3D1), alpha = .8f)
    zoom(1f, blink(t + d, 4f), 50f, 59f) {
        ellipse(41f, 59f, 4.2f, 5.2f, Color(0xFF1B1448)); ellipse(59f, 59f, 4.2f, 5.2f, Color(0xFF1B1448))
        circle(42.5f, 57f, 1.6f, Color.White); circle(60.5f, 57f, 1.6f, Color.White)
    }
    path("M47 65H53L50 68.5Z", Color(0xFFFF7FA8), Ink, 1.5f)
    path("M50 69V71M46 71Q48 74 50 71Q52 74 54 71", stroke = Ink, sw = 1.8f)
    path("M30 64L20 62M30 68L20 70M70 64L80 62M70 68L80 70", stroke = Ink, sw = 1.2f, alpha = .55f)
    path("M36 86L50 79L64 86", stroke = Ink, sw = 1.6f)
}

private fun DrawScope.portal(x: Float, y: Float, r: Float, c1: Color, c2: Color, c3: Color, clear: Boolean, t: Float, fast: Boolean) = group(x, y, 1f, 1.25f) {
    val k = if (fast) 4f else 1f
    circle(0f, 0f, r * (if (clear) 1.35f else 1.3f), c1, alpha = .22f)
    circle(0f, 0f, r, Color(0xFF2A1460), Ink, 4f)
    val ring = if (clear) .9f else .82f
    spin(t * 72f * k, 0f, 0f) {
        drawCircle(c2, r * ring, Offset.Zero, style = Stroke(r * (if (clear) .14f else .2f), cap = StrokeCap.Butt, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * .9f, r * .35f))))
    }
    spin(-t * 100f * k, 0f, 0f) {
        if (clear) drawCircle(c3, r * 1.12f, Offset.Zero, style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * .4f, r * .25f))))
        else drawCircle(c3, r * .55f, Offset.Zero, style = Stroke(r * .14f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * .5f, r * .3f))))
    }
    if (clear) {
        circle(0f, 0f, r * .78f, Color(0xFFEFFCFF), c1, 3f)
        circle(0f, 0f, r * .78f, c1, alpha = .3f)
    } else {
        circle(0f, 0f, r * .32f, c1, alpha = .55f)
        circle(0f, 0f, r, stroke = c1, sw = 3f)
    }
}

private fun DrawScope.flower(x: Float, y: Float, h: Float, c: Color, d: Float, t: Float) =
    spin(lerp(-6f, 6f, swing(t + d, 3f)), x, y) {
        path("M$x ${y}Q${x - 6} ${y - h / 2} $x ${y - h}", stroke = Color(0xFF2F9A4A), sw = 3f)
        circle(x, y - h - 4, 6f, c, Ink, 2f, alpha = lerp(.45f, 1f, swing(t + d, .8f)))
        spin(-30f, x + 5, y - h * .45f) { ellipse(x + 5, y - h * .45f, 6f, 3f, Color(0xFF3FBF5A)) }
    }

private fun DrawScope.island(x: Float, y: Float, w: Float, d: Float, t: Float, extra: DrawScope.() -> Unit = {}) =
    group(x, y + lerp(-4f, 5f, swing(t + d, 5f))) {
        val h = w * .3f
        val under = "M${-w / 2} 0Q${-w * .32f} $h 0 ${h * 1.6f}Q${w * .3f} $h ${w / 2} 0Z"
        path(under, brush = vGrad(0f, h * 1.6f, 0f to Color(0xFFB07CFF), 1f to Color(0xFF5A3FC0)))
        path(under, stroke = Ink, sw = 3f)
        path("M${-w * .2f} ${h * .4f}L${-w * .1f} ${h * .9f}M${w * .15f} ${h * .3f}L${w * .08f} ${h * .8f}", stroke = Color(0xFFE2D0FF), sw = 2.5f, alpha = .6f)
        ellipse(0f, 0f, w / 2 + 2, w * .11f, brush = vGrad(-w * .11f, w * .11f, 0f to Color(0xFFB8FF8A), 1f to Color(0xFF3FBF5A)))
        ellipse(0f, 0f, w / 2 + 2, w * .11f, stroke = Ink, sw = 3f)
        path("M${-w * .3f} ${w * .06f}q4 8 8 0M${w * .1f} ${w * .08f}q4 9 8 0", stroke = Color(0xFF3FBF5A), sw = 5f)
        extra()
    }

private fun DrawScope.meadow(t: Float, won: Boolean) {
    rect(0f, 0f, 640f, 360f, brush = vGrad(0f, 360f, 0f to Color(0xFF5B4BD8), .4f to Color(0xFFA77CF0), .75f to Color(0xFFFF9FD0), 1f to Color(0xFFFFD9A8)))
    listOf(Triple(30f, 20f, 1.4f), Triple(150f, 36f, 1.1f), Triple(260f, 14f, 1.3f), Triple(400f, 30f, 1f), Triple(470f, 12f, 1.4f), Triple(620f, 90f, 1.2f), Triple(16f, 110f, 1f), Triple(300f, 120f, 1f))
        .forEachIndexed { i, (x, y, r) -> circle(x, y, r * 1.3f, Color.White, alpha = lerp(.3f, 1f, swing(t, 1.2f, i * .19f))) }
    group(578f, 50f, rot = -14f) {
        ellipse(0f, 0f, 52f, 11f, stroke = Color(0xFFFFF3A0), sw = 5f)
        circle(0f, 0f, 28f, Color(0xFFFFB86B), Ink, 3f)
        path("M-26 -6Q0 2 26 -8M-24 10Q0 18 26 8", stroke = Color(0xFFE0703C), sw = 4f, alpha = .6f)
        path("M-52 0A52 11 0 0 0 52 0", stroke = Color(0xFFFFF3A0), sw = 5f)
    }
    circle(44f, 40f, 12f, Color(0xFFE8F6FF), Ink, 2.5f); circle(40f, 37f, 3f, Color(0xFFBCD9E8))
    group(12f * wave(t, 24f)) { ellipse(120f, 96f, 34f, 11f, Color.White, alpha = .85f); ellipse(108f, 88f, 16f, 11f, Color.White, alpha = .85f); ellipse(130f, 86f, 13f, 9f, Color.White, alpha = .85f) }
    group(12f * wave(t, 24f, .3f)) { ellipse(500f, 118f, 30f, 10f, Color.White, alpha = .85f); ellipse(490f, 110f, 14f, 10f, Color.White, alpha = .85f) }
    portal(548f, 162f, 34f, Color(0xFFFF7FC2), Color(0xFFFFC6EC), Color(0xFFFFF3A0), false, t, won)
    island(92f, 226f, 96f, 1.4f, t); island(548f, 222f, 90f, 2.6f, t)
    island(186f, 118f, 64f, .8f, t) { flower(-8f, -4f, 16f, Color(0xFF7DFFE3), .5f, t) }
    island(452f, 104f, 56f, 3f, t) { flower(6f, -4f, 14f, Color(0xFFFFE36A), 1.2f, t) }
    drawLine(Color(0xFF9FE8FF), Offset(380f, 98f), Offset(380f, 170f), 4f, alpha = .8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), -t * 20f))
    island(320f, 90f, 300f, 0f, t) {
        mush(-118f, -6f, .5f, Color(0xFFFF7FC2), Color(0xFFFFE8F2))
        mush(124f, -4f, .4f, Color(0xFF7A4FD8), Color(0xFFE9DDFF))
    }
    portal(92f, 170f, 40f, Color(0xFF7DFFE3), Color(0xFF2DB8A8), Color(0xFFFFE36A), true, t, won)
    path("M-10 370V246Q110 206 230 232T470 220T650 230V370Z", brush = vGrad(206f, 370f, 0f to Color(0xFF6FE0C0), 1f to Color(0xFF2A9A9A)))
    path("M-10 370V246Q110 206 230 232T470 220T650 230V370Z", stroke = Ink, sw = 3f)
    flower(30f, 262f, 30f, Color(0xFFFF7FC2), 0f, t); flower(610f, 256f, 34f, Color(0xFF7DFFE3), 1f, t)
    flower(250f, 240f, 22f, Color(0xFFFFE36A), 2f, t); flower(420f, 232f, 24f, Color(0xFFFF9A3C), .6f, t)
    path("M-10 370V292Q160 262 320 286T650 280V370Z", brush = vGrad(262f, 370f, 0f to Color(0xFF9DF07A), 1f to Color(0xFF3AA860)))
    path("M-10 370V292Q160 262 320 286T650 280V370Z", stroke = Ink, sw = 3f)
    listOf(24f, 40f, 600f, 616f, 180f, 470f).forEachIndexed { j, x ->
        val o = if (j % 2 == 1) 5f else -5f
        path("M$x 352Q${x + o} 338 ${x + o * .4f} 326", stroke = Color(0xFF2F8A4A), sw = 3f)
    }
    flower(18f, 340f, 26f, Color(0xFFC9B0FF), 1.5f, t); flower(624f, 342f, 30f, Color(0xFFFFE36A), .3f, t)
}

@Composable
fun AnagramaGame(g: Game, s: Session) {
    val word = remember { letters(s.word.text.uppercase()) }
    val order = remember {
        var sh = word.shuffled()
        var n = 0
        while (sh == word && n++ < 10) sh = word.shuffled()
        sh
    }
    val n = word.size
    var pos by remember { mutableIntStateOf(0) }
    var won by remember { mutableStateOf(false) }
    val filled = remember { mutableStateMapOf<Int, String>() }
    val used = remember { mutableStateMapOf<Int, Boolean>() }
    val hops = remember { mutableStateMapOf<Int, Pair<Float, Int>>() }
    val nos = remember { mutableStateMapOf<Int, Float>() }
    var popAt by remember { mutableFloatStateOf(-9f) }
    val time = rememberTime()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { g.say("Toca los conejitos en orden para formar la palabra.") }

    fun bunny(i: Int) {
        if (s.locked || used[i] == true || hops.containsKey(i)) return
        val c = order[i]
        if (c != word[pos]) {
            g.wrong()
            g.say("¡Ese conejito no va ahí! Busca la letra que sigue.")
            nos[i] = time.value
            return
        }
        val slot = pos
        pos++
        g.tone(520 + pos * 40)
        hops[i] = time.value to slot
        val last = pos == n
        if (last) g.win(!s.tried) else g.say("¡Salta, conejito! Busca la siguiente letra.")
        scope.launch {
            delay(600)
            used[i] = true
            hops.remove(i)
            filled[slot] = c
            popAt = time.value
            if (last) won = true
        }
    }

    QFrame(g, s, hear = true) {
        BoxWithConstraints(Modifier.scene(16f / 9f, Ink, 22.dp, 3.dp).background(Color(0xFFA77CF0))) {
            val W = maxWidth
            val H = maxHeight
            val q = W / 100f
            Canvas(Modifier.fillMaxSize()) { viewBox(640f, 360f) { meadow(time.value, won) } }
            Box(Modifier.at(W, H, 14.4f, 47.2f, q * 11, q * 11), contentAlignment = Alignment.Center) {
                Text(s.word.pic, style = TextStyle(fontSize = (q * 6).asSp()))
            }
            // Casillas sobre la isla
            val sz = q * min(8f, 72f / n / 1.14f)
            val sGap = sz * .14f
            Row(Modifier.align(Alignment.TopCenter).offset(y = H * .15f - sz * .575f), horizontalArrangement = Arrangement.spacedBy(sGap)) {
                for (j in 0 until n) {
                    val c = filled[j]
                    val shape = RoundedCornerShape(sz * .2f)
                    val cur = j == pos && !won && c == null
                    val bg = when {
                        c != null && won -> Color(0xFF8BEFA6)
                        c != null -> Color(0xFFFFE07A)
                        else -> Color.White.copy(alpha = .3f)
                    }
                    Box(
                        Modifier.size(sz, sz * 1.15f).graphicsLayer {
                            if (cur) alpha = lerp(.4f, 1f, swing(time.value, .9f))
                            if (c != null && j == pos - 1) {
                                val p = ((time.value - popAt) / .45f).coerceIn(0f, 1f)
                                val k = lerp(.4f, 1f, 1f - (1f - p) * (1f - p))
                                scaleX = k; scaleY = k
                            }
                        }.clip(shape).background(bg).border(maxOf(sz * .06f, 2.dp), if (c != null) Ink else if (cur) Color(0xFFFFF36A) else Color.White, shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (c != null) Text(c, style = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (sz * .72f).asSp(), color = Color(0xFF3A1F7A)))
                    }
                }
            }
            // Conejitos
            val u = q * min(14f, 88f / n / 1.04f)
            val gap = u * .04f
            Row(Modifier.align(Alignment.BottomCenter).offset(y = -H * .025f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                order.forEachIndexed { i, c ->
                    val r = ((((i * 37) % 7) - 3) * 2).toFloat()
                    Box(
                        Modifier.size(u, u * 1.3f).graphicsLayer {
                            val t = time.value
                            transformOrigin = TransformOrigin(.5f, 1f)
                            val hop = hops[i]
                            if (used[i] == true && !won) { alpha = 0f; return@graphicsLayer }
                            if (hop != null) {
                                val p = ((t - hop.first) / .6f).coerceIn(0f, 1f)
                                val dx = ((hop.second - (n - 1) / 2f) * (sz + sGap) - (i - (n - 1) / 2f) * (u + gap)).toPx()
                                val dy = ((H * .15f) - (H * .975f - u * .65f)).toPx()
                                translationX = dx * p
                                translationY = dy * p - u.toPx() * .8f * sin(PI.toFloat() * p)
                                val k = lerp(1f, .25f, p)
                                scaleX = k; scaleY = k
                                rotationZ = -10f * sin(PI.toFloat() * p)
                                alpha = lerp(1f, .3f, p)
                                return@graphicsLayer
                            }
                            if (won) {
                                val f = swing(t, .5f, i * .16f)
                                translationY = -size.height * .14f * f
                                scaleX = lerp(1.03f, .98f, f); scaleY = lerp(.97f, 1.03f, f)
                                return@graphicsLayer
                            }
                            val no = t - (nos[i] ?: -9f)
                            if (no in 0f..0.45f) rotationZ = -8f * sin(no / .45f * 2 * PI.toFloat())
                            val b = swing(t, 1.3f, i * .1f)
                            scaleX = lerp(1f, 1.03f, b); scaleY = lerp(1f, .97f, b)
                        }.tap(enabled = used[i] != true) { bunny(i) },
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val t = time.value
                            val no = t - (nos[i] ?: -9f)
                            val droop = if (no in 0f..1f) sin(no * PI.toFloat()) else 0f
                            group(s = size.width / 100f) { rabbit(i, t, droop) }
                        }
                        Box(
                            Modifier.align(Alignment.TopCenter).offset(y = u * 1.3f * .64f).size(u * .52f, u * 1.3f * .3f)
                                .graphicsLayer { rotationZ = r }.clip(RoundedCornerShape(u * .07f)).background(SIGNC[i % SIGNC.size])
                                .border(maxOf(u * .03f, 2.dp), Ink, RoundedCornerShape(u * .07f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(c, style = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (u * .3f).asSp(), color = Ink))
                        }
                    }
                }
            }
        }
    }
}
