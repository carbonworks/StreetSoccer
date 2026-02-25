package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.World
import com.streetsoccer.ecs.components.ColliderComponent
import com.streetsoccer.ecs.components.SpinComponent
import com.streetsoccer.ecs.components.TransformComponent
import com.streetsoccer.ecs.components.VelocityComponent
import com.streetsoccer.ecs.components.VisualComponent
import com.streetsoccer.ecs.spin
import com.streetsoccer.ecs.transform
import com.streetsoccer.ecs.visual
import com.streetsoccer.input.FlickResult
import com.streetsoccer.input.InputRouter
import com.streetsoccer.physics.TuningConstants
import com.streetsoccer.services.AudioService
import com.streetsoccer.state.GameState
import com.streetsoccer.state.GameStateManager
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bridges gesture detection (InputRouter / FlickDetector / SteerDetector) to ECS entities.
 *
 * Responsibilities:
 * - On valid flick: create a ball entity with initial velocity from FlickResult
 * - Detect Big Bomb kicks (power >= 0.9, slider >= 0.7)
 * - During BALL_IN_FLIGHT: apply accumulated steer deltas to ball SpinComponent
 * - Create a ball shadow entity that tracks the ball's ground position
 * - Reset SteerDetector swipe counter on each new kick
 *
 * @see com.streetsoccer.input.InputRouter
 * @see com.streetsoccer.input.FlickDetector
 * @see com.streetsoccer.input.SteerDetector
 */
