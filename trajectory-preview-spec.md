# Street Soccer -- Trajectory Preview Spec

This document specifies the trajectory preview system that renders a dotted arc showing the ball's predicted flight path. The preview helps players visualize the relationship between their angle slider setting and the resulting ball arc. For the design intent, see `game-design-document.md` Section 3 (Trajectory Preview). For the underlying physics model, see `physics-and-tuning.md` Section 2. For the current implementation, see `core/src/main/kotlin/com/streetsoccer/ecs/systems/TrajectorySystem.kt`.

---

## 1. Overview

The trajectory preview is a **dotted arc** rendered on screen during the AIMING state. It shows an approximate predicted flight path based on the current angle slider position and a fixed mid-range power assumption. The preview is a **toggleable setting**, disabled by default, controlled by `trajectoryPreviewEnabled` in `SettingsData`.

### Key Behaviors

| Behavior | Detail |
|----------|--------|
| **Visible during** | AIMING and READY states only (when `trajectoryPreviewEnabled == true`) |
| **Hidden during** | BALL_IN_FLIGHT, SCORING, IMPACT_MISSED, PAUSED, MAIN_MENU |
| **Updates** | Every frame during AIMING -- recalculates as the angle slider moves |
| **Physics model** | Simplified: gravity + drag only. No Magnus effect (spin) in the preview |
| **Purpose** | Training aid -- helps players understand slider-to-arc relationship |

---

## 2. Arc Calculation

### Forward Physics Simulation

The trajectory preview uses a forward simulation of the ball flight model from `physics-and-tuning.md` Section 2, with the following simplifications:

| Full Physics Model | Preview Simplification | Reason |
|-------------------|----------------------|--------|
| Gravity + drag + Magnus + spin decay | Gravity + drag only | Spin is unknown before launch; Magnus would show a misleading straight arc. Drag is included because it noticeably affects arc shape at longer ranges |
| Variable power from flick | Fixed power (`PREVIEW_POWER = 0.5`) | The flick has not occurred yet during AIMING; the preview shows a "typical" mid-power kick to establish the arc shape |
| Variable direction from flick | Straight ahead (`direction = 0`) | The horizontal aim is unknown before the flick; the preview shows the arc firing straight into the scene |
| Full flight duration | Capped at `PREVIEW_STEPS` steps | The preview only needs to show the first ~1 second of flight, enough to communicate the arc shape |

### Simulation Pseudocode

```
sliderValue = inputRouter.sliderValue
launchAngle = MIN_ANGLE + sliderValue * (MAX_ANGLE - MIN_ANGLE)
launchAngleRad = toRadians(launchAngle)

horizontalSpeed = PREVIEW_POWER * MAX_KICK_SPEED
vx = 0                                          // no lateral component
vy = horizontalSpeed * cos(0)                    // straight into scene
vz = horizontalSpeed * sin(launchAngleRad)       // upward component from slider

px = PLAYER_ORIGIN_X                             // 960 (screen center)
py = PLAYER_ORIGIN_Y                             // 0 (bottom of screen)
pz = 0                                           // ground level

for step in 0 until PREVIEW_STEPS:
    // Gravity (vertical axis only)
    vz -= GRAVITY * PREVIEW_DT

    // Drag (all axes)
    vx *= (1 - DRAG * PREVIEW_DT)
    vy *= (1 - DRAG * PREVIEW_DT)
    vz *= (1 - DRAG * PREVIEW_DT)

    // Position update
    px += vx * PREVIEW_DT
    py += vy * PREVIEW_DT
    pz += vz * PREVIEW_DT

    // Ground hit -- stop simulation
    if pz < 0: break

    // Record dot position every DOT_SKIP-th step
    if step % DOT_SKIP == 0:
        drawDot(px, py + pz, DOT_RADIUS)
```

The screen position of each dot is `(px, py + pz)` -- the ground-plane position offset vertically by the ball's height, matching how the ball is rendered during actual flight.

### Simulation Constants

| Constant | Value | Notes |
|----------|-------|-------|
| `PREVIEW_STEPS` | 60 | Number of simulation ticks. At 1/60 s per tick, this covers 1 second of flight |
| `PREVIEW_DT` | 1/60 s | Matches the game's fixed timestep for consistent arc shape |
| `PREVIEW_POWER` | 0.5 | Mid-range power. Chosen so the preview arc is representative of a typical kick without implying a specific power level |
| `DOT_SKIP` | 3 | Draw every 3rd simulation step. With 60 steps, this produces up to 20 dot positions, of which 8-15 are typically visible before the arc hits the ground |

