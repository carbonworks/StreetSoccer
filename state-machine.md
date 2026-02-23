**This document defines the game state machine for Street Soccer.** It specifies every game state, the entry/exit actions for each, transition triggers, and which systems are active per state. The `GameStateManager` is the runtime implementation of this spec. For the class architecture that hosts it, see `technical-architecture.md`. For input routing per state, see `input-system.md` Section 5.

---

## 1. High-Level State Flow

```
BOOT → LOADING → MAIN_MENU → READY ⇄ AIMING → BALL_IN_FLIGHT → SCORING → READY
                                                               → IMPACT_MISSED → READY

Any gameplay state → PAUSED → (RESUME to pre-pause state) or (QUIT to MAIN_MENU)
```

---

## 2. Detailed State Definitions

### A. `BOOT` (Initialization)

- **Purpose:** Initial app startup.
- **Entry Actions:** Initialize LibGDX engine; set up `KtxAsync` coroutine context.
- **Exit Actions:** None.
- **Transitions:** Automatically to `LOADING` once the engine is ready.

### B. `LOADING` (Asset Acquisition)

- **Purpose:** Load the `background.jpg`, `suburban-crossroads.json`, and SVG assets via AmanithSVG.
- **Entry Actions:** Show progress bar; trigger `LevelLoader` to parse JSON and build Box2D static bodies.
- **Exit Actions:** Hide progress bar; dispose of temporary loading assets.
- **Transitions:** To `MAIN_MENU` when `AssetManager.update()` returns `true`.

### C. `MAIN_MENU` (UI State)

- **Purpose:** Await player engagement via the attract screen.
- **Entry Actions:** Render background image; display attract screen (TAP TO PLAY, icon bar); reset session-scoped values if a previous session existed.
- **Exit Actions:** Hide menu UI.
- **Transitions:** To `READY` when TAP TO PLAY is tapped. Session counters initialize to zero (see `save-and-persistence.md` Section 5).

### D. `READY` (Idle Gameplay)

- **Purpose:** The player is stationary, waiting to kick.
- **Active Systems:** Spawn lanes (drones, vehicles, runners) are moving. HUD is fully visible. Ball is at player origin.
- **Inactive Systems:** Ball physics (no ball in play).
- **Transitions:** To `AIMING` on `touchDown` in the main play area.

### E. `AIMING` (Input Handling)

- **Purpose:** Player is dragging to determine power and horizontal aim. The angle slider (visible on a side rail) sets the launch angle independently via multi-touch.
- **Entry Actions:** Begin tracking flick gesture; if trajectory preview is enabled, render predicted arc.
- **Updates:** Update `FlickDetector` position on each `touchDragged`. Update trajectory preview based on current drag and slider position. Check for Big Bomb threshold (if power > 90% and angle slider >= 70%).
- **Transitions:**
  - To `BALL_IN_FLIGHT` on `touchUp` (if flick speed >= `MIN_FLICK_SPEED`).
  - Back to `READY` if flick is cancelled (swipe speed below `MIN_FLICK_SPEED` threshold — see `input-system.md` Section 3).

### F. `BALL_IN_FLIGHT` (Physics Active)

- **Purpose:** The ball is traveling through the 2.5D environment.
- **Active Systems:** PhysicsSystem (gravity, drag, Magnus, spin decay, position update). Box2D world stepping (collision detection). Ball shadow rendering. Steer input (full-screen swipe zone).
- **Updates:**
  - Fixed-timestep physics simulation (accumulator pattern).
  - Ball shadow position tracks (x, y); opacity fades with height.
  - Depth scaling applied per frame.
  - If Big Bomb is active, ball color ramps toward red with corridor depth.
