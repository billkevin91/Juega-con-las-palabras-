package com.billkevin91.martypalabras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/* Telaraña: cueva de seda con terminal retro; las arañas con lentes traen cada letra. */

private class SpiderC(val b: Color, val d: Color, val l: Color, val g: Color)

private val SPAL = listOf(
    SpiderC(Color(0xFFA98BFF), Color(0xFF5A3FC0), Color(0xFFE2D6FF), Color(0xFF1B1448)),
    SpiderC(Color(0xFFFF9A3C), Color(0xFFA8521A), Color(0xFFFFD9B0), Color(0xFF5A2A0A)),
    SpiderC(Color(0xFF27B7D8), Color(0xFF14799A), Color(0xFFC6F6FF), Color(0xFF0A3550)),
    SpiderC(Color(0xFFFF7FC2), Color(0xFFB03A7A), Color(0xFFFFD6EE), Color(0xFF5A1040)),
    SpiderC(Color(0xFF7EE06A), Color(0xFF2F9A4A), Color(0xFFE6FFC8), Color(0xFF1A4A20)),
)
private val THREADS = listOf(5f, 8f, 6.5f, 9f, 5.5f, 7.5f)
private const val MAX_LETTERS = 12

private val LEGS: String = buildString {
    for (sd in listOf(-1, 1)) append(
        "M${sd * 8} -6Q${sd * 24} -24 ${sd * 32} -10M${sd * 9} -1Q${sd * 28} -12 ${sd * 34} 2" +
            "M${sd * 8} 6Q${sd * 20} 8 ${sd * 22} 24M${sd * 5} 9Q${sd * 12} 15 ${sd * 12} 26"
    )
}

/** Araña con lentes (mismo dibujo que en el HTML, unidades -34..34). */
private fun DrawScope.spider(c: SpiderC, t: Float) {
    path(LEGS, stroke = Ink, sw = 6f)
    path(LEGS, stroke = c.d, sw = 3.4f)
    ellipse(0f, -12f, 13f, 12f, c.b, Ink, 2.5f)
    path("M-7 -18Q0 -22 7 -18", stroke = Color.White, sw = 2.5f, alpha = .55f)
    circle(0f, 5f, 12f, c.l, Ink, 2.5f)
    zoom(1f, blink(t, 4.4f), 0f, 3f) {
        circle(-5.5f, 3f, 5f, Color.White); circle(5.5f, 3f, 5f, Color.White)
        circle(-5f, 3.8f, 2.1f, Color(0xFF1B1448)); circle(6f, 3.8f, 2.1f, Color(0xFF1B1448))
    }
    circle(-5.5f, 3f, 5f, stroke = c.g, sw = 1.9f); circle(5.5f, 3f, 5f, stroke = c.g, sw = 1.9f)
    path("M-.5 2.4Q0 1.4 .5 2.4M-10.5 2L-12 0M10.5 2L12 0", stroke = c.g, sw = 1.9f)
    path("M-8 0.4L-6.4 -1", stroke = Color(0xFFBFE9FF), sw = 1.4f)
    circle(-9f, 10f, 2.2f, Color(0xFFFF8FB5), alpha = .75f); circle(9f, 10f, 2.2f, Color(0xFFFF8FB5), alpha = .75f)
    path("M-3.5 11Q0 14 3.5 11", stroke = Color(0xFF1B1448), sw = 1.8f)
}

private class Web(val spokes: String, val rings: String, val drops: List<FloatArray>)

private fun web(cx: Float, cy: Float, n: Int, maxR: Float, rnd: Random): Web {
    val dirs = List(n) { i -> val a = i.toFloat() / n * 2 * PI.toFloat() + .12f; cos(a) to sin(a) * .82f }
    val sp = StringBuilder()
    dirs.forEach { (dx, dy) -> sp.append("M$cx ${cy}L${cx + dx * maxR * 1.3f} ${cy + dy * maxR * 1.3f}") }
    val rg = StringBuilder()
    val drops = mutableListOf<FloatArray>()
    fun pt(j: Int, r: Float) = dirs[j % n].let { (dx, dy) -> (cx + dx * r) to (cy + dy * r) }
    var r = 12f
    while (r < maxR) {
        val (x0, y0) = pt(0, r)
        rg.append("M$x0 $y0")
        for (i in 1..n) {
            val (px, py) = pt(i, r)
            val (qx, qy) = pt(i - 1, r)
            val mx = cx + ((px + qx) / 2 - cx) * .9f
            val my = cy + ((py + qy) / 2 - cy) * .9f
            rg.append("Q$mx $my $px $py")
        }
        for (k in 0 until n) if (rnd.nextFloat() < .1f) {
            val (px, py) = pt(k, r)
            drops += floatArrayOf(px, py, 1.8f + rnd.nextFloat() * 1.4f, rnd.nextFloat() * 2.6f)
        }
        r = r * 1.18f + 5f
    }
    return Web(sp.toString(), rg.toString(), drops)
}

