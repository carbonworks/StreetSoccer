**This document defines the layout, sizing, and behavior of all on-screen HUD elements for the alpha build of Street Soccer.** It consolidates UI details currently scattered across the GDD (Section 9), `input-system.md`, and `physics-and-tuning.md` into a single, implementable spec. All coordinates use the 1920×1080 reference resolution. Elements are described in screen-region order.

---

## 1. Layout Overview

### Coordinate Reference

All positions use a **1920×1080 pixel** coordinate space (the target resolution from the GDD). On devices with different aspect ratios, the HUD anchors to screen edges; the play field scales to fit.

### Screen Region Map

```
┌──────────────────────────────────────────────────────┐
│  [Pause]            SESSION SCORE          [Streak]  │
│  top-left           top-center            top-right  │
│                                                      │
│                                                      │
│  ┌─────┐                                             │
│  │Angle│         MAIN PLAY AREA                      │
│  │Slide│      (flick zone / steer zone)              │
│  │  r  │                                             │
│  │     │                                             │
│  └─────┘                              ┌──────────┐   │
│  left edge                            │  Steer   │   │
│                                       │  Budget  │   │
│                                       │  Meter   │   │
│                                       └──────────┘   │
│                                       opposite edge  │
│                                                      │
│            [Score Popups at impact point]             │
│                                                      │
│               LAUNCH ZONE (bottom)                   │
└──────────────────────────────────────────────────────┘
```

> **Note:** The angle slider defaults to the left edge. The steer budget meter is placed on the **opposite** side from the slider. If the player switches the slider to the right edge (handedness preference, beta feature), the steer budget meter mirrors to the left.

---

## 2. Session Score Display

The session score is the player's running point total for the current play session.

| Property | Value |
|----------|-------|
| **Position** | Top-center of screen, horizontally centered |
| **Anchor** | Top edge, centered horizontally |
| **Font style** | Large, bold, arcade-style — high contrast against the sky/background |
| **Content** | Plain integer, no decimal or separator (e.g., `4250`, not `4,250`) |
| **Initial value** | `0` at session start |
| **Update trigger** | Increments on each SCORING event by the multiplied point value |
| **Animation** | Scale pulse on increment — briefly scales up ~120% then returns to 100% over ~0.2 s |
| **Visibility** | Always visible during gameplay states (READY, AIMING, BALL_IN_FLIGHT, SCORING, IMPACT_MISSED) |

> **Design note:** The score is intentionally minimal — a single number, no label. The arcade-style font and scale pulse on increment provide all the feedback needed.

---

## 3. Streak Counter & Multiplier Badge

Displays the current consecutive-hit streak and the active multiplier.

| Property | Value |
|----------|-------|
| **Position** | Top-right corner of screen |
| **Anchor** | Top-right edge with margin (~32 px from edges) |
| **Layout** | Two elements stacked vertically: streak count on top (e.g., `×3`), multiplier badge below |
| **Streak count** | Shows the current multiplier as `×1`, `×1.5`, `×2`, `×2.5`, or `×3` |
| **Badge appearance** | A small rounded rectangle or pill behind the multiplier text |
| **Badge color tiers** | Follows streak level: ×1 = gray/dim, ×1.5 = white, ×2 = yellow, ×2.5 = orange, ×3 = red/gold with glow |
| **Pulse animation** | At 5+ consecutive hits, the badge pulses (scale oscillation ~105%–110%) on each new hit |
| **Reset animation** | On streak break: badge dims to gray, brief deflation scale (shrink to ~90% then return) |
| **Visibility** | Always visible during gameplay states; shows `×1` at minimum (never hidden) |

> **Cross-reference:** Streak multiplier values and thresholds are defined in GDD Section 5 (Combo / Streak Multiplier). Streak feedback audio/visual cues are in GDD Section 9 (Streak Feedback).

---

## 4. Steer Budget Meter

A visual indicator showing how much steer effectiveness remains for the current kick.

