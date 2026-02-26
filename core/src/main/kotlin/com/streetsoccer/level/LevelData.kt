package com.streetsoccer.level

import com.badlogic.gdx.utils.JsonValue

/**
 * Parsed level configuration from suburban-crossroads.json.
 *
 * Holds all data needed to bootstrap ECS entities and configure
 * game systems for a specific level: catcher position, spawn lanes,
 * and collider geometry. Parsed once in LoadingScreen and passed
 * through to ECSBootstrapper via GameBootstrapper.
 */
data class LevelData(
    val levelId: String,
    val catcherX: Float,
    val catcherY: Float,
    val catcherRadius: Float,
    val spawnLanes: List<SpawnLaneData>,
    val staticColliders: List<ColliderData>,
    val targetSensors: List<TargetSensorData>
) {
    companion object {
        /** Parse a LevelData from a LibGDX JsonValue (root of suburban-crossroads.json). */
        fun fromJson(json: JsonValue): LevelData {
            val meta = json.get("level_meta")
            val levelId = meta.getString("id")

            val catcherSpawn = json.get("catcher_spawn_point")
            val catcherX = catcherSpawn.getFloat("x")
            val catcherY = catcherSpawn.getFloat("y")
            val catcherRadius = catcherSpawn.getFloat("catch_radius")

            val spawnLanes = mutableListOf<SpawnLaneData>()
            json.get("spawn_lanes")?.forEach { lane ->
                spawnLanes.add(
                    SpawnLaneData(
                        id = lane.getString("id"),
                        targetType = lane.getString("target_type"),
                        startX = lane.getFloat("start_x"),
                        startY = lane.getFloat("start_y"),
                        endX = lane.getFloat("end_x"),
                        endY = lane.getFloat("end_y"),
                        zLayer = lane.getInt("z_layer"),
                        speedMin = lane.get("speed_range").getFloat(0),
                        speedMax = lane.get("speed_range").getFloat(1)
                    )
                )
            }

            val staticColliders = mutableListOf<ColliderData>()
            json.get("static_colliders")?.forEach { collider ->
                staticColliders.add(
                    ColliderData(
                        id = collider.getString("id"),
                        type = collider.getString("type"),
                        x = collider.getFloat("x"),
                        y = collider.getFloat("y"),
                        width = collider.getFloat("width"),
                        height = collider.getFloat("height"),
                        zLayer = collider.getInt("z_layer"),
                        restitution = collider.getFloat("restitution")
                    )
                )
            }

            val targetSensors = mutableListOf<TargetSensorData>()
            json.get("target_sensors")?.forEach { sensor ->
                targetSensors.add(
                    TargetSensorData(
                        id = sensor.getString("id"),
                        type = sensor.getString("type"),
                        x = sensor.getFloat("x"),
                        y = sensor.getFloat("y"),
                        width = sensor.getFloat("width"),
                        height = sensor.getFloat("height"),
                        points = sensor.getInt("points"),
                        zLayer = sensor.getInt("z_layer")
                    )
                )
            }

            return LevelData(
                levelId = levelId,
                catcherX = catcherX,
                catcherY = catcherY,
                catcherRadius = catcherRadius,
                spawnLanes = spawnLanes,
                staticColliders = staticColliders,
                targetSensors = targetSensors
            )
        }
    }
}

data class SpawnLaneData(
    val id: String,
    val targetType: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val zLayer: Int,
    val speedMin: Float,
    val speedMax: Float
)

data class ColliderData(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val zLayer: Int,
    val restitution: Float
)

data class TargetSensorData(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val points: Int,
    val zLayer: Int
)
