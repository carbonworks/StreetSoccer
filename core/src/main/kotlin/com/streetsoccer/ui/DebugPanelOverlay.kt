package com.streetsoccer.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.streetsoccer.physics.DebugOverrides

/**
 * Debug panel overlay for live-tuning all ball flight constants.
 *
 * Renders as a semi-transparent panel over the game scene. Contains:
 * - A master enable/disable toggle at the top
 * - A horizontally scrolling container of vertical slider columns,
 *   one per tuning constant in [DebugOverrides.allConstants]
 * - Each column shows the constant name, current value, and a slider
 *   with the defined min/max range
 * - A close button to dismiss the overlay
 *
 * Values are session-only and never written to disk. When the master
 * toggle is off, all overrides are null and the game uses
 * [com.streetsoccer.physics.TuningConstants] defaults.
 *
 * Follows the same lifecycle pattern as [PauseOverlay]: show/hide/render/resize/dispose.
 *
 * All [BitmapFont] and [Texture] instances created by this overlay are tracked
 * in managed lists and disposed when [dispose] is called.
 */
class DebugPanelOverlay {

    companion object {
        private const val WORLD_WIDTH = 1920f
        private const val WORLD_HEIGHT = 1080f

        // Panel layout
        private const val PANEL_HEIGHT = 420f
        private const val PANEL_TOP_MARGIN = 60f
        private const val HEADER_HEIGHT = 60f
        private const val SLIDER_COLUMN_WIDTH = 180f
        private const val SLIDER_HEIGHT = 220f
        private const val COLUMN_PAD = 12f
        private const val VALUE_LABEL_FONT_SCALE = 1.3f
        private const val NAME_LABEL_FONT_SCALE = 1.1f
        private const val HEADER_FONT_SCALE = 2f
        private const val CLOSE_BUTTON_SIZE = 48f

        // Colors
        private val PANEL_BG_COLOR = Color(0.08f, 0.08f, 0.12f, 0.92f)
        private val HEADER_BG_COLOR = Color(0.12f, 0.14f, 0.18f, 0.95f)
        private val COLUMN_BG_COLOR = Color(0.14f, 0.16f, 0.2f, 0.9f)
        private val TOGGLE_ON_COLOR = Color(0.3f, 0.85f, 0.4f, 1f)
        private val TOGGLE_OFF_COLOR = Color(0.5f, 0.2f, 0.2f, 0.85f)
        private val SLIDER_BG_COLOR = Color(0.22f, 0.24f, 0.28f, 1f)
        private val SLIDER_KNOB_COLOR = Color(0.85f, 0.85f, 0.9f, 1f)
        private val VALUE_TEXT_COLOR = Color(0.95f, 0.95f, 0.6f, 1f)
        private val DEFAULT_VALUE_TEXT_COLOR = Color(0.6f, 0.6f, 0.6f, 1f)
    }

    private val viewport = FitViewport(WORLD_WIDTH, WORLD_HEIGHT)
    val stage = Stage(viewport)
    private val managedTextures = mutableListOf<Texture>()
    private val managedFonts = mutableListOf<BitmapFont>()
    private var visible = false

    /** Value labels for each constant, updated when sliders change. */
    private val valueLabels = mutableMapOf<String, Label>()

    /** The master toggle button reference for visual updates. */
    private lateinit var masterToggleLabel: Label
    private lateinit var masterToggleBg: Image

    /** Textures for toggle states, created once. */
    private lateinit var toggleOnTexture: Texture
    private lateinit var toggleOffTexture: Texture

    init {
        buildUI()
    }