private fun DrawScope.drawWeb(w: Web, op: Float, t: Float, won: Boolean) {
    val col = if (won) Color(0xFFFFF3A0) else Color(0xFFECE4FF)
    path(w.spokes, stroke = col, sw = 1.8f, alpha = op)
    path(w.rings, stroke = col, sw = 1.3f, alpha = op * .8f)
    w.drops.forEach { (x, y, r, d) -> circle(x, y, r, Color(0xFFC6F6FF), alpha = lerp(.3f, 1f, swing(t + d, 1.3f))) }
}

private class Cave(rnd: Random) {
    val back = web(320f, 120f, 16, 250f, rnd)
    val left = web(-10f, -10f, 12, 200f, rnd)
    val right = web(650f, -10f, 12, 190f, rnd)
    val small = web(412f, 186f, 8, 40f, rnd)
    val ceiling: String = buildString {
        append("M-10 -10H650V20")
        var x = 640
        while (x > 0) {
            val len = 10 + ((x * 37) % 7) * 6
            append("L${x - 8} 20L${x - 16} ${20 + len}L${x - 24} 20")
            x -= 32
        }
        append("L-10 20Z")
    }
}

private const val WALL = "M-10 -10V370H66Q40 320 58 270Q30 214 52 160Q24 110 44 60Q30 30 40 -10Z"

private fun DrawScope.hangSpider(x: Float, y: Float, s: Float, c: SpiderC, card: String, d: Float, t: Float, tm: TextMeasurer) {
    group(y = lerp(-8f, 8f, swing(t + d, 3.4f))) {
        line(x, -20f, x, y - 24 * s, Color(0xFFF0E6FF), 1.5f, .8f)
        group(x, y, s) {
            rect(-15f, 17f, 30f, 36f, 5f, Color(0xFFFFF6D8), Ink, 3f)
            textAt(tm, card, 0f, 36f, TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = units(26f), color = Color(0xFF3A1F7A)))
            spider(c, t + d)
        }
    }
}

