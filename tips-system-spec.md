# Street Soccer -- Tips System Spec

This document specifies the implementation of the rotating tips system described in `game-design-document.md` Section 10. Tips are a low-friction discovery mechanism that surfaces game mechanics and settings to players without requiring a tutorial. For persistence of dismissed tips, see `save-and-persistence.md` Section 3 (`ProfileData.dismissedTips`).

---

## 1. Design Goals

- **Contextual**: Tips appear when the player's behavior suggests they would benefit from the information
- **Non-intrusive**: Tips never block gameplay input and dismiss automatically
- **Non-repetitive**: Each tip is shown at most once per session unless the trigger condition recurs
- **Extensible**: New tips are added by appending entries to a data list, with no code changes

---

## 2. Tip Data Model

### TipData

```kotlin
data class TipData(
    val id: String,
    val text: String,
    val triggerCondition: TipTrigger,
    val priority: Int = 0
)
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Unique identifier (e.g., `"tip_trajectory_preview"`). Used for persistence in `ProfileData.dismissedTips` |
| `text` | `String` | The display text shown to the player. Supports simple **bold** markers for emphasis |
| `triggerCondition` | `TipTrigger` | A sealed class describing when this tip should appear (see Section 3) |
| `priority` | `Int` | Higher priority tips are evaluated first when multiple triggers fire simultaneously. Default is 0 |

### TipTrigger

```kotlin
sealed class TipTrigger {
    data class ConsecutiveMisses(val count: Int) : TipTrigger()
    data class KickCountWithout(val kickCount: Int, val condition: String) : TipTrigger()
    data class AfterEvent(val event: String) : TipTrigger()
    data class IdleTime(val seconds: Float) : TipTrigger()
    object FirstLaunch : TipTrigger()
}
```

| Trigger Type | Parameters | Description |
|-------------|------------|-------------|
| `ConsecutiveMisses` | `count` | Fires after N consecutive misses (streak = 0 for N kicks in a row) |
| `KickCountWithout` | `kickCount`, `condition` | Fires after N kicks without the player performing `condition` (e.g., "steer_swipe", "big_bomb") |
| `AfterEvent` | `event` | Fires after a specific game event (e.g., `"first_big_bomb"`, `"first_streak_2"`) |
| `IdleTime` | `seconds` | Fires after N seconds of inactivity in READY state (no slider movement, no flick) |
| `FirstLaunch` | (none) | Fires on the player's very first session (totalKicks == 0 in career stats) |

---

## 3. Trigger Conditions

### When Tips Appear

Tips are evaluated at specific moments, not every frame. The `TipSystem` checks triggers at the following events:

| Check Point | Game Moment | Triggers Evaluated |
|-------------|-------------|-------------------|
| **State entry: READY** | After each kick resolves (SCORING/IMPACT_MISSED -> READY) | `ConsecutiveMisses`, `KickCountWithout`, `AfterEvent` |
| **State entry: READY (first time)** | Session start (MAIN_MENU -> READY) | `FirstLaunch` |
| **Idle timer** | During READY state, once per second | `IdleTime` |

### Trigger Evaluation Rules

1. **Dismissed tips are skipped**: If `tip.id` is in `ProfileData.dismissedTips`, the tip is never shown again across sessions
2. **Session-shown tips are skipped**: If a tip has already been shown during the current session (even if not explicitly dismissed), it is not shown again until the trigger recurs in a later session
3. **One tip at a time**: If multiple triggers fire simultaneously, only the highest-priority tip is shown. Lower-priority tips remain eligible for future evaluation
4. **Cooldown between tips**: After a tip is displayed, no new tip can appear for at least 10 seconds (prevents tip spam during rough play stretches)
5. **Not during flight**: Tips never appear during BALL_IN_FLIGHT, SCORING, or IMPACT_MISSED states. If a trigger fires during these states, the tip is queued and shown on the next READY state entry

---

## 4. Starter Tips

These four tips are defined in GDD Section 10. They are loaded as the initial tip list.

| ID | Text | Trigger | Priority |
|----|------|---------|----------|
| `tip_trajectory_preview` | "Enable **Trajectory Preview** in Settings to see your ball's predicted path." | `ConsecutiveMisses(3)` or `FirstLaunch` | 10 |
| `tip_steer_swipe` | "Swipe while the ball is in the air to add **spin** and curve your shot!" | `AfterEvent("first_big_bomb")` or `KickCountWithout(10, "steer_swipe")` | 8 |
| `tip_streak_multiplier` | "Hit targets in a row to build a **streak multiplier** -- up to x3!" | `AfterEvent("first_streak_2")` | 5 |
| `tip_big_bomb` | "Aim a powerful flick straight up to send a **Big Bomb** down the central corridor for bonus points." | `KickCountWithout(20, "big_bomb")` | 6 |

### Trigger Details

**`tip_trajectory_preview`**: Shown on first launch (welcome/tutorial context) or after 3 consecutive misses (suggesting the player needs aiming help). Priority 10 ensures it appears first in both situations.

**`tip_steer_swipe`**: Shown after the player's first Big Bomb (they have achieved power but may not know about steering) or after 10 kicks with no steer input (suggesting they are not using mid-flight controls). Priority 8 makes it second-most important.

**`tip_big_bomb`**: Shown after 20 kicks without a Big Bomb attempt (the player has not discovered the mechanic). Priority 6 places it below steering tips since Big Bombs are an advanced technique.

**`tip_streak_multiplier`**: Shown after the player achieves their first streak of 2+ consecutive hits, reinforcing the positive behavior. Priority 5 is lowest since streak feedback is already visible in the HUD.

---

## 5. Display Behavior

### Visual Presentation

| Property | Value |
|----------|-------|
| **Position** | Top of screen, horizontally centered, below the session score display |
| **Width** | 80% of screen width (1536 px at 1920x1080 reference) |
| **Height** | Auto-sized to text content, approximately 60-80 px |
| **Background** | Semi-transparent dark overlay (black at 70% opacity) with rounded corners |
| **Text color** | White, with **bold** segments rendered in a brighter or larger style |
| **Font size** | Medium (readable at arm's length on a phone -- approximately 24 sp equivalent) |
| **Padding** | 16 px internal padding on all sides |
| **Z-order** | Above HUD elements, below pause overlay |

### Animation

| Phase | Duration | Behavior |
|-------|----------|----------|
| **Fade in** | 0.3 s | Banner slides down from above screen edge and fades in (alpha 0 -> 1) |
| **Display** | 3.0 s | Banner is fully visible and readable |
| **Fade out** | 0.5 s | Banner fades out (alpha 1 -> 0) and slides up |
| **Total lifetime** | 3.8 s | From first visible pixel to fully gone |

### Dismissal

| Method | Behavior |
|--------|----------|
| **Auto-fade** | After 3.0 seconds of display, the banner fades out automatically |
| **Tap to dismiss** | Tapping anywhere on the banner immediately starts the fade-out animation (0.5 s). The tap is consumed and does not propagate to gameplay input |
| **State change** | If the game state changes to BALL_IN_FLIGHT while a tip is visible, the banner fades out immediately (0.2 s quick fade) |

### Dismissal Persistence

When a tip is dismissed (either by auto-fade or tap), its `id` is added to `ProfileData.dismissedTips`. This persists across sessions, so the player never sees the same tip twice unless the tip list is updated with new entries.

---

## 6. Tip Rotation

### Round-Robin Algorithm

Tips rotate through the available (non-dismissed) list to ensure variety:

```
availableTips = allTips.filter { it.id !in dismissedTips && it.id !in sessionShownTips }
eligibleTips = availableTips.filter { evaluateTrigger(it.triggerCondition) == true }

