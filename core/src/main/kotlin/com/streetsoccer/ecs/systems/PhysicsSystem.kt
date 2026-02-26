package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.ashley.core.Family
import com.streetsoccer.ecs.components.TransformComponent
import com.streetsoccer.ecs.components.VelocityComponent
import com.streetsoccer.ecs.components.SpinComponent
import com.streetsoccer.ecs.transform
import com.streetsoccer.ecs.velocity
import com.streetsoccer.ecs.spin
import com.streetsoccer.physics.TuningConstants
import kotlin.math.sqrt

/**
 * Applies flight physics (gravity, drag, Magnus effect, spin decay, position
 * integration, ground collision) to every entity with Transform + Velocity + Spin.
 *
 * This system does NOT maintain its own fixed-timestep accumulator.
 * LevelScreen's fixed-timestep loop is authoritative: it calls engine.update()
 * with FIXED_TIMESTEP once per accumulated step, so processEntity receives
 * exactly one physics tick per call.
 */
class PhysicsSystem : IteratingSystem(
    Family.all(TransformComponent::class.java, VelocityComponent::class.java, SpinComponent::class.java).get()
) {

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val transform = entity.transform ?: return
        val velocity = entity.velocity ?: return
        val spin = entity.spin ?: return

        // Gravity
        velocity.vz -= TuningConstants.GRAVITY * deltaTime

        // Air Resistance (Drag)
        velocity.vx *= (1f - TuningConstants.DRAG * deltaTime)
        velocity.vy *= (1f - TuningConstants.DRAG * deltaTime)
        velocity.vz *= (1f - TuningConstants.DRAG * deltaTime)

        // Magnus effect
        val ballSpeed = sqrt(velocity.vx * velocity.vx + velocity.vy * velocity.vy + velocity.vz * velocity.vz)
        val magnusForceX = spin.spinX * ballSpeed * TuningConstants.MAGNUS_COEFFICIENT
        val magnusForceY = spin.spinY * ballSpeed * TuningConstants.MAGNUS_COEFFICIENT
        velocity.vx += magnusForceX * deltaTime
        velocity.vy += magnusForceY * deltaTime

        // Spin Decay
        spin.spinX *= (1f - TuningConstants.SPIN_DECAY * deltaTime)
        spin.spinY *= (1f - TuningConstants.SPIN_DECAY * deltaTime)

        // Position update
        transform.x += velocity.vx * deltaTime
        transform.y += velocity.vy * deltaTime
        transform.height += velocity.vz * deltaTime

        // Ground Collision check
        if (transform.height <= 0) {
            transform.height = 0f
            // Handle surface collision bounce (restitution = 0.3 for asphalt ground)
            velocity.vz = -velocity.vz * 0.3f
            velocity.vx *= 0.3f
            velocity.vy *= 0.3f
        }

        // Screen Scale calculation
        transform.screenScale = 0.05f.coerceAtLeast((TuningConstants.HORIZON_Y - transform.y) / TuningConstants.HORIZON_Y)
    }
}
