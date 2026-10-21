package ro.septica.tabinet

import kotlin.random.Random
import ro.septica.game.Player
import ro.septica.game.Suit

class TabinetEngine(
    private val random: Random = Random.Default,
) {
    fun newGame(): TabinetState {
        val cards = Suit.entries.flatMap { suit ->
            TabinetRank.entries.map { rank -> TabinetCard(suit, rank) }
        }.shuffled(random)
        return TabinetState(
            humanHand = cards.drop(4).take(6),
            computerHand = cards.drop(10).take(6),
            deck = cards.drop(16),
            table = cards.take(4),
            turn = Player.HUMAN,
            notice = "Joc nou. Este rândul tău.",
        )
    }

    fun canCapture(card: TabinetCard, cards: List<TabinetCard>): Boolean {
        if (cards.isEmpty() || cards.map { it.id }.toSet().size != cards.size) return false
        return CapturePartition(card.rank.values, cards).solve()
    }

    fun playCard(
        state: TabinetState,
        player: Player,
        cardId: String,
        capturedIds: List<String> = emptyList(),
    ): TabinetState {
        requireTurn(state, player)
        val card = requireNotNull(state.handFor(player).singleOrNull { it.id == cardId }) {
            "The selected card is not in the player's hand."
        }
        require(capturedIds.toSet().size == capturedIds.size) { "Capture IDs must be unique." }
        val selected = capturedIds.map { id ->
            requireNotNull(state.table.singleOrNull { it.id == id }) {
                "The selected capture is not on the table."
            }
        }
        require(selected.isEmpty() || canCapture(card, selected)) { "Invalid capture groups." }
        val hand = state.handFor(player).filterNot { it.id == cardId }
        val humanHand = if (player == Player.HUMAN) hand else state.humanHand
        val computerHand = if (player == Player.COMPUTER) hand else state.computerHand
        val finalMove = state.deck.isEmpty() && humanHand.isEmpty() && computerHand.isEmpty()
        val capture = selected.isNotEmpty()
        val sweep = capture && selected.size == state.table.size && !finalMove
        val captured = if (capture) listOf(card) + selected else emptyList()
        val selectedIds = capturedIds.toSet()
        val actor = if (player == Player.HUMAN) "Tu" else "Adversarul"
        val playedLabel = "${card.rank.symbol}${card.suit.symbol}"
        val action = if (capture) {
            "$actor: $playedLabel a capturat ${selected.size} cărți."
        } else {
            "$actor: $playedLabel a rămas pe masă."
        }
        val sweepNotice = if (sweep) " Tablă! +1 punct." else ""
        var next = state.copy(
            humanHand = humanHand,
            computerHand = computerHand,
            table = if (capture) state.table.filterNot { it.id in selectedIds } else state.table + card,
            turn = player.opponent,
            humanCaptured = state.humanCaptured + if (player == Player.HUMAN) captured else emptyList(),
            computerCaptured = state.computerCaptured + if (player == Player.COMPUTER) captured else emptyList(),
            humanSweeps = state.humanSweeps + if (sweep && player == Player.HUMAN) 1 else 0,
            computerSweeps = state.computerSweeps + if (sweep && player == Player.COMPUTER) 1 else 0,
            lastCapturedBy = if (capture) player else state.lastCapturedBy,
            notice = "$action$sweepNotice ${player.opponent.turnLabel}",
        )
        if (finalMove) return finish(next)
        if (humanHand.isEmpty() && computerHand.isEmpty()) {
            require(state.deck.size % 2 == 0) { "The stock must support equal hands." }
            val count = minOf(6, state.deck.size / 2)
            next = next.copy(
                humanHand = state.deck.take(count),
                computerHand = state.deck.drop(count).take(count),
                deck = state.deck.drop(count * 2),
                turn = Player.HUMAN,
                notice = "$action$sweepNotice Cărți noi. Este rândul tău.",
            )
        } else {
            require(next.handFor(player.opponent).isNotEmpty()) {
                "The hands must support alternating turns."
            }
        }
        return next
    }

    fun suggestCapture(state: TabinetState, player: Player, cardId: String): List<String> {
        requireTurn(state, player)
        val card = requireNotNull(state.handFor(player).singleOrNull { it.id == cardId }) {
            "The selected card is not in the player's hand."
        }
        var remaining = state.table.toList()
        val captured = mutableListOf<String>()
        while (remaining.isNotEmpty()) {
            val group = bestGroup(card.rank.values, remaining) ?: break
            val ids = group.map { it.id }.toSet()
            captured.addAll(group.map { it.id })
            remaining = remaining.filterNot { it.id in ids }
        }
        return captured.toList()
    }

    fun chooseComputerMove(state: TabinetState): TabinetMove {
        requireTurn(state, Player.COMPUTER)
        require(state.computerHand.isNotEmpty()) { "The computer's hand is empty." }
        val moves = state.computerHand.map { card ->
            TabinetMove(card.id, suggestCapture(state, Player.COMPUTER, card.id))
        }
        // Captured piles and the table reveal the last move without inspecting hidden hands or stock.
        val finalMove = state.computerHand.size == 1 &&
            state.humanCaptured.size + state.computerCaptured.size + state.table.size == 51
        return moves.filter { it.capturedIds.isNotEmpty() }.maxByOrNull { move ->
            val card = state.computerHand.single { it.id == move.cardId }
            val selected = state.table.filter { it.id in move.capturedIds }
            val sweep = if (selected.size == state.table.size && !finalMove) 1 else 0
            (card.points + selected.sumOf { it.points } + sweep) * 53 + selected.size + 1
        } ?: TabinetMove(
            state.computerHand.minBy { it.rank.values.first() }.id,
            emptyList(),
        )
    }

    private fun bestGroup(targets: List<Int>, cards: List<TabinetCard>): List<TabinetCard>? {
        var groups = arrayOfNulls<List<TabinetCard>>(targets.max() + 1)
        groups[0] = emptyList()
        for (card in cards) {
            val next = groups.copyOf()
            for (sum in groups.indices) {
                val previous = groups[sum] ?: continue
                for (value in card.rank.values) {
                    if (sum + value >= groups.size) continue
                    val candidate = previous + card
                    val existing = next[sum + value]
                    if (existing == null || candidate.groupWeight > existing.groupWeight) {
                        next[sum + value] = candidate
                    }
                }
            }
            groups = next
        }
        return targets.mapNotNull { groups[it] }.maxByOrNull { it.groupWeight }
    }

    private val List<TabinetCard>.groupWeight: Int
        get() = sumOf { it.points } * 53 + size

    private fun finish(state: TabinetState): TabinetState {
        val collector = state.lastCapturedBy ?: Player.COMPUTER
        val finished = state.copy(
            table = emptyList(),
            humanCaptured = state.humanCaptured + if (collector == Player.HUMAN) state.table else emptyList(),
            computerCaptured = state.computerCaptured + if (collector == Player.COMPUTER) state.table else emptyList(),
            turn = null,
            finished = true,
        )
        return finished.copy(
            notice = when (finished.gameWinner) {
                Player.HUMAN -> "Ai câștigat!"
                Player.COMPUTER -> "Calculatorul a câștigat!"
                null -> "Egalitate!"
            },
        )
    }

    private fun requireTurn(state: TabinetState, player: Player) {
        require(!state.finished) { "The game has already finished." }
        require(state.turn == player) { "It is not this player's turn." }
    }

    private fun TabinetState.handFor(player: Player): List<TabinetCard> =
        if (player == Player.HUMAN) humanHand else computerHand

    private val Player.turnLabel: String
        get() = if (this == Player.HUMAN) "Este rândul tău." else "Este rândul calculatorului."
}

