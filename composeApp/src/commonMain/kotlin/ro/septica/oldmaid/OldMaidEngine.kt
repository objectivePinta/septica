package ro.septica.oldmaid

import kotlin.random.Random
import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.cards.standardDeck
import ro.septica.game.Player
import ro.septica.game.Suit

data class PairRemoval(val hand: List<StandardCard>, val discarded: List<StandardCard>)

fun removePairs(cards: List<StandardCard>): PairRemoval {
    val hand = mutableListOf<StandardCard>()
    val discarded = mutableListOf<StandardCard>()
    cards.forEach { card ->
        val match = hand.indexOfFirst { it.rank == card.rank }
        if (match >= 0) {
            discarded.add(hand.removeAt(match))
            discarded.add(card)
        } else hand.add(card)
    }
    return PairRemoval(hand.toList(), discarded.toList())
}

data class OldMaidState(
    val human: List<StandardCard>,
    val computer: List<StandardCard>,
    val discarded: List<StandardCard>,
    val turn: Player = Player.HUMAN,
    val winner: Player? = null,
    val notice: String = "Alege o carte ascunsă de la calculator.",
) {
    val finished: Boolean get() = winner != null
}

class OldMaidEngine(private val random: Random = Random.Default) {
    fun newGame(): OldMaidState = deal(
        standardDeck().filterNot { it.rank == StandardRank.KING && it.suit == Suit.CLUBS }
            .shuffled(random),
    )

    fun deal(deck: List<StandardCard>): OldMaidState {
        require(deck.size == 51 && deck.toSet().size == 51)
        require(deck.none { it.rank == StandardRank.KING && it.suit == Suit.CLUBS })
        val h = removePairs(deck.filterIndexed { index, _ -> index % 2 == 0 })
        val c = removePairs(deck.filterIndexed { index, _ -> index % 2 != 0 })
        return complete(OldMaidState(
            h.hand.shuffled(random), c.hand.shuffled(random), h.discarded + c.discarded,
        ))
    }

    fun draw(state: OldMaidState, player: Player, opponentIndex: Int): OldMaidState {
        require(!state.finished && state.turn == player) { "Not your turn" }
        val source = if (player == Player.HUMAN) state.computer else state.human
        val target = if (player == Player.HUMAN) state.human else state.computer
        require(opponentIndex in source.indices) { "Invalid hidden card position" }
        val card = source[opponentIndex]
        val remaining = source.filterIndexed { index, _ -> index != opponentIndex }.shuffled(random)
        val paired = removePairs(target + card)
        val hand = paired.hand.shuffled(random)
        return complete(state.copy(
            human = if (player == Player.HUMAN) hand else remaining,
            computer = if (player == Player.COMPUTER) hand else remaining,
            discarded = state.discarded + paired.discarded,
            turn = player.opponent,
            notice = if (player == Player.HUMAN)
                "Ai tras ${card.accessibilityLabel}. ${if (paired.discarded.isNotEmpty()) "Pereche eliminată!" else "Fără pereche."}"
            else "Calculatorul a tras o carte. ${if (paired.discarded.isNotEmpty()) "Pereche eliminată!" else "Fără pereche."}",
        ))
    }

    fun computerMove(state: OldMaidState): OldMaidState {
        require(!state.finished && state.turn == Player.COMPUTER)
        return draw(state, Player.COMPUTER, random.nextInt(state.human.size))
    }

    private fun complete(state: OldMaidState): OldMaidState {
        val winner = when {
            state.human.isEmpty() -> Player.HUMAN
            state.computer.isEmpty() -> Player.COMPUTER
            else -> return state
        }
        return state.copy(winner = winner, notice = if (winner == Player.HUMAN)
            "Ai câștigat! Calculatorul a rămas cu popa." else "Calculatorul a câștigat. Ai rămas cu popa.")
    }
}
