package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.streetsoccer.ecs.components.BallShadowComponent
import com.streetsoccer.ecs.components.TransformComponent
import com.streetsoccer.ecs.components.VisualComponent
import com.streetsoccer.ecs.transform
import com.streetsoccer.ecs.visual
import ktx.ashley.allOf
import ktx.ashley.mapperFor
import java.util.Comparator
import kotlin.math.sqrt

/**
 * Renders all entities with Transform + Visual components, sorted back-to-front.
 *
 * Sort order: Z-layer descending (sky first), then Y-position descending (far first).
 * This places the ball shadow (renderLayer 1) behind the ball sprite (renderLayer 0)
 * per environment-z-depth-and-collosion.md Section 1 and technical-architecture.md Section 8.
 *
 * Ball shadow rendering: Shadow entities are identified by the BallShadowComponent tag
 * or, as a fallback, by having a null texture region with a black tint (the signature
 * of the shadow entity created by InputSystem). Shadows are drawn using a programmatic
 * ellipse texture with opacity computed by InputSystem per physics-and-tuning.md Section 5:
 *   shadowAlpha = max(0.1, 1.0 - ball.height / SHADOW_FADE_HEIGHT)
 */
class RenderSystem(
    private val batch: SpriteBatch
) : SortedIteratingSystem(
    allOf(TransformComponent::class, VisualComponent::class).get(),
    Comparator<Entity> { e1, e2 ->
        val v1 = e1.visual ?: return@Comparator 0
        val v2 = e2.visual ?: return@Comparator 0
        val t1 = e1.transform ?: return@Comparator 0
        val t2 = e2.transform ?: return@Comparator 0

        // Z-layer sort first (layer 0 = foreground, layer 4 = sky)
        val layerDiff = v2.renderLayer.compareTo(v1.renderLayer)
        if (layerDiff != 0) return@Comparator layerDiff

        // Then sort by Y-position (higher Y = further away = drawn first)
        t2.y.compareTo(t1.y)
    }
), Disposable {

    companion object {
        /** Width of the programmatic shadow ellipse texture in pixels. */
        private const val SHADOW_TEXTURE_WIDTH = 64

        /** Height of the programmatic shadow ellipse texture in pixels. */
        private const val SHADOW_TEXTURE_HEIGHT = 32

        /**
         * Render layer used by InputSystem for the ball shadow.
         * Used as a fallback heuristic to identify shadow entities when
         * BallShadowComponent is not present.
         */
        private const val SHADOW_RENDER_LAYER = 1
    }

    private val ballShadowMapper = mapperFor<BallShadowComponent>()

    /**
     * Programmatic shadow ellipse texture: a white ellipse with soft edges on a
     * transparent background. The entity's black tint and computed opacity are
     * applied by the SpriteBatch to produce the final dark, fading shadow.
     */
    private val shadowTexture: Texture
    private val shadowRegion: TextureRegion

    init {
        val pixmap = Pixmap(SHADOW_TEXTURE_WIDTH, SHADOW_TEXTURE_HEIGHT, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f) // Transparent background
        pixmap.fill()

        // Draw a filled ellipse with a soft radial falloff.
        // Center of the ellipse in pixmap coordinates.
        val cx = SHADOW_TEXTURE_WIDTH / 2f
        val cy = SHADOW_TEXTURE_HEIGHT / 2f
        val rx = cx // semi-axis X = half width
        val ry = cy // semi-axis Y = half height

        for (py in 0 until SHADOW_TEXTURE_HEIGHT) {
            for (px in 0 until SHADOW_TEXTURE_WIDTH) {
                // Normalized distance from center (0 at center, 1 at ellipse edge)
                val dx = (px - cx) / rx
                val dy = (py - cy) / ry
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (dist <= 1f) {
                    // Soft falloff: full opacity at center, fading toward edges
                    val alpha = (1f - dist * dist).coerceIn(0f, 1f)
                    pixmap.setColor(1f, 1f, 1f, alpha)
                    pixmap.drawPixel(px, py)
                }
            }
        }

        shadowTexture = Texture(pixmap)
        shadowTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        shadowRegion = TextureRegion(shadowTexture)
        pixmap.dispose()
    }

    override fun update(deltaTime: Float) {
        batch.begin()
        super.update(deltaTime)
        // Reset batch color so tints from shadows/entities don't bleed into other draws
        batch.setColor(1f, 1f, 1f, 1f)
        batch.end()
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val visual = entity.visual ?: return
        val transform = entity.transform ?: return

        // Determine which texture region to use for this entity.
        val region = visual.region ?: getShadowRegionIfApplicable(entity, visual) ?: return

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

    /**
     * If the entity is a ball shadow, return the programmatic shadow region.
     *
     * Detection uses BallShadowComponent as the primary signal. As a fallback
     * (until InputSystem adds BallShadowComponent to shadow entities), we
     * identify shadows by their render-layer heuristic: renderLayer == SHADOW_RENDER_LAYER
     * combined with a black tint (r < 0.1, g < 0.1, b < 0.1).
     */
    private fun getShadowRegionIfApplicable(entity: Entity, visual: VisualComponent): TextureRegion? {
        // Primary: tagged with BallShadowComponent
        if (ballShadowMapper.has(entity)) {
            return shadowRegion
        }

        // Fallback heuristic: shadow render layer + black tint + no assigned region
        if (visual.renderLayer == SHADOW_RENDER_LAYER
            && visual.tint.r < 0.1f
            && visual.tint.g < 0.1f
            && visual.tint.b < 0.1f
        ) {
            return shadowRegion
        }

        return null
    }

    override fun dispose() {
        shadowTexture.dispose()
    }
}
