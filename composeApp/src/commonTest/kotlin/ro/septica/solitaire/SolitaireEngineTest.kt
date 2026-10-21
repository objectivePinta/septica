package ro.septica.solitaire

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.cards.standardDeck
import ro.septica.game.Suit

class SolitaireEngineTest {
    private fun card(rank: StandardRank, suit: Suit = Suit.CLUBS) = StandardCard(suit, rank)
    private fun up(rank: StandardRank, suit: Suit = Suit.CLUBS) = TableauCard(card(rank, suit), true)
    private fun state(
        stock: List<StandardCard> = emptyList(),
        waste: List<StandardCard> = emptyList(),
        columns: List<List<TableauCard>> = emptyList(),
        foundations: List<List<StandardCard>> = emptyList(),
    ) = SolitaireState(SolitaireBoard(stock, waste,
        columns + List(7 - columns.size) { emptyList() },
        foundations + List(4 - foundations.size) { emptyList() }))

    private fun allCards(board: SolitaireBoard) =
        board.stock + board.waste + board.tableau.flatten().map { it.card } + board.foundations.flatten()

    private fun reject(state: SolitaireState, source: SolitaireSource, target: SolitaireTarget) {
        val before = state.copy()
        assertFailsWith<IllegalArgumentException> { SolitaireEngine.move(state, source, target) }
        assertEquals(before, state)
    }

    @Test
    fun dealIsSeededAndContainsEveryCardOnce() {
        val initial = SolitaireEngine.newGame(81)
        assertEquals(initial, SolitaireEngine.newGame(81))
        assertEquals((1..7).toList(), initial.board.tableau.map { it.size })
        assertTrue(initial.board.tableau.all { it.last().faceUp && it.dropLast(1).none { c -> c.faceUp } })
        assertEquals(24, initial.board.stock.size)
        assertTrue(initial.board.waste.isEmpty())
        assertTrue(initial.board.foundations.all { it.isEmpty() })
        assertEquals(52, allCards(initial.board).size)
        assertEquals(standardDeck().toSet(), allCards(initial.board).toSet())
        assertFalse(initial.board.won)
    }

    @Test
    fun drawOneAndUnlimitedRecyclingPreserveOrder() {
        val stock = listOf(card(StandardRank.ACE), card(StandardRank.TWO), card(StandardRank.THREE))
        var current = state(stock = stock)
        repeat(5) {
            stock.reversed().forEach { expected ->
                val before = current
                current = SolitaireEngine.draw(current)
                assertEquals(expected, current.board.waste.last())
                assertEquals(before.board.stock.size - 1, current.board.stock.size)
                assertEquals(before, SolitaireEngine.undo(current))
            }
            assertTrue(current.board.stock.isEmpty())
            val beforeRecycle = current
            current = SolitaireEngine.draw(current)
            assertEquals(stock, current.board.stock)
            assertTrue(current.board.waste.isEmpty())
            assertEquals(beforeRecycle, SolitaireEngine.undo(current))
        }
        assertFailsWith<IllegalArgumentException> { SolitaireEngine.draw(state()) }
    }

    @Test
    fun hiddenCardsAndBadIndicesCannotBeSelectedOrMoved() {
        val initial = state(columns = listOf(listOf(
            TableauCard(card(StandardRank.KING), false), up(StandardRank.QUEEN, Suit.HEARTS),
        )))
        reject(initial, SolitaireSource.Tableau(0, 0), SolitaireTarget.Tableau(1))
        reject(initial, SolitaireSource.Tableau(0, -1), SolitaireTarget.Tableau(1))
        reject(initial, SolitaireSource.Tableau(0, 2), SolitaireTarget.Tableau(1))
        reject(initial, SolitaireSource.Tableau(7, 0), SolitaireTarget.Tableau(1))
        reject(initial, SolitaireSource.Tableau(0, 1), SolitaireTarget.Tableau(7))
    }

    @Test
    fun alternatingDescendingSuffixMovesAsAUnit() {
        val initial = state(columns = listOf(
            listOf(up(StandardRank.QUEEN, Suit.HEARTS), up(StandardRank.JACK)),
            listOf(up(StandardRank.KING)),
        ))
        val moved = SolitaireEngine.move(initial, SolitaireSource.Tableau(0, 0), SolitaireTarget.Tableau(1))
        assertTrue(moved.board.tableau[0].isEmpty())
        assertEquals(listOf(StandardRank.KING, StandardRank.QUEEN, StandardRank.JACK),
            moved.board.tableau[1].map { it.card.rank })
        assertEquals(initial, SolitaireEngine.undo(moved))
        reject(initial, SolitaireSource.Tableau(0, 0), SolitaireTarget.Tableau(0))
    }

