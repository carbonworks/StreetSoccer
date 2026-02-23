package com.streetsoccer.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool.Poolable

class SpinComponent : Component, Poolable {
    var spinX: Float = 0f
    var spinY: Float = 0f

    override fun reset() {
        spinX = 0f
        spinY = 0f
    }
}
