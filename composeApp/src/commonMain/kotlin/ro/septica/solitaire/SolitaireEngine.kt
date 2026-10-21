package ro.septica.solitaire

import kotlin.random.Random
import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.cards.standardDeck
import ro.septica.game.Suit

data class TableauCard(val card: StandardCard, val faceUp: Boolean)

/** Stock, waste and foundation tops are at the end of their lists. */
data class SolitaireBoard(
    val stock: List<StandardCard>,
    val waste: List<StandardCard>,
    val tableau: List<List<TableauCard>>,
    val foundations: List<List<StandardCard>>,
) {
    init {
        require(tableau.size == 7)
        require(foundations.size == 4)
    }

    val won: Boolean get() = foundations.all { pile ->
        pile.size == 13 && pile.map { it.rankValue } == (1..13).toList() &&
            pile.all { it.suit == pile.first().suit }
    } && foundations.map { it.first().suit }.distinct().size == 4
}

data class SolitaireState(
    val board: SolitaireBoard,
    val history: List<SolitaireBoard> = emptyList(),
)

sealed interface SolitaireSource {
    data object Waste : SolitaireSource
    data class Tableau(val column: Int, val index: Int) : SolitaireSource
    data class Foundation(val index: Int) : SolitaireSource
}

sealed interface SolitaireTarget {
    data class Tableau(val column: Int) : SolitaireTarget
    data class Foundation(val index: Int) : SolitaireTarget
}

private val StandardCard.rankValue: Int
    get() = if (rank == StandardRank.ACE) 1 else rank.value

object SolitaireEngine {
    fun newGame(seed: Int): SolitaireState {
        val deck = standardDeck().shuffled(Random(seed))
        var dealt = 0
        val columns = (1..7).map { count ->
            List(count) { index -> TableauCard(deck[dealt++], index == count - 1) }
        }
        return SolitaireState(SolitaireBoard(deck.drop(dealt), emptyList(), columns, List(4) { emptyList() }))
    }

    fun draw(state: SolitaireState): SolitaireState {
        val board = state.board
        require(!board.won) { "Jocul este încheiat. Începe un joc nou." }
        val next = if (board.stock.isNotEmpty()) {
            board.copy(stock = board.stock.dropLast(1), waste = board.waste + board.stock.last())
        } else {
            require(board.waste.isNotEmpty()) { "Pachetul și teancul de cărți trase sunt goale." }
            board.copy(stock = board.waste.reversed(), waste = emptyList())
        }
        return commit(state, next)
    }

    fun selectedCards(board: SolitaireBoard, source: SolitaireSource): List<StandardCard> =
        when (source) {
            SolitaireSource.Waste -> {
                require(board.waste.isNotEmpty()) { "Nu există nicio carte trasă." }
                listOf(board.waste.last())
            }
            is SolitaireSource.Foundation -> {
                require(source.index in 0..3) { "Fundație inexistentă." }
                require(board.foundations[source.index].isNotEmpty()) { "Fundația este goală." }
                listOf(board.foundations[source.index].last())
            }
            is SolitaireSource.Tableau -> {
                require(source.column in 0..6) { "Coloană inexistentă." }
                val pile = board.tableau[source.column]
                require(source.index in pile.indices) { "Selectează o carte din coloană." }
                val run = pile.drop(source.index)
                require(run.all { it.faceUp }) { "Cărțile ascunse nu pot fi mutate." }
                val cards = run.map { it.card }
                require(cards.zipWithNext().all { (upper, lower) ->
                    upper.rankValue == lower.rankValue + 1 && upper.suit.isRed != lower.suit.isRed
                }) { "Secvența trebuie să fie descrescătoare, cu culori alternate." }
                cards
            }
        }

    fun move(state: SolitaireState, source: SolitaireSource, target: SolitaireTarget): SolitaireState {
        val board = state.board
        require(!board.won) { "Jocul este încheiat. Începe un joc nou." }
        val cards = selectedCards(board, source)
        val first = cards.first()
        when (target) {
            is SolitaireTarget.Tableau -> {
                require(target.column in 0..6) { "Coloană inexistentă." }
                require(source !is SolitaireSource.Tableau || source.column != target.column) {
                    "Alege o altă coloană."
                }
                val top = board.tableau[target.column].lastOrNull()
                require(if (top == null) first.rank == StandardRank.KING else
                    top.faceUp && top.card.rankValue == first.rankValue + 1 &&
                        top.card.suit.isRed != first.suit.isRed
                ) { "Pune un rege pe coloana goală sau o carte cu o valoare mai mică și culoare opusă." }
            }
            is SolitaireTarget.Foundation -> {
                require(target.index in 0..3) { "Fundație inexistentă." }
                require(source !is SolitaireSource.Foundation || source.index != target.index) {
                    "Alege o altă destinație."
                }
                val pile = board.foundations[target.index]
                require(cards.size == 1 && first.suit == Suit.entries[target.index] &&
                    first.rankValue == pile.size + 1 &&
                    (pile.isEmpty() || pile.last().suit == first.suit &&
                        pile.last().rankValue + 1 == first.rankValue)
                ) { "Fundația primește o singură carte de aceeași suită, de la as până la rege." }
            }
        }
        var next = when (source) {
            SolitaireSource.Waste -> board.copy(waste = board.waste.dropLast(1))
            is SolitaireSource.Foundation -> board.copy(
                foundations = board.foundations.replace(source.index, board.foundations[source.index].dropLast(1)),
            )
            is SolitaireSource.Tableau -> {
                val remaining = board.tableau[source.column].take(source.index)
                val flipped = if (remaining.isEmpty()) remaining else
                    remaining.dropLast(1) + remaining.last().copy(faceUp = true)
                board.copy(tableau = board.tableau.replace(source.column, flipped))
            }
        }
        next = when (target) {
            is SolitaireTarget.Tableau -> next.copy(
                tableau = next.tableau.replace(target.column,
                    next.tableau[target.column] + cards.map { TableauCard(it, true) }),
            )
            is SolitaireTarget.Foundation -> next.copy(
                foundations = next.foundations.replace(target.index, next.foundations[target.index] + first),
            )
        }
        return commit(state, next)
    }

    fun undo(state: SolitaireState): SolitaireState {
        require(state.history.isNotEmpty()) { "Nu există mutări de anulat." }
        return SolitaireState(state.history.last(), state.history.dropLast(1))
    }

    private fun commit(state: SolitaireState, board: SolitaireBoard) =
        SolitaireState(board, state.history + state.board)

    private fun <T> List<T>.replace(index: Int, value: T): List<T> =
        mapIndexed { current, old -> if (current == index) value else old }
}
