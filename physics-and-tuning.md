**This document is the concrete implementation of the GDD's "physically grounded" mandate (GDD Section 3, Ball Flight Physics).** It defines the physics model, equations, and tuning constants that govern ball flight, spin, drag, and collision response. All values are suggested starting points with documented valid ranges — tuning for game feel is expected. Input values consumed here are produced by the input pipeline defined in `input-system.md`.

---

## 1. Coordinate System Reference

These spatial constants are restated from `environment-z-depth-and-collosion.md` and `suburban-crossroads.json` for quick reference. The physics model operates within this coordinate frame.

| Constant | Value | Source |
|----------|-------|--------|
| **Base resolution** | 1920 × 1080 px | `suburban-crossroads.json` `level_meta` |
| **Player origin** | (960, 0) — bottom center | `suburban-crossroads.json` `player_origin` |
| **Horizon Y** | 540 px (50% of screen height) | `suburban-crossroads.json` `horizon_y` |
| **Depth scaling formula** | `scale = max(0.05, (540 - y_position) / 540)` | Z-depth spec Section 3 |
| **Minimum scale** | 0.05 — ball never fully vanishes until behind a visual layer | Z-depth spec Section 3 |

The ball's screen position uses X for horizontal movement and Y for depth into the scene (Y increases = further from camera). A separate `height` value tracks the ball's altitude above the ground plane for the parabolic arc.

---

## 2. Ball Flight Model

The ball is subject to three forces each frame: **gravity**, **air resistance (drag)**, and the **Magnus effect (spin)**. The simulation uses a fixed timestep for deterministic behavior.

### Initial Velocity from Input

The `FlickResult` produced by the input system (see `input-system.md` Section 3) is converted into initial velocity components:

```
horizontalSpeed = power * MAX_KICK_SPEED
vx = horizontalSpeed * sin(direction)             // lateral velocity
vy = horizontalSpeed * cos(direction)              // depth velocity (into scene)
vz = horizontalSpeed * sin(launchAngle)            // vertical velocity (upward arc)
```

Where:
- `power` is the normalized flick strength (0.0–1.0)
- `direction` is the horizontal aim angle from the flick (radians)
- `launchAngle` is derived from the angle slider: `MIN_ANGLE + sliderValue * (MAX_ANGLE - MIN_ANGLE)` (see `input-system.md` Section 2)
- `MAX_KICK_SPEED` is a tuning constant (Section 7)

### Per-Frame Update (Pseudocode)

```
// Fixed timestep (recommended: 1/60 s)
dt = FIXED_TIMESTEP

// Gravity (acts on vertical axis only)
vz = vz - GRAVITY * dt

// Air resistance (acts on all velocity components)
vx = vx * (1 - DRAG * dt)
vy = vy * (1 - DRAG * dt)
vz = vz * (1 - DRAG * dt)

// Magnus effect — dual-axis (see Section 3)
ballSpeed = sqrt(vx * vx + vy * vy + vz * vz)
magnusForceX = spinX * ballSpeed * MAGNUS_COEFFICIENT
magnusForceY = spinY * ballSpeed * MAGNUS_COEFFICIENT
vx = vx + magnusForceX * dt
vy = vy + magnusForceY * dt

// Spin decay (both axes independently)
spinX = spinX * (1 - SPIN_DECAY * dt)
spinY = spinY * (1 - SPIN_DECAY * dt)

// Position update
ball.x = ball.x + vx * dt
ball.y = ball.y + vy * dt
ball.height = ball.height + vz * dt

// Ground collision check
if ball.height <= 0:
    ball.height = 0
    handle_surface_collision()

// Compute screen scale from depth
ball.screenScale = max(0.05, (540 - ball.y) / 540)
```

> **Implementation note:** Use a fixed timestep (e.g., 1/60 s) with an accumulator pattern to decouple physics from frame rate. LibGDX's `Gdx.graphics.deltaTime` varies per frame; accumulate it and step physics in fixed increments for deterministic results.

---

## 3. Spin & Magnus Effect

Spin is the core mid-flight mechanic. It transforms a launched ball's straight-line trajectory into a curve, rewarding players who layer steer swipes onto their initial flick.

### Spin Model

