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

## Wave 5 — Debt Clearance (Per No-Forward-Debt Policy)

Per `no-forward-debt.md`, all P0/Critical items must be resolved before new features.
P1/Important items are included where they share file ownership with P0 fixes.

### WP-21: LevelScreen Physics & Rendering Fixes

**Status:** done
**Backlog items:** #26 (TrajectorySystem crash), #29 (double accumulation), #31 (spiral-of-death)
**Owns:** `LevelScreen.kt` (render loop restructure), `TrajectorySystem.kt` (render phase change), `PhysicsSystem.kt` (accumulator removal)
**Reads:** `physics-and-tuning.md`, `technical-architecture.md` (Section 7), `no-forward-debt.md`
**Touches:** none
**Depends on:** none

**Scope:**
1. **#26 — TrajectorySystem rendering crash:** Move trajectory preview rendering out of the ECS engine.update() call. TrajectorySystem should not render during RenderSystem's batch block. Options: (a) TrajectorySystem sets a flag and LevelScreen renders after engine.update(), or (b) TrajectorySystem manages its own render phase with proper batch/ShapeRenderer sequencing.
2. **#29 — Double physics accumulation:** Remove the internal accumulator from PhysicsSystem. LevelScreen's fixed-timestep loop is authoritative. PhysicsSystem should accept a deltaTime and apply it once per call, not maintain its own accumulator.
3. **#31 — Spiral-of-death protection:** Cap the accumulator in LevelScreen's fixed-step loop (e.g., `accumulator = minOf(accumulator, FIXED_TIMESTEP * 5)`) to prevent runaway iterations on frame hitches.

**Acceptance:** TrajectorySystem renders without crashing. Physics steps exactly once per fixed timestep (no double-stepping). Frame hitches during flight don't cause cascading stutter.

---

### WP-22: HudSystem & PauseOverlay Resource Fixes

**Status:** done
**Backlog items:** #27 (font leaks), #37 (batch state reset), #38 (popup font allocations)
**Owns:** `HudSystem.kt` (font lifecycle, batch reset), `PauseOverlay.kt` (font disposal)
**Reads:** `no-forward-debt.md`
**Touches:** none
**Depends on:** none

**Scope:**
1. **#27 — Font memory leaks:** Track all BitmapFont instances in HudSystem's `managedTextures` list (or a parallel `managedFonts` list) and dispose in `dispose()`. In PauseOverlay, add fonts to the managed disposal list.
2. **#37 — Batch state reset:** After `stage.draw()` in HudSystem.update(), reset batch color to white and blend function to standard alpha.
3. **#38 — Popup font allocations:** Replace per-popup `BitmapFont()` creation in `spawnScorePopup()` with a shared pre-allocated font instance.

**Acceptance:** No BitmapFont leaks over a session. Batch state is clean after HUD rendering. Score popups reuse a shared font.

---

### WP-23: Null-Safe ECS Accessors & Cached Mappers

**Status:** done
**Backlog items:** #28 (null-safe accessors), #30 (cached mappers)
**Owns:** `EntityExtensions.kt` (nullable accessors), `PhysicsSystem.kt` (mapper usage), `CollisionSystem.kt` (mapper usage)
**Reads:** `technical-architecture.md` (Section 4), `no-forward-debt.md`
**Touches:** `LevelScreen.kt` (syncBox2DPositions mapper usage), `CatcherSystem.kt` (already uses mappers — verify), `InputSystem.kt` (already uses extensions — verify)
**Depends on:** none

**Scope:**
1. **#28 — Null-safe accessors:** Change EntityExtensions properties to return nullable types (`TransformComponent?` instead of `TransformComponent`). Update all call sites to use safe calls (`?.`) or explicit checks.
2. **#30 — Cached mappers:** Replace all `entity.getComponent(T::class.java)` calls in PhysicsSystem, CollisionSystem, and LevelScreen.syncBox2DPositions() with cached `mapperFor<T>()` accessors from EntityExtensions.

**Acceptance:** No component access uses `getComponent()`. All extension properties return nullable types. No NPE risk from missing components.

---

### WP-24: InputRouter Slider Side & Magic Numbers

**Status:** done
**Backlog items:** #33 (slider side wiring), #34 (centralize magic numbers)
**Owns:** `InputRouter.kt` (slider side logic), `PhysicsContactListener.kt` (collision tolerance constant)
**Reads:** `input-system.md`, `no-forward-debt.md`
**Touches:** `TuningConstants.kt` (add new constants), `CollisionSystem.kt` (reference centralized bounds), `RenderSystem.kt` (reference centralized shadow dimensions)
**Depends on:** none

