package ro.septica.tricks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.game.Suit

class TrickGamesTest {
    private fun card(suit: Suit, rank: StandardRank) = StandardCard(suit, rank)
    private val h7 = card(Suit.HEARTS, StandardRank.SEVEN)
    private val ha = card(Suit.HEARTS, StandardRank.ACE)
    private val s7 = card(Suit.SPADES, StandardRank.SEVEN)
    private val sa = card(Suit.SPADES, StandardRank.ACE)
    private val ca = card(Suit.CLUBS, StandardRank.ACE)

    @Test
    fun scheduleAndDeal() {
        assertEquals(listOf(1, 1, 1, 1, 2, 3, 4, 5, 6, 7, 8, 8, 8, 8, 7, 6, 5, 4, 3, 2, 1, 1, 1, 1), whistSchedule)
        val state = newTrickGame(TrickMode.WHIST, 42)
        assertEquals(1, state.turn)
        assertEquals(28, state.stock.size)
        assertEquals(state.stock.first().suit, state.trump)
        assertEquals(state, newTrickGame(TrickMode.WHIST, 42))
    }

    @Test
    fun mandatorySuitThenTrumpWithoutOvertrump() {
        val table = listOf(PlayedCard(0, h7), PlayedCard(1, sa))
        assertEquals(listOf(ha), legalTrickCards(listOf(ha, s7, ca), table, Suit.SPADES))
        assertEquals(listOf(s7), legalTrickCards(listOf(s7, ca), table, Suit.SPADES))
        assertEquals(listOf(ca), legalTrickCards(listOf(ca), table, Suit.SPADES))
        assertEquals(listOf(s7, ca), legalTrickCards(listOf(s7, ca), table, null))
    }

    @Test
    fun winnerUsesTrumpOrLedSuitOnly() {
        val table = listOf(PlayedCard(0, h7), PlayedCard(1, ha), PlayedCard(2, s7), PlayedCard(3, ca))
        assertEquals(2, trickWinner(table, Suit.SPADES))
        assertEquals(1, trickWinner(table, null))
    }

    @Test
    fun lastBidRestrictionAndScore() {
        var state = newTrickGame(TrickMode.WHIST, 4)
        repeat(3) { state = bid(state, 0) }
        assertEquals(0, state.turn)
        assertEquals(listOf(0), legalBids(state))
        assertFailsWith<IllegalArgumentException> { bid(state, 1) }
        state = bid(state, 0)
        assertEquals(TrickPhase.PLAYING, state.phase)
        assertEquals(1, state.turn)
        assertEquals(5, whistScore(0, 0))
        assertEquals(8, whistScore(3, 3))
        assertEquals(-2, whistScore(3, 1))
        assertEquals(-2, whistScore(1, 3))
    }

    @Test
    fun eachContractScoresIndependently() {
        val cards = listOf(h7, card(Suit.HEARTS, StandardRank.KING),
            card(Suit.CLUBS, StandardRank.QUEEN), card(Suit.HEARTS, StandardRank.QUEEN))
        assertEquals(-10, contractScore(Contract.NO_TRICKS, cards, 0))
        assertEquals(-30, contractScore(Contract.NO_HEARTS, cards, 0))
        assertEquals(-50, contractScore(Contract.NO_QUEENS, cards, 0))
        assertEquals(-100, contractScore(Contract.NO_KING_HEARTS, cards, 0))
        assertEquals(0, contractScore(Contract.NO_KING_HEARTS, listOf(h7, ca), 7))
        assertEquals(0, contractScore(Contract.NO_LAST_TWO, cards, 5))
        assertEquals(-50, contractScore(Contract.NO_LAST_TWO, cards, 6))
        assertEquals(-50, contractScore(Contract.NO_LAST_TWO, cards, 7))
        assertEquals(-190, contractScore(Contract.TOTALS, cards, 0))
        assertEquals(-240, contractScore(Contract.TOTALS, cards, 6))
        assertEquals(25, contractScore(Contract.WHIST, cards, 7))
        assertEquals(0, contractScore(Contract.RENTZ, cards, 7))
    }

    @Test
    fun dominoStartsAtJackAndNeverAllowsGaps() {
        val jack = card(Suit.CLUBS, StandardRank.JACK)
        val ten = card(Suit.CLUBS, StandardRank.TEN)
        val queen = card(Suit.CLUBS, StandardRank.QUEEN)
        assertTrue(legalPlacement(jack, emptySet()))
        assertFalse(legalPlacement(ten, emptySet()))
        assertTrue(legalPlacement(ten, setOf(jack)))
        assertTrue(legalPlacement(queen, setOf(jack)))
        assertFalse(legalPlacement(ca, setOf(jack, queen)))
        assertFalse(legalPlacement(jack, setOf(jack)))
        assertFalse(legalPlacement(h7, setOf(jack)))
        assertTrue(legalPlacement(card(Suit.HEARTS, StandardRank.JACK), setOf(jack)))
    }

