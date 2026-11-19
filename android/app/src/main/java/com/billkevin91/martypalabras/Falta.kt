package com.billkevin91.martypalabras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* Letra perdida: el tren de las letras, en un planeta de dulces. Falta una letra en un vagón. */

private const val TALL_CANDY = 800f

/** Letras que se confunden entre sí (para las opciones de los modos normal y difícil). */
private val CONFUSE = mapOf(
    "B" to "V", "V" to "B", "S" to "Z", "Z" to "S", "C" to "S", "G" to "J", "J" to "G", "Y" to "L", "L" to "Y",
    "N" to "Ñ", "Ñ" to "N", "H" to "J", "R" to "L", "M" to "N",
    "Á" to "A", "É" to "E", "Í" to "I", "Ó" to "O", "Ú" to "U", "Ü" to "U",
    "A" to "Á", "E" to "É", "I" to "Í", "O" to "Ó", "U" to "Ú",
)
private const val TRICKY = "ÁÉÍÓÚÜÑHBVGJYZC"

/** Dónde va cada cosa de la escena (en unidades del dibujo: 640 de ancho). */
private class TrainLayout(n: Int, tall: Boolean) {
    val vh = if (tall) TALL_CANDY else 480f
    val railY = if (tall) 430f else 280f
    /** La locomotora se dibuja al 80 % para dejar más ancho a los vagones (y letras más grandes). */
    val locoK = .8f
    private val locoW = 128f * locoK
    val ww = min(92f, (624f - locoW) / n)
    val x0 = (640f - n * ww - locoW) / 2
    val locoX = x0 + n * ww
    fun wagonX(i: Int) = x0 + i * ww
    val bodyTop = railY - 16 - ww * .5f
    /** Centro y tamaño de la caja con la letra encima de cada vagón. */
    fun crateC(i: Int) = wagonX(i) + ww / 2
    val crateY = bodyTop - ww * .42f
    val crateS = ww * .9f
    // Cajas de opciones en el andén
    val optY = if (tall) 600f else 395f
    val optS = if (tall) 118f else 88f
    val picX = if (tall) 320f else 92f
    val picY = if (tall) 140f else 88f
    val picR = if (tall) 72f else 54f
}

