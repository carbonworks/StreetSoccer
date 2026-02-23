package com.streetsoccer.level

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.JsonReader
import com.streetsoccer.ecs.components.*

class LevelLoader(private val engine: Engine, private val world: World) {

    fun loadLevel(levelPath: String) {
        val file = Gdx.files.internal(levelPath)
        val json = JsonReader().parse(file)

        // Parse static colliders
        val colliders = json.get("static_colliders")
        for (i in 0 until colliders.size) {
            val element = colliders.get(i)
            val entity = Entity()

            val transform = engine.createComponent(TransformComponent::class.java).apply {
                x = element.getFloat("x")
                y = element.getFloat("y")
            }

            val collider = engine.createComponent(ColliderComponent::class.java).apply {
                isSensor = false
                // Box2D body creation deferred to LevelScreen initialization
            }

            entity.add(transform)
            entity.add(collider)
            engine.addEntity(entity)
        }

        // Parse target sensors
        val targets = json.get("target_sensors")
        for (i in 0 until targets.size) {
            val element = targets.get(i)
            val entity = Entity()

            val transform = engine.createComponent(TransformComponent::class.java).apply {
                x = element.getFloat("x")
                y = element.getFloat("y")
            }

            val target = engine.createComponent(TargetComponent::class.java).apply {
                basePoints = element.getInt("points")
                targetTypeId = element.getString("type")
            }

            val visual = engine.createComponent(VisualComponent::class.java).apply {
                // TextureRegion assignment deferred to asset loading
            }

            val collider = engine.createComponent(ColliderComponent::class.java).apply {
                isSensor = true
                // Box2D sensor body creation deferred to LevelScreen initialization
            }

            entity.add(transform)
            entity.add(target)
            entity.add(visual)
            entity.add(collider)
            engine.addEntity(entity)
        }
    }
}
