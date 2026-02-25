# Street Soccer — Parallel Work Packages

This file defines independent work packages for parallel agent development. Each package owns specific files to minimize merge conflicts. See `CLAUDE.md` "Parallel Agent Workflow" section for how to run them.

---

## How to Read This File

- **Status**: `ready` (can start now), `blocked` (dependency not met), `done` (merged to main)
- **Owns**: Files this package creates or heavily modifies. Only one package should own a given file.
- **Reads**: Files the agent needs to reference but should NOT modify.
- **Touches**: Files where this package adds a small, well-scoped change (e.g., a few lines). Merge conflicts here are expected and acceptable.
- **Depends on**: Other packages that must be merged first.

---

## Pipeline

1. Backlog items are prioritized in `backlog.md` (top = highest priority)
2. Take the highest-priority unassigned items from the backlog
3. Group them into a wave based on file ownership — items that don't
   conflict on owned files can run in the same wave
4. Each item becomes a WP with Owns/Reads/Touches/Depends-on
5. Each WP runs as its own worktree subagent
6. Each WP merges independently when done (own branch, own commit)
7. After all WPs in a wave merge, define the next wave from backlog

---

## Wave 3 (Done)

### WP-10: Pause Overlay Menu

**Status:** done
**Backlog item:** #1 — Pause overlay menu (bug)
**Owns:** `HudSystem.kt` (pause overlay logic), new `PauseOverlay.kt`
**Reads:** `state-machine.md`, `ui-hud-layout.md`, `menu-and-navigation-flow.md`
**Touches:** `LevelScreen.kt` (wire overlay into screen), `GameStateManager.kt` (pause/resume calls)
**Depends on:** none (WP-0–9 done)

**Scope:**
1. Create `PauseOverlay.kt` — a Scene2D overlay with Resume and Quit buttons
2. In `HudSystem.kt`, replace the current pause-hides-everything behavior:
   - Pause tap → `GameStateManager.transitionTo(PAUSED)` + show PauseOverlay
   - Resume tap → `GameStateManager.transitionTo(previousState)` + hide overlay
   - Quit → save session and return to AttractScreen
3. Wire the overlay into `LevelScreen` so it renders above the game
4. Android back button during gameplay triggers pause (per `menu-and-navigation-flow.md`)

**Acceptance:** Pressing pause shows an overlay with Resume/Quit. HUD elements remain visible (dimmed or behind overlay). Resume returns to prior state. Quit saves and exits to attract screen.

---

### WP-11: Save Session Integration

**Status:** done
**Backlog item:** #2 — Save session integration (integration)
**Owns:** `GameBootstrapper.kt` (lifecycle hooks)
**Reads:** `save-and-persistence.md`, `state-machine.md`
**Touches:** `LevelScreen.kt` (save on hide/pause), `Services.kt` (mergeInto call)
**Depends on:** none (WP-0–9 done)

**Scope:**
1. Wire `SaveService` session lifecycle into the game loop:
   - On session start (LevelScreen show): begin new session accumulator
   - On session end (quit to attract, app backgrounded): merge session stats into career via `ProfileData.mergeInto()`
   - On pause: save current state
2. In `GameBootstrapper`, add `dispose()` hook to flush pending saves
3. Load profile on app start, apply settings

**Acceptance:** Session scores accumulate into career stats. Data persists across app restart. Backgrounding the app triggers a save. Corrupted save file recovers gracefully.

---

### WP-12: Settings Overlay (Functional)

**Status:** done
**Backlog item:** #3 — Settings overlay (functional) (feature)
**Owns:** `AttractScreen.kt` (settings panel replacement)
**Reads:** `menu-and-navigation-flow.md`, `save-and-persistence.md`, `ui-hud-layout.md`
**Touches:** `Services.kt` (SettingsData getters/setters)
**Depends on:** none (WP-0–9 done)

**Scope:**
1. Replace the placeholder settings panel in `AttractScreen` with functional controls:
   - Trajectory preview toggle (on/off)
   - Slider side toggle (left/right) — UI only, handedness logic is a later WP
   - Music volume slider
   - SFX volume slider
2. Wire controls to `SettingsData` via `SaveService`
3. Changes save immediately (no "Apply" button needed)
4. Android back button closes the settings overlay

**Acceptance:** Settings panel shows real controls. Toggling/sliding updates SettingsData. Values persist after closing and reopening settings. Back button dismisses overlay.