| Property | Detail |
|----------|--------|
| **Data type** | Two signed `Float` values: `spinX` (lateral) and `spinY` (depth) |
| **spinX** | Positive = rightward lateral curve, negative = leftward lateral curve |
| **spinY** | Positive = deeper into scene (depth curve), negative = shallower (pulls ball back) |
| **Initial value** | Both `0.0` at launch (no inherent spin from the flick) |
| **Accumulation** | Each steer swipe adds `spinDeltaX` and `spinDeltaY` to the current spin values, subject to the 4-swipe diminishing multiplier (see `input-system.md` Section 4) |
| **Decay formula** | `spinX = spinX * (1 - SPIN_DECAY * dt)` and `spinY = spinY * (1 - SPIN_DECAY * dt)` — both components decay independently via exponential decay toward zero |
| **Decay rate** | `SPIN_DECAY` at 2.0/s: each spin component halves roughly every 0.35 seconds |

### Magnus Force

The Magnus effect produces forces on both lateral and depth axes based on the ball's spin components:

```
ballSpeed = sqrt(vx^2 + vy^2 + vz^2)
magnusForceX = spinX * ballSpeed * MAGNUS_COEFFICIENT
magnusForceY = spinY * ballSpeed * MAGNUS_COEFFICIENT
vx += magnusForceX * dt
vy += magnusForceY * dt
```

`magnusForceX` curves the ball left or right (lateral); `magnusForceY` pushes the ball deeper into the scene or pulls it back (depth).

| Behavior | Explanation |
|----------|-------------|
| **Fast ball + heavy lateral spin** | Dramatic lateral curve — the Magnus force scales with ball speed, so a powerful kick with accumulated spin produces a visible bend |
| **Fast ball + heavy depth spin** | Ball extends or shortens its depth travel — a deep swipe pushes the ball further toward the vanishing point, a pull swipe reins it back |
| **Slow ball + heavy spin** | Gentle drift — as the ball decelerates, spin has less effect |
| **Fast ball + no spin** | Straight-line trajectory — no steer input means no lateral or depth force |
| **Spin without new input** | The existing spin decays over time, producing a curve that gradually straightens out |

> **Tuning note:** `MAGNUS_COEFFICIENT` is the most feel-sensitive constant in the system. Too low and steer swipes feel pointless; too high and the ball becomes uncontrollable. Start at 0.0003 and adjust in small increments (±0.0001).

---

## 4. Air Resistance (Drag)

Drag applies a speed-proportional deceleration to all velocity components each frame:

```
vx = vx * (1 - DRAG * dt)
vy = vy * (1 - DRAG * dt)
vz = vz * (1 - DRAG * dt)
```

At `DRAG = 0.15/s`, the ball retains approximately **86% of its speed after 1 second** of flight (`(1 - 0.15)^60 ≈ 0.86` at 60 fps with `dt = 1/60`). This provides a subtle but meaningful deceleration:

- Short shots (< 1 second flight) are barely affected
- Long Big Bomb arcs (2–3 seconds) lose noticeable speed, making distant targets require genuinely powerful flicks
- Drag also affects vertical velocity, slightly shortening the arc compared to a vacuum trajectory

> **Tuning note:** If long shots feel like they "die" mid-flight, reduce DRAG. If weak flicks travel too far, increase it. The sweet spot is where a max-power Big Bomb still reaches the deep corridor but a half-power kick clearly falls short.

---

## 5. Ball Shadow

The ball casts a shadow onto the ground plane to help the player gauge both depth (how far) and height (how high) at a glance.

| Property | Detail |
|----------|--------|
| **Position** | The ball's (x, y) ground-plane coordinates — i.e., where the ball would be if its height were 0. The shadow stays "stuck" to the ground directly below the ball. |
| **Scale** | Follows the same depth scaling formula as the ball: `max(0.05, (540 - ball.y) / 540)`. The shadow shrinks as the ball travels deeper into the scene, matching the ball's visual size. |
| **Opacity** | Inversely proportional to the ball's height above the ground. Full opacity when the ball is at ground level; fades as the ball rises. Suggested formula: `shadowAlpha = max(0.1, 1.0 - ball.height / SHADOW_FADE_HEIGHT)`. The minimum of 0.1 ensures the shadow never fully disappears during flight. |
| **Shape** | A dark ellipse — simple and readable at all scales. |
| **Rendering** | Drawn on the ground plane (below the ball sprite in the render order), within the "floor" zones defined in `environment-z-depth-and-collosion.md` Section 2A. |

> **Design note:** The shadow provides two simultaneous readouts: its *position* tells the player where the ball is over the ground (depth), and its *opacity* tells the player how high the ball is. Together, they make mid-flight steering more intuitive — the player can see where a steer correction will land the ball.

---

## 6. Big Bomb Conditions

A Big Bomb occurs when the player delivers a high-power, high-angle kick that sends the ball deep into the central corridor (Z-layer 3). It is the game's peak risk/reward moment.

