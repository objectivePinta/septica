package ro.septica.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ro.septica.game.Player
import ro.septica.sixtysix.SixtySixEngine

@Composable
internal fun SixtySixGame(onMenu: () -> Unit, paused: Boolean) {
    var state by remember { mutableStateOf(SixtySixEngine.newMatch()) }
    var gameId by remember { mutableIntStateOf(0) }
    var modalPaused by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var announce by remember { mutableStateOf(false) }
    val active = !paused && !modalPaused
    val humanTurn = active && state.turn == Player.HUMAN
    val legalIds = SixtySixEngine.legalCards(state, Player.HUMAN).map { it.id }.toSet()
    val selected = state.human.hand.find { it.id == selectedId }
    val eligibleMarriage = selected != null && SixtySixEngine.canAnnounce(state, Player.HUMAN, selected)

    fun clearSelection() {
        selectedId = null
        announce = false
    }

    LaunchedEffect(state, gameId, paused, modalPaused) {
        if (!paused && !modalPaused && state.result == null) {
            if (state.table.size == 2) {
                delay(1_500)
                state = SixtySixEngine.resolveTrick(state)
            } else if (state.turn == Player.COMPUTER) {
                delay(950)
                state = SixtySixEngine.computerMove(state)
            }
        }
    }
    GameFrame(
        title = "ȘAIZECI ȘI ȘASE",
        onMenu = onMenu,
        onRestart = {
            gameId++
            state = SixtySixEngine.newMatch()
            clearSelection()
        },
        rules = sixtySixRules,
        onPauseChange = { modalPaused = it },
    ) {
        CoachPanel(state, humanTurn,
            analyze = { ro.septica.coach.sixtySixAdvice(ro.septica.coach.SixtySixView.from(state)) },
            onSelectCard = { selectedId = it; announce = false })
        Text("Partida ${state.roundNumber} · Meci: tu ${state.human.gamePoints} – ${state.computer.gamePoints} adversar · primul la 7")
        Text("Puncte: tu ${state.human.points} – ${state.computer.points} adversar")
        for (player in Player.entries) {
            val owner = state.player(player)
            val name = if (player == Player.HUMAN) "Tu" else "Adversarul"
            val marriages = owner.marriages.joinToString { it.symbol }.ifEmpty { "niciuna" }
            Text("$name · levate: ${owner.tricks} · căsătorii: $marriages")
            if (owner.pendingMarriagePoints > 0) {
                Text("$name: ${owner.pendingMarriagePoints} puncte de căsătorie în așteptarea primei levate.")
            }
        }
        Text("Adversarul: ${state.computer.hand.size} cărți ascunse")
        Text("Atu: ${state.trump.symbol} · Talon: ${state.stock.size} cărți · " + when {
            state.closure != null -> "ÎNCHIS"
            !state.stockOpen -> "EPUIZAT"
            else -> "deschis"
        })
        state.faceUpTrump?.let { StandardCardView(it) }
        state.closure?.let {
            Text("${if (it.player == Player.HUMAN) "Tu ai" else "Adversarul a"} închis talonul; punctele adversarului la închidere: ${it.opponentPoints}.")
        }
        Text(if (state.stockOpen) "Răspuns liber." else "Obligatoriu: culoare și depășire; fără culoare, atu.")
        if (state.table.isNotEmpty()) {
            Text("Levată curentă${if (state.table.size == 2 && state.result == null) " · se rezolvă…" else ""}")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                state.table.forEach { StandardCardView(it.card) }
            }
            Text("Prima carte: ${if (state.leader == Player.HUMAN) "tu" else "adversarul"}")
        }
        state.lastTrick?.let {
            Text("Ultima levată: ${if (it.winner == Player.HUMAN) "tu" else "adversarul"} · +${it.points} puncte din cărți · " +
                it.cards.joinToString { card -> "${card.rank.symbol}${card.suit.symbol}" })
        }
        val result = state.result
        if (result != null) {
            Text("${if (result.winner == Player.HUMAN) "Ai câștigat" else "Adversarul a câștigat"} partida: +${result.gamePoints} puncte de meci.")
            Text(result.reason)
            if (state.matchWinner != null) {
                Text(if (state.matchWinner == Player.HUMAN) "Ai câștigat meciul!" else "Adversarul a câștigat meciul.")
            } else {
                Button(
                    enabled = active,
                    onClick = {
                        gameId++
                        state = SixtySixEngine.nextRound(state)
                        clearSelection()
                    },
                ) { Text("PARTIDA URMĂTOARE") }
            }
        } else {
            Text(when {
                paused -> "Joc întrerupt."
                state.table.size == 2 -> "Se compară cărțile…"
                humanTurn -> "Rândul tău: selectează o carte, apoi joacă."
                else -> "Adversarul joacă…"
            })
        }
        Text("Mâna ta · ${state.human.hand.size} cărți")
        StandardHand(
            cards = state.human.hand,
            selectedId = selectedId,
            legalIds = legalIds,
            onSelect = if (humanTurn) { id -> selectedId = id; announce = false } else null,
        )
        if (result == null) {
            if (eligibleMarriage) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = announce, onCheckedChange = { announce = it }, enabled = humanTurn)
                    Text("Anunță căsătoria: ${if (selected?.suit == state.trump) 40 else 20} puncte")
                }
            }
            Button(
                enabled = humanTurn && selectedId in legalIds,
                onClick = {
                    selectedId?.let { state = SixtySixEngine.play(state, Player.HUMAN, it, announce && eligibleMarriage) }
                    clearSelection()
                },
            ) { Text("JOACĂ") }
            OutlinedButton(
                enabled = humanTurn && SixtySixEngine.canExchange(state, Player.HUMAN),
                onClick = { state = SixtySixEngine.exchangeTrump(state, Player.HUMAN); clearSelection() },
            ) { Text("SCHIMBĂ NOUĂ DE ATU") }
            OutlinedButton(
                enabled = humanTurn && SixtySixEngine.canClose(state, Player.HUMAN),
                onClick = { state = SixtySixEngine.closeStock(state, Player.HUMAN); clearSelection() },
            ) { Text("ÎNCHIDE TALONUL") }
        }
    }
}