private fun DrawScope.cave(cv: Cave, t: Float, won: Boolean, tm: TextMeasurer) {
    val rock = vGrad(0f, 360f, 0f to Color(0xFF4A2A96), 1f to Color(0xFF22105A))
    rect(0f, 0f, 640f, 360f, brush = rGrad(320f, 151f, 450f, 0f to Color(0xFF9A6AF0), .45f to Color(0xFF5A2FB0), 1f to Color(0xFF1E0C50)))
    ellipse(150f, 180f, 90f, 60f, Color(0xFF3C1F86), alpha = .55f); ellipse(500f, 170f, 100f, 66f, Color(0xFF3C1F86), alpha = .55f); ellipse(320f, 90f, 130f, 40f, Color(0xFF3C1F86), alpha = .55f)
    circle(320f, 250f, 150f, brush = rGrad(320f, 250f, 150f, 0f to Color(0x737DFFE3), 1f to Color(0x007DFFE3)))
    drawWeb(cv.back, .28f, t, won); drawWeb(cv.left, .55f, t, won); drawWeb(cv.right, .55f, t, won)
    path(cv.ceiling, brush = rock); path(cv.ceiling, stroke = Ink, sw = 4f)
    for (m in listOf(false, true)) {
        group(if (m) 640f else 0f, 0f, if (m) -1f else 1f, 1f) {
            path(WALL, brush = rock); path(WALL, stroke = Ink, sw = 4f)
            path("M30 300Q22 260 36 230M24 140Q18 110 30 90", stroke = Color(0xFF7A5AE0), sw = 3f, alpha = .7f)
        }
    }
    path("M-10 370V318Q120 296 240 310T420 306T650 314V370Z", brush = rock); path("M-10 370V318Q120 296 240 310T420 306T650 314V370Z", stroke = Ink, sw = 4f)
    crystal(40f, 334f, .75f, Color(0xFF7DFFE3), Color(0xFF2DB8A8), t, 0f); crystal(84f, 340f, .45f, Color(0xFFFFE36A), Color(0xFFE0A400), t, 1f)
    crystal(604f, 330f, .85f, Color(0xFFFF7FC2), Color(0xFFC04A98), t, .5f); crystal(560f, 340f, .5f, Color(0xFF7DFFE3), Color(0xFF2DB8A8), t, 1.5f)
    crystal(30f, 120f, .4f, Color(0xFFFF7FC2), Color(0xFFC04A98), t, 2f); crystal(612f, 150f, .42f, Color(0xFF7DFFE3), Color(0xFF2DB8A8), t, .8f)
    mush(150f, 336f, .32f, Color(0xFFFF7FC2), Color(0xFFFFE8F2)); mush(176f, 342f, .22f, Color(0xFF7DFFE3), Color(0xFFE6FFFA)); mush(470f, 338f, .3f, Color(0xFFFFE36A), Color(0xFFFFF8D6))
    hangSpider(118f, 220f, 1.05f, SPAL[3], "A", 0f, t, tm)
    hangSpider(524f, 206f, 1f, SPAL[2], "Ñ", 1.7f, t, tm)
    // terminal retro
    val body = vGrad(186f, 312f, 0f to Color(0xFFE2D6FF), .5f to Color(0xFFA98BFF), 1f to Color(0xFF6A4AD0))
    line(320f, 334f, 320f, 370f, Ink, 10f); line(320f, 334f, 320f, 370f, Color(0xFF8A6AF0), 4f)
    path("M292 312H348L358 334H282Z", brush = body); path("M292 312H348L358 334H282Z", stroke = Ink, sw = 4f)
    line(372f, 186f, 392f, 150f, Ink, 4f)
    circle(393f, 148f, 6f, Color(0xFFFFE36A), Ink, 2.5f, alpha = lerp(.35f, 1f, swing(t, .8f)))
    rect(228f, 186f, 184f, 126f, 14f, brush = body); rect(228f, 186f, 184f, 126f, 14f, stroke = Ink, sw = 4f)
    path("M240 196Q320 188 400 196", stroke = Color.White, sw = 3f, alpha = .6f)
    rect(244f, 199f, 152f, 97f, 12f, Ink)
    circle(252f, 304f, 3.5f, Color(0xFF7DFFE3), alpha = lerp(.35f, 1f, swing(t, .8f)))
    circle(264f, 304f, 3.5f, Color(0xFFFF7FC2), alpha = lerp(.35f, 1f, swing(t, .8f, .4f)))
    rect(350f, 301f, 44f, 2.5f, 1f, Ink); rect(350f, 306f, 44f, 2.5f, 1f, Ink)
    group(244f, 178f, .62f) {
        spin(lerp(-12f, 14f, swing(t, .8f)), -26f, -6f) { path("M-26 -6Q-40 -30 -34 -46", stroke = Ink, sw = 7f) }
        spider(SPAL[4], t + .7f)
    }
    drawWeb(cv.small, .7f, t, won)
}

private val KB_ROWS = listOf("qwertyuiop", "asdfghjklñ", "zxcvbnm")

