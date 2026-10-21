package ro.septica.game

import kotlin.random.Random

class SepticaEngine(
    private val random: Random = Random.Default,
) {
    fun newGame(
        startingPlayer: SepticaPlayer = SepticaPlayer.HUMAN,
        playerCount: Int = 2,
    ): GameState {
        require(playerCount in 2..4) { "Septica supports two to four players." }
        val players = SepticaPlayer.entries.take(playerCount)
        require(startingPlayer in players) { "The starting player is not in this game." }
        val shuffledDeck = SepticaRules.fullDeck(playerCount).shuffled(random)
        val hands = players.associateWith { mutableListOf<Card>() }
        var nextCard = 0
        repeat(SepticaRules.INITIAL_HAND_SIZE) {
            players.forEach { hands.getValue(it) += shuffledDeck[nextCard++] }
        }
        return GameState(
            hands = hands.mapValues { it.value.sortedForHand() },
            deck = shuffledDeck.drop(nextCard),
            turn = startingPlayer,
            trickLeader = startingPlayer,
        )
    }

    fun legalCards(state: GameState, player: SepticaPlayer): List<Card> {
        if (state.phase != GamePhase.PLAYING || state.turn != player) return emptyList()
        if (state.table.isEmpty() || player != state.trickLeader) return state.handFor(player)
        val targetRank = requireNotNull(state.targetRank)
        return state.handFor(player).filter { SepticaRules.isCut(targetRank, it, state.players.size) }
    }

    fun canPass(state: GameState, player: SepticaPlayer): Boolean =
        state.phase == GamePhase.PLAYING &&
            state.turn == player &&
            state.table.isNotEmpty() &&
            player == state.trickLeader &&
            state.lastCutBy != null && state.lastCutBy != player

    fun playCard(state: GameState, player: SepticaPlayer, cardId: String): GameState {
        require(state.phase == GamePhase.PLAYING) { "The game is not accepting cards." }
        require(state.turn == player) { "It is not this player's turn." }
        val card = state.handFor(player).singleOrNull { it.id == cardId }
            ?: error("The selected card is not in the player's hand.")
        require(card in legalCards(state, player)) { "That card cannot continue this trick." }

        val opening = state.table.isEmpty()
        val cardCuts = opening || SepticaRules.isCut(requireNotNull(state.targetRank), card, state.players.size)
        val updated = state.copy(
            hands = state.hands + (player to (state.handFor(player) - card)),
            table = state.table + PlayedCard(player, card),
            lastCutBy = if (cardCuts) player else state.lastCutBy,
            lastTrick = if (opening) null else state.lastTrick,
        )
        return advanceTurn(updated, player)
    }

    private fun advanceTurn(state: GameState, playedBy: SepticaPlayer): GameState {
        // Every follower responds before the original leader may continue or concede.
        for (next in state.orderFrom(playedBy).drop(1) + playedBy) {
            if (next == state.trickLeader) {
                val winner = requireNotNull(state.lastCutBy)
                return if (winner == next) revealTrick(state, winner)
                else state.copy(turn = next)
            }
            if (state.handFor(next).isNotEmpty()) return state.copy(turn = next)
        }
        error("A trick must return to its leader.")
    }

    fun pass(state: GameState, player: SepticaPlayer): GameState {
        require(canPass(state, player)) { "Passing is not legal in the current state." }
        return revealTrick(state, requireNotNull(state.lastCutBy))
    }

    fun completeTrick(state: GameState): GameState {
        require(state.phase == GamePhase.TRICK_REVEAL) { "There is no trick to collect." }
        val winner = requireNotNull(state.trickWinner)
        var deck = state.deck
        val hands = state.hands.toMutableMap()
        val drawOrder = state.orderFrom(winner)
        while (deck.isNotEmpty() && hands.values.any { it.size < SepticaRules.INITIAL_HAND_SIZE }) {
            for (player in drawOrder) {
                if (deck.isEmpty()) break
                val hand = hands.getValue(player)
                if (hand.size < SepticaRules.INITIAL_HAND_SIZE) {
                    hands[player] = hand + deck.first()
                    deck = deck.drop(1)
                }
            }
        }
        val nextLeader = drawOrder.firstOrNull { hands.getValue(it).isNotEmpty() }
        val finished = nextLeader == null
        return state.copy(
            hands = hands.mapValues { it.value.sortedForHand() },
            deck = deck,
            turn = nextLeader,
            trickLeader = nextLeader ?: winner,
            table = emptyList(),
            lastCutBy = null,
            phase = if (finished) GamePhase.FINISHED else GamePhase.PLAYING,
            trickWinner = null,
            trickPoints = 0,
            completedTricks = state.completedTricks + 1,
            lastTrick = TrickSummary(winner, state.trickPoints, state.table.map(PlayedCard::card)),
        )
    }

    fun chooseComputerCard(state: GameState): Card? {
        val player = requireNotNull(state.turn)
        require(state.phase == GamePhase.PLAYING && player != SepticaPlayer.HUMAN) {
            "The computer cannot act now."
        }
        val hand = state.handFor(player)
        val playerCount = state.players.size
        if (state.table.isEmpty()) return chooseLead(hand, playerCount)
        val targetRank = requireNotNull(state.targetRank)
        val cuts = hand.filter { SepticaRules.isCut(targetRank, it, playerCount) }
        if (player == state.trickLeader) {
            if (cuts.isEmpty()) return null
            val pointsAtStake = SepticaRules.points(state.table.map(PlayedCard::card))
            return if (pointsAtStake > 0) chooseCheapestCut(cuts, targetRank, playerCount) else null
        }
        return if (cuts.isNotEmpty()) chooseCheapestCut(cuts, targetRank, playerCount)
        else hand.minByOrNull { preservationCost(it, playerCount) }
    }

    private fun chooseLead(hand: List<Card>, playerCount: Int): Card {
        val cardsPerRank = hand.groupingBy(Card::rank).eachCount()
        return requireNotNull(
            hand.minWithOrNull(
                compareBy<Card> { if (cardsPerRank.getValue(it.rank) > 1) 0 else 1 }
                    .thenBy { preservationCost(it, playerCount) },
            ),
        )
    }

    private fun chooseCheapestCut(cuts: List<Card>, targetRank: Rank, playerCount: Int): Card =
        requireNotNull(
            cuts.minWithOrNull(
                compareBy<Card> { if (it.rank == targetRank) 0 else 1 }
                    .thenBy { preservationCost(it, playerCount) },
            ),
        )

    private fun preservationCost(card: Card, playerCount: Int): Int =
        when {
            SepticaRules.isUniversalCut(card, playerCount) -> 40
            SepticaRules.pointValue(card) > 0 -> 20 + card.rank.sortOrder
            else -> card.rank.sortOrder
        }

    private fun revealTrick(state: GameState, winner: SepticaPlayer): GameState {
        val points = SepticaRules.points(state.table.map(PlayedCard::card))
        return state.copy(
            turn = null,
            phase = GamePhase.TRICK_REVEAL,
            trickWinner = winner,
            trickPoints = points,
            scores = state.scores + (winner to (state.scoreFor(winner) + points)),
            tricks = state.tricks + (winner to ((state.tricks[winner] ?: 0) + 1)),
        )
    }

    private fun List<Card>.sortedForHand(): List<Card> =
        sortedWith(compareBy<Card> { it.suit.ordinal }.thenBy { it.rank.sortOrder })
}
