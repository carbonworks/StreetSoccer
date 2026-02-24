package com.streetsoccer.screens

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputAdapter
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
    private val collisionSystem = CollisionSystem(contactListener, gameStateManager, sessionAccumulator, engine)
    private val renderSystem = RenderSystem(batch)
    private val spawnSystem = SpawnSystem(gameStateManager)
    private val inputSystem = InputSystem(inputRouter, gameStateManager, world)
    private val hudSystem = HudSystem(gameStateManager, sessionAccumulator)

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

        // Catch Android back key so it doesn't exit the app
        @Suppress("DEPRECATION")
        Gdx.input.setCatchBackKey(true)

        // Register pause state listener to auto-show/hide the overlay
        gameStateManager.addListener(pauseStateListener)

        // Register all ECS systems with the engine
        engine.addSystem(physicsSystem)
        engine.addSystem(collisionSystem)
        engine.addSystem(renderSystem)
        engine.addSystem(spawnSystem)
        engine.addSystem(inputSystem)
        engine.addSystem(hudSystem)

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
        // Clear input processor when leaving this screen
        Gdx.input.inputProcessor = null
        // Unregister pause listener to prevent stale callbacks
        gameStateManager.removeListener(pauseStateListener)
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
