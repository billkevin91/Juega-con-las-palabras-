package com.billkevin91.martypalabras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Planetas que giran: las manchas de la superficie y las nubes se mueven sobre una esfera
 * (se achatan al llegar al borde), con atmósfera, lado de noche y brillo. Cada planeta tiene su textura.
 */

/** Una mancha de la superficie: longitud (0..1 vuelta), latitud (-1..1), tamaño y color. */
private class Spot(val lon: Float, val lat: Float, val size: Float, val color: Color, val alpha: Float = 1f)

private class Look(
    val spots: List<Spot>,
    val bands: List<Pair<Float, Color>> = emptyList(), // franjas de planeta gaseoso: latitud y color
    val clouds: Int = 0,
    val speed: Float = .03f, // vueltas por segundo
    val air: Color,
)

private fun spots(seed: Int, n: Int, color: Color, min: Float, max: Float, alpha: Float = 1f): List<Spot> {
    val r = kotlin.random.Random(seed)
    return List(n) { Spot(r.nextFloat(), r.nextFloat() * 1.6f - .8f, min + (max - min) * r.nextFloat(), color, alpha) }
}

private val LOOKS = HashMap<String, Look>()

private fun look(kind: String, a: Color, b: Color): Look = LOOKS.getOrPut(kind + a.value + b.value) {
    when (kind) {
        // Pantanal: charcos oscuros y musgo claro, nubes de neblina.
        "pantano" -> Look(
            spots(1, 9, Color(0xFF0F5A34), .12f, .28f, .7f) + spots(2, 7, Color(0xFFB8F5C8), .06f, .12f, .55f),
            clouds = 5, air = Color(0xFFB8FFD0),
        )
        // Burbujeo: océano con islas de coral y muchas nubes.
        "burbuja" -> Look(
            spots(3, 6, Color(0xFFFFB0C8), .08f, .17f, .85f) + spots(4, 8, Color(0xFF1A5FA8), .14f, .3f, .5f),
            clouds = 7, speed = .025f, air = Color(0xFFB0EEFF),
        )
        // Telaraña: franjas violetas y nudos de seda.
        "tela" -> Look(
            spots(5, 10, Color.White, .03f, .07f, .55f),
            bands = listOf(-.55f to Color(0xFF7B5CE0), -.15f to Color(0xFFDCCBFF), .25f to Color(0xFF6A4BD0), .6f to Color(0xFFB79CFF)),
            speed = .02f, air = Color(0xFFE2D4FF),
        )
        // Anagrama: gigante dorado con franjas y una gran tormenta.
        "anagrama" -> Look(
            listOf(Spot(.3f, .3f, .22f, Color(0xFFC85A00), .85f), Spot(.32f, .3f, .12f, Color(0xFFFFD27A), .9f)) +
                spots(6, 5, Color(0xFFFFF2B8), .05f, .1f, .6f),
            bands = listOf(-.6f to Color(0xFFF0A020), -.25f to Color(0xFFFFE9A8), .1f to Color(0xFFE08A00), .5f to Color(0xFFFFD06A)),
            speed = .035f, air = Color(0xFFFFF0B0),
        )
        // Letra perdida: planeta de caramelo con cráteres.
        "falta" -> Look(
            spots(7, 8, Color(0xFFB02A55), .07f, .16f, .65f) + spots(8, 10, Color.White, .03f, .06f, .7f),
            clouds = 3, air = Color(0xFFFFD0DE),
        )
        // Lectura: desierto naranja con valles y cráteres.
        "lee" -> Look(
            spots(9, 7, Color(0xFFB84A2E), .1f, .22f, .6f) + spots(10, 6, Color(0xFFFFE0B0), .05f, .1f, .6f),
            bands = listOf(-.3f to Color(0xFFE88A40), .35f to Color(0xFFF4A86A)),
            clouds = 2, speed = .025f, air = Color(0xFFFFE2C4),
        )
        else -> Look(spots(11, 6, b, .08f, .18f, .5f), clouds = 3, air = a)
    }
}

