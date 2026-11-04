package ro.septica.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeStateTest {
    @Test
    fun recordLaunchMovesGameToFrontAndRemovesDuplicates() {
        val game = GameDestination.SEPTICA
        val state = HomeState(recentGames = listOf(game, game), favoriteGame = null)

        assertEquals(listOf(game), state.recordLaunch(game).recentGames)
    }

    @Test
    fun toggleFavoriteSetsAndClearsFavorite() {
        val game = GameDestination.SEPTICA

        assertEquals(game, HomeState().toggleFavorite(game).favoriteGame)
        assertNull(HomeState(favoriteGame = game).toggleFavorite(game).favoriteGame)
    }
}
