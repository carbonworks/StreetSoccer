# Street Soccer — Game Design Document

This is the authoritative design reference for Street Soccer. It describes **what it feels like to play** and defines the rules that govern scoring, progression, and player experience. Technical implementation details live in the companion specs (`technical-architecture.md`, `environment-z-depth-and-collosion.md`, `state-machine.md`); this document is the "why" and "what," not the "how."

---

## 1. Game Overview

**Elevator Pitch:** Stand at the end of a suburban street and kick a soccer ball at anything that looks breakable. Nail windows, peg moving cars, and send Big Bombs screaming down the central alley — then do it again in the next neighborhood.

| Attribute | Value |
|-----------|-------|
| **Genre** | 2.5D arcade / skill toy |
| **Platform** | Android (primary), Desktop (debug/test) |
| **Camera** | Stationary, single-vanishing-point perspective |
| **Art Style** | Flat 2.0 vector aesthetic — clean geometry, bold colors, minimal shading |
| **Target Resolution** | 1920×1080 (16:9) |
| **Session Length** | Pick-up-and-play; no forced stopping point |
| **Tone** | Lighthearted mischief — cartoon destruction, no real consequences |

---

## 2. Core Loop

The moment-to-moment cycle is deliberately tight:

```
AIM  →  KICK  →  WATCH  →  SCORE  →  REPEAT
```

1. **Aim** — The player touches the screen and drags; a dotted trajectory arc previews the ball's flight path in real time.
2. **Kick** — The player lifts their finger. Power and direction are locked in from the gesture.
3. **Watch** — The ball flies into the scene, shrinking as it travels deeper. The player tracks it visually.
4. **Score** — The ball strikes a target (or misses). Points appear, combo state updates, and feedback plays.
5. **Reset** — The ball disappears and instantly resets to the launch zone. Only one ball is in flight at a time. Moving targets continue cycling. The player aims again.

There is **no lives system**, **no timer**, and **no shot limit**. The player kicks as many times as they want, for as long as they want. The game rewards sustained engagement through combos and stat milestones rather than punishing failure.

> **Implementation note:** The state machine spec references a `shotCount > 0` gate on the READY → AIMING transition. Under this free-play design, shot count is effectively unlimited — that gate should always pass.

---

## 3. Controls & Kick Mechanics

### Single Unified Swipe

All kicks use the same input gesture — a single swipe that determines both **direction** and **power** simultaneously.

| Gesture Property | Game Effect |
|-----------------|-------------|
| **Swipe direction** | Determines the horizontal angle and depth trajectory of the ball |
| **Swipe length / speed** | Determines kick power (longer/faster = more force) |

There are no separate kick types or mode switches. The player's skill comes from reading the field and executing the right swipe to reach their intended target.

### Trajectory Preview

While the player drags, a **dotted arc** renders in real time showing the predicted flight path. This preview is essential to the "skill-based precision" feel — the player sees exactly where the ball will go before committing. The arc updates every frame during the AIMING state.

### Kick Feel

- **Low power, angled swipe** → Short arc toward a nearby window or building target
- **High power, upward swipe** → The ball launches deep into the scene, shrinking rapidly — the "Big Bomb"
- **Big Bomb threshold** — When kick power exceeds ~90% of maximum, the ball enters the central corridor (Z-layer 3) for long-distance travel and bonus scoring potential

The intent is **skill-based precision**: the player must learn how swipe gestures map to ball trajectories and improve over time. Easy to pick up, difficult to master.

---

## 4. The 2.5D Play Field

The player views the scene from a fixed position at the bottom of the screen, looking into a suburban intersection that recedes toward a central vanishing point.

### Depth Zones (Design Perspective)

| Zone | What the Player Sees | What Lives There |
|------|---------------------|-----------------|
| **Foreground** (bottom 20%) | Street surface, manhole cover, sidewalks | The launch zone — this is where the player "stands" |
| **Cross-street** (mid-foreground) | A horizontal road | Fast-moving targets: cars, bikers, animals |
| **Primary properties** (mid-field) | Townhomes with windows, fences, lawns | The bread-and-butter targets — windows and garage doors; fences and walls are obstacles |
| **Deep neighborhood** (background) | Central road to vanishing point, distant houses | Big Bomb corridor — skill shots for distance-based scoring |
| **Sky** (upper portion) | Open blue sky | Aerial targets: drones, birds |

The ball shrinks as it travels deeper, creating a natural feedback loop: distant targets are smaller and harder to hit, so they're worth more.

### Key Design Constraint

The camera never moves. The entire level is visible at once. This keeps the game readable and ensures the player always knows where targets are — success is about execution, not discovery.

---

## 5. Targets & Scoring

### Target Categories

**Static targets** are `target_sensor` fixtures in the level JSON (see `suburban-crossroads.json`). **Moving targets** are spawned on lanes; their point values are design targets to be added to the level data.

