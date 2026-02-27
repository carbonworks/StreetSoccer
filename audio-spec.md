# Street Soccer — Audio Specification

**This document is the master reference for all sound effects in Street Soccer.** It catalogs every audio cue, defines mixing priorities, and specifies file format and volume requirements. All sound descriptions derive from the feedback tables in `game-design-document.md` Section 9 and the `AudioService` interface in `Services.kt`.

---

## 1. File Format & Technical Requirements

| Property | Value |
|----------|-------|
| **Format** | OGG Vorbis (`.ogg`) |
| **Sample rate** | 44,100 Hz |
| **Channels** | Mono (sound effects) |
| **Bit depth** | 16-bit |
| **File location** | `assets/sounds/` |

### Why OGG

OGG Vorbis is the recommended format for LibGDX on Android. It is natively supported, produces smaller files than WAV, and decodes efficiently on mobile hardware. MP3 has licensing constraints and inconsistent Android behavior; WAV is too large for a mobile distribution.

### Volume Model

All sound effects are played through LibGDX's `Sound` API. Effective playback volume is computed as:

```
effectiveVolume = defaultVolume * sfxVolume * masterVolume
```

Where:
- `defaultVolume` is the per-cue level defined in Section 2 (0.0 -- 1.0)
- `sfxVolume` is the player's SFX slider setting from `SettingsData.sfxVolume` (0.0 -- 1.0, default 1.0)
- `masterVolume` is the player's master slider setting from `SettingsData.masterVolume` (0.0 -- 1.0, default 1.0)

> **Cross-reference:** Volume sliders are post-alpha settings controls defined in `menu-and-navigation-flow.md` Section 5 (Post-Alpha Additions) and stored in `save-and-persistence.md` Section 3 (`SettingsData`).

---

## 2. Sound Cue Catalog

### 2.1 Kick Sounds

| # | Asset ID | Description | Duration | Default Volume | Category | GDD Reference |
|---|----------|-------------|:--------:|:--------------:|----------|---------------|
| 1 | `sfx_kick_normal` | Standard kick launch -- a clean, firm contact sound. Short percussive thump with a slight "whap" character. | 0.3 s | 0.8 | kick | Section 9 (implied -- every kick produces feedback) |
| 2 | `sfx_kick_big_bomb` | Big Bomb launch -- a deep, resonant bass boom that conveys massive power. Low-frequency emphasis with a brief rumble tail. Plays instead of `sfx_kick_normal` when Big Bomb thresholds are met. | 0.6 s | 0.9 | kick | Section 9: "Low bass 'boom' on launch" |

### 2.2 Impact Sounds (Target Hits -- Scoring)

| # | Asset ID | Description | Duration | Default Volume | Category | GDD Reference |
|---|----------|-------------|:--------:|:--------------:|----------|---------------|
| 3 | `sfx_impact_glass` | Window hit -- crisp, bright glass-break sound. High-frequency shatter with a short decay. Should feel satisfying and clean. | 0.5 s | 0.85 | impact | Section 9: "Crisp glass-break sound" |
| 4 | `sfx_impact_metal` | Garage door hit -- deep, resonant metallic clang. Mid-to-low frequency with a reverberant ring. | 0.6 s | 0.8 | impact | Section 9: "Deep metallic clang" |
| 5 | `sfx_impact_vehicle` | Vehicle hit -- car alarm chirp layered with an impact thud. Two-part sound: initial metallic dent followed by a brief alarm chirp. | 0.8 s | 0.85 | impact | Section 9: "Car alarm chirp + impact thud" |
| 6 | `sfx_impact_drone` | Drone hit -- electronic fizz followed by a mechanical crash. Starts with a high-frequency electrical crackle, then transitions to a metallic clatter. | 0.7 s | 0.8 | impact | Section 9: "Electronic fizz + crash" |

### 2.3 Impact Sounds (Misses -- Non-Scoring)

