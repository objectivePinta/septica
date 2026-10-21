package ro.septica.sixtysix

import kotlin.random.Random
import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.cards.standardDeck
import ro.septica.game.Player
import ro.septica.game.Suit

val StandardCard.sixtySixPoints: Int
    get() = when (rank) {
        StandardRank.ACE -> 11
        StandardRank.TEN -> 10
        StandardRank.KING -> 4
        StandardRank.QUEEN -> 3
        StandardRank.JACK -> 2
        else -> 0
    }

val StandardCard.sixtySixStrength: Int
    get() = when (rank) {
        StandardRank.ACE -> 5
        StandardRank.TEN -> 4
        StandardRank.KING -> 3
        StandardRank.QUEEN -> 2
        StandardRank.JACK -> 1
        else -> 0
    }

fun sixtySixDeck(): List<StandardCard> = standardDeck().filter {
    it.rank in setOf(
        StandardRank.NINE, StandardRank.JACK, StandardRank.QUEEN,
        StandardRank.KING, StandardRank.TEN, StandardRank.ACE,
    )
}

data class SixtySixPlayer(
    val hand: List<StandardCard> = emptyList(),
    val captured: List<StandardCard> = emptyList(),
    val points: Int = 0,
    val pendingMarriagePoints: Int = 0,
    val marriages: Set<Suit> = emptySet(),
    val tricks: Int = 0,
    val gamePoints: Int = 0,
)

data class SixtySixPlay(val player: Player, val card: StandardCard)
data class SixtySixTrick(val winner: Player, val cards: List<StandardCard>, val points: Int)
data class StockClosure(val player: Player, val opponentPoints: Int)
data class SixtySixResult(val winner: Player, val gamePoints: Int, val reason: String)

data class SixtySixState(
    val human: SixtySixPlayer,
    val computer: SixtySixPlayer,
    val stock: List<StandardCard>,
    val trump: Suit,
    val leader: Player,
    val firstLeader: Player,
    val table: List<SixtySixPlay> = emptyList(),
    val closure: StockClosure? = null,
    val lastTrick: SixtySixTrick? = null,
    val result: SixtySixResult? = null,
    val roundNumber: Int = 1,
) {
    fun player(player: Player): SixtySixPlayer = if (player == Player.HUMAN) human else computer
    val stockOpen: Boolean get() = closure == null && stock.isNotEmpty()
    val faceUpTrump: StandardCard? get() = if (stockOpen) stock.last() else null
    val turn: Player?
        get() = if (result != null || table.size == 2) null
        else if (table.isEmpty()) leader else leader.opponent
    val matchWinner: Player?
        get() = when {
            human.gamePoints >= 7 -> Player.HUMAN
            computer.gamePoints >= 7 -> Player.COMPUTER
            else -> null
        }

    internal fun withPlayer(player: Player, value: SixtySixPlayer): SixtySixState =
        if (player == Player.HUMAN) copy(human = value) else copy(computer = value)
}

object SixtySixEngine {
    fun newMatch(random: Random = Random.Default): SixtySixState =
        deal(sixtySixDeck().shuffled(random))

    fun deal(deck: List<StandardCard>, firstLeader: Player = Player.HUMAN): SixtySixState {
        require(deck.size == 24 && deck.toSet() == sixtySixDeck().toSet())
        return SixtySixState(
            human = SixtySixPlayer(hand = deck.take(6).toList()),
            computer = SixtySixPlayer(hand = deck.drop(6).take(6)),
            stock = deck.drop(12),
            trump = deck.last().suit,
            leader = firstLeader,
            firstLeader = firstLeader,
        )
    }

    fun nextRound(state: SixtySixState, random: Random = Random.Default): SixtySixState {
        require(state.result != null && state.matchWinner == null)
        val next = deal(sixtySixDeck().shuffled(random), state.firstLeader.opponent)
        return next.copy(
            human = next.human.copy(gamePoints = state.human.gamePoints),
            computer = next.computer.copy(gamePoints = state.computer.gamePoints),
            roundNumber = state.roundNumber + 1,
        )
    }

    fun beats(card: StandardCard, lead: StandardCard, trump: Suit): Boolean =
        if (card.suit == lead.suit) card.sixtySixStrength > lead.sixtySixStrength
        else card.suit == trump

    fun legalCards(state: SixtySixState, player: Player): List<StandardCard> {
        if (state.turn != player) return emptyList()
        val hand = state.player(player).hand
        if (state.stockOpen || state.table.isEmpty()) return hand
        val lead = state.table.single().card
        val sameSuit = hand.filter { it.suit == lead.suit }
        if (sameSuit.isNotEmpty()) {
            val higher = sameSuit.filter { beats(it, lead, state.trump) }
            return higher.ifEmpty { sameSuit }
        }
        return hand.filter { it.suit == state.trump }.ifEmpty { hand }
    }

    fun canExchange(state: SixtySixState, player: Player): Boolean =
        state.turn == player && state.table.isEmpty() && state.stockOpen &&
            state.stock.size >= 2 &&
            state.player(player).hand.any { it.suit == state.trump && it.rank == StandardRank.NINE }

    fun exchangeTrump(state: SixtySixState, player: Player): SixtySixState {
        require(canExchange(state, player))
        val owner = state.player(player)
        val nine = owner.hand.single { it.suit == state.trump && it.rank == StandardRank.NINE }
        val exposed = state.stock.last()
        return state.withPlayer(player, owner.copy(hand = owner.hand.map {
            if (it == nine) exposed else it
        })).copy(stock = state.stock.dropLast(1) + nine)
    }

