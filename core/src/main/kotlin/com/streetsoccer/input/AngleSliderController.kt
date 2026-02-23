package com.streetsoccer.input

import com.streetsoccer.physics.TuningConstants

class AngleSliderController {
    
    var sliderValue: Float = 0.5f // 0.0 to 1.0
        private set

    val launchAngle: Float
        get() = TuningConstants.MIN_ANGLE + sliderValue * (TuningConstants.MAX_ANGLE - TuningConstants.MIN_ANGLE)
        
    fun updateValue(screenY: Float, screenHeight: Float) {
        // Simple mapping: top of screen = 1.0, bottom = 0.0
        val rawValue = (screenHeight - screenY) / screenHeight
        sliderValue = rawValue.coerceIn(0f, 1f)
    }
}
