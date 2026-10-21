package ro.septica.coach

import kotlin.random.Random
import kotlin.test.*
import kotlin.time.TimeSource
import ro.septica.cards.*
import ro.septica.game.Card
import ro.septica.game.GamePhase
import ro.septica.game.GameState
import ro.septica.game.Player
import ro.septica.game.Rank
import ro.septica.game.SepticaEngine
import ro.septica.game.SepticaPlayer
import ro.septica.game.Suit
import ro.septica.macao.*
import ro.septica.tabinet.*
import ro.septica.tricks.*
import ro.septica.sixtysix.*
import ro.septica.solitaire.*

class CoachTest {
    private fun card(rank: StandardRank, suit: Suit = Suit.CLUBS) = StandardCard(suit, rank)

    @Test fun blackjackHardAndSoftBasicStrategy() {
        val cases = listOf(
            Triple(listOf(StandardRank.TEN, StandardRank.TWO), StandardRank.THREE, CoachAction.Hit),
            Triple(listOf(StandardRank.TEN, StandardRank.TWO), StandardRank.FOUR, CoachAction.Stand),
            Triple(listOf(StandardRank.TEN, StandardRank.SIX), StandardRank.SIX, CoachAction.Stand),
            Triple(listOf(StandardRank.TEN, StandardRank.SIX), StandardRank.SEVEN, CoachAction.Hit),
            Triple(listOf(StandardRank.TEN, StandardRank.SEVEN), StandardRank.ACE, CoachAction.Stand),
            Triple(listOf(StandardRank.ACE, StandardRank.SIX), StandardRank.SIX, CoachAction.Hit),
            Triple(listOf(StandardRank.ACE, StandardRank.SEVEN), StandardRank.EIGHT, CoachAction.Stand),
            Triple(listOf(StandardRank.ACE, StandardRank.SEVEN), StandardRank.NINE, CoachAction.Hit),
            Triple(listOf(StandardRank.ACE, StandardRank.SEVEN), StandardRank.ACE, CoachAction.Hit),
            Triple(listOf(StandardRank.ACE, StandardRank.EIGHT), StandardRank.TEN, CoachAction.Stand),
            Triple(listOf(StandardRank.ACE, StandardRank.SIX, StandardRank.TEN), StandardRank.TEN, CoachAction.Stand),
            Triple(listOf(StandardRank.ACE, StandardRank.ACE, StandardRank.SIX), StandardRank.NINE, CoachAction.Hit),
        )
        cases.forEach { (ranks, up, action) ->
            assertEquals(action, blackjackAdvice(ranks.mapIndexed { i, rank -> card(rank, Suit.entries[i]) }, card(up, Suit.SPADES)).action)
        }
    }

    @Test fun septicaDeterminizationIsRepeatableAndHiddenInvariant() {
        val engine = SepticaEngine(Random(7))
        val state = engine.newGame()
        val hidden = (state.handFor(SepticaPlayer.COMPUTER) + state.deck).reversed()
        val permuted = state.copy(hands = state.hands + (SepticaPlayer.COMPUTER to hidden.take(4)), deck = hidden.drop(4))
        val advice = SepticaCoach.advise(SepticaView.from(state))
        assertEquals(advice, SepticaCoach.advise(SepticaView.from(permuted)))
        assertEquals(advice, SepticaCoach.advise(SepticaView.from(state)))
        assertTrue(advice.recommendedCardId in engine.legalCards(state, SepticaPlayer.HUMAN).map { it.id })
        assertEquals(32, SepticaCoach.SAMPLES)
    }

    @Test fun septicaPassAndEndStates() {
        val lead = Card(Suit.CLUBS, Rank.ACE)
        val cut = Card(Suit.HEARTS, Rank.SEVEN)
        val state = GameState(
            mapOf(SepticaPlayer.HUMAN to listOf(Card(Suit.CLUBS, Rank.EIGHT)),
                SepticaPlayer.COMPUTER to listOf(Card(Suit.SPADES, Rank.KING))), emptyList(),
            SepticaPlayer.HUMAN, SepticaPlayer.HUMAN,
            table = listOf(ro.septica.game.PlayedCard(SepticaPlayer.HUMAN, lead),
                ro.septica.game.PlayedCard(SepticaPlayer.COMPUTER, cut)),
            lastCutBy = SepticaPlayer.COMPUTER)
        assertEquals(CoachAction.Pass, SepticaCoach.advise(SepticaView.from(state)).action)
        assertNull(SepticaCoach.advise(SepticaView.from(state.copy(phase = GamePhase.FINISHED))).action)
        assertNull(SepticaCoach.advise(SepticaView.from(state.copy(turn = SepticaPlayer.COMPUTER))).action)
    }

