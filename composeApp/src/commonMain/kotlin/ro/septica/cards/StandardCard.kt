package ro.septica.cards

import ro.septica.game.Suit

enum class StandardRank(val symbol: String, val romanianName: String, val value: Int) {
    TWO("2", "doi", 2),
    THREE("3", "trei", 3),
    FOUR("4", "patru", 4),
    FIVE("5", "cinci", 5),
    SIX("6", "șase", 6),
    SEVEN("7", "șapte", 7),
    EIGHT("8", "opt", 8),
    NINE("9", "nouă", 9),
    TEN("10", "zece", 10),
    JACK("J", "valet", 11),
    QUEEN("Q", "damă", 12),
    KING("K", "rege", 13),
    ACE("A", "as", 14),
}

data class StandardCard(val suit: Suit, val rank: StandardRank) {
    val id: String get() = "${suit.name}-${rank.name}"
    val accessibilityLabel: String get() = "${rank.romanianName} de ${suit.romanianName}"
}

fun standardDeck(): List<StandardCard> =
    Suit.entries.flatMap { suit -> StandardRank.entries.map { StandardCard(suit, it) } }
