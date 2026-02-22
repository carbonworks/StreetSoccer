# Street Soccer — Game Design Document

This is the authoritative design reference for Street Soccer. It describes **what it feels like to play** and defines the rules that govern scoring, progression, and player experience. Technical implementation details live in the companion specs (`technical-architecture.md`, `environment-z-depth-and-collosion.md`, `state-machine.md`); this document is the "why" and "what," not the "how."

---

## 1. Game Overview

**Elevator Pitch:** Stand at the end of a suburban street and kick a soccer ball at anything that looks breakable. Nail windows, peg moving cars, and send Big Bombs screaming down the central alley — then watch the street transform as seasons change and holidays arrive.

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
AIM  →  KICK  →  STEER  →  SCORE  →  RESET
```

1. **Aim** — The player sets the angle slider for their desired launch arc, then touches the screen to begin a flick gesture.
2. **Kick** — The player releases the flick. Swipe speed and length set power; swipe direction sets the initial trajectory. The ball launches.
3. **Steer** — While the ball is in flight, the player swipes to add spin. Each swipe curves the ball's path, allowing mid-flight corrections. Longer flights (Big Bombs) give more time to steer; short shots demand a precise initial flick.
4. **Score** — The ball strikes a target (or misses). Points appear, combo state updates, and feedback plays.
5. **Reset** — The ball disappears and instantly resets to the launch zone. Only one ball is in flight at a time. Moving targets continue cycling. The player aims again.

There is **no lives system**, **no timer**, and **no shot limit**. The player kicks as many times as they want, for as long as they want. The game rewards sustained engagement through combos and stat milestones rather than punishing failure.

> **Implementation note:** The state machine spec references a `shotCount > 0` gate on the READY → AIMING transition. Under this free-play design, shot count is effectively unlimited — that gate should always pass.

---

## 3. Controls & Kick Mechanics

### Flick + Steer

Kicking uses three inputs: an **angle slider** to set launch angle, a **flick** to launch, then optional **steer** swipes during flight.

#### Angle Slider

A vertical slider on the screen's side rail (left or right edge) controls the ball's **launch angle from the ground** — from a low line drive at the bottom to a high lob at the top.

| Property | Detail |
|----------|--------|
| **Position** | Vertical rail along one screen edge, always visible during READY and AIMING states |
| **Range** | Low (near-flat trajectory) to high (steep arc) |
| **Default** | Mid-range — a moderate arc suitable for mid-field targets |
| **Multi-touch** | The slider supports simultaneous input — the player can adjust the angle with one thumb while flicking with the other |

The slider decouples launch angle from power. This means a fast, powerful flick can produce either a screaming line drive (slider low) or a towering Big Bomb arc (slider high). Without the slider, power and angle would be entangled in a single gesture, limiting expressiveness.

#### Phase 1 — Flick (Launch)

The player touches the screen, drags, and releases. The flick determines horizontal aim and power; the angle slider (set before or during the flick) determines the vertical arc:

| Input | Game Effect |
|-------|-------------|
| **Flick direction (left/right)** | Determines the horizontal aim — where the ball goes laterally |
| **Flick speed / length** | Determines kick power (longer/faster = more force) |
| **Angle slider position** | Determines launch angle from the ground (low line drive ↔ high lob) |

There are no separate kick types or mode switches. The flick is intuitive, and the angle slider adds precision without complexity — new players can ignore it and use the default angle.

#### Phase 2 — Steer (Mid-Flight Spin)

Once the ball is airborne, the player can swipe across the screen to add **spin** to the ball. Each swipe applies a spin force that curves the ball's trajectory:

| Steer Input | Effect |
|------------|--------|
| **Light swipe** | Gentle curve — fine-tune aim toward a nearby target |
| **Rapid repeated swipes** | Heavy accumulated spin — hard bend for dramatic corrections |
| **No swipes** | Ball follows its original flick trajectory unchanged |

Spin is **cumulative** — each additional swipe adds to the existing spin force. Spin **decays over time**, so sustained correction requires repeated input. The ball's trajectory curves proportionally to accumulated spin.

#### Skill Depth

- **Short shots** (nearby windows, garage doors) leave little flight time to steer — the initial flick must be precise
- **Long shots** (Big Bombs down the corridor) give the player several seconds of flight time and multiple steering opportunities
- **Moving targets** can be tracked mid-flight — flick toward the target's general area, then steer into it as it moves
- This creates a natural skill gradient: beginners rely on the flick alone, experienced players layer in spin to bend shots around obstacles and into difficult targets

### Trajectory Preview

A **dotted arc** shows the ball's predicted flight path. During the AIMING state, it previews the flick trajectory. During BALL_IN_FLIGHT, it dynamically updates to reflect accumulated spin, showing the curved path ahead.

The trajectory preview is a **toggleable setting**, disabled by default. Players who want the extra visual aid can enable it in Settings. This keeps the default experience clean while giving newer players an optional training tool.

> **Tip discovery:** The Tips system (Section 10) surfaces the trajectory preview setting to help players find it.

### Kick Feel

- **Slider low + fast flick** → Screaming line drive at a garage door or cross-street vehicle
- **Slider high + fast flick** → Towering Big Bomb arc deep into the corridor
- **Slider mid + angled flick** → Mid-height arc toward an upper-story window
- **Flick + steer combo** → Launch toward the corridor, then bend the ball into a window mid-flight
- **Big Bomb threshold** — When kick power exceeds ~90% of maximum **and** the angle slider is set high enough, the ball enters the central corridor (Z-layer 3) for long-distance travel and bonus scoring potential

The intent is **skill-based precision with expressive depth**: the flick is easy to learn, the angle slider adds tactical control, and layering in spin steering rewards practice and mastery.

### Ball Flight Physics

Ball flight should feel **physically grounded** — not a rigid simulation, but close enough that a player's real-world intuition about kicking a ball transfers into the game.

| Property | Model |
|----------|-------|
| **Gravity** | Standard parabolic arc. The ball rises and falls under constant gravitational acceleration. The arc height is determined by the angle slider — slider low produces a flat trajectory, slider high produces a steep lob. |
| **Time of flight** | Proportional to launch power and angle slider position. A full-power Big Bomb with a high angle should feel like a real long-range kick (~2–3 seconds of hang time), not an instant teleport. A low-angle line drive at the same power arrives quickly. |
| **Distance** | Correlated with power and angle realistically. A ~45° angle slider setting at max power travels the farthest; steeper or shallower settings cover less ground. |
| **Spin (Magnus effect)** | Steer swipes apply spin that curves the ball laterally via the Magnus effect. The curve magnitude scales with spin rate and ball speed — spin has more visible effect on fast-moving balls and diminishes as the ball slows. |
| **Spin decay** | Spin bleeds off gradually due to air resistance, not instantly. A single steer swipe produces a smooth, sustained curve that gently straightens out. |
| **Air resistance (drag)** | Light drag so the ball doesn't fly forever. The ball decelerates slightly over its flight, making distant targets require genuinely powerful flicks. |

> **Tuning note:** "Realistic" means the physics *feel* correct to a player, not that they pass a physics exam. Values should be tuned for game feel — e.g., gravity may be slightly stronger than 9.8 m/s² to keep rallies snappy, and Magnus effect may be amplified so spin steering feels responsive. The goal is *plausible*, not *simulation-accurate*.

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

Fences and house facades are **static colliders**, not targets — the ball bounces off them (a miss). Future seasons and events may introduce additional target types (see Section 6).

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

## 6. Seasons & Events (Levels)

The game takes place on **one street** — the Suburban Crossroads. Instead of traveling to new locations, the street itself transforms to reflect different **seasons, holidays, and neighborhood events**. The layout and collision geometry stay the same, but the visual theme, target dressing, moving elements, and audio all change.

### The Base Street: Suburban Crossroads

The default environment. A wide suburban intersection with:

- Two flanking townhomes (left and right) with targetable windows and garage doors
- White picket fences along the front yards
- A horizontal cross-street with vehicle traffic
- A deep central road leading to the vanishing point
- Open sky above for aerial targets

This is the street the player always returns to. It teaches the core mechanics: aiming at static targets, timing shots against moving traffic, and discovering the Big Bomb corridor.

### Seasonal & Event Variants (Planned)

Each variant re-skins the same street and swaps in thematic targets and moving elements, while keeping the core controls and scoring system identical.

| Variant | Theme | What Changes |
|---------|-------|-------------|
| **Summer Block Party** | Cookouts, lawn games, sprinklers | Inflatable targets on lawns, kids on bikes in cross-street, ice cream truck |
| **Halloween Night** | Jack-o-lanterns, fog, spooky lighting | Pumpkin targets on porches, trick-or-treaters as moving targets, bats in sky lane |
| **Winter Holidays** | Snow, string lights, decorations | Snowman targets, light-up reindeer on roofs, mail carrier with packages |
| **Rainy Day** | Overcast, puddles, umbrellas | Umbrella-carrying pedestrians, delivery vans, altered bounce physics on wet ground |
| **Garage Sale Saturday** | Tables on driveways, signs, clutter | Breakable junk on tables as new static targets, browsers milling on sidewalks |

Each variant is defined by its own level JSON file that extends the base `suburban-crossroads.json` format — same collider geometry, but different `target_sensors`, `spawn_lanes`, background image, and audio set.

### Unlocking Variants

New variants unlock based on **cumulative score milestones** across all play sessions. This rewards continued play without gating content behind skill walls — even a player who misses often will eventually accumulate enough points to unlock the next event.

---

## 7. Progression & Rewards

Street Soccer uses **free play / sandbox** structure. There are no lives, no game-over screens, and no forced restarts. The player kicks forever if they choose.

### What Drives Engagement

| Mechanism | Description |
|-----------|-------------|
| **Personal bests** | Highest single-kick score, longest streak, best Big Bomb distance |
| **Cumulative stats** | Total kicks, total points, total targets hit, total windows broken |
| **Season/event unlocks** | New street variants unlock at score milestones |
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

A persistent stats screen tracks the player's career. A **session** begins when the player enters a street variant from the menu and ends when they return to the menu or close the app.

- **All-time high score** (best single session)
- **Best streak** (most consecutive hits in any session)
- **Longest Big Bomb** (greatest Y-distance reached)
- **Total kicks / Total hits / Hit rate**
- **Targets broken by type** (windows, vehicles, drones, etc.)
- **Variants unlocked**

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
- **Visual life** — a street with moving cars, flying drones, and darting cats feels alive and playful

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

### Spin & Steer Feedback

| Event | Visual | Audio |
|-------|--------|-------|
| **Steer swipe applied** | Ball rotation speed increases visibly; a curved motion trail appears behind the ball | Subtle whoosh / spin sound on each swipe |
| **Heavy spin accumulated** | Trail becomes more pronounced and colorful; ball wobbles slightly | Spin sound intensifies in pitch |
| **Spin decay** | Trail gradually fades back to normal | No audio — silent decay feels natural |

Spin feedback must be immediate and readable so the player sees the connection between their steer swipe and the ball's change in curvature.

### Score Popups

Points appear at the impact location, float upward briefly, and fade. If a multiplier is active, the multiplied total is shown in a larger, more prominent style (e.g., "750" instead of "250 ×3") so the player sees the reward immediately.

---

## 10. Tips & Feature Discovery

Street Soccer includes a **rotating tips system** that helps players discover mechanics and settings they might otherwise miss.

### How Tips Are Shown

- Tips appear contextually: on loading screens, after consecutive misses, and in the pause/settings menu
- Each tip is shown once per session unless the player has not yet engaged with the feature it describes
- Tips rotate through the list so the player sees different hints over time

### Starter Tips

| # | Tip Text | Context |
|---|----------|---------|
| 1 | "Enable **Trajectory Preview** in Settings to see your ball's predicted path." | After 3+ consecutive misses, or on first launch |
| 2 | "Swipe while the ball is in the air to add **spin** and curve your shot!" | After the player's first Big Bomb, or after 10 kicks with no steer input |
| 3 | "Hit targets in a row to build a **streak multiplier** — up to ×3!" | After the player's first streak of 2+ |
| 4 | "Aim a powerful flick straight up to send a **Big Bomb** down the central corridor for bonus points." | After 20 kicks with no Big Bomb attempt |

### Design Intent

Tips are a low-friction discovery mechanism — they point players toward depth without requiring a tutorial. The tips list is extensible; new tips can be added as features are introduced in seasonal updates.

---

## 11. Future Considerations

These ideas are explicitly **parked for later**. They are not part of the initial scope and should not influence current implementation decisions.

| Idea | Notes |
|------|-------|
| **Local leaderboards** | Per-variant high score tables stored on-device |
| **Online leaderboards** | Would require a backend service — significant scope increase |
| **Multiplayer** | Turn-based "beat my score" sharing; would need score validation |
| **Limited-time events** | Time-limited variants or exclusive ball skins tied to real-world holidays |
| **Challenge mode** | Timed rounds or limited-kick scenarios as an alternative to free play |
| **Weather effects** | Wind that alters ball trajectory, rain that changes bounce physics |
| **Trick shots** | Ricochet scoring — ball bounces off a wall and hits a window for bonus points |
| **Destructible environments** | Cumulative damage to buildings across sessions |

These features may be revisited once the core loop is solid and the base street is fully playable.

---

*This document is the design "North Star" for Street Soccer. When in doubt about a feature, scoring rule, or mechanic, defer to this GDD. Technical implementation should serve these design goals, not the other way around.*
