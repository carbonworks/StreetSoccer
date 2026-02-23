# Street Soccer — AI Project Context

## What This Project Is

Street Soccer is a **2.5D arcade game** for Android (LibGDX + Kotlin). The player stands at the bottom of a suburban intersection and kicks a soccer ball at targets — windows, vehicles, drones — using a three-input system: angle slider, flick, and mid-flight steer swipes. There is no code yet; the project is in the **design/specification phase**.

## Project Status

**Phase: Documentation & Design** — No source code exists. All work so far is specification documents that define the game's mechanics, physics, input system, and environment. The next major milestone is beginning implementation in LibGDX/Kotlin.

## File Map

| File | Role | Authority Level |
|------|------|-----------------|
| `game-design-document.md` | **Design authority** — the "North Star" for what the game feels like. Update this first when mechanics change. | Primary |
| `input-system.md` | Technical spec for touch input: angle slider, flick detection, steer swipe detection (2-axis + diminishing returns) | Derives from GDD Section 3 |
| `physics-and-tuning.md` | Physics model: flight equations, dual-axis Magnus effect (spinX/spinY), drag, Big Bomb, ball shadow, 17 tuning constants | Derives from GDD Section 3 |
| `environment-z-depth-and-collosion.md` | Z-layer architecture (5 layers), collision mapping, depth scaling formula, ball shadow rendering zones | Spatial framework |
| `state-machine.md` | Game states (BOOT → LOADING → MAIN_MENU → READY → AIMING → BALL_IN_FLIGHT → SCORING/IMPACT_MISSED → READY) | Control flow |
| `game-mechanics-overview.md` | Concise mechanics summary — quick-reference for the three-input model, flight physics, scoring, Big Bombs | Summary (derives from all above) |
| `ui-hud-layout.md` | UI/HUD layout spec: screen positions, sizing, and behavior for all alpha-scope HUD elements | Derives from GDD Section 9 |
| `save-and-persistence.md` | Save system: JSON storage via `Gdx.files.local()`, domain objects, session lifecycle, save triggers, schema versioning, error handling | Derives from GDD Section 7 |
| `menu-and-navigation-flow.md` | Menu structure, screen flow, and navigation: attract screen, variant selection, pause menu, settings/stats/cosmetics overlays, Android back button, tips integration | Derives from GDD + state-machine.md |
| `technical-architecture.md` | Architecture blueprint: project structure, ECS (ktx-ashley components/systems), Box2D integration, input architecture, game loop pipeline, rendering, state management, persistence, asset pipeline | Architecture |
| `suburban-crossroads.json` | Level data: collider geometry, target sensors, spawn lanes, restitution values | Level definition |
| `backlog.md` | Outstanding work items (checked = done, unchecked = pending) | Task tracking |
| `documentation-overview.md` | Original doc planning checklist (historical — largely superseded by actual docs) | Historical |
| `initial-plan-rough-draft.md` | Early brainstorm about AI workflow (historical) | Historical |
| `background.jpg` | Level background image — flat-style suburban intersection illustration | Art asset |

## Key Design Decisions Already Made

1. **Three-input kick model**: Angle slider (launch angle) + flick (power/aim) + steer swipes (mid-flight spin)
2. **2-axis steer with diminishing returns**: Swipes use both X (lateral curve) and Y (depth curve) plus swipe speed. Budget of 4 swipes per kick: swipes 1-2 full effect, 3rd at 25%, 4th+ at 0%. Resets each kick.
3. **Dual-axis spin**: `spinX` (lateral) and `spinY` (depth) — both decay independently. Magnus force applies to vx and vy respectively.
4. **Ball shadow**: Ground-plane projection below the ball, scaled by depth formula, opacity fades with height (`SHADOW_FADE_HEIGHT` = 400px).
5. **Free play / sandbox**: No lives, no timer, no shot limit. Engagement via streaks, personal bests, and cosmetic unlocks.
6. **Single street, seasonal variants**: One level layout (Suburban Crossroads) re-skinned for seasons/events.
7. **Big Bomb**: Dual-threshold activation (power >= 0.9, slider >= 0.7) sends ball into central corridor for distance-based scoring.

## Conventions

- **Edit hierarchy**: When mechanics change, update `game-design-document.md` first, then propagate to technical specs, then to `game-mechanics-overview.md`.
- **Section cross-references**: Docs reference each other by section number. After adding/removing sections, audit cross-references in all docs.
- **Tuning constants**: All physics constants live in `physics-and-tuning.md` Section 8 with suggested values and valid ranges. Currently 17 constants.
- **Commit style**: Imperative mood, 1-2 sentence summary of "why".
- **No Co-Authored-By**: Never add a Co-Authored-By trailer or any other attribution of AI tools in commit messages. All work in this repository belongs solely to the human author.

## Tech Stack (Planned for Implementation)

- **Platform**: Android (primary), Desktop (debug/test)
- **Engine**: LibGDX 1.14.x with Box2D physics
- **Language**: Kotlin with LibKTX extensions
- **Vector rendering**: AmanithSVG
- **Target resolution**: 1920x1080 (16:9)
- **Architecture**: Screen-based (KtxGame), component-based entities (ktx-ashley or Kotlin delegates)

## When Starting a New Session

1. Check `backlog.md` for pending work items
2. Run `git log --oneline -10` to see recent changes
3. If the user references a specific mechanic, read the relevant spec doc before making changes
4. Respect the edit hierarchy: GDD first, then technical specs, then overview
