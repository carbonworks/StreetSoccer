# No Forward Debt Policy

Street Soccer is a ship-once, stabilize, publish project. Each wave delivers complete, correct work. Technical debt discovered or created within a work package MUST be resolved before the WP merges. No debt crosses a wave boundary.

---

## Core Principles

### 1. WP Boundary is Debt Boundary

Technical debt discovered or created within a WP must be resolved before that WP's branch merges to main. This includes:

- Resource leaks (undisposed textures, fonts, batches, ShapeRenderers, Pixmaps, Box2D bodies)
- Magic numbers (hardcoded values that belong in TuningConstants or a companion object)
- Unsafe patterns (nullable access without checks, reflection-based getComponent() instead of cached mappers)
- Missing tests or verification steps
- Coupling violations (systems directly accessing other systems' internals)

**Prohibited language in commits and docs:**
- "TODO: fix later"
- "Legacy pattern still works"
- "Migration priority is LOW"
- "Will address in a future wave"

**Acceptable:** "Discovered issue #N during WP-X — resolved in this WP" or "Created backlog item #N with P0 severity — resolved before wave merge."

### 2. Build It Right the First Time

Each WP designs and implements the correct solution for its scope. Avoid "works for now" patterns:

- Use cached component mappers (`mapperFor<>()`), never `entity.getComponent()`
- Dispose every Disposable you create, in the same class that creates it
- Pair every `begin()` with an `end()` — SpriteBatch, ShapeRenderer, Stage
- Never mix ShapeRenderer and SpriteBatch in the same begin/end block
- Use TuningConstants for all physics and gameplay values, never inline numbers
- Return nullable types from component accessors; check nulls at call sites

### 3. No Retroactive Innovation

Once a WP's feature is merged and working, it is done. Future WPs do not "improve" or "clean up" previously shipped code unless:

- A bug is filed against it (with reproduction steps)
- A new WP's scope explicitly requires modifying it (documented in Owns/Touches)
- A code review finding is logged as a backlog item with severity

Working code that meets its acceptance criteria stays untouched.

### 4. Discovered Work Gets Tracked Immediately

When a WP agent discovers an issue outside its scope during implementation:

1. **In-scope fix** — If the fix is small and directly supports the WP's acceptance criteria, fix it in this WP. Document what was found and fixed in the commit message.
2. **Out-of-scope issue** — If the fix is outside this WP's file ownership or scope, create a backlog item with severity and continue. Do NOT attempt cross-WP fixes from a worktree.
3. **Blocking issue** — If the discovered issue prevents the WP from meeting acceptance criteria, stop and report. The issue becomes P0 and must be resolved before the wave merges.

---

## Enforcement: WP Completion Checklist

Before any WP branch merges to main, ALL of the following must be true:

### Build & Correctness
- [ ] `./gradlew build` passes with no errors
- [ ] No new compiler warnings introduced (check `compileKotlin` output)
- [ ] All acceptance criteria from the WP definition are met

### Resource Management
- [ ] Every `Texture`, `Pixmap`, `BitmapFont`, `SpriteBatch`, `ShapeRenderer`, `Stage`, and `World` created in this WP has a corresponding `dispose()` call
- [ ] `Pixmap` objects are disposed immediately after creating a `Texture` from them
- [ ] No per-frame allocations in `update()` or `render()` methods (no `BitmapFont()`, `GlyphLayout()`, `Color()`, or string concatenation in hot paths)
- [ ] Box2D bodies are destroyed before their entities are removed from the engine

### ECS Patterns
- [ ] All component access uses cached `mapperFor<>()` mappers, never `entity.getComponent()`
- [ ] Component accessors return nullable types or are guarded with null checks
- [ ] Systems only access components through their declared Family
- [ ] No system directly references another system's internal state

### Rendering
- [ ] `SpriteBatch.begin()` and `end()` are always paired in the same method
- [ ] `ShapeRenderer.begin()` and `end()` are always paired in the same method
- [ ] ShapeRenderer is never used inside an active SpriteBatch block (or vice versa)
- [ ] Batch color/blend state is reset after Stage.draw() or tinted rendering
- [ ] GL blend state (`glEnable/glDisable GL_BLEND`) is balanced

### Constants & Configuration
- [ ] No magic numbers — all gameplay/physics values are in `TuningConstants` or named companion object constants
- [ ] No duplicate constants across files (e.g., HORIZON_Y should exist in one place)
- [ ] Collision tolerances, bounds, dimensions, and thresholds are named and centralized

### Input & State
- [ ] Input processors return correct boolean values (true = consumed, false = propagate)
- [ ] Game state checks use `is` type checks, not string comparisons
- [ ] Settings values (from SettingsData) are actually read and applied, not ignored

### No Debt Created
- [ ] No TODO comments added (fix it now or file a backlog item)
- [ ] No "temporary" or "placeholder" code without a corresponding backlog item for replacement
- [ ] No silent exception swallowing (every catch block logs or handles meaningfully)
- [ ] No fallback heuristics when a proper solution exists

---

## Enforcement: Wave Completion Gate

Before defining the next wave, ALL of the following must be true:

- [ ] Every WP in the wave has been merged and passes its completion checklist
- [ ] `./gradlew build` passes on main after all merges
- [ ] No P0 or P1 backlog items remain from this wave's discovered issues
- [ ] `backlog.md` is updated: completed items moved, new items added with severity
- [ ] `work-packages.md` is updated: all WP statuses set to `done`
- [ ] Merge conflicts were resolved correctly (both sides taken for additive changes; spec doc is authority for logic conflicts)

---

## Severity Levels for Discovered Issues

| Severity | Definition | Resolution Timing |
|----------|-----------|-------------------|
| P0 | Crash, data loss, or blocks other WPs | Before this WP merges |
| P1 | Incorrect behavior visible to the player | Before the wave merges |
| P2 | Performance issue, code smell, or minor UX problem | Before the next wave starts |
| P3 | Polish, documentation, or style improvement | Prioritized in backlog normally |

---

## WP Agent Prompt Addition

When launching worktree agents, append this to the agent prompt:

```
## Quality Rules (No Forward Debt)
Before committing, verify:
1. Every Disposable you created has a dispose() call
2. All component access uses mapperFor<>() mappers, not getComponent()
3. No magic numbers — use TuningConstants or named constants
4. No ShapeRenderer inside SpriteBatch begin/end blocks (or vice versa)
5. No per-frame allocations (new Font(), new Color(), string templates in logs)
6. No TODO comments — fix it or file a backlog item
7. ./gradlew build passes
```

---

## Applying This Policy Retroactively

The existing codebase has accumulated debt from Waves 1-4 (backlog items #26-42). These are grandfathered as known issues with assigned severity. They must be resolved according to their severity:

- **P0/Critical (#26, #27, #28, #29):** Resolve in Wave 5 before any new features
- **P1/Important (#30, #31, #32, #33, #34, #35):** Resolve in Wave 5 or Wave 6
- **P2/Medium (#36, #37, #38):** Resolve before beta
- **P3/Polish (#39, #40, #41, #42):** Prioritized in backlog normally

Once the existing debt is cleared, no new debt moves forward from any wave.
