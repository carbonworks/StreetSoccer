package com.streetsoccer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
import com.streetsoccer.GameBootstrapper
import com.streetsoccer.services.CareerStats
import com.streetsoccer.services.ProfileData
import ktx.app.KtxScreen
import java.text.NumberFormat
import java.util.Locale

/**
 * Attract screen (main menu) — the arcade-style entry point that draws the player
 * into gameplay with minimal friction.
 *
 * Layout (1920x1080 virtual resolution):
 * - Game title "STREET SOCCER" at top-center, ~15% from top
 * - "TAP TO PLAY" center of screen with pulsing scale/opacity animation
 * - Bottom icon bar: [Stats] and [Settings] labels at ~48px from bottom
 * - Overlays: Settings and Stats placeholder panels (at most one open at a time)
 */
class AttractScreen(private val game: GameBootstrapper) : KtxScreen {

    // Virtual resolution matching the target spec
    private val worldWidth = 1920f
    private val worldHeight = 1080f

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(worldWidth, worldHeight, camera)
    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val font = BitmapFont()
    private val titleFont = BitmapFont()
    private val layout = GlyphLayout()

    // Background texture — attempt to load, fall back to solid color
    private var backgroundTexture: Texture? = null
    private var backgroundLoaded = false

    // Pulse animation state for TAP TO PLAY
    private var pulseTimer = 0f
    private val pulseCycleDuration = 2f // ~2 second full cycle

    // Overlay state — at most one overlay active at a time
    private enum class Overlay { NONE, SETTINGS, STATS }
    private var activeOverlay = Overlay.NONE

    // Touch target rectangles (in world coordinates)
    private val statsButtonBounds = Rectangle()
    private val settingsButtonBounds = Rectangle()
    private val overlayCloseBounds = Rectangle()
    private val overlayPanelBounds = Rectangle()

    // Overlay panel dimensions
    private val panelWidth = 600f
    private val panelHeight = 700f

    // Cached profile data — loaded fresh each time the stats overlay opens
    private var cachedProfile: ProfileData? = null

    // Temporary vector for unprojecting touch coordinates
    private val touchPoint = Vector3()

    private val inputProcessor = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            touchPoint.set(screenX.toFloat(), screenY.toFloat(), 0f)
            viewport.unproject(touchPoint)
            val wx = touchPoint.x
            val wy = touchPoint.y

            if (activeOverlay != Overlay.NONE) {
                // Check close button (X) on overlay
                if (overlayCloseBounds.contains(wx, wy)) {
                    activeOverlay = Overlay.NONE
                    return true
                }
                // Tap outside the overlay panel closes it
                if (!overlayPanelBounds.contains(wx, wy)) {
                    activeOverlay = Overlay.NONE
                    return true
                }
                // Consume taps inside the overlay panel (no pass-through)
                return true
            }

            // No overlay open — check icon bar
            if (statsButtonBounds.contains(wx, wy)) {
                cachedProfile = game.saveService.loadProfile()
                activeOverlay = Overlay.STATS
                return true
            }
            if (settingsButtonBounds.contains(wx, wy)) {
                activeOverlay = Overlay.SETTINGS
                return true
            }

