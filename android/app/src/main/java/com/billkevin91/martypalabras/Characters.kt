package com.billkevin91.martypalabras

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/* ---------- Marty, el presentador alienígena ---------- */

private val GREEN = Color(0xFF7EE06A)
private val GREEN2 = Color(0xFF5ED38B)
private val SUIT = Color(0xFFFF9F4A)
private val SKY = Color(0xFF4CC3FF)
private val YEL = Color(0xFFFFE36A)
private val PINK = Color(0xFFFF7FC2)
private val BERRY = Color(0xFFFF6B8B)
private val NAVY = Color(0xFF1B1448)

/** Mirada que va de un lado a otro (como la animación "look" del HTML). */
fun look(t: Float, period: Float): Pair<Float, Float> {
    val k = listOf(0f to (0f to 0f), .25f to (-2.5f to -1f), .5f to (2.5f to 1f), .75f to (0f to -2f), 1f to (0f to 0f))
    val p = (t / period) % 1f
    for (i in 1 until k.size) if (p <= k[i].first) {
        val (p0, a) = k[i - 1]
        val (p1, b) = k[i]
        val f = (p - p0) / (p1 - p0)
        return lerp(a.first, b.first, f) to lerp(a.second, b.second, f)
    }
    return 0f to 0f
}

/** Parpadeo: escala vertical de los ojos. */
fun blink(t: Float, period: Float): Float {
    val p = (t / period) % 1f
    return if (p in .92f..1f) 1f - .9f * (1f - abs(p - .95f) / .03f).coerceIn(0f, 1f) else 1f
}

fun DrawScope.martyHead(t: Float, mood: Mood) {
    val a1 = lerp(-9f, 9f, swing(t, 1.1f))
    val a2 = lerp(-9f, 9f, swing(t, 1.1f, .5f))
    spin(a1, 40f, 24f) {
        path("M40 24Q34 10 28 5", stroke = Ink, sw = 3f)
        circle(27f, 4f, 4.5f, YEL, Ink, 2f)
    }
    spin(a2, 60f, 24f) {
        path("M60 24Q66 10 72 5", stroke = Ink, sw = 3f)
        circle(73f, 4f, 4.5f, PINK, Ink, 2f)
    }
    path("M25 42L9 32L22 55Z", GREEN2, Ink)
    path("M75 42L91 32L78 55Z", GREEN2, Ink)
    ellipse(50f, 46f, 27f, 25f, GREEN, Ink)
    circle(29f, 40f, 2.5f, GREEN2)
    circle(71f, 37f, 3f, GREEN2)
    path("M27 35Q25 12 50 12Q66 5 79 16Q71 16 69 22Q75 26 73 32Q61 22 47 26Q37 28 27 35Z", Color(0xFFA98BFF), Ink, 2.5f)
    path("M40 20Q52 12 66 14", stroke = Color(0xFFE2D6FF), sw = 2.5f)
    group(y = -1.5f * swing(t, .8f)) {
        path("M30 33Q37 28 44 32M56 32Q63 28 70 33", stroke = Ink, sw = 3.2f)
    }
    ellipse(38f, 44f, 8.5f, 10f, Color.White, Ink, 2f)
    ellipse(62f, 44f, 8.5f, 10f, Color.White, Ink, 2f)
    val (lx, ly) = look(t, 4f)
    group(lx, ly) {
        circle(39.5f, 45.5f, 4.2f, NAVY); circle(60.5f, 45.5f, 4.2f, NAVY)
        circle(41f, 43.5f, 1.5f, Color.White); circle(62f, 43.5f, 1.5f, Color.White)
    }
    circle(28f, 56f, 4f, Color(0xFFFF7FA8), alpha = .6f)
    circle(72f, 56f, 4f, Color(0xFFFF7FA8), alpha = .6f)
    when (mood) {
        Mood.NONE -> {
            path("M37 57Q50 71 63 57Q50 61 37 57Z", NAVY, NAVY, 2f)
            path("M41 59Q50 62 59 59L57.5 61.5Q50 64 42.5 61.5Z", Color.White)
        }
        Mood.HAPPY -> {
            path("M36 56Q50 78 64 56Q50 60 36 56Z", NAVY)
            path("M43 66Q50 71 57 66Q50 63 43 66Z", BERRY)
        }
        Mood.SAD -> path("M40 66Q50 58 60 66", stroke = NAVY, sw = 3.5f)
    }
}