- **Mid-flight steering:** Swipe gestures apply spin to the ball via SteerDetector. Each swipe adds cumulative spin subject to the graduated diminishing curve `[1.0, 0.6, 0.25, 0.1]`. The state does not transition on steer input — the ball remains in flight.
- **Transitions:**
  - To `SCORING` if collision with a `target_sensor` (Box2D sensor fixture) occurs.
  - To `IMPACT_MISSED` if collision with a `static_collider` (wall/facade) occurs or the ball exits screen bounds.

### G. `SCORING` (Success Feedback)

- **Purpose:** Process a successful target hit and deliver visual/audio feedback.
- **Duration:** 1.0 second from entry to automatic transition.
- **Entry Actions (in order):**
  1. Remove ball from play (sprite disappears at impact point).
  2. Spawn impact particles appropriate to the target type (glass shatter for windows, thud ripple for garage doors, sparks for drones, dent/crumple for vehicles — per GDD Section 9).
  3. Play impact sound appropriate to the target type (crisp glass-break, metallic clang, electronic buzz, etc. — per GDD Section 9).
  4. Spawn score popup at impact location showing the final multiplied point value (per `ui-hud-layout.md` Section 6).
  5. Add multiplied points to session score: `base_points * streak_multiplier`.
  6. Increment streak counter; update multiplier badge color and tier (per `ui-hud-layout.md` Section 3).
- **Exit Actions:** None — all feedback and scoring updates happen on entry.
- **Active Systems:** Spawn lanes frozen (moving targets hold position). HUD visible (score and streak update on entry).
- **Transition:** After 1.0 s → `READY`. Ball resets to player origin at the start of READY.
- **Pause during SCORING:** Allowed. Transitions to PAUSED; animation timer freezes. Resumes on RESUME with remaining duration intact.

### H. `IMPACT_MISSED` (Miss Feedback)

- **Purpose:** Process a missed shot (wall/facade hit or out of bounds) and deliver feedback.
- **Duration:** 0.75 seconds from entry to automatic transition.
- **Entry Actions (in order):**
  1. Remove ball from play. For wall/facade hits, the ball sprite disappears at impact point. For out-of-bounds exits, the ball fades out over 0.3 s.
  2. Spawn surface-appropriate particles (dust puff for house facades, wood splinters for fences, fade-out for OOB — per GDD Section 9).
  3. Play surface-appropriate sound (dull thud for facades, wooden crack for fences, quiet whoosh for OOB — per GDD Section 9).
  4. Reset streak counter to 0; dim multiplier badge to gray with brief deflation animation (per `ui-hud-layout.md` Section 3).
  5. No score popup.
- **Exit Actions:** None — all feedback happens on entry.
- **Active Systems:** Spawn lanes frozen. HUD visible (streak resets on entry).
- **Transition:** After 0.75 s → `READY`. Ball resets to player origin at the start of READY.
- **Pause during IMPACT_MISSED:** Allowed, same freeze behavior as SCORING.
- **Clarification — bounces vs. misses:** Ground-plane bounces (asphalt, lawns) are handled by physics restitution during BALL_IN_FLIGHT and do NOT trigger IMPACT_MISSED. Only wall/facade collider hits and out-of-bounds exits trigger this state.

### I. `PAUSED` (Gameplay Frozen)

- **Purpose:** Freeze all gameplay and present the pause menu.
- **Entry Actions (in order):**
  1. Freeze physics — stop Box2D `world.step()` and `PhysicsSystem` updates.
  2. Freeze spawn lane movement — moving targets hold position.
  3. If ball is in flight, freeze ball at current position (do not discard — it resumes on RESUME).
  4. If in SCORING or IMPACT_MISSED, freeze the feedback timer.
  5. Dim game scene with semi-transparent dark overlay (per `menu-and-navigation-flow.md` Section 4).
  6. Show pause menu (RESUME / SETTINGS / QUIT).
  7. Mute SFX; reduce music volume (when audio system exists).
