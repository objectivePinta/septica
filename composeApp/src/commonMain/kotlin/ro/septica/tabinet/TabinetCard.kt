package ro.septica.tabinet

import ro.septica.game.Suit

enum class TabinetRank(
    val symbol: String,
    val values: List<Int>,
    val romanianName: String,
) {
    TWO("2", listOf(2), "doi"),
    THREE("3", listOf(3), "trei"),
    FOUR("4", listOf(4), "patru"),
    FIVE("5", listOf(5), "cinci"),
    SIX("6", listOf(6), "șase"),
    SEVEN("7", listOf(7), "șapte"),
    EIGHT("8", listOf(8), "opt"),
    NINE("9", listOf(9), "nouă"),
    TEN("10", listOf(10), "zece"),
    JACK("J", listOf(12), "valet"),
    QUEEN("Q", listOf(13), "damă"),
    KING("K", listOf(14), "rege"),
    ACE("A", listOf(1, 11), "as"),
}

data class TabinetCard(
    val suit: Suit,
    val rank: TabinetRank,
) {
    val id: String = "${suit.name}-${rank.name}"
    val accessibilityLabel: String = "${rank.romanianName} de ${suit.romanianName}"
    val points: Int
        get() = when {
            rank == TabinetRank.TEN && suit == Suit.DIAMONDS -> 2
            rank == TabinetRank.TWO && suit == Suit.CLUBS -> 1
            rank in TabinetRank.TEN..TabinetRank.ACE -> 1
            else -> 0
        }
}
