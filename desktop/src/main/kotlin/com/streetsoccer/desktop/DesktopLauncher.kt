package com.streetsoccer.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.streetsoccer.GameBootstrapper

object DesktopLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Street Soccer")
            setWindowedMode(1920, 1080)
            useVsync(true)
            setForegroundFPS(60)
        }
        Lwjgl3Application(GameBootstrapper(), config)
    }
}