**Scope:**
1. **#33 — Slider side wiring:** InputRouter must read `SettingsData.sliderSide` and position the slider zone on the correct edge. Add a `sliderSide` parameter or settings reference to InputRouter.
2. **#34 — Centralize magic numbers:** Move scattered constants to TuningConstants or appropriate companion objects:
   - `yDiff < 40f` → `TuningConstants.DEPTH_COLLISION_TOLERANCE`
   - `SLIDER_ZONE_FRACTION` → stays in InputRouter but documented
   - CollisionSystem bounds (MIN_X, MAX_X, etc.) → `TuningConstants` or CollisionSystem companion
   - RenderSystem shadow dimensions → RenderSystem companion (already there, just verify)
   - Deduplicate HORIZON_Y (one source of truth in TuningConstants)

**Acceptance:** Slider side toggle in settings actually moves the slider. No magic numbers in method bodies — all named constants.

---

### WP-25: Build Config & Ball Entity Caching

**Status:** done
**Backlog items:** #35 (ProGuard/signing), #36 (cache ball reference)
**Owns:** `android/build.gradle.kts` (ProGuard, signing config)
**Reads:** `no-forward-debt.md`
**Touches:** `InputSystem.kt` (expose activeBallEntity getter — already exists), `CollisionSystem.kt` (use cached ball ref), `CatcherSystem.kt` (use cached ball ref)
**Depends on:** none

**Scope:**
1. **#35 — ProGuard/R8:** Enable `isMinifyEnabled = true` for release builds. Create `proguard-rules.pro` with keep rules for LibGDX, Box2D, KTX, and Kotlin reflection. Add a placeholder signing config block (commented out with instructions).
2. **#36 — Cache ball entity reference:** Instead of iterating all entities to find the ball each frame, have CollisionSystem and CatcherSystem obtain the ball entity from InputSystem's existing `getActiveBall()` method. Pass InputSystem reference or use a shared ball tracker.

**Acceptance:** `./gradlew assembleRelease` produces a minified APK. No linear ball-entity search in CollisionSystem or CatcherSystem.

---

## Wave 5 Conflict Check

- LevelScreen: only WP-21 (owns), WP-23 touches (syncBox2DPositions mapper change — small, additive)
- PhysicsSystem: WP-21 owns, WP-23 touches (mapper change) — both change PhysicsSystem but WP-21 restructures accumulator while WP-23 changes component access; separable
- HudSystem: only WP-22
- PauseOverlay: only WP-22
- EntityExtensions: only WP-23
- CollisionSystem: WP-23 touches (mappers), WP-24 touches (bounds constants), WP-25 touches (ball caching) — three WPs touch but different code sections
- InputRouter: only WP-24
- TuningConstants: only WP-24 (adds constants)
- build.gradle.kts: only WP-25
- CatcherSystem: WP-23 touches (verify), WP-25 touches (ball caching) — different code sections

**Acceptable overlaps:** CollisionSystem and CatcherSystem are touched by multiple WPs but in non-overlapping code sections (mapper usage vs. bounds constants vs. ball reference). Merge conflicts will be additive.

**Excluded from Wave 5:** Items #32 (LevelScreen decomposition — too risky alongside #26/#29/#31 which restructure the same file), #39-42 (P3/polish — deferred per policy).

---

## Wave 6 — Remaining Debt + Polish

Final debt clearance wave. All remaining P2/P3 items plus the LevelScreen decomposition.

### WP-26: LevelScreen Decomposition + Level JSON Integration

**Status:** ready
**Backlog items:** #32 (decompose LevelScreen), #40 (integrate level JSON)
**Owns:** `LevelScreen.kt` (restructure into thin wrapper), new `GameLoop.kt` (render/update coordination), new `ECSBootstrapper.kt` (engine + system setup + entity creation)
**Reads:** `technical-architecture.md` (Section 7), `no-forward-debt.md`
**Touches:** `LoadingScreen.kt` (expose level data for LevelScreen to consume)
**Depends on:** none

**Scope:**
1. **#32 — Decompose LevelScreen:** Extract from the 440-line god object into:
   - `GameLoop.kt` — owns the render/update cycle: background rendering, fixed-timestep physics loop, engine.update(), trajectory rendering, state machine update, Box2D sync, HUD wiring, pause overlay rendering. Receives all systems and managers as constructor params.
   - `ECSBootstrapper.kt` — creates and configures the Engine, registers all systems, creates the catcher entity, creates the InputMultiplexer. Returns a bundle of references (engine, systems, inputMultiplexer) for GameLoop.
   - `LevelScreen.kt` — thin KtxScreen wrapper: creates GameStateManager, SessionAccumulator, World, SpriteBatch, viewport, BackgroundRenderer, InputRouter, PauseOverlay. Delegates to ECSBootstrapper in `show()` and GameLoop in `render()`. Handles lifecycle (hide/pause/dispose).
