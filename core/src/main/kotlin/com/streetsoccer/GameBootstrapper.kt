package com.streetsoccer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.streetsoccer.screens.AttractScreen
import com.streetsoccer.screens.LevelScreen
import com.streetsoccer.screens.LoadingScreen
import com.streetsoccer.services.AudioService
import com.streetsoccer.services.NoopAudioService
import com.streetsoccer.services.SaveService
import io.github.libktx.app.KtxGame
import io.github.libktx.app.KtxScreen
import ktx.async.KtxAsync

class GameBootstrapper : KtxGame<KtxScreen>() {

    lateinit var saveService: SaveService
        private set
    lateinit var audioService: AudioService
        private set
    lateinit var assets: AssetManager
        private set

    override fun create() {
        // Initialize coroutine context (must be called once at startup)
        KtxAsync.initiate()

        // Create shared services
        saveService = SaveService()
        audioService = NoopAudioService()
        assets = AssetManager()

        addScreen(LoadingScreen(this))
        addScreen(AttractScreen(this))
        addScreen(LevelScreen(this))

        setScreen<LoadingScreen>()
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        super.render()
    }

    override fun dispose() {
        super.dispose()
        assets.dispose()
    }
}
