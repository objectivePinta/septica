package ro.septica.blackjack

import kotlin.random.Random
import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.cards.standardDeck

fun handValue(cards: List<StandardCard>): Int {
    var total = cards.sumOf { if (it.rank == StandardRank.ACE) 11 else minOf(10, it.rank.value) }
    var aces = cards.count { it.rank == StandardRank.ACE }
    while (total > 21 && aces > 0) { total -= 10; aces-- }
    return total
}

enum class BlackjackPhase { PLAYER, DEALER, FINISHED }
enum class BlackjackOutcome { WIN, LOSS, PUSH, NATURAL }

data class BlackjackState(
    val human: List<StandardCard>,
    val dealer: List<StandardCard>,
    val deck: List<StandardCard>,
    val bankroll: Int,
    val phase: BlackjackPhase = BlackjackPhase.PLAYER,
    val outcome: BlackjackOutcome? = null,
    val notice: String = "Mai ceri o carte sau te oprești?",
) {
    val bankrupt: Boolean get() = phase == BlackjackPhase.FINISHED && bankroll < 10
}

class BlackjackEngine(private val random: Random = Random.Default) {
    fun newGame(): BlackjackState = deal(100)

    fun deal(bankroll: Int, deck: List<StandardCard> = standardDeck().shuffled(random)): BlackjackState {
        require(bankroll >= 10)
        require(deck.size == 52 && deck.toSet().size == 52)
        val state = BlackjackState(listOf(deck[0], deck[2]), listOf(deck[1], deck[3]), deck.drop(4), bankroll - 10)
        val humanNatural = handValue(state.human) == 21
        val dealerNatural = handValue(state.dealer) == 21
        return when {
            humanNatural && dealerNatural -> settle(state, BlackjackOutcome.PUSH)
            humanNatural -> settle(state, BlackjackOutcome.NATURAL)
            dealerNatural -> settle(state, BlackjackOutcome.LOSS)
            else -> state
        }
    }

    fun nextRound(state: BlackjackState): BlackjackState {
        require(state.phase == BlackjackPhase.FINISHED && !state.bankrupt)
        return deal(state.bankroll)
    }

    fun hit(state: BlackjackState): BlackjackState {
        require(state.phase == BlackjackPhase.PLAYER)
        val next = state.copy(human = state.human + state.deck.first(), deck = state.deck.drop(1))
        return when {
            handValue(next.human) > 21 -> settle(next, BlackjackOutcome.LOSS)
            handValue(next.human) == 21 -> stand(next)
            else -> next
        }
    }

    fun stand(state: BlackjackState): BlackjackState {
        require(state.phase == BlackjackPhase.PLAYER)
        return state.copy(phase = BlackjackPhase.DEALER, notice = "Dealerul întoarce cartea ascunsă și joacă.")
    }

    fun dealerStep(state: BlackjackState): BlackjackState {
        require(state.phase == BlackjackPhase.DEALER)
        if (handValue(state.dealer) >= 17) return compare(state)
        val next = state.copy(dealer = state.dealer + state.deck.first(), deck = state.deck.drop(1))
        return if (handValue(next.dealer) >= 17) compare(next) else next
    }

    private fun compare(state: BlackjackState): BlackjackState {
        val h = handValue(state.human)
        val d = handValue(state.dealer)
        return settle(state, when {
            d > 21 || h > d -> BlackjackOutcome.WIN
            h == d -> BlackjackOutcome.PUSH
            else -> BlackjackOutcome.LOSS
        })
    }

    private fun settle(state: BlackjackState, outcome: BlackjackOutcome): BlackjackState {
        require(state.phase != BlackjackPhase.FINISHED)
        val payout = when (outcome) {
            BlackjackOutcome.WIN -> 20
            BlackjackOutcome.NATURAL -> 25
            BlackjackOutcome.PUSH -> 10
            BlackjackOutcome.LOSS -> 0
        }
        return state.copy(
            bankroll = state.bankroll + payout, phase = BlackjackPhase.FINISHED, outcome = outcome,
            notice = when (outcome) {
                BlackjackOutcome.WIN -> "Ai câștigat! +10 puncte net."
                BlackjackOutcome.NATURAL -> "Blackjack natural! +15 puncte net."
                BlackjackOutcome.PUSH -> "Egalitate. Miza a fost restituită."
                BlackjackOutcome.LOSS -> "Ai pierdut miza de 10 puncte."
            },
        )
    }
}