2. **#40 — Level JSON integration:** Make LevelScreen read level data from `LoadingScreen.getLevelData()` (or equivalent) and pass catcher position, spawn lane config, and collider geometry to ECSBootstrapper instead of hardcoding (960, 400) etc.

**Acceptance:** LevelScreen is under 120 lines. GameLoop and ECSBootstrapper each have clear single responsibilities. Level JSON data drives entity creation. Build passes. Game plays identically to before.

---

### WP-27: Ball Entity Caching + Shadow Heuristic Removal

**Status:** ready
**Backlog items:** #36 (cache ball entity), #41 (remove shadow heuristic)
**Owns:** `CollisionSystem.kt` (cached ball ref), `CatcherSystem.kt` (cached ball ref), `RenderSystem.kt` (shadow rendering)
**Reads:** `EntityExtensions.kt`, `no-forward-debt.md`
**Touches:** `InputSystem.kt` (expose `activeBallEntity` getter)
**Depends on:** none

**Scope:**
1. **#36 — Cache ball entity reference:** Add a public `getActiveBall(): Entity?` method to InputSystem that returns the currently tracked ball entity (already stored as `activeBallEntity`). CollisionSystem and CatcherSystem should call this instead of iterating all entities with `velocityCmpMapper.has()`. Fall back to linear search only if `getActiveBall()` returns null.
2. **#41 — Remove shadow heuristic:** In RenderSystem, remove the fallback heuristic that detects shadows via `renderLayer == 1 + black tint`. Use `ballShadowCmpMapper.has(entity)` exclusively. Verify InputSystem always adds BallShadowComponent when creating the ball shadow entity.

**Acceptance:** CollisionSystem and CatcherSystem use cached ball reference. RenderSystem shadow rendering uses only BallShadowComponent — no heuristic fallback. Build passes.

---

### WP-28: Conditional Logging

**Status:** ready
**Backlog items:** #39 (reduce GC pressure from logging)
**Owns:** `GameBootstrapper.kt` (logging), `LoadingScreen.kt` (logging), `BackgroundRenderer.kt` (logging), `InputSystem.kt` (logging)
**Reads:** `no-forward-debt.md`
**Touches:** none
**Depends on:** none

**Scope:**
1. **#39 — Conditional logging:** Wrap all `Gdx.app.log()` calls that use string templates (`"value=$x"`) in conditional checks: `if (Gdx.app.logLevel >= Application.LOG_INFO)`. This prevents string allocation and concatenation when logging is disabled. Apply across GameBootstrapper.kt, LoadingScreen.kt, BackgroundRenderer.kt, and InputSystem.kt. Also check LevelScreen.kt and any other files with template-based log calls.

**Acceptance:** No string template allocations occur in log calls when logging is disabled. Build passes. Log output is unchanged when logging is enabled.

---

### WP-29: AndroidLauncher Lifecycle Overrides

**Status:** ready
**Backlog items:** #42 (lifecycle overrides)
**Owns:** `AndroidLauncher.kt`
**Reads:** `no-forward-debt.md`
**Touches:** none
**Depends on:** none

**Scope:**
1. **#42 — Lifecycle overrides:** Add explicit `onPause()`, `onResume()`, and `onDestroy()` overrides to AndroidLauncher. Each should call `super` and log a message via `Gdx.app.log()` (or `android.util.Log` if Gdx is not yet initialized in onDestroy). This makes lifecycle transitions visible in logcat for on-device debugging.

**Acceptance:** AndroidLauncher has all 4 lifecycle methods (onCreate, onPause, onResume, onDestroy). Each logs a message. Build passes. App launches and backgrounds correctly on device.

---

### Wave 6 Conflict Check

- LevelScreen.kt: only WP-26 (owns)
- CollisionSystem.kt: only WP-27 (owns)
- CatcherSystem.kt: only WP-27 (owns)
- RenderSystem.kt: only WP-27 (owns)
- InputSystem.kt: WP-27 touches (add getter), WP-28 touches (logging) — different code sections, acceptable
- LoadingScreen.kt: WP-26 touches (expose level data), WP-28 owns (logging) — different code sections, acceptable
- GameBootstrapper.kt: only WP-28 (owns)
- BackgroundRenderer.kt: only WP-28 (owns)
- AndroidLauncher.kt: only WP-29 (owns)

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
