package ro.septica.macao

import ro.septica.game.Suit

enum class MacaoRank(
    val symbol: String,
    val romanianName: String,
) {
    TWO("2", "doi"),
    THREE("3", "trei"),
    FOUR("4", "patru"),
    FIVE("5", "cinci"),
    SIX("6", "șase"),
    SEVEN("7", "șapte"),
    EIGHT("8", "opt"),
    NINE("9", "nouă"),
    TEN("10", "zece"),
    JACK("J", "valet"),
    QUEEN("Q", "damă"),
    KING("K", "rege"),
    ACE("A", "as"),
    BLACK_JOKER("JOKER", "joker negru"),
    RED_JOKER("JOKER", "joker roșu"),
}

data class MacaoCard(
    val suit: Suit?,
    val rank: MacaoRank,
) {
    init {
        val isJoker = rank == MacaoRank.BLACK_JOKER || rank == MacaoRank.RED_JOKER
        require((suit == null) == isJoker) { "Only jokers must have no suit." }
    }

    val id: String = "${suit?.name ?: "JOKER"}-${rank.name}"
    val accessibilityLabel: String =
        if (suit == null) rank.romanianName else "${rank.romanianName} de ${suit.romanianName}"
}
