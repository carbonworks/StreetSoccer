package com.streetsoccer.input

import kotlin.math.atan2
import kotlin.math.sqrt

data class FlickResult(
    val power: Float,
    val direction: Float,
    val sliderValue: Float
)

class FlickDetector {
    
    private var startX = 0f
    private var startY = 0f
    private var startTime = 0L

    fun touchDown(screenX: Float, screenY: Float, time: Long) {
        startX = screenX
        startY = screenY
        startTime = time
    }

    fun touchUp(screenX: Float, screenY: Float, time: Long, currentSliderValue: Float): FlickResult? {
        val dt = (time - startTime) / 1000f // to seconds
        if(dt <= 0f) return null

        val dx = screenX - startX
        val dy = -(screenY - startY) // Invert Y because screen Y is down

        val length = sqrt(dx*dx + dy*dy)
        val speed = length / dt // pixels per second

        if (speed < com.streetsoccer.physics.TuningConstants.MIN_FLICK_SPEED) {
            return null
        }

        val power = (speed / com.streetsoccer.physics.TuningConstants.MAX_FLICK_SPEED).coerceIn(0f, 1f)
        val direction = atan2(dy, dx)

        return FlickResult(power, direction, currentSliderValue)
    }
}
