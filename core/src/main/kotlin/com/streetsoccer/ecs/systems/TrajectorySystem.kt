package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.streetsoccer.input.InputRouter
import com.streetsoccer.physics.TuningConstants
import com.streetsoccer.state.GameState
import com.streetsoccer.state.GameStateManager
import kotlin.math.sin

/**
 * Renders a toggleable dotted arc showing the predicted ball trajectory during AIMING.
 *
 * The preview uses the same physics equations as the real ball flight from
 * physics-and-tuning.md Section 2: gravity, drag, and launch angle from the
 * angle slider. Spin and steer are not included since no spin exists before launch.
 *
 * Visual style: evenly spaced dots along the predicted path with fading opacity
 * toward the end of the arc. The arc updates in real time as the angle slider moves.
 *
 * Respects [trajectoryPreviewEnabled] -- if false, no rendering occurs.
 *
 * @param inputRouter provides the current slider value for launch angle computation
 * @param stateManager determines whether the system should render (AIMING/READY states)
 * @param projectionMatrix the camera/viewport projection matrix for correct screen mapping
 *
 * @see com.streetsoccer.physics.TuningConstants
 * @see com.streetsoccer.input.InputRouter
 */
class TrajectorySystem(
    private val inputRouter: InputRouter,
    private val stateManager: GameStateManager,
    private val projectionMatrix: () -> Matrix4
) : EntitySystem(), Disposable {

    companion object {
        /** Player origin X -- bottom center of a 1920x1080 screen. */
        private const val PLAYER_ORIGIN_X = 960f

        /** Player origin Y -- bottom of screen. */
        private const val PLAYER_ORIGIN_Y = 0f

        /** Horizon Y for depth scaling (from suburban-crossroads.json). */
        private const val HORIZON_Y = 540f

        /**
         * Default assumed power for the trajectory preview.
         * A moderate power gives a representative arc without implying max or min kick.
         * The player sees roughly what a "normal" flick would produce at the current angle.
         */
        private const val PREVIEW_POWER = 0.6f

        /** Number of simulation steps to compute for the preview arc. */
        private const val SIMULATION_STEPS = 120

        /** Time step for each simulation step (matches physics fixed timestep). */
        private const val SIM_DT = TuningConstants.FIXED_TIMESTEP

        /** Number of dots to render along the arc. */
        private const val DOT_COUNT = 24

        /** Base radius of each dot in pixels (before depth scaling). */
        private const val DOT_BASE_RADIUS = 4f

        /** Minimum dot radius after depth scaling. */
        private const val DOT_MIN_RADIUS = 1f

        /** Starting opacity for the first dot in the arc. */
        private const val DOT_START_OPACITY = 0.7f

        /** Ending opacity for the last dot in the arc. */
        private const val DOT_END_OPACITY = 0.05f

        private const val TAG = "TrajectorySystem"
    }

    /** Whether the trajectory preview is enabled. Set from SettingsData on screen creation. */
    var trajectoryPreviewEnabled: Boolean = false

    private val shapeRenderer = ShapeRenderer()

    /**
     * Reusable buffer for simulated trajectory points.
     * Each point stores (x, y, height) in game space.
     */
    private data class TrajectoryPoint(var x: Float, var y: Float, var height: Float)

    private val trajectoryPoints = Array(SIMULATION_STEPS) { TrajectoryPoint(0f, 0f, 0f) }

    override fun update(deltaTime: Float) {
        if (!trajectoryPreviewEnabled) return

        val state = stateManager.currentState
        if (state !is GameState.Aiming && state !is GameState.Ready) return

        // Compute trajectory points from current slider value
        val pointCount = simulateTrajectory(inputRouter.sliderValue)
        if (pointCount < 2) return

        // Render the dotted arc
        renderDottedArc(pointCount)
    }

    /**
     * Simulate the ball trajectory using the same physics as PhysicsSystem.
     *
     * Uses the current slider value for launch angle and [PREVIEW_POWER] for kick power.
     * Direction is straight ahead (0 radians) since we cannot know the flick direction yet.
     * No spin or Magnus effect is applied (preview shows un-steered path).
     *
     * @param sliderValue current angle slider value (0.0 - 1.0)
     * @return the number of valid trajectory points computed (may be less than SIMULATION_STEPS
     *         if the ball hits the ground or goes off-screen)
     */
    private fun simulateTrajectory(sliderValue: Float): Int {
        // Compute initial velocity per physics-and-tuning.md Section 2:
        //   horizontalSpeed = power * MAX_KICK_SPEED
        //   vx = horizontalSpeed * sin(direction)  -- lateral (0 for straight ahead)
        //   vy = horizontalSpeed * cos(direction)   -- depth (into scene)
        //   vz = horizontalSpeed * sin(launchAngle) -- vertical (upward arc)
        val launchAngleDeg = TuningConstants.MIN_ANGLE +
                sliderValue * (TuningConstants.MAX_ANGLE - TuningConstants.MIN_ANGLE)
        val launchAngleRad = Math.toRadians(launchAngleDeg.toDouble()).toFloat()

        val horizontalSpeed = PREVIEW_POWER * TuningConstants.MAX_KICK_SPEED

        // Direction = 0 (straight ahead): sin(0) = 0, cos(0) = 1
        // Per InputSystem.handleFlick() and physics-and-tuning.md Section 2:
        //   vx = horizontalSpeed * sin(direction)   -> 0 (straight ahead)
        //   vy = horizontalSpeed * cos(direction)   -> horizontalSpeed (full forward)
        //   vz = horizontalSpeed * sin(launchAngle) -> vertical component from slider
        var vx = 0f
        var vy = horizontalSpeed   // cos(0) = 1, full forward speed
        var vz = horizontalSpeed * sin(launchAngleRad)

        var posX = PLAYER_ORIGIN_X
        var posY = PLAYER_ORIGIN_Y
        var posHeight = 0f

        var pointCount = 0

        for (step in 0 until SIMULATION_STEPS) {
            // Store current point
            trajectoryPoints[step].x = posX
            trajectoryPoints[step].y = posY
            trajectoryPoints[step].height = posHeight
            pointCount++

            // Per-frame physics update (same as PhysicsSystem and physics-and-tuning.md Section 2)

            // Gravity (vertical axis only)
            vz -= TuningConstants.GRAVITY * SIM_DT

            // Air resistance (drag on all components)
            val dragFactor = 1f - TuningConstants.DRAG * SIM_DT
            vx *= dragFactor
            vy *= dragFactor
            vz *= dragFactor

            // No Magnus effect in preview (no spin before launch)

            // Position update
            posX += vx * SIM_DT
            posY += vy * SIM_DT
            posHeight += vz * SIM_DT

            // Stop if ball hits ground after going up (completed the arc)
            if (posHeight <= 0f && step > 0) {
                // Add the landing point
                posHeight = 0f
                if (step + 1 < SIMULATION_STEPS) {
                    trajectoryPoints[step + 1].x = posX
                    trajectoryPoints[step + 1].y = posY
                    trajectoryPoints[step + 1].height = 0f
                    pointCount++
                }
                break
            }

            // Stop if ball goes past the horizon (too deep)
            if (posY >= HORIZON_Y) break

            // Stop if ball goes off-screen horizontally
            if (posX < 0f || posX > 1920f) break
        }

        return pointCount
    }

    /**
     * Render the trajectory as evenly spaced dots with fading opacity.
     *
     * Dots are placed at evenly distributed intervals along the simulated path.
     * Each dot's size is scaled by the depth scaling formula, and opacity fades
     * linearly from [DOT_START_OPACITY] to [DOT_END_OPACITY] along the arc.
     *
     * @param pointCount the number of valid points in [trajectoryPoints]
     */
    private fun renderDottedArc(pointCount: Int) {
        // Determine which simulation points to sample for dot placement.
        // We want DOT_COUNT dots evenly distributed across the arc.
        val dotInterval = (pointCount - 1).toFloat() / (DOT_COUNT - 1).coerceAtLeast(1)

        // Enable blending for transparent dots
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.projectionMatrix = projectionMatrix()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        val actualDotCount = DOT_COUNT.coerceAtMost(pointCount)

        for (i in 0 until actualDotCount) {
            // Sample position from the simulation buffer
            val sampleIndex = (i * dotInterval).toInt().coerceIn(0, pointCount - 1)
            val point = trajectoryPoints[sampleIndex]

            // Compute screen position:
            // X is direct, Y is game-space Y + height (ball is elevated above ground plane)
            val screenX = point.x
            val screenY = point.y + point.height

            // Depth scaling (same formula as the ball/shadow)
            val depthScale = 0.05f.coerceAtLeast((HORIZON_Y - point.y) / HORIZON_Y)

            // Dot radius scaled by depth
            val dotRadius = (DOT_BASE_RADIUS * depthScale).coerceAtLeast(DOT_MIN_RADIUS)

            // Opacity fades linearly along the arc
            val t = i.toFloat() / (actualDotCount - 1).coerceAtLeast(1)
            val opacity = DOT_START_OPACITY + t * (DOT_END_OPACITY - DOT_START_OPACITY)

            // White dots with fading alpha
            shapeRenderer.setColor(1f, 1f, 1f, opacity)
            shapeRenderer.circle(screenX, screenY, dotRadius, 8)
        }

        shapeRenderer.end()
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
