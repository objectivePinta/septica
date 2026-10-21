package ro.septica.sixtysix

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.cards.StandardRank.ACE
import ro.septica.cards.StandardRank.JACK
import ro.septica.cards.StandardRank.KING
import ro.septica.cards.StandardRank.NINE
import ro.septica.cards.StandardRank.QUEEN
import ro.septica.cards.StandardRank.TEN
import ro.septica.game.Player
import ro.septica.game.Player.COMPUTER
import ro.septica.game.Player.HUMAN
import ro.septica.game.Suit
import ro.septica.game.Suit.CLUBS
import ro.septica.game.Suit.DIAMONDS
import ro.septica.game.Suit.HEARTS
import ro.septica.game.Suit.SPADES

class SixtySixEngineTest {
    private val engine = SixtySixEngine
    private fun card(rank: StandardRank, suit: Suit = CLUBS) = StandardCard(suit, rank)
    private fun fixture(
        human: List<StandardCard>,
        computer: List<StandardCard>,
        stock: List<StandardCard> = emptyList(),
        trump: Suit = HEARTS,
    ) = SixtySixState(
        SixtySixPlayer(hand = human), SixtySixPlayer(hand = computer),
        stock, trump, HUMAN, HUMAN,
    )

    private fun trick(state: SixtySixState, lead: StandardCard, follow: StandardCard): SixtySixState {
        val leader = state.leader
        return engine.resolveTrick(engine.play(
            engine.play(state, leader, lead.id), leader.opponent, follow.id,
        ))
    }

    @Test
    fun ranksPointsAndDeal() {
        val ranks = listOf(NINE, JACK, QUEEN, KING, TEN, ACE).map { card(it) }
        assertEquals(listOf(0, 2, 3, 4, 10, 11), ranks.map { it.sixtySixPoints })
        assertEquals((0..5).toList(), ranks.map { it.sixtySixStrength })
        val deck = sixtySixDeck()
        assertEquals(24, deck.size)
        assertEquals(120, deck.sumOf { it.sixtySixPoints })
        val state = engine.deal(deck)
        assertEquals(deck.take(6), state.human.hand)
        assertEquals(deck.drop(6).take(6), state.computer.hand)
        assertEquals(deck.drop(12), state.stock)
        assertEquals(deck.last(), state.faceUpTrump)
        assertEquals(HUMAN, state.turn)
        assertConserved(state)
        assertFailsWith<IllegalArgumentException> { engine.deal(deck.dropLast(1)) }
        assertFailsWith<IllegalArgumentException> { engine.deal(deck.dropLast(1) + deck.first()) }
    }

    @Test
    fun openStockAllowsAnyResponseAndTrumpWins() {
        val lead = card(ACE)
        val hand = listOf(card(NINE), card(JACK, HEARTS), card(ACE, SPADES))
        val state = engine.play(fixture(listOf(lead), hand, listOf(card(QUEEN), card(TEN, HEARTS))), HUMAN, lead.id)
        assertEquals(hand, engine.legalCards(state, COMPUTER))
        assertTrue(engine.beats(card(NINE, HEARTS), lead, HEARTS))
        assertFalse(engine.beats(card(ACE, SPADES), card(NINE), HEARTS))
        assertTrue(engine.beats(card(TEN), card(KING), HEARTS))
    }

    @Test
    fun strictFollowMustHeadAndCannotDiscardOrTrumpInstead() {
        val lead = card(QUEEN)
        val high = card(TEN)
        val low = card(JACK)
        val state = engine.play(fixture(listOf(lead), listOf(high, low, card(ACE, HEARTS))), HUMAN, lead.id)
        assertEquals(listOf(high), engine.legalCards(state, COMPUTER))
        assertFailsWith<IllegalArgumentException> { engine.play(state, COMPUTER, low.id) }
        val noHigher = state.copy(computer = state.computer.copy(hand = listOf(low, card(ACE, HEARTS))))
        assertEquals(listOf(low), engine.legalCards(noHigher, COMPUTER))
    }

