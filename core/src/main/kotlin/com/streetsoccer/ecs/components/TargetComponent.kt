package com.streetsoccer.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool.Poolable

class TargetComponent : Component, Poolable {
    var basePoints: Int = 100
    var targetTypeId: String = ""

    override fun reset() {
        basePoints = 100
        targetTypeId = ""
    }
}
