package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.streetsoccer.ecs.components.SpawnLaneComponent
import com.streetsoccer.ecs.components.TransformComponent
import ktx.ashley.allOf

class SpawnSystem : IteratingSystem(allOf(TransformComponent::class, SpawnLaneComponent::class).get()) {

    override fun processEntity(entity: Entity, deltaTime: Float) {
        // Manage moving-target lifecycle: spawn, translate along lane, despawn when off-screen
    }
}
