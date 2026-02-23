package com.streetsoccer.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Pool.Poolable

class VisualComponent : Component, Poolable {
    var region: TextureRegion? = null
    var tint: Color = Color(Color.WHITE)
    var opacity: Float = 1f
    var renderLayer: Int = 0

    override fun reset() {
        region = null
        tint.set(Color.WHITE)
        opacity = 1f
        renderLayer = 0
    }
}
