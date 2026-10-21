package ro.septica.tabinet

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ro.septica.game.Player
import ro.septica.game.Suit

class TabinetEngineTest {
    private val engine = TabinetEngine(Random(7))
    private val spare = card(TabinetRank.KING, Suit.SPADES)

    @Test
    fun initialDealHasFiftyTwoUniqueCardsAndTwentyTwoPoints() {
        repeat(20) { seed ->
            val state = TabinetEngine(Random(seed)).newGame()
            assertEquals(6, state.humanHand.size)
            assertEquals(6, state.computerHand.size)
            assertEquals(36, state.deck.size)
            assertEquals(4, state.table.size)
            assertEquals(fullDeck().toSet(), state.allCards().toSet())
            assertEquals(52, state.allCards().size)
            assertEquals(22, state.allCards().sumOf { it.points })
            assertEquals(Player.HUMAN, state.turn)
            assertEquals(0, state.humanScore)
            assertEquals(0, state.computerScore)
            assertFalse(state.finished)
            assertNull(state.gameWinner)
            assertNull(state.lastCapturedBy)
        }
        assertEquals(TabinetEngine(Random(7)).newGame(), TabinetEngine(Random(7)).newGame())
    }

    @Test
    fun ranksLabelsAndSpecialCardPoints() {
        assertEquals(
            (2..10).map { listOf(it) } + listOf(listOf(12), listOf(13), listOf(14), listOf(1, 11)),
            TabinetRank.entries.map { it.values },
        )
        assertEquals(
            listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"),
            TabinetRank.entries.map { it.symbol },
        )
        val ace = card(TabinetRank.ACE, Suit.HEARTS)
        assertEquals("HEARTS-ACE", ace.id)
        assertEquals("as de cupă", ace.accessibilityLabel)
        assertEquals(1, ace.points)
        assertEquals(2, card(TabinetRank.TEN, Suit.DIAMONDS).points)
        assertEquals(1, card(TabinetRank.TWO, Suit.CLUBS).points)
        assertEquals(0, card(TabinetRank.TWO, Suit.SPADES).points)
        assertEquals(1, card(TabinetRank.JACK).points)
        assertEquals(1, card(TabinetRank.QUEEN).points)
        assertEquals(1, card(TabinetRank.KING).points)
        assertEquals(1, card(TabinetRank.TEN).points)
    }

    @Test
    fun captureRequiresAnExactPartitionIntoNonoverlappingGroups() {
        val nine = card(TabinetRank.NINE, Suit.HEARTS)
        assertTrue(engine.canCapture(nine, listOf(card(TabinetRank.NINE))))
        assertTrue(engine.canCapture(nine, listOf(card(TabinetRank.TWO), card(TabinetRank.SEVEN))))
        assertTrue(engine.canCapture(nine, listOf(
            card(TabinetRank.NINE), card(TabinetRank.TWO), card(TabinetRank.SEVEN),
            card(TabinetRank.THREE), card(TabinetRank.SIX),
        )))
        assertFalse(engine.canCapture(nine, emptyList()))
        assertFalse(engine.canCapture(nine, listOf(card(TabinetRank.NINE), card(TabinetRank.NINE))))
        assertFalse(engine.canCapture(nine, listOf(
            card(TabinetRank.TWO), card(TabinetRank.THREE), card(TabinetRank.FOUR), card(TabinetRank.SEVEN),
        )))
        assertFalse(engine.canCapture(card(TabinetRank.TEN), listOf(
            card(TabinetRank.SIX), card(TabinetRank.SIX, Suit.HEARTS), card(TabinetRank.EIGHT),
        )))
        assertFalse(engine.canCapture(nine, listOf(card(TabinetRank.KING))))
        assertTrue(engine.canCapture(card(TabinetRank.JACK), listOf(card(TabinetRank.FIVE), card(TabinetRank.SEVEN))))
    }

