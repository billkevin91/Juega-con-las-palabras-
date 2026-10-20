package com.billkevin91.martypalabras

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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

val Ink = Color(0xFF1B0F3A)

object Paths {
    private val cache = HashMap<String, Path>()
    fun of(d: String): Path {
        if (cache.size > 6000) cache.clear()
        return cache.getOrPut(d) { PathParser().parsePathString(plain(d)).toPath() }
    }

    /** Los números generados en Kotlin pueden salir como 1.2E-15: los paso a decimales normales. */
    private val EXP = Regex("-?[0-9]*\\.?[0-9]+[eE][-+]?[0-9]+")
    private fun plain(d: String): String =
        if (d.contains('E') || d.contains('e')) d.replace(EXP) { java.math.BigDecimal(it.value).toPlainString() } else d
}

private fun outline(sw: Float) = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

fun DrawScope.path(
    d: String, fill: Color = Color.Unspecified, stroke: Color = Color.Unspecified,
    sw: Float = 3f, alpha: Float = 1f, brush: Brush? = null,
) {
    val p = Paths.of(d)
    if (brush != null) drawPath(p, brush, alpha) else if (fill.isSpecified) drawPath(p, fill, alpha)
    if (stroke.isSpecified) drawPath(p, stroke, alpha, style = outline(sw))
}

fun DrawScope.circle(
    cx: Float, cy: Float, r: Float, fill: Color = Color.Unspecified, stroke: Color = Color.Unspecified,
    sw: Float = 3f, alpha: Float = 1f, brush: Brush? = null,
) {
    val c = Offset(cx, cy)
    if (brush != null) drawCircle(brush, r, c, alpha) else if (fill.isSpecified) drawCircle(fill, r, c, alpha)
    if (stroke.isSpecified) drawCircle(stroke, r, c, alpha, style = outline(sw))
}

fun DrawScope.ellipse(
    cx: Float, cy: Float, rx: Float, ry: Float, fill: Color = Color.Unspecified, stroke: Color = Color.Unspecified,
    sw: Float = 3f, alpha: Float = 1f, brush: Brush? = null,
) {
    val tl = Offset(cx - rx, cy - ry)
    val s = Size(rx * 2, ry * 2)
    if (brush != null) drawOval(brush, tl, s, alpha) else if (fill.isSpecified) drawOval(fill, tl, s, alpha)
    if (stroke.isSpecified) drawOval(stroke, tl, s, alpha, style = outline(sw))
}

fun DrawScope.rect(
    x: Float, y: Float, w: Float, h: Float, r: Float = 0f, fill: Color = Color.Unspecified,
    stroke: Color = Color.Unspecified, sw: Float = 3f, alpha: Float = 1f, brush: Brush? = null,
) {
    val tl = Offset(x, y)
    val s = Size(w, h)
    val cr = CornerRadius(r, r)
    if (brush != null) drawRoundRect(brush, tl, s, cr, alpha) else if (fill.isSpecified) drawRoundRect(fill, tl, s, cr, alpha = alpha)
    if (stroke.isSpecified) drawRoundRect(stroke, tl, s, cr, alpha = alpha, style = outline(sw))
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

fun vGrad(y0: Float, y1: Float, vararg stops: Pair<Float, Color>) =
    Brush.verticalGradient(*stops, startY = y0, endY = y1)

fun rGrad(cx: Float, cy: Float, r: Float, vararg stops: Pair<Float, Color>) =
    Brush.radialGradient(*stops, center = Offset(cx, cy), radius = r)

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