---

### WP-13: Audio Implementation

**Status:** done
**Backlog item:** #5 — Audio implementation (feature)
**Owns:** new `AudioServiceImpl.kt`, new audio placeholder assets
**Reads:** `game-design-document.md` (Section 9), `save-and-persistence.md` (volume settings)
**Touches:** `Services.kt` (replace NoopAudioService), `CollisionSystem.kt` (play sounds on hit), `GameBootstrapper.kt` (init AudioServiceImpl)
**Depends on:** none (WP-0–9 done)

**Scope:**
1. Create `AudioServiceImpl` implementing the existing `AudioService` interface
2. Generate or source placeholder sound files (short beeps/clicks) for each of the ~12 GDD Section 9 cues:
   - Kick launch (bass boom), glass-break, metallic clang, car alarm, whoosh (flight), bounce, miss, streak milestone, Big Bomb activation, score popup, UI tap
3. Wire into `CollisionSystem`: play appropriate sound on target hit vs. wall hit
4. Wire into ball launch: play kick sound
5. Respect `SettingsData.sfxVolume` and `musicVolume`
6. Replace `NoopAudioService` registration in `Services.kt` / `GameBootstrapper.kt`

**Acceptance:** Sound effects play on kick, hit, and miss. Volume settings are respected. NoopAudioService is fully replaced. Placeholder sounds are clearly temporary but functional.

---

### WP-14: Ball Catcher NPC

**Status:** done
**Backlog item:** #7 — Ball Catcher NPC (gameplay)
**Owns:** new `CatcherComponent.kt`, new `CatcherSystem.kt`
**Reads:** `environment-z-depth-and-collosion.md`, `technical-architecture.md` (Section 4), `physics-and-tuning.md`
**Touches:** `CollisionSystem.kt` (catch handling branch), `LevelScreen.kt` (register CatcherSystem), `suburban-crossroads.json` (add catcher spawn point)
**Depends on:** none (WP-0–9 done)

**Scope:**
1. Create `CatcherComponent` — marks an entity as a ball catcher with position and catch radius
2. Create `CatcherSystem` — handles catcher idle animation and catch detection
3. Add a catcher spawn point to `suburban-crossroads.json` (center of intersection)
4. In `CollisionSystem`, add a branch: if ball contacts catcher entity, trigger a "caught" outcome (distinct from target hit or wall miss)
5. Create a placeholder SVG for the catcher character
6. Catcher stands in place (no movement for now)

**Acceptance:** A catcher NPC appears in the intersection. Ball contacting catcher triggers a catch event. Catch is visually and mechanically distinct from a target hit or wall miss.

---

## Wave 4 (Done)

### WP-15: Stats Overlay (Live Data)

**Status:** done
**Backlog item:** #4 — Stats overlay (live data)
**Owns:** `AttractScreen.kt` (stats panel replacement)
**Reads:** `menu-and-navigation-flow.md`, `save-and-persistence.md`
**Touches:** `Services.kt` (ProfileData getters)
**Depends on:** none (Wave 3 done)

**Scope:**
Replace the placeholder stats panel in AttractScreen with live ProfileData display: career score, best streak, best session score, total kicks, targets by type breakdown.

---

### WP-16: Bomb Mode Button

**Status:** done
**Backlog item:** #6 — Bomb Mode Button
**Owns:** `HudSystem.kt` (bomb button), `InputSystem.kt` (bomb mode flag)
**Reads:** `ui-hud-layout.md`, `input-system.md`, `physics-and-tuning.md`
**Touches:** `LevelScreen.kt` (register bomb mode)
**Depends on:** none (Wave 3 done)

**Scope:**
Add a red "bomb mode" button to the HUD. Player presses it before launching to activate powered-up kick. Visual feedback on press, zoom effect on ball launch.

---

### WP-17: Trajectory Preview Rendering

**Status:** done
**Backlog item:** #8 — Trajectory preview rendering
**Owns:** new `TrajectorySystem.kt`
**Reads:** `physics-and-tuning.md`, `ui-hud-layout.md`
**Touches:** `RenderSystem.kt` (draw preview arc), `LevelScreen.kt` (register system), `HudSystem.kt` (toggle UI)
**Depends on:** none (Wave 3 done)

**Scope:**
Toggleable dotted arc showing predicted ball path during AIMING state. Updates in real-time as angle slider and power change. Respects trajectoryPreviewEnabled setting.

