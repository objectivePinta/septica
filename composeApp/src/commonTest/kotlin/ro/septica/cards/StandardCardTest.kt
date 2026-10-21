package ro.septica.cards

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandardCardTest {
    @Test
    fun deckContainsAllFiftyTwoUniqueCards() {
        val deck = standardDeck()
        assertEquals(52, deck.size)
        assertEquals(52, deck.map { it.id }.toSet().size)
        assertTrue(deck.groupBy { it.suit }.values.all { it.size == 13 })
        assertEquals((2..14).toList(), StandardRank.entries.map { it.value })
    }

    @Test
    fun ranksUseInternationalSymbols() {
        assertEquals(
            listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"),
            StandardRank.entries.map { it.symbol },
        )
    }
}