    @Test
    fun acesCanUseIndependentValuesWithinAndAcrossGroups() {
        val ace = card(TabinetRank.ACE)
        val otherAce = card(TabinetRank.ACE, Suit.DIAMONDS)
        assertTrue(engine.canCapture(card(TabinetRank.THREE), listOf(ace, card(TabinetRank.TWO))))
        assertTrue(engine.canCapture(card(TabinetRank.QUEEN), listOf(ace, card(TabinetRank.TWO))))
        assertTrue(engine.canCapture(card(TabinetRank.KING), listOf(ace, otherAce, card(TabinetRank.TWO))))
        assertTrue(engine.canCapture(card(TabinetRank.ACE, Suit.HEARTS), listOf(
            ace, card(TabinetRank.FIVE), card(TabinetRank.SIX),
        )))
        assertTrue(engine.canCapture(card(TabinetRank.ACE, Suit.HEARTS), listOf(
            ace, otherAce, card(TabinetRank.FIVE), card(TabinetRank.SIX),
            card(TabinetRank.THREE), card(TabinetRank.EIGHT),
        )))
        assertFalse(engine.canCapture(card(TabinetRank.ACE), listOf(card(TabinetRank.JACK))))
        assertFalse(engine.canCapture(card(TabinetRank.ACE), listOf(card(TabinetRank.TWO))))
    }

    @Test
    fun capturesMoveOnlyTheSelectedCardsAndDiscardIsAlwaysAllowed() {
        val nine = card(TabinetRank.NINE, Suit.HEARTS)
        val two = card(TabinetRank.TWO)
        val seven = card(TabinetRank.SEVEN)
        val leftover = card(TabinetRank.QUEEN)
        val state = testState(listOf(nine), listOf(two, seven, leftover))
        val captured = engine.playCard(state, Player.HUMAN, nine.id, listOf(two.id, seven.id))
        assertEquals(listOf(leftover), captured.table)
        assertEquals(listOf(nine, two, seven), captured.humanCaptured)
        assertEquals(Player.HUMAN, captured.lastCapturedBy)
        assertEquals(Player.COMPUTER, captured.turn)
        assertEquals(0, captured.humanSweeps)
        assertEquals(1, captured.humanCardPoints)
        assertEquals(0, captured.humanMajorityBonus)
        assertConserved(state, captured)
        val discarded = engine.playCard(state, Player.HUMAN, nine.id)
        assertEquals(state.table + nine, discarded.table)
        assertTrue(discarded.humanCaptured.isEmpty())
        assertNull(discarded.lastCapturedBy)
        assertConserved(state, discarded)
        assertEquals(listOf(nine), state.humanHand)
        assertEquals(listOf(two, seven, leftover), state.table)
    }

    @Test
    fun sweepsAccumulateForBothPlayersWithoutCancellation() {
        val nine = card(TabinetRank.NINE, Suit.HEARTS)
        val tableNine = card(TabinetRank.NINE)
        val state = testState(listOf(nine), listOf(tableNine)).copy(humanSweeps = 2, computerSweeps = 4)
        val human = engine.playCard(state, Player.HUMAN, nine.id, listOf(tableNine.id))
        assertTrue(human.table.isEmpty())
        assertEquals(3, human.humanSweeps)
        assertEquals(4, human.computerSweeps)
        assertEquals(3, human.humanScore)
        val computerState = testState(
            humanHand = listOf(spare),
            table = listOf(tableNine),
            computerHand = listOf(nine),
        ).copy(turn = Player.COMPUTER, humanSweeps = 3, computerSweeps = 4)
        val computer = engine.playCard(computerState, Player.COMPUTER, nine.id, listOf(tableNine.id))
        assertEquals(3, computer.humanSweeps)
        assertEquals(5, computer.computerSweeps)
        assertEquals(Player.COMPUTER, computer.lastCapturedBy)
    }

