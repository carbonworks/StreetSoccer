package com.streetsoccer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.streetsoccer.GameBootstrapper
import io.github.libktx.app.KtxScreen

class AttractScreen(private val game: GameBootstrapper) : KtxScreen {

    override fun show() {
        Gdx.app.log("AttractScreen", "show")
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (Gdx.input.justTouched()) {
            game.setScreen<LevelScreen>()
        }
    }
}
