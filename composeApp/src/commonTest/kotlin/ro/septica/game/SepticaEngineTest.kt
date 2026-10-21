package ro.septica.game

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import ro.septica.game.SepticaPlayer as Player

class SepticaEngineTest {
    private val engine = SepticaEngine(Random(7))

    @Test
    fun ranksUseStandardCardSymbols() {
        assertEquals(
            listOf("7", "8", "9", "10", "J", "Q", "K", "A"),
            enumValues<Rank>().map { it.symbol },
        )
    }

    @Test
    fun deckHasThirtyTwoUniqueCardsAndEightPoints() {
        val deck = SepticaRules.fullDeck()

        assertEquals(32, deck.size)
        assertEquals(32, deck.map(Card::id).toSet().size)
        assertEquals(8, SepticaRules.points(deck))
    }

    @Test
    fun sevenAndMatchingRankCutRegardlessOfSuit() {
        assertTrue(
            SepticaRules.isCut(
                Rank.KING,
                Card(Suit.HEARTS, Rank.KING),
            ),
        )
        assertTrue(
            SepticaRules.isCut(
                Rank.KING,
                Card(Suit.DIAMONDS, Rank.SEVEN),
            ),
        )
    }

    @Test
    fun leaderCanCutBackAndWinsWhenFollowerDoesNotCutAgain() {
        val leaderKing = Card(Suit.CLUBS, Rank.KING)
        val followerKing = Card(Suit.HEARTS, Rank.KING)
        val leaderSeven = Card(Suit.SPADES, Rank.SEVEN)
        val followerEight = Card(Suit.DIAMONDS, Rank.EIGHT)
        var state = testState(
            humanHand = listOf(leaderKing, leaderSeven),
            computerHand = listOf(followerKing, followerEight),
        )

        state = engine.playCard(state, Player.HUMAN, leaderKing.id)
        state = engine.playCard(state, Player.COMPUTER, followerKing.id)

        assertTrue(engine.canPass(state, Player.HUMAN))
        assertEquals(listOf(leaderSeven), engine.legalCards(state, Player.HUMAN))

        state = engine.playCard(state, Player.HUMAN, leaderSeven.id)
        state = engine.playCard(state, Player.COMPUTER, followerEight.id)

        assertEquals(GamePhase.TRICK_REVEAL, state.phase)
        assertEquals(Player.HUMAN, state.trickWinner)
        assertEquals(4, state.table.size)
    }

    @Test
    fun leaderMayPassAfterFollowerCuts() {
        val leaderKing = Card(Suit.CLUBS, Rank.KING)
        val followerKing = Card(Suit.HEARTS, Rank.KING)
        var state = testState(
            humanHand = listOf(leaderKing),
            computerHand = listOf(followerKing),
        )

        state = engine.playCard(state, Player.HUMAN, leaderKing.id)
        state = engine.playCard(state, Player.COMPUTER, followerKing.id)
        state = engine.pass(state, Player.HUMAN)

        assertEquals(GamePhase.TRICK_REVEAL, state.phase)
        assertEquals(Player.COMPUTER, state.trickWinner)
    }

    @Test
    fun leaderCannotContinueWithAnUnrelatedCard() {
        val leaderKing = Card(Suit.CLUBS, Rank.KING)
        val followerKing = Card(Suit.HEARTS, Rank.KING)
        val unrelatedEight = Card(Suit.SPADES, Rank.EIGHT)
        var state = testState(
            humanHand = listOf(leaderKing, unrelatedEight),
            computerHand = listOf(followerKing),
        )

        state = engine.playCard(state, Player.HUMAN, leaderKing.id)
        state = engine.playCard(state, Player.COMPUTER, followerKing.id)

        assertTrue(engine.legalCards(state, Player.HUMAN).isEmpty())
        assertFailsWith<IllegalArgumentException> {
            engine.playCard(state, Player.HUMAN, unrelatedEight.id)
        }
    }

    @Test
    fun aCompleteAutomatedGameAccountsForEveryPointCard() {
        var state = engine.newGame()
        var actions = 0

        while (state.phase != GamePhase.FINISHED && actions < 200) {
            state = when (state.phase) {
                GamePhase.TRICK_REVEAL -> engine.completeTrick(state)
                GamePhase.PLAYING -> {
                    val player = assertNotNull(state.turn)
                    val card = if (player == Player.COMPUTER) {
                        engine.chooseComputerCard(state)
                    } else {
                        engine.legalCards(state, player).firstOrNull()
                    }

                    if (card == null) {
                        engine.pass(state, player)
                    } else {
                        engine.playCard(state, player, card.id)
                    }
                }
                GamePhase.FINISHED -> state
            }
            actions++
        }

        assertEquals(GamePhase.FINISHED, state.phase)
        assertEquals(8, state.scores.values.sum())
        assertEquals(0, state.hands.values.sumOf { it.size } + state.deck.size)
        assertTrue(actions < 200)
    }

