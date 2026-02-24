# Street Soccer — Backlog

Items are ordered by priority (top = do first). Tags indicate category. Wave assignments show which work package picks up each item.

## Open Items

| #  | Item | Tag | Wave |
|----|------|-----|------|
| 1  | Pause overlay menu | bug | → WP-10 (Wave 3) |
| 2  | Save session integration | integration | → WP-11 (Wave 3) |
| 3  | Settings overlay (functional) | feature | → WP-12 (Wave 3) |
| 4  | Stats overlay (live data) | feature | → WP-15 (Wave 4) |
| 5  | Audio implementation | feature | → WP-13 (Wave 3) |
| 6  | Bomb Mode Button | gameplay | → WP-16 (Wave 4) |
| 7  | Ball Catcher NPC | gameplay | → WP-14 (Wave 3) |
| 8  | Trajectory preview rendering | feature | → WP-17 (Wave 4) |
| 9  | Big Bomb meteor feedback | feature | Wave 5+ |
| 10 | Separate buildings from background | art | → WP-18 (Wave 4) |
| 11 | Flatten front-left hill | art | → WP-19 (Wave 4) |
| 12 | Cosmetic & unlock system | feature | Wave 5+ |
| 13 | Audio Spec | doc | Wave 5+ |
| 14 | Asset Registry (doc + placeholders) | doc | Wave 5+ |
| 15 | Cosmetic & Unlock System Spec | doc | Wave 5+ |
| 16 | Seasonal Variant Templates | doc | Wave 5+ |
| 17 | Big Bomb Meteor Sprite Set | beta | Wave 5+ |
| 18 | Handedness Configuration | beta | Wave 5+ |
| 19 | Testing & Performance Plan | doc | Wave 5+ |
| 20 | Tips System Spec | doc | Wave 5+ |
| 21 | Trajectory Preview Spec | doc | Wave 5+ |
| 22 | Accessibility & Localization | doc | Wave 5+ |
| 23 | Update README.md | doc | Wave 5+ |

## Item Details

### 1. Pause overlay menu `bug`

Pressing pause causes all HUD elements to disappear with nothing replacing them. Should show a pause overlay/menu instead.

### 2. Save session integration `integration`

Wire SaveService into the game loop: merge session stats to career on session end, save on pause/hide, load on resume.

### 3. Settings overlay (functional) `feature`

Replace the placeholder settings panel in AttractScreen with functional controls wired to SettingsData (trajectory preview toggle, slider side, volume sliders).

### 4. Stats overlay (live data) `feature`

Replace the placeholder stats panel in AttractScreen with live ProfileData display (career score, best streak, best session, kicks, targets by type).

### 5. Audio implementation `feature`

Implement `AudioServiceImpl` replacing `NoopAudioService`. Wire sound effects into CollisionSystem (glass-break, metallic clang, car alarm), BALL_IN_FLIGHT (whoosh), and kick launch (bass boom). Use placeholder beep/boop sounds until real assets exist. See GDD Section 9 for all ~12 cues.

### 6. Bomb Mode Button `gameplay`

A red button on the HUD that the player presses before launching to activate "bomb mode" for a powered-up kick. Visual feedback on press, zooms the ball on launch.

### 7. Ball Catcher NPC `gameplay`

A person standing in the middle of the intersection who can catch the ball. Acts as a target or obstacle in the play area.

### 8. Trajectory preview rendering `feature`

Toggleable dotted arc showing predicted ball path during AIMING state. Updates in real-time as angle slider moves. Respects trajectoryPreviewEnabled setting.

### 9. Big Bomb meteor feedback `feature`

Replace the alpha-build red overlay with dedicated fireball sprite + flame trail for Big Bomb flights. See `ui-hud-layout.md` Section 11.

### 10. Separate buildings from background `art`

Extract buildings into separate layers from `background.jpg`. Keep roads in place. The road going straight back should curve right. The upper-right sidewalk continues straight and intersects with the curved road in the back. The horizon sits just behind the curved road with some grass and/or sidewalk. Separate land from sky for future sky replacement.