@Composable
fun TelaGame(g: Game, s: Session) {
    val w = s.word.text
    val sp = remember { g.canSpeak }
    var typed by remember { mutableStateOf("") }
    var marks by remember { mutableStateOf<String?>(null) }
    var att by remember { mutableIntStateOf(0) }
    var won by remember { mutableStateOf(false) }
    var clue by remember { mutableStateOf(!sp) }
    var fxKind by remember { mutableIntStateOf(0) }
    var fxAt by remember { mutableFloatStateOf(-9f) }
    val born = remember { mutableStateListOf<Float>() }
    val time = rememberTime()
    val tm = rememberTextMeasurer()
    val cave = remember { Cave(Random(s.i * 31 + 7)) }
    val flies = remember { Fireflies(9, 20f, 90f, Color(0xFFBDF6FF)) }
    var pressed by remember { mutableStateOf("") }
    var pressedAt by remember { mutableFloatStateOf(-9f) }

    LaunchedEffect(Unit) {
        g.say(if (sp) "Escucha la palabra y escríbela en el teclado. ¡Las arañas te traerán cada letra!" else "Tu teléfono no puede hablar. Mira la pantalla y escribe la palabra en el teclado.")
        if (sp) {
            delay(400)
            if (!s.locked) g.speak(w)
        }
    }

    fun check() {
        if (s.locked || typed.isEmpty()) return
        val v = typed
        marks = v
        if (v == w) {
            won = true; clue = true
            fxKind = 1; fxAt = time.value
            g.win(!s.tried)
            return
        }
        s.tried = true
        att++
        g.tone(200, 150)
        g.react(Mood.SAD)
        fxKind = 2; fxAt = time.value
        g.say(
            when {
                norm(v) == norm(w) -> "¡Casi! Revisa las tildes, la ñ y la ü."
                v.length != w.length -> "Casi. La palabra tiene ${w.length} letras."
                else -> "Todavía no. Escucha otra vez."
            }
        )
    }

    fun key(c: String) {
        if (s.locked) return
        pressed = c; pressedAt = time.value
        when (c) {
            "↵" -> return check()
            "⌫" -> {
                if (typed.isEmpty()) return
                typed = typed.dropLast(1)
                if (born.isNotEmpty()) born.removeAt(born.lastIndex)
            }
            else -> {
                if (typed.length >= MAX_LETTERS) return
                typed += c
                born += time.value
                g.tone(660 + typed.length * 30)
            }
        }
        marks = null
    }

    QFrame(g, s, hear = sp, clue = if (sp) clue else null, onClue = { clue = true }) {
        Column(Modifier.fillMaxWidth()) {
            BoxWithConstraints(Modifier.scene(16f / 9f, Ink, 22.dp, 3.dp).background(Color(0xFF2A1460))) {
                val W = maxWidth
                val H = maxHeight
                val q = W / 100f
                Canvas(Modifier.fillMaxSize()) {
                    val t = time.value
                    val fxT = t - fxAt
                    viewBox(640f, 360f) { cave(cave, t, won, tm) }
                    with(flies) { drawFlies(t) }
                    if (fxKind == 2 && fxT in 0f..0.5f) drawRect(Color(0xFFFF5078), alpha = .12f * sin(fxT / .5f * PI.toFloat()))
                }
                // Pantalla de la terminal
                val scrShape = RoundedCornerShape(q * 1.6f)
                Box(
                    Modifier.at(W, H, 50f, 68.6f, q * 21.9f, q * 13.3f).clip(scrShape).background(
                        Brush.radialGradient(if (won) listOf(Color(0xFF3FC77A), Color(0xFF0D3A2A)) else listOf(Color(0xFF1F6A80), Color(0xFF0A1D36)))
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (clue) s.word.pic else "?", style = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (q * 8.5f).asSp(), color = Color(0xFF7DFFE3), shadow = Shadow(Color(0xE67DFFE3), blurRadius = 14f)))
                    Canvas(Modifier.fillMaxSize()) {
                        var y = 0f
                        while (y < size.height) { drawRect(Color.Black.copy(alpha = .2f), topLeft = androidx.compose.ui.geometry.Offset(0f, y), size = androidx.compose.ui.geometry.Size(size.width, 2f)); y += 4f }
                        val fxT = time.value - fxAt
                        if (fxKind == 2 && fxT in 0f..0.5f) drawRect(Color(0xFFFF5078), alpha = .5f * sin(fxT / .5f * PI.toFloat()))
                    }
                }
                // Arañas con las letras
                val n = typed.length + if (!s.locked && typed.length < MAX_LETTERS) 1 else 0
                val u = q * min(8f, 90f / maxOf(n, 1) / 1.12f)
                Row(Modifier.align(Alignment.TopCenter), horizontalArrangement = Arrangement.spacedBy(u * .12f)) {
                    typed.forEachIndexed { i, ch ->
                        val c = ch.toString()
                        val ok = marks?.let { m -> if (m.getOrNull(i) == w.getOrNull(i)) 1 else 2 } ?: 0
                        Column(
                            Modifier.width(u).graphicsLayer {
                                val t = time.value
                                transformOrigin = TransformOrigin(.5f, 0f)
                                val p = ((t - (born.getOrNull(i) ?: -9f)) / .6f).coerceIn(0f, 1f)
                                val e = 1f - (1f - p) * (1f - p) * (1f - p)
                                translationY = -(1f - e) * size.height * 1.1f
                                rotationZ = lerp(-4f, 4f, swing(t, 3.2f, i * .22f))
                                val fxT = t - fxAt
                                if (fxKind == 1 && fxT in 0f..0.6f) translationY -= 2.4f * q.toPx() * sin(fxT / .6f * PI.toFloat())
                                if (fxKind == 2 && fxT in 0f..0.5f) rotationZ += 6f * sin(fxT / .5f * 2 * PI.toFloat())
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.width(maxOf(q * .3f, 1.dp)).height(q * THREADS[i % 6]).background(Color(0xD9F0E6FF)))
                            Canvas(Modifier.size(u * 1.12f, u * .94f).offset(y = -u * .05f)) {
                                group(size.width / 2, size.height * 26f / 56f, size.width / 68f) { spider(SPAL[i % SPAL.size], time.value + i * .3f) }
                            }
                            Card(c, ok, u, Modifier.offset(y = -u * .26f))
                        }
                    }
                    if (n > typed.length) {
                        val i = typed.length
                        Column(Modifier.width(u), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.width(maxOf(q * .3f, 1.dp)).height(q * THREADS[i % 6] + u * .68f).background(Color(0xD9F0E6FF)))
                            Card("", 3, u, Modifier.graphicsLayer { alpha = lerp(.35f, 1f, swing(time.value, 1f)) })
                        }
                    }
                }
            }
            // Teclado de la terminal
            val deck = RoundedCornerShape(0.dp, 0.dp, 22.dp, 22.dp)
            Column(
                Modifier.fillMaxWidth().clip(deck).background(Brush.verticalGradient(listOf(Color(0xFF4F34A8), Color(0xFF26155E))))
                    .border(3.dp, Ink, deck).padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF7DFFE3)))
                    Text("TECLADO DE SEDA", style = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 2.4.sp, color = Color(0xFFC9B8FF)))
                    Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFF7FC2)))
                }
                KB_ROWS.forEachIndexed { r, row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { ch -> Key(ch.toString(), Modifier.weight(1f), time, pressed, pressedAt) { key(ch.toString()) } }
                        if (r == 2) Key("⌫", Modifier.weight(3f), time, pressed, pressedAt, label = "⌫ Borrar") { key("⌫") }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    "áéíóúü".forEach { ch -> Key(ch.toString(), Modifier.weight(1f), time, pressed, pressedAt, accent = true) { key(ch.toString()) } }
                    Key("↵", Modifier.weight(4f), time, pressed, pressedAt, label = "Comprobar ↵", go = true) { key("↵") }
                }
            }
        }
        if (att >= 3 && !won) {
            Ghost("Ver respuesta") {
                if (s.locked) return@Ghost
                s.locked = true
                typed = w
                born.clear(); repeat(w.length) { born += -9f }
                marks = w
                clue = true
                g.say("Se escribe $w. ¡Sigamos!")
                g.later(s, 2400)
            }
        }
        Spacer(Modifier.height(0.dp))
    }
}

