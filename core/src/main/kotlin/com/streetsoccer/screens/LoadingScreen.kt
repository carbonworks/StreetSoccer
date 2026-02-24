package com.streetsoccer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.streetsoccer.GameBootstrapper
import ktx.app.KtxScreen

/**
 * Handles asset loading via LibGDX AssetManager, shows a progress bar,
 * parses level data, and auto-advances to AttractScreen when complete.
 */
class LoadingScreen(private val game: GameBootstrapper) : KtxScreen {

    private lateinit var shapeRenderer: ShapeRenderer
    private var levelData: JsonValue? = null
    private var loadingComplete = false

    // Progress bar layout (centered, fixed pixel sizes for 1920x1080 target)
    private val barWidth = 600f
    private val barHeight = 24f
    private val barBorderWidth = 2f

    override fun show() {
        Gdx.app.log("LoadingScreen", "show — beginning asset load")
        shapeRenderer = ShapeRenderer()

        queueAssets()
    }

    /**
     * Queue all game assets into the shared AssetManager.
     *
     * Currently queues background.jpg (the level background texture).
     * SVG sprites are not loaded yet — AmanithSVG integration is pending.
     * When new texture assets are added to assets/, queue them here.
     */
    private fun queueAssets() {
        val assets = game.assets

        // Queue the level background image.
        // This file must exist in the assets/ directory to be loadable.
        if (Gdx.files.internal("background.jpg").exists()) {
            assets.load("background.jpg", Texture::class.java)
        } else {
            Gdx.app.log("LoadingScreen", "background.jpg not found in assets — skipping")
        }

        // Future: queue additional textures, atlases, sounds here.
        // Example: assets.load("sprites/ball.png", Texture::class.java)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (!loadingComplete) {
            // Drive the AssetManager — returns true when all queued assets are loaded.
            val done = game.assets.update()
            val progress = game.assets.progress

            drawProgressBar(progress)

            if (done) {
                loadingComplete = true
                parseLevelData()
                Gdx.app.log("LoadingScreen", "All assets loaded — advancing to AttractScreen")
                game.setScreen<AttractScreen>()
            }
        }
    }

    /**
     * Draw a simple centered progress bar using ShapeRenderer.
     * No texture dependencies — works even before any assets are loaded.
     */
    private fun drawProgressBar(progress: Float) {
        val screenW = Gdx.graphics.width.toFloat()
        val screenH = Gdx.graphics.height.toFloat()
        val barX = (screenW - barWidth) / 2f
        val barY = (screenH - barHeight) / 2f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Background (dark grey)
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f)
        shapeRenderer.rect(barX - barBorderWidth, barY - barBorderWidth,
            barWidth + barBorderWidth * 2f, barHeight + barBorderWidth * 2f)

        // Fill (accent blue, width proportional to progress)
        shapeRenderer.setColor(0.2f, 0.6f, 1.0f, 1f)
        shapeRenderer.rect(barX, barY, barWidth * progress, barHeight)

        shapeRenderer.end()
    }

    /**
     * Parse suburban-crossroads.json and store the result so other systems
     * can access the level data. The actual entity creation from this data
     * is handled by LevelLoader when LevelScreen initializes.
     */
    private fun parseLevelData() {
        val levelFile = Gdx.files.internal("suburban-crossroads.json")
        if (levelFile.exists()) {
            levelData = JsonReader().parse(levelFile)
            Gdx.app.log("LoadingScreen", "Level data parsed: ${levelData?.get("level_meta")?.getString("id")}")
        } else {
            Gdx.app.log("LoadingScreen", "suburban-crossroads.json not found — level data unavailable")
        }
    }

    /** Retrieve parsed level data. Called by LevelScreen or GameBootstrapper after loading. */
    fun getLevelData(): JsonValue? = levelData

    override fun hide() {
        // Screen is being switched away from — no cleanup needed yet.
    }

    override fun dispose() {
        if (::shapeRenderer.isInitialized) {
            shapeRenderer.dispose()
        }
    }
}