| # | Asset ID | Description | Duration | Default Volume | Category | GDD Reference |
|---|----------|-------------|:--------:|:--------------:|----------|---------------|
| 7 | `sfx_miss_facade` | House facade hit -- dull, flat thud. Low-frequency impact with no ring or sustain. Communicates "you hit nothing interesting." | 0.3 s | 0.6 | impact | Section 9: "Dull thud" |
| 8 | `sfx_miss_fence` | Fence hit -- wooden crack. Dry, short snap with a slight splintery character. | 0.35 s | 0.65 | impact | Section 9: "Wooden crack" |
| 9 | `sfx_miss_out_of_bounds` | Out of bounds -- quiet whoosh or silence. A soft, trailing wind sound that communicates the ball leaving the play area. May also be implemented as silence. | 0.4 s | 0.3 | impact | Section 9: "Silence or quiet whoosh" |

### 2.4 Score & Streak Sounds

| # | Asset ID | Description | Duration | Default Volume | Category | GDD Reference |
|---|----------|-------------|:--------:|:--------------:|----------|---------------|
| 10 | `sfx_score_popup` | Score popup -- brief, bright point chime. A short ascending tone that punctuates the score display. Plays on every target hit alongside the impact sound. | 0.25 s | 0.5 | score | Section 9: Score Popups |
| 11 | `sfx_streak_chime` | Streak milestone -- quick fanfare chime. A rising two- or three-note arpeggio that escalates in pitch with higher streaks. At streak 5+, pitch shifts upward noticeably. | 0.4 s | 0.7 | score | Section 9: "Quick chime" at 3 hits, "escalating chime pitch" at 5+ |
| 12 | `sfx_streak_broken` | Streak broken -- deflation sound. A descending tone or "wah-wah" that conveys the loss of a multiplier. Subdued, not punishing. | 0.5 s | 0.5 | score | Section 9: "Deflation sound" |
| 13 | `sfx_big_bomb_score` | Big Bomb distance score -- a sustained rising tone that plays when the Big Bomb distance score is awarded. Longer and more dramatic than the standard score popup. | 0.6 s | 0.7 | score | Section 9: Big Bomb Distance Feedback (visual is primary; this audio supplements) |

### 2.5 Flight & Steer Sounds

| # | Asset ID | Description | Duration | Default Volume | Category | GDD Reference |
|---|----------|-------------|:--------:|:--------------:|----------|---------------|
| 14 | `sfx_steer_whoosh_full` | Steer swipe (1st swipe, full effect) -- a firm, satisfying whoosh. Clear air-cutting sound at full volume. | 0.3 s | 0.6 | feedback | Section 9: "Subtle whoosh / spin sound on each swipe" |
| 15 | `sfx_steer_whoosh_reduced` | Steer swipe (2nd swipe, 0.6x effect) -- a noticeably softer whoosh. Same character as `sfx_steer_whoosh_full` but at reduced intensity. | 0.25 s | 0.4 | feedback | Section 9: "Whoosh at reduced volume" |
| 16 | `sfx_steer_whoosh_weak` | Steer swipe (3rd swipe, 0.25x effect) -- a dampened, quiet whoosh. Muted version signaling minimal remaining effect. | 0.2 s | 0.25 | feedback | Section 9: "Dampened whoosh -- quiet" |
| 17 | `sfx_steer_whoosh_residual` | Steer swipe (4th+ swipe, 0.1x residual) -- a barely audible whoosh. Confirms the input registered without overstating its effect. | 0.15 s | 0.15 | feedback | Section 9: "Faint whoosh -- barely audible" |

### 2.6 Catcher Feedback

| # | Asset ID | Description | Duration | Default Volume | Category | GDD Reference |
|---|----------|-------------|:--------:|:--------------:|----------|---------------|
| 18 | `sfx_catcher_catch` | Ball caught by the intersection catcher NPC. A clean, firm catch sound -- leather impact with a brief "smack." Signals that the ball was intercepted without scoring. | 0.35 s | 0.7 | feedback | `suburban-crossroads.json`: catcher_spawn_point |

### 2.7 UI Sounds

| # | Asset ID | Description | Duration | Default Volume | Category | GDD Reference |
|---|----------|-------------|:--------:|:--------------:|----------|---------------|
| 19 | `sfx_ui_tap` | Button/icon tap -- a clean, light tap or click. Plays on menu buttons, overlay close, and HUD icon interactions. | 0.1 s | 0.5 | ui | Implied by menu interactions |
| 20 | `sfx_ui_slider_drag` | Angle slider drag feedback -- a very subtle tick or soft scrape. Plays periodically while the slider thumb is being dragged. Optional; may be omitted if it feels distracting. | 0.05 s | 0.2 | ui | `input-system.md` Section 2: slider interaction |
| 21 | `sfx_ui_pause` | Pause/resume -- a brief mechanical click or shutter sound. Plays when entering or leaving the PAUSED state. | 0.15 s | 0.4 | ui | `menu-and-navigation-flow.md` Section 4: pause menu |

