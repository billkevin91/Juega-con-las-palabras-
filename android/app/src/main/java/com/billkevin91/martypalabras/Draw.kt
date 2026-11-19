package com.billkevin91.martypalabras

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.graphics.asComposePathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/* Ayudas de dibujo: permiten usar trazos con la misma sintaxis que SVG (M, Q, C, A…) en un Canvas. */

val Ink = Color(0xFF221B2A)

object Paths {
    private val cache = HashMap<String, Path>()
    private val bounds = HashMap<String, Rect>()
    fun of(d: String): Path {
        if (cache.size > 6000) { cache.clear(); bounds.clear() }
        return cache.getOrPut(d) { PathParser().parsePathString(plain(d)).toPath() }
    }

    fun boundsOf(d: String): Rect = bounds.getOrPut(d) { of(d).getBounds() }

    /** Los números generados en Kotlin pueden salir como 1.2E-15: los paso a decimales normales. */
    private val EXP = Regex("-?[0-9]*\\.?[0-9]+[eE][-+]?[0-9]+")
    private fun plain(d: String): String =
        if (d.contains('E') || d.contains('e')) d.replace(EXP) { java.math.BigDecimal(it.value).toPlainString() } else d
}

/**
 * Trazo a mano: el contorno tiembla un poquito (como tinta sobre papel) en vez de ser una línea perfecta.
 * El temblor es siempre el mismo para cada forma, así no "hierve" entre cuadros.
 */
private val HAND = android.graphics.DiscretePathEffect(7f, 1.1f).asComposePathEffect()
private fun outline(sw: Float) = Stroke(sw * 1.25f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = HAND)

