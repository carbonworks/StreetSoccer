package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.streetsoccer.services.SessionAccumulator
import com.streetsoccer.state.GameState
import com.streetsoccer.state.GameStateListener
import com.streetsoccer.state.GameStateManager

/**
 * Renders all in-game HUD elements using a ktx-scene2d Stage.
 *
 * HUD elements (per ui-hud-layout.md):
 * - Session score (top-center) with scale pulse on increment
 * - Streak multiplier badge (top-right) with color tiers
 * - Steer budget meter (right edge, 4 segments) visible only during BALL_IN_FLIGHT
 * - Pause icon (top-left) tappable to pause
 * - Angle slider visual (left rail) visible during READY/AIMING
 * - Score popups at impact locations, floating upward while fading
 *
 * @see com.streetsoccer.state.GameStateManager
 * @see com.streetsoccer.services.SessionAccumulator
 */
class HudSystem(
    private val gameStateManager: GameStateManager,
    private val sessionAccumulator: SessionAccumulator
) : EntitySystem(), GameStateListener {

    companion object {
        // Reference resolution (from ui-hud-layout.md Section 1)
        private const val WORLD_WIDTH = 1920f
        private const val WORLD_HEIGHT = 1080f

        // Margins (from ui-hud-layout.md)
        private const val EDGE_MARGIN = 32f

        // Pause icon size (from ui-hud-layout.md Section 7)
        private const val PAUSE_ICON_SIZE = 64f

        // Steer meter dimensions (from ui-hud-layout.md Section 4)
        private const val STEER_METER_WIDTH = 40f
        private const val STEER_METER_SEGMENT_HEIGHT = 80f
        private const val STEER_METER_GAP = 4f

        // Angle slider dimensions (from ui-hud-layout.md Section 5)
        private const val SLIDER_RAIL_WIDTH = 20f
        private const val SLIDER_RAIL_HEIGHT = 600f
        private const val SLIDER_THUMB_SIZE = 40f
        private const val SLIDER_TOUCH_WIDTH = 80f

        // Bomb button dimensions and positioning
        private const val BOMB_BUTTON_WIDTH = 140f
        private const val BOMB_BUTTON_HEIGHT = 64f
        private const val BOMB_BUTTON_MARGIN_BOTTOM = 120f
        private const val BOMB_BUTTON_MARGIN_RIGHT = 80f

        // Bomb button colors
        private val BOMB_COLOR_OFF = Color(0.7f, 0.15f, 0.15f, 0.85f)
        private val BOMB_COLOR_ON = Color(1f, 0.2f, 0.1f, 1f)
        private val BOMB_BORDER_COLOR_ON = Color(1f, 0.85f, 0.2f, 1f)

        // Bomb button pulse animation
        private const val BOMB_PULSE_MIN_SCALE = 1.0f
        private const val BOMB_PULSE_MAX_SCALE = 1.08f
        private const val BOMB_PULSE_PERIOD = 0.6f

        // Score popup animation (from ui-hud-layout.md Section 6)
        private const val POPUP_FLOAT_DISTANCE = 80f
        private const val POPUP_DURATION = 1.0f

        // Score pulse animation (from ui-hud-layout.md Section 2)
        private const val SCORE_PULSE_SCALE = 1.2f
        private const val SCORE_PULSE_DURATION = 0.2f

        // Streak multiplier tiers (from GDD Section 5)
        private val STREAK_MULTIPLIERS = floatArrayOf(1.0f, 1.5f, 2.0f, 2.5f, 3.0f)

        // Streak badge colors (from ui-hud-layout.md Section 3)
        private val STREAK_COLORS = arrayOf(
            Color(0.5f, 0.5f, 0.5f, 1f),   // x1 = gray/dim
            Color(1f, 1f, 1f, 1f),           // x1.5 = white
            Color(1f, 1f, 0f, 1f),           // x2 = yellow
            Color(1f, 0.6f, 0f, 1f),         // x2.5 = orange
            Color(1f, 0.85f, 0f, 1f)         // x3 = red/gold
        )

        // Steer meter segment colors (from ui-hud-layout.md Section 4)
        private val STEER_SEGMENT_COLORS = arrayOf(
            Color(0.2f, 0.8f, 0.2f, 1f),    // Segment 1 (x1.0) = bright green
            Color(1f, 1f, 0f, 1f),            // Segment 2 (x0.6) = yellow
            Color(1f, 0.6f, 0f, 1f),          // Segment 3 (x0.25) = orange
            Color(0.5f, 0.2f, 0.2f, 0.6f)    // Segment 4 (x0.1) = dim red/gray
        )
    }

    // --- Stage and viewport ---
    private lateinit var stage: Stage
    private lateinit var viewport: FitViewport

    // --- Managed resources (created at init, disposed at end) ---
    private val managedTextures = mutableListOf<Texture>()
    private val managedFonts = mutableListOf<BitmapFont>()

    // --- Shared font for score popups (avoids per-popup allocation) ---
    private lateinit var popupFont: BitmapFont

    // --- HUD actors ---
    private lateinit var scoreLabel: Label
    private lateinit var streakLabel: Label
    private lateinit var streakBadge: Image
    private lateinit var pauseIcon: Image
    private lateinit var sliderRail: Image
    private lateinit var sliderThumb: Image
    private lateinit var angleLabel: Label
    private lateinit var steerMeterSegments: Array<Image>
    private lateinit var bombButton: Image
    private lateinit var bombButtonBorder: Image
    private lateinit var bombButtonLabel: Label

    // --- Tracking previous values for change detection ---
    private var lastDisplayedScore: Long = -1L
    private var lastDisplayedStreak: Int = -1

    /**
     * Whether bomb mode is currently armed.
     *
     * When true, the next kick will be treated as a Big Bomb regardless of
     * power and slider thresholds. InputSystem reads this flag each frame;
     * HudSystem resets it automatically after the kick transitions to
     * BALL_IN_FLIGHT.
     */
    var isBombModeActive: Boolean = false
        private set

    // Timer for bomb button pulse animation when armed
    private var bombPulseTimer: Float = 0f

    /**
     * Current steer swipe count for the active kick.
     *
     * Since SteerDetector's swipeCount is private and we cannot modify other files,
     * this property is exposed for external wiring (e.g., LevelScreen can set it
     * based on steer events, or a GameStateListener can track it). Resets to 0
     * on entering BALL_IN_FLIGHT.
     */
    var steerSwipeCount: Int = 0

    /**
     * Slider value (0.0-1.0) for the angle slider visual.
     *
     * Exposed for external wiring. LevelScreen or InputRouter can push the
     * AngleSliderController.sliderValue here each frame.
     */
    var sliderValue: Float = 0.5f

    // ---- Lifecycle ----

    override fun addedToEngine(engine: com.badlogic.ashley.core.Engine) {
        super.addedToEngine(engine)
        gameStateManager.addListener(this)
        createStage()
    }

    override fun removedFromEngine(engine: com.badlogic.ashley.core.Engine) {
        super.removedFromEngine(engine)
        gameStateManager.removeListener(this)
        dispose()
    }

    /**
     * Build the Stage, create all HUD actors, and lay them out per ui-hud-layout.md.
     */
    private fun createStage() {
        viewport = FitViewport(WORLD_WIDTH, WORLD_HEIGHT)
        stage = Stage(viewport)

        val font = BitmapFont() // Default LibGDX font; will be replaced with custom arcade font later
        managedFonts.add(font)

        // Pre-allocate shared font for score popups (reused across all popups)
        popupFont = BitmapFont()
        managedFonts.add(popupFont)

        // --- Score Label (top-center) ---
        val scoreLabelStyle = Label.LabelStyle(font, Color.WHITE)
        scoreLabel = Label("0", scoreLabelStyle).apply {
            setFontScale(3f)
            setAlignment(Align.center)
        }
        scoreLabel.setPosition(
            WORLD_WIDTH / 2f - scoreLabel.prefWidth * 3f / 2f,
            WORLD_HEIGHT - EDGE_MARGIN - scoreLabel.prefHeight * 3f
        )
        // We'll position dynamically in update since prefWidth changes with content
        stage.addActor(scoreLabel)

        // --- Streak Badge (top-right) ---
        val streakBadgeTexture = createRoundedRectTexture(120, 48, Color(0.5f, 0.5f, 0.5f, 0.8f))
        streakBadge = Image(TextureRegionDrawable(TextureRegion(streakBadgeTexture))).apply {
            setSize(120f, 48f)
        }

        val streakLabelStyle = Label.LabelStyle(font, Color.WHITE)
        streakLabel = Label("\u00d71", streakLabelStyle).apply {
            setFontScale(2f)
            setAlignment(Align.center)
        }

        // Position streak at top-right
        streakBadge.setPosition(
            WORLD_WIDTH - EDGE_MARGIN - 120f,
            WORLD_HEIGHT - EDGE_MARGIN - 48f
        )
        streakLabel.setPosition(
            WORLD_WIDTH - EDGE_MARGIN - 120f,
            WORLD_HEIGHT - EDGE_MARGIN - 48f
        )
        streakLabel.setSize(120f, 48f)

        stage.addActor(streakBadge)
        stage.addActor(streakLabel)

        // --- Pause Icon (top-left) ---
        val pauseTexture = createPauseIconTexture(64, 64)
        pauseIcon = Image(TextureRegionDrawable(TextureRegion(pauseTexture))).apply {
            setSize(PAUSE_ICON_SIZE, PAUSE_ICON_SIZE)
            setPosition(EDGE_MARGIN, WORLD_HEIGHT - EDGE_MARGIN - PAUSE_ICON_SIZE)
            color.a = 0.6f // Semi-transparent when idle (from ui-hud-layout.md Section 7)
        }
        pauseIcon.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                pauseIcon.color.a = 1.0f // Fully opaque on touch
                gameStateManager.pause()
                return true
            }

            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                pauseIcon.color.a = 0.6f // Return to semi-transparent
            }
        })
        stage.addActor(pauseIcon)

        // --- Angle Slider Visual (left edge) ---
        val sliderRailTexture = createSolidTexture(
            SLIDER_RAIL_WIDTH.toInt(), SLIDER_RAIL_HEIGHT.toInt(),
            Color(1f, 1f, 1f, 0.3f)
        )
        sliderRail = Image(TextureRegionDrawable(TextureRegion(sliderRailTexture))).apply {
            setSize(SLIDER_RAIL_WIDTH, SLIDER_RAIL_HEIGHT)
            setPosition(
                EDGE_MARGIN + (SLIDER_TOUCH_WIDTH - SLIDER_RAIL_WIDTH) / 2f,
                (WORLD_HEIGHT - SLIDER_RAIL_HEIGHT) / 2f
            )
        }

        val sliderThumbTexture = createSolidTexture(
            SLIDER_THUMB_SIZE.toInt(), SLIDER_THUMB_SIZE.toInt(),
            Color(1f, 1f, 1f, 0.9f)
        )
        sliderThumb = Image(TextureRegionDrawable(TextureRegion(sliderThumbTexture))).apply {
            setSize(SLIDER_THUMB_SIZE, SLIDER_THUMB_SIZE)
        }

        val angleLabelStyle = Label.LabelStyle(font, Color.WHITE)
        angleLabel = Label("42\u00b0", angleLabelStyle).apply {
            setFontScale(1.2f)
            setAlignment(Align.center)
        }

        stage.addActor(sliderRail)
        stage.addActor(sliderThumb)
        stage.addActor(angleLabel)

        // --- Steer Budget Meter (right edge, 4 segments) ---
        steerMeterSegments = Array(4) { index ->
            val segTexture = createSolidTexture(
                STEER_METER_WIDTH.toInt(), STEER_METER_SEGMENT_HEIGHT.toInt(),
                STEER_SEGMENT_COLORS[index]
            )
            Image(TextureRegionDrawable(TextureRegion(segTexture))).apply {
                setSize(STEER_METER_WIDTH, STEER_METER_SEGMENT_HEIGHT)
                // Segments stack bottom-to-top: segment 1 at bottom, segment 4 at top
                val meterStartY = (WORLD_HEIGHT - (4 * STEER_METER_SEGMENT_HEIGHT + 3 * STEER_METER_GAP)) / 2f
                setPosition(
                    WORLD_WIDTH - EDGE_MARGIN - STEER_METER_WIDTH,
                    meterStartY + index * (STEER_METER_SEGMENT_HEIGHT + STEER_METER_GAP)
                )
                isVisible = false
            }
        }

        for (segment in steerMeterSegments) {
            stage.addActor(segment)
        }

        // --- Bomb Mode Button (bottom-right, above launch zone) ---
        // Border/glow layer (slightly larger, hidden when bomb mode is off)
        val bombBorderTexture = createRoundedRectTexture(
            (BOMB_BUTTON_WIDTH + 8f).toInt(), (BOMB_BUTTON_HEIGHT + 8f).toInt(),
            BOMB_BORDER_COLOR_ON
        )
        bombButtonBorder = Image(TextureRegionDrawable(TextureRegion(bombBorderTexture))).apply {
            setSize(BOMB_BUTTON_WIDTH + 8f, BOMB_BUTTON_HEIGHT + 8f)
            setPosition(
                WORLD_WIDTH - BOMB_BUTTON_MARGIN_RIGHT - BOMB_BUTTON_WIDTH - 4f,
                BOMB_BUTTON_MARGIN_BOTTOM - 4f
            )
            isVisible = false
            setOrigin(width / 2f, height / 2f)
        }
        stage.addActor(bombButtonBorder)

        // Main button background
        val bombBgTexture = createRoundedRectTexture(
            BOMB_BUTTON_WIDTH.toInt(), BOMB_BUTTON_HEIGHT.toInt(),
            BOMB_COLOR_OFF
        )
        bombButton = Image(TextureRegionDrawable(TextureRegion(bombBgTexture))).apply {
            setSize(BOMB_BUTTON_WIDTH, BOMB_BUTTON_HEIGHT)
            setPosition(
                WORLD_WIDTH - BOMB_BUTTON_MARGIN_RIGHT - BOMB_BUTTON_WIDTH,
                BOMB_BUTTON_MARGIN_BOTTOM
            )
            isVisible = false
            setOrigin(width / 2f, height / 2f)
        }

        // Touch listener for toggle behavior
        bombButton.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                toggleBombMode()
                return true
            }
        })
        stage.addActor(bombButton)

        // Label on the button
        val bombLabelStyle = Label.LabelStyle(font, Color.WHITE)
        bombButtonLabel = Label("BOMB", bombLabelStyle).apply {
            setFontScale(1.8f)
            setAlignment(Align.center)
        }
        bombButtonLabel.setSize(BOMB_BUTTON_WIDTH, BOMB_BUTTON_HEIGHT)
        bombButtonLabel.setPosition(
            WORLD_WIDTH - BOMB_BUTTON_MARGIN_RIGHT - BOMB_BUTTON_WIDTH,
            BOMB_BUTTON_MARGIN_BOTTOM
        )
        bombButtonLabel.isVisible = false
        bombButtonLabel.touchable = com.badlogic.gdx.scenes.scene2d.Touchable.disabled // Let clicks pass through to the button
        stage.addActor(bombButtonLabel)
    }

    // ---- Per-frame update ----

    override fun update(deltaTime: Float) {
        val state = gameStateManager.currentState

        // Determine if we are in a gameplay state (including PAUSED, which freezes gameplay).
        // During PAUSED, the HUD stays visible behind the pause overlay so the player
        // can still see the game state. The PauseOverlay itself is managed by LevelScreen.
        val prePauseState = if (state is GameState.Paused) {
            (state as GameState.Paused).previousState
        } else {
            null
        }

        val isGameplay = state is GameState.Ready || state is GameState.Aiming ||
                state is GameState.BallInFlight || state is GameState.Scoring ||
                state is GameState.ImpactMissed || state is GameState.Paused

        if (!isGameplay) {
            stage.root.isVisible = false
            stage.act(deltaTime)
            drawStageAndResetBatch()
            return
        }

        stage.root.isVisible = true

        // When paused, render the HUD in its frozen state but skip data updates
        // so score/streak/slider don't change while paused.
        if (state is GameState.Paused) {
            // Keep existing HUD element visibility based on the pre-pause state
            updateVisibilityForState(prePauseState)
            stage.act(deltaTime)
            drawStageAndResetBatch()
            return
        }

        // --- Update score display ---
        updateScoreDisplay()

        // --- Update streak badge ---
        updateStreakBadge()

        // --- Update steer budget meter visibility and state ---
        updateSteerMeter(state)

        // --- Update angle slider visual ---
        updateSliderVisual(state)

        // --- Update bomb mode button ---
        updateBombButton(state, deltaTime)

        // --- Pause icon always visible during gameplay ---
        pauseIcon.isVisible = true

        // Tick and draw the stage
        stage.act(deltaTime)
        drawStageAndResetBatch()
    }

    /**
     * Draw the stage and reset the batch state to clean defaults.
     *
     * After stage.draw(), the batch color and blend state may be left dirty
     * (e.g., tinted or non-standard blend mode from scene2d actors). This
     * resets the batch to white color and standard alpha blending so subsequent
     * render passes (other ECS systems) start from a known-good state.
     */
    private fun drawStageAndResetBatch() {
        stage.draw()

        val batch = stage.batch
        batch.color = Color.WHITE
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    /**
     * Update the session score label. Triggers a scale pulse animation when the score changes.
     */
    private fun updateScoreDisplay() {
        val currentScore = sessionAccumulator.sessionScore
        if (currentScore != lastDisplayedScore) {
            scoreLabel.setText(currentScore.toString())

            // Reposition to keep centered (prefWidth changes with digit count)
            scoreLabel.pack()
            scoreLabel.setPosition(
                (WORLD_WIDTH - scoreLabel.width * scoreLabel.fontScaleX) / 2f,
                WORLD_HEIGHT - EDGE_MARGIN - scoreLabel.height * scoreLabel.fontScaleY
            )

            // Scale pulse animation (from ui-hud-layout.md Section 2)
            if (lastDisplayedScore >= 0) { // Skip pulse on initial display
                scoreLabel.clearActions()
                scoreLabel.addAction(
                    Actions.sequence(
                        Actions.scaleTo(SCORE_PULSE_SCALE, SCORE_PULSE_SCALE, SCORE_PULSE_DURATION / 2f, Interpolation.smooth),
                        Actions.scaleTo(1f, 1f, SCORE_PULSE_DURATION / 2f, Interpolation.smooth)
                    )
                )
            }

            lastDisplayedScore = currentScore
        }
    }

    /**
     * Update the streak multiplier badge with correct text, color tier, and animations.
     */
    private fun updateStreakBadge() {
        val currentStreak = sessionAccumulator.currentStreak
        if (currentStreak != lastDisplayedStreak) {
            val tierIndex = getStreakTierIndex(currentStreak)
            val multiplier = STREAK_MULTIPLIERS[tierIndex]

            // Format multiplier text: show as integer if whole number, else one decimal
            val multiplierText = if (multiplier == multiplier.toLong().toFloat()) {
                "\u00d7${multiplier.toLong()}"
            } else {
                "\u00d7$multiplier"
            }
            streakLabel.setText(multiplierText)

            // Update badge color per tier (from ui-hud-layout.md Section 3)
            val tierColor = STREAK_COLORS[tierIndex]
            streakBadge.color.set(tierColor.r, tierColor.g, tierColor.b, 0.8f)
            streakLabel.style.fontColor = Color.WHITE

            // Pulse animation at 5+ consecutive hits (from ui-hud-layout.md Section 3)
            if (currentStreak >= 5 && currentStreak > lastDisplayedStreak && lastDisplayedStreak >= 0) {
                streakBadge.clearActions()
                streakBadge.addAction(
                    Actions.sequence(
                        Actions.scaleTo(1.1f, 1.1f, 0.1f, Interpolation.smooth),
                        Actions.scaleTo(1f, 1f, 0.1f, Interpolation.smooth)
                    )
                )
            }

            // Reset/deflation animation when streak breaks (from ui-hud-layout.md Section 3)
            if (currentStreak == 0 && lastDisplayedStreak > 0) {
                streakBadge.clearActions()
                streakBadge.addAction(
                    Actions.sequence(
                        Actions.scaleTo(0.9f, 0.9f, 0.1f, Interpolation.smooth),
                        Actions.scaleTo(1f, 1f, 0.15f, Interpolation.smooth)
                    )
                )
            }

            lastDisplayedStreak = currentStreak
        }
    }

    /**
     * Map streak count to multiplier tier index (0-4).
     *
     * From GDD Section 5:
     * - 0 or 1 hits = x1 (index 0)
     * - 2 hits = x1.5 (index 1)
     * - 3 hits = x2 (index 2)
     * - 4 hits = x2.5 (index 3)
     * - 5+ hits = x3 (index 4)
     */
    private fun getStreakTierIndex(streak: Int): Int {
        return when {
            streak <= 1 -> 0
            streak == 2 -> 1
            streak == 3 -> 2
            streak == 4 -> 3
            else -> 4
        }
    }

    /**
     * Set HUD element visibility based on a given state without updating data.
     * Used during PAUSED to freeze the HUD in its pre-pause visual state.
     */
    private fun updateVisibilityForState(state: GameState?) {
        if (state == null) return
        val showSlider = state is GameState.Ready || state is GameState.Aiming
        sliderRail.isVisible = showSlider
        sliderThumb.isVisible = showSlider
        angleLabel.isVisible = showSlider

        val showMeter = state is GameState.BallInFlight
        for (segment in steerMeterSegments) {
            segment.isVisible = showMeter
        }

        pauseIcon.isVisible = true
    }

    /**
     * Update steer budget meter visibility and segment fill states.
     *
     * Visible only during BALL_IN_FLIGHT (from ui-hud-layout.md Section 4).
     * Segments drain bottom-to-top as swipes are used.
     */
    private fun updateSteerMeter(state: GameState) {
        val showMeter = state is GameState.BallInFlight

        for (i in steerMeterSegments.indices) {
            steerMeterSegments[i].isVisible = showMeter
        }

        if (showMeter) {
            // Determine which segments are drained based on steerSwipeCount.
            // Swipe count 0: all 4 segments full
            // Swipe count 1: segment 0 (bottom) drained
            // Swipe count 2: segments 0,1 drained
            // Swipe count 3: segments 0,1,2 drained
            // Swipe count 4+: segments 0,1,2 drained; segment 3 (residual) pulses but never fully drains
            for (i in steerMeterSegments.indices) {
                val segment = steerMeterSegments[i]
                if (i < steerSwipeCount && i < 3) {
                    // Drained segment — dim it
                    segment.color.a = 0.15f
                } else if (i == 3 && steerSwipeCount >= 4) {
                    // Residual segment: pulse faintly but don't fully drain
                    // Simple visual: reduce alpha slightly to indicate usage
                    segment.color.a = 0.4f
                } else {
                    // Full segment
                    segment.color.a = STEER_SEGMENT_COLORS[i].a
                }
            }
        }
    }

    /**
     * Update angle slider visual position and visibility.
     *
     * Visible during READY and AIMING states (from ui-hud-layout.md Section 5).
     * Thumb position reflects [sliderValue].
     */
    private fun updateSliderVisual(state: GameState) {
        val showSlider = state is GameState.Ready || state is GameState.Aiming

        sliderRail.isVisible = showSlider
        sliderThumb.isVisible = showSlider
        angleLabel.isVisible = showSlider

        if (showSlider) {
            val railX = sliderRail.x
            val railY = sliderRail.y
            val railH = sliderRail.height

            // Thumb position along the rail
            val thumbY = railY + sliderValue * (railH - SLIDER_THUMB_SIZE)
            sliderThumb.setPosition(
                railX + (SLIDER_RAIL_WIDTH - SLIDER_THUMB_SIZE) / 2f,
                thumbY
            )

            // Compute current launch angle for the label
            val launchAngle = com.streetsoccer.physics.TuningConstants.MIN_ANGLE +
                    sliderValue * (com.streetsoccer.physics.TuningConstants.MAX_ANGLE - com.streetsoccer.physics.TuningConstants.MIN_ANGLE)
            angleLabel.setText("${launchAngle.toInt()}\u00b0")
            angleLabel.pack()
            angleLabel.setPosition(
                railX + SLIDER_RAIL_WIDTH + 8f,
                thumbY + (SLIDER_THUMB_SIZE - angleLabel.height) / 2f
            )
        }
    }

    // ---- Bomb Mode ----

    /**
     * Toggle bomb mode on/off. Called when the player taps the bomb button.
     */
    private fun toggleBombMode() {
        isBombModeActive = !isBombModeActive
        bombPulseTimer = 0f

        if (isBombModeActive) {
            // Visual feedback: brighten button and show border glow
            bombButton.color.set(BOMB_COLOR_ON)
            bombButtonBorder.isVisible = true
        } else {
            // Visual feedback: dim button and hide border
            bombButton.color.set(BOMB_COLOR_OFF)
            bombButtonBorder.isVisible = false
            // Reset scale in case pulse was mid-animation
            bombButton.setScale(1f)
            bombButtonBorder.setScale(1f)
        }
    }

    /**
     * Update the bomb button visibility and pulse animation.
     *
     * Button is visible only during READY and AIMING states. When armed,
     * it pulses gently to reinforce that bomb mode is active.
     */
    private fun updateBombButton(state: GameState, deltaTime: Float) {
        val showButton = state is GameState.Ready || state is GameState.Aiming

        bombButton.isVisible = showButton
        bombButtonLabel.isVisible = showButton
        // Border only visible when armed AND button is shown
        bombButtonBorder.isVisible = showButton && isBombModeActive

        if (showButton && isBombModeActive) {
            // Pulse animation: oscillate scale using a sine wave
            bombPulseTimer += deltaTime
            val t = (bombPulseTimer % BOMB_PULSE_PERIOD) / BOMB_PULSE_PERIOD
            val sineValue = kotlin.math.sin(t * 2f * Math.PI.toFloat())
            val scale = BOMB_PULSE_MIN_SCALE + (BOMB_PULSE_MAX_SCALE - BOMB_PULSE_MIN_SCALE) * (0.5f + 0.5f * sineValue)

            bombButton.setScale(scale)
            bombButtonBorder.setScale(scale)
            bombButtonLabel.setFontScale(1.8f * scale)
        } else {
            // Ensure normal scale when not pulsing
            bombButton.setScale(1f)
            bombButtonBorder.setScale(1f)
            bombButtonLabel.setFontScale(1.8f)
        }
    }

    // ---- Score Popups ----

    /**
     * Spawn a score popup at the given world-space position.
     *
     * The popup floats upward ~80px over ~1.0s while fading out,
     * per ui-hud-layout.md Section 6.
     *
     * @param worldX X position in game-space (1920x1080 reference)
     * @param worldY Y position in game-space
     * @param scoreValue The final multiplied point value to display
     * @param isMultiplied True if the hit was at x1.5+ streak (uses larger/gold style)
     */
    fun spawnScorePopup(worldX: Float, worldY: Float, scoreValue: Long, isMultiplied: Boolean = false) {
        if (!::stage.isInitialized || !::popupFont.isInitialized) return

        val color = if (isMultiplied) Color(1f, 0.9f, 0.2f, 1f) else Color.WHITE
        val style = Label.LabelStyle(popupFont, color)
        val popup = Label(scoreValue.toString(), style).apply {
            setFontScale(if (isMultiplied) 2.5f else 2f)
            setAlignment(Align.center)
        }
        popup.pack()

        // Position at the impact point in HUD viewport coordinates
        popup.setPosition(worldX - popup.width / 2f, worldY)

        // Scale-in animation for multiplied hits (from ui-hud-layout.md Section 6)
        if (isMultiplied) {
            popup.setScale(0.5f)
            popup.addAction(
                Actions.parallel(
                    Actions.scaleTo(1f, 1f, 0.15f, Interpolation.smooth),
                    Actions.sequence(
                        Actions.moveBy(0f, POPUP_FLOAT_DISTANCE, POPUP_DURATION, Interpolation.smooth),
                        Actions.removeActor()
                    ),
                    Actions.sequence(
                        Actions.delay(POPUP_DURATION * 0.5f),
                        Actions.fadeOut(POPUP_DURATION * 0.5f)
                    )
                )
            )
        } else {
            popup.addAction(
                Actions.parallel(
                    Actions.moveBy(0f, POPUP_FLOAT_DISTANCE, POPUP_DURATION, Interpolation.smooth),
                    Actions.sequence(
                        Actions.delay(POPUP_DURATION * 0.3f),
                        Actions.fadeOut(POPUP_DURATION * 0.7f)
                    ),
                    Actions.sequence(
                        Actions.delay(POPUP_DURATION),
                        Actions.removeActor()
                    )
                )
            )
        }

        stage.addActor(popup)
    }

    // ---- GameStateListener callbacks ----

    override fun onStateEnter(newState: GameState) {
        when (newState) {
            is GameState.BallInFlight -> {
                // Reset steer swipe count for the new kick
                steerSwipeCount = 0
                // Reset bomb mode after the kick has been launched (single-use per activation)
                if (isBombModeActive) {
                    isBombModeActive = false
                    bombPulseTimer = 0f
                    if (::bombButton.isInitialized) {
                        bombButton.color.set(BOMB_COLOR_OFF)
                        bombButton.setScale(1f)
                    }
                    if (::bombButtonBorder.isInitialized) {
                        bombButtonBorder.isVisible = false
                        bombButtonBorder.setScale(1f)
                    }
                    if (::bombButtonLabel.isInitialized) {
                        bombButtonLabel.setFontScale(1.8f)
                    }
                }
            }
            is GameState.Ready -> {
                // Steer meter will be hidden by updateSteerMeter
            }
            else -> { /* no action */ }
        }
    }

    override fun onStateExit(oldState: GameState) {
        // No specific exit actions needed — visibility is managed in update()
    }

    // ---- Stage access for input multiplexing ----

    /**
     * Returns the HUD stage so it can be added to an InputMultiplexer
     * or set as a secondary input processor. The pause icon requires
     * touch event delivery via this stage.
     */
    fun getStage(): Stage {
        return if (::stage.isInitialized) stage else {
            // Fallback: create a minimal stage (should not happen in normal flow)
            createStage()
            stage
        }
    }

    /**
     * Call when the screen viewport is resized so the HUD viewport stays in sync.
     */
    fun resize(width: Int, height: Int) {
        if (::viewport.isInitialized) {
            viewport.update(width, height, true)
        }
    }

    // ---- Procedural texture helpers ----

    /**
     * Create a solid-color texture with the given dimensions.
     * The texture is tracked for disposal.
     */
    private fun createSolidTexture(width: Int, height: Int, color: Color): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        managedTextures.add(texture)
        return texture
    }

    /**
     * Create a rounded-rectangle texture for badge backgrounds.
     * Uses a simple filled rectangle with corner rounding approximated by
     * a solid fill (true rounding requires a shader or NinePatch; deferred to polish).
     */
    private fun createRoundedRectTexture(width: Int, height: Int, color: Color): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        // Simple corner rounding: clear the 4 corner pixels for a subtle effect
        val transparent = Color(0f, 0f, 0f, 0f)
        pixmap.setColor(transparent)
        // Top-left corner
        pixmap.drawPixel(0, 0)
        pixmap.drawPixel(1, 0)
        pixmap.drawPixel(0, 1)
        // Top-right corner
        pixmap.drawPixel(width - 1, 0)
        pixmap.drawPixel(width - 2, 0)
        pixmap.drawPixel(width - 1, 1)
        // Bottom-left corner
        pixmap.drawPixel(0, height - 1)
        pixmap.drawPixel(1, height - 1)
        pixmap.drawPixel(0, height - 2)
        // Bottom-right corner
        pixmap.drawPixel(width - 1, height - 1)
        pixmap.drawPixel(width - 2, height - 1)
        pixmap.drawPixel(width - 1, height - 2)

        val texture = Texture(pixmap)
        pixmap.dispose()
        managedTextures.add(texture)
        return texture
    }

    /**
     * Create a pause icon texture (two vertical bars).
     * Per ui-hud-layout.md Section 7: standard double-bar pause icon.
     */
    private fun createPauseIconTexture(width: Int, height: Int): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color(0f, 0f, 0f, 0f)) // Clear background
        pixmap.fill()

        pixmap.setColor(Color.WHITE)
        val barWidth = width / 5
        val barHeight = (height * 0.7f).toInt()
        val barY = (height - barHeight) / 2
        // Left bar
        pixmap.fillRectangle(width / 4 - barWidth / 2, barY, barWidth, barHeight)
        // Right bar
        pixmap.fillRectangle(3 * width / 4 - barWidth / 2, barY, barWidth, barHeight)

        val texture = Texture(pixmap)
        pixmap.dispose()
        managedTextures.add(texture)
        return texture
    }

    // ---- Disposal ----

    /**
     * Dispose of all managed resources. Called when the system is removed from the engine.
     */
    fun dispose() {
        if (::stage.isInitialized) {
            stage.dispose()
        }
        for (texture in managedTextures) {
            texture.dispose()
        }
        managedTextures.clear()
        for (font in managedFonts) {
            font.dispose()
        }
        managedFonts.clear()
    }
}