These constants are defined in `TrajectorySystem.companion` and read the shared tuning constants `GRAVITY`, `DRAG`, `MAX_KICK_SPEED`, `MIN_ANGLE`, and `MAX_ANGLE` from `TuningConstants`.

---

## 3. Visual Style

### Dot Appearance

| Property | Value |
|----------|-------|
| **Shape** | Filled circle |
| **Radius** | 4 px (`DOT_RADIUS`) -- visible but not obtrusive. Valid range: 3-6 px |
| **Color** | White at 40% opacity (`Color(1f, 1f, 1f, 0.4f)`) |
| **Opacity gradient** | Currently uniform. Future enhancement: dots could fade in opacity along the arc (100% at launch, 20% at arc end) to communicate decreasing prediction confidence |
| **Spacing** | Every 3rd simulation step (`DOT_SKIP = 3`). At the simulation rate, this produces dots spaced approximately 10-20 px apart at mid-power, giving a clean dotted-line appearance |
| **Count** | 8-15 visible dots for a typical mid-range kick. Varies with slider position: low angle shows more dots (flatter arc stays above ground longer in the near field), high angle shows fewer dots (steep arc rises quickly and descends behind the horizon) |

### Arc Characteristics by Slider Position

| Slider Position | Launch Angle | Arc Shape | Visible Dots |
|-----------------|-------------|-----------|-------------|
| 0.0 (bottom) | 10 deg | Low, flat trajectory -- dots stretch far into the scene | 12-15 |
| 0.25 | ~26 deg | Moderate rise, medium depth reach | 10-13 |
| 0.5 (default) | ~42.5 deg | Balanced arc -- moderate height and depth | 8-12 |
| 0.75 | ~59 deg | High arc with shorter ground reach | 8-10 |
| 1.0 (top) | 75 deg | Near-vertical lob -- dots rise steeply | 6-8 |

### Rendering Method

The trajectory preview uses `ShapeRenderer` (not `SpriteBatch`) to draw filled circles. This avoids creating a texture for the dots and keeps the rendering lightweight.

**Rendering sequence in the game loop:**

```
// 1. SpriteBatch rendering (background, sprites, shadows)
batch.begin()
engine.update(deltaTime)  // RenderSystem draws via batch
batch.end()

// 2. Trajectory preview (ShapeRenderer -- no SpriteBatch active)
trajectorySystem.renderTrajectory(camera)

// 3. HUD rendering (Stage)
hudSystem.update()
```

This ordering prevents the ShapeRenderer/SpriteBatch conflict documented in issue #26.

---

## 4. State Visibility

| Game State | Preview Visible | Notes |
|------------|:---------------:|-------|
| READY | Yes | Shows arc based on current slider position. Helps player decide angle before starting a flick |
| AIMING | Yes | Updates in real-time as slider moves. The arc responds immediately to slider adjustments |
| BALL_IN_FLIGHT | No | The real ball is now in flight -- showing a predicted path would be misleading since steer swipes alter the trajectory |
| SCORING | No | Post-kick feedback state |
| IMPACT_MISSED | No | Post-kick feedback state |
| PAUSED | No | Game is paused; no gameplay visuals update |
| MAIN_MENU | No | Not in gameplay |

### State Transition Behavior

- **READY -> AIMING**: Preview continues rendering (no visual change)
- **AIMING -> BALL_IN_FLIGHT**: Preview disappears instantly (no fade-out). The ball entity replaces it
- **SCORING/IMPACT_MISSED -> READY**: Preview reappears instantly (no fade-in) at the current slider position

---

## 5. Settings Integration

### SettingsData

The trajectory preview is controlled by `trajectoryPreviewEnabled` in `SettingsData`:

```kotlin
@Serializable
data class SettingsData(
    val trajectoryPreviewEnabled: Boolean = false,
    // ... other settings
)
```

**Default: `false` (disabled)**. This keeps the default experience clean for players who prefer to develop aim intuitively. Players who want help can enable it via the Settings overlay.

### Settings -> TrajectorySystem Wiring

When `LevelScreen` creates the `TrajectorySystem`, it reads the current `SettingsData.trajectoryPreviewEnabled` value and sets `trajectorySystem.trajectoryPreviewEnabled`. If the player changes the setting mid-session (via the pause menu settings), the new value should be applied immediately.

### Tips Integration