---

### WP-18: Separate Buildings from Background

**Status:** done
**Backlog item:** #10 — Separate buildings from background
**Owns:** new `BackgroundRenderer.kt`, new layered background assets
**Reads:** `environment-z-depth-and-collosion.md`
**Touches:** `LevelScreen.kt` (multi-layer background rendering), `RenderSystem.kt` (layer ordering)
**Depends on:** none (Wave 3 done)

**Scope:**
Extract buildings into separate image layers from `background.jpg`. Keep roads in place. Curve the back road right. Separate land from sky for future sky replacement.

---

### WP-19: Flatten Front-Left Hill

**Status:** done (art documentation delivered; asset-only — actual image edit is manual)
**Backlog item:** #11 — Flatten front-left hill
**Owns:** modified background asset(s)
**Reads:** none
**Touches:** none
**Depends on:** none (Wave 3 done)

**Scope:**
Remove the hill in the front-left of the scene and replace it with flat grass. Asset-only change.

---

### WP-20: Bug Fix — Straight Kick Invisible Wall

**Status:** done
**Backlog item:** #1 — Straight kick hits invisible wall (bug)
**Owns:** `InputSystem.kt` (velocity calculation fix)
**Reads:** `physics-and-tuning.md`, `input-system.md`
**Touches:** none
**Depends on:** none

**Scope:**
Fix swapped sin/cos in ball velocity calculation. For a straight-up flick (direction ≈ PI/2), sin was used for vx and cos for vy — sending the ball fully lateral instead of forward. Corrected to `vx = cos(direction)`, `vy = sin(direction)`.

---

## Wave 4 Conflict Check

- AttractScreen: only WP-15
- HudSystem: only WP-16
- InputSystem: WP-16 (bomb mode flag), WP-20 (velocity fix) — different code sections, acceptable
- RenderSystem: WP-17 touches, WP-18 touches — different code paths (preview arc vs. layer ordering), acceptable
- LevelScreen: WP-16, WP-17, WP-18 all touch — additive registrations, acceptable
- Art assets: WP-18 and WP-19 both modify background layers but WP-19 is asset-only with no code overlap

**Excluded from Wave 4:** Big Bomb meteor feedback (conflicts with WP-17 on RenderSystem), Cosmetic system (too broad, conflicts with many files)

---

## Completed Work Packages

<details>
<summary>Wave 1 & Wave 2 — WP-0 through WP-9 (all done)</summary>

### WP-0: LevelScreen Game Loop Skeleton — FOUNDATION

**Status:** done
**Owns:** `LevelScreen.kt`
**Reads:** `technical-architecture.md` (Section 3, 7), `GameBootstrapper.kt`, all ECS system files
**Touches:** `GameBootstrapper.kt` (add KtxAsync init, SaveService, AudioService)

**Scope:**
Wire up `LevelScreen` as the core gameplay screen per tech-arch Section 3 and 7:
1. Create and configure the ktx-ashley `Engine` with all systems (PhysicsSystem, CollisionSystem, RenderSystem, SpawnSystem, InputSystem)
2. Create the Box2D `World` (zero gravity) and `PhysicsContactListener`
3. Create the `PhysicsAccumulator`
4. Create and set the `InputRouter` as the LibGDX input processor
5. Create the `SpriteBatch` and `Stage` (for HUD)
6. Implement the `render(delta)` game loop per tech-arch Section 7 pseudocode:
   - Poll input, accumulate time, fixed-step physics, state update, spawn update, sync Box2D, render, draw HUD
7. Add `dispose()` cleanup for batch, stage, world
8. In `GameBootstrapper.create()`: initialize `KtxAsync`, create `SaveService` and `NoopAudioService` instances

**Acceptance:** LevelScreen compiles with all systems registered (even though most systems are stubs). The game loop structure matches the tech-arch pseudocode. Desktop launcher reaches LevelScreen via touch-through from Loading → Attract → Level.

---

### WP-1: GameStateManager — State Transitions & Timers

**Status:** done
**Owns:** `GameStateManager.kt`
**Reads:** `state-machine.md`, `technical-architecture.md` (Section 9)
**Touches:** none

