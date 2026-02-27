# Street Soccer — Cosmetic & Unlock System Specification

**This document defines the implementation spec for Street Soccer's cosmetic unlock system.** It covers data models, unlock thresholds, selection UI behavior, and runtime application. All cosmetics are purely visual -- they do not affect gameplay (GDD Section 7). The persistence layer is already defined in `save-and-persistence.md`; this spec defines the progression logic and rendering integration.

---

## 1. Design Principles

These principles are derived from `game-design-document.md` Section 7:

1. **Purely visual** -- No cosmetic item alters physics, scoring, hitboxes, or any gameplay parameter.
2. **Intentionally minimal** -- A small, achievable collection of unlocks. Not an endless grind.
3. **Proof of mastery** -- Each unlock corresponds to a meaningful play milestone, not a random drop or time gate.
4. **Three categories only** -- Ball skins, impact effects, and trail effects. No other cosmetic categories in scope.
5. **One equipped per category** -- The player selects one ball skin, one impact effect, and one trail effect. All three are active simultaneously.

---

## 2. Data Model

### 2.1 Existing Persistence (No Changes Required)

The data model for cosmetic state is already implemented in `Services.kt` and defined in `save-and-persistence.md` Section 3:

```kotlin
@Serializable
data class CosmeticState(
    val unlockedBallSkins: Set<String> = setOf("classic_white"),
    val unlockedImpactEffects: Set<String> = setOf("default_shatter"),
    val unlockedTrailEffects: Set<String> = setOf("none"),
    val selectedBallSkin: String = "classic_white",
    val selectedImpactEffect: String = "default_shatter",
    val selectedTrailEffect: String = "none"
)
```

This structure supports the cosmetic system without modification:
- `Set<String>` for unlocked items allows the item catalog to grow without schema changes.
- String-based IDs decouple persistence from rendering implementation.
- Default values ensure a clean first-launch state.

### 2.2 Cosmetic Item Definition (New)

Each cosmetic item is defined by a data class that the unlock evaluation system references. This is not persisted -- it is a static catalog compiled into the game.

```kotlin
data class CosmeticItem(
    val id: String,                    // Matches IDs in CosmeticState sets
    val category: CosmeticCategory,    // BALL_SKIN, IMPACT_EFFECT, TRAIL_EFFECT
    val displayName: String,           // Shown in cosmetics overlay
    val unlockCondition: UnlockCondition,
    val assetPath: String              // Path to the sprite/effect asset
)

enum class CosmeticCategory {
    BALL_SKIN,
    IMPACT_EFFECT,
    TRAIL_EFFECT
}

sealed class UnlockCondition {
    object AlwaysUnlocked : UnlockCondition()
    data class TotalScore(val threshold: Long) : UnlockCondition()
    data class BestStreak(val threshold: Int) : UnlockCondition()
    data class TotalHits(val threshold: Long) : UnlockCondition()
    data class BigBombDistance(val threshold: Float) : UnlockCondition()
    data class TargetTypeHits(val targetType: String, val threshold: Long) : UnlockCondition()
}
```

---

## 3. Cosmetic Item Catalog

### 3.1 Ball Skins

Ball skins replace the default ball sprite during gameplay. The ball's physics, hitbox, and shadow are unaffected.

| ID | Display Name | Unlock Condition | Threshold | Asset Path | Description |
|----|-------------|------------------|:---------:|------------|-------------|
| `classic_white` | Classic White | Always unlocked | -- | `sprites/ball.svg` | Default white soccer ball. Available from first launch. |
| `orange_street` | Street Ball | Total score | 5,000 | `sprites/skins/ball_orange.svg` | Orange rubber street ball with black pentagon pattern. The first unlock most players will earn. |
| `chrome` | Chrome Ball | Total score | 25,000 | `sprites/skins/ball_chrome.svg` | Reflective chrome ball with a subtle metallic gradient. Mid-tier achievement. |
| `flame` | Flame Ball | Total score | 50,000 | `sprites/skins/ball_flame.svg` | Ball with red-orange flame decal wrapping around the surface. High-tier achievement. |
| `pixel` | Pixel Ball | Total score | 100,000 | `sprites/skins/ball_pixel.svg` | Retro pixel-art style ball with chunky square pixels. Endgame reward. |

### 3.2 Impact Effects

Impact effects are visual variations on the particle burst that plays when the ball hits a target. The default glass-shatter effect is replaced by the selected impact effect for all target types. The impact sound is unaffected.

