package ro.septica.game

enum class Suit(
    val symbol: String,
    val romanianName: String,
    val isRed: Boolean,
) {
    CLUBS("♣", "treflă", false),
    DIAMONDS("♦", "caro", true),
    HEARTS("♥", "cupă", true),
    SPADES("♠", "pică", false),
}

enum class Rank(
    val symbol: String,
    val romanianName: String,
    val sortOrder: Int,
) {
    SEVEN("7", "șapte", 0),
    EIGHT("8", "opt", 1),
    NINE("9", "nouă", 2),
    TEN("10", "zece", 3),
    JACK("J", "valet", 4),
    QUEEN("Q", "damă", 5),
    KING("K", "rege", 6),
    ACE("A", "as", 7),
}

data class Card(
    val suit: Suit,
    val rank: Rank,
) {
    val id: String = "${suit.name}-${rank.name}"
    val accessibilityLabel: String = "${rank.romanianName} de ${suit.romanianName}"
}

enum class Player {
    HUMAN,
    COMPUTER;

    val opponent: Player
        get() = if (this == HUMAN) COMPUTER else HUMAN
}
