package ro.septica.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ro.septica.game.Player
import ro.septica.oldmaid.OldMaidEngine

@Composable
internal fun OldMaidGame(onMenu: () -> Unit, paused: Boolean) {
    val engine = remember { OldMaidEngine() }
    var gameId by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf(engine.newGame()) }
    var modalPaused by remember { mutableStateOf(false) }
    fun restart() { gameId++; state = engine.newGame() }
    LaunchedEffect(state, gameId, paused, modalPaused) {
        if (!paused && !modalPaused && !state.finished && state.turn == Player.COMPUTER) {
            delay(850)
            state = engine.computerMove(state)
        }
    }
    GameFrame(
        title = "Popa prostu’", onMenu = onMenu, onRestart = ::restart,
        onPauseChange = { modalPaused = it },
        rules = "Se scoate regele de treflă din pachet: rămân 51 de cărți și un rege fără pereche. " +
            "Cărțile se împart integral. Perechile de același rang, indiferent de suită, se elimină " +
            "automat la început și după fiecare extragere; patru cărți egale formează două perechi. " +
            "Tu începi: alege o poziție ascunsă din mâna calculatorului. Apoi calculatorul trage " +
            "aleatoriu o carte de la tine. Mâinile sunt amestecate după extragere și eliminarea perechilor. " +
            "Primul rămas fără cărți câștigă; celălalt rămâne cu popa. Nu există limită artificială de ture.",
    ) {
        CoachPanel(state, !paused && !modalPaused && !state.finished && state.turn == Player.HUMAN,
            analyze = { ro.septica.coach.oldMaidAdvice() })
        Text("Calculator: ${state.computer.size} cărți · Perechi eliminate: ${state.discarded.size / 2}")
        LazyRow(modifier = Modifier.height(158.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.computer.indices.toList(), key = { it }) { index ->
                TextButton(
                    enabled = !paused && !modalPaused && !state.finished && state.turn == Player.HUMAN,
                    onClick = { state = engine.draw(state, Player.HUMAN, index) },
                ) {
                    Column {
                        CardBack(Modifier.width(72.dp).height(104.dp).clearAndSetSemantics { })
                        Text("Carte ascunsă ${index + 1}")
                    }
                }
            }
        }
        Text("Cărțile tale (${state.human.size})")
        StandardHand(state.human)
        Text(state.notice)
        if (!state.finished) Text(if (state.turn == Player.HUMAN) "Rândul tău" else "Calculatorul alege…")
        else Button(onClick = ::restart, enabled = !paused && !modalPaused) { Text("Joacă din nou") }
    }
}
