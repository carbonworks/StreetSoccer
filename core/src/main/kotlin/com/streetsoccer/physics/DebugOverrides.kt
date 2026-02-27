package com.streetsoccer.physics

/**
 * Session-only overrides for [TuningConstants] values.
 *
 * When the debug panel is enabled, systems read constants through this singleton
 * using the pattern: `DebugOverrides.gravity ?: TuningConstants.GRAVITY`.
 * Override values are nullable — when null, the default constant is used.
 *
 * All values reset to null on app restart (never persisted to disk).
 * The [enabled] master toggle controls whether overrides are active.
 * When [enabled] is false, all property getters return null regardless
 * of stored values, so systems always fall back to [TuningConstants].
 */
object DebugOverrides {

    /**
     * Master toggle. When false, all overrides return null so the game
     * uses [TuningConstants] defaults without any per-property checks.
     */
    var enabled: Boolean = false

    // --- Backing fields for each tunable constant ---

    private var _gravity: Float? = null
    private var _maxKickSpeed: Float? = null
    private var _drag: Float? = null
    private var _magnusCoefficient: Float? = null
    private var _spinDecay: Float? = null
    private var _steerSensitivity: Float? = null
    private var _minFlickSpeed: Float? = null
    private var _maxFlickSpeed: Float? = null
    private var _minAngle: Float? = null
    private var _maxAngle: Float? = null
    private var _bigBombPowerThreshold: Float? = null
    private var _bigBombSliderThreshold: Float? = null
    private var _shadowFadeHeight: Float? = null
    private var _bigBombColorStartDepth: Float? = null
    private var _bigBombColorMaxDepth: Float? = null
    private var _horizonY: Float? = null
    private var _depthCollisionTolerance: Float? = null

    // --- Public properties that respect the master toggle ---

    var gravity: Float?
        get() = if (enabled) _gravity else null
        set(value) { _gravity = value }

    var maxKickSpeed: Float?
        get() = if (enabled) _maxKickSpeed else null
        set(value) { _maxKickSpeed = value }

    var drag: Float?
        get() = if (enabled) _drag else null
        set(value) { _drag = value }

    var magnusCoefficient: Float?
        get() = if (enabled) _magnusCoefficient else null
        set(value) { _magnusCoefficient = value }

    var spinDecay: Float?
        get() = if (enabled) _spinDecay else null
        set(value) { _spinDecay = value }

    var steerSensitivity: Float?
        get() = if (enabled) _steerSensitivity else null
        set(value) { _steerSensitivity = value }

    var minFlickSpeed: Float?
        get() = if (enabled) _minFlickSpeed else null
        set(value) { _minFlickSpeed = value }

    var maxFlickSpeed: Float?
        get() = if (enabled) _maxFlickSpeed else null
        set(value) { _maxFlickSpeed = value }

    var minAngle: Float?
        get() = if (enabled) _minAngle else null
        set(value) { _minAngle = value }

    var maxAngle: Float?
        get() = if (enabled) _maxAngle else null
        set(value) { _maxAngle = value }

    var bigBombPowerThreshold: Float?
        get() = if (enabled) _bigBombPowerThreshold else null
        set(value) { _bigBombPowerThreshold = value }

    var bigBombSliderThreshold: Float?
        get() = if (enabled) _bigBombSliderThreshold else null
        set(value) { _bigBombSliderThreshold = value }

    var shadowFadeHeight: Float?
        get() = if (enabled) _shadowFadeHeight else null
        set(value) { _shadowFadeHeight = value }

    var bigBombColorStartDepth: Float?
        get() = if (enabled) _bigBombColorStartDepth else null
        set(value) { _bigBombColorStartDepth = value }

    var bigBombColorMaxDepth: Float?
        get() = if (enabled) _bigBombColorMaxDepth else null
        set(value) { _bigBombColorMaxDepth = value }

    var horizonY: Float?
        get() = if (enabled) _horizonY else null
        set(value) { _horizonY = value }

    var depthCollisionTolerance: Float?
        get() = if (enabled) _depthCollisionTolerance else null
        set(value) { _depthCollisionTolerance = value }

