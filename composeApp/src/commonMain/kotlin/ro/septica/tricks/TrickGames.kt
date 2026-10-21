package ro.septica.tricks

import kotlin.math.abs
import kotlin.random.Random
import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.cards.standardDeck
import ro.septica.game.Suit

enum class TrickMode { WHIST, RENTZ }
enum class TrickPhase { BIDDING, PLAYING, REVEAL, ROUND_OVER, FINISHED }
enum class Contract(val title: String, val rules: String) {
    NO_TRICKS("Fără levate", "−10 puncte pentru fiecare levată."),
    NO_HEARTS("Fără cupe", "−10 puncte pentru fiecare cupă luată."),
    NO_QUEENS("Fără dame", "−25 puncte pentru fiecare damă luată."),
    NO_KING_HEARTS("Fără regele de cupă", "−100 puncte pentru regele de cupă luat."),
    NO_LAST_TWO("Fără ultimele două", "−50 puncte pentru fiecare dintre ultimele două levate."),
    TOTALS("Totale", "Se adună toate cele cinci penalizări precedente."),
    WHIST("Whist pozitiv", "+25 puncte pentru fiecare levată, fără atu."),
    RENTZ("Rentz", "Pornești fiecare culoare cu J; continui în jos 10–7 sau în sus Q–A. Pas numai fără carte legală. Locurile primesc 100, 75, 50, 25 puncte."),
}

data class PlayedCard(val seat: Int, val card: StandardCard)
data class TrickState(
    val mode: TrickMode,
    val seed: Int,
    val round: Int,
    val dealer: Int,
    val hands: List<List<StandardCard>>,
    val stock: List<StandardCard>,
    val turn: Int,
    val phase: TrickPhase,
    val scores: List<Int> = List(4) { 0 },
    val roundScores: List<Int> = List(4) { 0 },
    val bids: List<Int?> = List(4) { null },
    val tricks: List<Int> = List(4) { 0 },
    val table: List<PlayedCard> = emptyList(),
    val captured: List<StandardCard> = emptyList(),
    val lastTrick: List<PlayedCard> = emptyList(),
    val lastWinner: Int? = null,
    val lanes: Set<StandardCard> = emptySet(),
    val finishOrder: List<Int> = emptyList(),
    val lastPass: Int? = null,
) {
    val handSize: Int get() = if (mode == TrickMode.WHIST) whistSchedule[round] else 8
    val contract: Contract? get() = if (mode == TrickMode.RENTZ) Contract.entries[round] else null
    val trumpCard: StandardCard? get() = if (mode == TrickMode.WHIST) stock.firstOrNull() else null
    val trump: Suit? get() = trumpCard?.suit
    val roundCount: Int get() = if (mode == TrickMode.WHIST) whistSchedule.size else Contract.entries.size
    val winners: List<Int> get() = scores.indices.filter { scores[it] == scores.maxOrNull() }
}

val whistSchedule = List(4) { 1 } + (2..7).toList() + List(4) { 8 } +
    (7 downTo 2).toList() + List(4) { 1 }

fun trickDeck(): List<StandardCard> = standardDeck().filter { it.rank.value >= 7 }

private fun <T> List<T>.replace(index: Int, value: T): List<T> =
    mapIndexed { i, old -> if (i == index) value else old }

fun newTrickGame(mode: TrickMode, seed: Int = Random.nextInt()): TrickState =
    deal(mode, seed, 0, List(4) { 0 })

private fun deal(mode: TrickMode, seed: Int, round: Int, scores: List<Int>): TrickState {
    val dealer = round % 4
    val size = if (mode == TrickMode.WHIST) whistSchedule[round] else 8
    val deck = trickDeck().shuffled(Random(seed + round))
    val hands = List(4) { seat ->
        deck.take(size * 4).filterIndexed { i, _ -> (dealer + 1 + i) % 4 == seat }
            .sortedWith(compareBy({ it.suit.ordinal }, { it.rank.value }))
    }
    return TrickState(mode, seed, round, dealer, hands, deck.drop(size * 4),
        (dealer + 1) % 4, if (mode == TrickMode.WHIST) TrickPhase.BIDDING else TrickPhase.PLAYING,
        scores = scores)
}

fun legalBids(state: TrickState): List<Int> {
    if (state.phase != TrickPhase.BIDDING) return emptyList()
    val forbidden = if (state.bids.count { it != null } == 3) {
        state.handSize - state.bids.filterNotNull().sum()
    } else -1
    return (0..state.handSize).filter { it != forbidden }
}

fun bid(state: TrickState, value: Int): TrickState {
    require(value in legalBids(state))
    val bids = state.bids.replace(state.turn, value)
    return state.copy(bids = bids, turn = (state.turn + 1) % 4,
        phase = if (bids.all { it != null }) TrickPhase.PLAYING else TrickPhase.BIDDING)
}

fun legalTrickCards(hand: List<StandardCard>, table: List<PlayedCard>, trump: Suit?): List<StandardCard> {
    if (table.isEmpty()) return hand
    val following = hand.filter { it.suit == table.first().card.suit }
    if (following.isNotEmpty()) return following
    val trumps = hand.filter { it.suit == trump }
    return trumps.ifEmpty { hand }
}

fun trickWinner(table: List<PlayedCard>, trump: Suit?): Int {
    require(table.size == 4)
    val suit = if (table.any { it.card.suit == trump }) trump else table.first().card.suit
    return table.filter { it.card.suit == suit }.maxBy { it.card.rank.value }.seat
}