### Dual-Threshold Activation

Both conditions must be met simultaneously:

| Condition | Threshold | From |
|-----------|-----------|------|
| **Kick power** | `power >= 0.9` (90% of maximum) | `FlickResult.power` from `input-system.md` Section 3 |
| **Angle slider** | `sliderValue >= 0.7` (70% of range, ≈55.5°) | `FlickResult.sliderValue` from `input-system.md` Section 2 |

When both thresholds are met, the ball enters the Big Bomb flight mode:

- The ball trajectory aims toward the central corridor (Z-layer 3)
- The screen-edge light flash and bass "boom" feedback trigger (GDD Section 9)
- Distance scoring activates

### Distance Scoring

| Property | Value |
|----------|-------|
| **Scoring method** | **1 point per Y-unit** of maximum depth reached relative to player origin |
| **Expected base points** | 200–500 points for a full-depth Big Bomb, depending on power and angle |
| **Multiplier stacking** | Big Bomb base points are multiplied by the current streak multiplier (GDD Section 5) |
| **Measurement** | Track the ball's maximum `ball.y` value during flight; score = `max_y - player_origin_y` |

> **Design note (from GDD):** Big Bombs require a precise upward flick to thread the central corridor without hitting the flanking house facades. A slight angle error means a wall hit (miss + streak reset), but a clean shot at high streak is the game's peak scoring moment.

---

## 7. Surface Restitution

When the ball collides with a surface, the bounce intensity is governed by the surface's restitution coefficient. These values are defined in `suburban-crossroads.json` for the base level:

| Surface | Restitution | Collision ID | Behavior |
|---------|:-----------:|--------------|----------|
| **Asphalt (ground)** | 0.3 | Ground plane | Moderate bounce; ball loses most vertical energy |
| **Fences** | 0.4 | `left_fence`, `right_fence` | Slightly bouncier; ball deflects over if `height > fence_height` |
| **House facades** | 0.2 | `left_house_facade`, `right_house_facade` | Low bounce; ball thuds against the wall and drops (miss + streak reset) |
| **Out of bounds** | 0.0 | Screen boundary | No bounce; ball is removed and reset to player origin |

Bounce velocity calculation:

```
vz_after = -vz_before * restitution
vx_after = vx_before * restitution    // lateral energy also reduced
vy_after = vy_before * restitution    // depth energy also reduced
```

> **Implementation note:** Fence colliders have a defined height (80 px per `suburban-crossroads.json`). If `ball.height > fence_visual_height` at the moment the ball's ground-plane position intersects the fence collider, the ball passes over cleanly. Otherwise, it bounces.

---

## 8. Tuning Constants Summary

All physics constants in one table. Start with the suggested values, then tune using the methodology in Section 10.

| # | Constant | Suggested Value | Valid Range | Affects |
|---|----------|:-:|:-:|---------|
| 1 | `GRAVITY` | 980 px/s² | 600–1400 | Arc height and hang time; higher = shorter arcs |
| 2 | `MAX_KICK_SPEED` | 1200 px/s | 800–1600 | Maximum initial velocity at power = 1.0 |
| 3 | `DRAG` | 0.15 /s | 0.05–0.40 | Speed decay over flight; higher = ball slows faster |
| 4 | `MAGNUS_COEFFICIENT` | 0.0003 | 0.0001–0.001 | Spin-to-curve responsiveness; higher = more dramatic curves |
| 5 | `SPIN_DECAY` | 2.0 /s | 0.5–5.0 | How quickly spin bleeds off; higher = spin fades faster |
| 6 | `STEER_SENSITIVITY` | 0.005 | 0.001–0.02 | Swipe-to-spin conversion; higher = more spin per swipe |
| 7 | `MIN_FLICK_SPEED` | 200 px/s | 100–400 | Minimum swipe speed to register as a kick (below = cancelled drag) |
| 8 | `MAX_FLICK_SPEED` | 2000 px/s | 1200–3000 | Swipe speed that produces power = 1.0 |
| 9 | `MIN_ANGLE` | 10° | 5–20 | Lowest launch angle (slider at 0.0) |
| 10 | `MAX_ANGLE` | 75° | 60–85 | Highest launch angle (slider at 1.0) |
| 11 | `BIG_BOMB_POWER_THRESHOLD` | 0.9 | 0.8–0.95 | Minimum power for Big Bomb activation |
| 12 | `BIG_BOMB_SLIDER_THRESHOLD` | 0.7 | 0.6–0.85 | Minimum slider value for Big Bomb activation |
| 13 | `FIXED_TIMESTEP` | 1/60 s | 1/30–1/120 | Physics step interval; smaller = more accurate but costlier |
| 14 | `STEER_DIMINISH_CURVE` | [1.0, 1.0, 0.25, 0.0] | — | Multiplier per swipe index (0-based); swipes beyond index 3 use the last value (0.0) |
| 15 | `SHADOW_FADE_HEIGHT` | 400 px | 200–600 | Ball height at which the shadow reaches minimum opacity (0.1) |

