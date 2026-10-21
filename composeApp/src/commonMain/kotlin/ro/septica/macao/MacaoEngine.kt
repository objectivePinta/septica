package ro.septica.macao

import kotlin.random.Random
import ro.septica.game.Player
import ro.septica.game.Suit

/**
 * Two-player variant: the last card wins immediately, including special cards.
 * Reaching one remaining card declares Macao automatically, without a declaration penalty.
 */
class MacaoEngine(
    private val random: Random = Random.Default,
) {
    fun newGame(): MacaoState {
        val cards = buildList {
            for (suit in Suit.entries) {
                for (rank in MacaoRank.entries) {
                    if (!rank.isJoker) add(MacaoCard(suit, rank))
                }
            }
            add(MacaoCard(null, MacaoRank.BLACK_JOKER))
            add(MacaoCard(null, MacaoRank.RED_JOKER))
        }.shuffled(random)
        val stock = cards.drop(10)
        val openingIndex = stock.indexOfFirst {
            it.rank in MacaoRank.FIVE..MacaoRank.KING
        }
        val opening = stock[openingIndex]
        return MacaoState(
            humanHand = cards.take(5),
            computerHand = cards.drop(5).take(5),
            deck = stock.filterIndexed { index, _ -> index != openingIndex },
            discardPile = listOf(opening),
            turn = Player.HUMAN,
            activeSuit = requireNotNull(opening.suit),
            notice = "Joc nou. Este rândul tău.",
        )
    }

    fun legalCards(state: MacaoState, player: Player): List<MacaoCard> {
        if (state.winner != null || state.turn != player) return emptyList()
        return state.handFor(player).filter { card ->
            if (state.pendingDraw > 0) {
                card.rank.drawPenalty > 0
            } else {
                card.rank.isWild ||
                    card.suit == state.activeSuit ||
                    card.rank == state.discardPile.last().rank
            }
        }
    }

    fun playCard(
        state: MacaoState,
        player: Player,
        cardId: String,
        chosenSuit: Suit? = null,
    ): MacaoState {
        requireTurn(state, player)
        val card = requireNotNull(state.handFor(player).singleOrNull { it.id == cardId }) {
            "The selected card is not in the player's hand."
        }
        require(card in legalCards(state, player)) { "The selected card is not legal." }
        require(!card.rank.isWild || chosenSuit != null) {
            "An ace or joker requires an explicit suit choice."
        }
        val activeSuit = if (card.rank.isWild) requireNotNull(chosenSuit) else requireNotNull(card.suit)
        val hand = state.handFor(player) - card
        val won = hand.isEmpty()
        val nextPlayer = if (won || card.rank == MacaoRank.FOUR) player else player.opponent
        val pendingDraw = if (won) 0 else state.pendingDraw + card.rank.drawPenalty
        val effect = when {
            won -> if (player == Player.HUMAN) "Ai câștigat!" else "Calculatorul a câștigat!"
            pendingDraw > 0 -> "Penalizare: $pendingDraw cărți. Pune un atac sau trage cărțile."
            card.rank == MacaoRank.FOUR -> "Rând sărit. ${nextPlayer.turnLabel}"
            card.rank.isWild -> "Culoarea aleasă: ${activeSuit.romanianName}. ${nextPlayer.turnLabel}"
            else -> nextPlayer.turnLabel
        }
        return state.copy(
            humanHand = if (player == Player.HUMAN) hand else state.humanHand,
            computerHand = if (player == Player.COMPUTER) hand else state.computerHand,
            discardPile = state.discardPile + card,
            activeSuit = activeSuit,
            turn = nextPlayer,
            pendingDraw = pendingDraw,
            winner = if (won) player else null,
            notice = if (hand.size == 1) "Macao! $effect" else effect,
        )
    }

    fun drawCards(state: MacaoState, player: Player): MacaoState {
        requireTurn(state, player)
        val requested = if (state.pendingDraw > 0) state.pendingDraw else 1
        var deck = state.deck
        var discardPile = state.discardPile
        val drawn = mutableListOf<MacaoCard>()
        while (drawn.size < requested) {
            if (deck.isEmpty()) {
                if (discardPile.size <= 1) break
                deck = discardPile.dropLast(1).shuffled(random)
                discardPile = listOf(discardPile.last())
            }
            val count = minOf(requested - drawn.size, deck.size)
            drawn.addAll(deck.take(count))
            deck = deck.drop(count)
        }
        val hand = state.handFor(player) + drawn
        val result = if (drawn.size < requested) {
            "Pachet epuizat: ${drawn.size} din $requested cărți trase."
        } else {
            "${drawn.size} cărți trase."
        }
        return state.copy(
            humanHand = if (player == Player.HUMAN) hand else state.humanHand,
            computerHand = if (player == Player.COMPUTER) hand else state.computerHand,
            deck = deck,
            discardPile = discardPile,
            turn = player.opponent,
            pendingDraw = 0,
            notice = "$result ${player.opponent.turnLabel}",
        )
    }

    fun chooseComputerCard(state: MacaoState): MacaoCard? =
        legalCards(state, Player.COMPUTER).maxByOrNull { card ->
            val priority = when {
                card.rank == MacaoRank.FOUR -> 30
                !card.rank.isWild && card.rank.drawPenalty > 0 -> 20
                !card.rank.isWild -> 10
                else -> 0
            }
            priority + state.computerHand.count { it.suit == card.suit }
        }

    fun chooseComputerSuit(state: MacaoState): Suit =
        Suit.entries.maxByOrNull { suit -> state.computerHand.count { it.suit == suit } }
            ?: Suit.CLUBS

    private fun requireTurn(state: MacaoState, player: Player) {
        require(state.winner == null) { "The game has already finished." }
        require(state.turn == player) { "It is not this player's turn." }
    }

    private fun MacaoState.handFor(player: Player): List<MacaoCard> =
        if (player == Player.HUMAN) humanHand else computerHand

    private val MacaoRank.isJoker: Boolean
        get() = this == MacaoRank.BLACK_JOKER || this == MacaoRank.RED_JOKER

    private val MacaoRank.isWild: Boolean
        get() = this == MacaoRank.ACE || isJoker

    private val MacaoRank.drawPenalty: Int
        get() = when (this) {
            MacaoRank.TWO -> 2
            MacaoRank.THREE -> 3
            MacaoRank.BLACK_JOKER -> 5
            MacaoRank.RED_JOKER -> 10
            else -> 0
        }

    private val Player.turnLabel: String
        get() = if (this == Player.HUMAN) "Este rândul tău." else "Este rândul calculatorului."
}
