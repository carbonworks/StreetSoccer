**This document defines the save and persistence model for Street Soccer.** It covers what data is stored, where it lives on disk, when it is written, and how the schema evolves. For the stats and progression concepts that this system persists, see `game-design-document.md` Section 7. For the game states that trigger loads and saves, see `state-machine.md`.

---

## 1. Storage Approach

All persistent data is stored as **JSON files on Android internal storage** using LibGDX's `Gdx.files.local()` API, which maps to the app's private internal directory. Encoding and decoding use `kotlinx.serialization`.

### Why JSON + Internal Storage

| Consideration | Decision |
|---------------|----------|
| **Data volume** | Less than 10 KB total — a handful of counters, IDs, and preference flags |
| **Access pattern** | Read once on launch, write on session end and settings change — no mid-gameplay I/O |
| **Readability** | JSON is human-readable, simplifying debugging and manual inspection during development |
| **Portability** | `Gdx.files.local()` works identically on Android and Desktop (debug builds), with no platform-specific code |

### Why Not Alternatives

| Alternative | Reason to Skip |
|-------------|----------------|
| **SharedPreferences** | Flat key-value store with no structure — awkward for nested objects like `CareerStats` and cosmetic inventories. No schema versioning. |
| **SQLite / Room** | Relational database is overkill for <10 KB of flat data with no queries, joins, or concurrent access. Adds a dependency and migration framework the project doesn't need yet. |
| **DataStore (Jetpack)** | Android-only, no LibGDX integration, and Proto DataStore requires `.proto` schema definitions — unnecessary complexity. |

The JSON approach is deliberately lightweight for alpha. Section 10 describes the migration path to Room or cloud sync if the project outgrows flat files.

---

## 2. File Layout

Two separate JSON files, each with a distinct write cadence:

| File | Contents | Typical Write Frequency |
|------|----------|------------------------|
| `profile.json` | Career stats, personal bests, cosmetic unlock state, variant unlock state, dismissed tips | On session end, on unlock/selection events, on `onPause` |
| `settings.json` | User preferences (trajectory preview toggle, slider side, audio volume) | On setting change, on `onPause` |

### Why Two Files

Settings change independently of gameplay — the player may toggle trajectory preview without kicking a single ball. Separate files mean a settings write never risks corrupting profile data, and vice versa. Each file can be loaded, validated, and defaulted independently.

### File Paths

```
Gdx.files.local("profile.json")   → <internal_storage>/profile.json
Gdx.files.local("settings.json")  → <internal_storage>/settings.json
```

---

## 3. Domain Objects

All domain objects are Kotlin `@Serializable` data classes with **default values on every field**. This ensures forward compatibility: when a new field is added in a future version, existing JSON files missing that field will deserialize cleanly using the default.

### ProfileData

```kotlin
@Serializable
data class ProfileData(
    val version: Int = 1,
    val career: CareerStats = CareerStats(),
    val cosmetics: CosmeticState = CosmeticState(),
    val variants: VariantState = VariantState(),
    val dismissedTips: Set<String> = emptySet()
)

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
```

### SettingsData

```kotlin
@Serializable
data class SettingsData(
    val version: Int = 1,
    val trajectoryPreviewEnabled: Boolean = false,
    val sliderSide: String = "left",
    val masterVolume: Float = 1.0f,
    val sfxVolume: Float = 1.0f
)
```

### Design Notes

- **`Long` for cumulative counters**: `totalKicks` and `totalScore` will grow indefinitely over a player's lifetime. `Int` overflows at ~2.1 billion; `Long` provides headroom without needing `BigInteger`.
- **`Map<String, Long>` for `targetsByType`**: Keys are target type IDs (e.g., `"window"`, `"vehicle"`, `"drone"`, `"garage_door"`, `"runner"`). These IDs are provisional — final IDs will come from the Asset Registry spec. The map structure means new target types are automatically accommodated without schema changes.
- **`Set<String>` for cosmetic/variant IDs**: String-based IDs allow the cosmetic and variant systems to grow without modifying the persistence schema. Unlock thresholds and progression logic are NOT defined here — they belong in the future Cosmetic & Unlock System Spec.
- **Default unlocks**: `"classic_white"` ball skin, `"default_shatter"` impact effect, `"none"` trail effect, and `"suburban_crossroads"` variant are available from first launch.
- **`dismissedTips`**: Tracks which tip IDs the player has seen and dismissed, preventing repeat display. See GDD Section 10.

---

## 4. JSON Schema

Example `profile.json` after some gameplay:

```json
{
  "version": 1,
  "career": {
    "totalKicks": 347,
    "totalHits": 198,
    "totalScore": 42650,
    "bestSessionScore": 8720,
    "bestStreak": 7,
    "longestBigBombDistance": 412.5,
    "targetsByType": {
      "window": 89,
      "garage_door": 42,
      "vehicle": 31,
      "drone": 18,
      "runner": 18
    }
  },
  "cosmetics": {
    "unlockedBallSkins": ["classic_white", "orange_street", "chrome"],
    "unlockedImpactEffects": ["default_shatter", "confetti_burst"],
    "unlockedTrailEffects": ["none", "light_streak"],
    "selectedBallSkin": "chrome",
    "selectedImpactEffect": "confetti_burst",
    "selectedTrailEffect": "light_streak"
  },
  "variants": {
    "unlockedVariants": ["suburban_crossroads", "summer_block_party"],
    "selectedVariant": "suburban_crossroads"
  },
  "dismissedTips": ["tip_trajectory_preview", "tip_steer_swipe"]
}
```

Example `settings.json`:

```json
{
  "version": 1,
  "trajectoryPreviewEnabled": true,
  "sliderSide": "left",
  "masterVolume": 0.8,
  "sfxVolume": 1.0
}
```

---

## 5. Session Lifecycle

A **session** is the span of active gameplay between entering and leaving a street variant. Session-scoped values (score, streak, kick count) accumulate in memory only and are folded into career stats at session end.

### Session Boundaries

| Event | What Happens |
|-------|-------------|
| **Session start** | MAIN_MENU → READY transition. Session counters initialize to zero in memory. |
| **Session end (normal)** | Player returns to MAIN_MENU. Session stats are merged into `CareerStats` and `profile.json` is written. |
| **Session end (app pause)** | Android `onPause` fires. Same merge-and-write as normal session end — the `onPause` safety net ensures no progress is lost if the app is killed in the background. |

### Session-Scoped Values (Memory Only)

These values exist only in RAM during a session. They are NOT written to disk individually — they are folded into career stats at session end.

| Value | Purpose |
|-------|---------|
| `sessionScore` | Running score for the current session |
| `currentStreak` | Current consecutive-hit count |
| `sessionKicks` | Number of kicks this session |
| `sessionHits` | Number of target hits this session |
| `sessionTargetsByType` | Per-type hit counts for this session |

### Merge Logic at Session End

```
career.totalKicks += sessionKicks
career.totalHits += sessionHits
career.totalScore += sessionScore
career.bestSessionScore = max(career.bestSessionScore, sessionScore)
career.bestStreak = max(career.bestStreak, peakStreakThisSession)
// longestBigBombDistance updated immediately on each Big Bomb (see Section 6)
// targetsByType merged per key
for (type, count) in sessionTargetsByType:
    career.targetsByType[type] = (career.targetsByType[type] ?: 0) + count
```

> **Note:** `peakStreakThisSession` is the highest streak value reached at any point during the session, not the streak at session end (which may have been broken).

---

## 6. Save Triggers

### Profile Saves

| Trigger | What's Written | Why |
|---------|---------------|-----|
| **Session end** (return to MAIN_MENU) | Full `ProfileData` with merged session stats | Primary save point — session stats folded into career |
| **Cosmetic selection** | `CosmeticState` within `ProfileData` | Player changed their selected ball skin, impact effect, or trail effect |
| **Unlock event** | `CosmeticState` or `VariantState` within `ProfileData` | New cosmetic or variant unlocked — save immediately so it isn't lost |
| **Tip dismissal** | `dismissedTips` within `ProfileData` | Player dismissed a tip — prevents re-showing |
| **`onPause`** | Full `ProfileData` with merged session stats | Safety net — Android may kill the app after `onPause` without calling `onDestroy` |

### Settings Saves

| Trigger | What's Written |
|---------|---------------|
| **Setting changed** | Full `SettingsData` |
| **`onPause`** | Full `SettingsData` |

### What Does NOT Trigger a Save

Career stats are **never written mid-session during active gameplay**. No disk I/O occurs during AIMING, BALL_IN_FLIGHT, SCORING, or IMPACT_MISSED states. This avoids I/O latency during the core loop and keeps the in-flight experience smooth.

The one exception is `longestBigBombDistance`: this personal best is updated in memory immediately when a Big Bomb exceeds the previous record, but the write to disk still waits for session end or `onPause`.

### Edge Case: `onPause` During BALL_IN_FLIGHT

If `onPause` fires while the ball is in flight (e.g., the player receives a phone call), the in-progress kick is discarded. Only stats from completed kicks — those that have already passed through SCORING or IMPACT_MISSED — are included in the save. The partially completed kick's outcome is unknown, so it is not counted.

---

## 7. Schema Versioning

Both `profile.json` and `settings.json` carry an integer `version` field at the root level.

### Version Strategy

| Change Type | Action Required |
|-------------|----------------|
| **Additive field** | No migration needed. `kotlinx.serialization` uses the default value for any missing field. Bump `version` for tracking purposes. |
| **Renamed field** | Migration function maps old name to new name. |
| **Removed field** | Mark as `@Transient` or use `ignoreUnknownKeys = true` in the JSON decoder. No migration needed — the old field is silently dropped on next write. |
| **Restructured data** | Migration function transforms old structure to new. |

### Migration Approach

