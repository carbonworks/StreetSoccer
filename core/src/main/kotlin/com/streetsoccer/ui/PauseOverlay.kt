package com.streetsoccer.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport

/**
 * Semi-transparent pause overlay with Resume and Quit buttons.
 *
 * Renders above the game scene and HUD when the game is paused.
 * Per menu-and-navigation-flow.md Section 4:
 * - A semi-transparent overlay dims the frozen game scene
 * - Three large, vertically stacked buttons (RESUME, SETTINGS, QUIT)
 *   Alpha scope: RESUME and QUIT only (SETTINGS deferred)
 * - Buttons are large, bold, arcade-style — wide rounded rectangles with uppercase spaced lettering
 * - Overlay background is rgba(0, 0, 0, 0.7)
 *
 * @param onResume Called when the Resume button is tapped
 * @param onQuit Called when the Quit button is tapped
 */
class PauseOverlay(
    private val onResume: () -> Unit,
    private val onQuit: () -> Unit
) {
    companion object {
        private const val WORLD_WIDTH = 1920f
        private const val WORLD_HEIGHT = 1080f
        private const val BUTTON_WIDTH = 520f
        private const val BUTTON_HEIGHT = 100f
        private const val BUTTON_SPACING = 32f
        private val DIM_COLOR = Color(0f, 0f, 0f, 0.7f)
        private val BUTTON_COLOR = Color(0.2f, 0.24f, 0.3f, 0.95f)
        private val BUTTON_HOVER_COLOR = Color(0.3f, 0.36f, 0.44f, 0.95f)
    }

    private val viewport = FitViewport(WORLD_WIDTH, WORLD_HEIGHT)
    val stage = Stage(viewport)
    private val managedTextures = mutableListOf<Texture>()
    private var visible = false

    init {
        buildUI()
    }

    private fun buildUI() {
        // Full-screen dim background — consumes all taps so nothing passes through
        val dimTexture = createSolidTexture(1, 1, DIM_COLOR)
        val dimImage = Image(TextureRegionDrawable(TextureRegion(dimTexture))).apply {
            setSize(WORLD_WIDTH, WORLD_HEIGHT)
            setPosition(0f, 0f)
        }
        dimImage.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                // Consume taps on the dim background so they don't reach gameplay
                return true
            }
        })
        stage.addActor(dimImage)

        // Center table for buttons
        val table = Table().apply {
            setFillParent(true)
            center()
        }

        // Title label: "PAUSED"
        val font = BitmapFont()
        val titleStyle = Label.LabelStyle(font, Color.WHITE)
        val titleLabel = Label("P A U S E D", titleStyle).apply {
            setFontScale(4f)
            setAlignment(Align.center)
        }
        table.add(titleLabel).padBottom(60f)
        table.row()

        // Resume button
        val resumeButton = createButton("R E S U M E") {
            onResume()
        }
        table.add(resumeButton).size(BUTTON_WIDTH, BUTTON_HEIGHT).padBottom(BUTTON_SPACING)
        table.row()

        // Quit button
        val quitButton = createButton("Q U I T") {
            onQuit()
        }
        table.add(quitButton).size(BUTTON_WIDTH, BUTTON_HEIGHT)
        table.row()

        stage.addActor(table)
    }

    /**
     * Create an arcade-style button with a text label and tap callback.
     * Returns a Table containing the button background and label.
     */
    private fun createButton(text: String, onClick: () -> Unit): Table {
        val bgTexture = createSolidTexture(
            BUTTON_WIDTH.toInt(), BUTTON_HEIGHT.toInt(), BUTTON_COLOR
        )
        val bgDrawable = TextureRegionDrawable(TextureRegion(bgTexture))

        val hoverTexture = createSolidTexture(
            BUTTON_WIDTH.toInt(), BUTTON_HEIGHT.toInt(), BUTTON_HOVER_COLOR
        )
        val hoverDrawable = TextureRegionDrawable(TextureRegion(hoverTexture))

        val font = BitmapFont()
        val labelStyle = Label.LabelStyle(font, Color.WHITE)
        val label = Label(text, labelStyle).apply {
            setFontScale(2.5f)
            setAlignment(Align.center)
        }

        val buttonTable = Table().apply {
            setSize(BUTTON_WIDTH, BUTTON_HEIGHT)
            background = bgDrawable
            add(label).expand().fill()
        }

        buttonTable.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                buttonTable.background = hoverDrawable
                return true
            }

            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                buttonTable.background = bgDrawable
                // Only fire if the touch ended within the button bounds
                if (x >= 0 && x <= BUTTON_WIDTH && y >= 0 && y <= BUTTON_HEIGHT) {
                    onClick()
                }
            }
        })

        return buttonTable
    }

    /** Show the pause overlay and enable its input processing. */
    fun show() {
        visible = true
        stage.root.isVisible = true
    }

    /** Hide the pause overlay and disable its input processing. */
    fun hide() {
        visible = false
        stage.root.isVisible = false
    }

    /** Returns true if the overlay is currently visible. */
    fun isVisible(): Boolean = visible

    /** Render the overlay. Call this after drawing the game scene and HUD. */
    fun render(delta: Float) {
        if (!visible) return
        stage.act(delta)
        stage.draw()
    }

    /** Resize the overlay viewport to match the screen dimensions. */
    fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    /** Dispose of all managed resources. */
    fun dispose() {
        stage.dispose()
        for (texture in managedTextures) {
            texture.dispose()
        }
        managedTextures.clear()
    }

    private fun createSolidTexture(width: Int, height: Int, color: Color): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        managedTextures.add(texture)
        return texture
    }
}