    @Test
    fun strictVoidMustTrumpOtherwiseAnyAndTrumpLeadMustBeHeaded() {
        val lead = card(KING)
        val trumps = listOf(card(NINE, HEARTS), card(ACE, HEARTS))
        val discard = card(TEN, SPADES)
        val state = engine.play(fixture(listOf(lead), trumps + discard), HUMAN, lead.id)
        assertEquals(trumps, engine.legalCards(state, COMPUTER))
        assertEquals(listOf(discard), engine.legalCards(state.copy(
            computer = state.computer.copy(hand = listOf(discard)),
        ), COMPUTER))
        val trumpLead = card(KING, HEARTS)
        val trumpState = engine.play(fixture(listOf(trumpLead), trumps + discard), HUMAN, trumpLead.id)
        assertEquals(listOf(card(ACE, HEARTS)), engine.legalCards(trumpState, COMPUTER))
    }

    @Test
    fun exchangeOnlyByLeaderBeforePlayWithAtLeastTwoStockCards() {
        val nine = card(NINE, HEARTS)
        val exposed = card(ACE, HEARTS)
        val state = fixture(listOf(nine, card(KING)), listOf(card(JACK)), listOf(card(QUEEN), exposed))
        assertTrue(engine.canExchange(state, HUMAN))
        val next = engine.exchangeTrump(state, HUMAN)
        assertEquals(listOf(exposed, card(KING)), next.human.hand)
        assertEquals(nine, next.faceUpTrump)
        assertEquals(exposed, state.faceUpTrump)
        assertEquals(nine, state.human.hand.first())
        assertFalse(engine.canExchange(next, HUMAN))
        assertFalse(engine.canExchange(state, COMPUTER))
        assertFalse(engine.canExchange(state.copy(stock = listOf(exposed)), HUMAN))
        assertFalse(engine.canExchange(engine.closeStock(state, HUMAN), HUMAN))
        assertFalse(engine.canExchange(engine.play(state, HUMAN, card(KING).id), HUMAN))
        assertFailsWith<IllegalArgumentException> { engine.exchangeTrump(state, COMPUTER) }
    }

    @Test
    fun pendingMarriageCreditsOnlyOnFirstWinAndOnlyOnce() {
        val king = card(KING)
        val queen = card(QUEEN)
        var state = fixture(listOf(king, queen, card(ACE, HEARTS)), listOf(card(ACE), card(JACK, HEARTS), card(NINE)))
        state = engine.play(state, HUMAN, king.id, announce = true)
        assertEquals(0, state.human.points)
        assertEquals(20, state.human.pendingMarriagePoints)
        state = engine.resolveTrick(engine.play(state, COMPUTER, card(ACE).id))
        assertEquals(0, state.human.points)
        state = trick(state, card(JACK, HEARTS), card(ACE, HEARTS))
        assertEquals(33, state.human.points)
        assertEquals(0, state.human.pendingMarriagePoints)
        assertEquals(setOf(CLUBS), state.human.marriages)
        val repeated = state.copy(human = state.human.copy(hand = listOf(king, queen)))
        assertFalse(engine.canAnnounce(repeated, HUMAN, queen))
        assertFailsWith<IllegalArgumentException> { engine.play(repeated, HUMAN, queen.id, true) }
    }

    @Test
    fun trumpMarriageCreditsFortyAndWinsImmediatelyWithoutLosingPlayedCard() {
        val queen = card(QUEEN, HEARTS)
        val king = card(KING, HEARTS)
        val base = fixture(listOf(queen, king), listOf(card(ACE)))
        val state = base.copy(human = base.human.copy(points = 26, tricks = 1))
        val next = engine.play(state, HUMAN, queen.id, true)
        assertEquals(66, next.human.points)
        assertEquals(0, next.human.pendingMarriagePoints)
        assertEquals(listOf(king), next.human.hand)
        assertEquals(listOf(SixtySixPlay(HUMAN, queen)), next.table)
        assertEquals(HUMAN, next.result?.winner)
        assertEquals(3, next.human.gamePoints)
        assertNull(next.turn)
        assertFailsWith<IllegalArgumentException> { engine.resolveTrick(next) }
        assertFailsWith<IllegalArgumentException> { engine.play(next, COMPUTER, card(ACE).id) }
    }