    @Test
    fun rejectsSameColorBadRankAndMalformedRun() {
        val sameColor = state(columns = listOf(listOf(up(StandardRank.QUEEN)), listOf(up(StandardRank.KING))))
        reject(sameColor, SolitaireSource.Tableau(0, 0), SolitaireTarget.Tableau(1))
        val skippedRank = state(columns = listOf(listOf(up(StandardRank.JACK, Suit.HEARTS)), listOf(up(StandardRank.KING))))
        reject(skippedRank, SolitaireSource.Tableau(0, 0), SolitaireTarget.Tableau(1))
        val badRun = state(columns = listOf(
            listOf(up(StandardRank.KING), up(StandardRank.QUEEN)),
        ))
        reject(badRun, SolitaireSource.Tableau(0, 0), SolitaireTarget.Tableau(1))
        val badRunRank = state(columns = listOf(
            listOf(up(StandardRank.KING), up(StandardRank.JACK, Suit.HEARTS)),
        ))
        reject(badRunRank, SolitaireSource.Tableau(0, 0), SolitaireTarget.Tableau(1))
    }

    @Test
    fun onlyKingsCanFillEmptyColumnsIncludingRuns() {
        val initial = state(columns = listOf(listOf(up(StandardRank.KING), up(StandardRank.QUEEN, Suit.HEARTS))))
        reject(initial, SolitaireSource.Tableau(0, 1), SolitaireTarget.Tableau(1))
        val moved = SolitaireEngine.move(initial, SolitaireSource.Tableau(0, 0), SolitaireTarget.Tableau(1))
        assertEquals(initial.board.tableau[0], moved.board.tableau[1])
    }

    @Test
    fun aceIsLowAndCannotWrapToKing() {
        val initial = state(waste = listOf(card(StandardRank.ACE, Suit.HEARTS)),
            columns = listOf(listOf(up(StandardRank.TWO))))
        assertEquals(2, SolitaireEngine.move(initial, SolitaireSource.Waste, SolitaireTarget.Tableau(0)).board.tableau[0].size)
        val wrap = state(waste = listOf(card(StandardRank.KING, Suit.HEARTS)),
            columns = listOf(listOf(up(StandardRank.ACE))))
        reject(wrap, SolitaireSource.Waste, SolitaireTarget.Tableau(0))
    }

    @Test
    fun foundationRequiresAceThenAscendingSameSuitSingleCards() {
        val ace = card(StandardRank.ACE)
        val two = card(StandardRank.TWO)
        var current = state(waste = listOf(two, ace))
        reject(current, SolitaireSource.Waste, SolitaireTarget.Foundation(1))
        current = SolitaireEngine.move(current, SolitaireSource.Waste, SolitaireTarget.Foundation(0))
        current = SolitaireEngine.move(current, SolitaireSource.Waste, SolitaireTarget.Foundation(0))
        assertEquals(listOf(ace, two), current.board.foundations[0])
        reject(state(waste = listOf(two)), SolitaireSource.Waste, SolitaireTarget.Foundation(0))
        reject(state(waste = listOf(card(StandardRank.THREE)), foundations = listOf(listOf(ace))),
            SolitaireSource.Waste, SolitaireTarget.Foundation(0))
        reject(state(waste = listOf(card(StandardRank.TWO, Suit.SPADES)), foundations = listOf(listOf(ace))),
            SolitaireSource.Waste, SolitaireTarget.Foundation(0))
        val run = state(columns = listOf(listOf(up(StandardRank.TWO), up(StandardRank.ACE, Suit.HEARTS))))
        reject(run, SolitaireSource.Tableau(0, 0), SolitaireTarget.Foundation(0))
    }

    @Test
    fun wasteSourceCanOnlyMoveTopCard() {
        val ace = card(StandardRank.ACE)
        val king = card(StandardRank.KING)
        val initial = state(waste = listOf(ace, king))
        assertEquals(listOf(king), SolitaireEngine.selectedCards(initial.board, SolitaireSource.Waste))
        reject(initial, SolitaireSource.Waste, SolitaireTarget.Foundation(0))
        val moved = SolitaireEngine.move(initial, SolitaireSource.Waste, SolitaireTarget.Tableau(0))
        assertEquals(listOf(ace), moved.board.waste)
        assertEquals(king, moved.board.tableau[0].single().card)
    }

    @Test
    fun removingLastVisibleCardFlipsExactlyOneHiddenCardAndUndoRestoresIt() {
        val initial = state(columns = listOf(listOf(
            TableauCard(card(StandardRank.FOUR), false),
            TableauCard(card(StandardRank.FIVE), false),
            up(StandardRank.ACE),
        )))
        val moved = SolitaireEngine.move(initial, SolitaireSource.Tableau(0, 2), SolitaireTarget.Foundation(0))
        assertEquals(listOf(false, true), moved.board.tableau[0].map { it.faceUp })
        assertEquals(initial, SolitaireEngine.undo(moved))
        assertFalse(initial.board.tableau[0][1].faceUp)
    }