    private fun testState(
        humanHand: List<Card>,
        computerHand: List<Card>,
    ) = GameState(
        hands = mapOf(Player.HUMAN to humanHand, Player.COMPUTER to computerHand),
        deck = emptyList(),
        turn = Player.HUMAN,
        trickLeader = Player.HUMAN,
    )

    @Test
    fun dealsFourCardsPerPlayerAndRejectsInvalidConfigurations() {
        for (count in 2..4) {
            val deckSize = if (count == 3) 30 else 32
            val state = engine.newGame(Player.entries[count - 1], count)
            assertEquals(Player.entries.take(count), state.players)
            assertTrue(state.hands.values.all { it.size == 4 })
            assertEquals(deckSize - 4 * count, state.deck.size)
            assertEquals(Player.entries[count - 1], state.turn)
            val cards = state.hands.values.flatten() + state.deck
            assertEquals(deckSize, cards.toSet().size)
            assertEquals(SepticaRules.fullDeck(count).toSet(), cards.toSet())
        }
        for (count in listOf(1, 5)) {
            assertFailsWith<IllegalArgumentException> { engine.newGame(playerCount = count) }
        }
        assertFailsWith<IllegalArgumentException> { engine.newGame(Player.COMPUTER_3, 3) }
    }

    @Test
    fun allFollowersRespondBeforeTheLeaderCanContinue() {
        val ace = Card(Suit.CLUBS, Rank.ACE)
        val seven = Card(Suit.CLUBS, Rank.SEVEN)
        val eight = Card(Suit.CLUBS, Rank.EIGHT)
        val nine = Card(Suit.CLUBS, Rank.NINE)
        val otherSeven = Card(Suit.HEARTS, Rank.SEVEN)
        var state = GameState(
            hands = mapOf(
                Player.HUMAN to listOf(ace, seven),
                Player.COMPUTER to listOf(eight),
                Player.COMPUTER_2 to listOf(otherSeven),
                Player.COMPUTER_3 to listOf(nine),
            ),
            deck = emptyList(), turn = Player.HUMAN, trickLeader = Player.HUMAN,
        )
        state = engine.playCard(state, Player.HUMAN, ace.id)
        state = engine.playCard(state, Player.COMPUTER, eight.id)
        assertEquals(GamePhase.PLAYING, state.phase)
        assertEquals(Player.COMPUTER_2, state.turn)
        state = engine.playCard(state, Player.COMPUTER_2, otherSeven.id)
        assertEquals(Player.COMPUTER_3, state.turn)
        state = engine.playCard(state, Player.COMPUTER_3, nine.id)
        assertEquals(Player.HUMAN, state.turn)
        assertTrue(engine.canPass(state, Player.HUMAN))
        assertEquals(listOf(seven), engine.legalCards(state, Player.HUMAN))

        val passed = engine.pass(state, Player.HUMAN)
        assertEquals(Player.COMPUTER_2, passed.trickWinner)
        assertEquals(1, passed.scoreFor(Player.COMPUTER_2))
        state = engine.playCard(state, Player.HUMAN, seven.id)
        assertEquals(GamePhase.TRICK_REVEAL, state.phase)
        assertEquals(Player.HUMAN, state.trickWinner)
        assertEquals(5, state.table.size)
    }

    @Test
    fun lastCutterWinsAfterAllThreePlayersHaveResponded() {
        val cards = listOf(
            Card(Suit.CLUBS, Rank.TEN),
            Card(Suit.HEARTS, Rank.TEN),
            Card(Suit.SPADES, Rank.SEVEN),
        )
        var state = GameState(
            hands = Player.entries.take(3).zip(cards.map(::listOf)).toMap(),
            deck = emptyList(), turn = Player.HUMAN, trickLeader = Player.HUMAN,
        )
        cards.forEachIndexed { index, card ->
            state = engine.playCard(state, Player.entries[index], card.id)
        }
        assertTrue(engine.canPass(state, Player.HUMAN))
        state = engine.pass(state, Player.HUMAN)
        assertEquals(Player.COMPUTER_2, state.trickWinner)
        assertEquals(2, state.scoreFor(Player.COMPUTER_2))
        assertEquals(GamePhase.FINISHED, engine.completeTrick(state).phase)
    }