`tip_trajectory_preview` (see `tips-system-spec.md`) informs players that this setting exists. The tip triggers after 3 consecutive misses or on first launch, guiding struggling players toward the feature.

---

## 6. Current Implementation Notes

The trajectory preview is implemented in `TrajectorySystem.kt` (created in WP-17, fixed in WP-21).

### Architecture

- `TrajectorySystem` extends `EntitySystem` and implements `Disposable`
- The `update()` method is intentionally empty -- rendering is deferred to `renderTrajectory(camera)`, called explicitly by the game loop after `engine.update()` completes
- This deferred rendering pattern was adopted in WP-21 to fix issue #26 (ShapeRenderer/SpriteBatch conflict)
- The system owns a `ShapeRenderer` instance, created in the constructor and disposed in `dispose()`

### What the Implementation Does Correctly

- Forward-simulates gravity and drag from `TuningConstants`
- Uses the current `inputRouter.sliderValue` for real-time angle updates
- Stops simulation on ground collision (`pz < 0`)
- Respects `trajectoryPreviewEnabled` flag
- Only renders during AIMING and READY states
- Manages ShapeRenderer lifecycle (create/dispose)
- Renders outside SpriteBatch begin/end blocks

### Known Limitations

| Limitation | Impact | Future Fix |
|-----------|--------|------------|
| Fixed preview power (0.5) | Preview shows a mid-power arc regardless of how hard the player will actually flick. This is by design -- power is unknown before the flick -- but may confuse players who expect the preview to match their exact shot | Could fade dot opacity to communicate "this is approximate." Or during AIMING (after touchDown), use the current drag distance as a power hint |
| No lateral component | Preview always shows a straight-ahead arc. Angled flicks will produce a different trajectory | By design -- direction is unknown before release. The arc communicates vertical shape only |
| No depth scaling on dots | Dots do not shrink as they move toward the horizon. The actual ball does shrink via the depth scaling formula | Apply `max(0.05, (540 - py) / 540)` to DOT_RADIUS for dots deeper in the scene |
| Uniform dot opacity | All dots are the same opacity (40% white). No visual gradient communicating prediction confidence | Apply `opacity = 0.4 * (1.0 - step / PREVIEW_STEPS)` for a fade-along-arc effect |

---

## 7. Performance Considerations

| Concern | Budget | Current Implementation |
|---------|--------|----------------------|
| **Simulation steps** | Max 60 per frame | `PREVIEW_STEPS = 60`. Each step is 4 multiplications and 6 additions -- negligible CPU cost |
| **Draw calls** | Max 20 circles per frame | `DOT_SKIP = 3` with 60 steps = 20 potential dots. Actual count is lower (ground collision truncates the arc) |
| **ShapeRenderer overhead** | 1 begin/end pair per frame (AIMING only) | ShapeRenderer.begin(Filled) + N circle calls + end(). Batch overhead is minimal for <20 primitives |
| **Per-frame allocations** | 0 | All variables are stack-local primitives (float). No object allocation in the render loop. The `ARC_COLOR` is a companion object constant |
| **Memory** | ~256 bytes for ShapeRenderer | ShapeRenderer is pre-allocated in the constructor, not per-frame |
| **Active only during AIMING** | 0 cost during BALL_IN_FLIGHT | Early return in `renderTrajectory()` when state is not AIMING or READY |

### Performance Validation

The trajectory preview adds negligible overhead:
- **CPU**: 60 simple arithmetic operations + 20 circle draw calls = < 0.1 ms per frame
- **GPU**: 20 filled circles at 4 px radius = trivial fill cost
- **Memory**: ShapeRenderer instance (~256 bytes) + companion constants

No performance optimization is needed unless the dot count is increased significantly beyond 60 steps.

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `game-design-document.md` Section 3 | Design intent for trajectory preview as a toggleable training tool |
| `physics-and-tuning.md` Section 2 | Ball flight equations used in the forward simulation |
| `physics-and-tuning.md` Section 8 | Tuning constants (GRAVITY, DRAG, MAX_KICK_SPEED, MIN_ANGLE, MAX_ANGLE) consumed by the simulation |
| `ui-hud-layout.md` Section 9 | HUD-level properties of the trajectory preview |
| `input-system.md` Section 2 | Angle slider value that drives the preview arc |
| `save-and-persistence.md` Section 3 | `SettingsData.trajectoryPreviewEnabled` persistence |
| `tips-system-spec.md` | `tip_trajectory_preview` that promotes this feature to players |