    /**
     * Metadata for each tunable constant: display name, default value, and
     * min/max range for the slider UI. The order here defines the display
     * order in [com.streetsoccer.ui.DebugPanelOverlay].
     */
    data class ConstantMeta(
        val name: String,
        val defaultValue: Float,
        val min: Float,
        val max: Float,
        val getter: () -> Float?,
        val setter: (Float?) -> Unit
    )

    /** All tunable constants with their metadata, in display order. */
    val allConstants: List<ConstantMeta> = listOf(
        ConstantMeta("GRAVITY", TuningConstants.GRAVITY, 0f, 500f, { gravity }, { gravity = it }),
        ConstantMeta("MAX_KICK_SPEED", TuningConstants.MAX_KICK_SPEED, 100f, 1500f, { maxKickSpeed }, { maxKickSpeed = it }),
        ConstantMeta("DRAG", TuningConstants.DRAG, 0.5f, 1.0f, { drag }, { drag = it }),
        ConstantMeta("MAGNUS_COEFF", TuningConstants.MAGNUS_COEFFICIENT, 0f, 0.005f, { magnusCoefficient }, { magnusCoefficient = it }),
        ConstantMeta("SPIN_DECAY", TuningConstants.SPIN_DECAY, 0f, 10f, { spinDecay }, { spinDecay = it }),
        ConstantMeta("STEER_SENS", TuningConstants.STEER_SENSITIVITY, 0f, 0.05f, { steerSensitivity }, { steerSensitivity = it }),
        ConstantMeta("MIN_FLICK_SPD", TuningConstants.MIN_FLICK_SPEED, 50f, 1000f, { minFlickSpeed }, { minFlickSpeed = it }),
        ConstantMeta("MAX_FLICK_SPD", TuningConstants.MAX_FLICK_SPEED, 500f, 5000f, { maxFlickSpeed }, { maxFlickSpeed = it }),
        ConstantMeta("MIN_ANGLE", TuningConstants.MIN_ANGLE, 0f, 45f, { minAngle }, { minAngle = it }),
        ConstantMeta("MAX_ANGLE", TuningConstants.MAX_ANGLE, 45f, 90f, { maxAngle }, { maxAngle = it }),
        ConstantMeta("BB_PWR_THRESH", TuningConstants.BIG_BOMB_POWER_THRESHOLD, 0f, 1f, { bigBombPowerThreshold }, { bigBombPowerThreshold = it }),
        ConstantMeta("BB_SLD_THRESH", TuningConstants.BIG_BOMB_SLIDER_THRESHOLD, 0f, 1f, { bigBombSliderThreshold }, { bigBombSliderThreshold = it }),
        ConstantMeta("SHADOW_FADE_H", TuningConstants.SHADOW_FADE_HEIGHT, 50f, 1000f, { shadowFadeHeight }, { shadowFadeHeight = it }),
        ConstantMeta("BB_CLR_START", TuningConstants.BIG_BOMB_COLOR_START_DEPTH, 0f, 1f, { bigBombColorStartDepth }, { bigBombColorStartDepth = it }),
        ConstantMeta("BB_CLR_MAX", TuningConstants.BIG_BOMB_COLOR_MAX_DEPTH, 0f, 1f, { bigBombColorMaxDepth }, { bigBombColorMaxDepth = it }),
        ConstantMeta("HORIZON_Y", TuningConstants.HORIZON_Y, 100f, 900f, { horizonY }, { horizonY = it }),
        ConstantMeta("DEPTH_COL_TOL", TuningConstants.DEPTH_COLLISION_TOLERANCE, 5f, 200f, { depthCollisionTolerance }, { depthCollisionTolerance = it })
    )

    /**
     * Reset all overrides to null and disable the master toggle.
     * Called when the debug panel is dismissed or the master toggle is turned off.
     */
    fun resetAll() {
        enabled = false
        _gravity = null
        _maxKickSpeed = null
        _drag = null
        _magnusCoefficient = null
        _spinDecay = null
        _steerSensitivity = null
        _minFlickSpeed = null
        _maxFlickSpeed = null
        _minAngle = null
        _maxAngle = null
        _bigBombPowerThreshold = null
        _bigBombSliderThreshold = null
        _shadowFadeHeight = null
        _bigBombColorStartDepth = null
        _bigBombColorMaxDepth = null
        _horizonY = null
        _depthCollisionTolerance = null
    }
}
