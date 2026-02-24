package com.streetsoccer.services

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import kotlin.math.min

/**
 * Production [AudioService] implementation that loads WAV sound assets and plays
 * them at the volume level dictated by [SettingsData.masterVolume] and
 * [SettingsData.sfxVolume].
 *
 * Sound assets are loaded lazily from `sounds/` in the assets directory. If a
 * sound file is missing, playback is silently skipped (logged once) so the game
 * never crashes due to a missing audio asset. This makes the class safe to use
 * with placeholder or incomplete asset sets.
 *
 * ## Asset replacement
 *
 * To replace placeholder sounds with real ones, drop WAV (or OGG) files into
 * `assets/sounds/` using the filenames listed in [SoundCue]. No code changes
 * required --- the service picks them up automatically on next launch.
 *
 * @param initialMasterVolume master volume from [SettingsData.masterVolume] (0..1)
 * @param initialSfxVolume    SFX volume from [SettingsData.sfxVolume] (0..1)
 */
class AudioServiceImpl(
    initialMasterVolume: Float = 1.0f,
    initialSfxVolume: Float = 1.0f
) : AudioService {

    companion object {
        private const val TAG = "AudioServiceImpl"

        /** Base directory inside assets for all sound files. */
        private const val SOUNDS_DIR = "sounds"
    }

    // ---- Volume state ----

    private var masterVolume: Float = initialMasterVolume.coerceIn(0f, 1f)
    private var sfxVolume: Float = initialSfxVolume.coerceIn(0f, 1f)

    /** Effective volume = master * sfx, clamped to [0, 1]. */
    private val effectiveVolume: Float
        get() = min(1f, masterVolume * sfxVolume)

    // ---- Sound cue registry ----

    /**
     * Enumerates every sound cue the service can play, mapping it to a filename
     * under [SOUNDS_DIR]. The actual [Sound] handle is loaded lazily on first
     * play to avoid blocking startup.
     */
    private enum class SoundCue(val filename: String) {
        GLASS_BREAK("glass_break.wav"),
        METALLIC_CLANG("metallic_clang.wav"),
        CAR_ALARM("car_alarm.wav"),
        ELECTRONIC_FIZZ("electronic_fizz.wav"),
        KICK_LAUNCH("kick_launch.wav"),
        BIG_BOMB_ACTIVATION("big_bomb_activation.wav"),
        WHOOSH("whoosh.wav"),
        BOUNCE("bounce.wav"),
        MISS("miss.wav"),
        STREAK_CHIME("streak_chime.wav"),
        SCORE_POPUP("score_popup.wav"),
        UI_TAP("ui_tap.wav"),
    }

    /**
     * Lazy-loaded sound handles. A `null` value means the asset was not found
     * (or failed to load). Once set to `null` on failure, we never retry so we
     * don't spam the log.
     */
    private val sounds = mutableMapOf<SoundCue, Sound?>()

    /** Tracks which cues we have already warned about (missing asset). */
    private val warnedMissing = mutableSetOf<SoundCue>()

    // ---- Internal helpers ----

    /**
     * Lazily load and return the [Sound] for [cue], or `null` if unavailable.
     */
    private fun getSound(cue: SoundCue): Sound? {
        if (cue in sounds) return sounds[cue]

        val path = "$SOUNDS_DIR/${cue.filename}"
        return try {
            val fileHandle = Gdx.files.internal(path)
            if (!fileHandle.exists()) {
                if (cue !in warnedMissing) {
                    Gdx.app?.log(TAG, "Sound asset not found: $path (playback will be skipped)")
                    warnedMissing.add(cue)
                }
                sounds[cue] = null
                null
            } else {
                val sound = Gdx.audio.newSound(fileHandle)
                sounds[cue] = sound
                Gdx.app?.log(TAG, "Loaded sound: $path")
                sound
            }
        } catch (e: Exception) {
            if (cue !in warnedMissing) {
                Gdx.app?.log(TAG, "Failed to load sound $path: ${e.message}")
                warnedMissing.add(cue)
            }
            sounds[cue] = null
            null
        }
    }

    /**
     * Play a sound cue at the current effective volume. Gracefully no-ops if
     * the sound asset is unavailable or the effective volume is zero.
     */
    private fun play(cue: SoundCue) {
        val vol = effectiveVolume
        if (vol <= 0f) return
        getSound(cue)?.play(vol)
    }

    // ---- AudioService implementation ----

    // -- Impact sounds (target hits) --

    override fun playGlassBreak() = play(SoundCue.GLASS_BREAK)

    override fun playMetallicClang() = play(SoundCue.METALLIC_CLANG)

    override fun playCarAlarm() = play(SoundCue.CAR_ALARM)

    override fun playElectronicFizz() = play(SoundCue.ELECTRONIC_FIZZ)

    // -- Kick & flight --

    override fun playKickLaunch() = play(SoundCue.KICK_LAUNCH)

    override fun playBigBombActivation() = play(SoundCue.BIG_BOMB_ACTIVATION)

    override fun playWhoosh() = play(SoundCue.WHOOSH)

    // -- Miss / bounce --

    override fun playBounce() = play(SoundCue.BOUNCE)

    override fun playMiss() = play(SoundCue.MISS)

    // -- Streak & scoring --

    override fun playStreakChime() = play(SoundCue.STREAK_CHIME)

    override fun playScorePopup() = play(SoundCue.SCORE_POPUP)

    // -- UI --

    override fun playUiTap() = play(SoundCue.UI_TAP)

    // -- Volume control --

    override fun updateVolume(masterVolume: Float, sfxVolume: Float) {
        this.masterVolume = masterVolume.coerceIn(0f, 1f)
        this.sfxVolume = sfxVolume.coerceIn(0f, 1f)
    }

    // -- Lifecycle --

    override fun dispose() {
        for ((_, sound) in sounds) {
            sound?.dispose()
        }
        sounds.clear()
        warnedMissing.clear()
    }
}
