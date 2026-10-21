package ro.septica.war

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.cards.standardDeck
import ro.septica.game.Player
import ro.septica.game.Suit

class WarEngineTest {
    private fun card(rank: StandardRank, suit: Suit = Suit.CLUBS) = StandardCard(suit, rank)

    @Test fun seedAndDeal() {
        val a = WarEngine(Random(19)).newGame()
        assertEquals(a, WarEngine(Random(19)).newGame())
        assertEquals(26, a.human.size)
        assertEquals(standardDeck().toSet(), (a.human + a.computer).toSet())
    }

    @Test fun aceHighAndFinishedMovesRejected() {
        val engine = WarEngine()
        val state = engine.flip(WarState(listOf(card(StandardRank.ACE)), listOf(card(StandardRank.KING))))
        assertEquals(Player.HUMAN, state.winner)
        assertEquals(2, state.human.size)
        assertFailsWith<IllegalArgumentException> { engine.flip(state) }
    }

    @Test fun repeatedWarsConservePotAndUseLastCardWhenShort() {
        val engine = WarEngine()
        val human = listOf(StandardRank.TWO, StandardRank.THREE, StandardRank.FOUR, StandardRank.FIVE,
            StandardRank.SIX, StandardRank.SEVEN, StandardRank.ACE).map { card(it) }
        val computer = listOf(StandardRank.TWO, StandardRank.THREE, StandardRank.FOUR, StandardRank.FIVE,
            StandardRank.SIX, StandardRank.EIGHT, StandardRank.KING).map { card(it, Suit.HEARTS) }
        var state = engine.flip(WarState(human, computer))
        assertTrue(state.war)
        assertEquals(2, state.pot.size)
        state = engine.flip(state)
        assertTrue(state.war)
        assertEquals(10, state.pot.size)
        state = engine.flip(state)
        assertEquals(Player.HUMAN, state.winner)
        assertEquals((human + computer).toSet(), state.human.toSet())
        assertEquals(1, state.battles)
    }

    @Test fun exhaustionAndBattleCap() {
        val engine = WarEngine()
        val h = card(StandardRank.TWO)
        val c = card(StandardRank.TWO, Suit.HEARTS)
        val draw = engine.flip(WarState(listOf(h), listOf(c)))
        assertTrue(draw.finished)
        assertEquals(null, draw.winner)
        assertEquals(setOf(h, c), draw.pot.toSet())
        val loss = engine.flip(WarState(listOf(h), listOf(c, card(StandardRank.ACE))))
        assertEquals(Player.COMPUTER, loss.winner)
        assertEquals(3, loss.computer.size)
        val capped = engine.flip(WarState(
            listOf(card(StandardRank.ACE), h),
            listOf(card(StandardRank.KING), c), battles = 999,
        ))
        assertTrue(capped.finished)
        assertEquals(null, capped.winner)
    }

    @Test fun seededGamesConserveEveryCardAndTerminate() {
        repeat(20) { seed ->
            val engine = WarEngine(Random(seed))
            var state = engine.newGame()
            var flips = 0
            while (!state.finished && flips++ < 14000) {
                val previous = state
                val saved = previous.human.toList()
                state = engine.flip(state)
                assertEquals(saved, previous.human)
                val cards = state.human + state.computer + state.pot
                assertEquals(52, cards.size)
                assertEquals(standardDeck().toSet(), cards.toSet())
            }
            assertTrue(state.finished)
        }
    }
}
