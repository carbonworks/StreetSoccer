package com.streetsoccer.state

/**
 * Listener interface for observing game state transitions.
 *
 * Systems register themselves as listeners to react to state changes
 * without creating direct coupling between GameStateManager and each system.
 */
interface GameStateListener {
    /** Called after exiting [oldState], before entering the new state. */
    fun onStateExit(oldState: GameState) {}

    /** Called after entering [newState], after exit actions have completed. */
    fun onStateEnter(newState: GameState) {}
}

/**
 * Drives the game's finite state machine using [GameState] sealed class hierarchy.
 *
 * Responsibilities:
 * - Manage state transitions with entry/exit actions
 * - Tick internal timers for SCORING (1.0s) and IMPACT_MISSED (0.75s)
 * - Auto-transition timed states to READY when their timer expires
 * - Handle pause/resume with correct prior-state tracking
 * - Notify registered [GameStateListener] instances on every transition
 *
 * @see GameState
 * @see GameStateListener
 */
class GameStateManager {

    companion object {
        /** Duration in seconds that the SCORING feedback state lasts before auto-transitioning to READY. */
        const val SCORING_DURATION = 1.0f

        /** Duration in seconds that the IMPACT_MISSED feedback state lasts before auto-transitioning to READY. */
        const val IMPACT_MISSED_DURATION = 0.75f
    }

    // ---- State ----

    var currentState: GameState = GameState.Boot
        private set

    /** True when the current state is [GameState.Paused]. */
    val isPaused: Boolean
        get() = currentState is GameState.Paused

    // ---- Timers ----

    /**
     * Remaining time (seconds) for timed states (SCORING, IMPACT_MISSED).
     * Only meaningful when [currentState] is one of those two states.
     * When paused during a timed state, the timer value is preserved and resumes on unpause.
     */
    private var stateTimer: Float = 0f

    // ---- Listeners ----

    private val listeners = mutableListOf<GameStateListener>()

    /** Register a listener to be notified of state transitions. */
    fun addListener(listener: GameStateListener) {
        if (listener !in listeners) {
            listeners.add(listener)
        }
    }

    /** Remove a previously registered listener. */
    fun removeListener(listener: GameStateListener) {
        listeners.remove(listener)
    }

    // ---- Transitions ----

    /**
     * Transition from the current state to [newState].
     *
     * This method:
     * 1. Executes exit actions for the old state
     * 2. Notifies listeners of the exit
     * 3. Updates [currentState]
     * 4. Executes entry actions for the new state
     * 5. Notifies listeners of the entry
     *
     * Transition validation: not all transitions are valid (e.g., transitioning
     * from BOOT directly to SCORING makes no sense). However, validation is
     * intentionally lenient here because the calling code (InputRouter, collision
     * handlers, screen logic) is responsible for only requesting valid transitions.
     * This keeps GameStateManager a simple, predictable driver.
     */
    fun transitionTo(newState: GameState) {
        val oldState = currentState

        // Avoid no-op transitions (same state) unless it is a meaningful re-entry.
        // Paused->Paused is never valid; timed states shouldn't re-enter themselves.
        if (oldState == newState) return

        // Detect resume-from-pause: we are leaving PAUSED and returning to a
        // timed state. In that case, the timer should NOT be reset — it should
        // continue from the preserved remaining time.
        val isResumeFromPause = oldState is GameState.Paused

        // --- Exit actions for old state ---
        performExitActions(oldState)
        notifyExit(oldState)

        // --- Switch ---
        currentState = newState

        // --- Entry actions for new state ---
        performEntryActions(newState, isResumeFromPause)
        notifyEnter(newState)
    }

    // ---- Convenience: Pause / Resume ----

    /**
     * Pause the game from the current state.
     *
     * Wraps the current state inside [GameState.Paused] so it can be restored later.
     * If already paused, this is a no-op.
     *
     * Allowed from any gameplay state: READY, AIMING, BALL_IN_FLIGHT, SCORING, IMPACT_MISSED.
     */
    fun pause() {
        if (isPaused) return

        // Only gameplay states may be paused.
        val pausableStates = setOf(
            GameState.Ready::class,
            GameState.Aiming::class,
            GameState.BallInFlight::class,
            GameState.Scoring::class,
            GameState.ImpactMissed::class
        )
        if (currentState::class !in pausableStates) return

        transitionTo(GameState.Paused(previousState = currentState))
    }

    /**
     * Resume gameplay from a paused state.
     *
     * Restores [currentState] to the state that was active before pausing.
     * If not currently paused, this is a no-op.
     *
     * Note: The timer for SCORING/IMPACT_MISSED is preserved across pause/resume
     * because [stateTimer] is not reset on pause entry — it simply stops ticking
     * while paused (see [update]).
     */
    fun resume() {
        val paused = currentState as? GameState.Paused ?: return
        transitionTo(paused.previousState)
    }

