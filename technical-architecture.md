**This document defines the technical architecture for Street Soccer.** It specifies the project structure, tech stack, class architecture, and runtime pipelines that implement the game's design. Each section references the authoritative spec document for that subsystem — this document consolidates the architecture into a single blueprint. For design intent, see `game-design-document.md`. For implementation status, see `backlog.md`.

---

## 1. Project Structure

The project uses a **Gradle multi-module layout** to separate platform-specific code from shared game logic.

| Module | Purpose |
|--------|---------|
| **`:core`** | All game logic, physics, input, rendering, and ECS systems. ~95% of the codebase. |
| **`:android`** | Android launcher, manifest, lifecycle handling (`onPause`/`onResume`). |
| **`:desktop`** | Desktop launcher for fast iteration and debugging without mobile deploys. |
| **`:assets`** | Raw SVGs, sounds, level JSON, and background images. |

---

## 2. Tech Stack

| Category | Library | Role |
|----------|---------|------|
| **Engine** | LibGDX 1.14.x | Core framework — rendering, input, asset management, lifecycle. |
| **Kotlin Bridge** | LibKTX | Kotlin extensions, DSLs, and coroutine support for LibGDX APIs. |
| **ECS** | ktx-ashley | Entity-Component-System framework for game entities (ball, targets, colliders, shadows). |
| **Physics** | ktx-box2d | Box2D wrapper for collision detection (static colliders and target sensors). |
| **UI** | ktx-scene2d | Scene graph for HUD overlays, menus, and score popups. |
| **Vector Rendering** | AmanithSVG | SVG rendering at any resolution for flat-style art assets. |
| **Async** | ktx-async | Kotlin coroutines for async asset loading and non-blocking operations. |
| **Serialization** | kotlinx.serialization | JSON encoding/decoding for save data and level files. |

---

## 3. Application Entry & Screen Architecture

### GameBootstrapper

`GameBootstrapper` extends `KtxGame` and serves as the application entry point. It initializes the coroutine context (`KtxAsync`), creates shared services (SaveService, AudioService), and manages screen transitions.

### Screens

| Screen | State(s) | Responsibility |
|--------|----------|----------------|
| **`LoadingScreen`** | BOOT, LOADING | Initialize engine, load assets via `ktx-async` coroutines, parse level JSON, build Box2D static bodies. Auto-advances to `AttractScreen` when complete. |
| **`AttractScreen`** | MAIN_MENU | Render the attract screen (background + TAP TO PLAY + icon bar). Host Stats and Settings overlays as UI layers. Transition to `LevelScreen` on play. |
| **`LevelScreen`** | READY, AIMING, BALL_IN_FLIGHT, SCORING, IMPACT_MISSED, PAUSED | The core gameplay screen. Manages the ECS engine, physics world, input routing, HUD, and the pause menu overlay. |

### Overlays vs. Screens

Secondary panels (Stats, Settings, Pause Menu, and future Cosmetics/Variant Select) are **UI layers within their parent screen**, not separate `KtxScreen` instances. Opening an overlay does not trigger a screen transition. At most one overlay is active at a time.

> **Cross-reference:** `menu-and-navigation-flow.md` Section 9 defines the full state-to-UI mapping and the overlay-within-screen pattern.

---

## 4. Entity-Component-System (ktx-ashley)

All game objects are ktx-ashley entities composed from a shared component vocabulary. Systems iterate over entities matching their component families each frame.

### Components

| Component | Fields | Purpose |
|-----------|--------|---------|
| **`TransformComponent`** | `x`, `y`, `height`, `screenScale` | World position, altitude above ground, and depth-derived visual scale. |
| **`VelocityComponent`** | `vx`, `vy`, `vz` | Per-frame velocity on lateral, depth, and vertical axes. |
| **`SpinComponent`** | `spinX`, `spinY` | Lateral and depth spin values for the Magnus effect. Both decay independently. |
| **`VisualComponent`** | SVG asset reference, tint/overlay color, opacity | Rendering data — what to draw and any color modifications (e.g., Big Bomb red ramp). |
| **`ColliderComponent`** | Box2D body reference, fixture type | Links the entity to its Box2D body for collision detection. |
| **`SpawnLaneComponent`** | lane ID, direction, speed range, spawn interval | Configuration for moving-target spawn lanes (cross-street, deep alley, sky). |
| **`TargetComponent`** | base points, target type ID | Marks an entity as a scoreable target. Type ID maps to `CareerStats.targetsByType`. |

### Systems

