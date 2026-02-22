This State Machine Specification (SMS) is designed to be the "brain" of your LibGDX project. It defines exactly how the game flows from one moment to the next, preventing logic conflicts (like being able to kick while the game is paused) and providing the AI with a clear blueprint for the `GameStateManager.kt`.

---

# 🤖 Game State Machine (GSM) Specification

**Project:** Street Soccer

**Architecture:** Finite State Machine (FSM) implemented via LibKTX `KtxScreen` and a custom `State` enum.

---

## 1. High-Level State Diagram

This diagram outlines the primary flow of the user experience.

---

## 2. Detailed State Definitions

### A. `BOOT` (Initialization)

* **Purpose:** Initial app startup.
* **Entry Actions:** Initialize LibGDX engine; Set up `KtxAsync` coroutine context.
* **Exit Actions:** None.
* **Transitions:** Move to `LOADING` automatically once the engine is ready.

### B. `LOADING` (Asset Acquisition)

* **Purpose:** Loading the `background.jpg`, the `level_suburban_crossroads.json`, and SVG assets via AmanithSVG.
* **Entry Actions:** Show progress bar; Trigger `LevelLoader.kt` to parse JSON and build Box2D static bodies.
* **Exit Actions:** Hide progress bar; Dispose of temporary loading assets.
* **Transitions:** Move to `MAIN_MENU` when `AssetManager.update()` returns `true`.

### C. `MAIN_MENU` (UI State)

* **Purpose:** Await player engagement.
* **Entry Actions:** Render background image; Display "Play" button; Reset global score.
* **Exit Actions:** Hide Menu UI.
* **Transitions:** Move to `READY` when "Play" is clicked.

### D. `READY` (Idle Gameplay)

* **Purpose:** The stationary player is waiting to kick.
* **Logic:**
* Active: Spawn lanes (drones, pedestrians) are active and moving.
* Inactive: Physics world for the ball is dormant.


* **Transitions:** Move to `AIMING` on `touchDown` event.

### E. `AIMING` (Input Handling)

* **Purpose:** Player is dragging to determine power and horizontal aim. The angle slider (always visible on a side rail) sets the launch angle independently via multi-touch.
* **Entry Actions:** Calculate `v_initial` based on drag distance; read angle slider value for launch angle.
* **Updates:** * Update `TrajectoryRenderer.kt` to draw the dotted arc (incorporating angle slider position).
* Check for "Big Bomb" threshold (if power > 90% and angle slider is high).


* **Transitions:**
* To `BALL_IN_FLIGHT` on `touchUp`.
* Back to `READY` if drag is cancelled (e.g., drag back to origin).



### F. `BALL_IN_FLIGHT` (Physics Active)

* **Purpose:** The ball is traveling into the 2.5D environment.
* **Logic:**
* Update Box2D shadow position.
* Calculate  (height) and  based on the **Z-Depth Formulas** in the Technical Spec.
* Apply Z-Ordering: Ball moves behind/in front of buildings based on shadow Y.

> **Mid-flight steering:** This state now accepts touch input. Swipe gestures detected during `BALL_IN_FLIGHT` apply a spin force to the ball, curving its trajectory. Each swipe adds cumulative spin; spin decays over time. The state does **not** transition on steer input — the ball remains in flight while spin is applied.

* **Transitions:**
* To `SCORING` if collision with `target_sensor` occurs.
* To `IMPACT_MISSED` if collision with `static_collider` or ball goes off-screen.



### G. `SCORING` (Success Feedback)

* **Purpose:** Process a hit on a townhome window or lane target.
* **Entry Actions:** Play "Glass Break" or "Impact" sound; Trigger score animation (e.g., "+250" floating text).
* **Exit Actions:** Increment score; Check for level completion.
* **Transitions:** Move back to `READY` after animation completes (~1 second).

---

## 3. Transition Logic Table (For AI Coding)

| Current State | Trigger | Target State | Logic Constraint |
| --- | --- | --- | --- |
| `READY` | `Input.touchDown()` | `AIMING` | Only if `shotCount > 0` |
| `AIMING` | `Input.touchUp()` | `BALL_IN_FLIGHT` | Apply `Impulse` to Box2D body |
| `BALL_IN_FLIGHT` | `Input.swipe()` | `BALL_IN_FLIGHT` | Apply spin force to ball; remain in flight |
| `BALL_IN_FLIGHT` | `ContactListener.beginContact()` | `SCORING` | Only if `fixture.isSensor == true` |
| `BALL_IN_FLIGHT` | `Ball.isOutOfBounds()` | `READY` | Reset ball to player origin |
| `ANY` | `EscapeKey` | `PAUSED` | Pause Box2D `world.step()` |

---

## 4. AI Prompting Strategy for the State Machine

When you want Claude or Gemini to implement a specific state, use the following prompt format:

> **"According to the State Machine Specification, implement the transition from `BALL_IN_FLIGHT` to `SCORING`. Ensure the `ContactListener` checks the `target_sensor` ID from the `level_suburban_crossroads.json` and plays the associated sound before returning to `READY`."**

---

## 5. Non-Technical Concerns for this Specification

* **Interruptions:** If a phone call comes in (Android Lifecycle), the state must move to `PAUSED` automatically.
* **Feedback:** In `AIMING` state, the visual feedback (dotted line) must feel snappy. Any lag here will ruin the "Duck Hunt" precision feel.

**Would you like me to write the Kotlin `GameStateManager` class that handles these state transitions using a clean 'Sealed Class' pattern?**
