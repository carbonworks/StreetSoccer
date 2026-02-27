package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.streetsoccer.ecs.components.BallShadowComponent
import com.streetsoccer.ecs.components.TransformComponent
import com.streetsoccer.ecs.components.VelocityComponent
import com.streetsoccer.ecs.components.VisualComponent
import com.streetsoccer.ecs.transform
import com.streetsoccer.ecs.visual
import com.streetsoccer.physics.TuningConstants
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
 * Ball shadow rendering: Shadow entities are identified exclusively by the
 * BallShadowComponent tag added by InputSystem. Shadows are drawn using a programmatic
 * ellipse texture with opacity computed by InputSystem per physics-and-tuning.md Section 5:
 *   shadowAlpha = max(0.1, 1.0 - ball.height / SHADOW_FADE_HEIGHT)
 *
 * Big Bomb visual feedback (GDD Section 9, ui-hud-layout.md Section 8):
 * - Progressive red color ramp on the ball during Big Bomb flight based on depth
 * - Full-screen white flash on Big Bomb launch, decaying over FLASH_DURATION seconds
 * - Placeholder meteor sprite swap when depth reaches BIG_BOMB_COLOR_MAX_DEPTH
 */
class RenderSystem(
    private val batch: SpriteBatch,
    private val inputSystem: InputSystem
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

        /** Size of the procedural meteor fireball texture in pixels. */
        private const val METEOR_TEXTURE_SIZE = 64

        /** Duration of the Big Bomb screen-edge flash in seconds. */
        private const val FLASH_DURATION = 0.15f

        /** Initial alpha of the Big Bomb screen-edge flash. */
        private const val FLASH_INITIAL_ALPHA = 0.5f

        /** Design resolution width for the full-screen flash quad. */
        private const val WORLD_WIDTH = 1920f

        /** Design resolution height for the full-screen flash quad. */
        private const val WORLD_HEIGHT = 1080f
    }

    private val ballShadowMapper = mapperFor<BallShadowComponent>()
    private val velocityMapper = mapperFor<VelocityComponent>()

    /**
     * Programmatic shadow ellipse texture: a white ellipse with soft edges on a
     * transparent background. The entity's black tint and computed opacity are
     * applied by the SpriteBatch to produce the final dark, fading shadow.
     */
    private val shadowTexture: Texture
    private val shadowRegion: TextureRegion

    /**
     * Procedural meteor/fireball texture: an orange-red gradient circle used as
     * a placeholder when the ball reaches max Big Bomb depth.
     */
    private val meteorTexture: Texture
    private val meteorRegion: TextureRegion

    /**
     * 1x1 white pixel texture used for drawing the full-screen flash quad.
     */
    private val whitePixelTexture: Texture
    private val whitePixelRegion: TextureRegion

    /**
     * Remaining time for the Big Bomb launch flash effect.
     * When > 0, a full-screen white quad is drawn with decaying alpha.
     */
    private var flashTimer: Float = 0f

    /**
     * Tracks whether Big Bomb was active on the previous frame,
     * so we can detect the transition and trigger the flash.
     */
    private var wasBigBombActive: Boolean = false

    /**
     * Reusable Color instance for Big Bomb tint calculations.
     * Avoids per-frame allocation in processEntity.
     */
    private val bigBombTintColor = Color(Color.WHITE)

    init {
        // --- Shadow ellipse texture ---
        val shadowPixmap = Pixmap(SHADOW_TEXTURE_WIDTH, SHADOW_TEXTURE_HEIGHT, Pixmap.Format.RGBA8888)
        shadowPixmap.setColor(0f, 0f, 0f, 0f) // Transparent background
        shadowPixmap.fill()

        val cx = SHADOW_TEXTURE_WIDTH / 2f
        val cy = SHADOW_TEXTURE_HEIGHT / 2f
        val rx = cx
        val ry = cy

        for (py in 0 until SHADOW_TEXTURE_HEIGHT) {
            for (px in 0 until SHADOW_TEXTURE_WIDTH) {
                val dx = (px - cx) / rx
                val dy = (py - cy) / ry
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (dist <= 1f) {
                    val alpha = (1f - dist * dist).coerceIn(0f, 1f)
                    shadowPixmap.setColor(1f, 1f, 1f, alpha)
                    shadowPixmap.drawPixel(px, py)
                }
            }
        }

        shadowTexture = Texture(shadowPixmap)
        shadowTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        shadowRegion = TextureRegion(shadowTexture)
        shadowPixmap.dispose()

        // --- Meteor fireball texture (orange-red gradient circle) ---
        val meteorPixmap = Pixmap(METEOR_TEXTURE_SIZE, METEOR_TEXTURE_SIZE, Pixmap.Format.RGBA8888)
        meteorPixmap.setColor(0f, 0f, 0f, 0f)
        meteorPixmap.fill()

        val meteorCenter = METEOR_TEXTURE_SIZE / 2f
        val meteorRadius = meteorCenter

        for (py in 0 until METEOR_TEXTURE_SIZE) {
            for (px in 0 until METEOR_TEXTURE_SIZE) {
                val dx = (px - meteorCenter) / meteorRadius
                val dy = (py - meteorCenter) / meteorRadius
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (dist <= 1f) {
                    // Radial gradient: bright yellow-orange at center, deep red at edge
                    val r = 1f
                    val g = (1f - dist) * 0.7f  // Orange-yellow center, fades to 0
                    val b = (1f - dist) * 0.2f   // Slight warm glow at center
                    val a = (1f - dist * dist).coerceIn(0f, 1f) // Soft edge falloff
                    meteorPixmap.setColor(r, g, b, a)
                    meteorPixmap.drawPixel(px, py)
                }
            }
        }

        meteorTexture = Texture(meteorPixmap)
        meteorTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        meteorRegion = TextureRegion(meteorTexture)
        meteorPixmap.dispose()

        // --- White pixel texture for flash quad ---
        val whitePixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        whitePixmap.setColor(1f, 1f, 1f, 1f)
        whitePixmap.fill()
        whitePixelTexture = Texture(whitePixmap)
        whitePixelRegion = TextureRegion(whitePixelTexture)
        whitePixmap.dispose()
    }

    override fun update(deltaTime: Float) {
        // Detect Big Bomb launch transition to trigger screen flash
        val bigBombActive = inputSystem.isBigBombActive
        if (bigBombActive && !wasBigBombActive) {
            flashTimer = FLASH_DURATION
        }
        wasBigBombActive = bigBombActive

        // Tick flash timer
        if (flashTimer > 0f) {
            flashTimer = (flashTimer - deltaTime).coerceAtLeast(0f)
        }

        batch.begin()
        super.update(deltaTime)

        // Draw Big Bomb screen flash AFTER all entities
        if (flashTimer > 0f) {
            val flashAlpha = FLASH_INITIAL_ALPHA * (flashTimer / FLASH_DURATION)
            batch.setColor(1f, 1f, 1f, flashAlpha)
            batch.draw(whitePixelRegion, 0f, 0f, WORLD_WIDTH, WORLD_HEIGHT)
        }

        // Reset batch color so tints from shadows/entities don't bleed into other draws
        batch.setColor(1f, 1f, 1f, 1f)
        batch.end()
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val visual = entity.visual ?: return
        val transform = entity.transform ?: return

        // Check if this entity is the ball during a Big Bomb flight
        val isBigBombBall = inputSystem.isBigBombActive && velocityMapper.has(entity)

        // Calculate Big Bomb depth-based overlay for the ball
        var overlayAlpha = 0f
        if (isBigBombBall) {
            val normalizedDepth = (TuningConstants.HORIZON_Y - transform.y) / TuningConstants.HORIZON_Y
            overlayAlpha = ((normalizedDepth - TuningConstants.BIG_BOMB_COLOR_START_DEPTH) /
                (TuningConstants.BIG_BOMB_COLOR_MAX_DEPTH - TuningConstants.BIG_BOMB_COLOR_START_DEPTH))
                .coerceIn(0f, 1f)
        }

        // Determine which texture region to use for this entity.
        // Swap to meteor sprite when Big Bomb reaches max depth.
        val usesMeteor = isBigBombBall && overlayAlpha >= 1f
        val region = if (usesMeteor) {
            meteorRegion
        } else {
            visual.region ?: getShadowRegionIfApplicable(entity) ?: return
        }

        // Apply color: lerp from entity tint toward red for Big Bomb ball
        if (isBigBombBall && overlayAlpha > 0f && !usesMeteor) {
            bigBombTintColor.set(visual.tint)
            bigBombTintColor.r = bigBombTintColor.r + (1f - bigBombTintColor.r) * overlayAlpha
            bigBombTintColor.g = bigBombTintColor.g * (1f - overlayAlpha)
            bigBombTintColor.b = bigBombTintColor.b * (1f - overlayAlpha)
            bigBombTintColor.a = visual.opacity
            batch.color = bigBombTintColor
        } else if (usesMeteor) {
            // Meteor uses full white tint so the procedural texture colors show through
            batch.setColor(1f, 1f, 1f, visual.opacity)
        } else {
            batch.color = visual.tint
            batch.color.a = visual.opacity
        }

        val width = region.regionWidth * transform.screenScale
        val height = region.regionHeight * transform.screenScale

        batch.draw(
            region,
            transform.x - width / 2f,
            transform.y + transform.height,
            width,
            height
        )

        // Reset batch color after Big Bomb tinted entity so other entities aren't affected
        if (isBigBombBall) {
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    /**
     * If the entity is a ball shadow, return the programmatic shadow region.
     *
     * Detection uses BallShadowComponent exclusively. InputSystem tags all
     * shadow entities with this component at creation time.
     */
    private fun getShadowRegionIfApplicable(entity: Entity): TextureRegion? {
        if (ballShadowMapper.has(entity)) {
            return shadowRegion
        }
        return null
    }

    override fun dispose() {
        shadowTexture.dispose()
        meteorTexture.dispose()
        whitePixelTexture.dispose()
    }
}