/** Tarjeta que sostiene cada araña: 0 normal, 1 bien, 2 mal, 3 espera (cursor). */
@Composable
private fun Card(c: String, state: Int, u: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(u * .15f)
    val bg: Brush = when (state) {
        1 -> Brush.verticalGradient(listOf(Color(0xFFE2FFE9), C.leaf))
        2 -> Brush.verticalGradient(listOf(Color(0xFFE0BDE8), Color(0xFF8A4A96)))
        3 -> Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color(0x14FFFFFF)))
        else -> Brush.verticalGradient(listOf(Color(0xFFFFFDF2), Color(0xFFF1E3B8)))
    }
    Box(
        modifier.size(u * .97f, u * 1.18f).clip(shape).background(bg)
            .border(maxOf(u * .06f, 2.dp), if (state == 3) Color.White.copy(alpha = .75f) else Ink, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (c.isNotEmpty()) Text(
            c.uppercase(),
            style = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (u * .8f).asSp(), color = if (state == 1) C.onColor else if (state == 2) Color.White else Color(0xFF3A1F7A)),
        )
    }
}

@Composable
private fun Key(
    c: String, modifier: Modifier, time: androidx.compose.runtime.State<Float>, pressed: String, pressedAt: Float,
    label: String = c, accent: Boolean = false, go: Boolean = false, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(9.dp)
    val bg = if (go) Brush.verticalGradient(listOf(Color(0xFFFFF08A), Color(0xFFF0A400))) else Brush.verticalGradient(listOf(Color(0xFF3D2C90), Color(0xFF1F1358)))
    val fg = if (go) Color(0xFF2A1450) else if (accent) Color(0xFFFFC6EC) else Color(0xFFA6F6FF)
    Box(
        modifier.height(46.dp).graphicsLayer {
            val down = pressed == c && time.value - pressedAt < .12f
            translationY = if (down) 3.dp.toPx() else 0f
        }.clip(shape).background(Color(0xFF0E0830)).padding(bottom = 4.dp).clip(shape).background(bg)
            .border(2.dp, Color(0xFF0E0830), shape).tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (go || c == "⌫") label else c.uppercase(),
            style = TextStyle(
                fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = if (go || c == "⌫") 15.sp else 20.sp, color = fg,
                shadow = if (go) null else Shadow(fg.copy(alpha = .9f), blurRadius = 10f),
            ),
            maxLines = 1,
        )
    }
}
