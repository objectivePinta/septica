package ro.septica.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ro.septica.game.GamePhase
import ro.septica.game.SepticaEngine
import ro.septica.game.SepticaPlayer

class SepticaStatusTest {
    @Test
    fun eightPointMessagesApplyToEveryPlayerCountAndEveryRobot() {
        for (count in 2..4) {
            val state = SepticaEngine().newGame(playerCount = count).copy(phase = GamePhase.FINISHED)
            state.players.forEach { winner ->
                val result = state.copy(scores = mapOf(winner to 8))
                assertEquals(
                    if (winner == SepticaPlayer.HUMAN) "l-ai batut la fundu gol"
                    else "ai fost batut la fundu gol",
                    statusText(result),
                )
                assertFalse(statusText(result.copy(phase = GamePhase.PLAYING)).contains("fundu gol"))
            }
        }
    }

    @Test
    fun ordinaryWinsAndTiesDoNotUseTheEightPointMessage() {
        val state = SepticaEngine().newGame(playerCount = 3).copy(
            phase = GamePhase.FINISHED,
            scores = mapOf(SepticaPlayer.HUMAN to 3, SepticaPlayer.COMPUTER to 3, SepticaPlayer.COMPUTER_2 to 2),
        )
        assertTrue(statusText(state).startsWith("Egalitate"))
        assertTrue(statusText(state).contains("Ion: 3"))
        val win = state.copy(scores = mapOf(SepticaPlayer.COMPUTER_2 to 5, SepticaPlayer.HUMAN to 3))
        assertTrue(statusText(win).startsWith(SepticaPlayer.COMPUTER_2.label))
        assertFalse(statusText(win).contains("fundu gol"))
    }
}
