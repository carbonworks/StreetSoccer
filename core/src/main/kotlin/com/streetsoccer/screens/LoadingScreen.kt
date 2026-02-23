package com.streetsoccer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.streetsoccer.GameBootstrapper
import io.github.libktx.app.KtxScreen

class LoadingScreen(private val game: GameBootstrapper) : KtxScreen {

    override fun show() {
        Gdx.app.log("LoadingScreen", "show")
        // Load initial assets here
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        // Pseudo logic to transition for now
        if (Gdx.input.justTouched()) {
            game.setScreen<AttractScreen>()
        }
    }
}