---

## 9. Flight Duration Reference

Expected flight times for common shot types, using the suggested constants from Section 8. Use these as sanity checks during tuning — if actual values diverge significantly, investigate which constant is off.

| Shot Type | Power | Slider | Approx. Launch Angle | Expected Flight Time | Expected Behavior |
|-----------|:-----:|:------:|:--------------------:|:--------------------:|-------------------|
| **Low line drive** | 0.8 | 0.1 | ≈16° | 0.4–0.7 s | Fast, flat shot at a garage door or cross-street vehicle |
| **Medium line drive** | 0.6 | 0.3 | ≈30° | 0.8–1.2 s | Moderate arc toward primary property windows |
| **Mid-arc lob** | 0.7 | 0.5 | ≈42° | 1.2–1.8 s | Standard arc with good steer window; bread-and-butter shot |
| **Big Bomb** | 1.0 | 0.85 | ≈65° | 2.0–3.0 s | High, deep arc into central corridor; maximum steer opportunity |
| **Weak lob** | 0.3 | 0.8 | ≈62° | 0.8–1.2 s | High angle but low power; drops short — a "whiff" |

> **Key check:** A max-power Big Bomb should produce 2–3 seconds of hang time (GDD Section 3: "a real long-range kick"). If it's under 1.5 s, GRAVITY is too high or MAX_KICK_SPEED is too low. If it exceeds 4 s, the inverse is true.

---

## 10. Tuning Methodology

A step-by-step guide for dialing in game feel after initial implementation.

### Step 1 — Start with defaults

Load all constants from Section 8 at their suggested values. Do not pre-optimize.

### Step 2 — Enable trajectory preview

Turn on the dotted-arc trajectory preview (GDD Section 3, Trajectory Preview). This provides immediate visual feedback for how constants affect the ball's predicted path before it launches.

### Step 3 — Test the five reference shots

Execute each shot from Section 9 and compare actual flight time and landing position against the expected values. Note which shots feel wrong.

### Step 4 — Adjust one constant at a time

Change only one constant per test iteration. This isolates cause and effect:

- **Arcs too flat or too steep?** Adjust `GRAVITY`.
- **Ball doesn't reach deep targets?** Increase `MAX_KICK_SPEED` or decrease `DRAG`.
- **Spin feels unresponsive?** Increase `STEER_SENSITIVITY` or `MAGNUS_COEFFICIENT`.
- **Spin curves are too wild?** Decrease `MAGNUS_COEFFICIENT` or increase `SPIN_DECAY`.
- **Big Bombs too easy to trigger?** Raise `BIG_BOMB_POWER_THRESHOLD` or `BIG_BOMB_SLIDER_THRESHOLD`.
- **Flicks registering accidentally?** Raise `MIN_FLICK_SPEED`.

### Step 5 — Compare against flight duration reference

After each adjustment, re-test the five reference shots. The flight duration table in Section 9 is the primary sanity check — if times fall within the expected ranges and the ball *feels* right, the constants are in a good zone.

### Step 6 — Iterate

Repeat Steps 4–5 until all five reference shots feel satisfying. Then play freely for several minutes and make final micro-adjustments based on overall game feel.

> **Tuning note:** Resist the temptation to tune multiple constants simultaneously. Coupled changes make it impossible to attribute improvements or regressions to a specific cause. Patience here saves time overall.

---

## Companion Documents

- **`input-system.md`** — Defines the input pipeline that produces `FlickResult` and spin values consumed by the equations in this document (Sections 2–4 and 7).
- **`environment-z-depth-and-collosion.md`** — Defines the spatial framework (coordinate system, Z-layers, scaling formula) restated in Section 1 of this document.
- **`suburban-crossroads.json`** — Level data containing collider geometry and restitution values referenced in Sections 1 and 7.
- **`game-design-document.md`** Section 3 — The design intent for "physically grounded" ball flight; this spec implements that intent.
- **`state-machine.md`** — Defines when physics is active (BALL_IN_FLIGHT state) and transition triggers.