@Composable
fun FaltaGame(g: Game, s: Session) {
    val word = remember { letters(s.word.text.uppercase()) }
    // Fácil: falta una letra sencilla. Difícil: falta una que se confunde (tilde, ñ, b/v, h, g/j...).
    val gap = remember {
        val tricky = word.indices.filter { word[it] in TRICKY }
        val plain = word.indices.filter { word[it] !in TRICKY }
        when (s.level) {
            Level.FACIL -> plain.ifEmpty { word.indices.toList() }.random()
            Level.NORMAL -> word.indices.random()
            Level.DIFICIL -> tricky.ifEmpty { word.indices.toList() }.random()
        }
    }
    val miss = word[gap]
    val opts = remember {
        val count = when (s.level) { Level.FACIL -> 2; Level.NORMAL -> 3; Level.DIFICIL -> 4 }
        val set = linkedSetOf(miss)
        val abc = letters("ABCDEFGHILMNOPRSTUV")
        if (s.level == Level.FACIL) {
            val avoid = setOf(miss, norm(miss), CONFUSE[miss])
            set.add(abc.filter { it !in avoid }.random())
        } else {
            CONFUSE[miss]?.let { set.add(it) }
            norm(miss).let { if (it != miss) set.add(it) }
            CONFUSE[norm(miss)]?.let { if (set.size < count) set.add(it) }
            while (set.size < count) set.add(abc.random())
        }
        set.toList().take(count).shuffled()
    }
    var filled by remember { mutableStateOf(false) }
    var hopAt by remember { mutableFloatStateOf(-9f) }
    var hopFrom by remember { mutableStateOf(-1) }
    var departAt by remember { mutableFloatStateOf(-9f) }
    val shakes = remember { mutableStateMapOf<Int, Float>() }
    val time = rememberTime()
    val scope = rememberCoroutineScope()
    val tm = androidx.compose.ui.text.rememberTextMeasurer()

    LaunchedEffect(Unit) { g.intro(s, Frases.FALTA_INICIO) }

    fun choose(i: Int) {
        if (s.locked || hopFrom >= 0) return
        if (opts[i] == miss) {
            hopFrom = i
            hopAt = time.value
            g.sfx(Sfx.BOING)
            scope.launch {
                delay(450)
                filled = true
                g.win(!s.tried)
                delay(150)
                g.tone(880, 990, 880)
                g.sfx(Sfx.WHOOSH)
                departAt = time.value
            }
        } else {
            shakes[i] = time.value
            g.wrong()
        }
    }

    QFrame(g, s, hear = true) {
        SceneBox(4f / 3f, 640f / TALL_CANDY, Color(0xFF6B3FC0)) { W, H, tall ->
            val L = remember(tall, word.size) { TrainLayout(word.size, tall) }
            val px = W / 640f
            // Desplazamiento del tren cuando arranca (unidades del dibujo).
            fun trainDx(t: Float): Float {
                val p = ((t - departAt) / 1.4f).coerceIn(0f, 1f)
                return if (departAt < 0) 0f else 720f * p * p
            }
            Canvas(Modifier.fillMaxSize()) {
                val t = time.value
                viewBox(640f, L.vh) {
                    candyWorld(t, L)
                    group(trainDx(t)) { train(t, L, word.size, gap, filled, departAt >= 0, tm) }
                }
            }
            // Dibujo de la palabra en el reloj de la estación
            Box(Modifier.offset(px * L.picX - px * L.picR, px * L.picY - px * L.picR).size(px * L.picR * 2), contentAlignment = Alignment.Center) {
                Text(s.word.pic, style = TextStyle(fontSize = (px * L.picR * .95f).asSp()))
            }
            // Letras sobre los vagones
            word.forEachIndexed { j, c ->
                if (j == gap && !filled) return@forEachIndexed
                Box(
                    Modifier.offset(px * (L.crateC(j) - L.crateS / 2), px * (L.crateY - L.crateS / 2)).size(px * L.crateS)
                        .graphicsLayer { translationX = trainDx(time.value) * (W.toPx() / 640f) },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) { group(s = size.width / 100f) { crate(if (j == gap) 1 else 0) } }
                    Text(c, style = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (px * L.crateS * .62f).asSp(), color = Color(0xFF4A2C12)))
                }
            }
            // Cajas de opciones en el andén
            val n = opts.size
            val gapX = if (tall) 22f else 26f
            val rowW = n * L.optS + (n - 1) * gapX
            opts.forEachIndexed { i, c ->
                val cx = 320f - rowW / 2 + L.optS / 2 + i * (L.optS + gapX)
                Box(
                    Modifier.offset(px * (cx - L.optS / 2), px * (L.optY - L.optS / 2)).size(px * L.optS).graphicsLayer {
                        val t = time.value
                        val k = W.toPx() / 640f
                        if (hopFrom == i) {
                            if (filled) { alpha = 0f; return@graphicsLayer }
                            val p = ((t - hopAt) / .45f).coerceIn(0f, 1f)
                            translationX = (L.crateC(gap) - cx) * k * p
                            translationY = (L.crateY - L.optY) * k * p - 60f * k * sin(PI.toFloat() * p)
                            val sc = lerp(1f, L.crateS / L.optS, p)
                            scaleX = sc; scaleY = sc
                            rotationZ = 360f * p
                            return@graphicsLayer
                        }
                        val sh = t - (shakes[i] ?: -9f)
                        if (sh in 0f..0.5f) {
                            rotationZ = 10f * sin(sh / .5f * 4 * PI.toFloat()) * (1 - sh / .5f)
                            translationY = -8f * k * sin(sh / .5f * PI.toFloat())
                        } else translationY = -4f * k * swing(t, 1.2f, i * .2f)
                    }.tap(enabled = !s.locked) { choose(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sh = time.value - (shakes[i] ?: -9f)
                        group(s = size.width / 100f) { crate(if (sh in 0f..0.6f) 2 else 0) }
                    }
                    Text(c, style = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = (px * L.optS * .55f).asSp(), color = Color(0xFF4A2C12)))
                }
            }
        }
    }
}

