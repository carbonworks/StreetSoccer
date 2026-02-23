package com.streetsoccer.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool.Poolable

class VelocityComponent : Component, Poolable {
    var vx: Float = 0f
    var vy: Float = 0f
    var vz: Float = 0f

    override fun reset() {
        vx = 0f
        vy = 0f
        vz = 0f
    }
}
