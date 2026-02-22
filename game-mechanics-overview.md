**This document is a concise mechanics summary for Street Soccer.** It describes what the player does, what the ball does, and how scoring works — enough to understand the game at a glance. For design intent and rationale, see `game-design-document.md`. For implementation details, equations, and tuning constants, see the technical specs linked throughout and in the Companion Documents section at the end.

---

## 1. The Three-Input Model

Every kick uses three inputs: an **angle slider** set before (or during) the flick, a **flick** to launch, and optional **steer** swipes during flight.

| Input | What It Controls | When It's Active |
|-------|-----------------|-----------------|
| **Angle slider** | Launch angle — low line drive to high lob (10°–75°) | READY and AIMING states; adjustable with one thumb while flicking with the other |
| **Flick** | Kick power (swipe speed/length) and horizontal aim (swipe direction) | AIMING state — touch down to begin, release to launch |
| **Steer swipes** | Lateral and depth spin via the Magnus effect — curves the ball left/right (X-axis swipes) and deeper/shallower (Y-axis swipes) | BALL_IN_FLIGHT state — full-screen touch zone; cumulative and decay over time |

The slider decouples angle from power, so a fast flick can produce either a screaming line drive (slider low) or a towering Big Bomb arc (slider high). Steer swipes add a skill layer: short shots leave little time to correct, while long shots give several seconds of steering opportunity. Steer effectiveness follows a **graduated diminishing returns** curve: swipe 1 at full effect (×1.0), 2nd reduced (×0.6), 3rd minimal (×0.25), 4th+ a tiny residual (×0.1, no hard cap). Each kick resets the budget.

> **Detail:** `input-system.md` defines the full touch architecture — pointer tracking, zone boundaries, gesture detection, and the `FlickResult` data class.

---

## 2. The 2.5D Play Field

The player stands at the bottom of a fixed-camera suburban intersection that recedes toward a central vanishing point. The scene is organized into five Z-layers:

| Z-Layer | Name | What's There |
|:-------:|------|-------------|
| 0 | Launch Zone | Foreground street surface and sidewalks — the player's origin point |
| 1 | Cross-Street | Horizontal road — fast-moving vehicle targets |
| 2 | Primary Properties | Townhome facades, windows, garage doors, fences |
| 3 | Deep Neighborhood | Central corridor to vanishing point — Big Bomb territory and approaching runners |
| 4 | Sky | Open sky — drone and aerial targets |

The ball scales down as it travels deeper into the scene (toward horizon at Y = 540 px), creating a natural difficulty curve: distant targets are smaller and harder to hit.

> **Detail:** `environment-z-depth-and-collosion.md` defines the full Z-layer architecture, scaling formula, and collision mapping. `suburban-crossroads.json` contains the concrete coordinates and collider geometry.

---

## 3. Collision & Surfaces

The level contains two categories of collidable objects:

- **Static colliders** — house facades and fences. The ball bounces off these (a miss that resets the streak). Fences are low obstacles; the ball passes over them if its height exceeds the fence at the point of intersection.
- **Target sensors** — windows (`window_glass`, 250 pts) and the garage door (`door_metal`, 100 pts). Hitting these scores points and triggers feedback.

Each surface has a restitution coefficient that governs bounce intensity:

| Surface | Restitution | Behavior |
|---------|:-----------:|----------|
| Asphalt (ground) | 0.3 | Moderate bounce; ball loses most vertical energy |
| Fences | 0.4 | Slightly bouncier; ball deflects or passes over |
| House facades | 0.2 | Low bounce; ball thuds and drops |
| Out of bounds | 0.0 | Ball removed and reset |

> **Detail:** `physics-and-tuning.md` Section 7 defines the bounce velocity equations. `suburban-crossroads.json` lists all collider IDs, positions, and restitution values.

---

## 4. Ball Flight Physics

The ball is subject to three forces each frame: **gravity** (parabolic arc), **air resistance / drag** (gradual deceleration), and the **Magnus effect** (spin-induced lateral curve from steer swipes). A fixed-timestep simulation (1/60 s) keeps behavior deterministic.

Key characteristics:
- Arc height is set by the angle slider; power determines distance and hang time
- Drag is light — short shots are barely affected, but long Big Bombs lose noticeable speed
- Magnus effect (spin-induced curve from steer swipes on both lateral and depth axes) scales with both spin and ball speed, so fast balls curve more dramatically
- Spin decays exponentially, producing smooth curves that gradually straighten
- Steer swipes follow a graduated budget (×1.0 → ×0.6 → ×0.25 → ×0.1 floor) — the first two swipes are decisive, while the residual ×0.1 tail allows a "nursing" technique for persistent small corrections
- A ground shadow tracks the ball's position, fading with altitude, to help the player gauge depth and height

