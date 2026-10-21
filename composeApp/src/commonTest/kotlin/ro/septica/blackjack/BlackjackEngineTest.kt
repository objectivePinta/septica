package ro.septica.blackjack

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.cards.standardDeck
import ro.septica.game.Suit

class BlackjackEngineTest {
    private fun card(rank: StandardRank, suit: Suit = Suit.CLUBS) = StandardCard(suit, rank)
    private fun deck(vararg cards: StandardCard) = cards.toList() + standardDeck().filterNot { it in cards }
    private val engine = BlackjackEngine(Random(1))

    @Test fun acesAndFaceValues() {
        assertEquals(12, handValue(listOf(card(StandardRank.ACE), card(StandardRank.ACE, Suit.HEARTS))))
        assertEquals(21, handValue(listOf(card(StandardRank.ACE), card(StandardRank.KING))))
        assertEquals(21, handValue(listOf(card(StandardRank.ACE), card(StandardRank.ACE, Suit.HEARTS), card(StandardRank.NINE))))
        assertEquals(20, handValue(listOf(card(StandardRank.JACK), card(StandardRank.QUEEN))))
    }

    @Test fun naturalsAndPushSettleExactlyOnce() {
        val natural = engine.deal(100, deck(card(StandardRank.ACE), card(StandardRank.NINE),
            card(StandardRank.KING), card(StandardRank.SEVEN)))
        assertEquals(BlackjackOutcome.NATURAL, natural.outcome)
        assertEquals(115, natural.bankroll)
        val push = engine.deal(100, deck(card(StandardRank.ACE), card(StandardRank.ACE, Suit.HEARTS),
            card(StandardRank.KING), card(StandardRank.QUEEN)))
        assertEquals(BlackjackOutcome.PUSH, push.outcome)
        assertEquals(100, push.bankroll)
        val loss = engine.deal(100, deck(card(StandardRank.NINE), card(StandardRank.ACE),
            card(StandardRank.SEVEN), card(StandardRank.KING)))
        assertEquals(BlackjackOutcome.LOSS, loss.outcome)
        assertEquals(90, loss.bankroll)
        listOf(natural, push, loss).forEach { state ->
            assertFailsWith<IllegalArgumentException> { engine.hit(state) }
            assertFailsWith<IllegalArgumentException> { engine.stand(state) }
            assertFailsWith<IllegalArgumentException> { engine.dealerStep(state) }
        }
    }

    @Test fun dealerStandsOnSoftSeventeen() {
        val initial = engine.deal(100, deck(card(StandardRank.TEN), card(StandardRank.ACE),
            card(StandardRank.SEVEN), card(StandardRank.SIX)))
        val final = engine.dealerStep(engine.stand(initial))
        assertEquals(initial.dealer, final.dealer)
        assertEquals(BlackjackOutcome.PUSH, final.outcome)
        assertEquals(100, final.bankroll)
    }

    @Test fun dealerBustAndPlayerBustAndBankruptcy() {
        val initial = engine.deal(100, deck(card(StandardRank.TEN), card(StandardRank.NINE),
            card(StandardRank.EIGHT), card(StandardRank.SEVEN), card(StandardRank.KING)))
        val win = engine.dealerStep(engine.stand(initial))
        assertEquals(BlackjackOutcome.WIN, win.outcome)
        assertEquals(110, win.bankroll)
        assertEquals(3, win.dealer.size)
        val loss = engine.hit(initial.copy(bankroll = 0))
        assertEquals(BlackjackOutcome.LOSS, loss.outcome)
        assertTrue(loss.bankrupt)
        assertFailsWith<IllegalArgumentException> { engine.nextRound(loss) }
        assertFailsWith<IllegalArgumentException> { engine.deal(9) }
        assertFailsWith<IllegalArgumentException> { engine.nextRound(initial) }
        assertFailsWith<IllegalArgumentException> { engine.dealerStep(initial) }
        assertFailsWith<IllegalArgumentException> { engine.hit(engine.stand(initial)) }
    }

    @Test fun sequentialDealerDrawsAndTwentyOneAutoStand() {
        val initial = engine.deal(100, deck(card(StandardRank.TEN), card(StandardRank.TWO),
            card(StandardRank.FIVE), card(StandardRank.THREE), card(StandardRank.SIX),
            card(StandardRank.FOUR), card(StandardRank.SEVEN), card(StandardRank.EIGHT)))
        var state = engine.hit(initial)
        assertEquals(BlackjackPhase.DEALER, state.phase)
        state = engine.dealerStep(state)
        assertEquals(3, state.dealer.size)
        assertEquals(BlackjackPhase.DEALER, state.phase)
        state = engine.dealerStep(state)
        assertEquals(4, state.dealer.size)
        assertEquals(BlackjackPhase.DEALER, state.phase)
        state = engine.dealerStep(state)
        assertEquals(BlackjackOutcome.WIN, state.outcome)
    }

    @Test fun seededRoundsConserveDeckAndBankroll() {
        assertEquals(BlackjackEngine(Random(42)).newGame(), BlackjackEngine(Random(42)).newGame())
        repeat(20) { seed ->
            val seeded = BlackjackEngine(Random(seed))
            var state = seeded.newGame()
            var startingBankroll = 100
            repeat(100) {
                var steps = 0
                while (state.phase != BlackjackPhase.FINISHED && steps++ < 52) {
                    state = when (state.phase) {
                        BlackjackPhase.PLAYER -> if (handValue(state.human) < 17) seeded.hit(state) else seeded.stand(state)
                        BlackjackPhase.DEALER -> seeded.dealerStep(state)
                        BlackjackPhase.FINISHED -> state
                    }
                    val all = state.human + state.dealer + state.deck
                    assertEquals(52, all.size)
                    assertEquals(standardDeck().toSet(), all.toSet())
                }
                assertEquals(BlackjackPhase.FINISHED, state.phase)
                val profit = when (state.outcome) {
                    BlackjackOutcome.WIN -> 10
                    BlackjackOutcome.NATURAL -> 15
                    BlackjackOutcome.PUSH -> 0
                    BlackjackOutcome.LOSS -> -10
                    null -> error("Missing outcome")
                }
                assertEquals(startingBankroll + profit, state.bankroll)
                startingBankroll = if (state.bankrupt) 100 else state.bankroll
                state = if (state.bankrupt) seeded.newGame() else seeded.nextRound(state)
            }
        }
    }
}
