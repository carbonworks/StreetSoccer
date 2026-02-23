# Street Soccer — AI Project Context

## What This Project Is

Street Soccer is a **2.5D arcade game** for Android (LibGDX + Kotlin). The player stands at the bottom of a suburban intersection and kicks a soccer ball at targets — windows, vehicles, drones — using a three-input system: angle slider, flick, and mid-flight steer swipes.

## Project Status

**Phase: Early Implementation** — Gradle multi-module project scaffolded (core/android/desktop/assets). ECS components, physics/input/render systems, screens, and placeholder SVGs exist. Many systems are stubs. See `work-packages.md` for the remaining implementation work broken into parallelizable chunks.

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
| `antigravity-handoff-prompt.md` | Implementation handoff prompt for first Antigravity coding session — architecture context and build order | Reference |
| `work-packages.md` | Parallel work packages for agent-based development: WP-0 through WP-9 with file ownership, dependencies, and acceptance criteria | Task definitions |
| `background.jpg` | Level background image — flat-style suburban intersection illustration | Art asset |

## Key Design Decisions Already Made

1. **Three-input kick model**: Angle slider (launch angle) + flick (power/aim) + steer swipes (mid-flight spin)
2. **2-axis steer with diminishing returns**: Swipes use both X (lateral curve) and Y (depth curve) plus swipe speed. Graduated 4-tier curve with no hard cap: 1st at ×1.0, 2nd at ×0.6, 3rd at ×0.25, 4th+ at ×0.1 (residual floor). Resets each kick.
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
- **Absolute paths in Bash**: Never use `cd` in Bash tool calls. Always use absolute paths or pass paths via flags (e.g., `git -C /full/path status` instead of `cd /full/path && git status`).

## Tech Stack (Planned for Implementation)

- **Platform**: Android (primary), Desktop (debug/test)
- **Engine**: LibGDX 1.14.x with Box2D physics
- **Language**: Kotlin with LibKTX extensions
- **Vector rendering**: AmanithSVG
- **Target resolution**: 1920x1080 (16:9)
- **Architecture**: Screen-based (KtxGame), ECS entities (ktx-ashley)

## When Starting a New Session

1. Check `backlog.md` for pending work items
2. Check `work-packages.md` for implementation status
3. Run `git log --oneline -10` to see recent changes
4. If the user references a specific mechanic, read the relevant spec doc before making changes
5. Respect the edit hierarchy: GDD first, then technical specs, then overview

## Parallel Agent Workflow

This project supports parallel development via git worktree-isolated subagents. The user can ask Claude Code to launch multiple agents working on independent packages simultaneously.

### Work Packages

All implementation work is defined in `work-packages.md`. Each package specifies:
- **Owns**: Files the agent creates or heavily modifies (exclusive ownership)
- **Reads**: Spec docs to reference (read-only)
- **Touches**: Shared files where small additions are expected (merge conflicts acceptable)
- **Depends on**: Packages that must be merged first

### How to Launch Agents

When the user asks to run work packages (e.g., "run WP-1 and WP-2" or "kick off Wave 1"):

1. **Read `work-packages.md`** to get the current status and scope of each requested package.
2. **Check dependencies** — only launch packages whose dependencies are merged (status: `done`).
3. **Launch each package as a Task** with `isolation: "worktree"` and `subagent_type: "Bash"` or `"general-purpose"`. Each agent gets its own git worktree (isolated branch).
4. **Provide each agent a clear prompt** including:
   - The full text of the work package scope and acceptance criteria
   - The list of spec docs to read (from the "Reads" field)
   - Instructions to commit their work on the worktree branch when done
   - The project's commit conventions (imperative mood, no Co-Authored-By)
5. **Monitor agents** — check output periodically. When agents complete, their worktree branches contain the work.
6. **Merge completed branches** into `main` one at a time. Resolve any conflicts (most likely in shared files listed under "Touches"). Use standard `git merge` — do not squash, so the branch history is preserved.
7. **Update `work-packages.md`** — set the merged package's status to `done`.
8. **After merging WP-0**, Wave 2 packages become unblocked. Report this to the user.

### Agent Prompt Template

When launching a worktree agent for a work package, use this structure:

```
You are implementing work package WP-{N} for the Street Soccer project.

## Your Task
{paste the full Scope section from work-packages.md}

## Acceptance Criteria
{paste the Acceptance section}

## Key References
Read these files before writing code:
- {list from Reads field}
- CLAUDE.md (project conventions)

## File Ownership
You OWN (create/modify freely): {Owns list}
You may ADD SMALL CHANGES to: {Touches list}
Do NOT modify any other files.

## When Done
- Commit your work with an imperative-mood message summarizing what you built.
- Do not add Co-Authored-By or any AI attribution.
- Do not push — just commit locally on this branch.
```

### Conflict Resolution

Conflicts are most likely in files listed under "Touches" for multiple packages (primarily `LevelScreen.kt`, `GameBootstrapper.kt`, `LevelLoader.kt`). When merging:
- These are typically additive (adding system registrations, init calls) — take both sides.
- If logic conflicts occur, the work package's spec doc is the authority for its subsystem.

### Running Agents Concurrently

Launch all independent packages in a single message with multiple Task tool calls. For example, Wave 1 (WP-0, WP-6, WP-7, WP-8) can all run simultaneously since they have no dependencies on each other.