    /**
     * Quit to main menu from the paused state.
     *
     * If not currently paused, this is a no-op.
     * Listeners will receive exit(Paused) then enter(MainMenu), allowing the
     * save system to trigger session-end persistence on the MainMenu entry.
     */
    fun quit() {
        if (!isPaused) return
        transitionTo(GameState.MainMenu)
    }

    // ---- Update (called every frame from the game loop) ----

    /**
     * Tick internal timers and handle auto-transitions.
     *
     * Should be called every frame with the frame's delta time.
     * While paused, this method does nothing — timers are frozen.
     *
     * When a timed state's timer expires, this automatically transitions to READY.
     */
    fun update(deltaTime: Float) {
        // Do nothing while paused — all timers frozen.
        if (isPaused) return

        when (currentState) {
            is GameState.Scoring, is GameState.ImpactMissed -> {
                stateTimer -= deltaTime
                if (stateTimer <= 0f) {
                    transitionTo(GameState.Ready)
                }
            }
            else -> {
                // No timer logic for other states.
            }
        }
    }

    // ---- Internal: entry/exit action implementations ----

    /**
     * Perform internal bookkeeping when exiting [state].
     *
     * Actual gameplay side effects (removing ball entity, stopping particles, etc.)
     * are handled by listeners — not here. This method only manages state that
     * GameStateManager owns internally (timers).
     */
    private fun performExitActions(@Suppress("UNUSED_PARAMETER") state: GameState) {
        // Timer is intentionally NOT reset on exit from timed states.
        //
        // Two exit scenarios for SCORING/IMPACT_MISSED:
        // 1. Timer expired -> READY: stateTimer is already <= 0, no reset needed.
        // 2. Pausing -> PAUSED: stateTimer holds the remaining duration that
        //    must be preserved for resume.
        //
        // In both cases, leaving stateTimer untouched is correct. The timer is
        // freshly initialized on entry to SCORING/IMPACT_MISSED (when not
        // resuming from pause), so there is no stale-value risk.
        //
        // Actual gameplay side effects (removing ball, stopping particles, etc.)
        // are handled by listeners, not here.
    }

    /**
     * Perform internal bookkeeping when entering [state].
     *
     * Actual gameplay side effects (spawning ball, applying impulse, spawning
     * particles, updating score) are handled by listeners — not here.
     */
    /**
     * @param isResumeFromPause true when this entry is triggered by resuming
     *   from PAUSED. When true, timed states (SCORING, IMPACT_MISSED) skip
     *   resetting their timer so the remaining duration is preserved.
     */
    private fun performEntryActions(state: GameState, isResumeFromPause: Boolean = false) {
        when (state) {
            is GameState.Scoring -> {
                if (!isResumeFromPause) {
                    stateTimer = SCORING_DURATION
                }
                // On resume, stateTimer already holds the remaining duration.
            }
            is GameState.ImpactMissed -> {
                if (!isResumeFromPause) {
                    stateTimer = IMPACT_MISSED_DURATION
                }
                // On resume, stateTimer already holds the remaining duration.
            }
            is GameState.Paused -> {
                // Timer is intentionally NOT reset here. If we paused during
                // SCORING or IMPACT_MISSED, the remaining stateTimer value is
                // preserved and will resume ticking once we leave PAUSED.
            }
            else -> {
                // No internal entry actions for other states.
            }
        }
    }

    // ---- Internal: listener notification ----

    private fun notifyExit(oldState: GameState) {
        // Iterate over a snapshot to allow listeners to add/remove during callbacks.
        listeners.toList().forEach { it.onStateExit(oldState) }
    }

    private fun notifyEnter(newState: GameState) {
        listeners.toList().forEach { it.onStateEnter(newState) }
    }

    // ---- Query helpers ----

    /**
     * Returns the remaining time on the current timed state's timer, or 0 if
     * the current state is not a timed state.
     *
     * During PAUSED, if the pre-pause state was timed, this returns the frozen
     * remaining time.
     */
    fun remainingStateTime(): Float {
        return when (currentState) {
            is GameState.Scoring, is GameState.ImpactMissed -> stateTimer
            is GameState.Paused -> {
                val previous = (currentState as GameState.Paused).previousState
                if (previous is GameState.Scoring || previous is GameState.ImpactMissed) {
                    stateTimer
                } else {
                    0f
                }
            }
            else -> 0f
        }
    }

    /**
     * Whether the game is in an active gameplay state (not in menus or boot sequence).
     * Useful for systems that should only run during gameplay.
     */
    val isInGameplay: Boolean
        get() = when (currentState) {
            is GameState.Ready, is GameState.Aiming, is GameState.BallInFlight,
            is GameState.Scoring, is GameState.ImpactMissed -> true
            is GameState.Paused -> true // Paused is still "in gameplay" (just frozen)
            else -> false
        }
}
