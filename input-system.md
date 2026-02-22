**This document is the technical counterpart to GDD Section 3 (Controls & Kick Mechanics).** It defines exactly how the three-input model — angle slider, flick, and steer — is captured, validated, and forwarded to the physics system. Where the GDD describes what the player *experiences*, this spec describes what the code *does*. All input values produced here feed directly into the equations defined in `physics-and-tuning.md`.

---

## 1. Touch Input Architecture

### InputRouter

All touch events flow through a single `InputRouter` that implements LibGDX's `InputProcessor` interface. The router inspects two things before dispatching: **which screen zone** the touch landed in, and **which game state** is currently active. This two-axis dispatch prevents impossible inputs (e.g., steer swipes while in READY) and keeps each subsystem focused on a single responsibility.

### Multi-Touch Pointer Tracking

LibGDX delivers multi-touch events with a `pointer` index (0, 1, 2…). The InputRouter tracks active pointers so the angle slider and flick gesture can operate simultaneously on separate fingers:

| Pointer Assignment | Rule |
|--------------------|------|
| **Slider pointer** | The first pointer whose `touchDown` lands inside the slider rail zone is claimed by `AngleSliderController`. It is tracked until `touchUp`. |
| **Flick pointer** | The first pointer whose `touchDown` lands inside the main play area is claimed by `FlickDetector`. It is tracked until `touchUp`. |
| **Steer pointer** | During BALL_IN_FLIGHT, any `touchDown` in the full-screen zone is claimed by `SteerDetector`. Multiple sequential steer touches are each processed independently. |

Pointers are released on `touchUp` and become available for reassignment.

### Touch Zone Table

| Zone | Screen Region | Active States | Claimed By |
|------|--------------|---------------|------------|
| **Slider rail** | A vertical strip along the left or right screen edge (≈80 px wide) | READY, AIMING | `AngleSliderController` |
| **Main play area** | The remaining screen area outside the slider rail | READY, AIMING | `FlickDetector` |
| **Full screen** | The entire screen (slider rail + play area) | BALL_IN_FLIGHT | `SteerDetector` |

> **Implementation note:** The slider rail side (left vs. right) should be a user preference. Default to the left edge; mirror all X-boundary calculations when set to right.

---

## 2. Angle Slider

The angle slider is a vertical control that sets the ball's launch angle independently of the flick gesture. It is always visible during READY and AIMING states and supports simultaneous multi-touch input alongside the flick.

### Properties Table

| Property | Value |
|----------|-------|
| **Screen position** | Vertical rail along one screen edge; top of rail near screen top, bottom near screen bottom |
| **Rail width** | ≈80 px (touch target); visual thumb may be narrower |
| **Data model** | A single `Float` in the range `0.0` (bottom) to `1.0` (top) |
| **Angle mapping** | `launchAngle = MIN_ANGLE + sliderValue * (MAX_ANGLE - MIN_ANGLE)` |
| **MIN_ANGLE** | 10° — near-flat line drive |
| **MAX_ANGLE** | 75° — steep lob |
| **Default value** | `0.5` (≈42.5°) — a moderate arc suitable for mid-field targets |
| **Input method** | Drag the thumb up/down; the value updates continuously during `touchDragged` |
| **Persistence** | The slider retains its last value between kicks (does not reset to default on SCORING → READY) |

### Visual Representation

- The rail renders as a thin vertical track with a draggable thumb indicator.
- A small angle icon or degree label near the thumb provides at-a-glance feedback.
- During AIMING, if trajectory preview is enabled, the dotted arc updates in real time as the slider moves.

> **Tuning note:** The 10°–75° range is a starting point. If playtesters consistently max the slider for Big Bombs, consider widening the range or adjusting the mapping curve (e.g., quadratic) to give more precision in the mid-range.

---

## 3. Flick Detection

The flick gesture determines kick power and horizontal aim. It follows the standard LibGDX touch lifecycle.

### Gesture Lifecycle