| Target | Location | Points | Type | Notes |
|--------|----------|--------|------|-------|
| **Upper-story windows** | House facades, Z-layer 2 | 250 | Static | Glass-break effect on hit; small hitbox, high skill |
| **Garage door** | House facade, Z-layer 2 | 100 | Static | Large surface, low value — the "easy" target |
| **Vehicles** | Cross-street lane, Z-layer 1 | 300 | Moving | Timing required; fast horizontal movement |
| **Runners / pedestrians** | Deep alley lane, Z-layer 3 | 350 | Moving | Smaller hitbox, approaching camera |
| **Drones** | Sky lane, Z-layer 4 | 500 | Moving | Small, far away — hardest standard target |
| **Big Bomb distance** | Central corridor, Z-layer 3 | Variable | Skill | Points scale with distance traveled (see below) |

Fences and house facades are **static colliders**, not targets — the ball bounces off them (a miss). Future neighborhoods may introduce additional target types (see Section 6).

### Combo / Streak Multiplier

Consecutive target hits build a **streak multiplier** that amplifies the base point value:

| Consecutive Hits | Multiplier |
|-----------------|------------|
| 1 | ×1 |
| 2 | ×1.5 |
| 3 | ×2 |
| 4 | ×2.5 |
| 5+ | ×3 (cap) |

- A **miss** (ball hits a wall, bounces on the ground, or goes out of bounds) resets the streak to zero.
- The multiplier applies to the base target value: a 250-point window hit at ×3 streak = **750 points**.
- The streak counter and current multiplier are displayed on-screen so the player always knows the stakes.

### Big Bomb Bonus

When a kick exceeds the Big Bomb power threshold and enters the deep corridor (Z-layer 3):

- Base points are awarded based on **how far the ball travels** toward the vanishing point before it stops or exits the play field.
- Distance is measured as the ball's maximum Y-position relative to the player origin. Suggested scoring: **1 point per unit of Y-distance**, yielding roughly **200–500 base points** for a full-depth Big Bomb depending on power.
- The Big Bomb bonus stacks with the streak multiplier — a deep corridor shot at ×3 streak produces the game's highest-scoring moments.
- This creates the risk/reward tension: Big Bombs require a precise upward swipe to thread the central corridor without hitting the flanking house facades. A slight angle error means a wall hit (miss + streak reset), but a clean shot at high streak is the game's peak scoring moment.

---

## 6. Neighborhoods (Levels)

Each level is a self-contained **neighborhood** — a distinct environment with its own layout, target placement, and moving elements.

### Launch Neighborhood: Suburban Crossroads

The first and primary environment. A wide suburban intersection with:

- Two flanking townhomes (left and right) with targetable windows and garage doors
- White picket fences along the front yards
- A horizontal cross-street with vehicle traffic
- A deep central road leading to the vanishing point
- Open sky above for aerial targets

This neighborhood teaches the core mechanics: aiming at static targets, timing shots against moving traffic, and discovering the Big Bomb corridor.

### Future Neighborhoods (Planned)

New neighborhoods introduce **different layouts and target types** while keeping the same core controls and scoring system.

| Neighborhood | Concept | What Changes |
|-------------|---------|-------------|
| **Downtown Block** | Taller buildings, narrower alleys | More vertical targets (upper floors), tighter corridors |
| **Industrial Yard** | Warehouses, loading docks, forklifts | Large slow-moving targets, breakable crates, metal sounds |
| **Waterfront Pier** | Docks, boats, seagulls | Overwater targets, rocking boats as moving targets, wind effects |
| **Night Market** | String lights, food stalls, neon signs | Lit-up targets, different visual atmosphere, crowds |

Each neighborhood is defined by its own level JSON file (see `suburban-crossroads.json` for the format), allowing targets, colliders, and spawn lanes to be tuned independently.

### Neighborhood Unlocks

New neighborhoods unlock based on **cumulative score milestones** across all play sessions. This rewards continued play without gating content behind skill walls — even a player who misses often will eventually accumulate enough points to unlock the next area.

---

## 7. Progression & Rewards

Street Soccer uses **free play / sandbox** structure. There are no lives, no game-over screens, and no forced restarts. The player kicks forever if they choose.

### What Drives Engagement

| Mechanism | Description |
|-----------|-------------|
| **Personal bests** | Highest single-kick score, longest streak, best Big Bomb distance |
| **Cumulative stats** | Total kicks, total points, total targets hit, total windows broken |
| **Neighborhood unlocks** | New environments unlock at score milestones |
| **Cosmetic unlocks** | Ball skins and visual effects earned through play |

### Cosmetic Unlocks

Unlockable cosmetics are purely visual — they do not affect gameplay.

| Category | Examples | Unlock Method |
|----------|----------|---------------|
| **Ball skins** | Classic white, orange street ball, chrome, flame trail, pixel ball | Score milestones, streak achievements |
| **Impact effects** | Default shatter, confetti burst, pixel explosion, smoke puff | Cumulative target milestones |
| **Trail effects** | None (default), light streak, sparkle trail, smoke trail | Big Bomb distance milestones |

