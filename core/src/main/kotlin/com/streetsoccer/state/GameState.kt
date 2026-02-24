package com.streetsoccer.state

sealed class GameState {
    object Boot : GameState()
    object Loading : GameState()
    object MainMenu : GameState()
    object Ready : GameState()
    object Aiming : GameState()
    object BallInFlight : GameState()
    object Scoring : GameState()
    object ImpactMissed : GameState()
    object Caught : GameState()
    data class Paused(val previousState: GameState) : GameState()
}
