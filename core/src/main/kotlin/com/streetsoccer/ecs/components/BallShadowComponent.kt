package com.streetsoccer.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool.Poolable

/**
 * Tag component that marks an entity as the ball's ground-plane shadow.
 *
 * The shadow entity is created by InputSystem when a ball is spawned
 * and removed when the ball flight ends. RenderSystem uses this tag
 * to identify shadow entities and render them with a programmatic
 * dark ellipse texture, applying depth-scaled size and height-based
 * opacity fading per physics-and-tuning.md Section 5.
 *
 * Shadow position, scale, and opacity are updated each frame by
 * InputSystem.updateShadow() — RenderSystem only handles drawing.
 */
class BallShadowComponent : Component, Poolable {
    override fun reset() {
        // Tag component — no mutable state to reset.
    }
}