/**
 * Search rank counts, not card subsets. Every group is anchored on the largest
 * remaining rank; only complements up to 14 are enumerated. Three bits per rank
 * encode all possible counts (0..4), independently of suits and ace assignments.
 */
private class CapturePartition(
    private val targets: List<Int>,
    cards: List<TabinetCard>,
) {
    private val ranks = TabinetRank.entries.sortedByDescending { it.values.first() }
    private val counts = IntArray(ranks.size) { index -> cards.count { it.rank == ranks[index] } }
    private val failed = mutableSetOf<Long>()

    fun solve(): Boolean {
        val anchor = counts.indexOfFirst { it > 0 }
        if (anchor < 0) return true
        if (ranks[anchor].values.first() > targets.max()) return false
        val key = counts.foldIndexed(0L) { index, value, count ->
            value or (count.toLong() shl (index * 3))
        }
        if (key in failed) return false
        if (targets.size == 1) {
            val minimum = counts.indices.sumOf { counts[it] * ranks[it].values.first() }
            val aces = counts[ranks.lastIndex]
            if ((0..aces).none { (minimum + 10 * it) % targets.single() == 0 }) {
                failed.add(key)
                return false
            }
        }
        counts[anchor]--
        for (target in targets) {
            for (value in ranks[anchor].values) {
                if (value <= target && complement(anchor, target - value)) {
                    counts[anchor]++
                    return true
                }
            }
        }
        counts[anchor]++
        failed.add(key)
        return false
    }

    private fun complement(index: Int, remaining: Int): Boolean {
        if (remaining == 0) return solve()
        if (index == counts.size) return false
        val rank = ranks[index]
        val available = counts[index]
        for (take in minOf(available, remaining / rank.values.first()) downTo 0) {
            val highAceCounts = if (rank == TabinetRank.ACE) 0..take else 0..0
            for (highAces in highAceCounts) {
                val sum = take * rank.values.first() + 10 * highAces
                if (sum > remaining) continue
                counts[index] -= take
                val solved = complement(index + 1, remaining - sum)
                counts[index] += take
                if (solved) return true
            }
        }
        return false
    }
}