### 2.8 Ambient (Future)

| # | Asset ID | Description | Duration | Default Volume | Category | Notes |
|---|----------|-------------|:--------:|:--------------:|----------|-------|
| 22 | `sfx_ambient_suburban` | Ambient suburban loop -- light traffic hum, distant birds, occasional breeze. Loops continuously during gameplay. | 10.0+ s | 0.15 | ambient | Post-alpha. Base variant ambient bed. |

---

## 3. Sound Categories & Priority

### Category Definitions

| Category | Purpose | Sound Count |
|----------|---------|:-----------:|
| **kick** | Ball launch feedback | 2 |
| **impact** | Ball collision feedback (hits and misses) | 7 |
| **score** | Scoring and streak feedback | 4 |
| **feedback** | Mid-flight steer and catcher events | 5 |
| **ui** | Menu and HUD interaction | 3 |
| **ambient** | Background atmosphere (future) | 1 |

### Mixing & Priority Rules

Sound mixing follows these rules to prevent audio clutter during high-activity moments:

| Rule | Description |
|------|-------------|
| **Impact interrupts ambient** | Impact sounds (category: impact) should temporarily duck the ambient volume by 50% for 0.3 s, then restore. This ensures impacts cut through. |
| **UI always plays** | UI sounds (category: ui) are never suppressed or ducked. They play at their effective volume regardless of other concurrent sounds. |
| **One kick sound at a time** | Only one kick sound plays per launch. `sfx_kick_big_bomb` replaces `sfx_kick_normal` when Big Bomb thresholds are met; they are never layered. |
| **Impact + score layer** | When a target is hit, the impact sound and the score popup sound play simultaneously. They are mixed additively. |
| **Streak chime layers on score** | `sfx_streak_chime` plays in addition to `sfx_score_popup` and the impact sound when a streak milestone (3 or 5+) is reached. Up to 3 simultaneous sounds on a target hit. |
| **Steer whoosh: one at a time** | Only one steer whoosh plays per swipe. If the player swipes rapidly, the previous whoosh is stopped and replaced by the new one (keyed by diminishing tier). |
| **No concurrent duplicate sounds** | If the same asset ID is already playing, the existing instance is stopped before the new one starts. Exception: `sfx_score_popup` may overlap briefly during rapid multi-hit scenarios. |

### Polyphony Budget

LibGDX `Sound` objects support concurrent playback, but Android hardware typically handles 8-12 simultaneous audio streams well. The worst-case scenario (Big Bomb launch + impact + score + streak + ambient) produces at most 5 concurrent sounds, well within budget.

---

## 4. AudioService Mapping

The existing `AudioService` interface in `Services.kt` maps to asset IDs as follows:

| AudioService Method | Asset ID(s) | Notes |
|---------------------|-------------|-------|
| `playKickLaunch()` | `sfx_kick_normal` | Standard kick |
| `playBigBombActivation()` | `sfx_kick_big_bomb` | Replaces `playKickLaunch()` for Big Bomb kicks |
| `playGlassBreak()` | `sfx_impact_glass` | Window target hit |
| `playMetallicClang()` | `sfx_impact_metal` | Garage door target hit |
| `playCarAlarm()` | `sfx_impact_vehicle` | Vehicle target hit |
| `playElectronicFizz()` | `sfx_impact_drone` | Drone target hit |
| `playBounce()` | `sfx_miss_facade` or `sfx_miss_fence` | Determined by collider type (`left_house_facade`/`right_house_facade` vs. `left_fence`/`right_fence`) |
| `playMiss()` | `sfx_miss_out_of_bounds` | Out-of-bounds or silent miss |
| `playWhoosh()` | `sfx_steer_whoosh_full` / `_reduced` / `_weak` / `_residual` | Selected by current swipe index (0-based: 0 = full, 1 = reduced, 2 = weak, 3+ = residual) |
| `playStreakChime()` | `sfx_streak_chime` | Streak milestone |
| `playScorePopup()` | `sfx_score_popup` | Score display |
| `playUiTap()` | `sfx_ui_tap` | Menu/HUD interaction |