| System | Component Family | Responsibility |
|--------|-----------------|----------------|
| **`PhysicsSystem`** | Transform, Velocity, Spin | Apply gravity, drag, Magnus force, spin decay, and position update per fixed timestep. Runs only during BALL_IN_FLIGHT. |
| **`CollisionSystem`** | Collider, Target | Route Box2D `ContactListener` events. Target sensor hit → SCORING. Static collider hit or OOB → IMPACT_MISSED. |
| **`RenderSystem`** | Transform, Visual | Z-sorted drawing with depth scaling. Ball shadow drawn on ground plane below the ball sprite. |
| **`SpawnSystem`** | SpawnLane, Transform | Manage moving-target lifecycle: spawn, translate along lane, despawn when off-screen. Active during READY, AIMING, and BALL_IN_FLIGHT. |
| **`InputSystem`** | — | Delegates to `InputRouter` (Section 6). Reads gesture results and applies them to ball entities. |
| **`HudSystem`** | — | Update session score display, streak badge, steer budget meter, score popups. Uses ktx-scene2d actors. |

### Key Entity Templates

| Template | Components | Notes |
|----------|-----------|-------|
| **Ball** | Transform, Velocity, Spin, Visual, Collider | Created at player origin on each kick. Removed on SCORING or IMPACT_MISSED entry. |
| **StaticCollider** | Transform, Collider | House facades, fences. Built from `suburban-crossroads.json` geometry during LOADING. |
| **TargetSensor** | Transform, Collider, Target, Visual | Windows, garage doors. Box2D sensors (no physics response, trigger only). |
| **SpawnLaneTarget** | Transform, Collider, Target, Visual, SpawnLane | Vehicles, runners, drones. Managed by SpawnSystem. |
| **BallShadow** | Transform, Visual | Ground-plane ellipse. Position tracks ball's (x, y); opacity fades with ball height. |

---

## 5. Box2D Integration

Box2D is used **for collision detection only** — not for physics simulation. Gravity, drag, and Magnus forces are computed in game-space by `PhysicsSystem` (Section 4), giving full control over the 2.5D flight model.

### Coordinate Conversion

**PPM (Pixels Per Meter) = 100** — 100 game pixels = 1 Box2D meter.

| Game-Space Value | Pixels | Box2D (meters) |
|-----------------|:------:|:--------------:|
| Gravity | 980 px/s² | 9.8 m/s² |
| Max kick speed | 1200 px/s | 12 m/s |
| Screen width | 1920 px | 19.2 m |
| Screen height | 1080 px | 10.8 m |
| Fence height | 80 px | 0.8 m |

### World Setup

- **Gravity:** `Vector2(0f, 0f)` — zero-gravity Box2D world. Gravity is handled in game-space by `PhysicsSystem`.
- **Step rate:** Synced to the fixed timestep (1/60 s) via the accumulator pattern (Section 7).
- **Static bodies:** Built during LOADING from `suburban-crossroads.json` collider geometry. Converted from pixel coordinates to Box2D meters using PPM.
- **Dynamic body:** The ball. Its Box2D position is synced from game-space each physics frame (game-space is authoritative).

### ContactListener Pattern

```
ContactListener.beginContact(contact):
    fixtureA = contact.fixtureA
    fixtureB = contact.fixtureB

    entityA = fixtureA.body.userData as Entity
    entityB = fixtureB.body.userData as Entity

    if either entity has TargetComponent:
        → transition to SCORING (target hit)
    else if either entity has ColliderComponent (non-sensor):
        → transition to IMPACT_MISSED (wall/facade hit)
```

Each Box2D fixture's `userData` holds a reference to its ktx-ashley entity. The `CollisionSystem` inspects entity components to determine the collision outcome.

> **Cross-reference:** Collider geometry and restitution values are defined in `suburban-crossroads.json`. Bounce velocity equations are in `physics-and-tuning.md` Section 7.

---

## 6. Input Architecture

All touch input flows through a single `InputRouter` that implements LibGDX's `InputProcessor`. The router dispatches events based on two axes: **screen zone** (slider rail vs. play area vs. full screen) and **game state** (which subsystems are active).

### Input Classes

| Class | Responsibility |
|-------|---------------|
| **`InputRouter`** | Top-level dispatcher. Manages pointer-to-subsystem assignment. Enables/disables subsystems per game state. |
| **`AngleSliderController`** | Owns the slider value (0.0–1.0). Processes drag events on the slider rail. |
| **`FlickDetector`** | Records flick start/end, computes power and direction, produces `FlickResult`. |
| **`SteerDetector`** | Tracks multi-axis swipe movement during BALL_IN_FLIGHT. Computes spin deltas and applies diminishing returns. |

