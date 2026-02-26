package com.streetsoccer.services

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import kotlin.math.max

// ---------------------------------------------------------------------------
// Domain Objects (from save-and-persistence.md Section 3)
// ---------------------------------------------------------------------------

@Serializable
data class CareerStats(
    val totalKicks: Long = 0L,
    val totalHits: Long = 0L,
    val totalScore: Long = 0L,
    val bestSessionScore: Long = 0L,
    val bestStreak: Int = 0,
    val longestBigBombDistance: Float = 0f,
    val targetsByType: Map<String, Long> = emptyMap()
)

@Serializable
data class CosmeticState(
    val unlockedBallSkins: Set<String> = setOf("classic_white"),
    val unlockedImpactEffects: Set<String> = setOf("default_shatter"),
    val unlockedTrailEffects: Set<String> = setOf("none"),
    val selectedBallSkin: String = "classic_white",
    val selectedImpactEffect: String = "default_shatter",
    val selectedTrailEffect: String = "none"
)

@Serializable
data class VariantState(
    val unlockedVariants: Set<String> = setOf("suburban_crossroads"),
    val selectedVariant: String = "suburban_crossroads"
)

@Serializable
data class ProfileData(
    val version: Int = CURRENT_PROFILE_VERSION,
    val career: CareerStats = CareerStats(),
    val cosmetics: CosmeticState = CosmeticState(),
    val variants: VariantState = VariantState(),
    val dismissedTips: Set<String> = emptySet()
) {
    companion object {
        const val CURRENT_PROFILE_VERSION = 1
    }
}

@Serializable
data class SettingsData(
    val version: Int = CURRENT_SETTINGS_VERSION,
    val trajectoryPreviewEnabled: Boolean = false,
    val sliderSide: String = "left",
    val masterVolume: Float = 1.0f,
    val sfxVolume: Float = 1.0f
) {
    companion object {
        const val CURRENT_SETTINGS_VERSION = 1
    }
}

// ---------------------------------------------------------------------------
// Session Accumulator (from save-and-persistence.md Section 5)
// ---------------------------------------------------------------------------

/**
 * Mutable session-scoped values that accumulate in memory during gameplay.
 * Merged into [CareerStats] at session end via [SessionAccumulator.mergeInto].
 */
class SessionAccumulator {
    var sessionScore: Long = 0L
    var currentStreak: Int = 0
    var sessionKicks: Long = 0L
    var sessionHits: Long = 0L
    var peakStreakThisSession: Int = 0
    val sessionTargetsByType: MutableMap<String, Long> = mutableMapOf()

    /** Record a kick. */
    fun recordKick() {
        sessionKicks++
    }

    /** Record a hit on a target, updating streak and per-type counts. */
    fun recordHit(targetType: String, scoreValue: Long) {
        sessionHits++
        sessionScore += scoreValue
        currentStreak++
        if (currentStreak > peakStreakThisSession) {
            peakStreakThisSession = currentStreak
        }
        sessionTargetsByType[targetType] = (sessionTargetsByType[targetType] ?: 0L) + 1L
    }

    /** Reset the current streak (e.g., on a miss). */
    fun breakStreak() {
        currentStreak = 0
    }

    /**
     * Merge session stats into career stats and return the updated [ProfileData].
     * This is the primary save-point logic from save-and-persistence.md Section 5.
     */
    fun mergeInto(profile: ProfileData): ProfileData {
        val career = profile.career
        val mergedTargets = career.targetsByType.toMutableMap()
        for ((type, count) in sessionTargetsByType) {
            mergedTargets[type] = (mergedTargets[type] ?: 0L) + count
        }
        val updatedCareer = career.copy(
            totalKicks = career.totalKicks + sessionKicks,
            totalHits = career.totalHits + sessionHits,
            totalScore = career.totalScore + sessionScore,
            bestSessionScore = max(career.bestSessionScore, sessionScore),
            bestStreak = max(career.bestStreak.toLong(), peakStreakThisSession.toLong()).toInt(),
            targetsByType = mergedTargets
        )
        return profile.copy(career = updatedCareer)
    }

    /** Reset all session counters for a new session. */
    fun reset() {
        sessionScore = 0L
        currentStreak = 0
        sessionKicks = 0L
        sessionHits = 0L
        peakStreakThisSession = 0
        sessionTargetsByType.clear()
    }
}

