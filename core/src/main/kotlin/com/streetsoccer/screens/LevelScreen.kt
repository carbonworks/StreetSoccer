package com.streetsoccer.screens

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.FitViewport
import com.streetsoccer.GameBootstrapper
import com.streetsoccer.ecs.components.CatcherComponent
import com.streetsoccer.ecs.components.ColliderComponent
import com.streetsoccer.ecs.components.TransformComponent
import com.streetsoccer.ecs.components.VisualComponent
import com.streetsoccer.ecs.systems.CatcherSystem
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
import com.streetsoccer.state.GameStateListener
import com.streetsoccer.state.GameStateManager
import com.streetsoccer.ui.PauseOverlay
import ktx.app.KtxScreen

/**
 * Core gameplay screen. Manages the ECS engine, physics world, input routing,
 * HUD stage, pause overlay, and the main game loop per technical-architecture.md Section 7.
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
    private var backgroundTexture: Texture? = null

    // --- ECS Engine ---
    private val engine = Engine()

    // --- Input ---
    private val inputRouter = InputRouter(gameStateManager)
    private lateinit var inputMultiplexer: InputMultiplexer

    // --- Pause Overlay ---
    private lateinit var pauseOverlay: PauseOverlay

    // --- Fixed-timestep accumulator for Box2D world stepping ---
    private var accumulator = 0f

    // --- ECS Systems (held as fields for direct access in the game loop) ---
    private val physicsSystem = PhysicsSystem()
    private val collisionSystem = CollisionSystem(contactListener, gameStateManager, sessionAccumulator, engine, game.audioService)
    private val renderSystem = RenderSystem(batch)
    private val spawnSystem = SpawnSystem(gameStateManager)
    private val inputSystem = InputSystem(inputRouter, gameStateManager, world, game.audioService, game.assets)
    private val hudSystem = HudSystem(gameStateManager, sessionAccumulator)
    private val catcherSystem = CatcherSystem(gameStateManager, engine)

    /**
     * Listens for state transitions to automatically show/hide the pause overlay.
     * This ensures the overlay appears regardless of what triggered the pause
     * (pause icon tap, Android back button, etc.).
     */
    private val pauseStateListener = object : GameStateListener {
        override fun onStateEnter(newState: GameState) {
            if (newState is GameState.Paused && ::pauseOverlay.isInitialized) {
                pauseOverlay.show()
            }
        }

        override fun onStateExit(oldState: GameState) {
            if (oldState is GameState.Paused && ::pauseOverlay.isInitialized) {
                pauseOverlay.hide()
            }
        }
    }

    /**
     * Input processor that handles Android back button and escape key during gameplay.
     *
     * Per menu-and-navigation-flow.md Section 8:
     * - During active gameplay: back button triggers pause (same as tapping pause icon)
     * - During PAUSED (no overlay): back button acts as Resume
     */
    private val backButtonProcessor = object : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                if (gameStateManager.isPaused) {
                    // Paused state: back acts as Resume
                    handleResume()
                    return true
                }
                // Gameplay state: back triggers pause
                if (gameStateManager.isInGameplay) {
                    gameStateManager.pause()
                    // Overlay will be shown automatically by pauseStateListener
                    return true
                }
            }
            return false
        }
    }

    override fun show() {
        Gdx.app.log("LevelScreen", "show")

        // Load background texture from AssetManager
        if (game.assets.isLoaded("background.jpg")) {
            backgroundTexture = game.assets.get("background.jpg", Texture::class.java)
        }

        // Catch Android back key so it doesn't exit the app
        @Suppress("DEPRECATION")
        Gdx.input.setCatchBackKey(true)

        // Register pause state listener to auto-show/hide the overlay
        gameStateManager.addListener(pauseStateListener)

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
        engine.addSystem(catcherSystem)

        // Create the catcher NPC entity in the intersection
        createCatcherEntity()

        // Create pause overlay with resume/quit callbacks
        pauseOverlay = PauseOverlay(
            onResume = { handleResume() },
            onQuit = { handleQuit() }
        )

        // Set up input multiplexer with priority order:
        // 1. Back button handler (highest priority — catches BACK/ESCAPE before anyone else)
        // 2. Pause overlay stage (when visible, consumes all taps via dim background)
        // 3. HUD stage (for pause icon taps)
        // 4. InputRouter (for gameplay gestures — flick, slider, steer)
        inputMultiplexer = InputMultiplexer()
        inputMultiplexer.addProcessor(backButtonProcessor)
        inputMultiplexer.addProcessor(pauseOverlay.stage)
        inputMultiplexer.addProcessor(hudSystem.getStage())
        inputMultiplexer.addProcessor(inputRouter)
        Gdx.input.inputProcessor = inputMultiplexer

        // Start with overlay hidden
        pauseOverlay.hide()

        // Transition game state to Ready for gameplay
        gameStateManager.transitionTo(GameState.Ready)
    }

    override fun render(delta: Float) {
        // Clear the screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Update projection matrix so batch renders in game-world coordinates
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        // Draw level background (reset batch color to avoid tint bleed from RenderSystem)
        backgroundTexture?.let { bg ->
            batch.begin()
            batch.setColor(1f, 1f, 1f, 1f)
            batch.draw(bg, 0f, 0f, 1920f, 1080f)
            batch.end()
        }

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

        // 7. Render pause overlay on top of everything (if visible)
        pauseOverlay.render(delta)
    }

    // --- Pause overlay callbacks ---

    /**
     * Handle resume from pause: hide overlay, restore pre-pause game state.
     */
    private fun handleResume() {
        pauseOverlay.hide()
        gameStateManager.resume()
    }

    /**
     * Handle quit from pause menu: save session, return to attract screen.
     *
     * Per state-machine.md Section 2I and menu-and-navigation-flow.md Section 4:
     * - Trigger session end: merge session counters into CareerStats, write profile.json
     * - Transition to MAIN_MENU
     * - No confirmation dialog (arcade games don't ask "are you sure?")
     */
    private fun handleQuit() {
        pauseOverlay.hide()
        // TODO: merge session stats into CareerStats and persist via SaveService
        // game.saveService.mergeSessionAndSave(sessionAccumulator)
        gameStateManager.quit()
        game.setScreen<AttractScreen>()
    }

    /**
     * Create the ball catcher NPC entity at the center of the intersection.
     *
     * The catcher stands in the middle of the cross-street, positioned to
     * intercept balls headed toward the deep neighborhood corridor. Position
     * is derived from the catcher spawn point in suburban-crossroads.json.
     */
    private fun createCatcherEntity() {
        val entity = engine.createEntity()

        val transform = engine.createComponent(TransformComponent::class.java).apply {
            // Center of intersection — between the player and the deep corridor.
            // Coordinates from suburban-crossroads.json catcher_spawn_point.
            x = 960f
            y = 280f
            height = 0f
            screenScale = maxOf(0.05f, (540f - 280f) / 540f)
        }

        val catcher = engine.createComponent(CatcherComponent::class.java).apply {
            catchRadius = CatcherComponent.DEFAULT_CATCH_RADIUS
        }

        val visual = engine.createComponent(VisualComponent::class.java).apply {
            // Placeholder — texture will be assigned when assets are loaded.
            // Render on layer 1 (cross-street level) so the catcher appears
            // at the correct depth in the scene.
            renderLayer = 1
        }

        entity.add(transform)
        entity.add(catcher)
        entity.add(visual)
        engine.addEntity(entity)
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
        // Unregister pause listener to prevent stale callbacks
        gameStateManager.removeListener(pauseStateListener)
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
        if (::pauseOverlay.isInitialized) {
            pauseOverlay.resize(width, height)
        }
    }

    override fun dispose() {
        Gdx.app.log("LevelScreen", "dispose")
        renderSystem.dispose()
        hudSystem.dispose()
        if (::pauseOverlay.isInitialized) {
            pauseOverlay.dispose()
        }
        batch.dispose()
        world.dispose()
    }
}