private val sixtySixRules = """
    În doi, cu 24 de cărți: 9, J, Q, K, 10, A din fiecare culoare. Ordinea este A > 10 > K > Q > J > 9. Valorile: A=11, 10=10, K=4, Q=3, J=2, 9=0; total 120.

    Fiecare primește 6 cărți; talonul are 12, inclusiv atuul vizibil de dedesubt. Câștigătorul levatei trage primul, apoi adversarul, și conduce următoarea levată. Primul conducător alternează între partide.

    Cu talon deschis poți răspunde cu orice carte. Cel mai mare atu câștigă; fără atu, cea mai mare carte din culoarea cerută. După epuizare sau închidere trebuie să urmezi culoarea și să depășești dacă poți; dacă nu ai culoarea, trebuie atu, altfel orice carte. Nu există obligație suplimentară de supraatu.

    Înainte să conduci, cu talon deschis și cel puțin două cărți în el, poți schimba 9 de atu din mână cu atuul vizibil.

    Conducătorul cu K și Q din aceeași culoare poate bifa căsătoria și juca una dintre ele: 20 puncte, sau 40 la atu, o singură dată pe culoare și jucător. Punctele se creditează numai după cel puțin o levată câștigată; altfel rămân în așteptare până la prima levată. Anunțul dezvăluie culoarea, nu restul mâinii.

    66 se revendică automat după levată sau căsătorie creditată, încheind imediat partida. Câștigătorul primește 1 punct de meci dacă adversarul are minimum 33, 2 dacă are 1–32, 3 dacă are 0.

    Înainte de a conduce poți închide talonul: nu se mai trage, iar regulile stricte se aplică imediat. Te obligi să atingi 66 înaintea adversarului. Reușita aduce 1/2/3 puncte după punctele adversarului ÎN MOMENTUL ÎNCHIDERII (minimum 33 / 1–32 / 0). Dacă adversarul atinge 66 sau mâinile se termină înainte să reușești, adversarul primește 2 puncte, ori 3 dacă avea 0 la închidere.

    Fără închidere, dacă toate cărțile se joacă fără 66, câștigătorul ultimei levate primește 1 punct de meci. Nu se adaugă 10 puncte pentru ultima levată.

    Meciul se câștigă la cel puțin 7 puncte. Pornești explicit partida următoare; «Joc nou» resetează întregul meci. Calculatorul poate schimba atuul și anunța căsătorii, dar nu închide talonul și nu consultă cărțile ascunse.
""".trimIndent()
