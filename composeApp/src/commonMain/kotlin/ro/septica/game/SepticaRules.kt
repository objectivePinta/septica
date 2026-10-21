package ro.septica.game

object SepticaRules {
    const val INITIAL_HAND_SIZE = 4
    const val TOTAL_POINT_CARDS = 8

    fun fullDeck(playerCount: Int = 2): List<Card> {
        require(playerCount in 2..4) { "Septica supports two to four players." }
        return enumValues<Suit>().flatMap { suit ->
            enumValues<Rank>().map { rank -> Card(suit, rank) }
        }.filterNot { playerCount == 3 && it.rank == Rank.EIGHT && it.suit.isRed }
    }

    fun isUniversalCut(card: Card, playerCount: Int): Boolean =
        card.rank == Rank.SEVEN || (playerCount == 3 && card.rank == Rank.EIGHT)

    fun isCut(targetRank: Rank, card: Card, playerCount: Int = 2): Boolean =
        isUniversalCut(card, playerCount) || card.rank == targetRank

    fun pointValue(card: Card): Int =
        if (card.rank == Rank.ACE || card.rank == Rank.TEN) 1 else 0

    fun points(cards: Iterable<Card>): Int = cards.sumOf(::pointValue)
}
