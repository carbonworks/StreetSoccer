package com.streetsoccer.physics

class PhysicsAccumulator {
    var accumulator = 0f

    fun accumulate(deltaTime: Float) {
        accumulator += deltaTime
    }

    fun update(stepSize: Float, updateLogic: () -> Unit) {
        while (accumulator >= stepSize) {
            updateLogic()
            accumulator -= stepSize
        }
    }
}