1. **`touchDown(x, y, pointer, button)`** — Record the start position and timestamp. Transition game state from READY → AIMING.
2. **`touchDragged(x, y, pointer)`** — Update the current drag position. If trajectory preview is enabled, recalculate and render the predicted arc.
3. **`touchUp(x, y, pointer, button)`** — Record the end position and timestamp. Compute the flick result. Transition game state from AIMING → BALL_IN_FLIGHT.

### Calculation Table

| Value | Calculation | Notes |
|-------|-------------|-------|
| **Swipe vector** | `endPos - startPos` | Raw pixel displacement |
| **Swipe speed** | `swipeVector.length / elapsed_time` | Pixels per second |
| **Power (0.0–1.0)** | `clamp(swipeSpeed / MAX_FLICK_SPEED, 0.0, 1.0)` | Normalized against a tunable max; see `physics-and-tuning.md` Section 8 |
| **Direction (horizontal)** | `atan2(swipeVector.y, swipeVector.x)` | Radians; predominantly vertical swipes aim straight; angled swipes aim left or right |

### Rejection Threshold

If `swipeSpeed < MIN_FLICK_SPEED`, the gesture is treated as a cancelled drag, not a kick:

- Transition AIMING → READY (no ball launch).
- No `FlickResult` is produced.

`MIN_FLICK_SPEED` is a tuning constant (suggested starting value: **200 px/s**; see `physics-and-tuning.md` Section 8).

### FlickResult Data Class

```
FlickResult:
    power       : Float    // 0.0–1.0, normalized kick strength
    direction   : Float    // radians, horizontal aim angle
    sliderValue : Float    // 0.0–1.0, copied from AngleSliderController at touchUp
```

The `FlickResult` is passed to the physics system, which converts it into initial velocity components using the equations in `physics-and-tuning.md` Section 2.

---

## 4. Steer Swipe Detection

Steer input is active **only** during the BALL_IN_FLIGHT state. It applies spin to the ball, curving its trajectory via the Magnus effect.

### Properties Table

| Property | Value |
|----------|-------|
| **Active state** | BALL_IN_FLIGHT only |
| **Touch zone** | Full screen (no zone restriction) |
| **Axes** | Both horizontal (X) and vertical (Y) components are used |
| **Detection method** | Track displacement on both axes between consecutive `touchDragged` events |
| **Spin direction** | X-axis: positive ΔX → rightward lateral curve, negative ΔX → leftward lateral curve. Y-axis: positive ΔY → deeper into scene (depth curve), negative ΔY → shallower (pulls ball back) |
| **Spin magnitude** | Proportional to swipe speed on each axis: `sqrt(deltaX² + deltaY²) / deltaTime` — the combined displacement determines overall intensity, then split by axis weight |
| **Accumulation** | Each swipe's spin is **added** to the ball's current spin values (`spinX`, `spinY`). Spin is cumulative across multiple swipes, subject to the swipe budget. |
| **Cooldown** | None — the player can swipe continuously or in rapid bursts within the budget |
| **Swipe budget** | Graduated 4-tier curve with no hard cap. Swipe 1 at ×1.0, 2nd at ×0.6, 3rd at ×0.25, 4th+ at ×0.1 (residual floor). Swipes beyond the 3rd all use the ×0.1 floor — unlimited but negligible. Resets on each new kick. |
| **Swipe counter** | Increments on each distinct `touchDown` → `touchUp` gesture during BALL_IN_FLIGHT; resets when the state exits (transition to SCORING or IMPACT_MISSED) |

### Steer Processing

On each `touchDragged` event during BALL_IN_FLIGHT:

```
// Compute swipe speed from combined displacement
swipeSpeed = sqrt(deltaX² + deltaY²) / deltaTime

// Normalize direction into lateral and depth components
magnitude = sqrt(deltaX² + deltaY²)
lateralComponent = deltaX / magnitude
depthComponent   = deltaY / magnitude

// Apply graduated diminishing returns based on swipe count
index = min(swipeCount, STEER_DIMINISH_CURVE.size - 1)
diminish = STEER_DIMINISH_CURVE[index]  // [1.0, 0.6, 0.25, 0.1]

// Compute spin deltas on both axes
spinDeltaX = lateralComponent * swipeSpeed * STEER_SENSITIVITY * diminish
spinDeltaY = depthComponent  * swipeSpeed * STEER_SENSITIVITY * diminish

// Accumulate
ball.spinX += spinDeltaX
ball.spinY += spinDeltaY
```

