package com.streetsoccer.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.FitViewport
import com.streetsoccer.GameBootstrapper
import com.streetsoccer.input.InputRouter
import com.streetsoccer.physics.PhysicsContactListener
import com.streetsoccer.rendering.BackgroundRenderer
import com.streetsoccer.services.SessionAccumulator
import com.streetsoccer.state.GameState
import com.streetsoccer.state.GameStateListener
import com.streetsoccer.state.GameStateManager
import ktx.app.KtxScreen

/** Thin KtxScreen wrapper. Delegates ECS setup to [ECSBootstrapper] and per-frame work to [GameLoop]. */
class LevelScreen(private val game: GameBootstrapper) : KtxScreen {
    private val gsm = GameStateManager()
    private val session = SessionAccumulator()
    private val contactListener = PhysicsContactListener()
    private val world = World(Vector2(0f, 0f), true).apply { setContactListener(contactListener) }
    private val batch = SpriteBatch()
    private val viewport = FitViewport(1920f, 1080f)
    private val bgRenderer = BackgroundRenderer(1920f, 1080f)
    private val inputRouter = InputRouter(gsm)
    private var result: ECSBootstrapper.BootstrapResult? = null
    private var gameLoop: GameLoop? = null
    private var catcherTexture: Texture? = null

    private val pauseListener = object : GameStateListener {
        override fun onStateEnter(s: GameState) { if (s is GameState.Paused) result?.pauseOverlay?.show() }
        override fun onStateExit(s: GameState) { if (s is GameState.Paused) result?.pauseOverlay?.hide() }
    }
    private val backHandler = object : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                if (gsm.isPaused) { handleResume(); return true }
                if (gsm.isInGameplay) { gsm.pause(); return true }
            }
            return false
        }
    }

    override fun show() {
        bgRenderer.load()
        @Suppress("DEPRECATION") Gdx.input.setCatchBackKey(true)
        session.reset()
        val settings = game.saveService.loadSettings()
        inputRouter.sliderSide = settings.sliderSide
        val r = ECSBootstrapper(
            gsm, session, contactListener, world, batch, inputRouter,
            game.audioService, game.assets, settings.trajectoryPreviewEnabled
        ).bootstrap(game.levelData, ::handleResume, ::handleQuit, pauseListener, backHandler)
        result = r
        catcherTexture = r.catcherTexture
        gameLoop = GameLoop(
            r.engine, world, batch, viewport, bgRenderer, gsm, inputRouter,
            r.physicsSystem, r.collisionSystem, r.hudSystem, r.inputSystem,
            r.trajectorySystem, r.pauseOverlay
        )
        Gdx.input.inputProcessor = r.inputMultiplexer
        gsm.transitionTo(GameState.Ready)
    }

    override fun render(delta: Float) { gameLoop?.update(delta) }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        result?.hudSystem?.resize(width, height)
        result?.pauseOverlay?.resize(width, height)
    }

    override fun hide() {
        mergeSessionAndSave()
        Gdx.input.inputProcessor = null
        gsm.removeListener(pauseListener)
    }

    override fun pause() { mergeSessionAndSave() }

    override fun dispose() {
        bgRenderer.dispose()
        result?.run { renderSystem.dispose(); hudSystem.dispose(); trajectorySystem.dispose(); pauseOverlay.dispose() }
        catcherTexture?.dispose()
        batch.dispose()
        world.dispose()
    }

    private fun handleResume() { result?.pauseOverlay?.hide(); gsm.resume() }

    private fun handleQuit() {
        result?.pauseOverlay?.hide()
        gsm.quit()
        game.setScreen<AttractScreen>()
    }

    private fun mergeSessionAndSave() {
        try {
            game.profile = session.mergeInto(game.profile)
            game.saveService.saveProfile(game.profile)
            if (Gdx.app.logLevel >= Application.LOG_INFO) {
                Gdx.app.log(
                    "LevelScreen",
                    "Session merged: sessionScore=${session.sessionScore}, " +
                        "careerTotal=${game.profile.career.totalScore}"
                )
            }
            session.reset()
        } catch (e: Exception) {
            if (Gdx.app.logLevel >= Application.LOG_INFO) {
                Gdx.app.log("LevelScreen", "Failed to merge/save: ${e.message}")
            }
        }
    }
}