    @Test fun septicaMultiplayerAdviceUsesOnlyPublicInformationAndStaysLegal() {
        for (count in 3..4) {
            val engine = SepticaEngine(Random(count))
            var state = engine.newGame(playerCount = count)
            val opponents = state.players.filter { it != SepticaPlayer.HUMAN }
            val hidden = (opponents.flatMap { state.handFor(it) } + state.deck).reversed()
            val permuted = state.copy(
                hands = mapOf(SepticaPlayer.HUMAN to state.humanHand) +
                    opponents.mapIndexed { index, player -> player to hidden.drop(index * 4).take(4) }.toMap(),
                deck = hidden.drop(opponents.size * 4),
            )
            assertEquals(SepticaView.from(state), SepticaView.from(permuted))
            assertEquals(SepticaCoach.advise(SepticaView.from(state)), SepticaCoach.advise(SepticaView.from(permuted)))
            var steps = 0
            while (state.phase != GamePhase.FINISHED && steps++ < 200) {
                state = if (state.phase == GamePhase.TRICK_REVEAL) engine.completeTrick(state)
                else {
                    val player = assertNotNull(state.turn)
                    if (player == SepticaPlayer.HUMAN) {
                        when (val action = SepticaCoach.advise(SepticaView.from(state)).action) {
                            is CoachAction.Play -> {
                                assertTrue(action.cardId in engine.legalCards(state, player).map { it.id })
                                engine.playCard(state, player, action.cardId)
                            }
                            CoachAction.Pass -> {
                                assertTrue(engine.canPass(state, player))
                                engine.pass(state, player)
                            }
                            else -> fail("Expected a legal play or pass")
                        }
                    } else engine.chooseComputerCard(state)?.let { engine.playCard(state, player, it.id) }
                        ?: engine.pass(state, player)
                }
            }
            assertEquals(GamePhase.FINISHED, state.phase)
        }
    }

    @Test fun septicaCoachUsesEightForContinuationOnlyInThreePlayers() {
        val eight = Card(Suit.SPADES, Rank.EIGHT)
        val ace = Card(Suit.CLUBS, Rank.ACE)
        val seven = Card(Suit.CLUBS, Rank.SEVEN)
        for (count in 2..4) {
            val state = GameState(
                hands = SepticaPlayer.entries.take(count).associateWith {
                    if (it == SepticaPlayer.HUMAN) listOf(eight) else emptyList()
                },
                deck = emptyList(), turn = SepticaPlayer.HUMAN, trickLeader = SepticaPlayer.HUMAN,
                table = listOf(
                    ro.septica.game.PlayedCard(SepticaPlayer.HUMAN, ace),
                    ro.septica.game.PlayedCard(SepticaPlayer.COMPUTER, seven),
                ),
                lastCutBy = SepticaPlayer.COMPUTER,
            )
            val expected = if (count == 3) CoachAction.Play(eight.id) else CoachAction.Pass
            assertEquals(expected, SepticaCoach.advise(SepticaView.from(state)).action)
        }
    }

    @Test fun septicaRecommendationsStayLegalDuringPlayAndAreBounded() {
        val start = TimeSource.Monotonic.markNow()
        val engine = SepticaEngine(Random(31))
        var state = engine.newGame()
        repeat(60) {
            state = when {
                state.phase == GamePhase.FINISHED -> state
                state.phase == GamePhase.TRICK_REVEAL -> engine.completeTrick(state)
                state.turn == SepticaPlayer.HUMAN -> {
                    val advice = SepticaCoach.advise(SepticaView.from(state))
                    val action = advice.action
                    if (action == CoachAction.Pass) {
                        assertTrue(engine.canPass(state, SepticaPlayer.HUMAN))
                        engine.pass(state, SepticaPlayer.HUMAN)
                    } else {
                        assertIs<CoachAction.Play>(action)
                        assertTrue(action.cardId in engine.legalCards(state, SepticaPlayer.HUMAN).map { it.id })
                        engine.playCard(state, SepticaPlayer.HUMAN, action.cardId)
                    }
                }
                else -> engine.chooseComputerCard(state)?.let { engine.playCard(state, SepticaPlayer.COMPUTER, it.id) }
                    ?: engine.pass(state, SepticaPlayer.COMPUTER)
            }
        }
        assertTrue(start.elapsedNow().inWholeSeconds < 15, "Small current-trick sampling must stay bounded")
    }