fun DrawScope.martyMic(x: Float, y: Float, r: Float) = group(x, y, rot = r) {
    rect(-2.5f, 6f, 5f, 14f, 2f, Color(0xFF4A3A8A), Ink, 1.8f)
    rect(-6f, 9f, 12f, 7f, 1.5f, BERRY, Ink, 1.5f)
    rect(-7f, -11f, 14f, 18f, 6f, Color(0xFFD8DCE8), Ink, 2.2f)
    path("M-5 -6H5M-5 -2.5H5M-5 1H5", stroke = Color(0xFF7A7F95), sw = 1.4f)
    rect(-8f, -3f, 16f, 3f, 0f, YEL, Ink, 1.2f)
}

fun DrawScope.martyBust(t: Float, mood: Mood) {
    ellipse(50f, 97f, 30f, 4f, Color.Black, alpha = .2f)
    group(y = -2f * swing(t, .6f)) {
        path("M20 100Q20 78 50 74Q80 78 80 100Z", SUIT, Ink)
        path("M31 82L36 99M69 82L64 99", stroke = SKY, sw = 5f)
        rect(44f, 66f, 12f, 9f, 0f, GREEN2, Ink, 2f)
        ellipse(50f, 75f, 15f, 4.5f, YEL, Ink, 2.5f)
        path("M50 81L40 76V86ZM50 81L60 76V86Z", BERRY, Ink, 2f)
        circle(50f, 81f, 2.6f, BERRY, Ink, 2f)
        circle(50f, 93f, 3f, Color(0xFF7DFFE3), Ink, 1.5f, alpha = lerp(.35f, 1f, swing(t, .5f)))
        martyHead(t, mood)
        path("M75 97Q90 90 89 78", stroke = Ink, sw = 10f)
        path("M75 97Q90 90 89 78", stroke = SUIT, sw = 5.5f)
        martyMic(89f, 58f, 12f)
        circle(89f, 74f, 6f, GREEN, Ink, 2.5f)
    }
}

fun DrawScope.martyFull(t: Float) {
    ellipse(60f, 216f, 36f, 6f, Color.Black, alpha = .25f)
    group(y = -2f * swing(t, .6f)) {
        path("M46 154L44 200H58L60 154Z", SKY, Ink)
        path("M74 154L76 200H62L60 154Z", SKY, Ink)
        path("M45 178H59M61 178H75", stroke = SUIT, sw = 5f)
        path("M36 198H58V214H38Q30 214 32 206Z", BERRY, Ink)
        path("M84 198H62V214H82Q90 214 88 206Z", BERRY, Ink)
        path("M36 108Q38 96 60 94Q82 96 84 108L86 158Q60 166 34 158Z", SUIT, Ink)
        path("M45 99L41 157M75 99L79 157", stroke = SKY, sw = 5f)
        rect(48f, 112f, 24f, 18f, 4f, Color(0xFF2A1460), Ink)
        listOf(54f to Color(0xFF7DFFE3), 60f to YEL, 66f to PINK).forEachIndexed { i, (x, c) ->
            circle(x, 121f, 3f, c, alpha = lerp(.35f, 1f, swing(t, .5f, i * .3f)))
        }
        rect(35f, 145f, 50f, 8f, 0f, Color(0xFFA98BFF), Ink)
        path("M60 142l3 5h5l-4 3 2 5-6-3-6 3 2-5-4-3h5z", YEL, Ink, 1.5f)
        spin(lerp(-12f, 14f, swing(t, .7f)), 41f, 104f) {
            path("M41 104Q24 96 21 78", stroke = Ink, sw = 13f)
            path("M41 104Q24 96 21 78", stroke = SUIT, sw = 8f)
            circle(20f, 71f, 7.5f, GREEN, Ink)
            path("M14 66L11 60M20 64V57M26 66L29 60", stroke = Ink, sw = 3f)
        }
        rect(54f, 84f, 12f, 11f, 0f, GREEN2, Ink, 2f)
        ellipse(60f, 96f, 15f, 4.5f, YEL, Ink, 2.5f)
        path("M60 102L50 97V107ZM60 102L70 97V107Z", BERRY, Ink, 2f)
        circle(60f, 102f, 2.6f, BERRY, Ink, 2f)
        group(10f, 16f) { martyHead(t, Mood.NONE) }
        path("M79 104Q98 114 97 98", stroke = Ink, sw = 13f)
        path("M79 104Q98 114 97 98", stroke = SUIT, sw = 8f)
        martyMic(97f, 78f, 8f)
        circle(97f, 95f, 7.5f, GREEN, Ink)
    }
}

