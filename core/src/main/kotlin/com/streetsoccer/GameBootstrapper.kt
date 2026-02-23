package com.streetsoccer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.streetsoccer.screens.AttractScreen
import com.streetsoccer.screens.LevelScreen
import com.streetsoccer.screens.LoadingScreen
import io.github.libktx.app.KtxGame
import io.github.libktx.app.KtxScreen

class GameBootstrapper : KtxGame<KtxScreen>() {

    /** Shared AssetManager — initialized during create(), available to all screens. */
    lateinit var assets: AssetManager
        private set

    override fun create() {
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