/** Grano de papel: una textura de puntitos que se repite, para que las escenas parezcan impresas. */
val PaperGrain: androidx.compose.ui.graphics.ShaderBrush by lazy {
    val n = 192
    val r = kotlin.random.Random(11)
    val px = IntArray(n * n) {
        val v = r.nextInt(256)
        val a = if (r.nextFloat() < .5f) r.nextInt(18, 46) else 0
        android.graphics.Color.argb(a, v, v, v)
    }
    val bmp = android.graphics.Bitmap.createBitmap(px, n, n, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
    androidx.compose.ui.graphics.ShaderBrush(
        androidx.compose.ui.graphics.ImageShader(bmp, androidx.compose.ui.graphics.TileMode.Repeated, androidx.compose.ui.graphics.TileMode.Repeated),
    )
}

/* ---------- Sombreado de caricatura (cel shading) ----------
 * Todo relleno de color liso (y opaco) se pinta como en los dibujos animados: el color plano y una
 * sombra de un solo tono, con borde nítido (sin degradados suaves). Los degradados se guardan para no crearlos en
 * cada fotograma; las formas se dibujan trasladadas para que el degradado sirva en cualquier posición.
 */
object Shade {
    /** Se puede apagar para dibujar algo totalmente plano (por ejemplo, siluetas). */
    var on = true
    private val cache = HashMap<Any, Brush>()

    fun brush(key: Any, make: () -> Brush): Brush {
        if (cache.size > 3000) cache.clear()
        return cache.getOrPut(key, make)
    }

    fun light(c: Color, f: Float) = colorLerp(c, Color.White, f)
    fun dark(c: Color, f: Float) = colorLerp(c, Color(0xFF14082E), f)
    fun applies(fill: Color, alpha: Float, size: Float) = on && alpha >= .95f && fill.alpha >= .95f && size >= 6f
}

private data class LinKey(val c: ULong, val h: Float)
private data class RadKey(val c: ULong, val rx: Float, val ry: Float)
private data class PathKey(val c: ULong, val d: String)

private fun linear(c: Color, h: Float) = Shade.brush(LinKey(c.value, h)) {
    Brush.verticalGradient(*cel(c, .64f), startY = 0f, endY = h)
}

/** Color plano hasta [at] y sombra de un tono desde ahí, con corte nítido. */
private fun cel(c: Color, at: Float): Array<Pair<Float, Color>> {
    val d = Shade.dark(c, .2f)
    return arrayOf(0f to c, at to c, at to d, 1f to d)
}

/** Círculos y óvalos: color plano con una media luna de sombra abajo a la derecha. */
private fun radial(c: Color, rx: Float, ry: Float) = Shade.brush(RadKey(c.value, rx, ry)) {
    Brush.radialGradient(*cel(c, .78f), center = Offset(rx * .8f, ry * .72f), radius = maxOf(rx, ry) * 1.3f)
}

fun DrawScope.path(
    d: String, fill: Color = Color.Unspecified, stroke: Color = Color.Unspecified,
    sw: Float = 3f, alpha: Float = 1f, brush: Brush? = null,
) {
    val p = Paths.of(d)
    if (brush != null) drawPath(p, brush, alpha) else if (fill.isSpecified) {
        val b = Paths.boundsOf(d)
        if (Shade.applies(fill, alpha, b.height)) {
            val br = Shade.brush(PathKey(fill.value, d)) {
                Brush.verticalGradient(*cel(fill, .64f), startY = b.top, endY = b.bottom)
            }
            drawPath(p, br, alpha)
        } else drawPath(p, fill, alpha)
    }
    if (stroke.isSpecified) drawPath(p, stroke, alpha, style = outline(sw))
}

fun DrawScope.circle(
    cx: Float, cy: Float, r: Float, fill: Color = Color.Unspecified, stroke: Color = Color.Unspecified,
    sw: Float = 3f, alpha: Float = 1f, brush: Brush? = null,
) {
    val c = Offset(cx, cy)
    if (brush != null) drawCircle(brush, r, c, alpha) else if (fill.isSpecified) {
        if (Shade.applies(fill, alpha, r * 2)) {
            val br = radial(fill, r, r)
            withTransform({ translate(cx - r, cy - r) }) { drawCircle(br, r, Offset(r, r), alpha) }
        } else drawCircle(fill, r, c, alpha)
    }
    if (stroke.isSpecified) drawCircle(stroke, r, c, alpha, style = outline(sw))
}

fun DrawScope.ellipse(
    cx: Float, cy: Float, rx: Float, ry: Float, fill: Color = Color.Unspecified, stroke: Color = Color.Unspecified,
    sw: Float = 3f, alpha: Float = 1f, brush: Brush? = null,
) {
    val tl = Offset(cx - rx, cy - ry)
    val s = Size(rx * 2, ry * 2)
    if (brush != null) drawOval(brush, tl, s, alpha) else if (fill.isSpecified) {
        if (Shade.applies(fill, alpha, ry * 2)) {
            val br = radial(fill, rx, ry)
            withTransform({ translate(tl.x, tl.y) }) { drawOval(br, Offset.Zero, s, alpha) }
        } else drawOval(fill, tl, s, alpha)
    }
    if (stroke.isSpecified) drawOval(stroke, tl, s, alpha, style = outline(sw))
}

fun DrawScope.rect(
    x: Float, y: Float, w: Float, h: Float, r: Float = 0f, fill: Color = Color.Unspecified,
    stroke: Color = Color.Unspecified, sw: Float = 3f, alpha: Float = 1f, brush: Brush? = null,
) {
    val tl = Offset(x, y)
    val s = Size(w, h)
    val cr = CornerRadius(r, r)
    if (brush != null) drawRoundRect(brush, tl, s, cr, alpha) else if (fill.isSpecified) {
        if (Shade.applies(fill, alpha, h)) {
            val br = linear(fill, h)
            withTransform({ translate(x, y) }) { drawRoundRect(br, Offset.Zero, s, cr, alpha) }
        } else drawRoundRect(fill, tl, s, cr, alpha = alpha)
    }
    if (stroke.isSpecified) drawRoundRect(stroke, tl, s, cr, alpha = alpha, style = outline(sw))
}

private val scratch = Path()

/**
 * Curva suave de tres puntos (como "M Q T" en SVG) sin pasar por texto: para formas que cambian
 * en cada fotograma, como tentáculos, y que no conviene guardar en la caché de trazos.
 */
fun DrawScope.wiggle(x0: Float, y0: Float, cx: Float, cy: Float, x1: Float, y1: Float, x2: Float, y2: Float, color: Color, sw: Float) {
    scratch.reset()
    scratch.moveTo(x0, y0)
    scratch.quadraticBezierTo(cx, cy, x1, y1)
    scratch.quadraticBezierTo(2 * x1 - cx, 2 * y1 - cy, x2, y2)
    drawPath(scratch, color, style = outline(sw))
}

fun DrawScope.line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, sw: Float = 2f, alpha: Float = 1f) =
    drawLine(color, Offset(x1, y1), Offset(x2, y2), sw, StrokeCap.Round, alpha = alpha)