### State-Gated Dispatch

| Game State | Slider | Flick | Steer | UI |
|------------|:------:|:-----:|:-----:|:--:|
| READY | Active | Listening | — | Pause icon |
| AIMING | Active | Tracking | — | Pause icon |
| BALL_IN_FLIGHT | — | — | Active | Pause icon |
| SCORING | — | — | — | — |
| IMPACT_MISSED | — | — | — | — |
| PAUSED | — | — | — | Menu buttons |
| MAIN_MENU | — | — | — | Menu/overlay buttons |

> **Cross-reference:** `input-system.md` defines the full touch architecture, pointer tracking, zone boundaries, gesture detection, `FlickResult` data class, and the steer processing pseudocode.

---

## 7. Game Loop & Physics Pipeline

The game loop uses a **fixed-timestep accumulator pattern** to decouple physics from frame rate. LibGDX's variable `deltaTime` is accumulated and consumed in fixed increments.

### Per-Frame Pseudocode

```
render(deltaTime):
    // 1. Poll input
    inputRouter.processQueuedEvents()

    // 2. Accumulate time
    accumulator += deltaTime

    // 3. Fixed-step physics (only during BALL_IN_FLIGHT)
    if state == BALL_IN_FLIGHT:
        while accumulator >= FIXED_TIMESTEP:
            physicsSystem.update(FIXED_TIMESTEP)   // gravity, drag, Magnus, spin decay, position
            world.step(FIXED_TIMESTEP, 6, 2)       // Box2D collision detection
            collisionSystem.processContacts()       // route ContactListener results
            accumulator -= FIXED_TIMESTEP

    // 4. Update state machine (timers for SCORING/IMPACT_MISSED, spawn lanes)
    gameStateManager.update(deltaTime)
    spawnSystem.update(deltaTime)

    // 5. Sync Box2D positions from game-space (game-space is authoritative)
    syncBox2DPositions()

    // 6. Render (Z-sorted)
    renderSystem.update()    // depth-scaled sprites, ball shadow

    // 7. Draw HUD
    hudSystem.update()       // score, streak, popups, steer meter
```