/** Dibuja un planeta que gira, centrado en el lienzo. */
fun DrawScope.planetBall(a: Color, b: Color, ring: Boolean, kind: String = "", t: Float = 0f) {
    val r = size.minDimension / 2 * .82f
    val c = Offset(size.width / 2, size.height / 2)
    val lk = look(kind, a, b)
    // Atmósfera de caricatura: un aro plano y claro alrededor del planeta.
    drawCircle(lk.air.copy(alpha = .3f), r * 1.12f, c)
    // Parte trasera del anillo.
    if (ring) spin(-18f, c.x, c.y) {
        clipRect(0f, 0f, size.width, c.y) { ringBand(c, r, a, b) }
    }
    drawCircle(a, r, c)
    clipPath(Path().apply { addOval(Rect(c, r)) }) {
        // Franjas (planetas gaseosos), con borde ondulado que se mueve.
        for ((lat, col) in lk.bands) {
            val y = c.y + lat * r
            val wob = r * .04f * sin(t * .8f + lat * 9f)
            drawRect(col.copy(alpha = .55f), Offset(c.x - r, y - r * .11f + wob), androidx.compose.ui.geometry.Size(r * 2, r * .22f))
        }
        // Manchas y nubes sobre la esfera giratoria.
        val turn = t * lk.speed
        for (s in lk.spots) sphereSpot(c, r, s.lon + turn, s.lat, s.size, s.color, s.alpha)
        for (k in 0 until lk.clouds) {
            val lon = k / lk.clouds.toFloat() + turn * 1.6f
            val lat = sin(k * 2.3f) * .65f
            sphereSpot(c, r, lon, lat, .16f, Color.White, .55f, stretch = 2.2f)
            sphereSpot(c, r, lon + .03f, lat - .05f, .1f, Color.White, .5f, stretch = 1.8f)
        }
        // Lado de noche: media luna de sombra con borde nítido, del color oscuro del planeta.
        val off = r * .38f
        val night = Path().apply { addOval(Rect(c, r * 1.02f)) }
        val lit = Path().apply { addOval(Rect(Offset(c.x - off, c.y - off), r * 1.12f)) }
        drawPath(Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, night, lit), b, alpha = .75f)
    }
    // Brillo de caricatura: una "ventanita" blanca plana arriba a la izquierda.
    spin(-35f, c.x - r * .4f, c.y - r * .48f) {
        drawOval(Color.White.copy(alpha = .75f), Offset(c.x - r * .62f, c.y - r * .6f), androidx.compose.ui.geometry.Size(r * .42f, r * .2f))
    }
    drawCircle(Ink, r, c, style = Stroke(maxOf(r * .06f, 2f)))
    // Parte delantera del anillo.
    if (ring) spin(-18f, c.x, c.y) {
        clipRect(0f, c.y, size.width, size.height) { ringBand(c, r, a, b) }
    }
}

/** Anillo con varias bandas y partículas que lo recorren. */
private fun DrawScope.ringBand(c: Offset, r: Float, a: Color, b: Color) {
    ellipse(c.x, c.y, r * 1.5f, r * .3f, stroke = Ink, sw = r * .17f)
    ellipse(c.x, c.y, r * 1.5f, r * .3f, stroke = a, sw = r * .1f)
    ellipse(c.x, c.y, r * 1.62f, r * .33f, stroke = b, sw = r * .03f)
}

/** Una mancha redonda sobre la esfera: se achata y se esconde al girar hacia el otro lado. */
private fun DrawScope.sphereSpot(c: Offset, r: Float, lon: Float, lat: Float, s: Float, col: Color, alpha: Float, stretch: Float = 1f) {
    val phi = (lon % 1f) * 2f * PI.toFloat()
    val depth = cos(phi)
    if (depth < -.05f) return
    val ring = cos(lat * PI.toFloat() / 2f)
    val x = c.x + r * sin(phi) * ring
    val y = c.y + r * sin(lat * PI.toFloat() / 2f)
    val vis = depth.coerceIn(0f, 1f)
    val rx = r * s * stretch * (.2f + .8f * vis)
    val ry = r * s * .7f
    // drawOval directo (sin sombreado automático): el tamaño cambia en cada cuadro y no conviene guardar pinceles.
    drawOval(col, Offset(x - rx, y - ry), androidx.compose.ui.geometry.Size(rx * 2, ry * 2), alpha * (.35f + .65f * vis))
}