    @Test fun macaoAttackAndSuitAdviceIsLegal() {
        val engine = MacaoEngine(Random(8))
        val state = engine.newGame()
        val hand = listOf(MacaoCard(Suit.CLUBS, MacaoRank.TWO), MacaoCard(null, MacaoRank.RED_JOKER),
            MacaoCard(Suit.HEARTS, MacaoRank.SIX), MacaoCard(Suit.HEARTS, MacaoRank.KING))
        for (pending in listOf(0, 5)) {
            val position = state.copy(humanHand = hand, pendingDraw = pending)
            val advice = macaoAdvice(hand, position.discardPile.last(), position.activeSuit, pending)
            val play = assertIs<CoachAction.Play>(advice.action)
            assertTrue(play.cardId in engine.legalCards(position, Player.HUMAN).map { it.id })
            if (play.cardId == hand[1].id) assertEquals(Suit.HEARTS, play.suit)
        }
        assertEquals(CoachAction.Draw, macaoAdvice(listOf(hand[2]), state.discardPile.last(), Suit.CLUBS, 3).action)
    }

    @Test fun tabinetChoosesActualCaptureAcrossWholeHand() {
        val two = TabinetCard(Suit.CLUBS, TabinetRank.TWO)
        val three = TabinetCard(Suit.HEARTS, TabinetRank.THREE)
        val hand = listOf(TabinetCard(Suit.CLUBS, TabinetRank.FOUR), TabinetCard(Suit.SPADES, TabinetRank.FIVE))
        val advice = tabinetAdvice(hand, listOf(two, three), 0)
        val play = assertIs<CoachAction.Play>(advice.action)
        assertEquals(hand[1].id, play.cardId)
        assertEquals(setOf(two.id, three.id), play.capturedIds.toSet())
        assertTrue(TabinetEngine().canCapture(hand[1], listOf(two, three)))
    }

    @Test fun trickLegalBidFollowTrumpAndHiddenInvariance() {
        var state = newTrickGame(TrickMode.WHIST, 42).copy(turn = 0, bids = listOf(null, 0, 0, 0))
        val bidAdvice = trickAdvice(TrickView.from(state))
        assertTrue(assertIs<CoachAction.Bid>(bidAdvice.action).value in legalBids(state))
        val hand = listOf(card(StandardRank.SEVEN), card(StandardRank.ACE, Suit.HEARTS))
        state = state.copy(phase = TrickPhase.PLAYING, hands = listOf(hand) + state.hands.drop(1),
            table = listOf(PlayedCard(1, card(StandardRank.KING))), bids = listOf(0, 0, 0, 0))
        val advice = trickAdvice(TrickView.from(state))
        assertEquals(hand[0].id, advice.recommendedCardId)
        val changed = state.copy(seed = -77, hands = listOf(hand) + state.hands.drop(1).reversed(),
            stock = state.stock.take(1) + state.stock.drop(1).reversed())
        assertEquals(advice, trickAdvice(TrickView.from(changed)))
        val forcedTrump = TrickView.from(state).copy(
            hand = listOf(card(StandardRank.SEVEN, Suit.HEARTS), card(StandardRank.ACE, Suit.SPADES)),
            trump = Suit.HEARTS)
        assertEquals(forcedTrump.hand[0].id, trickAdvice(forcedTrump).recommendedCardId)
        assertNull(trickAdvice(TrickView.from(state.copy(phase = TrickPhase.FINISHED))).action)
        val rentz = newTrickGame(TrickMode.RENTZ, 5).copy(round = Contract.RENTZ.ordinal, turn = 0,
            hands = listOf(listOf(card(StandardRank.ACE))) + List(3) { emptyList() })
        assertEquals(CoachAction.Pass, trickAdvice(TrickView.from(rentz)).action)
        val placement = TrickView.from(rentz).copy(hand = listOf(card(StandardRank.JACK), card(StandardRank.TEN)))
        assertEquals(placement.hand[0].id, trickAdvice(placement).recommendedCardId)
    }

