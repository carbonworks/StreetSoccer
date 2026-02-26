package com.streetsoccer.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.streetsoccer.state.GameStateManager

/**
 * Semi-transparent overlay displayed during the PAUSED game state.
 *
 * Renders three vertically stacked buttons (RESUME, SETTINGS, QUIT) over a
 * dimmed background, per menu-and-navigation-flow.md Section 4.
 *
 * All BitmapFont and Texture instances created by this overlay are tracked
 * in managed lists and disposed when [dispose] is called, preventing memory
 * leaks across sessions.
 *
 * @see com.streetsoccer.state.GameStateManager
 */
class PauseOverlay(
    private val gameStateManager: GameStateManager
) : Disposable {

    companion object {
        // Reference resolution matching ui-hud-layout.md Section 1
        private const val WORLD_WIDTH = 1920f
        private const val WORLD_HEIGHT = 1080f

        // Overlay background dim color (from menu-and-navigation-flow.md Section 4)
        private val DIM_COLOR = Color(0f, 0f, 0f, 0.7f)

        // Button dimensions (arcade-style wide rounded rectangles)
        private const val BUTTON_WIDTH = 480f
        private const val BUTTON_HEIGHT = 96f
        private const val BUTTON_GAP = 16f

        // Button colors
        private val BUTTON_COLOR = Color(0.2f, 0.2f, 0.25f, 0.9f)
        private val BUTTON_HOVER_COLOR = Color(0.3f, 0.3f, 0.35f, 0.95f)
    }

    // --- Stage and viewport ---
    lateinit var stage: Stage
        private set
    private lateinit var viewport: FitViewport

    // --- Managed resources for leak-free disposal ---
    private val managedTextures = mutableListOf<Texture>()
    private val managedFonts = mutableListOf<BitmapFont>()

    // --- Visibility state ---
    var isVisible: Boolean = false
        private set

    /**
     * Initialize the overlay stage and all UI actors.
     * Must be called once before [show]/[hide]/[act]/[draw].
     */
    fun create() {
        viewport = FitViewport(WORLD_WIDTH, WORLD_HEIGHT)
        stage = Stage(viewport)

        // Shared font for all button labels
        val buttonFont = BitmapFont()
        managedFonts.add(buttonFont)

        // --- Fullscreen dim background ---
        val dimTexture = createSolidTexture(1, 1, DIM_COLOR)
        val dimBackground = Image(TextureRegionDrawable(TextureRegion(dimTexture))).apply {
            setSize(WORLD_WIDTH, WORLD_HEIGHT)
            setPosition(0f, 0f)
        }
        // Consume taps on the dim background so they don't pass through
        dimBackground.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                return true // consume
            }
        })
        stage.addActor(dimBackground)

        // --- Calculate button stack position (centered vertically) ---
        val totalHeight = 3 * BUTTON_HEIGHT + 2 * BUTTON_GAP
        val startY = (WORLD_HEIGHT + totalHeight) / 2f - BUTTON_HEIGHT
        val buttonX = (WORLD_WIDTH - BUTTON_WIDTH) / 2f

        // --- RESUME button (top of stack) ---
        createButton(buttonFont, "R E S U M E", buttonX, startY) {
            gameStateManager.resume()
        }

        // --- SETTINGS button (middle) ---
        createButton(buttonFont, "S E T T I N G S", buttonX, startY - (BUTTON_HEIGHT + BUTTON_GAP)) {
            // Settings overlay not yet implemented; no-op for now
        }

        // --- QUIT button (bottom) ---
        createButton(buttonFont, "Q U I T", buttonX, startY - 2 * (BUTTON_HEIGHT + BUTTON_GAP)) {
            gameStateManager.quit()
        }
    }

    /**
     * Create a single arcade-style button and add it to the stage.
     */
    private fun createButton(font: BitmapFont, text: String, x: Float, y: Float, onClick: () -> Unit) {
        val bgTexture = createRoundedRectTexture(BUTTON_WIDTH.toInt(), BUTTON_HEIGHT.toInt(), BUTTON_COLOR)
        val buttonBg = Image(TextureRegionDrawable(TextureRegion(bgTexture))).apply {
            setSize(BUTTON_WIDTH, BUTTON_HEIGHT)
            setPosition(x, y)
        }

        val labelStyle = Label.LabelStyle(font, Color.WHITE)
        val label = Label(text, labelStyle).apply {
            setFontScale(2f)
            setAlignment(Align.center)
            setSize(BUTTON_WIDTH, BUTTON_HEIGHT)
            setPosition(x, y)
        }

        buttonBg.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, ex: Float, ey: Float, pointer: Int, button: Int): Boolean {
                buttonBg.color.set(BUTTON_HOVER_COLOR)
                return true
            }

            override fun touchUp(event: InputEvent?, ex: Float, ey: Float, pointer: Int, button: Int) {
                buttonBg.color.set(BUTTON_COLOR)
                onClick()
            }
        })

        stage.addActor(buttonBg)
        stage.addActor(label)
    }

    /**
     * Show the pause overlay. Adds the stage to the input chain.
     */
    fun show() {
        isVisible = true
    }

    /**
     * Hide the pause overlay.
     */
    fun hide() {
        isVisible = false
    }

    /**
     * Tick the overlay stage (for animations).
     */
    fun act(delta: Float) {
        if (!isVisible) return
        stage.act(delta)
    }

    /**
     * Draw the overlay stage and reset batch state to clean defaults.
     *
     * After stage.draw(), the batch color and blend state may be left dirty.
     * This resets the batch to white color and standard alpha blending so
     * subsequent render passes start from a known-good state.
     */
    fun draw() {
        if (!isVisible) return
        stage.draw()

        // Reset batch state for subsequent renderers
        val batch = stage.batch
        batch.color = Color.WHITE
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    /**
     * Update the overlay viewport on screen resize.
     */
    fun resize(width: Int, height: Int) {
        if (::viewport.isInitialized) {
            viewport.update(width, height, true)
        }
    }

    // ---- Procedural texture helpers ----

    private fun createSolidTexture(width: Int, height: Int, color: Color): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        managedTextures.add(texture)
        return texture
    }

    private fun createRoundedRectTexture(width: Int, height: Int, color: Color): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val transparent = Color(0f, 0f, 0f, 0f)
        pixmap.setColor(transparent)
        pixmap.drawPixel(0, 0)
        pixmap.drawPixel(1, 0)
        pixmap.drawPixel(0, 1)
        pixmap.drawPixel(width - 1, 0)
        pixmap.drawPixel(width - 2, 0)
        pixmap.drawPixel(width - 1, 1)
        pixmap.drawPixel(0, height - 1)
        pixmap.drawPixel(1, height - 1)
        pixmap.drawPixel(0, height - 2)
        pixmap.drawPixel(width - 1, height - 1)
        pixmap.drawPixel(width - 2, height - 1)
        pixmap.drawPixel(width - 1, height - 2)
        val texture = Texture(pixmap)
        pixmap.dispose()
        managedTextures.add(texture)
        return texture
    }

    // ---- Disposal ----

    /**
     * Dispose of all managed resources (fonts, textures, stage).
     * Must be called when the overlay is no longer needed.
     */
    override fun dispose() {
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
