package ro.septica.macao

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ro.septica.game.Player
import ro.septica.game.Suit

class MacaoEngineTest {
    private val engine = MacaoEngine(Random(7))
    private val spare = card(MacaoRank.KING, Suit.SPADES)

    @Test
    fun fullDeckHasFiftyFourUniqueCardsAndSafeOpening() {
        repeat(100) { seed ->
            val state = MacaoEngine(Random(seed)).newGame()
            val cards = state.allCards()
            assertEquals(54, cards.size)
            assertEquals(54, cards.map { it.id }.toSet().size)
            assertEquals(5, state.humanHand.size)
            assertEquals(5, state.computerHand.size)
            assertEquals(43, state.deck.size)
            assertEquals(1, state.discardPile.size)
            assertEquals(Player.HUMAN, state.turn)
            assertNull(state.winner)
            assertEquals(0, state.pendingDraw)
            assertTrue(state.discardPile.single().rank in MacaoRank.FIVE..MacaoRank.KING)
            assertEquals(state.discardPile.single().suit, state.activeSuit)
            for (suit in Suit.entries) {
                assertEquals(MacaoRank.entries.take(13).toSet(), cards.filter { it.suit == suit }.map { it.rank }.toSet())
            }
            assertEquals(
                setOf(MacaoRank.BLACK_JOKER, MacaoRank.RED_JOKER),
                cards.filter { it.suit == null }.map { it.rank }.toSet(),
            )
        }
    }