| Property | Value |
|----------|-------|
| **Position** | Opposite screen edge from the angle slider (default: right edge) |
| **Anchor** | Vertically centered on the screen edge, mirroring the slider's vertical extent |
| **Layout** | 4 stacked horizontal sections (segments), bottom-to-top: section 1 (full, ×1.0), section 2 (reduced, ×0.6), section 3 (weak, ×0.25), section 4 (residual, ×0.1) |
| **Drain direction** | Bottom-up — section 1 drains first as the 1st swipe is used, then section 2, etc. |
| **Section 4 behavior** | Never fully drains — it pulses faintly when used and resists full depletion, visually communicating the ×0.1 residual floor |
| **Fill color** | Full = bright green/blue; reduced = yellow; weak = orange; residual = dim red/gray |
| **Width** | Narrow (~40 px) — unobtrusive but readable |
| **Visibility** | Visible only during BALL_IN_FLIGHT state; hidden during READY/AIMING |
| **Reset** | Fully refilled on each new kick (transition to BALL_IN_FLIGHT) |

### Drain Behavior

When a steer swipe is used:
1. The corresponding section animates a drain (fill level decreases over ~0.15 s)
2. The section's color may shift to indicate depletion
3. Section 4 (residual) pulses but does not fully drain, reinforcing that the player can keep swiping with diminished effect

