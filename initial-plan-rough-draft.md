Yes, I remember. We discussed a **"Two-Model Workflow"** designed to save you from "debugging loops." The goal is to use each AI for what it does best: **Gemini 1.5 Pro** as your **Senior Architect** (because of its massive context window) and **Claude 3.5 Sonnet** as your **Lead Coder** (because of its precision and "Artifacts" preview).

Here are the detailed instructions for that setup:

---

## 1. Gemini AI Studio: The "Project Manager"

Use **Google AI Studio** (Gemini 1.5 Pro) to hold the "big picture." Because it can remember up to 2 million tokens, you can dump your entire codebase, all your layout photos, and your full game design document into one chat.

* **The "Code Dump" Strategy:** Every time you start a new session, upload all your existing Kotlin/SVG files. Ask Gemini: *"Review my current game state. I want to add [Feature X]. Give me a high-level architectural plan and a briefing for a junior coder."*
* **Asset Management:** Use Gemini to keep track of your "Shopping List." If you generate a new house in Recraft, tell Gemini: *"I just added house_v3.svg. Update the level generator logic to include this in the random spawn pool."*
* **Detailed Logic:** Use Gemini for complex math, like calculating the **projectile trajectory** for your "Big Bombs" or the math for the 2.5D parallax scrolling.

## 2. Claude 3.5 Sonnet: The "Precision Coder"

Use **Claude** to write the actual, bug-free code. Claude is famously better at "one-shotting" code that actually runs without errors, especially for UI and game loops.

* **The "Handoff" Method:** Take the architectural plan Gemini gave you and paste it into Claude.
* *Prompt:* "Gemini Architect suggested this plan for the 2.5D camera follow. Here are my current `Player.kt` and `Camera.kt` files. Please implement the update."


* **Artifacts for UI/SVG:** Use Claude’s **Artifacts** feature to preview your SVG sprites. If you need a "Stop Sign" sprite, ask Claude to write the SVG. You can see it instantly in the side panel, tweak the colors, and then copy the code into your project.
* **Debugging:** If you get a Kotlin error, give it to Claude first. It is generally faster at spotting small syntax errors or LibGDX/KorGE lifecycle issues.

---

## 3. The Combined Workflow Step-by-Step

| Step | Action | Tool |
| --- | --- | --- |
| **1. Brainstorm** | "I want the ball to fly behind the houses if it's a 'Big Bomb'." | **Gemini** |
| **2. Architect** | "How should I handle the Z-index layering for this?" | **Gemini** |
| **3. Code** | "Write the Kotlin class for the Z-index sorting." | **Claude** |
| **4. Visuals** | "Create an SVG for the 'Shadow' that scales with height." | **Claude** |
| **5. Review** | Upload the new code back to the main thread. | **Gemini** |

---

## 4. Pro-Tip: The "claude.md" File

As your project grows, create a file in your project called `claude.md`.

1. Ask **Gemini** to update this file after every major milestone.
2. It should contain a summary of the current classes, the coordinate system you're using, and any "gotchas" in your code.
3. Whenever you start a new chat with **Claude**, paste the contents of `claude.md` first. This gives Claude "instant context" so it doesn't suggest code that breaks your existing systems.

**Would you like me to write the first version of your `claude.md` based on the 2.5D "Big Bomb" layout we just finalized?**