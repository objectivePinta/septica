package ro.septica.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import ro.septica.war.WarEngine

@Composable
internal fun WarGame(onMenu: () -> Unit, paused: Boolean) {
    val engine = remember { WarEngine() }
    var state by remember { mutableStateOf(engine.newGame()) }
    var modalPaused by remember { mutableStateOf(false) }
    GameFrame(
        title = "Război", onMenu = onMenu, onRestart = { state = engine.newGame() },
        onPauseChange = { modalPaused = it },
        rules = "52 de cărți, câte 26 fiecare. Asul este cel mai mare. Apasă pentru a întoarce " +
            "simultan câte o carte; rangul mai mare câștigă toate cărțile puse în joc, la coada pachetului. " +
            "La egalitate începe războiul: la următoarea apăsare fiecare pune până la 3 cărți ascunse, " +
            "păstrând una de întors. Egalitățile se repetă. Cine nu mai poate întoarce o carte pierde; " +
            "dacă ambii rămân fără cărți în război, este remiză. Câștigi când deții toate cele 52 de cărți. " +
            "Pentru evitarea ciclurilor, după 1.000 de bătălii încheiate fără câștigător este remiză. " +
            "Rezultatul rămâne vizibil până apeși din nou; nu există joc automat.",
    ) {
        CoachPanel(state, !paused && !modalPaused && !state.finished, analyze = { ro.septica.coach.warAdvice() })
        Text("Tu: ${state.human.size} · Calculator: ${state.computer.size} · În joc: ${state.pot.size}")
        Text("Bătălia ${state.battles} / 1.000")
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column {
                Text("Tu")
                state.humanFace?.let { StandardCardView(it) }
            }
            Column {
                Text("Calculator")
                state.computerFace?.let { StandardCardView(it) }
            }
        }
        Text(state.notice)
        if (!state.finished) {
            Button(onClick = { state = engine.flip(state) }, enabled = !paused && !modalPaused) {
                Text(if (state.war) "Continuă războiul" else "Întoarce cărțile")
            }
        } else {
            Button(onClick = { state = engine.newGame() }, enabled = !paused && !modalPaused) {
                Text("Joacă din nou")
            }
        }
    }
}
