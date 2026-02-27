package com.streetsoccer.screens

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.FitViewport
import com.streetsoccer.ecs.collider
import com.streetsoccer.ecs.components.ColliderComponent
import com.streetsoccer.ecs.components.TransformComponent
import com.streetsoccer.ecs.systems.CollisionSystem
import com.streetsoccer.ecs.systems.HudSystem
import com.streetsoccer.ecs.systems.InputSystem
import com.streetsoccer.ecs.systems.PhysicsSystem
import com.streetsoccer.ecs.systems.TrajectorySystem
import com.streetsoccer.ecs.transform
import com.streetsoccer.input.InputRouter
import com.streetsoccer.physics.TuningConstants
import com.streetsoccer.rendering.BackgroundRenderer
import com.streetsoccer.state.GameState
import com.streetsoccer.state.GameStateManager
import com.streetsoccer.ui.DebugPanelOverlay
import com.streetsoccer.ui.PauseOverlay

/**
 * Owns the per-frame render/update cycle per technical-architecture.md Section 7.
 *
 * Responsibilities:
 * - Clear screen and render background
 * - Fixed-timestep physics loop (PhysicsSystem + Box2D step + CollisionSystem)
 * - Engine.update() for all other ECS systems
 * - Trajectory preview rendering (after SpriteBatch is inactive)
 * - State machine update (timed-state auto-transitions)
 * - Box2D position sync from game-space
 * - HUD wiring (slider value, steer count, bomb mode)
 * - Pause overlay rendering
 *
 * All systems and managers are received as constructor params. This class
 * does not create or dispose any resources.
 */
class GameLoop(
    private val engine: Engine,
    private val world: World,
    private val batch: SpriteBatch,
    private val viewport: FitViewport,
    private val backgroundRenderer: BackgroundRenderer,
    private val gameStateManager: GameStateManager,
    private val inputRouter: InputRouter,
    private val physicsSystem: PhysicsSystem,
    private val collisionSystem: CollisionSystem,
    private val hudSystem: HudSystem,
    private val inputSystem: InputSystem,
    private val trajectorySystem: TrajectorySystem,
    private val pauseOverlay: PauseOverlay,
    private val debugPanelOverlay: DebugPanelOverlay?
) {

    /** Fixed-timestep accumulator for physics + Box2D stepping. */
    private var accumulator = 0f

    companion object {
        /**
         * Maximum number of fixed-timestep iterations per frame.
         * Caps the accumulator to prevent a spiral-of-death on frame hitches
         * (issue #31). If a frame takes longer than 5 physics ticks, excess
         * time is discarded rather than queued.
         */
        private const val MAX_STEPS_PER_FRAME = 5

        /** Box2D velocity iterations per step. */
        private const val BOX2D_VELOCITY_ITERATIONS = 6

        /** Box2D position iterations per step. */
        private const val BOX2D_POSITION_ITERATIONS = 2
    }

    /**
     * Execute one frame of the game loop.
     *
     * @param delta Frame time in seconds from LibGDX
     */
    fun update(delta: Float) {
        // Clear the screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Draw background layers before any ECS entities
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined
        backgroundRenderer.render(batch)

        // --- Game Loop (per technical-architecture.md Section 7) ---

        // 1. Poll input -- InputRouter receives events via LibGDX InputProcessor
        //    callbacks. No explicit polling needed.

        // 2. Accumulate time for fixed-step physics coordination
        accumulator += delta

        // 3. Fixed-step physics + Box2D + collision (only during BALL_IN_FLIGHT)
        if (gameStateManager.currentState is GameState.BallInFlight) {
            // Spiral-of-death protection (issue #31)
            accumulator = minOf(accumulator, TuningConstants.FIXED_TIMESTEP * MAX_STEPS_PER_FRAME)

            while (accumulator >= TuningConstants.FIXED_TIMESTEP) {
                physicsSystem.update(TuningConstants.FIXED_TIMESTEP)
                world.step(
                    TuningConstants.FIXED_TIMESTEP,
                    BOX2D_VELOCITY_ITERATIONS,
                    BOX2D_POSITION_ITERATIONS
                )
                collisionSystem.update(TuningConstants.FIXED_TIMESTEP)
                accumulator -= TuningConstants.FIXED_TIMESTEP
            }
        } else {
            // Drain accumulator when not in flight to prevent stale time buildup
            accumulator = 0f
        }

        // Run all ECS systems except PhysicsSystem (disabled via setProcessing)
        engine.update(delta)

        // Render trajectory preview AFTER engine.update() so the SpriteBatch
        // is no longer active, preventing ShapeRenderer/SpriteBatch conflict (issue #26)
        trajectorySystem.renderTrajectory(viewport.camera)

        // 4. Update state machine (timers for SCORING/IMPACT_MISSED transitions)
        gameStateManager.update(delta)

        // 5. Sync Box2D positions from game-space (game-space is authoritative)
        syncBox2DPositions()

        // 6. Wire HUD inputs from InputRouter
        hudSystem.sliderValue = inputRouter.sliderValue
        hudSystem.steerSwipeCount = inputRouter.steerSwipeCount

        // 7. Wire bomb mode: HudSystem button state -> InputSystem override flag
        inputSystem.bombModeOverride = hudSystem.isBombModeActive

        // 8. Render pause overlay on top of everything (if visible)
        pauseOverlay.render(delta)

        // 9. Render debug panel overlay on top of pause overlay (if visible)
        debugPanelOverlay?.render(delta)
    }

    /**
     * Sync Box2D body positions from game-space transform data.
     * Game-space is authoritative; Box2D is used only for collision detection.
     */
    private fun syncBox2DPositions() {
        val family = Family.all(TransformComponent::class.java, ColliderComponent::class.java).get()
        val entities = engine.getEntitiesFor(family)
        for (i in 0 until entities.size()) {
            val entity = entities[i]
            val tc = entity.transform ?: continue
            val cc = entity.collider ?: continue
            val body = cc.body ?: continue
            body.setTransform(
                tc.x / TuningConstants.PPM,
                tc.y / TuningConstants.PPM,
                body.angle
            )
        }
    }
}