if (eligibleTips.isNotEmpty()) {
    // Sort by priority (descending), then by list order for stability
    nextTip = eligibleTips.sortedByDescending { it.priority }.first()
    show(nextTip)
    sessionShownTips.add(nextTip.id)
}
```

### Rotation Rules

1. **Do not repeat until all shown**: Within a session, each eligible tip is shown at most once before any tip repeats. This is enforced by the `sessionShownTips` set
2. **Priority breaks ties**: When multiple tips become eligible simultaneously, priority determines which one is shown first. The remaining tips stay eligible for future trigger evaluations
3. **Session reset**: The `sessionShownTips` set is cleared at the start of each new session. However, permanently dismissed tips (in `ProfileData.dismissedTips`) remain excluded
4. **Empty list**: When all tips have been dismissed or shown this session, no tips appear. This is the expected end state for experienced players

---

## 7. Implementation Architecture

### TipSystem (ECS System)

`TipSystem` is an `EntitySystem` registered with the ktx-ashley Engine. It does not process entities (no component family). Instead, it monitors game events and state transitions to evaluate tip triggers.

```kotlin
class TipSystem(
    private val gameStateManager: GameStateManager,
    private val hudSystem: HudSystem,
    private val profileData: ProfileData,
    private val tips: List<TipData> = StarterTips.ALL
) : EntitySystem() {

    private val sessionShownTips = mutableSetOf<String>()
    private var consecutiveMisses = 0
    private var kicksWithoutSteer = 0
    private var kicksWithoutBigBomb = 0
    private var idleTimer = 0f
    private var cooldownTimer = 0f
    private var lastState: GameState? = null

    override fun update(deltaTime: Float) {
        // Track state transitions
        val currentState = gameStateManager.currentState
        if (currentState != lastState) {
            onStateTransition(lastState, currentState)
            lastState = currentState
        }

        // Update idle timer during READY
        if (currentState is GameState.Ready) {
            idleTimer += deltaTime
        }

        // Update cooldown
        if (cooldownTimer > 0f) {
            cooldownTimer -= deltaTime
        }

        // Check idle trigger
        if (currentState is GameState.Ready && cooldownTimer <= 0f) {
            checkIdleTrigger()
        }
    }
}
```

### Integration Points

| System | Integration | Direction |
|--------|------------|-----------|
| **GameStateManager** | TipSystem observes state transitions to track consecutive misses, kick counts, and events | TipSystem reads state |
| **HudSystem** | TipSystem requests tip display via `hudSystem.showTip(tipText)` | TipSystem -> HudSystem |
| **InputSystem** | TipSystem monitors whether steer swipes were used during a kick | TipSystem reads event flags |
| **CollisionSystem** | TipSystem receives hit/miss events to track consecutive misses | TipSystem reads event flags |
| **ProfileData** | TipSystem reads `dismissedTips` to filter and writes new dismissals | TipSystem reads/writes |

### Event Tracking

TipSystem maintains these counters, reset appropriately:

| Counter | Incremented When | Reset When |
|---------|-----------------|------------|
| `consecutiveMisses` | IMPACT_MISSED state entered | SCORING state entered (target hit) |
| `kicksWithoutSteer` | SCORING or IMPACT_MISSED entered and no steer swipe was detected during flight | A steer swipe is detected during any flight |
| `kicksWithoutBigBomb` | SCORING or IMPACT_MISSED entered and the kick was not a Big Bomb | A Big Bomb kick is detected |
| `idleTimer` | Each frame during READY state | Any touch input (slider drag or flick start) |

---

## 8. Extensibility

### Adding New Tips

To add a new tip, append a `TipData` entry to the tips list. No code changes are required beyond the data definition:

```kotlin
object StarterTips {
    val ALL = listOf(
        TipData(
            id = "tip_trajectory_preview",
            text = "Enable **Trajectory Preview** in Settings to see your ball's predicted path.",
            triggerCondition = TipTrigger.ConsecutiveMisses(3),
            priority = 10
        ),
        // ... existing tips ...

        // New tip — just add here
        TipData(
            id = "tip_slider_angle",
            text = "Use the **angle slider** on the left edge to control your kick's launch arc.",
            triggerCondition = TipTrigger.KickCountWithout(5, "slider_adjusted"),
            priority = 7
        )
    )
}
```

### Adding New Trigger Types

To support a new trigger condition:

1. Add a new case to the `TipTrigger` sealed class
2. Add evaluation logic in `TipSystem.evaluateTrigger()`
3. Add counter/tracking logic in `TipSystem.update()` or `onStateTransition()`

The sealed class ensures the compiler flags any unhandled trigger types.

### Future: External Tip Definitions

For seasonal content updates, tips could be loaded from a JSON file in the assets directory instead of compiled into the `StarterTips` object. The `TipData` data class is already structured for straightforward serialization:

```json
[
  {
    "id": "tip_halloween_pumpkins",
    "text": "Hit **pumpkin targets** on porches for bonus Halloween points!",
    "triggerCondition": { "type": "FirstLaunch" },
    "priority": 12
  }
]
```

This migration is non-breaking -- the `TipSystem` constructor accepts any `List<TipData>`, regardless of source.

---

## 9. Performance Considerations

| Concern | Mitigation |
|---------|------------|
| **Per-frame evaluation** | TipSystem.update() only checks idle timers during READY state. Trigger evaluation happens on state transitions (infrequent), not every frame |
| **String allocation** | Tip text strings are pre-allocated in the tips list. No string concatenation occurs during display |
| **HUD rendering** | Tip banner is a single Label actor in the HUD Stage. Creation/destruction uses Scene2D actor pooling if available; otherwise creates once and reuses |
| **Dismissal persistence** | `ProfileData.dismissedTips` is updated in memory immediately but written to disk only at session end or `onPause`, consistent with `save-and-persistence.md` Section 6 |

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `game-design-document.md` Section 10 | Design authority for tips content, display behavior, and design intent |
| `save-and-persistence.md` Section 3 | `ProfileData.dismissedTips` persistence model |
| `ui-hud-layout.md` | Screen positioning context for the tip banner |
| `state-machine.md` | Game states that govern when tips can appear |
| `menu-and-navigation-flow.md` | Tips in pause/settings menu context (future consideration) |