> **Cross-reference:** The graduated curve values `[1.0, 0.6, 0.25, 0.1]` are defined in `physics-and-tuning.md` Section 8 (constant #14 `STEER_DIMINISH_CURVE`).

---

## 5. Angle Slider

The angle slider is a persistent input control that sets the ball's launch angle.

| Property | Value |
|----------|-------|
| **Position** | Vertical rail along one screen edge (default: left) |
| **Anchor** | Left edge (or right edge if handedness is configured) |
| **Rail dimensions** | ≈80 px wide touch target; visual rail may be narrower (~20 px track) |
| **Thumb** | A draggable circular or pill-shaped indicator on the rail |
| **Angle label** | Small text or icon near the thumb showing the approximate launch angle (e.g., `42°`) |
| **Range** | Bottom of rail = `MIN_ANGLE` (10°), top = `MAX_ANGLE` (75°) |
| **Default position** | `0.5` (mid-rail, ≈42.5°) |
| **Persistence** | Retains last value between kicks — does not reset on SCORING → READY |
| **Visibility** | Visible during READY and AIMING states; hidden during BALL_IN_FLIGHT |
| **Multi-touch** | Supports simultaneous input — one thumb on slider, other thumb flicking |

> **Cross-reference:** Full input behavior is defined in `input-system.md` Section 2. Angle mapping formula: `launchAngle = MIN_ANGLE + sliderValue * (MAX_ANGLE - MIN_ANGLE)`.

---

## 6. Score Popups

Floating point displays that appear at the impact location when a target is hit.

| Property | Value |
|----------|-------|
| **Position** | Spawns at the ball's impact point in world space |
| **Motion** | Floats upward (~80 px over ~1.0 s) while fading out |
| **Content** | The final multiplied point value as a plain integer (e.g., `750`, not `250 ×3`) |
| **Font size** | Base size for ×1 hits; larger/bolder for multiplied hits (×1.5+) |
| **Color** | White with a dark outline/shadow for readability against any background |
| **Multiplied style** | Hits at ×2+ use a larger font, brighter color (gold/yellow), and a brief scale-in animation |
| **Stacking** | Multiple popups can be visible simultaneously (e.g., rapid hits from bounces) — they should not overlap; offset vertically if needed |
| **Duration** | ~1.0 s from spawn to fully faded |

> **Cross-reference:** GDD Section 9 (Score Popups) defines the design intent.

---

## 7. Pause Icon

A persistent touch target for pausing the game.

| Property | Value |
|----------|-------|
| **Position** | Top-left corner of screen |
| **Anchor** | Top-left edge with margin (~32 px from edges) |
| **Icon** | Standard double-bar pause icon (`❚❚`) |
| **Size** | ~64×64 px touch target (icon may be smaller visually) |
| **Behavior** | Tap transitions game state to PAUSED |
| **Visibility** | Always visible during gameplay states |
| **Style** | Semi-transparent when idle; fully opaque on touch |

---

## 8. Big Bomb Meteor Color Feedback

During a Big Bomb flight, the ball itself provides depth feedback through a progressive color shift.

| Property | Value |
|----------|-------|
| **Trigger** | Active only during Big Bomb flights (power ≥ 0.9, slider ≥ 0.7) |
| **Method** | Alpha-blended red overlay on the ball sprite |
| **Color ramp start** | `BIG_BOMB_COLOR_START_DEPTH` (0.25) — no overlay before this depth |
| **Color ramp end** | `BIG_BOMB_COLOR_MAX_DEPTH` (0.90) — full red overlay at this depth |
| **Interpolation** | Linear alpha ramp: `overlayAlpha = clamp((normalizedDepth - START) / (MAX - START), 0.0, 1.0)` |
| **Overlay color** | Red (`#FF0000` or similar warm red) with the computed alpha |
| **Glow at max** | At depths ≥ 0.90, add a subtle bright glow/bloom around the ball to sell the "meteor" feeling |
| **No HUD element** | This feedback is entirely on the ball — no meters, gauges, or screen overlays |

### Color Ramp Stages

| Stage | Normalized Depth | Ball Appearance |
|-------|:----------------:|-----------------|
| **Entry** | 0.00–0.25 | Normal ball color |
| **Mid** | 0.25–0.50 | Faint red tint begins |
| **Deep** | 0.50–0.90 | Strong red glow — clearly "heating up" |
| **Max** | 0.90–1.00 | Full red with bright glow — meteor visual |

> **Cross-reference:** Tuning constants `BIG_BOMB_COLOR_START_DEPTH` (#16) and `BIG_BOMB_COLOR_MAX_DEPTH` (#17) are in `physics-and-tuning.md` Section 8. Design intent is in `game-design-document.md` Section 9 (Big Bomb Distance Feedback).

---

## 9. Trajectory Preview

A dotted arc showing the ball's predicted flight path. This is an existing feature described in the GDD; this section consolidates its HUD-relevant properties.

| Property | Value |
|----------|-------|
| **Visibility** | Toggleable setting, **disabled by default** |
| **During AIMING** | Shows predicted arc based on current flick drag and slider position; updates in real time |
| **During BALL_IN_FLIGHT** | Dynamically updates to reflect accumulated spin, showing the curved path ahead |
| **Style** | Dotted arc — evenly spaced dots along the predicted trajectory |
| **Color** | White with slight transparency, or a subtle contrasting color that reads against the background |

> **Cross-reference:** GDD Section 3 (Trajectory Preview) defines the design intent and discoverability via the Tips system.

---

## 10. Ball Shadow

A ground-plane projection below the ball that communicates depth and height. This is an existing feature; this section consolidates its HUD-relevant properties.

| Property | Value |
|----------|-------|
| **Shape** | Dark ellipse on the ground plane |
| **Position** | Ball's (x, y) ground-plane coordinates — directly below the ball |
| **Scale** | Same depth formula as the ball: `max(0.05, (540 - ball.y) / 540)` |
| **Opacity** | Fades with height: `shadowAlpha = max(0.1, 1.0 - ball.height / SHADOW_FADE_HEIGHT)` |
| **Rendering** | Below the ball sprite in render order; within ground-plane zones |
| **Visibility** | Always visible when the ball is in flight |

> **Cross-reference:** `physics-and-tuning.md` Section 5 defines the shadow model. `SHADOW_FADE_HEIGHT` (constant #15) is in Section 8.

---

## 11. Out of Scope (Beta)

The following HUD/UI elements are explicitly **not part of the alpha build**. They are listed here to prevent scope creep and to document known future work.

| Element | Notes |
|---------|-------|
| **Meteor sprite set** | Replace the alpha-blended red overlay with a dedicated fireball sprite + flame trail during Big Bomb flights |
| **Handedness configuration** | Settings toggle to move the angle slider to the right edge (and mirror steer budget meter to the left). Default left-edge placement is sufficient for alpha. |
| **Season/event indicator** | A badge or label showing which seasonal variant is active. Not needed until seasonal variants are implemented. |
| **Economy UI** | Currency display, shop button, unlock progress bars — deferred until the cosmetic/unlock system is specified |
| **Streak timer** | A visual timer showing how long the player has to maintain a streak before it auto-resets. Current design has no time-based streak decay — streaks only break on a miss. Listed here in case the design changes. |

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `game-design-document.md` Section 9 | Design intent for all feedback: impact, streak, spin/steer, Big Bomb, score popups |
| `input-system.md` Section 2 | Angle slider input behavior and touch zone definitions |
| `physics-and-tuning.md` Section 8 | All tuning constants referenced by HUD elements (steer curve, shadow fade, Big Bomb color ramp) |
| `state-machine.md` | Game states that control HUD element visibility |
