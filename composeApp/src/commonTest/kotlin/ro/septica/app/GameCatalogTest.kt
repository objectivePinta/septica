package ro.septica.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameCatalogTest {
    @Test
    fun allTenGamesAreAvailable() {
        assertEquals(10, GameDestination.entries.size)
        assertEquals(GameDestination.entries.toList(), matchingGames(" "))
        assertTrue(GameDestination.entries.all { it.title.isNotBlank() && it.description.isNotBlank() })
    }

    @Test
    fun searchIgnoresCaseAndRomanianDiacritics() {
        assertEquals(listOf(GameDestination.WAR), matchingGames(" RAZBOI "))
        assertEquals(listOf(GameDestination.SIXTY_SIX), matchingGames("șaizeci"))
        assertEquals(listOf(GameDestination.SEPTICA), matchingGames("septica"))
        assertEquals(listOf(GameDestination.SOLITAIRE), matchingGames("klondike"))
        assertTrue(matchingGames("unavailable-game").isEmpty())
    }
}
