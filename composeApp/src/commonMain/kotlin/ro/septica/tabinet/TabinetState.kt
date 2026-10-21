package ro.septica.tabinet

import ro.septica.game.Player

data class TabinetState(
    val humanHand: List<TabinetCard>,
    val computerHand: List<TabinetCard>,
    val deck: List<TabinetCard>,
    val table: List<TabinetCard>,
    val turn: Player?,
    val humanCaptured: List<TabinetCard> = emptyList(),
    val computerCaptured: List<TabinetCard> = emptyList(),
    val humanSweeps: Int = 0,
    val computerSweeps: Int = 0,
    val lastCapturedBy: Player? = null,
    val finished: Boolean = false,
    val notice: String = "Este rândul tău.",
) {
    val humanCardPoints: Int get() = humanCaptured.sumOf { it.points }
    val computerCardPoints: Int get() = computerCaptured.sumOf { it.points }
    val humanMajorityBonus: Int
        get() = if (finished && humanCaptured.size > computerCaptured.size) 3 else 0
    val computerMajorityBonus: Int
        get() = if (finished && computerCaptured.size > humanCaptured.size) 3 else 0
    val humanScore: Int get() = humanCardPoints + humanSweeps + humanMajorityBonus
    val computerScore: Int get() = computerCardPoints + computerSweeps + computerMajorityBonus
    val gameWinner: Player?
        get() = when {
            !finished || humanScore == computerScore -> null
            humanScore > computerScore -> Player.HUMAN
            else -> Player.COMPUTER
        }
}

data class TabinetMove(
    val cardId: String,
    val capturedIds: List<String>,
)