            // Tap anywhere else → transition to gameplay
            game.setScreen<LevelScreen>()
            return true
        }

        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                if (activeOverlay != Overlay.NONE) {
                    activeOverlay = Overlay.NONE
                    return true
                }
                // At attract screen root with no overlay — no-op per spec
                // (exit confirmation is a polish item, not alpha scope)
                return true
            }
            return false
        }
    }

    override fun show() {
        Gdx.app.log("AttractScreen", "show")

        // Catch Android back key so it doesn't exit the app
        @Suppress("DEPRECATION")
        Gdx.input.setCatchBackKey(true)
        Gdx.input.inputProcessor = inputProcessor

        // Try to load background texture
        tryLoadBackground()

        // Configure fonts — scale the default bitmap font for our virtual resolution.
        // LibGDX's default BitmapFont is 15px, designed for pixel-perfect rendering.
        // At 1920x1080 we need to scale it significantly.
        font.data.setScale(3f)
        font.color = Color.WHITE

        titleFont.data.setScale(5f)
        titleFont.color = Color.WHITE

        // Compute icon bar touch targets at bottom of screen
        val iconY = 48f
        val iconSize = 64f
        val iconSpacing = 200f // distance between the two icon centers

        val centerX = worldWidth / 2f
        // Stats on the left, Settings on the right (per spec Section 2)
        statsButtonBounds.set(
            centerX - iconSpacing / 2f - iconSize / 2f,
            iconY - iconSize / 2f,
            iconSize * 2.5f, // wider touch target for text label
            iconSize
        )
        settingsButtonBounds.set(
            centerX + iconSpacing / 2f - iconSize / 2f,
            iconY - iconSize / 2f,
            iconSize * 2.5f, // wider touch target for text label
            iconSize
        )

        activeOverlay = Overlay.NONE
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun render(delta: Float) {
        // Update pulse animation
        pulseTimer += delta
        if (pulseTimer > pulseCycleDuration) {
            pulseTimer -= pulseCycleDuration
        }

        // Clear screen
        Gdx.gl.glClearColor(0.12f, 0.15f, 0.18f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        batch.projectionMatrix = camera.combined
        shapeRenderer.projectionMatrix = camera.combined

        // --- Layer 1: Background ---
        batch.begin()
        if (backgroundLoaded && backgroundTexture != null) {
            batch.draw(backgroundTexture, 0f, 0f, worldWidth, worldHeight)
        }
        batch.end()

        // --- Layer 2: Title and TAP TO PLAY ---
        batch.begin()
        drawTitle()
        drawTapToPlay()
        drawIconBar()
        batch.end()

        // --- Layer 3: Overlay (if active) ---
        if (activeOverlay != Overlay.NONE) {
            drawOverlay()
        }

        // Handle back key via polling as a fallback
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (activeOverlay != Overlay.NONE) {
                activeOverlay = Overlay.NONE
            }
        }
    }

    // --- Drawing Methods ---

    private fun drawTitle() {
        titleFont.color = Color.WHITE
        layout.setText(titleFont, "S T R E E T   S O C C E R")
        val titleX = (worldWidth - layout.width) / 2f
        val titleY = worldHeight - worldHeight * 0.15f + layout.height / 2f
        titleFont.draw(batch, layout, titleX, titleY)
    }

    private fun drawTapToPlay() {
        // Pulse animation: scale oscillates ~95%-105%, opacity breathes 80%-100%
        val phase = (pulseTimer / pulseCycleDuration) * MathUtils.PI2
        val scaleOscillation = 1f + 0.05f * MathUtils.sin(phase) // 0.95 - 1.05
        val opacityBreath = 0.9f + 0.1f * MathUtils.sin(phase)   // 0.80 - 1.00

        val baseScale = 4f
        val animatedScale = baseScale * scaleOscillation

        font.data.setScale(animatedScale)
        font.color = Color(1f, 1f, 1f, opacityBreath)

        layout.setText(font, "TAP  TO  PLAY")
        val textX = (worldWidth - layout.width) / 2f
        val textY = worldHeight / 2f + layout.height / 2f
        font.draw(batch, layout, textX, textY)

        // Reset font scale for other uses
        font.data.setScale(3f)
        font.color = Color.WHITE
    }

    private fun drawIconBar() {
        font.data.setScale(2f)

        // Stats icon (left) — trophy placeholder
        font.color = Color.LIGHT_GRAY
        layout.setText(font, "[Stats]")
        val statsX = statsButtonBounds.x + (statsButtonBounds.width - layout.width) / 2f
        val statsY = statsButtonBounds.y + statsButtonBounds.height / 2f + layout.height / 2f
        font.draw(batch, layout, statsX, statsY)

        // Settings icon (right) — gear placeholder
        layout.setText(font, "[Settings]")
        val settingsX = settingsButtonBounds.x + (settingsButtonBounds.width - layout.width) / 2f
        val settingsY = settingsButtonBounds.y + settingsButtonBounds.height / 2f + layout.height / 2f
        font.draw(batch, layout, settingsX, settingsY)

        // Reset font
        font.data.setScale(3f)
        font.color = Color.WHITE
    }

    private fun drawOverlay() {
        val panelX = (worldWidth - panelWidth) / 2f
        val panelY = (worldHeight - panelHeight) / 2f

        // Update panel bounds for touch detection
        overlayPanelBounds.set(panelX, panelY, panelWidth, panelHeight)

        // Close button in top-right of panel
        val closeBtnSize = 48f
        overlayCloseBounds.set(
            panelX + panelWidth - closeBtnSize - 10f,
            panelY + panelHeight - closeBtnSize - 10f,
            closeBtnSize,
            closeBtnSize
        )

        // --- Draw dimmed background ---
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.7f)
        shapeRenderer.rect(0f, 0f, worldWidth, worldHeight)
        shapeRenderer.end()

        // --- Draw panel background ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.15f, 0.18f, 0.22f, 0.95f)
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight)
        shapeRenderer.end()

        // --- Draw panel border ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color(0.4f, 0.45f, 0.5f, 1f)
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight)
        shapeRenderer.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)

        // --- Draw panel content ---
        batch.begin()
        when (activeOverlay) {
            Overlay.SETTINGS -> drawSettingsContent(panelX, panelY)
            Overlay.STATS -> drawStatsContent(panelX, panelY)
            Overlay.NONE -> { /* unreachable */ }
        }

        // Draw close button [X]
        font.data.setScale(2.5f)
        font.color = Color.LIGHT_GRAY
        layout.setText(font, "X")
        font.draw(
            batch, layout,
            overlayCloseBounds.x + (overlayCloseBounds.width - layout.width) / 2f,
            overlayCloseBounds.y + overlayCloseBounds.height / 2f + layout.height / 2f
        )
        font.data.setScale(3f)
        font.color = Color.WHITE

        batch.end()
    }

    private fun drawSettingsContent(panelX: Float, panelY: Float) {
        val contentX = panelX + 40f
        val topY = panelY + panelHeight - 60f

        // Header
        titleFont.data.setScale(3f)
        titleFont.color = Color.WHITE
        layout.setText(titleFont, "S E T T I N G S")
        titleFont.draw(batch, layout, panelX + (panelWidth - layout.width) / 2f, topY)
        titleFont.data.setScale(5f)

        // Separator line will be drawn as text
        font.data.setScale(1.5f)
        font.color = Color.GRAY
        layout.setText(font, "----------------------------")
        font.draw(batch, layout, panelX + (panelWidth - layout.width) / 2f, topY - 40f)

        // Trajectory Preview toggle (placeholder)
        font.data.setScale(2f)
        font.color = Color.LIGHT_GRAY
        layout.setText(font, "Trajectory Preview")
        font.draw(batch, layout, contentX, topY - 100f)

        font.color = Color(0.5f, 0.8f, 0.5f, 1f)
        layout.setText(font, "[OFF | on]")
        font.draw(batch, layout, panelX + panelWidth - 220f, topY - 100f)

        // Placeholder note
        font.data.setScale(1.5f)
        font.color = Color.DARK_GRAY
        layout.setText(font, "(settings placeholder)")
        font.draw(batch, layout, panelX + (panelWidth - layout.width) / 2f, topY - 200f)

        font.data.setScale(3f)
        font.color = Color.WHITE
    }

    private fun drawStatsContent(panelX: Float, panelY: Float) {
        val career = cachedProfile?.career ?: CareerStats()
        val contentX = panelX + 40f
        val valueRightEdge = panelX + panelWidth - 40f
        val topY = panelY + panelHeight - 60f

        // Header
        titleFont.data.setScale(3f)
        titleFont.color = Color.WHITE
        layout.setText(titleFont, "H I G H   S C O R E")
        titleFont.draw(batch, layout, panelX + (panelWidth - layout.width) / 2f, topY)
        titleFont.data.setScale(5f)

        // Separator
        font.data.setScale(1.5f)
        font.color = Color.GRAY
        layout.setText(font, "----------------------------")
        font.draw(batch, layout, panelX + (panelWidth - layout.width) / 2f, topY - 40f)

        // Format numbers with thousands separators
        val nf = NumberFormat.getIntegerInstance(Locale.US)

        // Compute hit rate
        val hitRate = if (career.totalKicks > 0) {
            String.format(Locale.US, "%.1f%%", career.totalHits.toDouble() / career.totalKicks.toDouble() * 100.0)
        } else {
            "--"
        }

        // Format longest bomb distance
        val longestBomb = if (career.longestBigBombDistance > 0f) {
            String.format(Locale.US, "%.1fm", career.longestBigBombDistance)
        } else {
            "--"
        }

        // Career stat rows
        val stats = listOf(
            "Total Kicks" to nf.format(career.totalKicks),
            "Total Hits" to nf.format(career.totalHits),
            "Hit Rate" to hitRate,
            "Total Score" to nf.format(career.totalScore),
            "Best Session" to nf.format(career.bestSessionScore),
            "Best Streak" to nf.format(career.bestStreak),
            "Longest Bomb" to longestBomb
        )

        font.data.setScale(2f)
        val lineHeight = 42f
        var y = topY - 100f

        for ((label, value) in stats) {
            font.color = Color.LIGHT_GRAY
            layout.setText(font, label)
            font.draw(batch, layout, contentX, y)

            font.color = Color.WHITE
            layout.setText(font, value)
            font.draw(batch, layout, valueRightEdge - layout.width, y)

            y -= lineHeight
        }

        // Targets by type section
        if (career.targetsByType.isNotEmpty()) {
            y -= 10f // extra spacing before section

            // Section header
            font.data.setScale(1.5f)
            font.color = Color.GRAY
            layout.setText(font, "-- Targets --")
            font.draw(batch, layout, panelX + (panelWidth - layout.width) / 2f, y)
            y -= 38f

            font.data.setScale(1.8f)
            // Sort by count descending for a leaderboard feel
            val sortedTargets = career.targetsByType.entries.sortedByDescending { it.value }
            for ((typeKey, count) in sortedTargets) {
                font.color = Color.LIGHT_GRAY
                val displayName = formatTargetTypeName(typeKey)
                layout.setText(font, displayName)
                font.draw(batch, layout, contentX + 20f, y)

                font.color = Color.WHITE
                layout.setText(font, nf.format(count))
                font.draw(batch, layout, valueRightEdge - layout.width, y)

                y -= 36f
            }
        }

        font.data.setScale(3f)
        font.color = Color.WHITE
    }

    /**
     * Convert a target type key (e.g. "garage_door", "window") to a display name
     * with title case and underscores replaced by spaces.
     */
    private fun formatTargetTypeName(key: String): String {
        return key.split("_").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
    }

    // --- Utility ---

    private fun tryLoadBackground() {
        if (backgroundLoaded) return
        try {
            // Try loading from internal assets path first, then from root
            val fileHandle = Gdx.files.internal("background.jpg")
            if (fileHandle.exists()) {
                backgroundTexture = Texture(fileHandle)
                backgroundLoaded = true
                Gdx.app.log("AttractScreen", "Loaded background.jpg")
            } else {
                Gdx.app.log("AttractScreen", "background.jpg not found — using solid color fallback")
            }
        } catch (e: Exception) {
            Gdx.app.log("AttractScreen", "Failed to load background: ${e.message}")
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        font.dispose()
        titleFont.dispose()
        backgroundTexture?.dispose()
    }
}
