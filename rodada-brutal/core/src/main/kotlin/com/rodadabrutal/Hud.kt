package com.rodadabrutal

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Overlay mínimo: muestra las cuatro zonas de control cuando se tocan,
 * en coordenadas del mundo (sin batch de texto, por ahora).
 */
object Hud {
    fun render(r: ShapeRenderer, cam: OrthographicCamera, c: Controls) {
        val x0 = cam.position.x - cam.viewportWidth / 2f
        val y0 = cam.position.y - cam.viewportHeight / 2f
        val w = cam.viewportWidth
        val h = cam.viewportHeight

        r.begin(ShapeRenderer.ShapeType.Line)

        // Divisorias
        r.color = Color(1f, 1f, 1f, 0.15f)
        r.line(x0 + w / 2f, y0, x0 + w / 2f, y0 + h)
        r.line(x0, y0 + h / 2f, x0 + w, y0 + h / 2f)

        // Resalto por zona activa
        r.color = Color.YELLOW
        if (c.throttle > 0.5f) r.rect(x0 + w / 2f, y0, w / 2f, h / 2f)
        if (c.brake > 0.5f)    r.rect(x0,           y0, w / 2f, h / 2f)
        if (c.lean > 0.5f)     r.rect(x0 + w / 2f, y0 + h / 2f, w / 2f, h / 2f)
        if (c.lean < -0.5f)    r.rect(x0,           y0 + h / 2f, w / 2f, h / 2f)

        r.end()
    }
}
