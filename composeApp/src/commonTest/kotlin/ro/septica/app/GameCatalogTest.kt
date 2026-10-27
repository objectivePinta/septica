package ro.septica.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameCatalogTest {
    @Test
    fun onlySepticaIsAvailable() {
        assertEquals(1, GameDestination.entries.size)
        assertEquals(GameDestination.entries.toList(), matchingGames(" "))
        assertTrue(GameDestination.entries.all { it.title.isNotBlank() && it.description.isNotBlank() })
    }

    @Test
    fun searchIgnoresCaseAndRomanianDiacritics() {
        assertEquals(listOf(GameDestination.SEPTICA), matchingGames("septica"))
        assertTrue(matchingGames("unavailable-game").isEmpty())
    }
}