### 11. Flatten front-left hill `art`

Remove the hill in the front-left of the scene and replace it with flat grass.

### 12. Cosmetic & unlock system `feature`

Ball skins, impact effects, and trail effects (GDD Section 7): selection UI, preview before unlock, unlock threshold calculations (score milestones, streak achievements, distance milestones), runtime application to ball rendering.

### 13. Audio Spec `doc`

Master list of all sound effects with asset IDs, format, duration, volume levels, and mixing/priority rules. GDD Section 9 describes ~12 cues by feel ("crisp glass-break", "deep metallic clang", etc.) but no implementable asset definitions exist.

### 14. Asset Registry (doc + placeholders) `doc`

Starter version listing all known and planned assets: sprites (ball skins, impact effects, trail effects), sounds (from Audio Spec), particle effects (glass-shatter, confetti, sparks, smoke, splinters), background images, and level JSONs. Include asset IDs, source tool, and style notes. Generate Flat 2.0 style placeholder SVGs for all visual elements that don't yet have assets.

### 15. Cosmetic & Unlock System Spec `doc`

Implementation details for ball skins, impact effects, and trail effects: selection UI, preview before unlock, unlock threshold calculations, runtime application to ball rendering.

### 16. Seasonal Variant Templates `doc`

JSON files or schema extensions for the 5 planned variants (Summer Block Party, Halloween, Winter Holidays, Rainy Day, Garage Sale). GDD Section 6 describes thematic changes but no level data or target/spawn-lane modifications are defined.

### 17. Big Bomb Meteor Sprite Set `beta`

Replace the alpha-build red overlay with a dedicated fireball sprite + flame trail for Big Bomb flights. See `ui-hud-layout.md` Section 11.

### 18. Handedness Configuration `beta`

Settings toggle to move the angle slider to the right edge and mirror the steer budget meter. See `ui-hud-layout.md` Section 11.

### 19. Testing & Performance Plan `doc`

Target devices (min API level, min RAM/GPU), frame rate target (60 FPS?), input latency requirements, SVG rendering budget, memory budget for assets.

### 20. Tips System Spec `doc`

Rendering/dismissal UI, trigger logic implementation, tip rotation/frequency, extensibility for new tips. GDD Section 10 defines 4 starter tips.

### 21. Trajectory Preview Rendering Spec `doc`

Dotted arc calculation, update frequency during AIMING vs. BALL_IN_FLIGHT, visual style (dot spacing, color, fade), performance considerations. Described as a toggleable setting in GDD Section 3.

### 22. Accessibility & Localization `doc`

Colorblind mode considerations, text localization approach, font sizing for screen densities, touch target sizing.

### 23. Update README.md `doc`

Reflect current mechanics (three-input model, 2-axis steer, diminishing returns, ball shadow), add references to all spec documents.

## Completed

- [x] Update technical-architecture.md — Add input handling architecture and physics constants — Delivered as standalone specs: `input-system.md` and `physics-and-tuning.md`
- [x] Physics tuning guide — Define actual constants for gravity, drag, Magnus scaling, spin decay rate — Delivered as `physics-and-tuning.md`
- [x] Update game-mechanics-overview.md — Sync with the new flick+steer+angle slider mechanics, 2-axis steer, diminishing returns, and ball shadow
- [x] Fix project name in `state-machine.md` line 7 — Still says "Suburban Striker" instead of "Street Soccer" (committed as `0c6a645`)
- [x] UI/HUD Layout Spec — Delivered as `ui-hud-layout.md`
- [x] Save & Persistence Spec — Delivered as `save-and-persistence.md`
- [x] Menu & Navigation Flow Spec — Delivered as `menu-and-navigation-flow.md`
- [x] Detailed State Behavior Spec — Delivered in `state-machine.md`
- [x] Update technical-architecture.md — Delivered as 12-section architecture spec
- [x] Audio Spec implementation notes — AudioService interface with no-op placeholders wired into states (done in WP-0/WP-8)