**Scope:**
Implement full state machine logic:
1. `transitionTo(newState)` with entry/exit actions for each state (per state-machine.md Section 2)
2. SCORING entry: start 1.0s timer, freeze spawns
3. IMPACT_MISSED entry: start 0.75s timer, reset streak, freeze spawns
4. `update(deltaTime)`: tick SCORING/IMPACT_MISSED timers, auto-transition to READY when expired
5. PAUSED: freeze/resume behavior, carry `previousState`
6. Expose `currentState` for InputRouter and system enable/disable checks

**Acceptance:** State transitions fire correct entry/exit actions. Timer states auto-advance to READY. Paused→resume returns to correct prior state.

---

### WP-2: CollisionSystem — Contact Routing & State Triggers

**Status:** done
**Owns:** `CollisionSystem.kt`
**Reads:** `PhysicsContactListener.kt`, `GameStateManager.kt`, `TargetComponent.kt`, `ColliderComponent.kt`, `technical-architecture.md` (Section 4, 5)
**Touches:** none

**Scope:**
1. Drain `collisionQueue` from PhysicsContactListener each frame
2. For each collision pair, inspect entity components:
   - Either entity has `TargetComponent` → calculate score, call `gameStateManager.transitionTo(Scoring)`
   - Either entity has `ColliderComponent` (non-sensor) → call `gameStateManager.transitionTo(ImpactMissed)`
3. Score calculation: `basePoints * depthMultiplier` (further targets worth more)
4. Track session score and streak counter (increment on target hit, reset on miss)
5. Remove ball entity from engine on collision

**Acceptance:** Target hits trigger SCORING with correct point value. Wall hits trigger IMPACT_MISSED. Ball entity is removed. Session score accumulates.

---

### WP-3: SpawnSystem — Moving Target Lifecycle

**Status:** done
**Owns:** `SpawnSystem.kt`
**Reads:** `SpawnLaneComponent.kt`, `suburban-crossroads.json`, `environment-z-depth-and-collosion.md`, `technical-architecture.md` (Section 4)
**Touches:** `LevelLoader.kt` (add spawn lane entity creation from JSON)

**Scope:**
1. Track per-lane spawn timers using `spawnIntervalSeconds`
2. When timer fires: create a new entity with Transform, Visual, Collider, Target, SpawnLane components
3. Each frame: translate entities along their lane direction at lane speed
4. Despawn entities that move off-screen (x < -100 or x > 2020 or y < -100 or y > 1180)
5. Freeze spawning during SCORING and IMPACT_MISSED states (check GameStateManager)
6. Parse spawn_lanes from suburban-crossroads.json in LevelLoader

**Acceptance:** Vehicles/drones spawn at intervals, move along lanes, and despawn off-screen. Spawning pauses during scoring/miss states.

---

### WP-4: InputSystem Bridge — Flick-to-Ball & Steer-to-Spin

**Status:** done
**Owns:** `InputSystem.kt`
**Reads:** `InputRouter.kt`, `FlickDetector.kt`, `SteerDetector.kt`, `AngleSliderController.kt`, `input-system.md`, `physics-and-tuning.md`
**Touches:** `InputRouter.kt` (add callback/result interface if needed)

**Scope:**
1. On valid flick (FlickResult from FlickDetector):
   - Create ball entity with Transform (player origin), Velocity (computed from power + angle + direction), Spin (initial zero), Visual (ball sprite), Collider (dynamic Box2D body)
   - Check Big Bomb thresholds (power >= 0.9, slider >= 0.7) → set special trajectory
2. During BALL_IN_FLIGHT, read SteerDetector results:
   - Apply deltaX/deltaY to ball's SpinComponent
3. Reset SteerDetector swipe counter on each new kick
4. Create ball shadow entity that tracks the ball

**Acceptance:** Flicking in the play area creates a ball entity with correct initial velocity. Steer swipes during flight modify ball spin. Big Bomb kicks are detected. Ball shadow follows the ball.

---

### WP-5: HUD System — Score, Streak, Steer Meter

**Status:** done
**Owns:** `HudSystem.kt`
**Reads:** `ui-hud-layout.md`, `technical-architecture.md` (Section 8), `GameStateManager.kt`
**Touches:** none

**Scope:**
1. Create a ktx-scene2d `Stage` with HUD actors per ui-hud-layout.md:
   - Session score (top-center)
   - Streak badge (below score, shows current streak count)
   - Steer budget meter (left edge, shows diminishing returns tier)
   - Pause icon (top-right corner)
   - Angle slider visual (left rail, reflects AngleSliderController value)
