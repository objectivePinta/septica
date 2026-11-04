package ro.septica.app

internal data class HomeState(
    val recentGames: List<GameDestination> = emptyList(),
    val favoriteGame: GameDestination? = null,
) {
    fun recordLaunch(game: GameDestination): HomeState =
        copy(recentGames = (listOf(game) + recentGames.filterNot { it == game }).take(5))

    fun toggleFavorite(game: GameDestination): HomeState =
        copy(favoriteGame = if (favoriteGame == game) null else game)
}