    @Test
    fun finalMoveCaptureAndCleanupNeverAwardSweeps() {
        val nine = card(TabinetRank.NINE, Suit.HEARTS)
        val tableNine = card(TabinetRank.NINE)
        val state = testState(listOf(nine), listOf(tableNine), computerHand = emptyList())
            .copy(humanSweeps = 2, computerSweeps = 1)
        val finished = engine.playCard(state, Player.HUMAN, nine.id, listOf(tableNine.id))
        assertTrue(finished.finished)
        assertNull(finished.turn)
        assertTrue(finished.table.isEmpty())
        assertEquals(2, finished.humanSweeps)
        assertEquals(1, finished.computerSweeps)
        assertEquals(3, finished.humanMajorityBonus)
        assertEquals(5, finished.humanScore)
        assertConserved(state, finished)
        val cleanup = engine.playCard(state.copy(lastCapturedBy = Player.HUMAN), Player.HUMAN, nine.id)
        assertEquals(2, cleanup.humanSweeps)
        assertEquals(1, cleanup.computerSweeps)
        assertEquals(setOf(nine, tableNine), cleanup.humanCaptured.toSet())
        assertTrue(cleanup.table.isEmpty())
    }

    @Test
    fun finalLeftoversGoToLastCapturerOrDealerWhenNobodyCaptured() {
        val five = card(TabinetRank.FIVE)
        val ace = card(TabinetRank.ACE)
        for (lastCapturer in listOf(null, Player.HUMAN, Player.COMPUTER)) {
            val state = testState(listOf(five), listOf(ace), computerHand = emptyList())
                .copy(lastCapturedBy = lastCapturer)
            val finished = engine.playCard(state, Player.HUMAN, five.id)
            val collector = lastCapturer ?: Player.COMPUTER
            assertEquals(2, if (collector == Player.HUMAN) finished.humanCaptured.size else finished.computerCaptured.size)
            assertEquals(4, if (collector == Player.HUMAN) finished.humanScore else finished.computerScore)
            assertEquals(collector, finished.gameWinner)
            assertEquals(lastCapturer, finished.lastCapturedBy)
            assertEquals(0, finished.humanSweeps + finished.computerSweeps)
            assertTrue(finished.table.isEmpty())
            assertTrue(finished.finished)
            assertConserved(state, finished)
        }
        val nine = card(TabinetRank.NINE, Suit.HEARTS)
        val tableNine = card(TabinetRank.NINE)
        val lastCapture = testState(listOf(nine), listOf(tableNine, ace), computerHand = emptyList())
            .copy(lastCapturedBy = Player.COMPUTER)
        val result = engine.playCard(lastCapture, Player.HUMAN, nine.id, listOf(tableNine.id))
        assertEquals(Player.HUMAN, result.lastCapturedBy)
        assertEquals(setOf(nine, tableNine, ace), result.humanCaptured.toSet())
        assertTrue(result.computerCaptured.isEmpty())
    }

    @Test
    fun batchesRefillOnlyAfterBothHandsEmptyAndNeverAddTableCards() {
        var state = engine.newGame()
        val original = state
        repeat(48) { index ->
            val player = if (index % 2 == 0) Player.HUMAN else Player.COMPUTER
            assertEquals(player, state.turn)
            val hand = if (player == Player.HUMAN) state.humanHand else state.computerHand
            val previous = state
            state = engine.playCard(state, player, hand.first().id)
            if ((index + 1) % 12 == 0 && index < 47) {
                assertEquals(6, state.humanHand.size)
                assertEquals(6, state.computerHand.size)
                assertEquals(36 - (index + 1), state.deck.size)
                assertEquals(Player.HUMAN, state.turn)
            } else {
                assertEquals(previous.deck, state.deck)
            }
            if (index < 47) assertEquals(previous.table.size + 1, state.table.size)
            if (index == 10) {
                assertTrue(state.humanHand.isEmpty())
                assertEquals(1, state.computerHand.size)
                assertEquals(36, state.deck.size)
            }
            assertConserved(original, state)
        }
        assertTrue(state.finished)
        assertTrue(state.table.isEmpty())
        assertEquals(52, state.computerCaptured.size)
        assertEquals(25, state.computerScore)
        assertEquals(0, state.humanScore)
        assertEquals(0, state.humanSweeps + state.computerSweeps)
    }

