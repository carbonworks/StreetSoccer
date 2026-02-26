package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.streetsoccer.input.InputRouter
import com.streetsoccer.physics.TuningConstants
import com.streetsoccer.state.GameState
import com.streetsoccer.state.GameStateManager
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders a dotted trajectory preview arc during the AIMING state.
 *
 * This system does NOT render during engine.update(). Instead it exposes a
 * [renderTrajectory] method that LevelScreen calls AFTER engine.update()
 * completes and the SpriteBatch is no longer active. This prevents the
 * ShapeRenderer/SpriteBatch conflict described in issue #26.
 *
 * The trajectory is computed by forward-simulating the ball flight equations
 * from physics-and-tuning.md (gravity, drag, no spin) using the current
 * slider angle and a fixed preview power.
 */
class TrajectorySystem(
    private val gameStateManager: GameStateManager,
    private val inputRouter: InputRouter
) : EntitySystem(), Disposable {

    companion object {
        /** Number of simulation steps to compute for the preview arc. */
        private const val PREVIEW_STEPS = 60

        /** Time step per preview simulation tick (seconds). */
        private const val PREVIEW_DT = 1f / 60f

        /** Assumed power level for the preview arc (mid-range kick). */
        private const val PREVIEW_POWER = 0.5f

        /** Radius of each dot in the trajectory preview (pixels). */
        private const val DOT_RADIUS = 4f

        /** Number of dots to skip between drawn dots for a dotted-line effect. */
        private const val DOT_SKIP = 3

        /** Player origin X (bottom center of 1920x1080). */
        private const val PLAYER_ORIGIN_X = 960f

        /** Player origin Y (bottom of screen). */
        private const val PLAYER_ORIGIN_Y = 0f

        /** Preview arc color (semi-transparent white). */
        private val ARC_COLOR = Color(1f, 1f, 1f, 0.4f)
    }

    /** Whether the trajectory preview is enabled. Set from SettingsData on screen creation. */
    var trajectoryPreviewEnabled: Boolean = false

    private val shapeRenderer = ShapeRenderer()

    /**
     * Intentionally empty: trajectory rendering is handled by [renderTrajectory],
     * called explicitly by LevelScreen outside the engine.update() cycle.
     * This avoids ShapeRenderer/SpriteBatch conflicts (issue #26).
     */
    override fun update(deltaTime: Float) {
        // No-op: rendering is deferred to renderTrajectory()
    }

    /**
     * Render the trajectory preview arc. Must be called when no SpriteBatch
     * is active (i.e., after batch.end() or before batch.begin()).
     *
     * Only draws when enabled and during AIMING/READY states.
     * In all other states this is a no-op.
     *
     * @param camera the camera whose projection matrix to use for rendering
     */
    fun renderTrajectory(camera: Camera) {
        if (!trajectoryPreviewEnabled) return
        val state = gameStateManager.currentState
        if (state !is GameState.Aiming && state !is GameState.Ready) return

        val sliderVal = inputRouter.sliderValue
        val launchAngleDeg = TuningConstants.MIN_ANGLE +
            sliderVal * (TuningConstants.MAX_ANGLE - TuningConstants.MIN_ANGLE)
        val launchAngleRad = Math.toRadians(launchAngleDeg.toDouble()).toFloat()

        // Compute initial velocity from preview power
        val horizontalSpeed = PREVIEW_POWER * TuningConstants.MAX_KICK_SPEED
        // Preview fires straight ahead (direction = 0 -> no lateral component)
        val vx0 = 0f
        val vy0 = horizontalSpeed * cos(0f) // straight into the scene
        val vz0 = horizontalSpeed * sin(launchAngleRad)

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = ARC_COLOR

        // Forward-simulate the arc
        var px = PLAYER_ORIGIN_X
        var py = PLAYER_ORIGIN_Y
        var pz = 0f
        var cvx = vx0
        var cvy = vy0
        var cvz = vz0

        for (step in 0 until PREVIEW_STEPS) {
            // Apply physics (matching PhysicsSystem equations, no spin)
            cvz -= TuningConstants.GRAVITY * PREVIEW_DT
            cvx *= (1f - TuningConstants.DRAG * PREVIEW_DT)
            cvy *= (1f - TuningConstants.DRAG * PREVIEW_DT)
            cvz *= (1f - TuningConstants.DRAG * PREVIEW_DT)

            px += cvx * PREVIEW_DT
            py += cvy * PREVIEW_DT
            pz += cvz * PREVIEW_DT

            // Stop if the ball hits the ground
            if (pz < 0f) break

            // Draw every DOT_SKIP-th dot for a dotted appearance
            if (step % DOT_SKIP == 0) {
                // Screen position: x stays as px, y offset by height for visual arc
                shapeRenderer.circle(px, py + pz, DOT_RADIUS)
            }
        }

        shapeRenderer.end()
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
