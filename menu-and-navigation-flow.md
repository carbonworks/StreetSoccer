**This document defines the menu structure, screen flow, and navigation behavior for Street Soccer.** It covers every screen and overlay the player interacts with outside of active gameplay: the main menu (attract screen), pause menu, settings, and stats. For the in-gameplay HUD, see `ui-hud-layout.md`. For the game states that host these menus, see `state-machine.md`.

> **Alpha vs. Post-Alpha:** Sections marked **(Post-Alpha)** are fully designed but deferred from the alpha build. They remain in this spec as the design target for future implementation. All other sections describe the alpha-scope menu surface.

---

## 1. Navigation Map

All navigation follows a **flat, arcade-style structure** — at most two taps from the main menu to any function, and at most two taps from active gameplay to any setting.

```
                         ┌─────────────────────┐
                         │       BOOT           │
                         │   (auto-advance)     │
                         └─────────┬────────────┘
                                   ▼
                         ┌─────────────────────┐
                         │      LOADING         │
                         │   (auto-advance)     │
                         └─────────┬────────────┘
                                   ▼
┌──────────────────────────────────────────────────────────┐
│                  MAIN_MENU STATE                         │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │           ATTRACT SCREEN (base layer)              │  │
│  │                                                    │  │
│  │       TAP TO PLAY ──→ READY (gameplay begins)      │  │
│  │                                                    │  │
│  │   [Stats]  [Settings]                              │  │
│  │      │         │                                   │  │
│  └──────┼─────────┼──────────────────────────────────-┘  │
│         ▼         ▼                                      │
│   Stats Overlay  Settings                                │
│   (read-only)    Overlay                                 │
│                                                          │
│   ← Only ONE overlay active at a time →                  │
└──────────────────────────────────────────────────────────┘
                                   │
                          TAP TO PLAY
                                   ▼
┌──────────────────────────────────────────────────────────┐
│              GAMEPLAY STATES                              │
│         (READY / AIMING / BALL_IN_FLIGHT /               │
│          SCORING / IMPACT_MISSED)                         │
│                                                          │
│   Pause icon (top-left) or Android Back                  │
│                    │                                     │
└────────────────────┼─────────────────────────────────────┘
                     ▼
┌──────────────────────────────────────────────────────────┐
│                  PAUSED STATE                             │
│                                                          │
│   ┌────────────────────────────────────────────────────┐ │
│   │              PAUSE MENU (base layer)               │ │
│   │                                                    │ │
│   │   RESUME  ──→ return to pre-pause gameplay state   │ │
│   │   SETTINGS ──→ Settings Overlay                    │ │
│   │   QUIT ──→ MAIN_MENU (session end + save)          │ │
│   └────────────────────────────────────────────────────┘ │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### Design Principles

- **Two-tap maximum**: Any function is reachable in at most 2 taps from wherever the player is.
- **Overlays, not screens**: Secondary panels (stats, settings) are overlays within their parent state — they do not trigger game state transitions.
- **One overlay at a time**: Opening an overlay closes any currently open overlay. No stacking.
- **Arcade minimal**: Icons over text, bold options, fast transitions. Get into gameplay fast.

---

## 2. Main Menu (Attract Screen)

The main menu is an **arcade attract screen** — a bold, inviting entry point that draws the player into gameplay with minimal friction.

### Layout

```
┌──────────────────────────────────────────────────┐
│                                                  │
│              S T R E E T   S O C C E R           │
│              ─────────────────────────            │
│                    (game title)                   │
│                                                  │
│                                                  │
│                                                  │
│            ╔═════════════════════════╗            │
│            ║     TAP  TO  PLAY      ║            │
│            ╚═════════════════════════╝            │
│                  (pulsing glow)                   │
│                                                  │
│                                                  │
│                                                  │
│              [🏆]          [⚙]                    │
│              Stats       Settings                │
│                  (icons only)                     │
└──────────────────────────────────────────────────┘
```

### Elements

| Element | Position | Behavior |
|---------|----------|----------|
| **Game title** | Top-center, ~15% from top edge | Static. Large, bold, arcade-style font. |
| **TAP TO PLAY** | Center of screen | The dominant interactive element. Pulsing glow animation (scale oscillates ~95%–105%, opacity breathes between 80%–100%) on a ~2 s cycle. Tap transitions to READY state. |
| **Bottom icon bar** | Bottom edge, horizontally centered, ~48 px from bottom | Two evenly spaced icon buttons (~64×64 px touch targets). No text labels — icons only. Left: trophy icon (Stats). Right: gear icon (Settings). |

### Background

The full-screen level background (`background.jpg`) renders behind all menu elements, establishing the game's visual identity immediately.

### First-Launch Behavior

On first launch (no `profile.json` exists), the attract screen appears identically — there is no separate onboarding flow.

---

## 3. Variant Selection Overlay (Post-Alpha)

> **Deferred.** Only one variant (`suburban_crossroads`) exists in the alpha build. This overlay and the attract-screen variant badge will be implemented when a second variant ships. The design is retained here as the target for that milestone.

Accessible from a **variant badge** on the main menu attract screen (badge not shown in alpha). Not accessible from the pause menu.

### Layout

A horizontal carousel of variant cards, centered vertically on screen over a semi-transparent dimmed background.

```
┌──────────────────────────────────────────────────┐
│                                          [✕]     │
│                                                  │
│                                                  │
│     ┌─────────┐  ┌─────────┐  ┌─────────┐       │
│     │ ░░░░░░░ │  │ Suburban │  │ ░░░░░░░ │       │
│     │ ░░░░░░░ │  │ Cross-  │  │ ░░░░░░░ │       │
│     │ LOCKED  │  │  roads   │  │ LOCKED  │       │
│     │ 50,000  │  │         │  │ 100,000 │       │
│     │  pts    │  │  ✓ selected │  pts    │       │
│     └─────────┘  └─────────┘  └─────────┘       │
│                                                  │
│        ◄  swipe or tap arrows to browse  ►       │
│                                                  │
└──────────────────────────────────────────────────┘
```

### Behavior

| Aspect | Detail |
|--------|--------|
| **Card content (unlocked)** | Variant name + thumbnail of the variant's background art. Selected variant shows a checkmark or highlight border. |
| **Card content (locked)** | Silhouette thumbnail with the score milestone needed to unlock (e.g., "50,000 pts"). Not tappable. |
| **Selection** | Tap an unlocked card to select it. Selection takes effect immediately — the overlay closes and the variant badge on the attract screen updates. Selection is saved to `ProfileData.variants.selectedVariant` immediately (see `save-and-persistence.md` Section 6). |
| **Browsing** | Swipe left/right to scroll the carousel, or tap arrow indicators at the edges. |
| **Close** | X button (top-right) or Android back button. |
| **Data source** | Unlocked variants come from `ProfileData.variants.unlockedVariants`. Default: `suburban_crossroads` only. |

### Alpha-to-Post-Alpha Additions

When this section is implemented, the attract screen (Section 2) gains:
- A **variant badge** between the title and TAP TO PLAY, displaying the selected variant name. Tap opens this overlay.
- The bottom icon bar layout may be adjusted to accommodate the badge visually.

---

## 4. Pause Menu

Triggered during active gameplay by the **pause icon** (top-left, 64×64 px — see `ui-hud-layout.md` Section 7) or by the **Android back button**.

### Layout

A semi-transparent overlay dims the frozen game scene. Three large, vertically stacked buttons dominate the center.

```
┌──────────────────────────────────────────────────┐
│                                                  │
│          ┌──────────────────────────┐            │
│          │                          │            │
│          │        R E S U M E       │            │
│          │                          │            │
│          ├──────────────────────────┤            │
│          │                          │            │
│          │      S E T T I N G S     │            │
│          │                          │            │
│          ├──────────────────────────┤            │
│          │                          │            │
│          │         Q U I T          │            │
│          │                          │            │
│          └──────────────────────────┘            │
│                                                  │
└──────────────────────────────────────────────────┘
       (dimmed game scene behind overlay)
