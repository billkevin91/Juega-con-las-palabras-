package com.rodadabrutal

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.physics.box2d.World

/**
 * Pantalla principal. Un mundo Box2D con gravedad, un terreno y una moto.
 * Cámara ortográfica que sigue al chasis.
 */
class GameScreen : ScreenAdapter() {

    private val worldWidthMeters = 24f
    private val gravity = Vector2(0f, -22f)

    private val world = World(gravity, true)
    private val cam = OrthographicCamera()
    private val shape = ShapeRenderer()
    private val controls = Controls()

    private val terrain: Terrain
    private val bike: Bike

    private var accumulator = 0f
    private val step = 1f / 60f

    init {
        Box2D.init()
        cam.setToOrtho(false, worldWidthMeters, worldWidthMeters * 9f / 16f)
        terrain = Terrain(world, length = 300f, seed = 7L)
        bike = Bike(world, Vector2(3f, terrain.heightAt(3f) + 2f))
    }

    override fun render(delta: Float) {
        controls.poll()
        bike.applyControls(controls)
        stepPhysics(delta)
        if (bike.isWasted()) respawn()

        val c = bike.chassisPos
        cam.position.x = MathUtils.lerp(cam.position.x, c.x + 4f, 0.15f)
        cam.position.y = MathUtils.lerp(cam.position.y, c.y + 1.5f, 0.15f)
        cam.update()

        Gdx.gl.glClearColor(0.08f, 0.12f, 0.20f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shape.projectionMatrix = cam.combined

        terrain.render(shape)
        bike.render(shape)
        Hud.render(shape, cam, controls)
    }

    private fun stepPhysics(delta: Float) {
        accumulator += delta.coerceAtMost(0.1f)
        while (accumulator >= step) {
            world.step(step, 8, 3)
            accumulator -= step
        }
    }

    private fun respawn() {
        val x = bike.chassisPos.x - 6f
        bike.teleport(Vector2(x, terrain.heightAt(x) + 2f))
    }

    override fun resize(width: Int, height: Int) {
        cam.viewportWidth = worldWidthMeters
        cam.viewportHeight = worldWidthMeters * height / width.toFloat()
    }

    override fun dispose() {
        world.dispose()
        shape.dispose()
    }
}