/** Traslada, gira y escala (en ese orden) y dibuja dentro. */
inline fun DrawScope.group(
    x: Float = 0f, y: Float = 0f, s: Float = 1f, sy: Float = s, rot: Float = 0f,
    block: DrawScope.() -> Unit,
) = withTransform({
    translate(x, y)
    if (rot != 0f) rotate(rot, Offset.Zero)
    if (s != 1f || sy != 1f) scale(s, sy, Offset.Zero)
}, block)

/** Gira alrededor de un punto. */
inline fun DrawScope.spin(deg: Float, px: Float, py: Float, block: DrawScope.() -> Unit) =
    withTransform({ rotate(deg, Offset(px, py)) }, block)

/** Escala alrededor de un punto. */
inline fun DrawScope.zoom(sx: Float, sy: Float, px: Float, py: Float, block: DrawScope.() -> Unit) =
    withTransform({ scale(sx, sy, Offset(px, py)) }, block)

/** Igual que un viewBox de SVG con "slice": llena el lienzo y centra. */
inline fun DrawScope.viewBox(w: Float, h: Float, block: DrawScope.() -> Unit) {
    val k = max(size.width / w, size.height / h)
    withTransform({
        translate((size.width - w * k) / 2, (size.height - h * k) / 2)
        scale(k, k, Offset.Zero)
    }, block)
}

/**
 * Degradados en franjas: en vez de un paso suave, el color cambia en bandas nítidas, como en los
 * fondos pintados de las caricaturas.
 */
fun bands(n: Int, stops: Array<out Pair<Float, Color>>): Array<Pair<Float, Color>> {
    if (stops.size < 2) return arrayOf(*stops)
    fun at(f: Float): Color {
        for (i in 1 until stops.size) if (f <= stops[i].first) {
            val (a, ca) = stops[i - 1]
            val (b, cb) = stops[i]
            return colorLerp(ca, cb, if (b > a) ((f - a) / (b - a)).coerceIn(0f, 1f) else 1f)
        }
        return stops.last().second
    }
    val out = ArrayList<Pair<Float, Color>>()
    for (k in 0 until n) {
        val c = at((k + .5f) / n)
        out += k / n.toFloat() to c
        out += (k + 1) / n.toFloat() to c
    }
    return out.toTypedArray()
}

fun vGrad(y0: Float, y1: Float, vararg stops: Pair<Float, Color>) =
    Brush.verticalGradient(*bands(5, stops), startY = y0, endY = y1)

fun rGrad(cx: Float, cy: Float, r: Float, vararg stops: Pair<Float, Color>) =
    Brush.radialGradient(*bands(3, stops), center = Offset(cx, cy), radius = r)

/** Tamaño de letra en unidades del dibujo actual. */
fun DrawScope.units(u: Float): TextUnit = (u / (density * fontScale)).sp

fun DrawScope.textAt(tm: TextMeasurer, s: String, cx: Float, cy: Float, style: TextStyle) {
    val r = tm.measure(s, style)
    drawText(r, topLeft = Offset(cx - r.size.width / 2f, cy - r.size.height / 2f))
}

/** Onda de -1 a 1 con el periodo dado (segundos). */
fun wave(t: Float, period: Float, phase: Float = 0f): Float = sin(((t / period) + phase) * 2f * PI.toFloat())

/** Ida y vuelta de 0 a 1 (como "alternate" en CSS). */
fun swing(t: Float, period: Float, phase: Float = 0f): Float = (wave(t, period * 2, phase - .25f) + 1f) / 2f

fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

/** Segundos desde que aparece el elemento, actualizado en cada fotograma. */
@Composable
fun rememberTime(): State<Float> {
    val s = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = -1L
        while (true) {
            withFrameNanos { n ->
                if (start < 0) start = n
                s.floatValue = (n - start) / 1e9f
            }
        }
    }
    return s
}
