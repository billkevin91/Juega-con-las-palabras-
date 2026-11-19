package com.billkevin91.martypalabras

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/* Movimiento y profundidad: inclinación 3D con el sensor del teléfono y entradas animadas. */

/**
 * Inclinación del teléfono, de -1 a 1 en cada eje, relativa a como se está sosteniendo: si el teléfono
 * se queda quieto vuelve poco a poco a 0, así la escena solo se mueve cuando el jugador mueve el teléfono.
 */
object Tilt : SensorEventListener {
    var x by mutableFloatStateOf(0f)
        private set
    var y by mutableFloatStateOf(0f)
        private set

    private var baseX = Float.NaN
    private var baseY = Float.NaN
    private var manager: SensorManager? = null

    fun start(context: Context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        manager = sm
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        manager?.unregisterListener(this)
        manager = null
        x = 0f
        y = 0f
    }

    override fun onSensorChanged(e: SensorEvent) {
        val gx = e.values[0] / SensorManager.GRAVITY_EARTH
        val gy = e.values[1] / SensorManager.GRAVITY_EARTH
        if (baseX.isNaN()) { baseX = gx; baseY = gy }
        baseX += (gx - baseX) * .015f
        baseY += (gy - baseY) * .015f
        val tx = ((baseX - gx) * 2.5f).coerceIn(-1f, 1f)
        val ty = ((gy - baseY) * 2.5f).coerceIn(-1f, 1f)
        x += (tx - x) * .2f
        y += (ty - y) * .2f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/** La tarjeta gira un poco en 3D siguiendo la inclinación del teléfono. */
fun Modifier.tilt3d(depth: Float = 7f): Modifier = graphicsLayer {
    rotationY = Tilt.x * depth
    rotationX = Tilt.y * depth
    cameraDistance = 16f * density
}

/** Entrada animada: el elemento aparece desde abajo con un rebote, uno tras otro según [index]. */
@Composable
fun Modifier.popIn(index: Int): Modifier {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        p.animateTo(1f, spring(dampingRatio = .62f, stiffness = 260f))
    }
    return graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 48f * density
        val s = .9f + .1f * v
        scaleX = s
        scaleY = s
    }
}
