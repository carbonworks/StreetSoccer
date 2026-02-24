package com.streetsoccer.input

import kotlin.math.sqrt

// Track cumulative spins per kick
data class SteerResult(
    var spinDeltaX: Float = 0f,
    var spinDeltaY: Float = 0f
)

class SteerDetector {

    private var lastX = 0f
    private var lastY = 0f
    private var lastTime = 0L
    var swipeCount = 0
        private set

    // Reset this counter on each new kick (when entering BALL_IN_FLIGHT)
    fun resetSwipeCounter() {
        swipeCount = 0
    }

    fun touchDown(screenX: Float, screenY: Float, time: Long) {
        lastX = screenX
        lastY = screenY
        lastTime = time
        swipeCount++
    }

    fun touchDragged(screenX: Float, screenY: Float, time: Long) : SteerResult {
        val dt = (time - lastTime) / 1000f
        if (dt <= 0f) return SteerResult()

        val dx = screenX - lastX
        val dy = -(screenY - lastY) // Y inverted
        val magnitude = sqrt(dx*dx + dy*dy)

        if (magnitude < 1f) return SteerResult() // minor jitter filter

        val speed = magnitude / dt
        val latComp = dx / magnitude
        val depComp = dy / magnitude

        val index = (swipeCount - 1).coerceAtLeast(0).coerceAtMost(3)
        val diminish = com.streetsoccer.physics.TuningConstants.STEER_DIMINISH_CURVE[index]

        val deltaX = latComp * speed * com.streetsoccer.physics.TuningConstants.STEER_SENSITIVITY * diminish
        val deltaY = depComp * speed * com.streetsoccer.physics.TuningConstants.STEER_SENSITIVITY * diminish

        lastX = screenX
        lastY = screenY
        lastTime = time

        return SteerResult(deltaX, deltaY)
    }

    fun touchUp() {
        // end of current continuous swipe gesture
    }
}