    @Test
    fun refillStartsWithWinnerAndEmptyWinnerYieldsNextLead() {
        val deck = SepticaRules.fullDeck().take(6)
        val state = GameState(
            hands = Player.entries.associateWith { emptyList<Card>() },
            deck = deck, turn = null, trickLeader = Player.HUMAN,
            phase = GamePhase.TRICK_REVEAL, trickWinner = Player.COMPUTER_2,
        )
        val refilled = engine.completeTrick(state)
        assertEquals(setOf(deck[0], deck[4]), refilled.handFor(Player.COMPUTER_2).toSet())
        assertEquals(setOf(deck[1], deck[5]), refilled.handFor(Player.COMPUTER_3).toSet())
        assertEquals(listOf(deck[2]), refilled.humanHand)
        assertEquals(listOf(deck[3]), refilled.handFor(Player.COMPUTER))
        assertTrue(refilled.deck.isEmpty())
        assertEquals(Player.COMPUTER_2, refilled.turn)

        val exhausted = state.copy(
            deck = emptyList(),
            hands = state.hands + (Player.HUMAN to listOf(deck.first())),
        )
        val next = engine.completeTrick(exhausted)
        assertEquals(Player.HUMAN, next.turn)
        assertEquals(Player.HUMAN, next.trickLeader)
    }

    @Test
    fun refillDealsOneCardPerPlayerPerRoundForEveryCountAndWinner() {
        for (count in 2..4) {
            val players = Player.entries.take(count)
            for (winner in players) {
                for (stockSize in 0..(count * 4 + 1)) {
                    val deck = SepticaRules.fullDeck(count).take(stockSize)
                    val state = GameState(
                        hands = players.associateWith { emptyList<Card>() },
                        deck = deck, turn = null, trickLeader = winner,
                        phase = GamePhase.TRICK_REVEAL, trickWinner = winner,
                    )
                    val refilled = engine.completeTrick(state)
                    state.orderFrom(winner).forEachIndexed { seat, player ->
                        val expected = deck.filterIndexed { index, _ -> index % count == seat }.take(4)
                        assertEquals(expected.toSet(), refilled.handFor(player).toSet())
                    }
                    assertEquals(deck.drop(count * 4), refilled.deck)
                }
            }
        }
    }

    @Test
    fun refillSkipsFullHandsAndStopsDealingToPlayersAsTheyReachFour() {
        val cards = SepticaRules.fullDeck()
        val state = GameState(
            hands = mapOf(
                Player.HUMAN to cards.take(4),
                Player.COMPUTER to cards.subList(4, 7),
                Player.COMPUTER_2 to cards.subList(7, 8),
                Player.COMPUTER_3 to cards.subList(8, 10),
            ),
            deck = cards.drop(10), turn = null, trickLeader = Player.HUMAN,
            phase = GamePhase.TRICK_REVEAL, trickWinner = Player.HUMAN,
        )
        val refilled = engine.completeTrick(state)
        assertEquals(state.humanHand.toSet(), refilled.humanHand.toSet())
        assertEquals(
            (state.handFor(Player.COMPUTER) + cards[10]).toSet(),
            refilled.handFor(Player.COMPUTER).toSet(),
        )
        assertEquals(
            (state.handFor(Player.COMPUTER_2) + listOf(cards[11], cards[13], cards[15])).toSet(),
            refilled.handFor(Player.COMPUTER_2).toSet(),
        )
        assertEquals(
            (state.handFor(Player.COMPUTER_3) + listOf(cards[12], cards[14])).toSet(),
            refilled.handFor(Player.COMPUTER_3).toSet(),
        )
        assertTrue(refilled.hands.values.all { it.size == 4 })
        assertEquals(cards.drop(16), refilled.deck)
    }

    @Test
    fun automatedGamesFinishForEveryPlayerCountAndStarterWithoutLosingCards() {
        for (count in 2..4) {
            val deck = SepticaRules.fullDeck(count).toSet()
            for (seed in 0 until 30) {
                val game = SepticaEngine(Random(seed))
                var state = game.newGame(Player.entries[seed % count], count)
                val collected = mutableListOf<Card>()
                var actions = 0
                while (state.phase != GamePhase.FINISHED && actions++ < 200) {
                    val accounted = state.hands.values.flatten() + state.deck +
                        state.table.map { it.card } + collected
                    assertEquals(deck.size, accounted.size)
                    assertEquals(deck, accounted.toSet())
                    state = if (state.phase == GamePhase.TRICK_REVEAL) {
                        collected += state.table.map { it.card }
                        game.completeTrick(state)
                    } else {
                        val player = assertNotNull(state.turn)
                        val card = if (player == Player.HUMAN) {
                            game.legalCards(state, player).firstOrNull()
                        } else game.chooseComputerCard(state)
                        if (card == null) {
                            assertTrue(game.canPass(state, player))
                            game.pass(state, player)
                        } else game.playCard(state, player, card.id)
                    }
                }
                assertEquals(GamePhase.FINISHED, state.phase, "count=$count seed=$seed")
                assertEquals(8, state.scores.values.sum())
                assertEquals(deck, collected.toSet())
                assertTrue(state.hands.values.all { it.isEmpty() })
                assertEquals(state.completedTricks, state.tricks.values.sum())
            }
        }
    }