    @Test
    fun evenPartialStockIsDealtInPairsAndOddStockFailsWithoutMutation() {
        val five = card(TabinetRank.FIVE)
        val stock = listOf(card(TabinetRank.TWO), card(TabinetRank.THREE), card(TabinetRank.FOUR), card(TabinetRank.SIX))
        val state = testState(listOf(five), emptyList(), computerHand = emptyList(), deck = stock)
        val next = engine.playCard(state, Player.HUMAN, five.id)
        assertEquals(stock.take(2), next.humanHand)
        assertEquals(stock.drop(2), next.computerHand)
        assertTrue(next.deck.isEmpty())
        assertEquals(listOf(five), next.table)
        assertEquals(0, next.humanSweeps)
        assertEquals(Player.HUMAN, next.turn)
        assertConserved(state, next)
        val odd = state.copy(deck = stock.take(3))
        assertFailsWith<IllegalArgumentException> { engine.playCard(odd, Player.HUMAN, five.id) }
        assertEquals(stock.take(3), odd.deck)
        assertEquals(listOf(five), odd.humanHand)
        assertTrue(odd.table.isEmpty())
    }

    @Test
    fun lastMoveOfAnEarlierBatchStillAwardsASweep() {
        val nine = card(TabinetRank.NINE, Suit.HEARTS)
        val tableNine = card(TabinetRank.NINE)
        val state = testState(
            humanHand = emptyList(),
            table = listOf(tableNine),
            computerHand = listOf(nine),
            deck = listOf(card(TabinetRank.FIVE), spare),
        ).copy(turn = Player.COMPUTER)
        val next = engine.playCard(state, Player.COMPUTER, nine.id, listOf(tableNine.id))
        assertEquals(1, next.computerSweeps)
        assertTrue(next.table.isEmpty())
        assertFalse(next.finished)
        assertEquals(1, next.humanHand.size)
        assertEquals(1, next.computerHand.size)
        assertEquals(Player.HUMAN, next.turn)
        assertConserved(state, next)
    }

    @Test
    fun majorityBonusAndWinnerAreComputedOnlyAtFinish() {
        val humanCards = listOf(card(TabinetRank.ACE), card(TabinetRank.TEN, Suit.DIAMONDS))
        val computerCards = listOf(card(TabinetRank.KING))
        val ongoing = testState().copy(
            humanCaptured = humanCards,
            computerCaptured = computerCards,
            humanSweeps = 2,
            computerSweeps = 1,
        )
        assertEquals(3, ongoing.humanCardPoints)
        assertEquals(1, ongoing.computerCardPoints)
        assertEquals(5, ongoing.humanScore)
        assertEquals(2, ongoing.computerScore)
        assertEquals(0, ongoing.humanMajorityBonus)
        assertNull(ongoing.gameWinner)
        val finished = ongoing.copy(finished = true, turn = null)
        repeat(3) {
            assertEquals(3, finished.humanMajorityBonus)
            assertEquals(0, finished.computerMajorityBonus)
            assertEquals(8, finished.humanScore)
            assertEquals(Player.HUMAN, finished.gameWinner)
        }
        val tied = finished.copy(
            humanCaptured = listOf(card(TabinetRank.ACE)),
            computerCaptured = computerCards,
            humanSweeps = 0,
            computerSweeps = 0,
        )
        assertEquals(0, tied.humanMajorityBonus)
        assertEquals(0, tied.computerMajorityBonus)
        assertEquals(1, tied.humanScore)
        assertEquals(1, tied.computerScore)
        assertNull(tied.gameWinner)
        assertEquals(Player.COMPUTER, tied.copy(computerSweeps = 1).gameWinner)
        val computerMajority = tied.copy(computerCaptured = computerCards + card(TabinetRank.THREE))
        assertEquals(3, computerMajority.computerMajorityBonus)
        assertEquals(4, computerMajority.computerScore)
    }

