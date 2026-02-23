package com.streetsoccer.level

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
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

        // Parse spawn lanes — lane definition entities for SpawnSystem
        val spawnLanes = json.get("spawn_lanes")
        if (spawnLanes != null) {
            for (i in 0 until spawnLanes.size) {
                val element = spawnLanes.get(i)
                val entity = Entity()

                val startX = element.getFloat("start_x")
                val startY = element.getFloat("start_y")
                val endX = element.getFloat("end_x")
                val endY = element.getFloat("end_y")

                // Compute normalized direction from start to end
                val dir = Vector2(endX - startX, endY - startY).nor()

                val transform = engine.createComponent(TransformComponent::class.java).apply {
                    x = startX
                    y = startY
                }

                val speedRange = element.get("speed_range")
                val spawnLane = engine.createComponent(SpawnLaneComponent::class.java).apply {
                    laneId = element.getString("id")
                    directionX = dir.x
                    directionY = dir.y
                    minSpeed = speedRange.getFloat(0)
                    maxSpeed = speedRange.getFloat(1)
                    // Default interval — can be overridden per-lane in JSON
                    spawnIntervalSeconds = if (element.has("spawn_interval")) {
                        element.getFloat("spawn_interval")
                    } else {
                        5.0f
                    }
                }

                entity.add(transform)
                entity.add(spawnLane)
                engine.addEntity(entity)
            }
        }
    }
}