// ---------------------------------------------------------------------------
// Schema Migration (from save-and-persistence.md Section 7)
// ---------------------------------------------------------------------------

/**
 * Chained migration functions that transform raw JSON from older versions
 * to the current schema before deserialization.
 */
object SchemaMigration {

    /**
     * Migrate a profile JSON object through the version chain.
     * Each step transforms the [JsonObject] from version N to N+1.
     */
    @Suppress("UNUSED_VARIABLE")
    fun migrateProfile(json: JsonObject): JsonObject {
        var data = json
        val version = data["version"]?.jsonPrimitive?.int ?: 0

        // Chained migrations — add new steps here as the schema evolves:
        // if (version < 2) data = migrateProfileV1toV2(data)
        // if (version < 3) data = migrateProfileV2toV3(data)

        return data
    }

    /**
     * Migrate a settings JSON object through the version chain.
     */
    @Suppress("UNUSED_VARIABLE")
    fun migrateSettings(json: JsonObject): JsonObject {
        var data = json
        val version = data["version"]?.jsonPrimitive?.int ?: 0

        // Chained migrations — add new steps here as the schema evolves:
        // if (version < 2) data = migrateSettingsV1toV2(data)

        return data
    }
}

// ---------------------------------------------------------------------------
// SaveService (from save-and-persistence.md Sections 6-8)
// ---------------------------------------------------------------------------

/**
 * Manages all reads and writes to persistent storage.
 *
 * Important: [Gdx.files] is not available until after [com.badlogic.gdx.ApplicationListener.create].
 * File handles are lazily initialized to avoid accessing Gdx before it's ready.
 */
class SaveService {

