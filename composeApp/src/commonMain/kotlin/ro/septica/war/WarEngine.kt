package ro.septica.war

import kotlin.random.Random
import ro.septica.cards.StandardCard
import ro.septica.cards.standardDeck
import ro.septica.game.Player

data class WarState(
    val human: List<StandardCard>,
    val computer: List<StandardCard>,
    val pot: List<StandardCard> = emptyList(),
    val humanFace: StandardCard? = null,
    val computerFace: StandardCard? = null,
    val war: Boolean = false,
    val battles: Int = 0,
    val finished: Boolean = false,
    val winner: Player? = null,
    val notice: String = "Întoarce câte o carte pentru prima bătălie.",
)

class WarEngine(private val random: Random = Random.Default) {
    fun newGame(): WarState {
        val deck = standardDeck().shuffled(random)
        return WarState(deck.take(26), deck.drop(26))
    }

    fun flip(state: WarState): WarState {
        require(!state.finished) { "Game has finished" }
        val human = state.human.toMutableList()
        val computer = state.computer.toMutableList()
        val pot = state.pot.toMutableList()
        if (human.isEmpty() || computer.isEmpty()) {
            val winner = when {
                human.isEmpty() && computer.isEmpty() -> null
                human.isEmpty() -> Player.COMPUTER
                else -> Player.HUMAN
            }
            return state.copy(
                human = if (winner == Player.HUMAN) human + pot else human.toList(),
                computer = if (winner == Player.COMPUTER) computer + pot else computer.toList(),
                pot = if (winner == null) pot.toList() else emptyList(),
                finished = true, winner = winner, notice = result(winner),
            )
        }
        if (state.war) {
            repeat(minOf(3, human.size - 1)) { pot.add(human.removeAt(0)) }
            repeat(minOf(3, computer.size - 1)) { pot.add(computer.removeAt(0)) }
        }
        val h = human.removeAt(0)
        val c = computer.removeAt(0)
        pot.add(h)
        pot.add(c)
        val comparison = h.rank.value.compareTo(c.rank.value)
        val battles = state.battles + if (state.war) 0 else 1
        if (comparison == 0) {
            val tied = state.copy(
                human = human.toList(), computer = computer.toList(), pot = pot.toList(),
                humanFace = h, computerFace = c, war = true, battles = battles,
                notice = "Egalitate! Război: continuă cu până la 3 cărți ascunse și una la vedere.",
            )
            return if (human.isEmpty() || computer.isEmpty()) flip(tied) else tied
        }
        val winner = if (comparison > 0) Player.HUMAN else Player.COMPUTER
        if (winner == Player.HUMAN) human.addAll(pot) else computer.addAll(pot)
        val finished = human.isEmpty() || computer.isEmpty() || battles >= 1000
        val finalWinner = if (human.isEmpty() || computer.isEmpty()) winner else null
        return WarState(
            human.toList(), computer.toList(), humanFace = h, computerFace = c,
            battles = battles, finished = finished, winner = if (finished) finalWinner else null,
            notice = if (finished) result(finalWinner) else
                "${if (winner == Player.HUMAN) "Tu ai" else "Calculatorul a"} câștigat ${pot.size} cărți. Următoarea bătălie?",
        )
    }

    private fun result(winner: Player?): String = when (winner) {
        Player.HUMAN -> "Ai câștigat toate cărțile!"
        Player.COMPUTER -> "Calculatorul a câștigat toate cărțile."
        null -> "Remiză: cărți epuizate simultan sau limita de 1.000 de bătălii."
    }
}
