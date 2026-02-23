package com.streetsoccer

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.streetsoccer.screens.AttractScreen
import com.streetsoccer.screens.LevelScreen
import com.streetsoccer.screens.LoadingScreen
import io.github.libktx.app.KtxGame
import io.github.libktx.app.KtxScreen

class GameBootstrapper : KtxGame<KtxScreen>() {
    override fun create() {
        addScreen(LoadingScreen(this))
        addScreen(AttractScreen(this))
        addScreen(LevelScreen(this))

        setScreen<LoadingScreen>()
    }

    override fun render() {
        // Clear screen here before drawing the active screen?
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        super.render()
    }
}