    fun canClose(state: SixtySixState, player: Player): Boolean =
        state.turn == player && state.table.isEmpty() && state.stockOpen

    fun closeStock(state: SixtySixState, player: Player): SixtySixState {
        require(canClose(state, player))
        return state.copy(closure = StockClosure(player, state.player(player.opponent).points))
    }

    fun canAnnounce(state: SixtySixState, player: Player, card: StandardCard): Boolean {
        val owner = state.player(player)
        if (state.turn != player || state.table.isNotEmpty() || card !in owner.hand ||
            card.suit in owner.marriages ||
            card.rank !in setOf(StandardRank.KING, StandardRank.QUEEN)
        ) return false
        return owner.hand.any {
            it.suit == card.suit &&
                it.rank == (if (card.rank == StandardRank.KING) StandardRank.QUEEN else StandardRank.KING)
        }
    }

    fun play(
        state: SixtySixState,
        player: Player,
        cardId: String,
        announce: Boolean = false,
    ): SixtySixState {
        val card = legalCards(state, player).find { it.id == cardId }
        require(card != null)
        require(!announce || canAnnounce(state, player, card))
        var owner = state.player(player)
        owner = owner.copy(hand = owner.hand.filterNot { it.id == cardId })
        if (announce) {
            val bonus = if (card.suit == state.trump) 40 else 20
            owner = owner.copy(
                marriages = owner.marriages + card.suit,
                points = owner.points + if (owner.tricks > 0) bonus else 0,
                pendingMarriagePoints = owner.pendingMarriagePoints + if (owner.tricks == 0) bonus else 0,
            )
        }
        val next = state.withPlayer(player, owner).copy(table = state.table + SixtySixPlay(player, card))
        return if (owner.points >= 66) settle(next, player) else next
    }

    fun resolveTrick(state: SixtySixState): SixtySixState {
        require(state.result == null && state.table.size == 2)
        val lead = state.table[0]
        val follow = state.table[1]
        val winner = if (beats(follow.card, lead.card, state.trump)) follow.player else lead.player
        val cards = state.table.map { it.card }
        val points = cards.sumOf { it.sixtySixPoints }
        val owner = state.player(winner)
        var next = state.withPlayer(winner, owner.copy(
            points = owner.points + points + owner.pendingMarriagePoints,
            pendingMarriagePoints = 0,
            captured = owner.captured + cards,
            tricks = owner.tricks + 1,
        )).copy(table = emptyList(), leader = winner, lastTrick = SixtySixTrick(winner, cards, points))
        if (next.player(winner).points >= 66) return settle(next, winner)
        if (next.stockOpen) {
            // Stock order includes the face-up trump as the very last card.
            require(next.stock.size >= 2)
            next = next.withPlayer(winner, next.player(winner).copy(hand = next.player(winner).hand + next.stock[0]))
            next = next.withPlayer(winner.opponent, next.player(winner.opponent).copy(
                hand = next.player(winner.opponent).hand + next.stock[1],
            )).copy(stock = next.stock.drop(2))
        }
        if (next.human.hand.isEmpty() && next.computer.hand.isEmpty()) {
            val closure = next.closure
            return if (closure != null) settle(next, closure.player.opponent)
            else finish(next, winner, 1, "Ultima levată, fără 66: 1 punct de partidă.")
        }
        return next
    }

    private fun valueAgainst(points: Int): Int = when {
        points >= 33 -> 1
        points > 0 -> 2
        else -> 3
    }

    private fun settle(state: SixtySixState, winner: Player): SixtySixState {
        val closure = state.closure
        if (closure != null) {
            if (winner == closure.player) {
                return finish(state, winner, valueAgainst(closure.opponentPoints), "Închidere reușită: 66 de puncte.")
            }
            return finish(
                state, closure.player.opponent, if (closure.opponentPoints == 0) 3 else 2,
                "Închidere nereușită: adversarul celui care a închis câștigă.",
            )
        }
        return finish(state, winner, valueAgainst(state.player(winner.opponent).points), "66 de puncte revendicate automat.")
    }

    private fun finish(state: SixtySixState, winner: Player, points: Int, reason: String): SixtySixState =
        state.withPlayer(winner, state.player(winner).copy(
            gamePoints = state.player(winner).gamePoints + points,
        )).copy(result = SixtySixResult(winner, points, reason))

    fun computerMove(state: SixtySixState): SixtySixState {
        require(state.turn == Player.COMPUTER)
        var next = state
        if (canExchange(next, Player.COMPUTER)) next = exchangeTrump(next, Player.COMPUTER)
        val legal = legalCards(next, Player.COMPUTER)
        val marriage = legal.firstOrNull { canAnnounce(next, Player.COMPUTER, it) }
        // Only the bot's hand and the visible lead are consulted; never inspect unknown cards.
        val lead = next.table.firstOrNull()?.card
        val card = marriage ?: (if (lead != null) legal.filter { beats(it, lead, next.trump) } else emptyList())
            .minByOrNull { it.sixtySixPoints }
            ?: legal.minBy { it.sixtySixPoints + if (it.suit == next.trump) 12 else 0 }
        return play(next, Player.COMPUTER, card.id, announce = marriage != null)
    }
}
