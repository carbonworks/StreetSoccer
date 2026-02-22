# Street Soccer — Backlog

## Completed

- [x] Update technical-architecture.md — Add input handling architecture and physics constants — Delivered as standalone specs: `input-system.md` and `physics-and-tuning.md`
- [x] Physics tuning guide — Define actual constants for gravity, drag, Magnus scaling, spin decay rate — Delivered as `physics-and-tuning.md`
- [x] Update game-mechanics-overview.md — Sync with the new flick+steer+angle slider mechanics, 2-axis steer, diminishing returns, and ball shadow

## Quick Fixes

- [x] Fix project name in `state-machine.md` line 7 — Still says "Suburban Striker" instead of "Street Soccer" (committed as `0c6a645`)

## Documentation — Needed Before Implementation

- [x] UI/HUD Layout Spec — Delivered as `ui-hud-layout.md`: 11-section spec covering session score, streak badge, steer budget meter, angle slider, score popups, pause icon, Big Bomb meteor feedback, trajectory preview, ball shadow, and out-of-scope list
- [x] Save & Persistence Spec — Delivered as `save-and-persistence.md`: 10-section spec covering JSON storage via `Gdx.files.local()`, domain objects (`ProfileData`, `SettingsData`), session lifecycle, save triggers, schema versioning, and error handling
- [x] Menu & Navigation Flow Spec — Delivered as `menu-and-navigation-flow.md`: 11-section spec covering attract screen, variant selection, pause menu, settings/stats/cosmetics overlays, Android back button behavior, state machine integration, and tips integration

## Documentation — Important, Can Follow First Implementation Pass

- [ ] Audio Spec — Master list of all sound effects with asset IDs, format, duration, volume levels, and mixing/priority rules. GDD Section 9 describes ~12 cues by feel ("crisp glass-break", "deep metallic clang", etc.) but no implementable asset definitions exist. Deferred from pre-implementation — core loop can be built without audio; hooks added later.
- [ ] Asset Registry — Starter version listing all known and planned assets: sprites (ball skins, impact effects, trail effects), sounds (from Audio Spec), particle effects (glass-shatter, confetti, sparks, smoke, splinters), background images, and level JSONs. Include asset IDs, source tool (Recraft/Claude SVG), and style notes.
- [ ] Cosmetic & Unlock System Spec — Implementation details for ball skins, impact effects, and trail effects (GDD Section 7): selection UI, preview before unlock, unlock threshold calculations (score milestones, streak achievements, distance milestones), runtime application to ball rendering.
- [ ] Seasonal Variant Templates — JSON files or schema extensions for the 5 planned variants (Summer Block Party, Halloween, Winter Holidays, Rainy Day, Garage Sale). GDD Section 6 describes thematic changes but no level data or target/spawn-lane modifications are defined.
- [ ] Detailed State Behavior Spec — Flesh out underspecified states in `state-machine.md`: PAUSED (UI shown, settings access, save behavior), IMPACT_MISSED (animation duration, skip behavior, immediate vs. delayed transition to READY), SCORING (exact feedback sequence, when streak UI updates).
- [ ] Update technical-architecture.md — Current version is high-level and outdated; doesn't reference the actual state machine, input system, or physics specs. Needs concrete class architecture, ContactListener implementation approach, component model decision (ktx-ashley vs. Kotlin delegates), and 2.5D-to-Box2D coordinate mapping.

## Documentation — Lower Priority

- [ ] Testing & Performance Plan — Target devices (min API level, min RAM/GPU), frame rate target (60 FPS?), input latency requirements, SVG rendering budget (max on-screen count), memory budget for assets.
- [ ] Tips System Spec — GDD Section 10 defines 4 starter tips but no spec for: rendering/dismissal UI, trigger logic implementation, tip rotation/frequency, extensibility for new tips.
- [ ] Trajectory Preview Rendering Spec — Described as a toggleable setting (GDD Section 3) but no implementation details: dotted arc calculation, update frequency during AIMING vs. BALL_IN_FLIGHT, visual style (dot spacing, color, fade), performance considerations.
- [ ] Accessibility & Localization — Colorblind mode considerations, text localization approach, font sizing for screen densities, touch target sizing.

## Beta Scope

- [ ] Big Bomb Meteor Sprite Set — Replace the alpha-build red overlay with a dedicated fireball sprite + flame trail for Big Bomb flights. See `ui-hud-layout.md` Section 11.
- [ ] Handedness Configuration — Settings toggle to move the angle slider to the right edge and mirror the steer budget meter. See `ui-hud-layout.md` Section 11.

## Pending (Carried Over)

- [ ] Update README.md — Reflect current mechanics (three-input model, 2-axis steer, diminishing returns, ball shadow), add references to all spec documents including `input-system.md`, `physics-and-tuning.md`, `state-machine.md`, and `CLAUDE.md`
