package ro.septica.game

enum class SepticaPlayer(val label: String) {
    HUMAN("Tu"),
    COMPUTER("Ion"),
    COMPUTER_2("Costică"),
    COMPUTER_3("Vasile"),
}

enum class GamePhase {
    PLAYING,
    TRICK_REVEAL,
    FINISHED,
}

data class PlayedCard(
    val player: SepticaPlayer,
    val card: Card,
)

data class TrickSummary(
    val winner: SepticaPlayer,
    val points: Int,
    val cards: List<Card>,
)

data class GameState(
    val hands: Map<SepticaPlayer, List<Card>>,
    val deck: List<Card>,
    val turn: SepticaPlayer?,
    val trickLeader: SepticaPlayer,
    val table: List<PlayedCard> = emptyList(),
    val lastCutBy: SepticaPlayer? = null,
    val phase: GamePhase = GamePhase.PLAYING,
    val trickWinner: SepticaPlayer? = null,
    val trickPoints: Int = 0,
    val scores: Map<SepticaPlayer, Int> = emptyMap(),
    val tricks: Map<SepticaPlayer, Int> = emptyMap(),
    val completedTricks: Int = 0,
    val lastTrick: TrickSummary? = null,
) {
    val players: List<SepticaPlayer>
        get() = SepticaPlayer.entries.filter { it in hands }

    val humanHand: List<Card>
        get() = handFor(SepticaPlayer.HUMAN)

    val targetRank: Rank?
        get() = table.firstOrNull()?.card?.rank

    val gameWinners: List<SepticaPlayer>
        get() {
            if (phase != GamePhase.FINISHED) return emptyList()
            val bestScore = players.maxOf(::scoreFor)
            return players.filter { scoreFor(it) == bestScore }
        }

    val gameWinner: SepticaPlayer?
        get() = gameWinners.singleOrNull()

    fun handFor(player: SepticaPlayer): List<Card> = hands.getValue(player)

    fun scoreFor(player: SepticaPlayer): Int = scores[player] ?: 0

    fun orderFrom(player: SepticaPlayer): List<SepticaPlayer> {
        val index = players.indexOf(player)
        require(index >= 0) { "The player is not in this game." }
        return players.drop(index) + players.take(index)
    }
}