    @Test
    fun pendingMarriagesAccumulateAndDoNotWinBeforeFirstTrick() {
        val base = fixture(
            listOf(card(KING, HEARTS), card(QUEEN, HEARTS)), listOf(card(ACE)),
        )
        val state = base.copy(human = base.human.copy(pendingMarriagePoints = 40))
        val next = engine.play(state, HUMAN, card(KING, HEARTS).id, true)
        assertEquals(80, next.human.pendingMarriagePoints)
        assertEquals(0, next.human.points)
        assertNull(next.result)
        val resolved = engine.resolveTrick(engine.play(next, COMPUTER, card(ACE).id))
        assertEquals(95, resolved.human.points)
        assertEquals(HUMAN, resolved.result?.winner)
        assertEquals(0, resolved.human.pendingMarriagePoints)
    }

    @Test
    fun invalidMarriageRequiresLeaderAndPair() {
        val base = fixture(listOf(card(KING)), listOf(card(QUEEN), card(KING, SPADES)))
        assertFalse(engine.canAnnounce(base, HUMAN, card(KING)))
        assertFalse(engine.canAnnounce(base, COMPUTER, card(QUEEN)))
        assertFailsWith<IllegalArgumentException> { engine.play(base, HUMAN, card(KING).id, true) }
        val played = engine.play(base, HUMAN, card(KING).id)
        assertFailsWith<IllegalArgumentException> { engine.play(played, COMPUTER, card(QUEEN).id, true) }
    }

    @Test
    fun winnerDrawsFirstAndFinalTrumpGoesToLoser() {
        val lead = card(JACK)
        val follow = card(ACE)
        val first = card(KING, SPADES)
        val exposed = card(QUEEN, HEARTS)
        val state = fixture(listOf(lead), listOf(follow), listOf(first, exposed))
        val next = trick(state, lead, follow)
        assertEquals(listOf(first), next.computer.hand)
        assertEquals(listOf(exposed), next.human.hand)
        assertEquals(COMPUTER, next.leader)
        assertEquals(13, next.computer.points)
        assertEquals(0, next.stock.size)
        assertFalse(next.stockOpen)
        assertNull(next.faceUpTrump)
        assertEquals(listOf(lead, follow), next.computer.captured)
    }

    @Test
    fun sixDrawsExhaustStockAndMaintainHands() {
        var state = engine.deal(sixtySixDeck())
        repeat(6) {
            val lead = engine.legalCards(state, state.leader).minBy { it.sixtySixPoints }
            state = engine.play(state, state.leader, lead.id)
            val follower = state.leader.opponent
            state = engine.play(state, follower, engine.legalCards(state, follower).minBy { it.sixtySixPoints }.id)
            state = engine.resolveTrick(state)
            assertNull(state.result)
            assertEquals(6, state.human.hand.size)
            assertEquals(6, state.computer.hand.size)
            assertEquals(10 - it * 2, state.stock.size)
            assertConserved(state)
        }
        assertFalse(state.stockOpen)
    }

    @Test
    fun closureStopsDrawingAndImmediatelyAppliesStrictFollowing() {
        val state = fixture(
            listOf(card(QUEEN)), listOf(card(ACE), card(NINE, SPADES)),
            listOf(card(JACK, DIAMONDS), card(ACE, HEARTS)),
        )
        val closed = engine.closeStock(state, HUMAN)
        assertFalse(closed.stockOpen)
        assertNull(closed.faceUpTrump)
        val lead = engine.play(closed, HUMAN, card(QUEEN).id)
        assertEquals(listOf(card(ACE)), engine.legalCards(lead, COMPUTER))
        val resolved = engine.resolveTrick(engine.play(lead, COMPUTER, card(ACE).id))
        assertEquals(state.stock, resolved.stock)
        assertTrue(resolved.human.hand.isEmpty())
        assertEquals(1, resolved.computer.hand.size)
        assertFailsWith<IllegalArgumentException> { engine.closeStock(lead, COMPUTER) }
        assertFailsWith<IllegalArgumentException> { engine.closeStock(closed, HUMAN) }
        assertFailsWith<IllegalArgumentException> { engine.closeStock(state.copy(stock = emptyList()), HUMAN) }
    }