private val ENVELOPE = Path().apply { addOval(Rect(413f, 12f, 597f, 212f)) }

fun DrawScope.balloonShip(t: Float, tm: TextMeasurer) = group(y = lerp(-4f, 5f, swing(t, 4f))) {
    path("M455 196L475 224H535L555 196Z", Color(0xFFB9824A), Ink)
    clipPath(ENVELOPE) {
        var x = 410f
        var i = 0
        while (x < 600f) {
            rect(x, 0f, 24f, 230f, fill = listOf(BERRY, Color(0xFFFFF4CF), SKY, Color(0xFFFFF4CF))[i % 4])
            x += 24f; i++
        }
        ellipse(505f, 112f, 92f, 100f, brush = rGrad(470f, 70f, 160f, 0f to Color.White.copy(alpha = .35f), .6f to Color.Transparent, 1f to Ink.copy(alpha = .35f)))
    }
    ellipse(505f, 112f, 92f, 100f, stroke = Ink)
    path("M416 136Q505 166 594 136", stroke = Ink, sw = 9f); path("M416 136Q505 166 594 136", stroke = Color(0xFFE0A400), sw = 5f)
    path("M440 46Q505 66 570 46", stroke = Ink, sw = 8f); path("M440 46Q505 66 570 46", stroke = Color(0xFFE0A400), sw = 4f)
    listOf(430f, 455f, 480f, 505f, 530f, 555f, 580f).forEach { x -> val q = (x - 505f) / 89f; circle(x, 151f - 14f * q * q, 2f, Color(0xFFFFF3A0)) }
    rect(499f, -4f, 12f, 16f, 0f, Color(0xFFE0A400), Ink)
    line(505f, -4f, 505f, -26f, Ink, 3f)
    spin(lerp(-8f, 8f, swing(t, .5f)), 505f, -20f) { path("M505 -26L528 -20L505 -13Z", GREEN, Ink, 2f) }
    path("M460 200L448 252M550 200L562 252M482 222L478 252M528 222L532 252", stroke = Color(0xFF6D4420), sw = 2.5f)
    rect(430f, 224f, 12f, 28f, 0f, Color(0xFF9A9FB5), Ink)
    rect(427f, 220f, 18f, 6f, 2f, Color(0xFFE0A400), Ink)
    listOf(0f, .8f, 1.6f).forEach { d ->
        val p = ((t + d) / 2.4f) % 1f
        val a = if (p < .2f) p / .2f * .9f else .9f * (1f - (p - .2f) / .8f)
        circle(436f - 14f * p, 212f - 44f * p, 7f * lerp(.4f, 1.7f, p), Color.White, alpha = a)
    }
    path("M492 300H518L514 316H496Z", Color(0xFF9A9FB5), Ink)
    zoom(1f, lerp(1f, .65f, swing(t, .18f)), 505f, 316f) {
        path("M497 316Q505 346 513 316Z", SUIT)
        path("M501 316Q505 334 509 316Z", YEL)
    }
    path("M420 256H590Q588 294 548 302H462Q422 294 420 256Z", brush = vGrad(256f, 302f, 0f to Color(0xFFC98F52), 1f to Color(0xFF8A5A2E)))
    path("M420 256H590Q588 294 548 302H462Q422 294 420 256Z", stroke = Ink)
    line(432f, 280f, 578f, 280f, Color(0xFF6D4420), 2f, .6f)
    rect(414f, 248f, 182f, 12f, 6f, Color(0xFFFFC93C), Ink)
    for (cx in listOf(450f, 560f)) {
        circle(cx, 280f, 9f, Color(0xFF8FDCFF), Color(0xFFE0A400), 5f)
        circle(cx, 280f, 11.5f, stroke = Ink, sw = 2f)
    }
    rect(476f, 270f, 58f, 19f, 4f, YEL, Ink)
    textAt(tm, "MARTY", 505f, 279.5f, TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = units(13f), color = Color(0xFF4A2C12)))
    group(600f, 274f) {
        rect(-6f, -4f, 12f, 8f, 0f, Color(0xFFE0A400), Ink)
        zoom(1f, lerp(1f, .12f, swing(t, .12f)), 8f, 0f) { ellipse(8f, 0f, 4f, 22f, Color(0xFFE2D6FF), Ink) }
    }
    group(412f, 262f, rot = t * 120f) {
        for (k in 0 until 8) spin(k * 45f, 0f, 0f) { rect(-2.5f, -12f, 5f, 6f, 1f, Color(0xFFE0A400)) }
        circle(0f, 0f, 9f, Color(0xFFE0A400), Ink, 2f)
    }
}

