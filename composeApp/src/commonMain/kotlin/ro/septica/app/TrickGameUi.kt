package ro.septica.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import ro.septica.game.Suit
import ro.septica.tricks.*

private const val WHIST_RULES = "Patru jucători, 32 de cărți (7–A). Tu ești locul 1. " +
    "24 de runde: 1 carte de patru ori, 2–7, 8 cărți de patru ori, 7–2, 1 carte de patru ori. " +
    "Donatorul se rotește; următorul loc licitează și începe. La mai puțin de 8 cărți, " +
    "prima carte rămasă indică atuul; la 8 cărți nu există atu. Licitații 0–numărul de cărți; " +
    "ultimul nu poate face suma egală cu numărul de cărți. Urmezi culoarea cerută; dacă nu ai, " +
    "trebuie să dai atu dacă ai. Nu este obligatoriu să supratai. Cel mai mare atu câștigă, " +
    "altfel cea mai mare carte din culoarea cerută. Câștigătorul începe următoarea levată. " +
    "Exact licitația: 5 + licitația puncte; altfel −diferența absolută. " +
    "După 24 de runde câștigă scorul maxim; egalitățile împart victoria."

private val RENTZ_RULES = "Meci rapid: patru jucători, 32 de cărți (7–A), opt contracte, " +
    "fiecare exact o dată în ordinea de mai jos (nu opt contracte pentru fiecare jucător). " +
    "Toate cărțile se redistribuie după fiecare contract. Donatorul se rotește; următorul loc începe. " +
    "La contractele cu levate urmezi obligatoriu culoarea cerută; fără acea culoare poți da orice. " +
    "Nu există atu sau restricții speciale la deschiderea cu cupe/regele de cupă. " +
    "Cea mai mare carte din culoarea cerută câștigă; câștigătorul începe levata următoare. " +
    "Penalizările se aplică celui care ia levata și se combină numai la Totale.\n\n" +
    Contract.entries.joinToString("\n") { "${it.title}: ${it.rules}" } +
    "\n\nLa Rentz se joacă o singură carte pe tură; jucătorii terminați sunt săriți. " +
    "După al treilea clasat, ultimul primește automat 25 de puncte și cărțile rămase completează masa. " +
    "Scorul maxim cumulat câștigă; egalitățile împart victoria."

private fun seatName(seat: Int): String = if (seat == 0) "Tu" else "Robot $seat"