    private fun buildUI() {
        // Full-screen tap catcher behind the panel — consumes taps outside panel
        val tapCatcherTexture = createSolidTexture(1, 1, Color(0f, 0f, 0f, 0.3f))
        val tapCatcher = Image(TextureRegionDrawable(TextureRegion(tapCatcherTexture))).apply {
            setSize(WORLD_WIDTH, WORLD_HEIGHT)
            setPosition(0f, 0f)
        }
        tapCatcher.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                return true // consume taps on dim area
            }
        })
        stage.addActor(tapCatcher)

        // Main panel table, positioned at the top of the screen
        val panelY = WORLD_HEIGHT - PANEL_HEIGHT - PANEL_TOP_MARGIN
        val panelBgTexture = createSolidTexture(WORLD_WIDTH.toInt(), PANEL_HEIGHT.toInt(), PANEL_BG_COLOR)
        val panelBg = Image(TextureRegionDrawable(TextureRegion(panelBgTexture))).apply {
            setSize(WORLD_WIDTH, PANEL_HEIGHT)
            setPosition(0f, panelY)
        }
        panelBg.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                return true // consume taps inside panel
            }
        })
        stage.addActor(panelBg)

        // --- Header row ---
        val headerFont = BitmapFont()
        managedFonts.add(headerFont)

        val headerBgTexture = createSolidTexture(WORLD_WIDTH.toInt(), HEADER_HEIGHT.toInt(), HEADER_BG_COLOR)
        val headerBg = Image(TextureRegionDrawable(TextureRegion(headerBgTexture))).apply {
            setSize(WORLD_WIDTH, HEADER_HEIGHT)
            setPosition(0f, panelY + PANEL_HEIGHT - HEADER_HEIGHT)
        }
        stage.addActor(headerBg)

        // Title label
        val titleStyle = Label.LabelStyle(headerFont, Color.WHITE)
        val titleLabel = Label("PHYSICS DEBUG", titleStyle).apply {
            setFontScale(HEADER_FONT_SCALE)
            setAlignment(Align.left)
        }
        titleLabel.setPosition(20f, panelY + PANEL_HEIGHT - HEADER_HEIGHT + 15f)
        stage.addActor(titleLabel)

        // Master toggle background + label
        toggleOnTexture = createSolidTexture(180, 40, TOGGLE_ON_COLOR)
        toggleOffTexture = createSolidTexture(180, 40, TOGGLE_OFF_COLOR)

        val toggleDrawable = if (DebugOverrides.enabled) {
            TextureRegionDrawable(TextureRegion(toggleOnTexture))
        } else {
            TextureRegionDrawable(TextureRegion(toggleOffTexture))
        }

        masterToggleBg = Image(toggleDrawable).apply {
            setSize(180f, 40f)
            setPosition(300f, panelY + PANEL_HEIGHT - HEADER_HEIGHT + 10f)
        }

        val toggleLabelStyle = Label.LabelStyle(headerFont, Color.WHITE)
        masterToggleLabel = Label(
            if (DebugOverrides.enabled) "OVERRIDES: ON" else "OVERRIDES: OFF",
            toggleLabelStyle
        ).apply {
            setFontScale(1.5f)
            setAlignment(Align.center)
            setSize(180f, 40f)
            setPosition(300f, panelY + PANEL_HEIGHT - HEADER_HEIGHT + 10f)
        }

        masterToggleBg.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                toggleMasterOverride()
                return true
            }
        })
        masterToggleLabel.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                toggleMasterOverride()
                return true
            }
        })

        stage.addActor(masterToggleBg)
        stage.addActor(masterToggleLabel)

        // Reset All button
        val resetFont = BitmapFont()
        managedFonts.add(resetFont)
        val resetBgTexture = createSolidTexture(140, 40, Color(0.6f, 0.25f, 0.15f, 0.9f))
        val resetBg = Image(TextureRegionDrawable(TextureRegion(resetBgTexture))).apply {
            setSize(140f, 40f)
            setPosition(520f, panelY + PANEL_HEIGHT - HEADER_HEIGHT + 10f)
        }
        val resetLabelStyle = Label.LabelStyle(resetFont, Color.WHITE)
        val resetLabel = Label("RESET ALL", resetLabelStyle).apply {
            setFontScale(1.3f)
            setAlignment(Align.center)
            setSize(140f, 40f)
            setPosition(520f, panelY + PANEL_HEIGHT - HEADER_HEIGHT + 10f)
        }
        resetBg.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                resetAllToDefaults()
                return true
            }
        })
        resetLabel.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                resetAllToDefaults()
                return true
            }
        })
        stage.addActor(resetBg)
        stage.addActor(resetLabel)

        // Close button (X) at top-right of header
        val closeFont = BitmapFont()
        managedFonts.add(closeFont)
        val closeBgTexture = createSolidTexture(
            CLOSE_BUTTON_SIZE.toInt(), CLOSE_BUTTON_SIZE.toInt(),
            Color(0.6f, 0.15f, 0.15f, 0.9f)
        )
        val closeButton = Image(TextureRegionDrawable(TextureRegion(closeBgTexture))).apply {
            setSize(CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
            setPosition(
                WORLD_WIDTH - CLOSE_BUTTON_SIZE - 10f,
                panelY + PANEL_HEIGHT - HEADER_HEIGHT + 6f
            )
        }
        closeButton.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                hide()
                return true
            }
        })
        stage.addActor(closeButton)

        val closeLabelStyle = Label.LabelStyle(closeFont, Color.WHITE)
        val closeLabel = Label("X", closeLabelStyle).apply {
            setFontScale(2f)
            setAlignment(Align.center)
            setSize(CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
            setPosition(
                WORLD_WIDTH - CLOSE_BUTTON_SIZE - 10f,
                panelY + PANEL_HEIGHT - HEADER_HEIGHT + 6f
            )
            touchable = com.badlogic.gdx.scenes.scene2d.Touchable.disabled
        }
        stage.addActor(closeLabel)

        // --- Scrollable slider columns ---
        val sliderAreaY = panelY
        val sliderAreaHeight = PANEL_HEIGHT - HEADER_HEIGHT - 10f

        // Build the inner table containing all slider columns
        val columnsTable = Table()

        val sliderFont = BitmapFont()
        managedFonts.add(sliderFont)

        // Create slider style textures once
        val sliderBgTex = createSolidTexture(12, SLIDER_HEIGHT.toInt(), SLIDER_BG_COLOR)
        val sliderKnobTex = createSolidTexture(28, 16, SLIDER_KNOB_COLOR)

        val sliderStyle = Slider.SliderStyle().apply {
            background = TextureRegionDrawable(TextureRegion(sliderBgTex))
            knob = TextureRegionDrawable(TextureRegion(sliderKnobTex))
        }

        for (meta in DebugOverrides.allConstants) {
            val columnBgTex = createSolidTexture(
                SLIDER_COLUMN_WIDTH.toInt(), sliderAreaHeight.toInt(), COLUMN_BG_COLOR
            )

            val column = Table()
            column.background = TextureRegionDrawable(TextureRegion(columnBgTex))

            // Constant name label
            val nameLabelStyle = Label.LabelStyle(sliderFont, Color.LIGHT_GRAY)
            val nameLabel = Label(meta.name, nameLabelStyle).apply {
                setFontScale(NAME_LABEL_FONT_SCALE)
                setAlignment(Align.center)
                wrap = true
            }
            column.add(nameLabel).width(SLIDER_COLUMN_WIDTH - 8f).padTop(4f)
            column.row()

            // Current value label
            val currentValue = meta.getter() ?: meta.defaultValue
            val valueLabelStyle = Label.LabelStyle(sliderFont, VALUE_TEXT_COLOR)
            val valueLabel = Label(formatValue(currentValue, meta), valueLabelStyle).apply {
                setFontScale(VALUE_LABEL_FONT_SCALE)
                setAlignment(Align.center)
            }
            valueLabels[meta.name] = valueLabel
            column.add(valueLabel).width(SLIDER_COLUMN_WIDTH - 8f).padTop(2f)
            column.row()

            // Default value reference label
            val defaultLabelStyle = Label.LabelStyle(sliderFont, DEFAULT_VALUE_TEXT_COLOR)
            val defaultLabel = Label("def: ${formatValue(meta.defaultValue, meta)}", defaultLabelStyle).apply {
                setFontScale(0.9f)
                setAlignment(Align.center)
            }
            column.add(defaultLabel).width(SLIDER_COLUMN_WIDTH - 8f).padTop(1f)
            column.row()

            // Vertical slider
            val slider = Slider(meta.min, meta.max, computeStepSize(meta), true, sliderStyle)
            slider.value = meta.getter() ?: meta.defaultValue

            slider.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    val newValue = slider.value
                    meta.setter(newValue)
                    valueLabel.setText(formatValue(newValue, meta))
                    valueLabel.style.fontColor = VALUE_TEXT_COLOR
                }
            })

            column.add(slider).width(28f).height(SLIDER_HEIGHT).padTop(4f).padBottom(4f)
            column.row()

            // Min/Max range labels
            val maxRangeLabel = Label(formatValue(meta.max, meta), defaultLabelStyle).apply {
                setFontScale(0.8f)
                setAlignment(Align.center)
            }
            column.add(maxRangeLabel).width(SLIDER_COLUMN_WIDTH - 8f)
            column.row()
            val minRangeLabel = Label(formatValue(meta.min, meta), defaultLabelStyle).apply {
                setFontScale(0.8f)
                setAlignment(Align.center)
            }
            column.add(minRangeLabel).width(SLIDER_COLUMN_WIDTH - 8f).padBottom(4f)
            column.row()

            columnsTable.add(column)
                .width(SLIDER_COLUMN_WIDTH)
                .height(sliderAreaHeight)
                .padRight(COLUMN_PAD)
        }

        // Wrap columns in a horizontal ScrollPane
        val scrollPane = ScrollPane(columnsTable).apply {
            setScrollingDisabled(false, true) // horizontal scroll only
            setOverscroll(false, false)
            setScrollBarPositions(true, false) // bottom scrollbar
            setFadeScrollBars(false)
        }

        // Container table for the scroll pane, positioned in the panel body
        val scrollContainer = Table().apply {
            setSize(WORLD_WIDTH, sliderAreaHeight)
            setPosition(0f, sliderAreaY)
        }
        scrollContainer.add(scrollPane).expand().fill()

        stage.addActor(scrollContainer)
    }

    /**
     * Compute an appropriate slider step size based on the constant's range.
     * Small ranges (like 0-1) get fine steps; large ranges get coarser steps.
     */
    private fun computeStepSize(meta: DebugOverrides.ConstantMeta): Float {
        val range = meta.max - meta.min
        return when {
            range <= 0.05f -> 0.0001f
            range <= 1f -> 0.01f
            range <= 10f -> 0.1f
            range <= 100f -> 1f
            range <= 1000f -> 5f
            else -> 10f
        }
    }

    /**
     * Format a float value for display. Uses appropriate decimal places
     * based on the constant's range to avoid overly long numbers.
     */
    private fun formatValue(value: Float, meta: DebugOverrides.ConstantMeta): String {
        val range = meta.max - meta.min
        return when {
            range <= 0.05f -> String.format("%.4f", value)
            range <= 1f -> String.format("%.3f", value)
            range <= 10f -> String.format("%.2f", value)
            range <= 100f -> String.format("%.1f", value)
            else -> String.format("%.0f", value)
        }
    }

    /** Toggle the master override switch and update UI accordingly. */
    private fun toggleMasterOverride() {
        DebugOverrides.enabled = !DebugOverrides.enabled
        updateMasterToggleVisual()
    }

    /** Update the master toggle button appearance to match current state. */
    private fun updateMasterToggleVisual() {
        if (DebugOverrides.enabled) {
            masterToggleLabel.setText("OVERRIDES: ON")
            masterToggleBg.drawable = TextureRegionDrawable(TextureRegion(toggleOnTexture))
        } else {
            masterToggleLabel.setText("OVERRIDES: OFF")
            masterToggleBg.drawable = TextureRegionDrawable(TextureRegion(toggleOffTexture))
        }
    }

    /** Reset all override values to their defaults and update slider UI. */
    private fun resetAllToDefaults() {
        for (meta in DebugOverrides.allConstants) {
            meta.setter(null)
        }
        DebugOverrides.enabled = false
        updateMasterToggleVisual()

        // Update value labels to show default values with default styling
        for (meta in DebugOverrides.allConstants) {
            val label = valueLabels[meta.name] ?: continue
            label.setText(formatValue(meta.defaultValue, meta))
            label.style.fontColor = DEFAULT_VALUE_TEXT_COLOR
        }
    }

    /** Show the debug panel overlay and enable its input processing. */
    fun show() {
        visible = true
        stage.root.isVisible = true

        // Sync slider values with current overrides in case they changed
        updateMasterToggleVisual()
    }

    /** Hide the debug panel overlay and disable its input processing. */
    fun hide() {
        visible = false
        stage.root.isVisible = false
    }

    /** Returns true if the overlay is currently visible. */
    fun isVisible(): Boolean = visible

    /**
     * Render the overlay. Call this after drawing the game scene, HUD, and pause overlay.
     * Resets batch state after stage.draw() to prevent dirty color/blend
     * state from leaking to subsequent renderers.
     */
    fun render(delta: Float) {
        if (!visible) return
        stage.act(delta)
        stage.draw()

        // Reset batch state for subsequent renderers
        val batch = stage.batch
        batch.color = Color.WHITE
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    /** Resize the overlay viewport to match the screen dimensions. */
    fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    /** Dispose of all managed resources (fonts, textures, stage). */
    fun dispose() {
        stage.dispose()
        for (texture in managedTextures) {
            texture.dispose()
        }
        managedTextures.clear()
        for (font in managedFonts) {
            font.dispose()
        }
        managedFonts.clear()
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
