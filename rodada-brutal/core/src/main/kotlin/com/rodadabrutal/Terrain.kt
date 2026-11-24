package com.rodadabrutal

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.ChainShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.World
import kotlin.math.sin
import kotlin.random.Random

/**
 * Terreno procedural: colinas y rampas generadas con ruido sumado de senos.
 * Se construye como un ChainShape estático.
 */
class Terrain(world: World, length: Float, seed: Long) {

    private val points: Array<Vector2>
    private val minY: Float
    private val maxX: Float

    init {
        val rnd = Random(seed)
        val list = mutableListOf<Vector2>()
        val step = 0.5f
        var x = -8f
        val a1 = 1.0f + rnd.nextFloat() * 0.5f
        val a2 = 0.5f + rnd.nextFloat() * 0.4f
        val a3 = 0.3f + rnd.nextFloat() * 0.3f
        val k1 = 0.25f
        val k2 = 0.7f + rnd.nextFloat() * 0.3f
        val k3 = 1.6f + rnd.nextFloat() * 0.6f
        val phase1 = rnd.nextFloat() * MathUtils.PI2
        val phase2 = rnd.nextFloat() * MathUtils.PI2
        val phase3 = rnd.nextFloat() * MathUtils.PI2

        while (x < length) {
            val warmup = MathUtils.clamp((x + 8f) / 12f, 0f, 1f)
            val y = warmup * (
                a1 * sin(x * k1 + phase1) +
                a2 * sin(x * k2 + phase2) +
                a3 * sin(x * k3 + phase3)
            )
            list.add(Vector2(x, y))
            x += step
        }
        points = list.toTypedArray()
        minY = (points.minOfOrNull { it.y } ?: 0f) - 6f
        maxX = points.lastOrNull()?.x ?: length

        val body = world.createBody(BodyDef().apply { type = BodyDef.BodyType.StaticBody })
        val chain = ChainShape().apply { createChain(points) }
        body.createFixture(FixtureDef().apply {
            shape = chain
            friction = 0.95f
            restitution = 0.0f
        })
        chain.dispose()
    }

    fun heightAt(x: Float): Float {
        // Búsqueda binaria sobre los puntos.
        if (x <= points.first().x) return points.first().y
        if (x >= points.last().x) return points.last().y
        var lo = 0
        var hi = points.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (points[mid].x <= x) lo = mid else hi = mid
        }
        val a = points[lo]
        val b = points[hi]
        val t = (x - a.x) / (b.x - a.x)
        return a.y + t * (b.y - a.y)
    }

    fun render(r: ShapeRenderer) {
        r.begin(ShapeRenderer.ShapeType.Filled)
        r.color = Color.FOREST
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            // Triángulo-tira entre el suelo y la línea de abajo
            r.triangle(a.x, minY, b.x, minY, b.x, b.y)
            r.triangle(a.x, minY, b.x, b.y, a.x, a.y)
        }
        r.end()

        r.begin(ShapeRenderer.ShapeType.Line)
        r.color = Color.LIME
        for (i in 0 until points.size - 1) {
            r.line(points[i], points[i + 1])
        }
        r.end()
    }
}