@Composable
internal fun TrickGameUi(mode: TrickMode, onMenu: () -> Unit, paused: Boolean) {
    val tokens = LocalGameThemeTokens.current
    var gameId by remember(mode) { mutableStateOf(0) }
    var state by remember(mode) { mutableStateOf(newTrickGame(mode)) }
    var modalPaused by remember { mutableStateOf(false) }
    var selectedId by remember(gameId, state.round, state.turn, state.phase) { mutableStateOf<String?>(null) }
    LaunchedEffect(state, gameId, paused, modalPaused) {
        if (!paused && !modalPaused && state.turn != 0 &&
            state.phase in listOf(TrickPhase.BIDDING, TrickPhase.PLAYING)
        ) {
            delay(650)
            state = botStep(state)
        }
    }
    val enabled = !paused && !modalPaused
    val legal = if (state.turn == 0 && enabled) legalCards(state) else emptyList()
    GameFrame(
        title = if (mode == TrickMode.WHIST) "Whist" else "Rentz",
        onMenu = onMenu,
        onRestart = { gameId++; state = newTrickGame(mode) },
        rules = if (mode == TrickMode.WHIST) WHIST_RULES else RENTZ_RULES,
        onPauseChange = { modalPaused = it },
    ) {
        CoachPanel(state, enabled && state.turn == 0 && state.phase in listOf(TrickPhase.BIDDING, TrickPhase.PLAYING),
            analyze = { ro.septica.coach.trickAdvice(ro.septica.coach.TrickView.from(state)) },
            onSelectCard = { selectedId = it })
        Text(
            "Runda ${state.round + 1}/${state.roundCount} · Donator: ${seatName(state.dealer)}",
            color = tokens.onSurface,
        )
        state.contract?.let {
            Text("${it.title}: ${it.rules}", color = tokens.onSurface)
        }
        Text(
            "${state.handSize} cărți · Atu: ${state.trump?.romanianName ?: "fără atu"}" +
                " · Culoare cerută: ${state.table.firstOrNull()?.card?.suit?.romanianName ?: "—"}",
            color = tokens.onSurface,
        )
        state.trumpCard?.let {
            Text("Atu întors (în afara mâinilor)", color = tokens.onSurface)
            StandardCardView(it)
        }
        (0..3).forEach { seat ->
            val bidLabel = if (mode == TrickMode.WHIST) " · Licitat: ${state.bids[seat] ?: "—"}" else ""
            val place = state.finishOrder.indexOf(seat).takeIf { it >= 0 }?.let { " · Loc: ${it + 1}" } ?: ""
            Text(
                "${seatName(seat)} · Cărți: ${state.hands[seat].size}$bidLabel" +
                    " · Levate: ${state.tricks[seat]} · Scor: ${state.scores[seat]}" +
                    " · Runda: ${state.roundScores[seat]}$place",
                color = tokens.onSurface,
            )
        }
        if (state.contract == Contract.RENTZ) {
            Suit.entries.forEach { suit ->
                val cards = state.lanes.filter { it.suit == suit }.sortedBy { it.rank.value }
                Text("${suit.symbol} ${suit.romanianName}: " +
                    if (cards.isEmpty()) "așteaptă J" else cards.joinToString(" · ") { it.rank.symbol },
                    color = tokens.onSurface)
            }
            state.lastPass?.let { Text("${seatName(it)} a spus pas.", color = tokens.onSurface) }
        } else {
            if (state.table.isNotEmpty()) {
                Text("Levada curentă", color = tokens.onSurface)
                TrickCards(state.table)
            }
            if (state.lastTrick.isNotEmpty()) {
                Text("Ultima levată · Câștigător: ${seatName(state.lastWinner!!)}", color = tokens.onSurface)
                TrickCards(state.lastTrick)
            }
        }
        if (state.phase == TrickPhase.BIDDING || state.phase == TrickPhase.PLAYING) {
            Text("La rând: ${seatName(state.turn)}", color = tokens.onSurface)
        }
        Text("Mâna ta", color = tokens.onSurface)
        StandardHand(state.hands[0], selectedId, legal.map { it.id }.toSet()) { selectedId = it }
        when (state.phase) {
            TrickPhase.BIDDING -> if (state.turn == 0) {
                Text("Câte levate vei lua? Suma actuală: ${state.bids.filterNotNull().sum()}", color = tokens.onSurface)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(legalBids(state)) { value ->
                        Button(onClick = { state = bid(state, value) }, enabled = enabled) { Text("$value") }
                    }
                }
            }
            TrickPhase.PLAYING -> if (state.turn == 0) {
                if (state.contract == Contract.RENTZ && legalCards(state).isEmpty()) {
                    Button(onClick = { state = pass(state) }, enabled = enabled) { Text("Pas (fără carte legală)") }
                } else {
                    val selected = legal.firstOrNull { it.id == selectedId }
                    Button(onClick = { selected?.let { state = playCard(state, it) } },
                        enabled = enabled && selected != null) { Text("Joacă această carte") }
                }
            }
            TrickPhase.REVEAL -> Button(onClick = { state = continueTrick(state) }, enabled = enabled) {
                Text("Continuă după levată")
            }
            TrickPhase.ROUND_OVER -> Button(onClick = { state = nextRound(state) }, enabled = enabled) {
                Text(if (mode == TrickMode.WHIST) "Runda următoare" else "Contractul următor")
            }
            TrickPhase.FINISHED -> Text(
                "Meci încheiat · ${if (state.winners.size == 1) "Câștigător" else "Victorie împărțită"}: " +
                    state.winners.joinToString { seatName(it) },
                color = tokens.onSurface,
            )
        }
    }
}

@Composable
private fun TrickCards(cards: List<PlayedCard>) {
    val tokens = LocalGameThemeTokens.current
    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(cards, key = { it.seat }) { played ->
            androidx.compose.foundation.layout.Column {
                Text(seatName(played.seat), color = tokens.onSurface)
                StandardCardView(played.card)
            }
        }
    }
}
