package ro.septica.app

internal enum class GameDestination(
    val title: String,
    val description: String,
    val players: String,
) {
    SEPTICA("Șeptică", "Taie cu un 7 și adună așii și decarii.", "2–4 jucători · tu și 1–3 roboți"),
    MACAO("Macao", "Scapă de cărți. Ași, penalizări și doi jokeri.", "Tu și un adversar"),
    TABINET("Tabinet", "Potrivește valori și sume ca să golești masa.", "Tu și un adversar"),
    WAR("Război", "Cartea mai mare câștigă. Egalitatea pornește războiul.", "Tu și un adversar"),
    OLD_MAID("Popa prostu’", "Formează perechi și evită să rămâi cu ultimul popă.", "Tu și un adversar"),
    BLACKJACK("Blackjack", "Apropie-te de 21 fără să depășești. Doar jetoane virtuale.", "Tu și dealerul"),
    WHIST("Whist", "Licitează câte mâini vei câștiga și respectă estimarea.", "Tu și trei adversari"),
    RENTZ("Rentz", "Opt contracte: evită penalizările și construiește suite.", "Tu și trei adversari"),
    SOLITAIRE("Solitaire", "Klondike: construiește cele patru fundații de la As la Rege.", "Un singur jucător"),
    SIXTY_SIX("Șaizeci și șase", "Atuuri, căsătorii și 66 de puncte. Meci până la 7.", "Tu și un adversar"),
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
