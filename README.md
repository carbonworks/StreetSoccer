# Street Soccer

A **2.5D arcade game** for Android built with LibGDX and Kotlin. The player stands at the bottom of a suburban intersection and kicks a soccer ball at targets -- windows, vehicles, drones -- using a three-input control system. The game uses a single-vanishing-point perspective with ball scaling, shadow projection, and Z-layer ordering to create the illusion of 3D flight over a flat 2D background.

## Status

**Alpha** -- Waves 1 through 7 of development. Core gameplay loop is functional: kick, steer, score, repeat. ECS architecture is in place with physics, input, collision, rendering, spawn, HUD, and trajectory preview systems. Save/load, settings, pause overlay, stats overlay, and audio (placeholder sounds) are implemented. Technical debt from earlier waves has been cleared.

## Game Concept

- **Stationary kicker** at the bottom of the screen
- **2.5D physics** -- balls travel "into" the scene, shrinking as they approach the horizon
- **Suburban crossroads** environment with townhomes, fences, roads, and a deep central alley
- **Target variety** -- static targets (windows, garage doors) and moving targets (vehicles, runners, drones)
- **Free play / sandbox** -- no lives, no timer, no shot limit. Engagement through streaks, personal bests, and cosmetic unlocks

## Core Mechanics

### Three-Input Kick Model

1. **Angle slider** -- a vertical rail along the screen edge controls launch angle (10--75 degrees). Low = line drive, high = lob arc
2. **Flick** -- touch, drag, and release in the play area to launch the ball. Swipe speed sets power; swipe direction sets horizontal aim
3. **Steer swipes** -- while the ball is in flight, swipe to add spin. Lateral swipes curve left/right; vertical swipes push deeper or pull back

### 2-Axis Steer with Diminishing Returns

Steer swipes apply spin on both X (lateral) and Y (depth) axes via the Magnus effect. A graduated 4-tier curve prevents unlimited steering: 1st swipe at full effect (x1.0), 2nd at x0.6, 3rd at x0.25, 4th+ at x0.1 (residual floor, no hard cap). The budget resets each kick.

### Ball Shadow

A dark ellipse on the ground plane below the ball. Position tracks the ball's ground coordinates; opacity fades with height (`SHADOW_FADE_HEIGHT` = 400 px). Provides simultaneous depth (position) and height (opacity) readouts during flight.

### Big Bomb

When kick power exceeds 90% of maximum AND the angle slider is set above 70%, the ball enters the central corridor (Z-layer 3) for distance-based scoring. A progressive red color ramp provides depth feedback. Big Bomb base points stack with the streak multiplier for the game's highest-scoring moments.

### Scoring & Streaks

Consecutive target hits build a streak multiplier: x1 -> x1.5 -> x2 -> x2.5 -> x3 (cap). A miss resets the streak. Points appear at the impact location and float upward while fading.

## Tech Stack

| Category | Technology |
|----------|-----------|
| **Platform** | Android (primary), Desktop (debug/test) |
| **Engine** | LibGDX 1.14.x |
| **Language** | Kotlin with LibKTX extensions |
| **ECS** | ktx-ashley (Entity-Component-System) |
| **Physics** | Box2D (collision detection only; flight physics computed in game-space) |
| **Vector Rendering** | AmanithSVG |
| **UI** | ktx-scene2d (HUD, menus, overlays) |
| **Serialization** | kotlinx.serialization (JSON save data) |
| **Target Resolution** | 1920x1080 (16:9) |

## Project Structure

```
StreetSoccer/
  core/                          # All game logic (~95% of codebase)
    src/main/kotlin/com/streetsoccer/
      GameBootstrapper.kt        # Application entry point (KtxGame)
      ecs/
        components/              # ECS components (Transform, Velocity, Spin, Visual, etc.)
        systems/                 # ECS systems (Physics, Collision, Render, Spawn, Input, HUD, Trajectory, Catcher)
      input/                     # InputRouter, FlickDetector, SteerDetector, AngleSliderController
      level/                     # GameLoop, ECSBootstrapper, LevelData
      physics/                   # TuningConstants, PhysicsContactListener
      rendering/                 # BackgroundRenderer
      screens/                   # LoadingScreen, AttractScreen, LevelScreen
      services/                  # SaveService, AudioService, ProfileData, SettingsData
      state/                     # GameStateManager, GameState sealed class
      ui/                        # PauseOverlay
  android/                       # Android launcher, manifest, lifecycle
  desktop/                       # Desktop launcher for development
  assets/                        # SVGs, sounds, level JSON, background images
```

## Building & Running

### Prerequisites

- JDK 17+
- Android SDK (API 26+)

### Desktop (Development)

```
./gradlew :desktop:run
```

### Android Debug APK

```
./gradlew :android:assembleDebug
```

The APK is output to `android/build/outputs/apk/debug/`.

### Android Release APK

```
./gradlew :android:assembleRelease
```

ProGuard/R8 minification is enabled for release builds.

## Documentation

### Design & Mechanics

| Document | Description |
|----------|-------------|
| `game-design-document.md` | **Design authority** -- core loop, kick mechanics, scoring, progression, seasonal variants, tips system |
| `game-mechanics-overview.md` | Concise mechanics summary -- quick-reference for the three-input model, flight physics, scoring |

### Technical Specs

| Document | Description |
|----------|-------------|
| `technical-architecture.md` | Architecture blueprint -- project structure, ECS, Box2D integration, game loop, rendering, state management, persistence |
| `input-system.md` | Touch input spec -- angle slider, flick detection, steer swipe detection with 2-axis diminishing returns |
| `physics-and-tuning.md` | Physics model -- flight equations, Magnus effect, drag, Big Bomb thresholds, 17 tuning constants |
| `environment-z-depth-and-collosion.md` | Z-layer architecture (5 layers), collision mapping, depth scaling formula, shadow rendering zones |
| `state-machine.md` | Game state definitions and transitions (BOOT through SCORING/IMPACT_MISSED) |
| `save-and-persistence.md` | JSON save system -- domain objects, session lifecycle, save triggers, schema versioning, error handling |

### UI & Navigation

| Document | Description |
|----------|-------------|
| `ui-hud-layout.md` | HUD element positions, sizing, and behavior -- score, streak, steer meter, slider, popups, Big Bomb feedback |
| `menu-and-navigation-flow.md` | Menu structure, screen flow, overlays, Android back button handling |

### Planning & Process

| Document | Description |
|----------|-------------|
| `tips-system-spec.md` | Tips system -- trigger conditions, display behavior, rotation, extensibility, 4 starter tips |
| `trajectory-preview-spec.md` | Trajectory preview -- dotted arc calculation, visual style, state visibility, performance |
| `testing-performance-plan.md` | Target devices, performance budgets, testing strategy, profiling scenarios |
| `accessibility-localization.md` | Colorblind considerations, touch target sizing, font scaling, text localization approach |
| `work-packages.md` | Parallel work packages for agent-based development with file ownership and dependencies |
| `backlog.md` | Outstanding and completed work items |

### Level Data

| File | Description |
|------|-------------|
| `suburban-crossroads.json` | Level definition -- static colliders, target sensors with point values, spawn lanes for moving targets |
| `background.jpg` | Level background -- flat-style suburban intersection illustration |
