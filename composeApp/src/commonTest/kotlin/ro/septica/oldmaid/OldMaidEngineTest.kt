package ro.septica.oldmaid

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

class OldMaidEngineTest {
    private val deck = standardDeck().filterNot { it.rank == StandardRank.KING && it.suit == Suit.CLUBS }

    @Test fun fourOfRankAndThreeKings() {
        val four = removePairs(standardDeck().filter { it.rank == StandardRank.ACE })
        assertEquals(0, four.hand.size)
        assertEquals(4, four.discarded.size)
        val kings = removePairs(deck.filter { it.rank == StandardRank.KING })
        assertEquals(1, kings.hand.size)
        assertEquals(2, kings.discarded.size)
    }

    @Test fun initialCompletion() {
        val human = deck.groupBy { it.rank }.values.flatMap { it.take(2) }
        val computer = deck.filterNot { it in human }
        val ordered = mutableListOf<StandardCard>()
        human.forEachIndexed { i, card ->
            ordered.add(card)
            computer.getOrNull(i)?.let { ordered.add(it) }
        }
        val state = OldMaidEngine(Random(1)).deal(ordered)
        assertEquals(Player.HUMAN, state.winner)
        assertEquals(StandardRank.KING, state.computer.single().rank)
        assertEquals(50, state.discarded.size)
        assertFailsWith<IllegalArgumentException> { OldMaidEngine().draw(state, Player.HUMAN, 0) }
    }

    @Test fun invalidPositionsAndWrongPlayer() {
        val engine = OldMaidEngine(Random(5))
        val state = engine.newGame()
        assertTrue(!state.finished)
        assertFailsWith<IllegalArgumentException> { engine.draw(state, Player.HUMAN, -1) }
        assertFailsWith<IllegalArgumentException> { engine.draw(state, Player.HUMAN, state.computer.size) }
        assertFailsWith<IllegalArgumentException> { engine.draw(state, Player.COMPUTER, 0) }
        assertFailsWith<IllegalArgumentException> { engine.computerMove(state) }
        assertEquals(state, OldMaidEngine(Random(5)).newGame())
    }

    @Test fun hundredSeededGamesConserveCardsAndFinish() {
        repeat(100) { seed ->
            val engine = OldMaidEngine(Random(seed))
            val selection = Random(seed + 1000)
            var state = engine.newGame()
            var moves = 0
            while (!state.finished && moves++ < 10000) {
                val previous = state
                val saved = previous.human.toList()
                state = if (state.turn == Player.HUMAN)
                    engine.draw(state, Player.HUMAN, selection.nextInt(state.computer.size))
                else engine.computerMove(state)
                assertEquals(saved, previous.human)
                val all = state.human + state.computer + state.discarded
                assertEquals(51, all.size)
                assertEquals(deck.toSet(), all.toSet())
                assertEquals(state.human.size, state.human.map { it.rank }.toSet().size)
                assertEquals(state.computer.size, state.computer.map { it.rank }.toSet().size)
            }
            assertTrue(state.finished, "Seed $seed did not terminate")
            assertEquals(StandardRank.KING, (state.human + state.computer).single().rank)
            assertEquals(50, state.discarded.size)
        }
    }
}
