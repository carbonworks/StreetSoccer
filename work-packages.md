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

## WP-0: LevelScreen Game Loop Skeleton — FOUNDATION

**Status:** ready
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

**Why this is first:** Every other gameplay package needs LevelScreen to exist as the integration point. Systems are registered here, so agents working on individual systems know the interface contract.

**Acceptance:** LevelScreen compiles with all systems registered (even though most systems are stubs). The game loop structure matches the tech-arch pseudocode. Desktop launcher reaches LevelScreen via touch-through from Loading → Attract → Level.

---

## WP-1: GameStateManager — State Transitions & Timers

**Status:** blocked (depends on WP-0)
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

## WP-2: CollisionSystem — Contact Routing & State Triggers

**Status:** blocked (depends on WP-0)
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

## WP-3: SpawnSystem — Moving Target Lifecycle

**Status:** blocked (depends on WP-0)
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

## WP-4: InputSystem Bridge — Flick-to-Ball & Steer-to-Spin

**Status:** blocked (depends on WP-0)
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

## WP-5: HUD System — Score, Streak, Steer Meter

**Status:** blocked (depends on WP-0)
**Owns:** new file `core/.../ecs/systems/HudSystem.kt`
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

## WP-6: LoadingScreen + Asset Pipeline

**Status:** ready (no gameplay dependencies)
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

## WP-7: AttractScreen + Menu Overlays

**Status:** ready (no gameplay dependencies)
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

## WP-8: SaveService Expansion

**Status:** ready (no gameplay dependencies)
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

## WP-9: Ball Shadow Rendering

**Status:** blocked (depends on WP-0)
**Owns:** new file `core/.../ecs/components/BallShadowComponent.kt` (optional — could use tag on existing entity)
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

## Dependency Graph

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

## Recommended Execution Order

**Wave 1** (can all run in parallel):
- WP-0, WP-6, WP-7, WP-8

**Wave 2** (after WP-0 is merged; can all run in parallel):
- WP-1, WP-2, WP-3, WP-4, WP-5, WP-9

**Wave 3** (integration):
- Merge all Wave 2 branches, resolve conflicts in LevelScreen.kt if any
- Integration testing — verify the full loop works end-to-end