2. Score popups: spawn Label at impact world-space position, float upward while fading over 0.8s
3. Update HUD each frame based on GameStateManager state
4. Hide HUD during PAUSED state

**Acceptance:** Score displays and updates on hits. Streak increments/resets visually. Steer meter reflects swipe count. Pause icon is tappable. Score popups animate at hit locations.

---

### WP-6: LoadingScreen + Asset Pipeline

**Status:** done
**Owns:** `LoadingScreen.kt`
**Reads:** `technical-architecture.md` (Section 11), `LevelLoader.kt`
**Touches:** `GameBootstrapper.kt` (add AssetManager as shared resource)

**Scope:**
1. Initialize LibGDX `AssetManager`
2. Queue all SVG/texture assets for loading
3. Use `ktx-async` coroutines for non-blocking load with progress
4. Render a simple progress bar during loading
5. Auto-advance to AttractScreen when loading completes (no touch required)
6. Parse `suburban-crossroads.json` and prepare level data

**Acceptance:** LoadingScreen shows progress, auto-advances when done. Assets are available via AssetManager for all other systems. No touch needed to proceed.

---

### WP-7: AttractScreen + Menu Overlays

**Status:** done
**Owns:** `AttractScreen.kt`
**Reads:** `menu-and-navigation-flow.md`, `technical-architecture.md` (Section 3)
**Touches:** none

**Scope:**
1. Render background image (background.jpg)
2. "TAP TO PLAY" text with pulse animation
3. Icon bar (Settings gear, Stats chart icon)
4. Settings overlay: placeholder panel that opens/closes on gear tap
5. Stats overlay: placeholder panel that opens/closes on stats tap
6. Android back button: close overlay if open, otherwise no-op (attract screen is root)
7. Transition to LevelScreen on tap (outside overlay areas)

**Acceptance:** Attract screen renders background with tap-to-play. Settings/stats icons open placeholder overlays. Back button closes overlays. Tapping play area transitions to game.

---

### WP-8: SaveService Expansion

**Status:** done
**Owns:** `Services.kt`
**Reads:** `save-and-persistence.md`, `technical-architecture.md` (Section 10)
**Touches:** none

**Scope:**
1. Expand `ProfileData` to full spec: careerScore, careerKickCount, bestStreak, bestSessionScore, targetsByType map, dismissedTips set, unlockedCosmetics set, activeCosmetics map
2. Expand `SettingsData` to full spec: trajectoryPreviewEnabled, sliderSide, musicVolume, sfxVolume
3. Session lifecycle: accumulate session counters in memory, merge to career stats on session end
4. Atomic writes: write to temp file then rename
5. Schema versioning: version field in JSON, migration on load if version differs
6. Error handling: corrupted file → backup + reset to defaults

**Acceptance:** ProfileData/SettingsData match save-and-persistence.md spec. Save/load round-trips correctly. Corrupt file recovery works. Session merge logic accumulates correctly.

---

### WP-9: Ball Shadow Rendering

**Status:** done
**Owns:** `BallShadowComponent.kt`
**Reads:** `environment-z-depth-and-collosion.md`, `physics-and-tuning.md`, `technical-architecture.md` (Section 8)
**Touches:** `RenderSystem.kt` (add shadow drawing before ball sprite)

**Scope:**
1. Ball shadow is a dark ellipse drawn at ball's (x, y) ground position
2. Opacity: `shadowAlpha = max(0.1, 1.0 - ball.height / SHADOW_FADE_HEIGHT)`
3. Scale shadow ellipse with depth: use same screenScale formula
4. Draw shadow BEFORE the ball sprite (so ball overlaps it)
5. Shadow only exists during BALL_IN_FLIGHT

**Acceptance:** Shadow appears below ball during flight. Opacity fades as ball rises. Shadow scales with depth. Disappears when ball is removed.

---

### Waves 1–2 Dependency Graph

```
WP-0 (LevelScreen skeleton)  ──┬──> WP-1 (GameStateManager)
                                ├──> WP-2 (CollisionSystem)
                                ├──> WP-3 (SpawnSystem)
                                ├──> WP-4 (InputSystem)
                                ├──> WP-5 (HudSystem)
                                └──> WP-9 (Ball Shadow)

Independent (no dependencies):
  WP-6 (LoadingScreen)
  WP-7 (AttractScreen)
  WP-8 (SaveService)
```

</details>
