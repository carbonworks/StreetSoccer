package com.streetsoccer.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.utils.Pool.Poolable

class ColliderComponent : Component, Poolable {
    var body: Body? = null
    var isSensor: Boolean = false

    override fun reset() {
        body = null
        isSensor = false
    }
}
