package com.rodadabrutal

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys

/**
 * Entrada unificada teclado + touch. Cuatro zonas en pantalla:
 *   [ inclinar atrás | inclinar adelante ]
 *   [    freno       |     acelerar      ]
 */
class Controls {
    var throttle = 0f   // 0..1
    var brake = 0f      // 0..1
    var lean = 0f       // -1 atrás ... +1 adelante

    fun poll() {
        throttle = 0f; brake = 0f; lean = 0f

        // Teclado (debug / desktop)
        if (Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)) throttle = 1f
        if (Gdx.input.isKeyPressed(Keys.LEFT)  || Gdx.input.isKeyPressed(Keys.A)) brake = 1f
        if (Gdx.input.isKeyPressed(Keys.UP)    || Gdx.input.isKeyPressed(Keys.W)) lean = 1f
        if (Gdx.input.isKeyPressed(Keys.DOWN)  || Gdx.input.isKeyPressed(Keys.S)) lean = -1f

        // Touch multitáctil
        val w = Gdx.graphics.width
        val h = Gdx.graphics.height
        for (i in 0 until 5) {
            if (Gdx.input.isTouched(i)) {
                val x = Gdx.input.getX(i)
                val y = Gdx.input.getY(i)
                val right = x > w / 2
                val bottom = y > h / 2
                when {
                    bottom && right -> throttle = 1f
                    bottom && !right -> brake = 1f
                    !bottom && right -> lean = 1f
                    !bottom && !right -> lean = -1f
                }
            }
        }
    }
}