/**
 * Mapa del Desafío Galáctico: los 6 planetas unidos por un camino de puntos. Los superados llevan sus
 * estrellas, el actual late y tiene encima la nave de Marty; los que faltan se ven apagados.
 */
@Composable
fun GalaxyRoute(g: Game, current: Int) {
    val time = rememberTime()
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(4.2f),
    ) {
        val t = time.value
        val n = RETO.size
        val w = size.width
        val h = size.height
        val pts = List(n) { i ->
            Offset(w * (.08f + .84f * i / (n - 1)), h * (.5f + .2f * sin(i * 1.9f + .6f)))
        }
        // Camino de puntos; la parte ya recorrida en dorado, con puntos que avanzan.
        for (i in 0 until n - 1) {
            val a = pts[i]
            val b = pts[i + 1]
            val done = i < current
            for (k in 0..9) {
                val f = ((k + if (done) t * 1.5f else 0f) / 10f) % 1f
                drawCircle(
                    if (done) C.sun else Color.White, h * .025f,
                    Offset(lerp(a.x, b.x, f), lerp(a.y, b.y, f) - sin(f * PI.toFloat()) * h * .12f),
                    alpha = if (done) .9f else .3f,
                )
            }
        }
        for (i in 0 until n) {
            val p = planet(RETO[i])
            val c = pts[i]
            val rad = h * (if (i == current) .36f + .03f * sin(t * 4f) else .27f)
            if (i == current) {
                drawCircle(C.sun.copy(alpha = .35f), rad * 1.45f, c)
                drawCircle(C.sun, rad * 1.45f, c, style = Stroke(h * .02f))
            }
            inset(c.x - rad, c.y - rad, size.width - c.x - rad, size.height - c.y - rad) {
                planetBall(p.a, p.b, p.ring, p.key, t)
            }
            if (i > current) drawCircle(Color(0xAA150F3A), rad * .85f, c)
            // Estrellas ganadas bajo cada planeta superado.
            if (i < current) {
                val st = g.runStars(RETO[i])
                for (k in 0 until 3) {
                    val sx = c.x + (k - 1) * h * .13f
                    val sy = c.y + rad + h * .1f
                    drawCircle(if (k < st) Color(0xFFFFD84A) else Color(0xFF3F3299), h * .05f, Offset(sx, sy))
                }
            }
        }
        // La nave de Marty, en miniatura, sobre el planeta actual.
        val m = pts[current.coerceIn(0, n - 1)]
        val bob = sin(t * 3f) * h * .03f
        val top = Offset(m.x, m.y - h * .42f + bob)
        drawCircle(Ink, h * .115f, top)
        drawCircle(Color(0xFFFF8FB0), h * .1f, top)
        drawCircle(Color(0xFF6FC8FF), h * .1f, top, style = Stroke(h * .03f))
        drawLine(Color(0xFF8A5A2B), Offset(top.x - h * .06f, top.y + h * .07f), Offset(top.x - h * .04f, top.y + h * .15f), h * .012f)
        drawLine(Color(0xFF8A5A2B), Offset(top.x + h * .06f, top.y + h * .07f), Offset(top.x + h * .04f, top.y + h * .15f), h * .012f)
        drawRoundRect(Color(0xFFC98A3A), Offset(top.x - h * .06f, top.y + h * .14f), androidx.compose.ui.geometry.Size(h * .12f, h * .05f), androidx.compose.ui.geometry.CornerRadius(h * .02f))
    }
}