    @Test
    fun invalidActionsRejectWithoutChangingTheInput() {
        val nine = card(TabinetRank.NINE, Suit.HEARTS)
        val two = card(TabinetRank.TWO)
        val seven = card(TabinetRank.SEVEN)
        val state = testState(listOf(nine), listOf(two, seven))
        val before = state.copy()
        assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.COMPUTER, spare.id) }
        assertFailsWith<IllegalArgumentException> { engine.playCard(state.copy(finished = true), Player.HUMAN, nine.id) }
        assertFailsWith<IllegalArgumentException> { engine.playCard(state.copy(turn = null), Player.HUMAN, nine.id) }
        assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.HUMAN, "missing") }
        assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.HUMAN, nine.id, listOf("missing")) }
        assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.HUMAN, nine.id, listOf(two.id, two.id)) }
        assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.HUMAN, nine.id, listOf(two.id)) }
        assertFailsWith<IllegalArgumentException> { engine.suggestCapture(state, Player.HUMAN, "missing") }
        assertFailsWith<IllegalArgumentException> { engine.suggestCapture(state, Player.COMPUTER, spare.id) }
        assertFailsWith<IllegalArgumentException> { engine.chooseComputerMove(state) }
        assertFailsWith<IllegalArgumentException> {
            engine.chooseComputerMove(state.copy(turn = Player.COMPUTER, computerHand = emptyList()))
        }
        assertEquals(before, state)
    }

    @Test
    fun suggestionsCombineIndependentGroupsAndPreferScoringCards() {
        val ace = card(TabinetRank.ACE, Suit.HEARTS)
        val table = listOf(card(TabinetRank.ACE), card(TabinetRank.FIVE), card(TabinetRank.SIX))
        val state = testState(listOf(ace), table)
        assertEquals(table.map { it.id }.toSet(), engine.suggestCapture(state, Player.HUMAN, ace.id).toSet())
        val ten = card(TabinetRank.TEN, Suit.HEARTS)
        val clubTwo = card(TabinetRank.TWO)
        val plainTwo = card(TabinetRank.TWO, Suit.DIAMONDS)
        val eight = card(TabinetRank.EIGHT)
        val weighted = testState(listOf(ten), listOf(plainTwo, eight, clubTwo))
        assertEquals(setOf(clubTwo.id, eight.id), engine.suggestCapture(weighted, Player.HUMAN, ten.id).toSet())
        assertTrue(engine.suggestCapture(testState(listOf(ten), listOf(spare)), Player.HUMAN, ten.id).isEmpty())
        assertTrue(engine.suggestCapture(testState(listOf(ten), emptyList()), Player.HUMAN, ten.id).isEmpty())
    }

    @Test
    fun computerUsesOwnHandAndPublicInformationOnly() {
        val ten = card(TabinetRank.TEN, Suit.HEARTS)
        val nine = card(TabinetRank.NINE, Suit.HEARTS)
        val table = listOf(card(TabinetRank.TEN, Suit.DIAMONDS), card(TabinetRank.NINE))
        val state = testState(table = table, computerHand = listOf(nine, ten)).copy(turn = Player.COMPUTER)
        val expected = TabinetMove(ten.id, listOf(table.first().id))
        assertEquals(expected, engine.chooseComputerMove(state))
        repeat(20) { seed ->
            val hidden = fullDeck().filterNot { it in state.computerHand || it in table }.shuffled(Random(seed))
            val changedHiddenState = state.copy(humanHand = hidden.take(seed % 7), deck = hidden.drop(seed % 7))
            assertEquals(expected, engine.chooseComputerMove(changedHiddenState))
        }
        val low = card(TabinetRank.THREE)
        val noCapture = state.copy(table = listOf(spare), computerHand = listOf(nine, low, ten))
        assertEquals(TabinetMove(low.id, emptyList()), engine.chooseComputerMove(noCapture))
        val nineOnTable = card(TabinetRank.NINE)
        val sweeper = state.copy(
            table = listOf(nineOnTable, card(TabinetRank.TWO), card(TabinetRank.SEVEN)),
            computerHand = listOf(ten, nine),
        )
        val sweepMove = engine.chooseComputerMove(sweeper)
        assertEquals(nine.id, sweepMove.cardId)
        assertEquals(sweeper.table.map { it.id }.toSet(), sweepMove.capturedIds.toSet())
    }

    @Test
    fun oneHundredSeededGamesFinishInExactlyFortyEightMovesAndConserveCards() {
        repeat(100) { seed ->
            var state = TabinetEngine(Random(seed)).newGame()
            val initial = state
            var moves = 0
            while (!state.finished && moves < 49) {
                val player = requireNotNull(state.turn)
                val move = if (player == Player.COMPUTER) {
                    engine.chooseComputerMove(state)
                } else {
                    val card = state.humanHand.first()
                    TabinetMove(card.id, engine.suggestCapture(state, player, card.id))
                }
                val hand = if (player == Player.HUMAN) state.humanHand else state.computerHand
                val played = hand.single { it.id == move.cardId }
                val selected = state.table.filter { it.id in move.capturedIds }
                assertEquals(move.capturedIds.size, selected.size)
                assertEquals(move.capturedIds.size, move.capturedIds.toSet().size)
                assertTrue(selected.isEmpty() || engine.canCapture(played, selected), "Seed $seed, move $moves")
                state = engine.playCard(state, player, move.cardId, move.capturedIds)
                moves++
                assertConserved(initial, state)
            }
            assertTrue(state.finished, "Seed $seed exceeded the move bound")
            assertEquals(48, moves)
            assertNull(state.turn)
            assertTrue(state.humanHand.isEmpty() && state.computerHand.isEmpty() && state.deck.isEmpty() && state.table.isEmpty())
            assertEquals(52, state.humanCaptured.size + state.computerCaptured.size)
            assertEquals(22, state.humanCardPoints + state.computerCardPoints)
            val bonus = if (state.humanCaptured.size == state.computerCaptured.size) 0 else 3
            assertEquals(22 + bonus + state.humanSweeps + state.computerSweeps, state.humanScore + state.computerScore)
        }
    }

    @Test
    fun largeTablesUseBoundedSumSearchAndRankCountsRatherThanAllSubsets() {
        val deck = fullDeck()
        for (rank in TabinetRank.entries) {
            val played = card(rank, Suit.HEARTS)
            val table = deck - played
            val state = testState(listOf(played), table, computerHand = emptyList())
            val first = engine.suggestCapture(state, Player.HUMAN, played.id)
            assertTrue(first.isNotEmpty())
            val selected = table.filter { it.id in first }
            assertEquals(first.size, first.toSet().size)
            assertTrue(engine.canCapture(played, selected))
            assertEquals(first, engine.suggestCapture(state, Player.HUMAN, played.id))
            assertFalse(engine.canCapture(played, table))
            assertEquals(table, state.table)
        }
        // Large valid repeated groups, including independent high and low aces.
        val king = card(TabinetRank.KING, Suit.HEARTS)
        val groups = deck.filter { it.rank in setOf(
            TabinetRank.ACE, TabinetRank.TWO, TabinetRank.THREE, TabinetRank.FOUR,
            TabinetRank.FIVE, TabinetRank.SIX, TabinetRank.EIGHT, TabinetRank.NINE,
            TabinetRank.TEN, TabinetRank.JACK,
        ) }
        repeat(10) {
            assertTrue(engine.canCapture(king, groups.shuffled(Random(it))))
        }
    }

    private fun card(rank: TabinetRank, suit: Suit = Suit.CLUBS): TabinetCard = TabinetCard(suit, rank)

    private fun fullDeck(): List<TabinetCard> =
        Suit.entries.flatMap { suit -> TabinetRank.entries.map { card(it, suit) } }

    private fun testState(
        humanHand: List<TabinetCard> = listOf(card(TabinetRank.FIVE)),
        table: List<TabinetCard> = emptyList(),
        computerHand: List<TabinetCard> = listOf(spare),
        deck: List<TabinetCard> = emptyList(),
    ): TabinetState = TabinetState(
        humanHand = humanHand,
        computerHand = computerHand,
        deck = deck,
        table = table,
        turn = Player.HUMAN,
    )

    private fun TabinetState.allCards(): List<TabinetCard> =
        humanHand + computerHand + deck + table + humanCaptured + computerCaptured

    private fun assertConserved(before: TabinetState, after: TabinetState) {
        assertEquals(before.allCards().map { it.id }.sorted(), after.allCards().map { it.id }.sorted())
        assertEquals(after.allCards().size, after.allCards().map { it.id }.toSet().size)
    }
}
