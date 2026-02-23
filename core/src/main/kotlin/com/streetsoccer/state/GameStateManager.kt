package com.streetsoccer.state

class GameStateManager {
    var currentState: GameState = GameState.Boot
        private set

    fun transitionTo(newState: GameState) {
        // Handle exit actions
        when (currentState) {
            is GameState.Scoring, is GameState.ImpactMissed -> { /* clean up timers */ }
            else -> {}
        }

        currentState = newState

        // Handle entry actions
        when (newState) {
            is GameState.Ready -> { /* unlock steer budget, spawn ball */ }
            is GameState.BallInFlight -> { /* apply flick velocity */ }
            is GameState.Scoring -> { /* start 1s timer, spawn popup */ }
            is GameState.ImpactMissed -> { /* start 0.75s timer, impact burst */ }
            else -> {}
        }
    }

    fun update(deltaTime: Float) {
        // Handle timers for SCORING, IMPACT_MISSED
    }
}