fun legalPlacement(card: StandardCard, lanes: Set<StandardCard>): Boolean {
    val suitCards = lanes.filter { it.suit == card.suit }
    if (suitCards.isEmpty()) return card.rank == StandardRank.JACK
    return card !in lanes && (card.rank.value == suitCards.minOf { it.rank.value } - 1 ||
        card.rank.value == suitCards.maxOf { it.rank.value } + 1)
}

fun legalCards(state: TrickState): List<StandardCard> =
    if (state.phase != TrickPhase.PLAYING) emptyList()
    else if (state.contract == Contract.RENTZ) state.hands[state.turn].filter { legalPlacement(it, state.lanes) }
    else legalTrickCards(state.hands[state.turn], state.table, state.trump)

fun whistScore(bid: Int, tricks: Int): Int = if (bid == tricks) 5 + bid else -abs(tricks - bid)

fun contractScore(contract: Contract, cards: List<StandardCard>, trickIndex: Int): Int {
    val components = listOf(
        -10,
        -10 * cards.count { it.suit == Suit.HEARTS },
        -25 * cards.count { it.rank == StandardRank.QUEEN },
        if (cards.any { it.suit == Suit.HEARTS && it.rank == StandardRank.KING }) -100 else 0,
        if (trickIndex in 6..7) -50 else 0,
    )
    return when (contract) {
        Contract.TOTALS -> components.sum()
        Contract.WHIST -> 25
        Contract.RENTZ -> 0
        else -> components[contract.ordinal]
    }
}

fun playCard(state: TrickState, card: StandardCard): TrickState {
    require(card in legalCards(state))
    val hands = state.hands.replace(state.turn, state.hands[state.turn] - card)
    if (state.contract == Contract.RENTZ) {
        var order = state.finishOrder
        var points = state.roundScores
        if (hands[state.turn].isEmpty()) {
            points = points.replace(state.turn, 100 - order.size * 25)
            order = order + state.turn
        }
        var next = state.copy(hands = hands, lanes = state.lanes + card,
            finishOrder = order, roundScores = points, lastPass = null)
        if (order.size == 3) {
            val last = (0..3).first { it !in order }
            next = next.copy(
                lanes = next.lanes + next.hands[last],
                hands = next.hands.replace(last, emptyList()),
                finishOrder = order + last,
                roundScores = points.replace(last, 25),
            )
            return finishRound(next)
        }
        return next.copy(turn = nextActive(next, state.turn))
    }
    val table = state.table + PlayedCard(state.turn, card)
    if (table.size < 4) return state.copy(hands = hands, table = table, turn = (state.turn + 1) % 4)
    val winner = trickWinner(table, state.trump)
    val points = state.contract?.let { contractScore(it, table.map { p -> p.card }, state.tricks.sum()) } ?: 0
    return state.copy(hands = hands, table = emptyList(), lastTrick = table, lastWinner = winner,
        captured = state.captured + table.map { it.card }, turn = winner,
        tricks = state.tricks.replace(winner, state.tricks[winner] + 1),
        roundScores = state.roundScores.replace(winner, state.roundScores[winner] + points),
        phase = TrickPhase.REVEAL)
}

private fun nextActive(state: TrickState, seat: Int): Int =
    (1..4).map { (seat + it) % 4 }.first { state.hands[it].isNotEmpty() }

fun pass(state: TrickState): TrickState {
    require(state.phase == TrickPhase.PLAYING && state.contract == Contract.RENTZ && legalCards(state).isEmpty())
    require(state.hands[state.turn].isNotEmpty())
    return state.copy(turn = nextActive(state, state.turn), lastPass = state.turn)
}

fun continueTrick(state: TrickState): TrickState {
    require(state.phase == TrickPhase.REVEAL)
    return if (state.hands.all { it.isEmpty() }) finishRound(state)
    else state.copy(phase = TrickPhase.PLAYING)
}

private fun finishRound(state: TrickState): TrickState {
    val points = if (state.mode == TrickMode.WHIST) {
        (0..3).map { whistScore(state.bids[it]!!, state.tricks[it]) }
    } else state.roundScores
    return state.copy(roundScores = points, scores = (0..3).map { state.scores[it] + points[it] },
        phase = if (state.round + 1 == state.roundCount) TrickPhase.FINISHED else TrickPhase.ROUND_OVER)
}

fun nextRound(state: TrickState): TrickState {
    require(state.phase == TrickPhase.ROUND_OVER)
    return deal(state.mode, state.seed, state.round + 1, state.scores)
}

fun botBid(state: TrickState): Int {
    val estimate = state.hands[state.turn].count {
        it.rank == StandardRank.ACE || (it.suit == state.trump && it.rank.value >= 11)
    }
    return legalBids(state).minBy { abs(it - estimate) }
}

fun botStep(state: TrickState): TrickState = when (state.phase) {
    TrickPhase.BIDDING -> bid(state, botBid(state))
    TrickPhase.PLAYING -> {
        val legal = legalCards(state)
        if (legal.isEmpty()) pass(state)
        else {
            val targetMet = state.mode == TrickMode.WHIST && state.tricks[state.turn] >= state.bids[state.turn]!!
            val card = if (state.mode == TrickMode.WHIST && !targetMet || state.contract == Contract.WHIST) {
                legal.maxBy { it.rank.value + if (it.suit == state.trump) 20 else 0 }
            } else legal.minBy { it.rank.value }
            playCard(state, card)
        }
    }
    else -> throw IllegalArgumentException("Bot actions require bidding or playing phase")
}
