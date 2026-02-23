package com.streetsoccer.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool.Poolable

class TransformComponent : Component, Poolable {
    var x: Float = 0f
    var y: Float = 0f
    var height: Float = 0f
    var screenScale: Float = 1f
    
    override fun reset() {
        x = 0f
        y = 0f
        height = 0f
        screenScale = 1f
    }
}