    @Test
    fun passIsForcedAndFinishedSeatsAreSkipped() {
        val jack = card(Suit.CLUBS, StandardRank.JACK)
        val state = newTrickGame(TrickMode.RENTZ, 1).copy(
            round = 7, turn = 0, hands = listOf(listOf(ca), emptyList(), listOf(jack), listOf(h7)),
            finishOrder = listOf(1),
        )
        assertEquals(2, pass(state).turn)
        assertFailsWith<IllegalArgumentException> { pass(state.copy(turn = 2)) }
        val next = playCard(state.copy(turn = 2), jack)
        assertEquals(listOf(1, 2), next.finishOrder)
        assertEquals(75, next.roundScores[2])
        assertEquals(3, next.turn)
    }

    @Test
    fun finishingAwardsOnceAndCompletesLastSeat() {
        val jack = card(Suit.CLUBS, StandardRank.JACK)
        val state = newTrickGame(TrickMode.RENTZ, 1).copy(round = 7, turn = 2,
            hands = listOf(emptyList(), emptyList(), listOf(jack), listOf(ca)),
            finishOrder = listOf(0, 1), roundScores = listOf(100, 75, 0, 0))
        val finished = playCard(state, jack)
        assertEquals(TrickPhase.FINISHED, finished.phase)
        assertEquals(listOf(100, 75, 50, 25), finished.scores)
        assertEquals(listOf(0, 1, 2, 3), finished.finishOrder)
        assertTrue(finished.hands.all { it.isEmpty() })
        assertTrue(ca in finished.lanes)
        assertFailsWith<IllegalArgumentException> { botStep(finished) }
        assertFailsWith<IllegalArgumentException> { playCard(finished, jack) }
        assertFailsWith<IllegalArgumentException> { nextRound(finished) }
        assertEquals(listOf(0, 2), finished.copy(scores = listOf(100, 50, 100, 25)).winners)
    }

    @Test
    fun botActionsRejectNonActionablePhases() {
        val state = newTrickGame(TrickMode.WHIST, 42)
        for (phase in listOf(TrickPhase.REVEAL, TrickPhase.ROUND_OVER, TrickPhase.FINISHED)) {
            assertFailsWith<IllegalArgumentException> { botStep(state.copy(phase = phase)) }
        }
    }

    @Test
    fun boundedSeededCompleteMatchesConserveCardsAndRotate() {
        for (mode in TrickMode.entries) repeat(20) { seed ->
            var state = newTrickGame(mode, seed)
            var steps = 0
            val rounds = mutableSetOf<Int>()
            var expectedScores = List(4) { 0 }
            val scoredRounds = mutableSetOf<Int>()
            while (state.phase != TrickPhase.FINISHED) {
                assertTrue(++steps < 2500, "$mode seed=$seed failed to finish")
                rounds += state.round
                val all = state.hands.flatten() + state.stock + state.table.map { it.card } +
                    state.captured + state.lanes
                assertEquals(32, all.size)
                assertEquals(32, all.toSet().size)
                assertEquals(state.round % 4, state.dealer)
                if (mode == TrickMode.WHIST && state.handSize == 8) assertEquals(null, state.trump)
                state = when (state.phase) {
                    TrickPhase.REVEAL -> {
                        assertEquals(4, state.lastTrick.size)
                        continueTrick(state)
                    }
                    TrickPhase.ROUND_OVER -> {
                        assertTrue(scoredRounds.add(state.round), "Round scored more than once")
                        expectedScores = (0..3).map { expectedScores[it] + state.roundScores[it] }
                        assertEquals(expectedScores, state.scores)
                        if (mode == TrickMode.WHIST) {
                            assertEquals((0..3).map { whistScore(state.bids[it]!!, state.tricks[it]) },
                                state.roundScores)
                        }
                        if (mode == TrickMode.RENTZ) {
                            val total = when (state.contract!!) {
                                Contract.NO_TRICKS, Contract.NO_HEARTS -> -80
                                Contract.NO_QUEENS, Contract.NO_KING_HEARTS, Contract.NO_LAST_TWO -> -100
                                Contract.TOTALS -> -460
                                Contract.WHIST -> 200
                                Contract.RENTZ -> 250
                            }
                            assertEquals(total, state.roundScores.sum())
                        }
                        val next = nextRound(state)
                        assertEquals((next.dealer + 1) % 4, next.turn)
                        assertEquals(state.scores, next.scores)
                        next
                    }
                    else -> botStep(state)
                }
            }
            assertEquals(state.roundCount, rounds.size)
            assertTrue(scoredRounds.add(state.round))
            expectedScores = (0..3).map { expectedScores[it] + state.roundScores[it] }
            assertEquals(state.roundCount, scoredRounds.size)
            assertEquals(expectedScores, state.scores)
            if (mode == TrickMode.WHIST) {
                assertEquals((0..3).map { whistScore(state.bids[it]!!, state.tricks[it]) },
                    state.roundScores)
            }
            assertTrue(state.winners.isNotEmpty())
            assertEquals(32, (state.stock + state.captured + state.lanes).toSet().size)
            if (mode == TrickMode.RENTZ) {
                assertEquals(250, state.roundScores.sum())
                // Five negative contracts total -460; Totals repeats them, then +200 and +250.
                assertEquals(-80 - 80 - 100 - 100 - 100 - 460 + 200 + 250, state.scores.sum())
            }
        }
    }
}