`STEER_SENSITIVITY` is a tuning constant that controls how responsive steering feels (see `physics-and-tuning.md` Section 8). `STEER_DIMINISH_CURVE` is an array defining the multiplier for each swipe index (see `physics-and-tuning.md` Section 8). The accumulated `ball.spinX` and `ball.spinY` values feed into the Magnus force calculation each physics frame.

> **Implementation note:** Within each swipe gesture (a single `touchDown` → `touchUp` cycle), the player gets continuous, analog-feeling control via `touchDragged` — a slow drag produces a gentle curve; a fast flick of the thumb produces a sharp bend. The diminishing returns apply across discrete swipe gestures (each `touchDown` → `touchUp` cycle increments the swipe counter). The ×0.1 floor means swipes beyond the 3rd are not dead — they form a "nursing" region where persistent small corrections can still nudge the ball. The `min(swipeCount, curve.size - 1)` indexing ensures all swipes past index 3 clamp to the last value (0.1) with no hard cap.

---

## 5. State Integration

The game state machine (defined in `state-machine.md`) determines which input subsystems are active at any moment. The InputRouter enables/disables subsystems based on the current state:

| Game State | Angle Slider | Flick Detector | Steer Detector | Notes |
|------------|:------------:|:--------------:|:--------------:|-------|
| **BOOT** | — | — | — | No input |
| **LOADING** | — | — | — | No input |
| **MAIN_MENU** | — | — | — | UI buttons only |
| **READY** | Active | Listening for `touchDown` | — | Slider adjustable; flick begins on touch |
| **AIMING** | Active | Tracking drag | — | Both slider and drag update simultaneously |
| **BALL_IN_FLIGHT** | — | — | Active | Only steer swipes accepted |
| **SCORING** | — | — | — | Feedback animation; no input |
| **IMPACT_MISSED** | — | — | — | Feedback animation; no input |
| **PAUSED** | — | — | — | Pause menu UI only |

> **Design constraint (from GDD):** The READY → AIMING transition is gated by `shotCount > 0`. Under the free-play design, shot count is effectively unlimited — the gate should always pass.

---

## 6. Implementation Classes

| Class | Responsibility |
|-------|---------------|
| **`InputRouter`** | Implements `InputProcessor`. Dispatches touch events to the correct subsystem based on touch zone and game state. Manages pointer-to-subsystem assignment. |
| **`AngleSliderController`** | Owns the slider `Float` value (0.0–1.0). Processes `touchDown`/`touchDragged`/`touchUp` for the slider pointer. Provides `getCurrentSliderValue()` to the flick system and trajectory preview. |
| **`FlickDetector`** | Records flick start/end positions and timestamps. Computes power, direction, and packages the `FlickResult`. Enforces the `MIN_FLICK_SPEED` rejection threshold. |
| **`SteerDetector`** | Tracks touch movement on both axes during BALL_IN_FLIGHT. Computes per-frame spin deltas (lateral and depth) and applies them to the ball's spin accumulators (`spinX`, `spinY`). Manages the swipe counter and applies the diminishing-returns multiplier. |

These classes are suggestions — the exact package structure and naming may vary. The key requirement is that each input concern is isolated so it can be tested and tuned independently.

---

## Companion Documents

- **`physics-and-tuning.md`** — Defines the equations that consume `FlickResult` and spin values, and lists all tuning constants referenced in this document (Sections 2, 3, and 7).
- **`state-machine.md`** — Defines the game states and transitions that govern when each input subsystem is active (Section 2 and Transition Logic Table).
- **`game-design-document.md`** Section 3 — The design intent behind the three-input model; this spec implements that intent.
