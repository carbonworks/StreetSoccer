package com.streetsoccer.physics

import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import com.badlogic.ashley.core.Entity
import com.streetsoccer.ecs.components.TransformComponent

class PhysicsContactListener : ContactListener {
    // Array to queue up collisions for the system to process later on the main thread
    val collisionQueue = mutableListOf<Pair<Entity, Entity>>()

    override fun beginContact(contact: Contact) {
        val fixtureA = contact.fixtureA
        val fixtureB = contact.fixtureB

        val entityA = fixtureA.body.userData as? Entity
        val entityB = fixtureB.body.userData as? Entity

        if (entityA != null && entityB != null) {
            // Need to apply Z-depth filtering to verify collision
            val transformA = entityA.getComponent(TransformComponent::class.java)
            val transformB = entityB.getComponent(TransformComponent::class.java)

            if (transformA != null && transformB != null) {
                // If they intersect on Y and Height limits overlap, it's valid
                val yDiff = Math.abs(transformA.y - transformB.y)
                if (yDiff < TuningConstants.DEPTH_COLLISION_TOLERANCE) {
                    collisionQueue.add(Pair(entityA, entityB))
                }
            }
        }
    }

    override fun endContact(contact: Contact) {}
    override fun preSolve(contact: Contact, oldManifold: Manifold) {}
    override fun postSolve(contact: Contact, impulse: ContactImpulse) {}
}