    @Test fun sixtySixFollowsHeadsAndAnnouncesLegally() {
        val hand = listOf(card(StandardRank.NINE), card(StandardRank.ACE), card(StandardRank.JACK, Suit.HEARTS))
        val state = SixtySixEngine.newMatch(Random(4)).copy(
            human = SixtySixPlayer(hand = hand), stock = emptyList(), trump = Suit.HEARTS,
            leader = Player.COMPUTER, table = listOf(SixtySixPlay(Player.COMPUTER, card(StandardRank.KING))))
        val advice = sixtySixAdvice(SixtySixView.from(state))
        assertEquals(hand[1].id, advice.recommendedCardId)
        assertTrue(advice.recommendedCardId in SixtySixEngine.legalCards(state, Player.HUMAN).map { it.id })
        val marriage = state.copy(table = emptyList(), leader = Player.HUMAN,
            human = SixtySixPlayer(hand = listOf(card(StandardRank.KING), card(StandardRank.QUEEN)), tricks = 1))
        val play = assertIs<CoachAction.Play>(sixtySixAdvice(SixtySixView.from(marriage)).action)
        assertTrue(play.marriage)
        assertTrue(SixtySixEngine.canAnnounce(marriage, Player.HUMAN, marriage.human.hand.single { it.id == play.cardId }))
        val changed = state.copy(computer = state.computer.copy(hand = state.computer.hand.reversed()))
        assertEquals(advice, sixtySixAdvice(SixtySixView.from(changed)))
        val forcedTrump = SixtySixView.from(state).copy(
            hand = listOf(card(StandardRank.NINE, Suit.SPADES), card(StandardRank.JACK, Suit.HEARTS)))
        assertEquals(forcedTrump.hand[1].id, sixtySixAdvice(forcedTrump).recommendedCardId)
        val exchange = forcedTrump.copy(lead = null, stockOpen = true, stockCount = 4,
            hand = listOf(card(StandardRank.NINE, Suit.HEARTS)), faceUpTrump = card(StandardRank.ACE, Suit.HEARTS))
        assertEquals(CoachAction.ExchangeTrump, sixtySixAdvice(exchange).action)
    }

    @Test fun solitaireHiddenPermutationsDoNotChangeLegalRecommendation() {
        val hidden = card(StandardRank.THREE)
        val visible = card(StandardRank.QUEEN, Suit.HEARTS)
        val king = card(StandardRank.KING)
        val board = SolitaireBoard(
            listOf(card(StandardRank.FOUR), card(StandardRank.FIVE)), emptyList(),
            listOf(listOf(TableauCard(hidden, false), TableauCard(visible, true)), listOf(TableauCard(king, true))) +
                List(5) { emptyList() }, List(4) { emptyList() })
        val advice = solitaireAdvice(SolitaireView.from(board))
        val action = assertIs<CoachAction.Move>(advice.action)
        assertEquals(SolitaireSource.Tableau(0, 1), action.source)
        assertEquals(SolitaireTarget.Tableau(1), action.target)
        val moved = SolitaireEngine.move(SolitaireState(board), action.source, action.target)
        assertTrue(moved.board.tableau[0].single().faceUp)
        val changed = board.copy(stock = listOf(hidden, board.stock[0]),
            tableau = listOf(listOf(TableauCard(board.stock[1], false), TableauCard(visible, true))) + board.tableau.drop(1))
        assertEquals(advice, solitaireAdvice(SolitaireView.from(changed)))
    }

    @Test fun solitaireFoundationDrawRecycleAndNoMoves() {
        val empty = SolitaireView(0, 0, null, List(7) { VisibleColumn(0, emptyList()) }, List(4) { null }, List(4) { 0 })
        assertNull(solitaireAdvice(empty).action)
        assertEquals(CoachAction.Draw, solitaireAdvice(empty.copy(stockCount = 4)).action)
        assertEquals(CoachAction.Recycle, solitaireAdvice(empty.copy(wasteCount = 1)).action)
        val ace = solitaireAdvice(empty.copy(wasteTop = card(StandardRank.ACE), wasteCount = 1))
        assertEquals(CoachAction.Move(SolitaireSource.Waste, SolitaireTarget.Foundation(0)), ace.action)
    }

    @Test fun informationOnlyGamesNeverInventActionsOrPositions() {
        assertNull(warAdvice().action)
        assertNull(oldMaidAdvice().action)
        assertNull(oldMaidAdvice().recommendedCardId)
    }
}
