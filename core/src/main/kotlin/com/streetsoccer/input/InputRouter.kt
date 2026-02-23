package com.streetsoccer.input

import com.badlogic.gdx.InputAdapter
import com.streetsoccer.state.GameState
import com.streetsoccer.state.GameStateManager

class InputRouter(
    private val stateManager: GameStateManager
) : InputAdapter() {

    private val flickDetector = FlickDetector()
    private val steerDetector = SteerDetector()
    private val angleSliderController = AngleSliderController()

    // Strict pointer-ID tracking
    private var sliderPointerId: Int = -1
    private var flickPointerId: Int = -1

    // Zone constant
    private val SLIDER_RAIL_WIDTH = 80f

    // --- Result buffers polled by InputSystem each frame ---

    /**
     * The most recent valid flick result. Set on touchUp when a valid flick is detected.
     * InputSystem reads and clears this once per frame to spawn a ball entity.
     */
    var pendingFlickResult: FlickResult? = null
        private set

    /**
     * Accumulated steer spin deltas since the last InputSystem poll.
     * InputSystem reads and resets these each frame during BALL_IN_FLIGHT.
     */
    var pendingSteerDeltaX: Float = 0f
        private set
    var pendingSteerDeltaY: Float = 0f
        private set

    /** Called by InputSystem after reading the pending flick result. */
    fun consumeFlickResult(): FlickResult? {
        val result = pendingFlickResult
        pendingFlickResult = null
        return result
    }

    /** Called by InputSystem after reading accumulated steer deltas. Returns the deltas and resets them to zero. */
    fun consumeSteerDeltas(): SteerResult {
        val result = SteerResult(pendingSteerDeltaX, pendingSteerDeltaY)
        pendingSteerDeltaX = 0f
        pendingSteerDeltaY = 0f
        return result
    }

    /** Reset the steer detector's swipe counter. Called by InputSystem at the start of each new kick. */
    fun resetSteerSwipeCounter() {
        steerDetector.resetSwipeCounter()
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val state = stateManager.currentState

        // 1. BALL_IN_FLIGHT -> Steer Zone (Full Screen)
        if (state is GameState.BallInFlight) {
            steerDetector.touchDown(screenX.toFloat(), screenY.toFloat(), System.currentTimeMillis())
            return true
        }

        // 2. READY or AIMING -> Slider Rail vs Play Area
        if (state is GameState.Ready || state is GameState.Aiming) {
            if (screenX <= SLIDER_RAIL_WIDTH) { // Assuming left-aligned slider for now
                if (sliderPointerId == -1) {
                    sliderPointerId = pointer
                    angleSliderController.updateValue(screenY.toFloat(), com.badlogic.gdx.Gdx.graphics.height.toFloat())
                    return true
                }
            } else {
                if (flickPointerId == -1) {
                    flickPointerId = pointer
                    flickDetector.touchDown(screenX.toFloat(), screenY.toFloat(), System.currentTimeMillis())

                    if (state is GameState.Ready) {
                         stateManager.transitionTo(GameState.Aiming)
                    }
                    return true
                }
            }
        }

        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
       val state = stateManager.currentState

        if (state is GameState.BallInFlight) {
            val steerResult = steerDetector.touchDragged(screenX.toFloat(), screenY.toFloat(), System.currentTimeMillis())
            // Accumulate steer deltas for InputSystem to consume
            pendingSteerDeltaX += steerResult.spinDeltaX
            pendingSteerDeltaY += steerResult.spinDeltaY
            return true
        }

        if (state is GameState.Ready || state is GameState.Aiming) {
            if (pointer == sliderPointerId) {
                angleSliderController.updateValue(screenY.toFloat(), com.badlogic.gdx.Gdx.graphics.height.toFloat())
                return true
            }
            if (pointer == flickPointerId) {
                // Tracking visual drag for preview arc (no-op in input logic right now)
                return true
            }
        }

        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val state = stateManager.currentState

        if (state is GameState.BallInFlight) {
            steerDetector.touchUp()
            return true
        }

        if (state is GameState.Ready || state is GameState.Aiming) {
            if (pointer == sliderPointerId) {
                sliderPointerId = -1
                return true
            }

            if (pointer == flickPointerId) {
                flickPointerId = -1
                val result = flickDetector.touchUp(screenX.toFloat(), screenY.toFloat(), System.currentTimeMillis(), angleSliderController.sliderValue)

                if (result != null) {
                    // Store the flick result for InputSystem to consume
                    pendingFlickResult = result
                    stateManager.transitionTo(GameState.BallInFlight)
                } else {
                    // Invalid/too slow flick -> revert to Ready
                    stateManager.transitionTo(GameState.Ready)
                }
                return true
            }
        }

        return false
    }
}