    @Test
    fun foundationTopCanReturnToTableau() {
        val ace = card(StandardRank.ACE)
        val initial = state(columns = listOf(listOf(up(StandardRank.TWO, Suit.HEARTS))),
            foundations = listOf(listOf(ace)))
        val moved = SolitaireEngine.move(initial, SolitaireSource.Foundation(0), SolitaireTarget.Tableau(0))
        assertTrue(moved.board.foundations[0].isEmpty())
        assertEquals(ace, moved.board.tableau[0].last().card)
        assertEquals(initial, SolitaireEngine.undo(moved))
        reject(initial, SolitaireSource.Foundation(0), SolitaireTarget.Foundation(0))
        reject(initial, SolitaireSource.Foundation(0), SolitaireTarget.Tableau(1))
    }

    @Test
    fun undoRestoresEntireMultiActionHistoryIncludingRecycleAndFlip() {
        val initial = state(stock = listOf(card(StandardRank.KING, Suit.HEARTS)),
            columns = listOf(listOf(TableauCard(card(StandardRank.THREE), false), up(StandardRank.ACE))))
        val snapshots = mutableListOf(initial)
        snapshots += SolitaireEngine.draw(snapshots.last())
        snapshots += SolitaireEngine.draw(snapshots.last())
        snapshots += SolitaireEngine.move(snapshots.last(), SolitaireSource.Tableau(0, 1), SolitaireTarget.Foundation(0))
        var current = snapshots.last()
        snapshots.dropLast(1).reversed().forEach { expected ->
            current = SolitaireEngine.undo(current)
            assertEquals(expected, current)
        }
        assertFailsWith<IllegalArgumentException> { SolitaireEngine.undo(current) }
    }

    @Test
    fun finalKingCompletesVictory() {
        val ranks = listOf(StandardRank.ACE) + StandardRank.entries.filter { it != StandardRank.ACE }
        val full = Suit.entries.map { suit -> ranks.map { card(it, suit) } }
        val lastKing = full.last().last()
        val initial = state(waste = listOf(lastKing), foundations = full.dropLast(1) + listOf(full.last().dropLast(1)))
        assertFalse(initial.board.won)
        val won = SolitaireEngine.move(initial, SolitaireSource.Waste, SolitaireTarget.Foundation(3))
        assertTrue(won.board.won)
        assertEquals(52, allCards(won.board).size)
        assertEquals(initial, SolitaireEngine.undo(won))
        reject(won, SolitaireSource.Foundation(3), SolitaireTarget.Tableau(0))
    }

    @Test
    fun emptyAndOutOfRangeSourcesAndTargetsFailWithoutMutation() {
        val initial = state()
        reject(initial, SolitaireSource.Waste, SolitaireTarget.Tableau(0))
        reject(initial, SolitaireSource.Foundation(0), SolitaireTarget.Tableau(0))
        reject(initial, SolitaireSource.Foundation(-1), SolitaireTarget.Tableau(0))
        val ace = state(waste = listOf(card(StandardRank.ACE)))
        reject(ace, SolitaireSource.Waste, SolitaireTarget.Foundation(4))
    }

    @Test
    fun seededLegalMoveExplorationConservesEveryCardAndUndoIsExact() {
        repeat(6) { seed ->
            val random = Random(seed)
            var current = SolitaireEngine.newGame(seed)
            repeat(150) {
                val before = current
                val sources = listOf(SolitaireSource.Waste) +
                    (0..3).map { SolitaireSource.Foundation(it) } +
                    current.board.tableau.flatMapIndexed { column, pile ->
                        pile.indices.map { SolitaireSource.Tableau(column, it) }
                    }
                val targets = (0..6).map { SolitaireTarget.Tableau(it) } +
                    (0..3).map { SolitaireTarget.Foundation(it) }
                val moves = sources.flatMap { source ->
                    targets.mapNotNull { target ->
                        try { SolitaireEngine.move(current, source, target) }
                        catch (_: IllegalArgumentException) { null }
                    }
                }.toMutableList()
                if (!current.board.won && (current.board.stock.isNotEmpty() || current.board.waste.isNotEmpty())) {
                    moves += SolitaireEngine.draw(current)
                }
                if (moves.isNotEmpty()) {
                    current = moves[random.nextInt(moves.size)]
                    assertEquals(before, SolitaireEngine.undo(current))
                }
                assertEquals(52, allCards(current.board).size)
                assertEquals(standardDeck().toSet(), allCards(current.board).toSet())
                current.board.tableau.forEach { pile ->
                    val firstVisible = pile.indexOfFirst { it.faceUp }
                    assertTrue(pile.isEmpty() || firstVisible >= 0)
                    if (firstVisible >= 0) {
                        assertTrue(pile.drop(firstVisible).all { it.faceUp })
                        SolitaireEngine.selectedCards(current.board,
                            SolitaireSource.Tableau(current.board.tableau.indexOf(pile), firstVisible))
                    }
                }
            }
        }
    }
}
