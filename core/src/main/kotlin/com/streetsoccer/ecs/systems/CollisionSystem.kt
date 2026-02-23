package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.streetsoccer.physics.PhysicsContactListener

class CollisionSystem(
    val contactListener: PhysicsContactListener
) : EntitySystem() {

    override fun update(deltaTime: Float) {
        // Drain the queue
        val impacts = contactListener.collisionQueue.toList()
        contactListener.collisionQueue.clear()

        for ((entityA, entityB) in impacts) {
            // Check for Targets
            // Check for Obstacles
            // Decrease durability / trigger shatter / Add score
        }
    }
}