    companion object {
        private const val PROFILE_FILE = "profile.json"
        private const val PROFILE_TMP_FILE = "profile.tmp.json"
        private const val PROFILE_CORRUPT_FILE = "profile.corrupt.json"

        private const val SETTINGS_FILE = "settings.json"
        private const val SETTINGS_TMP_FILE = "settings.tmp.json"
        private const val SETTINGS_CORRUPT_FILE = "settings.corrupt.json"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    // Lazy file handles — Gdx.files is not available at construction time
    private val profileFile: FileHandle by lazy { Gdx.files.local(PROFILE_FILE) }
    private val profileTmpFile: FileHandle by lazy { Gdx.files.local(PROFILE_TMP_FILE) }
    private val profileCorruptFile: FileHandle by lazy { Gdx.files.local(PROFILE_CORRUPT_FILE) }

    private val settingsFile: FileHandle by lazy { Gdx.files.local(SETTINGS_FILE) }
    private val settingsTmpFile: FileHandle by lazy { Gdx.files.local(SETTINGS_TMP_FILE) }
    private val settingsCorruptFile: FileHandle by lazy { Gdx.files.local(SETTINGS_CORRUPT_FILE) }

    // ------------------------------------------------------------------
    // Profile
    // ------------------------------------------------------------------

    /**
     * Load [ProfileData] from disk. Handles:
     * - Missing file -> defaults
     * - Startup recovery (tmp file exists but primary doesn't)
     * - Corrupt JSON -> backup to .corrupt.json, return defaults
     * - Schema migration for older versions
     */
    fun loadProfile(): ProfileData {
        recoverTmpFile(profileFile, profileTmpFile)
        if (!profileFile.exists()) return ProfileData()

        return try {
            val raw = profileFile.readString()
            val jsonElement = json.parseToJsonElement(raw).jsonObject
            val migrated = SchemaMigration.migrateProfile(jsonElement)
            json.decodeFromString<ProfileData>(migrated.toString())
        } catch (e: Exception) {
            handleCorruptFile(profileFile, profileCorruptFile, "profile")
            ProfileData()
        }
    }

    /**
     * Save [ProfileData] to disk using atomic write (temp file then rename).
     */
    fun saveProfile(data: ProfileData) {
        atomicWrite(profileFile, profileTmpFile, json.encodeToString(data))
    }

    // ------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------

    /**
     * Load [SettingsData] from disk with the same recovery/migration/error
     * handling as [loadProfile].
     */
    fun loadSettings(): SettingsData {
        recoverTmpFile(settingsFile, settingsTmpFile)
        if (!settingsFile.exists()) return SettingsData()

        return try {
            val raw = settingsFile.readString()
            val jsonElement = json.parseToJsonElement(raw).jsonObject
            val migrated = SchemaMigration.migrateSettings(jsonElement)
            json.decodeFromString<SettingsData>(migrated.toString())
        } catch (e: Exception) {
            handleCorruptFile(settingsFile, settingsCorruptFile, "settings")
            SettingsData()
        }
    }

    /**
     * Save [SettingsData] to disk using atomic write (temp file then rename).
     */
    fun saveSettings(data: SettingsData) {
        atomicWrite(settingsFile, settingsTmpFile, json.encodeToString(data))
    }

    // ------------------------------------------------------------------
    // Atomic Write (from save-and-persistence.md Section 8)
    // ------------------------------------------------------------------

    /**
     * Write [content] to [primary] atomically:
     * 1. Write to [tmp]
     * 2. Delete [primary]
     * 3. Rename [tmp] to [primary]
     *
     * This ensures the file is either fully written or not present — never
     * partially written.
     */
    private fun atomicWrite(primary: FileHandle, tmp: FileHandle, content: String) {
        tmp.writeString(content, false)
        if (primary.exists()) {
            primary.delete()
        }
        tmp.file().renameTo(primary.file())
    }

    // ------------------------------------------------------------------
    // Startup Recovery (from save-and-persistence.md Section 8)
    // ------------------------------------------------------------------

    /**
     * If the primary file is missing but a .tmp.json file exists, rename
     * the tmp file to the primary name. This recovers from the case where
     * the app was killed between deleting the original and renaming the tmp
     * during an atomic write.
     */
    private fun recoverTmpFile(primary: FileHandle, tmp: FileHandle) {
        if (!primary.exists() && tmp.exists()) {
            tmp.file().renameTo(primary.file())
        }
    }

    // ------------------------------------------------------------------
    // Corrupt File Handling (from save-and-persistence.md Section 8)
    // ------------------------------------------------------------------

    /**
     * When a file cannot be parsed:
     * 1. Rename the corrupt file to .corrupt.json (overwriting any previous
     *    corrupt backup — only the most recent is retained).
     * 2. Log a warning.
     */
    private fun handleCorruptFile(primary: FileHandle, corrupt: FileHandle, label: String) {
        try {
            if (corrupt.exists()) {
                corrupt.delete()
            }
            primary.file().renameTo(corrupt.file())
            if (Gdx.app?.logLevel ?: 0 >= Application.LOG_INFO) {
                Gdx.app?.log("SaveService", "Corrupt $label file backed up to ${corrupt.name()}")
            }
        } catch (e: Exception) {
            if (Gdx.app?.logLevel ?: 0 >= Application.LOG_INFO) {
                Gdx.app?.log("SaveService", "Failed to backup corrupt $label file: ${e.message}")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Audio Service — interface covering all ~12 sound cues from GDD Section 9
// ---------------------------------------------------------------------------

interface AudioService {
    // --- Impact sounds (target hits) ---
    fun playGlassBreak()
    fun playMetallicClang()
    fun playCarAlarm()
    fun playElectronicFizz()    // Drone hit

    // --- Kick & flight ---
    fun playKickLaunch()        // Bass boom on launch
    fun playBigBombActivation() // Big Bomb activation emphasis
    fun playWhoosh()            // Flight / steer swipe whoosh

    // --- Miss / bounce ---
    fun playBounce()            // Wall / fence / obstacle hit
    fun playMiss()              // Out-of-bounds or silent miss

    // --- Streak & scoring ---
    fun playStreakChime()        // Streak milestone chime
    fun playScorePopup()         // Score popup sound

    // --- UI ---
    fun playUiTap()              // Menu / HUD tap

    // --- Volume control ---
    fun updateVolume(masterVolume: Float, sfxVolume: Float)

    // --- Lifecycle ---
    fun dispose()
}

class NoopAudioService : AudioService {
    override fun playGlassBreak() {}
    override fun playMetallicClang() {}
    override fun playCarAlarm() {}
    override fun playElectronicFizz() {}
    override fun playKickLaunch() {}
    override fun playBigBombActivation() {}
    override fun playWhoosh() {}
    override fun playBounce() {}
    override fun playMiss() {}
    override fun playStreakChime() {}
    override fun playScorePopup() {}
    override fun playUiTap() {}
    override fun updateVolume(masterVolume: Float, sfxVolume: Float) {}
    override fun dispose() {}
}
