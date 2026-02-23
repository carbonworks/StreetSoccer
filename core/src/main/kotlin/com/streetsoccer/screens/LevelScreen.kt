package com.streetsoccer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.streetsoccer.GameBootstrapper
import io.github.libktx.app.KtxScreen

class LevelScreen(private val game: GameBootstrapper) : KtxScreen {

    override fun show() {
        Gdx.app.log("LevelScreen", "show")
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.3f, 0.3f, 0.3f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        // Main game logic loop will go here
    }
}