- **Exit Actions (RESUME):**
  1. Hide pause menu; remove dim overlay.
  2. Unfreeze physics, spawn lanes, and ball (if it was in flight).
  3. Unfreeze feedback timer (if paused during SCORING or IMPACT_MISSED).
  4. Restore HUD.
  5. Restore audio levels.
  6. Return to the pre-pause gameplay state (READY, AIMING, BALL_IN_FLIGHT, SCORING, or IMPACT_MISSED).
- **Exit Actions (QUIT):**
  1. Trigger session end: merge session counters into `CareerStats`, write `profile.json` (per `save-and-persistence.md` Section 5).
  2. Transition to `MAIN_MENU`.
- **Android Lifecycle:** If the OS fires `onPause` during any gameplay state, auto-transition to PAUSED. On OS resume, remain in PAUSED — require a manual RESUME tap.
- **Input:** Only pause menu buttons are interactive. All gameplay input (slider, flick, steer) is disabled.

> **Cross-reference:** Pause menu layout and button behavior are defined in `menu-and-navigation-flow.md` Section 4. Android back button behavior during PAUSED is defined in Section 8.

---

## 3. Transition Logic Table

| Current State | Trigger | Target State | Logic / Side Effects |
|---------------|---------|-------------|---------------------|
| `BOOT` | Engine ready | `LOADING` | Auto-advance |
| `LOADING` | `AssetManager.update() == true` | `MAIN_MENU` | Dispose loading assets |
| `MAIN_MENU` | TAP TO PLAY | `READY` | Initialize session counters to zero |
| `READY` | `touchDown` in play area | `AIMING` | Begin flick tracking |
| `AIMING` | `touchUp` (speed >= `MIN_FLICK_SPEED`) | `BALL_IN_FLIGHT` | Create ball entity; apply impulse from `FlickResult` |
| `AIMING` | `touchUp` (speed < `MIN_FLICK_SPEED`) | `READY` | Cancelled drag; no ball launched |
| `BALL_IN_FLIGHT` | `swipe` gesture | `BALL_IN_FLIGHT` | Apply spin force to ball; remain in flight |
| `BALL_IN_FLIGHT` | `ContactListener` — target sensor hit | `SCORING` | Fixture `isSensor == true` and entity has `TargetComponent` |
| `BALL_IN_FLIGHT` | `ContactListener` — static collider hit | `IMPACT_MISSED` | Non-sensor fixture; wall/facade collision |
| `BALL_IN_FLIGHT` | `Ball.isOutOfBounds()` | `IMPACT_MISSED` | Ball exited screen bounds |
| `SCORING` | Timer expires (1.0 s) | `READY` | Reset ball to player origin |
| `IMPACT_MISSED` | Timer expires (0.75 s) | `READY` | Reset ball to player origin |
| `READY` / `AIMING` / `BALL_IN_FLIGHT` | Pause icon tap or Android back | `PAUSED` | Freeze physics, spawns, and ball position |
| `SCORING` / `IMPACT_MISSED` | Pause icon tap or Android back | `PAUSED` | Freeze animation/feedback timer |
| `PAUSED` | RESUME button or Android back | Pre-pause state | Unfreeze all; resume from frozen position |
| `PAUSED` | QUIT button | `MAIN_MENU` | Session end — merge stats, write `profile.json` |

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `technical-architecture.md` | `GameStateManager` sealed class, system activity per state, game loop integration |
| `input-system.md` | State-gated input dispatch table (Section 5) — which input subsystems are active per state |
| `physics-and-tuning.md` | Physics model active during BALL_IN_FLIGHT; tuning constants (Section 8) |
| `ui-hud-layout.md` | HUD elements affected by state transitions: score popups (Section 6), streak badge (Section 3), pause icon (Section 7) |
| `save-and-persistence.md` | Session lifecycle (Section 5) and save triggers (Section 6) tied to state transitions |
| `menu-and-navigation-flow.md` | Menu UI layers hosted within MAIN_MENU and PAUSED states (Section 9) |
| `game-design-document.md` | Design intent for feedback, scoring, and game feel that the state machine implements |
