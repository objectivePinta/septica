package ro.septica.coach

import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.game.Suit
import ro.septica.solitaire.*

data class VisibleColumn(val hiddenCount: Int, val cards: List<StandardCard>)
data class SolitaireView(
    val stockCount: Int, val wasteCount: Int, val wasteTop: StandardCard?,
    val columns: List<VisibleColumn>, val foundationTops: List<StandardCard?>,
    val foundationSizes: List<Int>,
) {
    companion object {
        fun from(board: SolitaireBoard) = SolitaireView(board.stock.size, board.waste.size,
            board.waste.lastOrNull(), board.tableau.map { column ->
                VisibleColumn(column.count { !it.faceUp }, column.filter { it.faceUp }.map { it.card })
            }, board.foundations.map { it.lastOrNull() }, board.foundations.map { it.size })
    }
}

private val StandardCard.foundationRank: Int get() = if (rank == StandardRank.ACE) 1 else rank.value

fun solitaireAdvice(view: SolitaireView): Advice {
    if (view.foundationSizes.all { it == 13 }) return Advice("Joc încheiat", "Toate cele patru fundații sunt complete.")
    data class Candidate(val source: SolitaireSource, val target: SolitaireTarget, val card: StandardCard, val score: Int, val reveals: Boolean)
    val candidates = mutableListOf<Candidate>()
    fun consider(source: SolitaireSource, cards: List<StandardCard>, reveals: Boolean) {
        if (cards.isEmpty()) return
        val first = cards.first()
        if (cards.zipWithNext().any { (a, b) -> a.foundationRank != b.foundationRank + 1 || a.suit.isRed == b.suit.isRed }) return
        for (index in 0..3) {
            val top = view.foundationTops[index]
            if (cards.size == 1 && source !is SolitaireSource.Foundation &&
                first.suit == Suit.entries[index] && first.foundationRank == view.foundationSizes[index] + 1 &&
                (top == null || top.suit == first.suit && top.foundationRank + 1 == first.foundationRank)) {
                val safe = first.foundationRank <= 2 || Suit.entries.filter { it.isRed != first.suit.isRed }
                    .all { view.foundationSizes[it.ordinal] >= first.foundationRank - 1 }
                candidates += Candidate(source, SolitaireTarget.Foundation(index), first,
                    (if (reveals) 120 else 0) + if (safe) 90 else 15, reveals)
            }
        }
        for (index in 0..6) {
            if (source is SolitaireSource.Tableau && source.column == index) continue
            val column = view.columns[index]
            val top = column.cards.lastOrNull()
            val empty = column.hiddenCount == 0 && column.cards.isEmpty()
            if (if (empty) first.rank == StandardRank.KING else top != null &&
                top.foundationRank == first.foundationRank + 1 && top.suit.isRed != first.suit.isRed) {
                // Moving an entire exposed king column to an empty column changes nothing.
                if (empty && source is SolitaireSource.Tableau && source.index == 0) continue
                candidates += Candidate(source, SolitaireTarget.Tableau(index), first,
                    (if (reveals) 150 else 0) + cards.size * 3 +
                        (if (source == SolitaireSource.Waste) 35 else 0) -
                        (if (source is SolitaireSource.Foundation) 25 else 0), reveals)
            }
        }
    }
    view.wasteTop?.let { consider(SolitaireSource.Waste, listOf(it), false) }
    view.columns.forEachIndexed { column, pile ->
        pile.cards.indices.forEach { index ->
            consider(SolitaireSource.Tableau(column, pile.hiddenCount + index), pile.cards.drop(index),
                index == 0 && pile.hiddenCount > 0)
        }
    }
    view.foundationTops.forEachIndexed { index, card ->
        card?.let { consider(SolitaireSource.Foundation(index), listOf(it), false) }
    }
    val best = candidates.filter { it.score > 0 }.maxByOrNull { it.score }
    if (best == null) return when {
        view.stockCount > 0 -> Advice("Trage o carte", "Nu găsesc o mutare vizibilă cu prioritate pozitivă. Următoarea carte din pachet este necunoscută.", action = CoachAction.Draw)
        view.wasteCount > 0 -> Advice("Reciclează cărțile trase", "Pachetul este gol; reciclarea este permisă. Nu promit că va debloca jocul.", action = CoachAction.Recycle)
        candidates.isNotEmpty() -> {
            val move = candidates.maxBy { it.score }
            Advice("Mută ${move.card.accessibilityLabel}", "${sourceLabel(move.source)} → ${targetLabel(move.target)}. Retragerea din fundație poate redeschide o continuare; evită repetarea aceleiași mutări.",
                action = CoachAction.Move(move.source, move.target))
        }
        else -> Advice("Nicio mutare utilă vizibilă", "Nu găsesc o mutare utilă și nici pachet de tras. Ignor transferul unei coloane întregi cu rege între două spații goale, care nu schimbă posibilitățile. Poți anula o mutare sau începe alt joc.")
    }
    return Advice("Mută ${best.card.accessibilityLabel}",
        "${sourceLabel(best.source)} → ${targetLabel(best.target)}. " +
            if (best.reveals) "Eliberează o carte acoperită, fără a-i consulta identitatea."
            else "Prioritizez fundațiile sigure (ași, doi sau ranguri susținute de culorile opuse), apoi mobilitatea secvențelor. Căutare locală, nu soluție garantată.",
        action = CoachAction.Move(best.source, best.target))
}

private fun sourceLabel(source: SolitaireSource): String = when (source) {
    SolitaireSource.Waste -> "Cartea trasă"
    is SolitaireSource.Tableau -> "Coloana ${source.column + 1}, cartea ${source.index + 1}"
    is SolitaireSource.Foundation -> "Fundația ${source.index + 1}"
}
private fun targetLabel(target: SolitaireTarget): String = when (target) {
    is SolitaireTarget.Tableau -> "coloana ${target.column + 1}"
    is SolitaireTarget.Foundation -> "fundația ${target.index + 1}"
}
