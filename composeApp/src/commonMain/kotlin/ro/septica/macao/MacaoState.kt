package ro.septica.macao

import ro.septica.game.Player
import ro.septica.game.Suit

data class MacaoState(
    val humanHand: List<MacaoCard>,
    val computerHand: List<MacaoCard>,
    val deck: List<MacaoCard>,
    val discardPile: List<MacaoCard>,
    val turn: Player,
    val activeSuit: Suit,
    val pendingDraw: Int = 0,
    val winner: Player? = null,
    val notice: String,
)
