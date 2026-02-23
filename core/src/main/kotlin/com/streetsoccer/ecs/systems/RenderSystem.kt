package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.streetsoccer.ecs.components.TransformComponent
import com.streetsoccer.ecs.components.VisualComponent
import com.streetsoccer.ecs.transform
import com.streetsoccer.ecs.visual
import ktx.ashley.allOf
import java.util.Comparator

class RenderSystem(
    private val batch: SpriteBatch
) : SortedIteratingSystem(
    allOf(TransformComponent::class, VisualComponent::class).get(),
    Comparator<Entity> { e1, e2 ->
        val v1 = e1.visual
        val v2 = e2.visual
        val t1 = e1.transform
        val t2 = e2.transform
        
        // Z-layer sort first (layer 0 = foreground, layer 4 = sky)
        val layerDiff = v2.renderLayer.compareTo(v1.renderLayer)
        if (layerDiff != 0) return@Comparator layerDiff
        
        // Then sort by Y-position (higher Y = further away = drawn first)
        t2.y.compareTo(t1.y)
    }
) {

    override fun update(deltaTime: Float) {
        batch.begin()
        super.update(deltaTime)
        batch.end()
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val visual = entity.visual
        val transform = entity.transform

        val region = visual.region ?: return
        
        batch.color = visual.tint
        batch.color.a = visual.opacity
        
        val width = region.regionWidth * transform.screenScale
        val height = region.regionHeight * transform.screenScale
        
        batch.draw(
            region,
            transform.x - width / 2f,
            transform.y + transform.height,
            width,
            height
        )
    }
}
