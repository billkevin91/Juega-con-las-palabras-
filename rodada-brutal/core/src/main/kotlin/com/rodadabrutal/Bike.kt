package com.rodadabrutal

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.physics.box2d.joints.WheelJoint
import com.badlogic.gdx.physics.box2d.joints.WheelJointDef
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef
import kotlin.math.abs

/**
 * Moto 2D: chasis, dos ruedas con suspensión (WheelJoint) y un cuerpo del piloto
 * articulado al chasis (RevoluteJoint). Todo lo que es de la moto comparte
 * filter.groupIndex = -1 para no colisionar entre sí.
 */
class Bike(private val world: World, start: Vector2) {

    val chassis: Body
    val rearWheel: Body
    val frontWheel: Body
    val rider: Body

    private val rearJoint: WheelJoint
    private val frontJoint: WheelJoint
    private val riderJoint: RevoluteJoint

    private val wheelRadius = 0.32f
    private val wheelbase = 1.5f
    private val maxMotorTorque = 55f
    private val maxMotorSpeed = 34f
    private val leanTorque = 11f

    private val filterGroup: Short = -1

    init {
        chassis = createChassis(start)
        rearWheel = createWheel(Vector2(start.x - wheelbase / 2f, start.y - 0.35f))
        frontWheel = createWheel(Vector2(start.x + wheelbase / 2f, start.y - 0.35f))
        rider = createRider(Vector2(start.x, start.y + 0.55f))

        rearJoint = world.createJoint(WheelJointDef().apply {
            initialize(chassis, rearWheel, rearWheel.position, Vector2(0f, 1f))
            enableMotor = true
            maxMotorTorque = this@Bike.maxMotorTorque
            frequencyHz = 5f
            dampingRatio = 0.75f
        }) as WheelJoint

        frontJoint = world.createJoint(WheelJointDef().apply {
            initialize(chassis, frontWheel, frontWheel.position, Vector2(0f, 1f))
            enableMotor = false
            frequencyHz = 5f
            dampingRatio = 0.75f
        }) as WheelJoint

        riderJoint = world.createJoint(RevoluteJointDef().apply {
            initialize(chassis, rider, Vector2(start.x, start.y + 0.25f))
            enableLimit = true
            lowerAngle = -0.6f
            upperAngle = 0.6f
        }) as RevoluteJoint
    }

    private fun createChassis(pos: Vector2): Body {
        val body = world.createBody(BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(pos)
            angularDamping = 0.1f
        })
        val shape = PolygonShape().apply { setAsBox(0.85f, 0.18f) }
        body.createFixture(FixtureDef().apply {
            this.shape = shape
            density = 1.3f
            friction = 0.3f
            restitution = 0.0f
            filter.groupIndex = filterGroup
        })
        shape.dispose()
        return body
    }

    private fun createWheel(pos: Vector2): Body {
        val body = world.createBody(BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(pos)
            angularDamping = 0.02f
        })
        val shape = CircleShape().apply { radius = wheelRadius }
        body.createFixture(FixtureDef().apply {
            this.shape = shape
            density = 1.0f
            friction = 2.2f
            restitution = 0.05f
            filter.groupIndex = filterGroup
        })
        shape.dispose()
        return body
    }

    private fun createRider(pos: Vector2): Body {
        val body = world.createBody(BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(pos)
        })
        val shape = PolygonShape().apply { setAsBox(0.22f, 0.38f) }
        body.createFixture(FixtureDef().apply {
            this.shape = shape
            density = 0.7f
            friction = 0.4f
            restitution = 0.0f
            filter.groupIndex = filterGroup
        })
        shape.dispose()
        return body
    }

    val chassisPos: Vector2 get() = chassis.position

    fun applyControls(c: Controls) {
        val throttle = c.throttle
        val brake = c.brake

        rearJoint.enableMotor = true
        when {
            brake > 0.1f -> {
                rearJoint.motorSpeed = 0f
                rearJoint.maxMotorTorque = maxMotorTorque * 1.6f
            }
            throttle > 0.1f -> {
                rearJoint.motorSpeed = -maxMotorSpeed * throttle
                rearJoint.maxMotorTorque = maxMotorTorque
            }
            else -> {
                rearJoint.motorSpeed = 0f
                rearJoint.maxMotorTorque = 2f
            }
        }

        // Inclinación del piloto: mueve el centro de masa adelante/atrás.
        chassis.applyTorque(c.lean * leanTorque, true)
    }

    fun isWasted(): Boolean {
        // Si el piloto toca el suelo cabeza abajo demasiado tiempo lo marca.
        // Simple: ángulo del chasis fuera de rango y velocidad baja.
        val angle = chassis.angle
        val badAngle = abs(angle % (Math.PI * 2).toFloat()) in 2.4f..3.9f
        val slow = chassis.linearVelocity.len2() < 0.5f
        return badAngle && slow
    }

    fun teleport(pos: Vector2) {
        chassis.setTransform(pos, 0f)
        chassis.setLinearVelocity(0f, 0f)
        chassis.angularVelocity = 0f
        rearWheel.setTransform(pos.x - wheelbase / 2f, pos.y - 0.35f, 0f)
        rearWheel.setLinearVelocity(0f, 0f)
        rearWheel.angularVelocity = 0f
        frontWheel.setTransform(pos.x + wheelbase / 2f, pos.y - 0.35f, 0f)
        frontWheel.setLinearVelocity(0f, 0f)
        frontWheel.angularVelocity = 0f
        rider.setTransform(pos.x, pos.y + 0.55f, 0f)
        rider.setLinearVelocity(0f, 0f)
        rider.angularVelocity = 0f
    }

    fun render(r: ShapeRenderer) {
        r.begin(ShapeRenderer.ShapeType.Filled)

        // Ruedas
        r.color = Color.LIGHT_GRAY
        r.circle(rearWheel.position.x, rearWheel.position.y, wheelRadius, 24)
        r.circle(frontWheel.position.x, frontWheel.position.y, wheelRadius, 24)

        // Chasis
        r.color = Color.ORANGE
        drawOrientedBox(r, chassis, 0.85f, 0.18f)

        // Piloto
        r.color = Color.CYAN
        drawOrientedBox(r, rider, 0.22f, 0.38f)
        r.end()

        // Contorno
        r.begin(ShapeRenderer.ShapeType.Line)
        r.color = Color.DARK_GRAY
        r.circle(rearWheel.position.x, rearWheel.position.y, wheelRadius, 24)
        r.circle(frontWheel.position.x, frontWheel.position.y, wheelRadius, 24)
        r.end()
    }

    private fun drawOrientedBox(r: ShapeRenderer, body: Body, hx: Float, hy: Float) {
        val cos = kotlin.math.cos(body.angle.toDouble()).toFloat()
        val sin = kotlin.math.sin(body.angle.toDouble()).toFloat()
        val cx = body.position.x
        val cy = body.position.y
        // Cuatro vértices del rectángulo en coordenadas locales rotados
        val ox = floatArrayOf(-hx,  hx,  hx, -hx)
        val oy = floatArrayOf(-hy, -hy,  hy,  hy)
        val wx = FloatArray(4)
        val wy = FloatArray(4)
        for (i in 0..3) {
            wx[i] = cx + ox[i] * cos - oy[i] * sin
            wy[i] = cy + ox[i] * sin + oy[i] * cos
        }
        // Dos triángulos
        r.triangle(wx[0], wy[0], wx[1], wy[1], wx[2], wy[2])
        r.triangle(wx[0], wy[0], wx[2], wy[2], wx[3], wy[3])
    }
}