### AudioService Extensions Needed

The following sounds are defined in this spec but not yet covered by the `AudioService` interface. These methods should be added when implementing audio:

| New Method | Asset ID | Purpose |
|------------|----------|---------|
| `playStreakBroken()` | `sfx_streak_broken` | Streak multiplier reset |
| `playBigBombScore()` | `sfx_big_bomb_score` | Big Bomb distance score award |
| `playCatcherCatch()` | `sfx_catcher_catch` | Ball intercepted by catcher NPC |
| `playPauseResume()` | `sfx_ui_pause` | Entering/leaving PAUSED state |

The `playBounce()` method should be split into `playFacadeHit()` and `playFenceHit()` to match the distinct sounds defined in the GDD, or accept a surface type parameter.

---

## 5. Asset Production Notes

### Sourcing Guidelines

| Approach | When to Use |
|----------|-------------|
| **Royalty-free libraries** | Primary source. Sites like Freesound.org (CC0/CC-BY), Sonniss GDC bundles, or Kenney.nl. Verify license permits commercial use. |
| **Synthesized (sfxr/jsfxr)** | Suitable for UI sounds, chimes, and simple impacts. Quick iteration. |
| **Recorded + processed** | Best for realistic impacts (glass, metal, wood). Record raw source and apply EQ, compression, reverb. |

### Style Consistency

All sounds should match the game's **lighthearted, arcade tone** (GDD Section 1: "Lighthearted mischief -- cartoon destruction, no real consequences"):

- Impacts should be **punchy and satisfying**, not realistic or violent
- Glass breaks should sound **crisp and fun**, not dangerous
- The Big Bomb boom should feel **powerful and exciting**, not threatening
- UI sounds should be **minimal and clean** -- they should not draw attention away from gameplay
- Steer whooshes should be **subtle** -- they provide confirmation, not spectacle

### Processing Chain

Recommended processing for consistency across all sound effects:

1. **Normalize** peak to -1 dB
2. **High-pass filter** at 80 Hz (remove rumble except for `sfx_kick_big_bomb`)
3. **Compress** with a fast attack (5 ms) and medium release (100 ms) to tame transients
4. **Trim** silence from start and end (no leading silence)
5. **Fade out** the last 10 ms to prevent clicks

---

## 6. Seasonal Variant Audio

Each seasonal variant (see `seasonal-variants.md`) may swap or extend the ambient audio and optionally modify impact sounds. The cue catalog above covers the base `suburban_crossroads` variant.

| Variant | Ambient Changes | Impact Changes |
|---------|----------------|----------------|
| **Summer Block Party** | Add crowd chatter, sizzling grill, sprinkler sounds | None -- base impacts work |
| **Halloween Night** | Replace with eerie wind, distant owl, creaking | Pumpkin targets use a unique squelchy impact |
| **Winter Holidays** | Add gentle snowfall ambience, distant jingle bells | Snowman targets use a soft "poof" impact |
| **Rainy Day** | Rain loop with occasional thunder | Wet splat layered on top of base impacts |
| **Garage Sale Saturday** | Replace with morning birds, radio playing in distance | Junk table targets use ceramic/glass crash sounds |

Variant-specific sounds follow the same format, naming, and volume conventions as the base catalog. Their asset IDs follow the pattern: `sfx_{variant}_{cue}` (e.g., `sfx_halloween_pumpkin_hit`, `sfx_winter_ambient`).

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `game-design-document.md` Section 9 | Design intent for all audio feedback -- the source for every sound description in this spec |
| `save-and-persistence.md` Section 3 | `SettingsData` fields `masterVolume` and `sfxVolume` that control playback levels |
| `menu-and-navigation-flow.md` Section 5 | Settings overlay where volume controls will appear (post-alpha) |
| `Services.kt` | `AudioService` interface and `NoopAudioService` stub that this spec informs |
| `asset-registry.md` | Cross-references all audio assets with file paths and production status |
| `seasonal-variants.md` | Defines per-variant audio swaps and additions |
