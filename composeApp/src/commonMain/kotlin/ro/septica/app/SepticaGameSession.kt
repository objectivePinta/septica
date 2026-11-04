package ro.septica.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ro.septica.game.GameState
import ro.septica.game.SepticaEngine

internal class SepticaGameSession {
    val engine = SepticaEngine()
    var gameNumber by mutableIntStateOf(0)
    var state: GameState by mutableStateOf(engine.newGame())
    var selectedCardId by mutableStateOf<String?>(null)
    var showRules by mutableStateOf(false)
    var showSetup by mutableStateOf(true)
    var playerCount by mutableIntStateOf(2)
}