| ID | Display Name | Unlock Condition | Threshold | Description |
|----|-------------|------------------|:---------:|-------------|
| `default_shatter` | Glass Shatter | Always unlocked | -- | Default particle effect: angular glass shards scatter from impact point. |
| `confetti_burst` | Confetti Burst | Total hits | 100 | Colorful confetti pieces burst outward from impact. Celebratory feel. |
| `pixel_explosion` | Pixel Explosion | Total hits | 500 | Retro pixel-art squares scatter from impact in cardinal directions. |
| `smoke_puff` | Smoke Puff | Total hits | 1,000 | A soft smoke cloud blooms from the impact point and dissipates. |

### 3.3 Trail Effects

Trail effects are visual variations on the ball's in-flight trail. The default behavior (no trail) shows only the ball sprite. Selected trail effects add a persistent visual element behind the ball during flight.

| ID | Display Name | Unlock Condition | Threshold | Description |
|----|-------------|------------------|:---------:|-------------|
| `none` | No Trail | Always unlocked | -- | Default: no trail effect. Ball flies clean. |
| `light_streak` | Light Streak | Big Bomb distance | 300 | A bright, short light trail stretches behind the ball. Fades over 0.2 s. |
| `sparkle_trail` | Sparkle Trail | Big Bomb distance | 400 | Small sparkle particles emit from the ball and fade. Magical feel. |
| `smoke_trail` | Smoke Trail | Big Bomb distance | 450 | A thin smoke ribbon trails behind the ball. Dissipates over 0.3 s. |

---

## 4. Unlock Evaluation

### 4.1 When to Evaluate

Unlock conditions are evaluated at **two points**:

1. **Session end** -- After session stats are merged into `CareerStats` (see `save-and-persistence.md` Section 5), check all locked items against the updated career stats.
2. **Immediately on Big Bomb distance record** -- `longestBigBombDistance` is updated in memory as soon as a Big Bomb exceeds the previous record (see `save-and-persistence.md` Section 6). Trail effect unlocks keyed on this value should be checked immediately so the player sees the unlock feedback during the same session.

### 4.2 Evaluation Logic

```
function evaluateUnlocks(profile: ProfileData): ProfileData {
    var cosmetics = profile.cosmetics

    for each item in COSMETIC_CATALOG:
        if item.id already in cosmetics.unlockedSet(item.category):
            continue  // already unlocked

        if item.unlockCondition.isMet(profile.career):
            cosmetics = cosmetics.withUnlocked(item.category, item.id)
            // Queue unlock notification for UI display

    return profile.copy(cosmetics = cosmetics)
}
```

### 4.3 Unlock Condition Evaluation

| Condition Type | Career Field Checked | Comparison |
|---------------|---------------------|------------|
| `AlwaysUnlocked` | -- | Always true (default items) |
| `TotalScore` | `career.totalScore` | `>=` threshold |
| `BestStreak` | `career.bestStreak` | `>=` threshold |
| `TotalHits` | `career.totalHits` | `>=` threshold |
| `BigBombDistance` | `career.longestBigBombDistance` | `>=` threshold |
| `TargetTypeHits` | `career.targetsByType[targetType]` | `>=` threshold |

### 4.4 Unlock Persistence

When a new item is unlocked:
1. Add the item ID to the appropriate `unlocked*` set in `CosmeticState`.
2. Save `ProfileData` immediately (this is an unlock event trigger per `save-and-persistence.md` Section 6).
3. The item remains unlocked permanently -- there is no mechanism to re-lock items.

---

## 5. Selection UI

### 5.1 Cosmetics Overlay

The cosmetics overlay is accessible from the main menu attract screen (post-alpha). Its layout and navigation behavior are defined in `menu-and-navigation-flow.md` Section 7. This section specifies the data flow.

### 5.2 Data Flow

```
1. Player taps palette icon on attract screen
2. Open Cosmetics Overlay
3. Load current CosmeticState from ProfileData
4. For each category tab (Ball Skins / Impact FX / Trail FX):
   a. Render all items from COSMETIC_CATALOG for that category
   b. Mark items as "unlocked", "selected", or "locked" based on CosmeticState
   c. For locked items: display the unlock condition as a hint (e.g., "50,000 pts")
5. On item tap (unlocked only):
   a. Update selectedBallSkin / selectedImpactEffect / selectedTrailEffect in CosmeticState
   b. Save ProfileData immediately (cosmetic selection trigger)
   c. Update visual highlight in the overlay
6. Close overlay: return to attract screen
```