```kotlin
// Chained migration functions
fun migrateProfile(json: JsonObject): JsonObject {
    var data = json
    val version = data["version"]?.jsonPrimitive?.int ?: 0

    if (version < 2) data = migrateProfileV1toV2(data)
    if (version < 3) data = migrateProfileV2toV3(data)
    // ... chain continues for each breaking version

    return data
}
```

Migrations are applied before deserialization. Each migration function transforms the raw `JsonObject` from one version to the next. This keeps the `@Serializable` data classes clean — they always reflect the current schema, and migration logic is isolated in a single file.

### JSON Decoder Configuration

```kotlin
val json = Json {
    ignoreUnknownKeys = true   // Tolerate fields from newer versions
    encodeDefaults = true      // Always write all fields for clarity
    prettyPrint = true         // Human-readable during development
}
```

`ignoreUnknownKeys = true` is important for forward compatibility: if a player downgrades the app, the older version can still read a file written by a newer version without crashing on unrecognized fields.

---

## 8. Error Handling

### Missing File

If `profile.json` or `settings.json` does not exist (first launch, or file was deleted), create a new file with default values. The default-value convention on all data class fields (Section 3) ensures a clean starting state.

### Corrupt JSON

If a file exists but cannot be parsed (malformed JSON, unexpected structure, deserialization failure):

1. Rename the corrupt file to `profile.corrupt.json` or `settings.corrupt.json` (preserving the data for manual inspection).
2. Create a new file with default values.
3. Log a warning (not a crash).

If a `.corrupt.json` file already exists from a previous corruption event, overwrite it — only the most recent corrupt file is retained.

### Atomic Writes

To prevent data loss from a write interrupted by a crash or `onPause` timeout:

1. Write the new JSON to a temporary file (`profile.tmp.json`).
2. Delete the original file.
3. Rename the temporary file to the original name.

This ensures the file is either fully written or not present — never partially written. `Gdx.files.local()` supports these operations on both Android and Desktop.

### Startup Recovery

On load, if the primary file is missing but a `.tmp.json` file exists, rename `.tmp.json` to the primary name and attempt to read it. This recovers from the case where the app was killed between steps 2 and 3 of the atomic write.

---

## 9. What Is NOT Persisted

The following values are **intentionally excluded** from persistence. They are transient, session-scoped, or frame-scoped and exist only in memory.

| Value | Reason |
|-------|--------|
| **Session score** | Folded into `career.totalScore` at session end; not stored independently |
| **Current streak** | Resets each session; only `career.bestStreak` is persisted |
| **Session kick count** | Folded into `career.totalKicks` at session end |
| **Ball position / velocity / spin** | Frame-level physics state — meaningless outside of active gameplay |
| **Angle slider position** | Resets to default each session |
| **Steer budget (swipe count)** | Resets each kick |
| **Pause state** | The game always resumes to MAIN_MENU after `onPause` + background kill |
| **Trajectory preview path** | Computed each frame from current input; not data |
| **Score popup animations** | Visual-only, no persistence value |
| **Spawn lane positions** | Moving targets reset each session |

---

## 10. Future Considerations

These are not part of the alpha implementation. They are documented here to show that the current design does not preclude them.

| Feature | Migration Path |
|---------|---------------|
| **Room database** | If data grows beyond flat files (e.g., per-session history, detailed shot logs), migrate to Room. The domain objects (Section 3) map directly to Room `@Entity` classes. JSON files can be imported as a one-time migration on first launch after the update. |
| **Cloud sync (GPGS Saved Games)** | Google Play Games Services Saved Games API stores opaque byte blobs. Serialize `ProfileData` to JSON bytes and upload. Conflict resolution: merge career stats (take max of bests, sum of cumulative), union of unlocked sets. |
| **Encryption** | If cosmetic unlocks gain real-money value, encrypt `profile.json` using Android Keystore-backed AES. The JSON structure is unchanged; only the on-disk representation changes. |
| **Data export** | Provide a "Copy Stats" button in settings that copies a formatted summary to the clipboard. No file picker needed. |
| **Local leaderboards** | Add a third file (`leaderboard.json`) containing a sorted list of top session scores with timestamps. Capped at 50 entries with oldest/lowest evicted. |
| **Per-session history** | Store a list of recent sessions (date, score, kicks, hits) for a "Recent Sessions" screen. Cap at 20–50 entries. This is where Room becomes more natural than flat JSON. |

---

## Companion Documents

- **`game-design-document.md`** Section 7 — Defines the progression and stat concepts that this system persists.
- **`game-design-document.md`** Section 10 — Defines the tips system; `dismissedTips` tracks which tips have been shown.
- **`state-machine.md`** — Defines the game states that trigger loads (BOOT → LOADING) and session boundaries (MAIN_MENU ↔ READY).
- **`game-mechanics-overview.md`** — Concise summary of scoring and streaks whose values are tracked by `CareerStats`.
- **`ui-hud-layout.md`** — Defines the HUD elements whose preferences (e.g., slider side) are stored in `SettingsData`.
