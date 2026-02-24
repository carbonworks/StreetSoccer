package com.streetsoccer.physics

object TuningConstants {
    const val PPM = 100f // Pixels Per Meter (Box2D conversion)
    const val GRAVITY = 98f // px/s^2 — tuned for tall, visible arcs
    const val MAX_KICK_SPEED = 700f // px/s
    const val DRAG = 0.8f // /s — moderate deceleration
    const val MAGNUS_COEFFICIENT = 0.0003f
    const val SPIN_DECAY = 2.0f // /s
    const val STEER_SENSITIVITY = 0.005f
    const val MIN_FLICK_SPEED = 300f // px/s
    const val MAX_FLICK_SPEED = 2000f // px/s
    const val MIN_ANGLE = 10f // degrees
    const val MAX_ANGLE = 85f // degrees
    const val BIG_BOMB_POWER_THRESHOLD = 0.9f
    const val BIG_BOMB_SLIDER_THRESHOLD = 0.7f
    const val FIXED_TIMESTEP = 1f / 60f // s
    
    val STEER_DIMINISH_CURVE = floatArrayOf(1.0f, 0.6f, 0.25f, 0.1f)
    
    const val SHADOW_FADE_HEIGHT = 400f // px
    const val BIG_BOMB_COLOR_START_DEPTH = 0.25f
    const val BIG_BOMB_COLOR_MAX_DEPTH = 0.90f
}
