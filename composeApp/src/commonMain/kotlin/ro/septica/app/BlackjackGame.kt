package ro.septica.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ro.septica.blackjack.BlackjackEngine
import ro.septica.blackjack.BlackjackPhase
import ro.septica.blackjack.handValue

@Composable
internal fun BlackjackGame(onMenu: () -> Unit, paused: Boolean) {
    val engine = remember { BlackjackEngine() }
    var gameId by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf(engine.newGame()) }
    var modalPaused by remember { mutableStateOf(false) }
    fun restart() { gameId++; state = engine.newGame() }
    LaunchedEffect(state, gameId, paused, modalPaused) {
        if (!paused && !modalPaused && state.phase == BlackjackPhase.DEALER) {
            delay(850)
            state = engine.dealerStep(state)
        }
    }
    GameFrame(
        title = "Blackjack · puncte virtuale", onMenu = onMenu, onRestart = ::restart,
        onPauseChange = { modalPaused = it },
        rules = "Doar puncte virtuale: fără bani reali, cumpărături sau premii. Începi cu 100 de puncte. " +
            "Fiecare mână folosește un pachet nou amestecat de 52 de cărți și o miză fixă de 10 puncte, " +
            "scăzută la împărțire. Tu și dealerul primiți câte două cărți; a doua a dealerului este ascunsă. " +
            "Cere o carte sau oprește-te; la 21 te oprești automat. Asul valorează 1 sau 11, figurile 10. " +
            "Peste 21 pierzi. Dealerul trage până la 17 și se oprește inclusiv la 17 cu as valorând 11. " +
            "Blackjack natural (as și carte de 10 din primele două) se rezolvă imediat; două naturale sunt egalitate. " +
            "Câștig normal: primești 20 (profit 10); egalitate: 10; natural: 25 (profit 15, plată 3:2). " +
            "Nu există asigurare, împărțire sau dublare. Următoarea mână păstrează soldul; sub 10 puncte " +
            "jocul se încheie. Joc nou restabilește 100 de puncte și începe prima mână.",
    ) {
        CoachPanel(state, !paused && !modalPaused && state.phase == BlackjackPhase.PLAYER,
            analyze = { ro.septica.coach.blackjackAdvice(state.human, state.dealer.first()) })
        Text("Sold: ${state.bankroll} puncte · Miza mâinii: 10")
        if (state.phase == BlackjackPhase.PLAYER) {
            Text("Dealer · o carte ascunsă")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StandardCardView(state.dealer.first())
                CardBack(Modifier.width(72.dp).height(104.dp))
            }
        } else {
            Text("Dealer: ${handValue(state.dealer)}")
            StandardHand(state.dealer)
        }
        Text("Tu: ${handValue(state.human)}")
        StandardHand(state.human)
        Text(state.notice)
        if (state.phase == BlackjackPhase.PLAYER) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { state = engine.hit(state) }, enabled = !paused && !modalPaused) {
                    Text("Carte")
                }
                Button(onClick = { state = engine.stand(state) }, enabled = !paused && !modalPaused) {
                    Text("Mă opresc")
                }
            }
        } else if (state.phase == BlackjackPhase.FINISHED) {
            if (state.bankrupt) {
                Text("Nu mai ai puncte suficiente. Joc încheiat.")
                Button(onClick = ::restart, enabled = !paused && !modalPaused) { Text("Joc nou · 100 puncte") }
            } else {
                Button(
                    onClick = { gameId++; state = engine.nextRound(state) },
                    enabled = !paused && !modalPaused,
                ) { Text("Mâna următoare · 10 puncte") }
            }
        }
    }
}