> **Detail:** `physics-and-tuning.md` Sections 2–4 contain the per-frame update pseudocode, Magnus force equation, and drag model. Section 5 defines the ball shadow. Section 8 lists all tuning constants with suggested values and valid ranges.

---

## 5. Moving Targets & Spawn Lanes

Moving targets cycle through the scene on defined spawn lanes, adding timing challenges on top of aiming skill.

| Lane | Z-Layer | Direction | Speed (units) | Targets |
|------|:-------:|-----------|:-------------:|---------|
| **Cross-street traffic** | 1 | Horizontal (L↔R) | 100–250 | Cars, trucks, bicycles |
| **Deep alley approach** | 3 | Toward camera (scaling up) | 50–80 | Joggers, dog walkers |
| **Sky drone path** | 4 | Horizontal | 80–150 | Delivery drones, birds |
| **Sidewalk dash** *(planned)* | 0–1 | Horizontal | Variable | Cats, small obstacles |

The first three lanes are defined in `suburban-crossroads.json`. The sidewalk lane is a planned future addition.

---

## 6. Big Bomb

A Big Bomb fires when the player delivers a high-power, high-angle kick that sends the ball deep into the central corridor (Z-layer 3).

**Dual-threshold activation** — both must be met:

| Condition | Threshold |
|-----------|-----------|
| Kick power | ≥ 0.9 (90% of maximum) |
| Angle slider | ≥ 0.7 (70% of range, ≈55.5°) |

**Scoring:** 1 point per Y-unit of maximum depth reached, yielding roughly 200–500 base points for a full-depth Big Bomb. The Big Bomb bonus stacks with the streak multiplier — a clean corridor shot at ×3 streak produces the game's highest-scoring moments.

**Risk/reward:** Threading the narrow corridor without clipping a house facade requires precision. A slight angle error means a wall hit, a miss, and a streak reset.

**Distance feedback:** As the ball travels deeper into the corridor, it progressively shifts color toward red — a visual "heating up" effect that tells the player how deep the shot is going. There is no HUD meter; the ball itself is the indicator. See `game-design-document.md` Section 9 (Big Bomb Distance Feedback) for the full color ramp spec.

> **Detail:** `physics-and-tuning.md` Section 6 defines the activation thresholds and distance scoring formula.

---

## 7. Scoring & Streaks

### Target Point Values

| Target | Points | Type |
|--------|:------:|------|
| Upper-story windows | 250 | Static |
| Garage door | 100 | Static |
| Vehicles (cross-street) | 300 | Moving |
| Runners (deep alley) | 350 | Moving |
| Drones (sky) | 500 | Moving |
| Big Bomb distance | Variable | Skill (1 pt per Y-unit) |

### Streak Multiplier

Consecutive target hits build a multiplier:

| Consecutive Hits | Multiplier |
|:----------------:|:----------:|
| 1 | ×1 |
| 2 | ×1.5 |
| 3 | ×2 |
| 4 | ×2.5 |
| 5+ | ×3 (cap) |

A miss (wall hit, ground bounce, or out of bounds) resets the streak to zero. The multiplier applies to the base target value — e.g., a 250-point window at ×3 = **750 points**.

---

## Companion Documents

| Document | Covers |
|----------|--------|
| `game-design-document.md` | Design intent, core loop, scoring rules, progression, seasonal variants |
| `input-system.md` | Touch architecture, angle slider spec, flick detection, steer detection, state integration |
| `physics-and-tuning.md` | Flight equations, Magnus/drag models, Big Bomb thresholds, restitution, all 17 tuning constants |
| `state-machine.md` | Game states (BOOT → READY → AIMING → BALL_IN_FLIGHT → SCORING), transitions, and logic constraints |
| `environment-z-depth-and-collosion.md` | Z-layer architecture, depth scaling formula, collision mapping, spawning coordinates |
| `save-and-persistence.md` | Save system — JSON storage, domain objects, session lifecycle, save triggers, schema versioning |
| `menu-and-navigation-flow.md` | Menu structure — attract screen, variant selection, pause menu, settings/stats/cosmetics overlays, navigation flow |
| `suburban-crossroads.json` | Level data — collider geometry, target sensors, spawn lanes, restitution values |