/** Portada: Marty en el escenario junto a su globo-nave. */
fun DrawScope.heroScene(t: Float, tm: TextMeasurer) = viewBox(640f, 360f) {
    rect(0f, 0f, 640f, 360f, brush = rGrad(256f, 162f, 480f, 0f to Color(0xFF4A33A8), .6f to Color(0xFF241B5E), 1f to Color(0xFF150F3A)))
    listOf(Triple(30f, 30f, 1.4f), Triple(90f, 120f, 1f), Triple(300f, 24f, 1.3f), Triple(360f, 90f, 1f), Triple(620f, 40f, 1.2f),
        Triple(610f, 200f, 1f), Triple(20f, 240f, 1.2f), Triple(380f, 180f, 1.1f)).forEachIndexed { i, (x, y, r) ->
        circle(x, y, r * 1.2f, Color.White, alpha = lerp(.3f, 1f, swing(t, 1.2f, i * .17f)))
    }
    path("M170 0L260 0L330 336L100 336Z", brush = vGrad(0f, 336f, 0f to Color(0x0DFFF6D8), 1f to Color(0x59FFF6D8)))
    ellipse(215f, 338f, 175f, 24f, Color(0xFF4A2FA0), Ink, 4f)
    ellipse(215f, 334f, 160f, 16f, Color(0xFF5B3FC0))
    group(131f, 26f, 1.42f) { martyFull(t) }
    for (i in 0 until 11) {
        val a = PI.toFloat() * (.12f + i * .076f)
        val x = 215f - kotlin.math.cos(a) * 170f
        val y = 338f + sin(a) * 22f
        val on = ((t / .9f + (i % 2) * .5f) % 1f) < .5f
        circle(x, y, 4f, if (i % 2 == 1) YEL else PINK, Ink, 1.5f, alpha = if (on) 1f else .35f)
    }
    group(-14f, 32f, .92f) { balloonShip(t, tm) }
}

/* ---------- Borrón, el saboteador ---------- */

