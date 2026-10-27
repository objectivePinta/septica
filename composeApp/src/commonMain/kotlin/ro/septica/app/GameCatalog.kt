package ro.septica.app

internal enum class GameDestination(
    val title: String,
    val description: String,
    val players: String,
) {
    SEPTICA("Șeptică", "Taie cu un 7 și adună așii și decarii.", "2–4 jucători · tu și 1–3 roboți"),
}

internal fun matchingGames(query: String): List<GameDestination> {
    val normalized = query.normalizedForSearch()
    return GameDestination.entries.filter {
        "${it.title} ${it.description} ${it.players}".normalizedForSearch().contains(normalized)
    }
}

private fun String.normalizedForSearch(): String =
    trim().lowercase()
        .replace('ă', 'a')
        .replace('â', 'a')
        .replace('î', 'i')
        .replace('ș', 's')
        .replace('ş', 's')
        .replace('ț', 't')
        .replace('ţ', 't')