```

### Button Behavior

| Button | Action |
|--------|--------|
| **RESUME** | Returns to the pre-pause gameplay state. Unpauses physics, restores HUD. |
| **SETTINGS** | Opens the Settings Overlay (Section 5) on top of the pause menu. |
| **QUIT** | Returns to MAIN_MENU immediately. Triggers session end: session stats are merged into `CareerStats` and `profile.json` is written (see `save-and-persistence.md` Section 5). **No confirmation dialog** — arcade games don't ask "are you sure?" when you walk away. |

### Style

- Buttons are large, bold, and arcade-style — wide rounded rectangles with uppercase spaced lettering.
- The overlay background is a semi-transparent dark fill (e.g., `rgba(0, 0, 0, 0.7)`) over the frozen game scene.
- The game world behind the overlay is visible but clearly dimmed and inactive.

---

## 5. Settings Overlay

Accessible from **two entry points**: the gear icon on the main menu bottom bar, and the SETTINGS button in the pause menu.

### Layout

A centered panel over a semi-transparent dimmed background. Contains the alpha-scope settings from `SettingsData` (see `save-and-persistence.md` Section 3).

```
┌──────────────────────────────────────────────────┐
│                                          [✕]     │
│                                                  │
│          ┌──────────────────────────┐            │
│          │       S E T T I N G S    │            │
│          ├──────────────────────────┤            │
│          │                          │            │
│          │  Trajectory Preview      │            │
│          │              [OFF | on ] │            │
│          │                          │            │
│          │  Master Volume           │            │
│          │     ●━━━━━━━━━━━━━━━━━━  │            │
│          │                          │            │
│          │  SFX Volume              │            │
│          │     ━━━━━━━━━━━━━━━━━●━  │            │
│          │                          │            │
│          └──────────────────────────┘            │
│                                                  │
└──────────────────────────────────────────────────┘
```

### Controls

| Setting | Control Type | Maps To |
|---------|-------------|---------|
| **Trajectory Preview** | Toggle switch (on/off) | `SettingsData.trajectoryPreviewEnabled` |
| **Master Volume** | Horizontal slider (0.0–1.0) | `SettingsData.masterVolume` |
| **SFX Volume** | Horizontal slider (0.0–1.0) | `SettingsData.sfxVolume` |

### Behavior

| Aspect | Detail |
|--------|--------|
| **Save behavior** | Changes save immediately to `settings.json` on each value change (see `save-and-persistence.md` Section 6). No "Apply" or "Save" button needed. |
| **Close** | X button (top-right) or Android back button. Returns to the previous context (attract screen or pause menu). |

### Post-Alpha Addition: Slider Side

The `SettingsData.sliderSide` field exists in the data model but the **Slider Side toggle is deferred to post-alpha**, alongside the handedness mirroring logic (see `ui-hud-layout.md` Section 11). When implemented, a left/right toggle will be added to this panel.

---

## 6. Stats Overlay

Accessible from the **trophy icon** on the main menu bottom bar. Read-only.

### Layout

A centered panel displaying all `CareerStats` fields (see `save-and-persistence.md` Section 3) in an arcade-style presentation.

```
┌──────────────────────────────────────────────────┐
│                                          [✕]     │
│                                                  │
│          ┌──────────────────────────┐            │
│          │     H I G H   S C O R E  │            │
│          ├──────────────────────────┤            │
│          │                          │            │
│          │  Total Kicks      1,247  │            │
│          │  Total Hits         683  │            │
│          │  Hit Rate         54.8%  │            │
│          │  Total Score    142,650  │            │
│          │  Best Session    12,480  │            │
│          │  Best Streak         11  │            │
│          │  Longest Bomb    412.5m  │            │
│          │                          │            │
│          │  ── Targets ──           │            │
│          │  Windows           342   │            │
│          │  Vehicles          127   │            │
│          │  Drones             89   │            │
│          │  Garage Doors       78   │            │
│          │  Runners            47   │            │
│          │                          │            │
│          └──────────────────────────┘            │
│                                                  │
└──────────────────────────────────────────────────┘
```

### Data Mapping

| Display Field | Source | Notes |
|---------------|--------|-------|
| **Total Kicks** | `CareerStats.totalKicks` | Plain integer with thousands separator |
| **Total Hits** | `CareerStats.totalHits` | Plain integer with thousands separator |
| **Hit Rate** | Computed: `totalHits / totalKicks × 100` | Percentage with one decimal. Shows "—" if `totalKicks` is 0. |
| **Total Score** | `CareerStats.totalScore` | Plain integer with thousands separator |
| **Best Session** | `CareerStats.bestSessionScore` | Highest single-session score |
| **Best Streak** | `CareerStats.bestStreak` | Consecutive hits |
| **Longest Bomb** | `CareerStats.longestBigBombDistance` | Float with one decimal + "m" suffix |
| **Targets by type** | `CareerStats.targetsByType` | Vertical list, one row per type. Key names from the map are displayed with title case. |

### Style

- **"HIGH SCORE"** header in large, bold, arcade-style lettering.
- Big numbers, bold labels — the stats should feel like a coin-op leaderboard.
- Read-only — no interactive elements besides the close button.
- Close via X button (top-right) or Android back button.

---

## 7. Cosmetics Overlay (Post-Alpha)

> **Deferred.** The alpha build ships with one item per cosmetic category (`classic_white` ball, `default_shatter` impact, `none` trail). This overlay will be implemented alongside the Cosmetic & Unlock System Spec (see `backlog.md`), when multiple items per category exist. The design is retained here as the target.

Accessible from a **palette icon** on the main menu bottom bar (icon not shown in alpha).

### Layout

A panel with three horizontal sections (or tabs), one for each cosmetic category.

```
┌──────────────────────────────────────────────────┐
│                                          [✕]     │
│                                                  │
│    [ Ball Skins ]  [ Impact FX ]  [ Trail FX ]   │
│    ─────────────────────────────────────────────  │
│                                                  │
│     ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐          │
│     │     │  │     │  │░░░░░│  │░░░░░│          │
│     │ ⚽  │  │ 🟠  │  │░░░░░│  │░░░░░│          │
│     │     │  │     │  │     │  │     │          │
│     │classic│ │orange│ │chrome│ │flame│          │
│     │ ✓   │  │     │  │ 🔒  │  │ 🔒  │          │
│     └─────┘  └─────┘  └─────┘  └─────┘          │
│                                                  │
│     selected    unlocked   locked     locked     │
│                                                  │
└──────────────────────────────────────────────────┘
```

### Behavior

| Aspect | Detail |
|--------|--------|
| **Tabs/sections** | Three categories matching `CosmeticState` (see `save-and-persistence.md` Section 3): Ball Skins, Impact Effects, Trail Effects. |
| **Grid items (unlocked)** | Icon/thumbnail of the cosmetic. Tappable. The currently selected item has a highlight border or glow. |
| **Grid items (locked)** | Silhouette icon. Not tappable. May display the unlock condition (e.g., "10,000 pts") if known. |
| **Selection** | Tap an unlocked item to select it immediately — no confirmation dialog. The highlight moves to the tapped item. Selection is saved to the corresponding `CosmeticState` field (`selectedBallSkin`, `selectedImpactEffect`, or `selectedTrailEffect`) immediately (see `save-and-persistence.md` Section 6). |
| **Close** | X button (top-right) or Android back button. |
| **Data source** | Unlocked/selected state from `ProfileData.cosmetics`. Unlock thresholds and progression logic are defined by the future Cosmetic & Unlock System Spec (see `backlog.md`). |

### Alpha-to-Post-Alpha Additions

When this section is implemented, the attract screen (Section 2) gains a **palette icon** in the bottom icon bar (expanding from 2 to 3 icons).

---

## 8. Android Back Button Behavior

The Android back button follows a consistent **"go up one level"** pattern throughout the app.

| Context | Back Button Action |
|---------|-------------------|
| **Overlay open** (stats, settings) | Close the overlay. Return to the underlying screen (attract screen or pause menu). |
| **Pause menu** (no overlay) | Same as tapping RESUME — return to the pre-pause gameplay state. |
| **Main menu attract screen** (no overlay) | Show an exit confirmation prompt: **"LEAVE?"** with **YES** / **NO** buttons. YES exits the app; NO dismisses the prompt. |
| **Active gameplay** (READY, AIMING, BALL_IN_FLIGHT, SCORING, IMPACT_MISSED) | Transition to PAUSED state (same as tapping the pause icon). |

### Why the Exit Confirmation

This is the **only confirmation dialog** in the entire navigation flow. It exists because Android users expect the back button to be non-destructive — accidentally closing the app would lose the player's sense of where they were. Every other navigation action (including QUIT from the pause menu) is instant and confirmation-free, staying true to the arcade ethos.

### Post-Alpha Back Button Contexts

When deferred overlays are implemented (variant select, cosmetics, tips), the back button closes them identically to the alpha overlays — no new behavior, just more contexts in the table above.

---

## 9. State Machine Integration

All menus and overlays map to **existing game states** defined in `state-machine.md`. No new game states are required.

### State-to-UI Mapping

| Game State | UI Layer | Managed Overlays |
|------------|----------|-----------------|
| **MAIN_MENU** | Attract screen (Section 2) | Stats, Settings — at most one active |
| **PAUSED** | Pause menu (Section 4) | Settings only |
| **READY / AIMING / BALL_IN_FLIGHT / SCORING / IMPACT_MISSED** | Gameplay HUD (see `ui-hud-layout.md`) | None — pause icon is the only menu entry point |

### Menu-Driven State Transitions

| From State | Trigger | To State | Side Effects |
|------------|---------|----------|-------------|
| MAIN_MENU | TAP TO PLAY | READY | Session begins. Session counters initialize to zero (see `save-and-persistence.md` Section 5). |
| READY / AIMING / BALL_IN_FLIGHT / SCORING / IMPACT_MISSED | Pause icon tap or Android back | PAUSED | Physics paused. If ball was in flight, the in-progress kick is frozen (not discarded — it resumes on RESUME). |
| PAUSED | RESUME button or Android back | Pre-pause state | Physics resumed. Ball continues from frozen position if it was in flight. |
| PAUSED | QUIT button | MAIN_MENU | Session ends. Stats merged and saved (see `save-and-persistence.md` Section 5). |

### Overlay Transitions (No State Change)

These transitions happen **within** a game state — they do not change the FSM state.

| Current State | Action | Result |
|---------------|--------|--------|
| MAIN_MENU | Tap Stats icon | Open Stats Overlay |
| MAIN_MENU | Tap Settings icon | Open Settings Overlay |
| MAIN_MENU | Close any overlay (X or back) | Return to attract screen |
| PAUSED | Tap SETTINGS | Open Settings Overlay |
| PAUSED | Close overlay (X or back) | Return to pause menu |

### Post-Alpha Overlay Additions

When deferred sections are implemented, overlay transitions are added — no state machine changes needed:

| Current State | Action | Result |
|---------------|--------|--------|
| MAIN_MENU | Tap Variant badge | Open Variant Select Overlay |
| MAIN_MENU | Tap Cosmetics icon | Open Cosmetics Overlay |
| PAUSED | Tap TIPS | Open Tips Panel |

---

## 10. Tips Integration (Post-Alpha)

> **Deferred.** The tips system requires trigger evaluation logic (tracking consecutive misses, kick counts, steer usage), rotation state, dismiss persistence, and the toast display system. This will be implemented alongside the Tips System Spec (see `backlog.md`). The design is retained here as the target.

Tips surface in two places, following the design intent in `game-design-document.md` Section 10.

### Pause Menu — Tips Panel

Accessible via a **TIPS** button in the pause menu (button not shown in alpha — see Section 4).

| Aspect | Detail |
|--------|--------|
| **Content** | Displays the current rotating tip from the starter tips list (GDD Section 10). Tips rotate so the player sees a different tip each time they open the panel. |
| **Layout** | A centered overlay panel (similar style to Settings) with the tip text prominently displayed. |
| **Dismiss** | Close via X button or Android back. Closing does NOT mark the tip as dismissed — the player can re-read tips freely. |
| **Tip cycling** | Each open shows the next tip in the rotation. The rotation wraps around. |

### Main Menu — Contextual Tip Toasts

| Aspect | Detail |
|--------|--------|
| **Trigger** | Brief toast-style popups appear on the attract screen after qualifying triggers. Example: if the player had 3+ consecutive misses in their last session, show the trajectory preview tip on return to the main menu. |
| **Display** | A small, non-blocking text banner near the bottom of the attract screen, above the icon bar. Auto-dismisses after ~4 seconds, or dismissible by tap. |
| **Dismiss tracking** | When a contextual tip is dismissed (by tap or timeout after display), its ID is added to `ProfileData.dismissedTips`. Dismissed tips are not shown again. |
| **Data source** | Tip IDs and trigger conditions come from GDD Section 10. Dismissed tip IDs are persisted in `ProfileData.dismissedTips` (see `save-and-persistence.md` Section 3). |

### Starter Tips Reference

These are the four starter tips defined in `game-design-document.md` Section 10:

| # | Tip | Contextual Trigger |
|---|-----|--------------------|
| 1 | "Enable **Trajectory Preview** in Settings to see your ball's predicted path." | After 3+ consecutive misses, or on first launch |
| 2 | "Swipe while the ball is in the air to add **spin** and curve your shot!" | After the player's first Big Bomb, or after 10 kicks with no steer input |
| 3 | "Hit targets in a row to build a **streak multiplier** — up to ×3!" | After the player's first streak of 2+ |
| 4 | "Aim a powerful flick straight up to send a **Big Bomb** down the central corridor for bonus points." | After 20 kicks with no Big Bomb attempt |

### Alpha-to-Post-Alpha Additions

When this section is implemented:
- The pause menu (Section 4) gains a **TIPS** button between SETTINGS and QUIT (expanding from 3 to 4 buttons).
- The attract screen gains contextual tip toasts near the bottom edge.

---

## 11. Out of Scope

The following features are explicitly **not part of this spec**. They are listed to prevent scope creep and document known future work.

| Feature | Notes |
|---------|-------|
| **Online leaderboards** | Requires backend infrastructure. See GDD Section 11. |
| **Friend/share functionality** | Social features are not in the alpha scope. |
| **Account/login** | No user accounts — all data is local (see `save-and-persistence.md`). |
| **In-app purchase UI** | No monetization in the alpha build. |
| **Tutorial/onboarding flow** | The Tips system (Section 10, post-alpha) will handle feature discovery. No dedicated tutorial. |
| **Accessibility options** | Font sizing, colorblind modes, and screen reader support are future work (see `backlog.md`). |
| **Challenge mode select** | Challenge mode is a future consideration (GDD Section 11). No mode selection UI needed yet. |
| **Notification/inbox UI** | No notification system exists. |

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `game-design-document.md` Section 7 | Progression, stats, and cosmetic unlock concepts surfaced by menu screens |
| `game-design-document.md` Section 10 | Tips system design — tip content, triggers, and dismissal behavior (post-alpha) |
| `state-machine.md` | Game states (MAIN_MENU, PAUSED) that host the menu and overlay layers |
| `save-and-persistence.md` Section 3 | Domain objects (`ProfileData`, `SettingsData`, `CareerStats`, `CosmeticState`, `VariantState`) displayed and modified by menu screens |
| `save-and-persistence.md` Section 6 | Save triggers for settings changes, cosmetic selections, and session end |
| `ui-hud-layout.md` | In-gameplay HUD layout — complements this spec's menu/overlay coverage |
| `ui-hud-layout.md` Section 7 | Pause icon definition (position, size, touch target) |
| `ui-hud-layout.md` Section 11 | Handedness configuration — deferred alongside Slider Side toggle |