fun DrawScope.borron(t: Float, mood: Mood) {
    val p = (t / 2.8f) % 1f
    val sx: Float
    val sy: Float
    val dy: Float
    if (mood == Mood.SAD) { sx = 1.04f; sy = .88f; dy = 0f } else if (mood == Mood.HAPPY) {
        val f = abs(sin(t * PI.toFloat() / .35f)); sx = 1f - .04f * f; sy = 1f + .05f * f; dy = -3f * f
    } else {
        val f = if (p < .4f) sin(p / .4f * PI.toFloat() / 2) else if (p < .7f) 1f - (p - .4f) / .3f * 2f else -1f + (p - .7f) / .3f
        sx = if (f >= 0) 1f - .04f * f else 1f - .05f * f
        sy = if (f >= 0) 1f + .05f * f else 1f + .05f * f
        dy = if (f >= 0) -3f * f else 0f
    }
    ellipse(50f, 93f, 28f * lerp(1f, .85f, swing(t, 1.4f)), 5f, Color.Black, alpha = .25f)
    group(y = dy) {
        zoom(sx, sy, 50f, 92f) {
            path("M20 40C14 20 34 10 50 16c18-8 36 4 30 24 12 8 10 30-4 36 2 14-14 20-26 14-14 8-34 0-30-16-12-6-14-22 0-34z", Color(0xFF5B49C9), Color(0xFF3A2B94), 3f)
            path("M26 30q6-9 15-10", stroke = Color(0xFF8C7BF0), sw = 3f, alpha = .7f)
            zoom(1f, blink(t, 4f), 50f, 46f) {
                ellipse(38f, 46f, 9f, 10f, Color.White); ellipse(62f, 46f, 9f, 10f, Color.White)
                val (lx, ly) = look(t, 5f)
                circle(40f + lx, 48f + ly, 4f, C.space); circle(60f + lx, 48f + ly, 4f, C.space)
            }
            group(y = if (mood == Mood.SAD) 4f else -1.5f * swing(t, 1.7f)) {
                path("M27 32l17 7M73 32l-17 7", stroke = C.space, sw = 4f)
            }
            when (mood) {
                Mood.NONE -> path("M34 68q16 14 32 0q-16 5-32 0z", C.space)
                Mood.HAPPY -> { path("M32 64q18 24 36 0z", C.space); path("M42 74q8 6 16 0q-8-4-16 0z", BERRY) }
                Mood.SAD -> path("M36 74q14-10 28 0", stroke = C.space, sw = 3.5f)
            }
        }
    }
    listOf(Triple(34f, 86f, 0f), Triple(64f, 88f, .9f), Triple(49f, 90f, 1.7f)).forEach { (x, y, d) ->
        val q = ((t + d) / 2.6f) % 1f
        val a = if (q < .2f) q / .2f else 1f - (q - .2f) / .8f
        val s = if (q < .2f) lerp(.2f, 1f, q / .2f) else 1f
        val yy = if (q < .2f) lerp(-4f, 0f, q / .2f) else lerp(0f, 20f, (q - .2f) / .8f)
        ellipse(x, y + yy, 2.3f * s, 3.3f * s, Color(0xFF5B49C9), alpha = a)
    }
}

/* ---------- Barra del presentador ---------- */

@Composable
fun HostBar(g: Game) {
    val t by rememberTime()
    val act = remember { Animatable(1f) }
    val enter = remember { Animatable(1f) }
    LaunchedEffect(g.moodTick) {
        act.snapTo(0f)
        act.animateTo(1f, tween(if (g.mood == Mood.SAD) 500 else 800))
    }
    LaunchedEffect(g.who) {
        enter.snapTo(0f)
        enter.animateTo(1f, tween(650))
    }
    val borron = g.who == Who.BORRON
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(
            Modifier.size(84.dp).graphicsLayer {
                val p = act.value
                val e = enter.value
                transformOrigin = TransformOrigin(.5f, 1f)
                var tx = 0f
                var ty = 0f
                var rot = 0f
                var sc = 1f
                if (g.mood == Mood.HAPPY && !borron) {
                    ty = -22f * sin(PI.toFloat() * min(p / .7f, 1f)); rot = -6f * sin(2 * PI.toFloat() * p)
                } else if (g.mood == Mood.HAPPY) {
                    ty = -8f * abs(sin(3 * PI.toFloat() * p)) * (1 - p); rot = 6f * sin(4 * PI.toFloat() * p) * (1 - p)
                } else if (g.mood == Mood.SAD && !borron) rot = -8f * sin(2 * PI.toFloat() * p)
                if (e < 1f) {
                    if (borron) { tx = 70f * (1 - e); sc = lerp(.2f, 1f, e); rot += 30f * (1 - e) } else sc = if (e < .7f) lerp(.5f, 1.1f, e / .7f) else lerp(1.1f, 1f, (e - .7f) / .3f)
                }
                translationX = tx * density
                translationY = ty * density
                rotationZ = rot
                scaleX = sc
                scaleY = sc
            }
        ) {
            group(s = size.width / 100f) { if (borron) borron(t, g.mood) else martyBust(t, g.mood) }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)).background(C.card)
                .border(2.dp, C.line, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)).padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(if (borron) "BORRÓN" else "MARTY", style = body(12, if (borron) C.berry else C.sun, FontWeight.ExtraBold).copy(letterSpacing = 1.2.sp))
            Text(g.text, style = body(16, weight = FontWeight.ExtraBold))
        }
    }
}