/** Caja de madera (0..100): 0 normal, 1 recién colocada (brilla), 2 equivocada (rojiza). */
private fun DrawScope.crate(state: Int) {
    ellipse(50f, 100f, 44f, 6f, Color.Black, alpha = .3f)
    if (state == 1) circle(50f, 50f, 62f, Color(0xFFFFF36A), alpha = .35f)
    val wood = if (state == 2) vGrad(0f, 100f, 0f to Color(0xFFFFB0A8), 1f to Color(0xFFD9675E))
    else vGrad(0f, 100f, 0f to Color(0xFFFFD99A), 1f to Color(0xFFE0A050))
    rect(4f, 6f, 92f, 90f, 10f, brush = wood)
    rect(4f, 6f, 92f, 90f, 10f, stroke = Ink, sw = 5f)
    path("M10 30H90M10 72H90", stroke = Color(0xFFB0702E), sw = 3f, alpha = .55f)
    rect(10f, 12f, 80f, 78f, 6f, fill = Color(0xFFFFF4D8), alpha = .75f)
    for ((x, y) in listOf(14f to 16f, 86f to 16f, 14f to 86f, 86f to 86f)) circle(x, y, 3.5f, Color(0xFF8A5A2E))
}

private fun DrawScope.lollipop(x: Float, y: Float, s: Float, a: Color, b: Color, t: Float) = group(x, y, s) {
    rect(-3f, -60f, 6f, 60f, 3f, Color(0xFFFFF4E0), Ink, 2f)
    spin(t * 30f, 0f, -72f) {
        circle(0f, -72f, 22f, a, Ink, 3f)
        path("M0 -72m-14 0a14 14 0 1 1 28 0a10 10 0 1 1 -20 0a6 6 0 1 1 12 0", stroke = b, sw = 4f)
    }
}

private fun DrawScope.gumdrop(x: Float, y: Float, s: Float, c: Color) = group(x, y, s) {
    path("M-20 0Q-22 -30 0 -30Q22 -30 20 0Z", c, Ink, 2.5f)
    ellipse(-7f, -20f, 5f, 3f, Color.White, alpha = .6f)
}