    @Test
    fun winnerIsHighestIndividualScoreAndTiesAreShared() {
        val state = engine.newGame(playerCount = 4).copy(
            phase = GamePhase.FINISHED,
            scores = mapOf(Player.HUMAN to 2, Player.COMPUTER to 1, Player.COMPUTER_2 to 3, Player.COMPUTER_3 to 2),
        )
        assertEquals(Player.COMPUTER_2, state.gameWinner)
        val tied = state.copy(scores = state.players.associateWith { 2 })
        assertEquals(null, tied.gameWinner)
        assertEquals(state.players, tied.gameWinners)
        assertTrue(state.copy(phase = GamePhase.PLAYING).gameWinners.isEmpty())
    }

    @Test
    fun threePlayersRemoveOnlyTheTwoRedEightsAndKeepAllPoints() {
        val removed = setOf(Card(Suit.HEARTS, Rank.EIGHT), Card(Suit.DIAMONDS, Rank.EIGHT))
        for (count in 2..4) {
            val deck = SepticaRules.fullDeck(count)
            assertEquals(if (count == 3) 30 else 32, deck.size)
            assertEquals(if (count == 3) 2 else 4, deck.count { it.rank == Rank.EIGHT })
            assertEquals(8, SepticaRules.points(deck))
            assertEquals(if (count == 3) removed else emptySet(), SepticaRules.fullDeck().toSet() - deck.toSet())
        }
    }

    @Test
    fun remainingEightsAreUniversalCutsOnlyInThreePlayerGames() {
        for (count in 2..4) {
            for (suit in listOf(Suit.CLUBS, Suit.SPADES)) {
                for (target in Rank.entries) {
                    assertEquals(
                        count == 3 || target == Rank.EIGHT,
                        SepticaRules.isCut(target, Card(suit, Rank.EIGHT), count),
                    )
                    assertTrue(SepticaRules.isCut(target, Card(suit, Rank.SEVEN), count))
                }
            }
        }
    }

    @Test
    fun botCutsWithEightAndHumanCanCutBackWithTheOtherEightInThreePlayers() {
        val ace = Card(Suit.CLUBS, Rank.ACE)
        val humanEight = Card(Suit.SPADES, Rank.EIGHT)
        val botEight = Card(Suit.CLUBS, Rank.EIGHT)
        val queen = Card(Suit.CLUBS, Rank.QUEEN)
        val nine = Card(Suit.CLUBS, Rank.NINE)
        var state = GameState(
            hands = mapOf(
                Player.HUMAN to listOf(ace, humanEight),
                Player.COMPUTER to listOf(botEight, queen),
                Player.COMPUTER_2 to listOf(nine),
            ),
            deck = emptyList(), turn = Player.HUMAN, trickLeader = Player.HUMAN,
        )
        state = engine.playCard(state, Player.HUMAN, ace.id)
        assertEquals(botEight, engine.chooseComputerCard(state))
        state = engine.playCard(state, Player.COMPUTER, botEight.id)
        assertEquals(Player.COMPUTER, state.lastCutBy)
        state = engine.playCard(state, Player.COMPUTER_2, nine.id)
        assertEquals(listOf(humanEight), engine.legalCards(state, Player.HUMAN))
        val passed = engine.pass(state, Player.HUMAN)
        assertEquals(Player.COMPUTER, passed.trickWinner)
        assertEquals(1, passed.scoreFor(Player.COMPUTER))
        state = engine.playCard(state, Player.HUMAN, humanEight.id)
        state = engine.playCard(state, Player.COMPUTER, queen.id)
        assertEquals(Player.HUMAN, state.trickWinner)
        assertEquals(1, state.scoreFor(Player.HUMAN))
    }

    @Test
    fun botsPreserveWildEightsWhenLeadingOnlyInThreePlayers() {
        val eight = Card(Suit.CLUBS, Rank.EIGHT)
        val nine = Card(Suit.CLUBS, Rank.NINE)
        for (count in 2..4) {
            val state = engine.newGame(Player.COMPUTER, count).copy(
                hands = Player.entries.take(count).associateWith { player ->
                    if (player == Player.COMPUTER) listOf(eight, nine) else emptyList()
                },
            )
            assertEquals(if (count == 3) nine else eight, engine.chooseComputerCard(state))
        }
    }
}
