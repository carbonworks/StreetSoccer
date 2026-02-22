# Street Soccer

A 2.5D arcade-style street soccer game for Android built with LibGDX. The player stands at a stationary position at the bottom of the screen and kicks a soccer ball into a suburban neighborhood environment, aiming at targets like windows, doors, and moving objects. The game uses a single-vanishing-point perspective to simulate depth, with ball scaling, shadow projection, and Z-layer ordering to create the illusion of 3D flight over a flat 2D background.

## Game Concept

- **Stationary kicker** at the bottom of the screen (inspired by Duck Hunt-style targeting)
- **2.5D physics** — balls travel "into" the scene, shrinking as they approach the horizon
- **Suburban crossroads** environment with townhomes, fences, roads, and a deep central alley
- **Target variety** — static targets (windows, garage doors) and moving targets (vehicles, runners, drones)
- **"Big Bomb" mechanic** — powerful long-distance kicks down the central corridor for bonus points

## Project Files

| File | Description |
|------|-------------|
| `background.jpg` | The primary level background — a flat-style illustration of a suburban intersection with a center vanishing point, flanked by townhomes with white fences and front lawns. |
| `documentation-overview.md` | A checklist of documents needed to take the project from architecture to a shippable game, including a Game Design Document, physics spec, asset registry, AI style guide, and testing plan. |
| `game-design-document.md` | The authoritative Game Design Document (GDD) — defines the core loop, kick mechanics, target scoring with combo/streak multipliers, neighborhood-based level progression, cosmetic unlocks, and the free-play sandbox structure. The design "North Star" for all implementation decisions. |
| `initial-plan-rough-draft.md` | An early planning document describing a two-AI workflow (Gemini as architect, Claude as coder) and the step-by-step process for brainstorming, coding, and reviewing. |
| `game-mechanics-overview.md` | A technical review of the background environment, defining Z-depth layers, interactive collision zones, ball scaling/shadow behavior, and potential moving environmental elements. |
| `environment-z-depth-and-collosion.md` | The formal Z-depth and collision specification — defines the 5 rendering layers, Box2D collision mapping, the depth-scaling formula, and reference spawn coordinates for targets. |
| `suburban-crossroads.json` | A machine-readable level definition containing static collider rectangles (houses, fences), target sensor zones (windows, garage door with point values), and spawn lanes for moving targets. |
| `technical-architecture.md` | The overall technical architecture — defines the Gradle multi-module project structure (core/android/desktop/assets), the LibGDX + LibKTX tech stack, the Screen-based application pattern, the depth-augmented 2D coordinate system, and the component-based "Big Bomb" architecture. |

## Tech Stack (Planned)

- **Platform:** Android
- **Engine:** LibGDX with Box2D physics
- **Language:** Kotlin
- **Art Style:** Flat 2.0 vector aesthetic (Recraft for generation, SVG rendering via AmanithSVG)
- **Resolution Target:** 1920x1080 (16:9)