    @Test
    fun closureSuccessUsesOpponentPointsAtClosureNotCurrentPoints() {
        for ((opponentPoints, award) in listOf(0 to 3, 1 to 2, 32 to 2, 33 to 1)) {
            val base = fixture(
                listOf(card(ACE)), listOf(card(JACK)),
                listOf(card(KING, SPADES), card(QUEEN, HEARTS)),
            )
            var state = base.copy(
                human = base.human.copy(points = 60, tricks = 1),
                computer = base.computer.copy(points = opponentPoints),
            )
            state = engine.closeStock(state, HUMAN)
            state = state.copy(computer = state.computer.copy(points = 45))
            state = trick(state, card(ACE), card(JACK))
            assertEquals(HUMAN, state.result?.winner)
            assertEquals(award, state.result?.gamePoints)
        }
    }

    @Test
    fun closureFailureOnExhaustionAwardsTwoOrThreeEvenWhenCloserTakesLastTrick() {
        for ((points, award) in listOf(0 to 3, 15 to 2, 40 to 2)) {
            val base = fixture(
                listOf(card(KING)), listOf(card(JACK)),
                listOf(card(ACE, SPADES), card(QUEEN, HEARTS)),
            )
            val state = engine.closeStock(base.copy(computer = base.computer.copy(points = points)), HUMAN)
            val next = trick(state, card(KING), card(JACK))
            assertEquals(HUMAN, next.lastTrick?.winner)
            assertEquals(COMPUTER, next.result?.winner)
            assertEquals(award, next.result?.gamePoints)
            assertEquals(2, next.stock.size)
        }
    }

    @Test
    fun closureFailureWhenOpponentReaches66UsesSnapshot() {
        val base = fixture(
            listOf(card(JACK)), listOf(card(ACE)),
            listOf(card(KING, SPADES), card(QUEEN, HEARTS)),
        )
        val closed = engine.closeStock(base, HUMAN)
        val state = closed.copy(computer = closed.computer.copy(points = 60))
        val next = trick(state, card(JACK), card(ACE))
        assertEquals(COMPUTER, next.result?.winner)
        assertEquals(3, next.result?.gamePoints)
    }

    @Test
    fun marriageCanSettleClosedRoundForEitherPlayer() {
        for (closer in Player.entries) {
            val base = fixture(
                listOf(card(KING), card(QUEEN)), listOf(card(ACE)),
                listOf(card(JACK, SPADES), card(NINE, HEARTS)),
            )
            val state = base.copy(
                human = base.human.copy(points = 46, tricks = 1),
                closure = StockClosure(closer, if (closer == HUMAN) 33 else 46),
            )
            val next = engine.play(state, HUMAN, card(QUEEN).id, true)
            assertEquals(HUMAN, next.result?.winner)
            assertEquals(if (closer == HUMAN) 1 else 2, next.result?.gamePoints)
            assertEquals(66, next.human.points)
            assertEquals(1, next.table.size)
            assertEquals(1, next.human.hand.size)
            assertEquals(state.stock, next.stock)
        }
    }

    @Test
    fun normalSettlementThresholdsAndLastTrickWithoutBonus() {
        for ((points, award) in listOf(0 to 3, 1 to 2, 32 to 2, 33 to 1)) {
            val base = fixture(listOf(card(ACE)), listOf(card(JACK)))
            val next = trick(base.copy(
                human = base.human.copy(points = 60),
                computer = base.computer.copy(points = points),
            ), card(ACE), card(JACK))
            assertEquals(73, next.human.points)
            assertEquals(award, next.result?.gamePoints)
        }
        val last = trick(fixture(listOf(card(KING)), listOf(card(JACK))), card(KING), card(JACK))
        assertEquals(6, last.human.points)
        assertEquals(1, last.result?.gamePoints)
    }

