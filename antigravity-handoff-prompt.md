**Project:** Street Soccer — a 2.5D arcade game for Android (LibGDX + Kotlin).

**Status:** All design and architecture specs are complete. No source code exists yet. This is the first implementation session.

**Entry point:** Read `CLAUDE.md` at the project root. It contains the full file map, design decisions, tech stack, and conventions. Every spec document is cross-referenced from there.

**Architecture authority:** `technical-architecture.md` is your implementation blueprint. It defines the project structure, ECS components and systems (ktx-ashley), Box2D integration (PPM=100, zero-gravity world for collision detection only), input architecture, game loop pipeline, rendering pipeline, state management (sealed class FSM), persistence, and asset pipeline. Build to this spec.

**Implementation order:**
1. Scaffold the Gradle multi-module project (`:core`, `:android`, `:desktop`, `:assets`).
2. Generate temporary Flat 2.0 style placeholder SVGs for all visual elements before writing game code — ball, target windows (intact/broken), vehicles, drones, runners, particle effects. Follow the art style described in GDD Section 1 (clean geometry, bold colors, minimal shading). See the Asset Registry implementation note in `backlog.md`.
3. Implement `GameBootstrapper`, `LoadingScreen`, `AttractScreen`, and `LevelScreen` per `technical-architecture.md` Section 3.
4. Build the ECS layer — components and systems per `technical-architecture.md` Section 4.
5. Implement the state machine exactly as defined in `state-machine.md` — all 9 states, entry/exit actions, and transition logic table.
6. Implement the physics pipeline per `physics-and-tuning.md` — fixed-timestep accumulator, gravity, drag, Magnus effect, spin decay. Use the 17 tuning constants from Section 8 as starting values.
7. Implement input handling per `input-system.md` — `InputRouter` with strict pointer-ID tracking to prevent flick/slider interference. State-gated dispatch per Section 5.
8. Build the HUD per `ui-hud-layout.md` — session score, streak badge, steer budget meter, angle slider, score popups, pause icon.
9. Implement persistence per `save-and-persistence.md` — `SaveService`, `ProfileData`, `SettingsData`, session lifecycle, atomic writes.
10. Wire up menus and navigation per `menu-and-navigation-flow.md` — attract screen, pause menu, settings/stats overlays.

**Audio:** Implement an `AudioService` interface with methods mapped to GDD Section 9 triggers. Use silent no-op placeholders — real assets don't exist yet. Wire it into SCORING, IMPACT_MISSED, and BALL_IN_FLIGHT states from the start.

**Level data:** `suburban-crossroads.json` contains all collider geometry, target sensors, spawn lanes, and restitution values. Parse this in `LevelLoader` to build Box2D bodies and ECS entities.

**Key constraint:** Box2D is for collision detection only. Gravity, drag, and Magnus forces are computed in game-space by `PhysicsSystem`, not by Box2D. The Box2D world uses zero gravity.
