package com.streetsoccer.screens

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.FitViewport
import com.streetsoccer.GameBootstrapper
import com.streetsoccer.ecs.components.ColliderComponent
import com.streetsoccer.ecs.components.TransformComponent
import com.streetsoccer.ecs.systems.CollisionSystem
import com.streetsoccer.ecs.systems.HudSystem
import com.streetsoccer.ecs.systems.InputSystem
import com.streetsoccer.ecs.systems.PhysicsSystem
import com.streetsoccer.ecs.systems.RenderSystem
import com.streetsoccer.ecs.systems.SpawnSystem
import com.streetsoccer.input.InputRouter
import com.streetsoccer.physics.PhysicsContactListener
import com.streetsoccer.physics.TuningConstants
import com.streetsoccer.services.SessionAccumulator
import com.streetsoccer.state.GameState
import com.streetsoccer.state.GameStateManager
import ktx.app.KtxScreen

/**
 * Core gameplay screen. Manages the ECS engine, physics world, input routing,
 * HUD stage, and the main game loop per technical-architecture.md Section 7.
 */
class LevelScreen(private val game: GameBootstrapper) : KtxScreen {

    // --- State Management ---
    private val gameStateManager = GameStateManager()

    // --- Session Accumulator (tracks score, streak, per-type hits) ---
    private val sessionAccumulator = SessionAccumulator()

    // --- Box2D World (zero gravity — gravity handled in game-space by PhysicsSystem) ---
    private val contactListener = PhysicsContactListener()
    private val world = World(Vector2(0f, 0f), true).apply {
        setContactListener(contactListener)
    }

    // --- Rendering ---
    private val batch = SpriteBatch()
    private val viewport = FitViewport(1920f, 1080f)

    // --- ECS Engine ---
    private val engine = Engine()

    // --- Input ---
    private val inputRouter = InputRouter(gameStateManager)

    // --- Fixed-timestep accumulator for Box2D world stepping ---
    private var accumulator = 0f

    // --- ECS Systems (held as fields for direct access in the game loop) ---
    private val physicsSystem = PhysicsSystem()
    private val collisionSystem = CollisionSystem(contactListener, gameStateManager, sessionAccumulator, engine)
    private val renderSystem = RenderSystem(batch)
    private val spawnSystem = SpawnSystem(gameStateManager)
    private val inputSystem = InputSystem(inputRouter, gameStateManager, world)
    private val hudSystem = HudSystem(gameStateManager, sessionAccumulator)

    override fun show() {
        Gdx.app.log("LevelScreen", "show")

        // Begin a new session: reset accumulator counters to zero
        // (per save-and-persistence.md Section 5 — session start)
        sessionAccumulator.reset()

        // Register all ECS systems with the engine
        engine.addSystem(physicsSystem)
        engine.addSystem(collisionSystem)
        engine.addSystem(renderSystem)
        engine.addSystem(spawnSystem)
        engine.addSystem(inputSystem)
        engine.addSystem(hudSystem)

        // Set up input multiplexer: HUD stage first (for pause icon taps),
        // then InputRouter (for gameplay gestures)
        val inputMultiplexer = InputMultiplexer()
        inputMultiplexer.addProcessor(hudSystem.getStage())
        inputMultiplexer.addProcessor(inputRouter)
        Gdx.input.inputProcessor = inputMultiplexer

        // Transition game state to Ready for gameplay
        gameStateManager.transitionTo(GameState.Ready)
    }