    @Test
    fun matchTargetNextRoundAndAlternatingLeader() {
        val base = fixture(listOf(card(ACE)), listOf(card(JACK)))
        val almost = base.copy(human = base.human.copy(points = 60, gamePoints = 5))
        val won = trick(almost, card(ACE), card(JACK))
        assertEquals(8, won.human.gamePoints)
        assertEquals(HUMAN, won.matchWinner)
        assertFailsWith<IllegalArgumentException> { engine.nextRound(won) }
        val round = trick(base, card(ACE), card(JACK))
        val next = engine.nextRound(round, Random(1))
        assertEquals(COMPUTER, next.firstLeader)
        assertEquals(COMPUTER, next.leader)
        assertEquals(round.human.gamePoints, next.human.gamePoints)
        assertEquals(2, next.roundNumber)
        assertEquals(0, next.human.points)
        assertTrue(next.human.marriages.isEmpty())
        assertConserved(next)
    }

    @Test
    fun invalidActionsLeaveOriginalStateUntouched() {
        val state = engine.newMatch(Random(1))
        val snapshot = state.copy()
        assertFailsWith<IllegalArgumentException> { engine.play(state, COMPUTER, state.computer.hand.first().id) }
        assertFailsWith<IllegalArgumentException> { engine.play(state, HUMAN, "missing") }
        assertFailsWith<IllegalArgumentException> { engine.resolveTrick(state) }
        assertFailsWith<IllegalArgumentException> { engine.nextRound(state) }
        assertFailsWith<IllegalArgumentException> { engine.closeStock(state, COMPUTER) }
        val played = engine.play(state, HUMAN, state.human.hand.first().id)
        assertEquals(snapshot, state)
        assertEquals(6, state.human.hand.size)
        assertEquals(5, played.human.hand.size)
        assertConserved(played)
        assertFailsWith<IllegalArgumentException> { engine.play(played, HUMAN, state.human.hand.last().id) }
    }

    @Test
    fun botDoesNotUseHiddenHandsOrStockOrder() {
        val base = fixture(
            listOf(card(ACE), card(TEN)), listOf(card(JACK), card(QUEEN, SPADES)),
            listOf(card(KING), card(NINE, HEARTS)),
        ).copy(leader = COMPUTER)
        val changed = base.copy(
            human = base.human.copy(hand = listOf(card(NINE, SPADES))),
            stock = listOf(card(ACE, DIAMONDS), base.stock.last()),
        )
        assertEquals(engine.computerMove(base).table, engine.computerMove(changed).table)
    }

    @Test
    fun seededRoundsConserveEveryCardAndAlwaysTerminate() {
        repeat(250) { seed ->
            val random = Random(seed)
            var state = engine.newMatch(random)
            var actions = 0
            while (state.result == null) {
                assertConserved(state)
                assertTrue(actions++ < 90, "Seed $seed did not terminate")
                val before = state
                if (state.table.size == 2) {
                    state = engine.resolveTrick(state)
                } else {
                    val player = assertNotNull(state.turn)
                    if (engine.canExchange(state, player) && random.nextBoolean()) {
                        state = engine.exchangeTrump(state, player)
                        assertConserved(state)
                    }
                    if (engine.canClose(state, player) && random.nextInt(12) == 0) {
                        state = engine.closeStock(state, player)
                    }
                    if (player == COMPUTER) {
                        state = engine.computerMove(state)
                    } else {
                        val legal = engine.legalCards(state, player)
                        val selected = legal.firstOrNull { engine.canAnnounce(state, player, it) } ?: legal.random(random)
                        state = engine.play(state, player, selected.id, engine.canAnnounce(state, player, selected))
                    }
                }
                assertConserved(before)
            }
            assertConserved(state)
            assertTrue(assertNotNull(state.result).gamePoints in 1..3)
        }
    }

    private fun assertConserved(state: SixtySixState) {
        val cards = state.human.hand + state.computer.hand + state.stock +
            state.human.captured + state.computer.captured + state.table.map { it.card }
        assertEquals(24, cards.size)
        assertEquals(sixtySixDeck().toSet(), cards.toSet())
    }
}