class InputSystem(
    private val inputRouter: InputRouter,
    private val stateManager: GameStateManager,
    private val world: World,
    private val audioService: AudioService,
    private val assetManager: AssetManager? = null
) : EntitySystem() {

    companion object {
        /** Player origin X — bottom center of a 1920x1080 screen (from suburban-crossroads.json). */
        private const val PLAYER_ORIGIN_X = 960f
        /** Player origin Y — slightly above bottom edge so ball is visible at launch. */
        private const val PLAYER_ORIGIN_Y = 60f

        /** Horizon Y for depth scaling (from suburban-crossroads.json). */
        private const val HORIZON_Y = 540f

        /** Ball radius in Box2D meters for the dynamic collider. */
        private const val BALL_RADIUS_METERS = 0.15f

        /**
         * Render layer for the ball shadow (ground plane, drawn behind the ball).
         * Higher layer value = processed first in the render sort = drawn behind.
         */
        private const val SHADOW_RENDER_LAYER = 1

        /**
         * Render layer for the ball itself (drawn in front of the shadow).
         * Lower layer value = processed later in the render sort = drawn in front.
         */
        private const val BALL_RENDER_LAYER = 0

        /** Minimum shadow opacity — shadow never fully disappears during flight. */
        private const val MIN_SHADOW_OPACITY = 0.1f

        private const val TAG = "InputSystem"
    }

    // --- Active ball and shadow entities (at most one ball in flight at a time) ---
    private var activeBallEntity: Entity? = null
    private var activeShadowEntity: Entity? = null

    /** True if the current ball was kicked as a Big Bomb. */
    var isBigBombActive: Boolean = false
        private set

    /**
     * When true, the next kick is forced into Big Bomb mode regardless of
     * power and slider thresholds. Set by external systems (e.g., HudSystem's
     * bomb mode button) via [LevelScreen] wiring each frame.
     *
     * InputSystem reads this on each flick and does NOT reset it — the caller
     * (HudSystem) is responsible for resetting after the kick launches.
     */
    var bombModeOverride: Boolean = false

    override fun update(deltaTime: Float) {
        // --- 1. Check for pending flick (ball spawn) ---
        val flickResult = inputRouter.consumeFlickResult()
        if (flickResult != null) {
            handleFlick(flickResult)
        }

        // --- 2. During BALL_IN_FLIGHT, apply steer deltas to ball spin ---
        if (stateManager.currentState is GameState.BallInFlight) {
            applySteerDeltas()
            updateShadow()
        }
    }

    /**
     * Process a valid flick: create ball entity, detect Big Bomb, create shadow.
     */
    private fun handleFlick(flickResult: FlickResult) {
        val engine = this.engine ?: return

        // Reset steer swipe counter for the new kick
        inputRouter.resetSteerSwipeCounter()

        // Check Big Bomb thresholds — bomb mode override forces activation regardless
        val meetsThresholds = flickResult.power >= TuningConstants.BIG_BOMB_POWER_THRESHOLD
                && flickResult.sliderValue >= TuningConstants.BIG_BOMB_SLIDER_THRESHOLD
        isBigBombActive = meetsThresholds || bombModeOverride

        // Play kick launch sound (bass boom — GDD Section 9)
        audioService.playKickLaunch()

        if (isBigBombActive) {
            val source = if (bombModeOverride && !meetsThresholds) "bomb mode button" else "thresholds"
            Gdx.app.log(TAG, "Big Bomb activated via $source! power=${flickResult.power}, slider=${flickResult.sliderValue}")
            audioService.playBigBombActivation()
        }

        // Compute initial velocity from FlickResult.
        // The launch angle slider splits kick energy between forward (vy) and upward (vz).
        // We use a softened forward ratio so that:
        //   - Low angles still have some upward arc (not 98% forward)
        //   - High angles still travel forward meaningfully (not 9% forward)
        val totalSpeed = flickResult.power * TuningConstants.MAX_KICK_SPEED
        val launchAngleRad = Math.toRadians(
            (TuningConstants.MIN_ANGLE + flickResult.sliderValue * (TuningConstants.MAX_ANGLE - TuningConstants.MIN_ANGLE)).toDouble()
        ).toFloat()

        val rawForwardRatio = cos(launchAngleRad)
        val forwardRatio = 0.40f + rawForwardRatio * 0.12f  // maps ~[0.09..0.98] to ~[0.41..0.52]
        val forwardSpeed = totalSpeed * forwardRatio
        // direction = atan2(dy, dx) where dy is the upward screen component.
        // A straight-up flick gives direction ≈ PI/2.
        // Standard trig: x-component = cos(θ), y-component = sin(θ).
        // vx = lateral (screen-x), vy = depth (into scene / screen-y).
        val vx = forwardSpeed * cos(flickResult.direction)
        val vy = forwardSpeed * sin(flickResult.direction)
        val vz = totalSpeed * sin(launchAngleRad)

        // --- Create ball entity ---
        val ballEntity = engine.createEntity()

        val transform = engine.createComponent(TransformComponent::class.java).apply {
            x = PLAYER_ORIGIN_X
            y = PLAYER_ORIGIN_Y
            height = 0f
            screenScale = 1f
        }
        ballEntity.add(transform)

        val velocity = engine.createComponent(VelocityComponent::class.java).apply {
            this.vx = vx
            this.vy = vy
            this.vz = vz
        }
        ballEntity.add(velocity)

        val spin = engine.createComponent(SpinComponent::class.java).apply {
            spinX = 0f
            spinY = 0f
        }
        ballEntity.add(spin)

        val visual = engine.createComponent(VisualComponent::class.java).apply {
            region = if (assetManager?.isLoaded("sprites/ball.png") == true) {
                TextureRegion(assetManager.get("sprites/ball.png", Texture::class.java))
            } else null
            renderLayer = BALL_RENDER_LAYER
            opacity = 1f
        }
        ballEntity.add(visual)

        val collider = engine.createComponent(ColliderComponent::class.java).apply {
            body = createBallBody(PLAYER_ORIGIN_X, PLAYER_ORIGIN_Y)
            isSensor = false
        }
        ballEntity.add(collider)

        engine.addEntity(ballEntity)
        activeBallEntity = ballEntity

        Gdx.app.log(TAG, "Ball spawned: vx=$vx, vy=$vy, vz=$vz, power=${flickResult.power}, " +
                "slider=${flickResult.sliderValue}, launchAngle=${Math.toDegrees(launchAngleRad.toDouble())}°")

        // --- Create ball shadow entity ---
        createShadowEntity(engine)
    }

    /**
     * Create a dynamic Box2D body for the ball at the given game-space position.
     */
    private fun createBallBody(gameX: Float, gameY: Float): com.badlogic.gdx.physics.box2d.Body {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(gameX / TuningConstants.PPM, gameY / TuningConstants.PPM)
        }
        val body = world.createBody(bodyDef)

        val shape = CircleShape().apply {
            radius = BALL_RADIUS_METERS
        }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            density = 1f
            friction = 0.3f
            restitution = 0.3f
        }
        body.createFixture(fixtureDef)
        shape.dispose()

        return body
    }

    /**
     * Create a shadow entity that tracks the ball's ground-plane position.
     *
     * The shadow has Transform (at ball x,y with height=0) and Visual (dark ellipse,
     * opacity fades with ball height). No Velocity, Spin, or Collider — it is purely visual.
     */
    private fun createShadowEntity(engine: Engine) {
        val shadowEntity = engine.createEntity()

        val shadowTransform = engine.createComponent(TransformComponent::class.java).apply {
            x = PLAYER_ORIGIN_X
            y = PLAYER_ORIGIN_Y
            height = 0f
            screenScale = 1f
        }
        shadowEntity.add(shadowTransform)

        val shadowVisual = engine.createComponent(VisualComponent::class.java).apply {
            // TextureRegion for shadow ellipse will be set externally.
            region = null
            renderLayer = SHADOW_RENDER_LAYER
            opacity = 1f
            tint.set(0f, 0f, 0f, 1f) // Dark shadow tint
        }
        shadowEntity.add(shadowVisual)

        engine.addEntity(shadowEntity)
        activeShadowEntity = shadowEntity
    }

    /**
     * Apply accumulated steer deltas from InputRouter to the active ball's SpinComponent.
     */
    private fun applySteerDeltas() {
        val ballEntity = activeBallEntity ?: return
        val steerDeltas = inputRouter.consumeSteerDeltas()

        if (steerDeltas.spinDeltaX != 0f || steerDeltas.spinDeltaY != 0f) {
            val spinComp = ballEntity.spin
            spinComp.spinX += steerDeltas.spinDeltaX
            spinComp.spinY += steerDeltas.spinDeltaY
        }
    }

    /**
     * Update the shadow entity to track the ball's ground-plane position.
     *
     * Shadow position = ball's (x, y) with height 0.
     * Shadow opacity fades with ball height per physics-and-tuning.md Section 5:
     *   shadowAlpha = max(0.1, 1.0 - ball.height / SHADOW_FADE_HEIGHT)
     * Shadow scale follows the same depth scaling as the ball.
     */
    private fun updateShadow() {
        val ballEntity = activeBallEntity ?: return
        val shadowEntity = activeShadowEntity ?: return

        val ballTransform = ballEntity.transform
        val shadowTransform = shadowEntity.transform
        val shadowVisual = shadowEntity.visual

        // Shadow position: ball's ground-plane projection
        shadowTransform.x = ballTransform.x
        shadowTransform.y = ballTransform.y
        shadowTransform.height = 0f

        // Shadow scale: same depth formula as ball
        shadowTransform.screenScale = 0.05f.coerceAtLeast((HORIZON_Y - ballTransform.y) / HORIZON_Y)

        // Shadow opacity: fades with ball height
        shadowVisual.opacity = MIN_SHADOW_OPACITY.coerceAtLeast(
            1f - ballTransform.height / TuningConstants.SHADOW_FADE_HEIGHT
        )
    }

    /**
     * Remove the active ball and shadow entities from the engine.
     *
     * Called when the ball flight ends (transition out of BALL_IN_FLIGHT).
     * Other systems (CollisionSystem, GameStateManager listeners) should call this
     * when appropriate.
     */
    fun removeActiveBall() {
        val engine = this.engine ?: return

        activeBallEntity?.let { ball ->
            // Destroy Box2D body before removing entity
            val colliderComp = ball.getComponent(ColliderComponent::class.java)
            colliderComp?.body?.let { body ->
                world.destroyBody(body)
            }
            engine.removeEntity(ball)
        }
        activeBallEntity = null

        activeShadowEntity?.let { shadow ->
            engine.removeEntity(shadow)
        }
        activeShadowEntity = null

        isBigBombActive = false
    }

    /**
     * Returns the active ball entity, or null if no ball is in flight.
     * Useful for other systems (CollisionSystem, HudSystem) that need to inspect ball state.
     */
    fun getActiveBall(): Entity? = activeBallEntity

    /**
     * Returns the active shadow entity, or null if no shadow exists.
     */
    fun getActiveShadow(): Entity? = activeShadowEntity
}
