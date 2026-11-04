package ro.septica.app

import kotlin.test.Test
import kotlin.test.assertEquals

class SepticaGameSessionTest {
    @Test
    fun sameSessionRetainsMutableGameState() {
        val session = SepticaGameSession()
        val initialState = session.state
        session.gameNumber = 2
        session.selectedCardId = "selected-card"
        session.showRules = true
        session.showSetup = false
        session.playerCount = 3

        val restoredSession = session

        assertEquals(initialState, restoredSession.state)
        assertEquals(2, restoredSession.gameNumber)
        assertEquals("selected-card", restoredSession.selectedCardId)
        assertEquals(true, restoredSession.showRules)
        assertEquals(false, restoredSession.showSetup)
        assertEquals(3, restoredSession.playerCount)
    }
}