The cosmetic system is intentionally minimal — a small collection of achievable unlocks that give the player visible proof of mastery, not an endless grind.

### Stats Screen

A persistent stats screen tracks the player's career. A **session** begins when the player enters a neighborhood from the menu and ends when they return to the menu or close the app.

- **All-time high score** (best single session)
- **Best streak** (most consecutive hits in any session)
- **Longest Big Bomb** (greatest Y-distance reached)
- **Total kicks / Total hits / Hit rate**
- **Targets broken by type** (windows, vehicles, drones, etc.)
- **Neighborhoods unlocked**

---

## 8. Moving Targets & Environmental Elements

Moving targets cycle through the scene on defined **spawn lanes**, creating timing challenges layered on top of the aiming skill.

### Spawn Lane Types

The first three lanes are defined in `suburban-crossroads.json`. The sidewalk lane is a design intent for future implementation.

| Lane | Location | Direction | Speed | Targets | Status |
|------|----------|-----------|-------|---------|--------|
| **Cross-street traffic** | Horizontal road, Z-layer 1 | Left-to-right or right-to-left | Fast (100–250 units) | Cars, trucks, bicycles | In level JSON |
| **Deep alley approach** | Central corridor, Z-layer 3 | Toward camera (scaling up) | Slow (50–80 units) | Joggers, dog walkers | In level JSON |
| **Sky path** | Upper screen, Z-layer 4 | Horizontal | Medium (80–150 units) | Delivery drones, birds | In level JSON |
| **Sidewalk dash** | Foreground sidewalks, Z-layer 0–1 | Horizontal | Variable | Cats, lawnmower Roombas | Planned |

### Design Intent

Moving targets serve multiple purposes:

- **Break up static gameplay** — the player can't just memorize aim angles; they must react to timing
- **Risk/reward layering** — moving targets are worth more points but harder to hit, especially at high streaks
- **Visual life** — a neighborhood with moving cars, flying drones, and darting cats feels alive and playful

Moving targets are **not enemies** — they don't attack the player or end the game. They are simply higher-value scoring opportunities that pass through the scene.

---

## 9. Audio & Visual Feedback

Every kick outcome should produce clear, satisfying feedback so the player immediately understands what happened.

### Impact Feedback

**Target hits** (scoring — triggers SCORING state):

| Event | Visual | Audio |
|-------|--------|-------|
| **Window hit** | Glass-shatter particle effect, score popup | Crisp glass-break sound |
| **Garage door hit** | Thud ripple, score popup | Deep metallic clang |
| **Vehicle hit** | Car alarm flash, score popup | Car alarm chirp + impact thud |
| **Drone hit** | Sparks + spiral fall animation, score popup | Electronic fizz + crash |
| **Big Bomb launch** | Screen-edge light flash, camera shake | Low bass "boom" on launch |

**Misses** (no score — triggers IMPACT_MISSED state, resets streak):

| Event | Visual | Audio |
|-------|--------|-------|
| **House facade hit** | Dust puff on impact | Dull thud |
| **Fence hit** | Wood splinter particles | Wooden crack |
| **Out of bounds** | Ball fades out | Silence or quiet whoosh |

### Streak Feedback

| Streak Level | Feedback |
|-------------|----------|
| 2 hits | "Nice!" text popup |
| 3 hits | Multiplier badge glows, quick chime |
| 5+ hits | Streak counter pulses, escalating chime pitch |
| Streak broken | Multiplier badge dims with a deflation sound |

### Score Popups

Points appear at the impact location, float upward briefly, and fade. If a multiplier is active, the multiplied total is shown in a larger, more prominent style (e.g., "750" instead of "250 ×3") so the player sees the reward immediately.

---

## 10. Future Considerations

These ideas are explicitly **parked for later**. They are not part of the initial scope and should not influence current implementation decisions.

| Idea | Notes |
|------|-------|
| **Local leaderboards** | Per-neighborhood high score tables stored on-device |
| **Online leaderboards** | Would require a backend service — significant scope increase |
| **Multiplayer** | Turn-based "beat my score" sharing; would need score validation |
| **Seasonal content** | Holiday-themed neighborhoods or limited-time ball skins |
| **Challenge mode** | Timed rounds or limited-kick scenarios as an alternative to free play |
| **Weather effects** | Wind that alters ball trajectory, rain that changes bounce physics |
| **Trick shots** | Ricochet scoring — ball bounces off a wall and hits a window for bonus points |
| **Destructible environments** | Cumulative damage to buildings across sessions |

These features may be revisited once the core loop is solid and the first neighborhood is fully playable.

---

*This document is the design "North Star" for Street Soccer. When in doubt about a feature, scoring rule, or mechanic, defer to this GDD. Technical implementation should serve these design goals, not the other way around.*