### 5.3 Preview Before Unlock

For locked items, the overlay should display:
- A **silhouette or dimmed version** of the item thumbnail (not the full art -- preserving surprise)
- The **unlock condition** in text below the thumbnail (e.g., "Score 50,000 total points")
- The **current progress** toward the condition (e.g., "32,450 / 50,000")

### 5.4 Unlock Notification

When a new item is unlocked (detected during evaluation in Section 4):
- A **brief toast notification** appears during gameplay or on return to the main menu
- Toast content: item icon + "New unlock: {displayName}!"
- Toast auto-dismisses after 3 seconds or on tap
- The cosmetics overlay icon on the attract screen may display a **notification badge** (a small dot or number) indicating unseen unlocks

---

## 6. Runtime Application

### 6.1 Ball Skin Application

The selected ball skin is applied by the rendering pipeline when drawing the ball entity.

| Step | Detail |
|------|--------|
| **When** | On session start (MAIN_MENU to READY transition) |
| **What** | Read `ProfileData.cosmetics.selectedBallSkin` |
| **How** | Look up the asset path from the `COSMETIC_CATALOG` for the selected skin ID. Load the corresponding texture/SVG. Assign it to the ball entity's `TextureComponent` (or equivalent rendering component). |
| **Fallback** | If the selected skin's asset is missing, fall back to `classic_white` (`sprites/ball.svg`). |
| **Physics** | No change. The ball's collision shape, mass, and physics properties are identical regardless of skin. |

### 6.2 Impact Effect Application

The selected impact effect determines which particle configuration is used when the ball hits a target.

| Step | Detail |
|------|--------|
| **When** | On each target hit (SCORING state entry) |
| **What** | Read `ProfileData.cosmetics.selectedImpactEffect` |
| **How** | Use the selected effect ID to look up the corresponding particle system configuration. Spawn the effect at the ball's impact position. If the effect ID is `default_shatter`, use the standard glass particle system (`fx_glass_shatter`). Other effect IDs map to their corresponding particle definitions in `asset-registry.md` Section 5. |
| **Fallback** | If the selected effect's assets are missing, fall back to `default_shatter`. |
| **Scoring** | No change. The particle effect is purely visual decoration on top of the scoring event. |

### 6.3 Trail Effect Application

The selected trail effect determines whether and how a visual trail renders behind the ball during flight.

| Step | Detail |
|------|--------|
| **When** | Per-frame during BALL_IN_FLIGHT state |
| **What** | Read `ProfileData.cosmetics.selectedTrailEffect` |
| **How** | If `none`, render nothing extra. Otherwise, each frame emit trail particles or render trail segments at the ball's position. Trail particles are spawned in world space and fade over their configured lifetime (0.2--0.3 s), creating a trailing ribbon or scatter behind the moving ball. |
| **Interaction with steer feedback** | Trail effects render in addition to the steer swipe trail described in GDD Section 9 (Spin & Steer Feedback). The cosmetic trail is continuous; the steer trail is momentary. They layer together. |
| **Interaction with Big Bomb color ramp** | The cosmetic trail's color is independent of the Big Bomb red overlay. During a Big Bomb, the ball sprite gets the red overlay while the trail continues in its own color. |
| **Fallback** | If the selected trail's assets are missing, fall back to `none`. |

### 6.4 Component Architecture

The cosmetic system integrates with the ECS architecture as follows:

```
BallEntity:
  - TextureComponent     ← skin asset assigned here
  - CosmeticComponent    ← stores selectedSkin, selectedImpact, selectedTrail IDs

RenderSystem:
  - Reads TextureComponent to draw the ball with the active skin
  - Reads CosmeticComponent to determine trail rendering behavior

ScoringSystem / ImpactSystem:
  - Reads CosmeticComponent to determine which particle effect to spawn on hit

CosmeticComponent (new):
  - activeSkinId: String
  - activeImpactEffectId: String
  - activeTrailEffectId: String
```

> **Note:** `CosmeticComponent` is a lightweight data holder. It does not contain rendering logic -- it provides the IDs that `RenderSystem` and `ImpactSystem` use to select the correct assets.

---

## 7. Unlock Threshold Rationale

### 7.1 Progression Curve

The unlock thresholds are designed around the following progression assumptions:

| Play Milestone | Approx. Time | Total Score | Total Hits | Best Big Bomb |
|---------------|:------------:|:-----------:|:----------:|:-------------:|
| First session (casual) | 5--10 min | 1,000--3,000 | 10--30 | 0--200 |
| Getting comfortable | 30 min | 5,000--8,000 | 50--80 | 200--300 |
| Regular player | 1--2 hours | 20,000--35,000 | 150--250 | 300--350 |
| Dedicated player | 3--5 hours | 50,000--80,000 | 400--700 | 350--400 |
| Completionist | 8--12 hours | 100,000+ | 1,000+ | 450+ |

### 7.2 Unlock Distribution

| Unlock Tier | Items | Approx. Play Time to Reach |
|-------------|:-----:|:--------------------------:|
| **Free** (always unlocked) | 3 items (1 per category) | Immediate |
| **Early** (5,000 score / 100 hits / 300 Big Bomb) | 3 items | 30 min -- 1 hour |
| **Mid** (25,000 score / 500 hits / 400 Big Bomb) | 3 items | 2 -- 4 hours |
| **Late** (50,000 score / 1,000 hits / 450 Big Bomb) | 3 items | 4 -- 8 hours |
| **Endgame** (100,000 score) | 1 item | 10+ hours |

### 7.3 Design Intent

- **Early unlocks** (30 min) ensure every player sees the system working. The first ball skin unlock at 5,000 total score should happen within a few sessions.
- **Mid unlocks** (2--4 hours) reward players who return to the game multiple times.
- **Late unlocks** (4--8 hours) are for dedicated fans and serve as visible proof of mastery.
- **The endgame unlock** (pixel ball at 100,000) is the completionist target. It should feel like a genuine achievement.
- **No grind walls**: Even a player who misses frequently (30% hit rate) will unlock everything through sustained play, because all score-based thresholds use `totalScore` (cumulative, never decreases).

---

## 8. Integration with Seasonal Variants

Cosmetic items are **variant-agnostic** -- the same ball skins, impact effects, and trail effects work across all seasonal variants. There are no variant-exclusive cosmetics in the current design.

Future expansion possibilities (not in scope):
- Variant-exclusive skins unlocked by playing specific variants
- Seasonal cosmetics available only during certain calendar periods
- Variant-specific impact effects that match the seasonal theme

These would be added to the `COSMETIC_CATALOG` with variant-aware unlock conditions (e.g., `data class VariantPlayTime(val variantId: String, val minutes: Int) : UnlockCondition()`).

---

## 9. Implementation Checklist

This section lists the concrete implementation tasks for the cosmetic system, in suggested build order:

1. **Define `CosmeticItem` data class and `COSMETIC_CATALOG`** -- Static list of all items with their unlock conditions.
2. **Implement `UnlockEvaluator`** -- Takes `ProfileData`, iterates the catalog, returns updated `CosmeticState` with any newly unlocked items.
3. **Call `UnlockEvaluator` at session end** -- After `SessionAccumulator.mergeInto()`, run unlock evaluation and save if changed.
4. **Call `UnlockEvaluator` on Big Bomb record** -- Immediately when `longestBigBombDistance` is updated.
5. **Add `CosmeticComponent` to the ball entity** -- Carries the active cosmetic IDs.
6. **Modify `RenderSystem` to read active skin** -- Swap the ball texture based on `CosmeticComponent.activeSkinId`.
7. **Modify impact handling to read active effect** -- Select the particle configuration from `CosmeticComponent.activeImpactEffectId`.
8. **Add trail rendering to `RenderSystem`** -- Emit trail particles per-frame based on `CosmeticComponent.activeTrailEffectId`.
9. **Build Cosmetics Overlay UI** -- Following `menu-and-navigation-flow.md` Section 7.
10. **Add unlock toast notification** -- Brief popup during gameplay or on menu return.
11. **Produce cosmetic art assets** -- Ball skin SVGs, effect particle sprites, trail sprites.

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `game-design-document.md` Section 7 | Design intent for cosmetic unlocks -- categories, examples, unlock methods |
| `save-and-persistence.md` Section 3 | `CosmeticState` data model and persistence structure |
| `save-and-persistence.md` Section 6 | Save triggers for cosmetic selection and unlock events |
| `menu-and-navigation-flow.md` Section 7 | Cosmetics overlay layout and navigation behavior (post-alpha) |
| `asset-registry.md` | File paths and production status for all cosmetic art assets |
| `audio-spec.md` | Impact and trail sounds are unaffected by cosmetic selection |
| `Services.kt` | `CosmeticState` data class and `ProfileData` integration |