> **Cross-reference:** `physics-and-tuning.md` Section 2 defines the per-step physics update (gravity, drag, Magnus, spin decay, position). Section 8 lists all tuning constants including `FIXED_TIMESTEP` (#13).

---

## 8. Rendering Pipeline

### Z-Layer Sort Order

Entities are rendered back-to-front using the 5-layer depth architecture:

| Layer | Name | Contents |
|:-----:|------|----------|
| 4 | Sky | Background sky, drone/aerial targets |
| 3 | Deep Neighborhood | Central corridor, distant townhomes, approaching runners |
| 2 | Primary Properties | House facades, windows, garage doors, fences |
| 1 | Cross-Street | Horizontal road, vehicle targets |
| 0 | Launch Zone | Foreground street, sidewalks, player origin |

Within each layer, entities are further sorted by Y-position (higher Y = further from camera = drawn first).

### Depth Scaling

All entities use the depth scaling formula: `screenScale = max(0.05, (540 - y) / 540)`. Entities shrink as they move toward the horizon (Y = 540 px). The 0.05 floor prevents entities from vanishing entirely.

### Ball Shadow

The ball shadow is a dark ellipse drawn on the ground plane below the ball sprite. Its position tracks the ball's (x, y) ground coordinates. Opacity fades with ball height: `shadowAlpha = max(0.1, 1.0 - ball.height / SHADOW_FADE_HEIGHT)`.

### HUD & Score Popups

HUD elements (score, streak badge, pause icon, steer meter) are rendered in a separate UI layer via ktx-scene2d, on top of the game world. Score popups spawn at impact points in world space and float upward while fading.

### SpriteBatch Approach

A single `SpriteBatch` handles all game-world rendering (Z-sorted sprites). A separate `Stage` (ktx-scene2d) handles HUD and menu overlays. Both share the same viewport for coordinate consistency.

> **Cross-reference:** `environment-z-depth-and-collosion.md` Section 1 defines the Z-layer architecture. `ui-hud-layout.md` defines all HUD element positions, sizes, and behaviors.

---

## 9. State Management

### GameStateManager

`GameStateManager` drives the game's finite state machine using a Kotlin `sealed class` hierarchy for type-safe state representation.

```
sealed class GameState {
    object Boot : GameState()
    object Loading : GameState()
    object MainMenu : GameState()
    object Ready : GameState()
    object Aiming : GameState()
    object BallInFlight : GameState()
    object Scoring : GameState()
    object ImpactMissed : GameState()
    data class Paused(val previousState: GameState) : GameState()
}
```

The `Paused` state carries a reference to the pre-pause state so gameplay can resume exactly where it left off.

### System Activity by State

| State | Physics | Spawns | Input | HUD | Timers |
|-------|:-------:|:------:|:-----:|:---:|:------:|
| READY | — | Active | Slider + Flick listen | Full | — |
| AIMING | — | Active | Slider + Flick track | Full | — |
| BALL_IN_FLIGHT | Active | Active | Steer | Full | — |
| SCORING | — | Frozen | — | Score update | 1.0 s → READY |
| IMPACT_MISSED | — | Frozen | — | Streak reset | 0.75 s → READY |
| PAUSED | Frozen | Frozen | Menu only | Hidden | Frozen |
| MAIN_MENU | — | — | Menu only | — | — |

> **Cross-reference:** `state-machine.md` defines all state definitions, entry/exit actions, transition triggers, and the full transition logic table.

---

## 10. Persistence

### SaveService

`SaveService` manages all reads and writes to persistent storage. It uses `Gdx.files.local()` for file access and `kotlinx.serialization` for JSON encoding/decoding.

### Data Files

| File | Domain Object | Write Cadence |
|------|--------------|---------------|
| `profile.json` | `ProfileData` (career stats, cosmetic state, variant state, dismissed tips) | Session end, unlock events, `onPause` |
| `settings.json` | `SettingsData` (trajectory preview, slider side, volume) | On each setting change, `onPause` |

### Session Lifecycle

Session-scoped values (score, streak, kick count) accumulate in memory only. At session end (return to MAIN_MENU or `onPause`), session counters are merged into `CareerStats` and written to `profile.json`.

No disk I/O occurs during active gameplay states (AIMING, BALL_IN_FLIGHT, SCORING, IMPACT_MISSED).

> **Cross-reference:** `save-and-persistence.md` defines the full persistence model — domain objects (Section 3), session lifecycle (Section 5), save triggers (Section 6), schema versioning (Section 7), and error handling (Section 8).

---

## 11. Asset Pipeline

### Loading

Assets are loaded asynchronously via `ktx-async` coroutines in `LoadingScreen`. A progress bar reflects `AssetManager.update()` progress.

### SVG Rendering

All visual assets use the flat 2.0 art style (GDD Section 1) rendered via AmanithSVG. SVGs are rasterized to textures at the target resolution during loading.

### Level Loading

`LevelLoader` parses `suburban-crossroads.json` and produces:
- Box2D static bodies for all collider geometry (facades, fences)
- Box2D sensor fixtures for all target zones (windows, garage doors)
- ktx-ashley entities for each collider/target/spawn-lane with appropriate components
- Spawn lane configurations for `SpawnSystem`

### AudioService

`AudioService` is an interface with methods mapped to GDD Section 9 triggers (glass-break, metallic clang, car alarm, bass boom, whoosh, etc.). The alpha build uses a no-op or placeholder implementation. Real audio assets will be wired in when the Audio Spec is completed.

> **Cross-reference:** `backlog.md` documents the Audio Spec and Asset Registry as post-first-pass items, with implementation notes for the placeholder approach.

---

## 12. Companion Documents

| Document | Covers |
|----------|--------|
| `game-design-document.md` | Design authority — core loop, scoring, progression, feedback, seasonal variants |
| `input-system.md` | Touch architecture, angle slider, flick detection, steer detection, `FlickResult`, state integration |
| `physics-and-tuning.md` | Flight equations, Magnus/drag models, Big Bomb thresholds, restitution, 17 tuning constants |
| `environment-z-depth-and-collosion.md` | Z-layer architecture, depth scaling formula, collision mapping, spawning coordinates |
| `state-machine.md` | Game states, entry/exit actions, transition triggers, system activity per state |
| `ui-hud-layout.md` | HUD element positions, sizing, behavior, score popups, Big Bomb meteor feedback |
| `save-and-persistence.md` | JSON storage, domain objects, session lifecycle, save triggers, schema versioning |
| `menu-and-navigation-flow.md` | Menu structure, attract screen, pause menu, overlays, Android back button, navigation flow |
| `suburban-crossroads.json` | Level data — collider geometry, target sensors, spawn lanes, restitution values |
| `backlog.md` | Outstanding work items and implementation notes |
