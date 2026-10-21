package ro.septica.coach

import kotlin.random.Random
import kotlin.math.roundToInt
import ro.septica.game.*
import ro.septica.game.SepticaPlayer as Player

/** A public projection: hidden identities and the engine's shuffle seed never enter analysis. */
data class SepticaView(
    val hand: List<Card>, val opponentCounts: Map<Player, Int>, val stockCount: Int,
    val table: List<PlayedCard>, val leader: Player, val lastCut: Player?,
    val lastPublicTrick: List<Card>, val active: Boolean,
) {
    companion object {
        fun from(state: GameState) = SepticaView(
            state.humanHand, state.players.filter { it != Player.HUMAN }.associateWith { state.handFor(it).size },
            state.deck.size, state.table,
            state.trickLeader, state.lastCutBy, state.lastTrick?.cards.orEmpty(),
            state.phase == GamePhase.PLAYING && state.turn == Player.HUMAN,
        )
    }
}

object SepticaCoach {
    const val SAMPLES = 32

    fun advise(view: SepticaView): Advice {
        if (!view.active) return unavailableAdvice()
        val engine = SepticaEngine(Random(0))
        val playerCount = view.opponentCounts.size + 1
        val known = (view.hand + view.table.map { it.card } + view.lastPublicTrick).toSet()
        val unknown = SepticaRules.fullDeck(playerCount).filterNot { it in known }
        val opponentCount = view.opponentCounts.values.sum()
        require(opponentCount + view.stockCount <= unknown.size)
        fun position(cards: List<Card>): GameState {
            var offset = 0
            val hands = mutableMapOf(Player.HUMAN to view.hand)
            view.opponentCounts.forEach { (player, count) ->
                hands[player] = cards.drop(offset).take(count)
                offset += count
            }
            return GameState(
                hands = hands,
                deck = cards.drop(offset).take(view.stockCount),
                turn = Player.HUMAN, trickLeader = view.leader,
                table = view.table, lastCutBy = view.lastCut,
            )
        }
        val initial = position(unknown)
        val moves = engine.legalCards(initial, Player.HUMAN).map { it.id } +
            if (engine.canPass(initial, Player.HUMAN)) listOf(null) else emptyList()
        if (moves.isEmpty()) return unavailableAdvice()
        val seed = (view.hand.sortedBy { it.id }.joinToString { it.id } +
            view.table.joinToString { "${it.player}:${it.card.id}" } +
            "${view.opponentCounts}:${view.stockCount}:${view.leader}:${view.lastCut}" +
            view.lastPublicTrick.sortedBy { it.id }.joinToString { it.id }).hashCode()
        val samples = List(SAMPLES) { unknown.shuffled(Random(seed + it * 7919)) }
        fun cost(card: Card) = when {
            SepticaRules.isUniversalCut(card, playerCount) -> 0.45
            SepticaRules.pointValue(card) > 0 -> 0.12
            else -> 0.0
        }
        val ranked = moves.map { move ->
            move to samples.sumOf { sample ->
                var state = position(sample)
                state = if (move == null) engine.pass(state, Player.HUMAN)
                else engine.playCard(state, Player.HUMAN, move)
                // Only this trick is simulated. No refills, no fabricated moves or full-game claim.
                var steps = 0
                while (state.phase == GamePhase.PLAYING && steps++ < 32) {
                    val player = requireNotNull(state.turn)
                    val legal = engine.legalCards(state, player)
                    val target = state.targetRank
                    val cut = legal.filter { target != null && SepticaRules.isCut(target, it, playerCount) }
                        .minByOrNull { cost(it) }
                    val card = cut ?: if (engine.canPass(state, player)) null
                    else legal.minByOrNull { cost(it) }
                    state = when {
                        card != null -> engine.playCard(state, player, card.id)
                        engine.canPass(state, player) -> engine.pass(state, player)
                        else -> break // Exhausted endgame hand: use only the public current winner estimate.
                    }
                }
                val winner = state.trickWinner ?: state.lastCutBy
                val points = SepticaRules.points(state.table.map { it.card })
                val spent = view.hand.filterNot { it in state.humanHand }.sumOf { cost(it) }
                (if (winner == Player.HUMAN) points.toDouble() else -points.toDouble()) - spent
            }
        }
        val (best, score) = ranked.maxBy { it.second }
        val estimate = (score / SAMPLES * 100).roundToInt() / 100.0
        val card = view.hand.find { it.id == best }
        return Advice(
            if (card == null) "Pas — cedează levata" else "Joacă ${card.accessibilityLabel}",
            "Estimare netă: $estimate puncte (după costul consumării cărților). " +
                "$SAMPLES distribuții locale posibile pentru fiecare mutare legală, inclusiv pasul permis. " +
                "Estimez doar punctele levatei curente, scăzând costul consumării cărților de tăiere. " +
                "Continuarea este aproximativă; mâinile roboților și pachetul sunt eșantionate, nu citite.",
            card?.id, card?.let { CoachAction.Play(it.id) } ?: CoachAction.Pass,
        )
    }
}
