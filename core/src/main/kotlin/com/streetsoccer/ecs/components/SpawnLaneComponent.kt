package com.streetsoccer.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool.Poolable

class SpawnLaneComponent : Component, Poolable {
    var laneId: String = ""
    var directionX: Float = 0f
    var directionY: Float = 0f
    var minSpeed: Float = 0f
    var maxSpeed: Float = 0f
    var spawnIntervalSeconds: Float = 0f

    override fun reset() {
        laneId = ""
        directionX = 0f
        directionY = 0f
        minSpeed = 0f
        maxSpeed = 0f
        spawnIntervalSeconds = 0f
    }
}
