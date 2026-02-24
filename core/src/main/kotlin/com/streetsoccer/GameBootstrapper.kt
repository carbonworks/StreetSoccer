package com.streetsoccer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.streetsoccer.screens.AttractScreen
import com.streetsoccer.screens.LevelScreen
import com.streetsoccer.screens.LoadingScreen
import com.streetsoccer.services.AudioService
import com.streetsoccer.services.NoopAudioService
import com.streetsoccer.services.ProfileData
import com.streetsoccer.services.SaveService
import com.streetsoccer.services.SettingsData
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync

class GameBootstrapper : KtxGame<KtxScreen>() {

    lateinit var saveService: SaveService
        private set
    lateinit var audioService: AudioService
        private set
    lateinit var assets: AssetManager
        private set

    /** In-memory profile loaded at startup; updated on session end and save triggers. */
    var profile: ProfileData = ProfileData()
        internal set

    /** In-memory settings loaded at startup; updated when settings change. */
    var settings: SettingsData = SettingsData()
        internal set

    override fun create() {
        // Initialize coroutine context (must be called once at startup)
        KtxAsync.initiate()

        // Create shared services
        saveService = SaveService()
        audioService = NoopAudioService()
        assets = AssetManager()

        // Load persisted data (gracefully falls back to defaults on missing/corrupt files)
        profile = saveService.loadProfile()
        settings = saveService.loadSettings()
        Gdx.app.log("GameBootstrapper", "Profile loaded: totalKicks=${profile.career.totalKicks}, totalScore=${profile.career.totalScore}")
        Gdx.app.log("GameBootstrapper", "Settings loaded: trajectoryPreview=${settings.trajectoryPreviewEnabled}, sliderSide=${settings.sliderSide}")

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

    /**
     * Called when the app is backgrounded (Android onPause).
     * Flush both profile and settings to disk as a safety net per
     * save-and-persistence.md Section 6 — Android may kill the app
     * after onPause without calling onDestroy.
     */
    override fun pause() {
        super.pause()
        flushSave()
    }

    override fun dispose() {
        // Final save before shutdown
        flushSave()
        super.dispose()
        assets.dispose()
    }

    /**
     * Persist current in-memory profile and settings to disk.
     * Called on pause (backgrounding) and dispose (shutdown).
     */
    private fun flushSave() {
        try {
            saveService.saveProfile(profile)
            saveService.saveSettings(settings)
            Gdx.app?.log("GameBootstrapper", "Save flushed: totalKicks=${profile.career.totalKicks}, totalScore=${profile.career.totalScore}")
        } catch (e: Exception) {
            Gdx.app?.log("GameBootstrapper", "Failed to flush save: ${e.message}")
        }
    }
}