    override fun render(delta: Float) {
        // Clear the screen
        Gdx.gl.glClearColor(0.3f, 0.3f, 0.3f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // --- Game Loop (per technical-architecture.md Section 7) ---

        // 1. Poll input — InputRouter receives events via LibGDX InputProcessor callbacks.
        //    No explicit polling needed; events are dispatched by the framework.

        // 2. Accumulate time for Box2D fixed-step coordination
        accumulator += delta

        // 3. Fixed-step physics + Box2D + collision (only during BALL_IN_FLIGHT)
        //    PhysicsSystem has its own internal accumulator for physics updates.
        //    Here we coordinate the Box2D world step and collision processing
        //    on the same fixed timestep.
        if (gameStateManager.currentState is GameState.BallInFlight) {
            while (accumulator >= TuningConstants.FIXED_TIMESTEP) {
                // PhysicsSystem runs via engine.update() below.
                // Step Box2D for collision detection.
                world.step(TuningConstants.FIXED_TIMESTEP, 6, 2)
                // Process contacts queued by PhysicsContactListener.
                collisionSystem.update(TuningConstants.FIXED_TIMESTEP)
                accumulator -= TuningConstants.FIXED_TIMESTEP
            }
        } else {
            // Drain accumulator when not in flight to prevent spiral-of-death
            accumulator = 0f
        }

        // Run all ECS systems (PhysicsSystem handles its own fixed-timestep internally,
        // RenderSystem draws, SpawnSystem manages lanes, InputSystem delegates to InputRouter,
        // HudSystem renders the HUD via its own stage)
        engine.update(delta)

        // 4. Update state machine (timers for SCORING/IMPACT_MISSED transitions)
        gameStateManager.update(delta)

        // 5. Sync Box2D positions from game-space (game-space is authoritative)
        syncBox2DPositions()

        // 6. Wire HUD inputs from InputRouter
        hudSystem.sliderValue = inputRouter.sliderValue
        hudSystem.steerSwipeCount = inputRouter.steerSwipeCount
    }

    /**
     * Sync Box2D body positions from game-space transform data.
     * Game-space is authoritative; Box2D is used only for collision detection.
     */
    private fun syncBox2DPositions() {
        val family = Family.all(TransformComponent::class.java, ColliderComponent::class.java).get()
        val entities = engine.getEntitiesFor(family)
        for (i in 0 until entities.size()) {
            val entity = entities[i]
            val transform = entity.getComponent(TransformComponent::class.java)
            val collider = entity.getComponent(ColliderComponent::class.java)
            val body = collider.body ?: continue
            // Convert game-space pixels to Box2D meters
            body.setTransform(
                transform.x / TuningConstants.PPM,
                transform.y / TuningConstants.PPM,
                body.angle
            )
        }
    }

    override fun hide() {
        Gdx.app.log("LevelScreen", "hide")

        // Session end (normal): merge session stats into career and persist.
        // Per save-and-persistence.md Section 5, session end folds all
        // accumulated stats into CareerStats and writes profile.json.
        mergeSessionAndSave()

        // Clear input processor when leaving this screen
        Gdx.input.inputProcessor = null
    }

    /**
     * Called when the app is backgrounded (Android onPause).
     * Treated as a session end per save-and-persistence.md Section 5:
     * "Same merge-and-write as normal session end." The safety net
     * ensures no progress is lost if the OS kills the app after onPause.
     *
     * The accumulator is reset after merging, so if the app resumes and
     * the player continues playing, new kicks accumulate into a fresh
     * mini-session. Career stats already reflect everything from before
     * the pause via the merge.
     */
    override fun pause() {
        Gdx.app.log("LevelScreen", "pause (backgrounded)")
        mergeSessionAndSave()
    }

    /**
     * Merge current session stats into the career profile and persist to disk.
     * After merging, the accumulator is reset to prevent double-counting if
     * called again (e.g., pause followed by hide, or multiple pause events).
     *
     * Only stats from completed kicks are included — any in-progress kick
     * during BALL_IN_FLIGHT is discarded per save-and-persistence.md Section 6.
     */
    private fun mergeSessionAndSave() {
        try {
            game.profile = sessionAccumulator.mergeInto(game.profile)
            game.saveService.saveProfile(game.profile)
            Gdx.app.log(
                "LevelScreen",
                "Session merged: sessionScore=${sessionAccumulator.sessionScore}, " +
                    "careerTotal=${game.profile.career.totalScore}"
            )
            // Reset after merge so subsequent calls are no-ops (merge zeros).
            sessionAccumulator.reset()
        } catch (e: Exception) {
            Gdx.app.log("LevelScreen", "Failed to merge/save session: ${e.message}")
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        hudSystem.resize(width, height)
    }

    override fun dispose() {
        Gdx.app.log("LevelScreen", "dispose")
        renderSystem.dispose()
        hudSystem.dispose()
        batch.dispose()
        world.dispose()
    }
}
