This architectural map is designed to maximize the strengths of the **LibGDX + LibKTX** ecosystem while leveraging the **Gemini/Claude AI workflow** for rapid development.

In 2026, this stack is the gold standard for "AI-Native" game dev because of its high predictability and the massive training data available for these specific libraries.

---

### 1. High-Level Project Structure (Multi-Module)

Your project should be structured as a **Gradle Multi-module project** to separate platform-specific code from your core game logic.

* **`:core`**: Contains 95% of your code. All game logic, physics, and AI instructions.
* **`:android`**: Android-specific setup (Manifest, launcher).
* **`:desktop`**: Essential for fast testing and "AI-assisted debugging" without waiting for mobile deploys.
* **`:assets`**: Raw SVGs, sounds, and the `AGENTS.md` file.

---

### 2. The Tech Stack (Engine Room)

| Category | Tool / Library | Why? |
| --- | --- | --- |
| **Engine** | **LibGDX 1.14.x** | The most "AI-readable" framework; extremely stable. |
| **Kotlin Bridge** | **LibKTX** | Provides Coroutines for async asset loading and DSLs for UI/Physics. |
| **Physics** | **ktx-box2d** | Handles ball trajectory, collisions with "Target" houses, and bounce physics. |
| **UI & Objects** | **ktx-scene2d** | A high-level scene graph that makes layering (Z-sorting) easy. |
| **Vector Support** | **AmanithSVG** | Essential for rendering your "Flat Architectural" SVGs at any resolution. |
| **Async Logic** | **ktx-async** | Uses Kotlin Coroutines to keep the UI smooth during "Big Bomb" calculations. |

---

### 3. Application Architecture: The "State-Screen" Pattern

We will use a **Screen-based architecture**, which is the cleanest way for AI to navigate your code. Each screen (Menu, GameLevel, Results) is its own isolated Kotlin file.

* **`GameBootstrapper.kt`**: Extends `KtxGame`. Handles the global state and initial AI context.
* **`LevelScreen.kt`**: The heart of the game. Manages the 2.5D view and physics world.
* **`AssetService.kt`**: Uses `ktx-assets` to load your houses as SVGs. Claude can easily write new "loading" logic here.

---

### 4. The 2.5D Coordinate System

To handle the "Big Bombs" and "Travel" we discussed, we'll use a **Depth-Augmented 2D System**:

* **X-Axis:** Horizontal movement (the scrolling street).
* **Y-Axis:** Height (the arc of the football kick).
* **Z-Index (Virtual):** We use a simple integer to define which layer an object belongs to.
* **Z = 0–10:** Foreground (Sidewalk, Player, Ball).
* **Z = 11–30:** Midground (Front yards, Target houses).
* **Z = 31+:** Background (Alleyways, Distant trees).



---

### 5. AI Workflow Implementation Map

This is how you "plug in" Gemini and Claude into the architecture:

1. **Gemini (The Indexer):** Stays in **Android Studio**. It reads your `build.gradle.kts` and `AGENTS.md` to ensure your project stays in sync. It is your primary tool for **refactoring** and **file-searching**.
2. **Claude (The Generator):** Stays in the **Web Browser**. Use it to generate the complex math for the `TrajectorySystem.kt` or to "draw" new houses in SVG code.
3. **The "Sync" Point:** You copy the logic from Claude into the file structure Gemini suggested.

---

### 6. The "Big Bomb" Component Architecture

To make the football kicks feel powerful, we use a **Component-Based Logic** (using `ktx-ashley` if you want a full ECS, or simple Kotlin Delegates):

* **`TransformComponent`**: Position (X, Y) and scale (simulating Z depth).
* **`PhysicsComponent`**: The Box2D body for the ball.
* **`VisualComponent`**: The SVG reference for the ball/house.
* **`TrajectorySystem`**: The "brain" that calculates where the ball will land based on kick power.

---

### Your Next Step

To get this architecture live, would you like me to generate the **`GameBootstrapper.kt`** and a basic **`LevelScreen.kt`** using the LibKTX DSL so you can see how the 2.5D layering looks in code?