private fun DrawScope.candyWorld(t: Float, L: TrainLayout) {
    val vh = L.vh
    val r = L.railY
    rect(0f, 0f, 640f, vh, brush = vGrad(0f, r, 0f to Color(0xFF5B3FC0), .55f to Color(0xFFE77FD0), 1f to Color(0xFFFFD1E8)))
    listOf(Triple(40f, 30f, 1.4f), Triple(150f, 60f, 1f), Triple(240f, 24f, 1.2f), Triple(420f, 40f, 1f), Triple(600f, 150f, 1.2f), Triple(560f, 30f, 1f))
        .forEachIndexed { i, (x, y) -> circle(x, y, 1.6f, Color.White, alpha = lerp(.3f, 1f, swing(t, 1.3f, i * .2f))) }
    // planeta de caramelo a rayas
    group(540f, if (L.vh > 480f) 260f else 80f) {
        circle(0f, 0f, 44f, Color(0xFFFFF0F7))
        clipPath(Paths.of("M-44 0a44 44 0 1 0 88 0a44 44 0 1 0 -88 0Z")) {
            for (k in -4..4) spin(-30f, 0f, 0f) { rect(k * 22f - 6f, -60f, 11f, 120f, 0f, Color(0xFFFF6FAE)) }
        }
        circle(0f, 0f, 44f, stroke = Ink, sw = 3f)
        spin(-18f, 0f, 0f) { ellipse(0f, 0f, 70f, 12f, stroke = Color(0xFF7DFFE3), sw = 5f) }
    }
    // nubes de algodón de azúcar
    for ((i, c) in listOf(Triple(120f, r - 170f, 1f), Triple(420f, r - 210f, .8f), Triple(300f, r - 120f, .6f)).withIndex()) {
        group(c.first + 14f * wave(t, 18f, i * .3f), c.second, c.third) {
            ellipse(0f, 0f, 46f, 16f, Color(0xFFFFE3F1)); ellipse(-18f, -10f, 22f, 16f, Color(0xFFFFE3F1)); ellipse(14f, -12f, 20f, 15f, Color(0xFFFFE3F1))
        }
    }
    // colinas
    path("M0 ${r - 40}Q80 ${r - 110} 170 ${r - 60}T340 ${r - 70}T500 ${r - 60}T640 ${r - 90}V${r}H0Z", Color(0xFFA98BFF))
    path("M0 ${r - 10}Q110 ${r - 70} 220 ${r - 30}T440 ${r - 36}T640 ${r - 30}V${r}H0Z", Color(0xFF9CF0D0))
    lollipop(60f, r - 24f, .9f, Color(0xFFFF6FAE), Color.White, t)
    lollipop(590f, r - 20f, 1f, Color(0xFF7DFFE3), Color(0xFFFF6FAE), t + 1f)
    lollipop(470f, r - 30f, .6f, Color(0xFFFFE36A), Color(0xFFFF9A3C), t + 2f)
    // suelo
    rect(0f, r, 640f, vh - r, brush = vGrad(r, vh, 0f to Color(0xFF7FE3B8), 1f to Color(0xFF2E9C78)))
    // vía del tren
    for (x in 0..640 step 26) rect(x.toFloat(), r + 2f, 16f, 10f, 2f, Color(0xFF8A5A2E))
    rect(0f, r - 3f, 640f, 6f, 0f, Color(0xFF9AA0B5)); line(0f, r - 3f, 640f, r - 3f, Ink, 1.5f)
    // andén
    val oy = L.optY + L.optS * .55f
    rect(30f, oy, 580f, 24f, 8f, brush = vGrad(oy, oy + 24f, 0f to Color(0xFFFFD99A), 1f to Color(0xFFC88A44)))
    rect(30f, oy, 580f, 24f, 8f, stroke = Ink, sw = 3f)
    for (x in listOf(60f, 320f, 580f)) rect(x - 6f, oy + 24f, 12f, 26f, 3f, Color(0xFF8A5A2E), Ink, 2f)
    gumdrop(40f, vh - 20f, 1f, Color(0xFFFF6FAE)); gumdrop(90f, vh - 12f, .7f, Color(0xFFFFE36A))
    gumdrop(600f, vh - 18f, .9f, Color(0xFF7DFFE3)); gumdrop(550f, vh - 10f, .6f, Color(0xFFA98BFF))
    // reloj de la estación (aquí va el dibujo)
    rect(L.picX - 5f, L.picY + L.picR - 4f, 10f, 40f, 3f, Color(0xFF8A5A2E), Ink, 2f)
    circle(L.picX, L.picY, L.picR + 8f, Color(0xFFFFE36A), Ink, 4f)
    circle(L.picX, L.picY, L.picR, Color(0xFFFFFDF2))
    for (k in 0 until 12) {
        val a = k * 30f * PI.toFloat() / 180f
        circle(L.picX + cos(a) * (L.picR - 7f), L.picY + sin(a) * (L.picR - 7f), 2.5f, Color(0xFFE0A050))
    }
}