    @Test
    fun cardsHaveStableIdsAndRomanianLabels() {
        val ace = card(MacaoRank.ACE, Suit.HEARTS)
        assertEquals("HEARTS-ACE", ace.id)
        assertEquals("as de cupă", ace.accessibilityLabel)
        assertEquals("joker negru", joker(MacaoRank.BLACK_JOKER).accessibilityLabel)
        assertEquals("joker roșu", joker(MacaoRank.RED_JOKER).accessibilityLabel)
        assertEquals(
            listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "JOKER", "JOKER"),
            MacaoRank.entries.map { it.symbol },
        )
        assertFailsWith<IllegalArgumentException> { MacaoCard(null, MacaoRank.ACE) }
        assertFailsWith<IllegalArgumentException> { MacaoCard(Suit.CLUBS, MacaoRank.RED_JOKER) }
    }

    @Test
    fun ordinaryCardsMatchSuitOrRank() {
        val sameSuit = card(MacaoRank.SIX, Suit.HEARTS)
        val sameRank = card(MacaoRank.FIVE, Suit.CLUBS)
        val state = testState(humanHand = listOf(sameSuit, sameRank, spare))
        assertEquals(listOf(sameSuit, sameRank), engine.legalCards(state, Player.HUMAN))
        val played = engine.playCard(state, Player.HUMAN, sameRank.id, Suit.SPADES)
        assertEquals(Suit.CLUBS, played.activeSuit)
        assertEquals(sameRank, played.discardPile.last())
        assertEquals(Player.COMPUTER, played.turn)
        assertEquals(listOf(sameSuit, spare), played.humanHand)
        assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.HUMAN, spare.id) }
    }

    @Test
    fun aceIsWildButRequiresExplicitSuitEvenAsLastCard() {
        val ace = card(MacaoRank.ACE, Suit.CLUBS)
        val state = testState(
            humanHand = listOf(ace, spare),
            computerHand = listOf(card(MacaoRank.SIX, Suit.SPADES), card(MacaoRank.SEVEN, Suit.CLUBS)),
        )
        assertTrue(ace in engine.legalCards(state, Player.HUMAN))
        assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.HUMAN, ace.id) }
        assertFailsWith<IllegalArgumentException> {
            engine.playCard(state.copy(humanHand = listOf(ace)), Player.HUMAN, ace.id)
        }
        val played = engine.playCard(state, Player.HUMAN, ace.id, Suit.SPADES)
        assertEquals(Suit.SPADES, played.activeSuit)
        assertEquals(listOf(card(MacaoRank.SIX, Suit.SPADES)), engine.legalCards(played, Player.COMPUTER))
        assertTrue(played.notice.contains("Macao!"))
        assertEquals(0, played.pendingDraw)
    }

    @Test
    fun bothJokersAreWildAndRequireSuitChoice() {
        for ((rank, penalty) in listOf(MacaoRank.BLACK_JOKER to 5, MacaoRank.RED_JOKER to 10)) {
            val joker = joker(rank)
            val state = testState(humanHand = listOf(joker, spare))
            assertTrue(joker in engine.legalCards(state, Player.HUMAN))
            assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.HUMAN, joker.id) }
            val played = engine.playCard(state, Player.HUMAN, joker.id, Suit.DIAMONDS)
            assertEquals(Suit.DIAMONDS, played.activeSuit)
            assertEquals(penalty, played.pendingDraw)
            assertEquals(Player.COMPUTER, played.turn)
        }
    }

    @Test
    fun attackCardsStackRegardlessOfSuitOrRankAndDrawingClearsPenalty() {
        val two = card(MacaoRank.TWO, Suit.HEARTS)
        val three = card(MacaoRank.THREE, Suit.CLUBS)
        val black = joker(MacaoRank.BLACK_JOKER)
        val red = joker(MacaoRank.RED_JOKER)
        var state = testState(
            humanHand = listOf(two, black, spare),
            computerHand = listOf(three, red, card(MacaoRank.KING, Suit.DIAMONDS)),
        )
        for ((attack, total) in listOf(two to 2, three to 5, black to 10, red to 20)) {
            state = engine.playCard(state, state.turn, attack.id, Suit.SPADES)
            assertEquals(total, state.pendingDraw)
        }
        val beforeDraw = state
        state = engine.drawCards(state, Player.HUMAN)
        assertEquals(beforeDraw.humanHand.size + 20, state.humanHand.size)
        assertEquals(beforeDraw.deck.drop(20), state.deck)
        assertEquals(0, state.pendingDraw)
        assertEquals(Player.COMPUTER, state.turn)
        assertEquals(beforeDraw.activeSuit, state.activeSuit)
        assertConserved(beforeDraw, state)
    }

    @Test
    fun pendingPenaltyAllowsOnlyAttacksAndDrawingIsAllowedDespiteAnAttackInHand() {
        val two = card(MacaoRank.TWO, Suit.SPADES)
        val three = card(MacaoRank.THREE, Suit.CLUBS)
        val black = joker(MacaoRank.BLACK_JOKER)
        val red = joker(MacaoRank.RED_JOKER)
        val four = card(MacaoRank.FOUR, Suit.HEARTS)
        val ace = card(MacaoRank.ACE, Suit.HEARTS)
        val ordinary = card(MacaoRank.SIX, Suit.HEARTS)
        val state = testState(humanHand = listOf(two, three, black, red, four, ace, ordinary)).copy(pendingDraw = 5)
        assertEquals(listOf(two, three, black, red), engine.legalCards(state, Player.HUMAN))
        for (invalid in listOf(four, ace, ordinary)) {
            assertFailsWith<IllegalArgumentException> {
                engine.playCard(state, Player.HUMAN, invalid.id, Suit.HEARTS)
            }
        }
        assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.HUMAN, black.id) }
        val drawn = engine.drawCards(state, Player.HUMAN)
        assertEquals(state.humanHand.size + 5, drawn.humanHand.size)
        assertEquals(0, drawn.pendingDraw)
        assertEquals(Player.COMPUTER, drawn.turn)
    }

    @Test
    fun fourSkipsOpponentAndCurrentPlayerCanActAgain() {
        val four = card(MacaoRank.FOUR, Suit.HEARTS)
        val six = card(MacaoRank.SIX, Suit.HEARTS)
        val state = testState(humanHand = listOf(four, six, spare))
        val skipped = engine.playCard(state, Player.HUMAN, four.id)
        assertEquals(Player.HUMAN, skipped.turn)
        assertEquals(0, skipped.pendingDraw)
        assertNull(skipped.winner)
        assertEquals(Player.COMPUTER, engine.playCard(skipped, Player.HUMAN, six.id).turn)
    }

    @Test
    fun normalDrawEndsTurnEvenIfDrawnCardIsPlayable() {
        val six = card(MacaoRank.SIX, Suit.HEARTS)
        val state = testState(humanHand = listOf(spare), deck = listOf(six))
        val drawn = engine.drawCards(state, Player.HUMAN)
        assertEquals(listOf(spare, six), drawn.humanHand)
        assertEquals(Player.COMPUTER, drawn.turn)
        assertEquals(state.discardPile, drawn.discardPile)
        assertFailsWith<IllegalArgumentException> { engine.playCard(drawn, Player.HUMAN, six.id) }
    }

    @Test
    fun drawingRecyclesAllButTopPreservingChosenSuitAndCards() {
        val top = joker(MacaoRank.BLACK_JOKER)
        val older = listOf(card(MacaoRank.SIX, Suit.CLUBS), card(MacaoRank.SEVEN, Suit.DIAMONDS))
        val stock = card(MacaoRank.EIGHT, Suit.HEARTS)
        val state = testState(
            humanHand = listOf(spare),
            deck = listOf(stock),
            discardPile = older + top,
        ).copy(activeSuit = Suit.SPADES, pendingDraw = 3)
        val drawn = engine.drawCards(state, Player.HUMAN)
        assertEquals(listOf(top), drawn.discardPile)
        assertEquals(Suit.SPADES, drawn.activeSuit)
        assertTrue(drawn.deck.isEmpty())
        assertEquals(stock, drawn.humanHand[1])
        assertEquals((older + stock + spare).toSet(), drawn.humanHand.toSet())
        assertEquals(0, drawn.pendingDraw)
        assertConserved(state, drawn)
    }

    @Test
    fun recyclingLeavesUndrawnCardsInStock() {
        val top = card(MacaoRank.FIVE, Suit.HEARTS)
        val older = listOf(card(MacaoRank.SIX, Suit.CLUBS), card(MacaoRank.SEVEN, Suit.DIAMONDS))
        val state = testState(humanHand = listOf(spare), deck = emptyList(), discardPile = older + top)
        val drawn = engine.drawCards(state, Player.HUMAN)
        assertEquals(1, drawn.deck.size)
        assertEquals(2, drawn.humanHand.size)
        assertEquals(listOf(top), drawn.discardPile)
        assertConserved(state, drawn)
    }

    @Test
    fun exhaustedStockDrawsOnlyAvailableCardsAndReportsActualCount() {
        val top = card(MacaoRank.FIVE, Suit.HEARTS)
        val state = testState(
            humanHand = listOf(spare),
            deck = listOf(card(MacaoRank.SIX, Suit.CLUBS)),
            discardPile = listOf(card(MacaoRank.SEVEN, Suit.DIAMONDS), top),
        ).copy(pendingDraw = 10)
        val drawn = engine.drawCards(state, Player.HUMAN)
        assertEquals(3, drawn.humanHand.size)
        assertEquals(listOf(top), drawn.discardPile)
        assertTrue(drawn.deck.isEmpty())
        assertTrue(drawn.notice.contains("2 din 10"))
        assertEquals(0, drawn.pendingDraw)
        assertEquals(Player.COMPUTER, drawn.turn)
        assertConserved(state, drawn)

        val emptyDraw = engine.drawCards(drawn, Player.COMPUTER)
        assertEquals(drawn.computerHand, emptyDraw.computerHand)
        assertTrue(emptyDraw.notice.contains("0 din 1"))
        assertEquals(Player.HUMAN, emptyDraw.turn)
        assertConserved(drawn, emptyDraw)
    }

    @Test
    fun lastCardWinsImmediatelyIncludingEverySpecialRankAndPendingAttacks() {
        for (player in Player.entries) {
            for (rank in MacaoRank.entries) {
                val last = if (rank in listOf(MacaoRank.BLACK_JOKER, MacaoRank.RED_JOKER)) joker(rank) else card(rank, Suit.CLUBS)
                val state = testState(
                    humanHand = if (player == Player.HUMAN) listOf(last) else listOf(spare),
                    computerHand = if (player == Player.COMPUTER) listOf(last) else listOf(spare),
                ).copy(turn = player, activeSuit = Suit.CLUBS)
                val won = engine.playCard(state, player, last.id, Suit.DIAMONDS)
                assertEquals(player, won.winner, rank.name)
                assertTrue((if (player == Player.HUMAN) won.humanHand else won.computerHand).isEmpty())
                assertEquals(0, won.pendingDraw)
                assertTrue(engine.legalCards(won, player).isEmpty())
                assertNull(engine.chooseComputerCard(won))
                assertFailsWith<IllegalArgumentException> { engine.drawCards(won, player) }
                assertFailsWith<IllegalArgumentException> { engine.playCard(won, player, last.id, Suit.CLUBS) }
            }
        }
        val attack = card(MacaoRank.THREE, Suit.CLUBS)
        val state = testState(humanHand = listOf(attack)).copy(pendingDraw = 12)
        val won = engine.playCard(state, Player.HUMAN, attack.id)
        assertEquals(Player.HUMAN, won.winner)
        assertEquals(0, won.pendingDraw)
        assertEquals(state.computerHand, won.computerHand)
    }

    @Test
    fun wrongTurnAndMissingCardThrowWithoutChangingState() {
        val playable = card(MacaoRank.SIX, Suit.HEARTS)
        val state = testState(humanHand = listOf(playable, spare))
        val snapshot = state.copy(
            humanHand = state.humanHand.toList(),
            computerHand = state.computerHand.toList(),
            deck = state.deck.toList(),
            discardPile = state.discardPile.toList(),
        )
        assertTrue(engine.legalCards(state, Player.COMPUTER).isEmpty())
        assertNull(engine.chooseComputerCard(state))
        assertFailsWith<IllegalArgumentException> { engine.drawCards(state, Player.COMPUTER) }
        assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.COMPUTER, playable.id) }
        assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.HUMAN, "unknown") }
        assertFailsWith<IllegalArgumentException> { engine.playCard(state, Player.HUMAN, state.computerHand.first().id) }
        engine.playCard(state, Player.HUMAN, playable.id)
        engine.drawCards(state, Player.HUMAN)
        assertEquals(snapshot, state)
    }

    @Test
    fun botUsesOwnHandAndPublicDataOnly() {
        val computer = listOf(
            card(MacaoRank.SIX, Suit.HEARTS),
            card(MacaoRank.FIVE, Suit.SPADES),
            card(MacaoRank.SEVEN, Suit.SPADES),
            card(MacaoRank.ACE, Suit.CLUBS),
        )
        val state = testState(humanHand = listOf(spare), computerHand = computer).copy(turn = Player.COMPUTER)
        val changedHiddenData = state.copy(
            humanHand = state.deck.take(12),
            deck = state.deck.drop(12).reversed(),
        )
        assertEquals(engine.chooseComputerCard(state), engine.chooseComputerCard(changedHiddenData))
        assertEquals(Suit.SPADES, engine.chooseComputerSuit(state))
        assertEquals(engine.chooseComputerSuit(state), engine.chooseComputerSuit(changedHiddenData))
        assertTrue(engine.chooseComputerCard(state) in engine.legalCards(state, Player.COMPUTER))
        val blocked = state.copy(computerHand = listOf(card(MacaoRank.SIX, Suit.CLUBS)))
        assertNull(engine.chooseComputerCard(blocked))
        assertEquals(Player.HUMAN, engine.drawCards(blocked, Player.COMPUTER).turn)
        assertEquals(Suit.CLUBS, engine.chooseComputerSuit(state.copy(computerHand = emptyList())))
    }

    @Test
    fun oneHundredSeededGamesTerminateWithoutLosingOrDuplicatingCards() {
        repeat(100) { seed ->
            val engine = MacaoEngine(Random(seed))
            var state = engine.newGame()
            val ids = state.allCards().map { it.id }.toSet()
            var actions = 0
            while (state.winner == null && actions < 10_000) {
                val player = state.turn
                val card = if (player == Player.COMPUTER) {
                    engine.chooseComputerCard(state)
                } else {
                    engine.legalCards(state, player).firstOrNull()
                }
                val hand = if (player == Player.HUMAN) state.humanHand else state.computerHand
                val suit = if (player == Player.COMPUTER) {
                    engine.chooseComputerSuit(state)
                } else {
                    Suit.entries.maxByOrNull { suit -> hand.count { it.suit == suit } }!!
                }
                state = if (card == null) {
                    engine.drawCards(state, player)
                } else {
                    assertTrue(card in engine.legalCards(state, player))
                    engine.playCard(state, player, card.id, suit)
                }
                assertEquals(54, state.allCards().size, "Seed $seed, action $actions")
                assertEquals(ids, state.allCards().map { it.id }.toSet(), "Seed $seed, action $actions")
                assertTrue(state.discardPile.isNotEmpty())
                assertTrue(state.pendingDraw >= 0)
                actions++
            }
            val winner = assertNotNull(state.winner, "Seed $seed did not finish within $actions actions")
            assertTrue((if (winner == Player.HUMAN) state.humanHand else state.computerHand).isEmpty())
        }
    }

    private fun card(rank: MacaoRank, suit: Suit) = MacaoCard(suit, rank)

    private fun joker(rank: MacaoRank) = MacaoCard(null, rank)

    private fun MacaoState.allCards() = humanHand + computerHand + deck + discardPile

    private fun assertConserved(before: MacaoState, after: MacaoState) {
        assertEquals(
            before.allCards().map { it.id }.sorted(),
            after.allCards().map { it.id }.sorted(),
        )
    }

    private fun testState(
        humanHand: List<MacaoCard>,
        computerHand: List<MacaoCard> = listOf(card(MacaoRank.QUEEN, Suit.DIAMONDS)),
        deck: List<MacaoCard>? = null,
        discardPile: List<MacaoCard> = listOf(card(MacaoRank.FIVE, Suit.HEARTS)),
    ): MacaoState {
        val used = humanHand + computerHand + discardPile
        return MacaoState(
            humanHand = humanHand,
            computerHand = computerHand,
            deck = deck ?: MacaoEngine(Random(0)).newGame().allCards().filterNot { it in used },
            discardPile = discardPile,
            turn = Player.HUMAN,
            activeSuit = discardPile.last().suit ?: Suit.HEARTS,
            notice = "",
        )
    }
}