/** El tren: vagones con letras y la locomotora (con Marty de maquinista). */
private fun DrawScope.train(t: Float, L: TrainLayout, n: Int, gap: Int, filled: Boolean, moving: Boolean, tm: androidx.compose.ui.text.TextMeasurer) {
    val r = L.railY
    val colors = listOf(Color(0xFFFF6FAE), Color(0xFF7DFFE3), Color(0xFFFFE36A), Color(0xFFA98BFF), Color(0xFFFF9A3C))
    val wheelR = maxOf(7f, L.ww * .14f)
    val spinA = if (moving) t * 720f else 0f
    for (i in 0 until n) {
        val x = L.wagonX(i)
        if (i > 0) line(x - 2f, r - 18f, x + 4f, r - 18f, Ink, 4f)
        rect(x + 3f, L.bodyTop, L.ww - 6f, L.ww * .5f, 6f, colors[i % colors.size], Ink, 3f)
        rect(x + 7f, L.bodyTop + 4f, L.ww - 14f, 4f, 2f, Color.White, alpha = .5f)
        for (wx in listOf(x + L.ww * .28f, x + L.ww * .72f)) {
            circle(wx, r - wheelR, wheelR, Color(0xFF3A2A6A), Ink, 2.5f)
            spin(spinA, wx, r - wheelR) { line(wx - wheelR * .7f, r - wheelR, wx + wheelR * .7f, r - wheelR, Color(0xFFB9B0EC), 2f) }
        }
        // hueco de la letra que falta
        if (i == gap && !filled) {
            val cy = L.crateY
            val pulse = lerp(.6f, 1f, swing(t, .8f))
            circle(L.crateC(i), cy, L.crateS * .75f, Color(0xFFFFF36A), alpha = .35f * pulse)
            rect(L.crateC(i) - L.crateS / 2, cy - L.crateS / 2, L.crateS, L.crateS, 8f, Color(0xFFFF4F9A))
            rect(L.crateC(i) - L.crateS / 2, cy - L.crateS / 2, L.crateS, L.crateS, 8f, stroke = Color(0xFFFFF36A), sw = 4f, alpha = pulse)
            textAt(tm, "?", L.crateC(i), cy, androidx.compose.ui.text.TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = units(L.crateS * .7f), color = Color.White))
        }
    }
    // locomotora (dibujada al tamaño locoK, apoyada en la vía)
    group(L.locoX, r, L.locoK) { loco(t, 0f, 0f, moving, spinA) }
}

private fun DrawScope.loco(t: Float, x: Float, r: Float, moving: Boolean, spinA: Float) {
    line(x - 4f, r - 18f, x + 6f, r - 18f, Ink, 4f)
    path("M${x + 108} ${r - 24}L${x + 128} ${r - 4}H${x + 104}Z", Color(0xFFFFE36A), Ink, 2.5f)
    rect(x + 8f, r - 64f, 84f, 44f, 18f, Color(0xFFFF6B8B), Ink, 3f)
    rect(x + 30f, r - 64f, 6f, 44f, 0f, Color(0xFFFFE36A))
    rect(x + 56f, r - 64f, 6f, 44f, 0f, Color(0xFFFFE36A))
    circle(x + 94f, r - 42f, 18f, Color(0xFFFFC93C), Ink, 3f)
    circle(x + 100f, r - 46f, 6f, Color(0xFFFFFDE0), alpha = lerp(.6f, 1f, swing(t, .6f)))
    rect(x + 66f, r - 94f, 16f, 32f, 3f, Color(0xFF3A2A6A), Ink, 2.5f)
    rect(x + 61f, r - 100f, 26f, 9f, 3f, Color(0xFF3A2A6A), Ink, 2.5f)
    rect(x + 2f, r - 104f, 48f, 86f, 8f, Color(0xFF4CC3FF), Ink, 3f)
    rect(x - 4f, r - 112f, 60f, 12f, 6f, Color(0xFFA98BFF), Ink, 3f)
    rect(x + 10f, r - 96f, 32f, 30f, 6f, Color(0xFFE8F8FF), Ink, 2f)
    group(x + 26f - 50f * .34f, r - 80f - 46f * .34f, .34f) { martyHead(t, if (moving) Mood.HAPPY else Mood.NONE) }
    for ((wx, wr) in listOf(x + 28f to 16f, x + 72f to 16f, x + 100f to 9f)) {
        circle(wx, r - wr, wr, Color(0xFF3A2A6A), Ink, 3f)
        spin(spinA, wx, r - wr) {
            line(wx - wr * .75f, r - wr, wx + wr * .75f, r - wr, Color(0xFFB9B0EC), 2.5f)
            line(wx, r - wr - wr * .75f, wx, r - wr + wr * .75f, Color(0xFFB9B0EC), 2.5f)
        }
        circle(wx, r - wr, wr * .25f, Color(0xFFFFC93C))
    }
    line(x + 28f, r - 16f, x + 72f, r - 16f, Color(0xFFB9B0EC), 3f)
    // humo
    for (k in 0 until 4) {
        val p = ((t * (if (moving) 1.8f else 1f) + k * .25f) % 1f)
        circle(x + 74f - 30f * p, r - 106f - 70f * p, 8f + 16f * p, Color.White, alpha = .75f * (1f - p))
    }
}
