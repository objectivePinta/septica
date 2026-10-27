package ro.septica.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ro.septica.game.Player
import ro.septica.game.Suit
import ro.septica.macao.MacaoCard
import ro.septica.macao.MacaoEngine
import ro.septica.macao.MacaoRank

@Composable
internal fun MacaoGame(onMenu: () -> Unit, paused: Boolean) {
    val engine = remember { MacaoEngine() }
    var state by remember { mutableStateOf(engine.newGame()) }
    var gameNumber by remember { mutableIntStateOf(0) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showRules by remember { mutableStateOf(false) }
    var chooseSuit by remember { mutableStateOf(false) }
    var confirmRestart by remember { mutableStateOf(false) }

    val humanTurn = state.winner == null && state.turn == Player.HUMAN
    val legalIds = engine.legalCards(state, Player.HUMAN).mapTo(mutableSetOf()) { it.id }
    val selectedCard = state.humanHand.find { it.id == selectedId && it.id in legalIds }

    LaunchedEffect(state, gameNumber, showRules, confirmRestart, paused) {
        if (state.winner == null && state.turn == Player.COMPUTER &&
            !showRules && !confirmRestart && !paused
        ) {
            delay(850)
            val card = engine.chooseComputerCard(state)
            state = if (card == null) {
                engine.drawCards(state, Player.COMPUTER)
            } else {
                engine.playCard(
                    state, Player.COMPUTER, card.id,
                    if (card.requiresSuitChoice) engine.chooseComputerSuit(state) else null,
                )
            }
            selectedId = null
        }
    }

    fun playSelected(suit: Suit? = null) {
        val card = selectedCard ?: return
        state = engine.playCard(state, Player.HUMAN, card.id, suit)
        selectedId = null
        chooseSuit = false
    }

    fun restart() {
        gameNumber++
        state = engine.newGame()
        selectedId = null
        chooseSuit = false
        confirmRestart = false
    }

    TableBackdrop(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("MACAO", style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { showRules = true }) { Text("Reguli") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onMenu) { Text("‹ Jocuri") }
                TextButton(onClick = { confirmRestart = true }) { Text("Joc nou") }
            }
            CoachPanel(state, humanTurn && !paused && !showRules && !confirmRestart && !chooseSuit,
                analyze = { ro.septica.coach.macaoAdvice(state.humanHand, state.discardPile.last(), state.activeSuit, state.pendingDraw) },
                onSelectCard = { selectedId = it })
            Surface(shape = RoundedCornerShape(18.dp), color = SepticaColors.Panel) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Adversar: ${state.computerHand.size} cărți",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Pachet: ${state.deck.size} · Pe masă: ${state.discardPile.size}",
                        color = SepticaColors.Muted,
                    )
                    if (state.computerHand.size == 1 && state.winner == null) {
                        Text("Adversarul: Macao!", color = SepticaColors.PaleGold)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardBack(Modifier.width(80.dp).height(116.dp))
                MacaoCardView(
                    card = state.discardPile.last(),
                    modifier = Modifier.width(80.dp).height(116.dp),
                )
            }
            Text(
                text = "Culoarea cerută: ${state.activeSuit.symbol} ${state.activeSuit.romanianName}",
                color = SepticaColors.PaleGold,
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.pendingDraw > 0 && state.winner == null) {
                Text(
                    "Penalizare: +${state.pendingDraw} cărți. Răspunde cu 2, 3 sau joker, ori trage.",
                    color = SepticaColors.PaleGold,
                )
            }
            Text(
                text = state.notice,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = when (state.winner) {
                    Player.HUMAN -> "Ai câștigat!"
                    Player.COMPUTER -> "Adversarul a câștigat."
                    null -> if (humanTurn) "Rândul tău" else "Adversarul joacă…"
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Mâna ta: ${state.humanHand.size} cărți" +
                    if (state.humanHand.size == 1 && state.winner == null) " · Macao!" else "",
                color = SepticaColors.Muted,
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(142.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                items(state.humanHand, key = { it.id }) { card ->
                    MacaoCardView(
                        card = card,
                        selected = card.id == selectedId,
                        muted = humanTurn && card.id !in legalIds,
                        onClick = if (humanTurn && card.id in legalIds) {
                            { selectedId = if (selectedId == card.id) null else card.id }
                        } else null,
                        modifier = Modifier.width(84.dp).height(122.dp),
                    )
                }
            }
            Text("Glisează pentru a vedea toate cărțile.", color = SepticaColors.Muted)
            if (state.winner != null) {
                Button(onClick = ::restart, modifier = Modifier.fillMaxWidth()) { Text("JOACĂ DIN NOU") }
            } else {
                Button(
                    onClick = {
                        if (selectedCard?.requiresSuitChoice == true) chooseSuit = true
                        else playSelected()
                    },
                    enabled = humanTurn && selectedCard != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (selectedCard == null) "ALEGE O CARTE" else "JOACĂ ACEASTĂ CARTE") }
                OutlinedButton(
                    onClick = {
                        state = engine.drawCards(state, Player.HUMAN)
                        selectedId = null
                    },
                    enabled = humanTurn,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.pendingDraw > 0) "TRAGE ${state.pendingDraw} CĂRȚI" else "TRAGE O CARTE")
                }
                Text("Tragerea încheie tura.", color = SepticaColors.Muted)
            }
        }
    }

    if (chooseSuit) {
        AlertDialog(
            onDismissRequest = { chooseSuit = false },
            title = { Text("Alege culoarea") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    enumValues<Suit>().forEach { suit ->
                        OutlinedButton(onClick = { playSelected(suit) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${suit.symbol} ${suit.romanianName}")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { chooseSuit = false }) { Text("ANULEAZĂ") }
            },
        )
    }
    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Începi o partidă nouă?") },
            text = { Text("Partida curentă nu va fi păstrată.") },
            confirmButton = { TextButton(onClick = ::restart) { Text("JOC NOU") } },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("ANULEAZĂ") } },
        )
    }
    if (showRules) {
        MacaoRulesDialog(onDismiss = { showRules = false })
    }
}

private val MacaoCard.requiresSuitChoice: Boolean
    get() = rank == MacaoRank.ACE || rank == MacaoRank.BLACK_JOKER || rank == MacaoRank.RED_JOKER

@Composable
private fun MacaoCardView(
    card: MacaoCard,
    modifier: Modifier,
    selected: Boolean = false,
    muted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    CardFace(
        rankSymbol = if (card.rank == MacaoRank.BLACK_JOKER) "+5"
            else if (card.rank == MacaoRank.RED_JOKER) "+10" else card.rank.symbol,
        suitSymbol = card.suit?.symbol ?: "★",
        isRed = card.suit?.isRed ?: (card.rank == MacaoRank.RED_JOKER),
        accessibilityLabel = card.accessibilityLabel,
        selected = selected,
        muted = muted,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun MacaoRulesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Macao · regulile acestei variante") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("54 de cărți: 2–10, J, Q, K, A și doi jokeri. Fiecare jucător primește cinci cărți.")
                Text("Pune o carte de aceeași culoare sau valoare cu cea de pe masă. Câștigă primul care rămâne fără cărți.")
                Text("2 dă +2, 3 dă +3, jokerul negru +5, iar cel roșu +10. Penalizările se cumulează: poți răspunde cu oricare dintre aceste cărți, indiferent de culoare.")
                Text("Dacă nu răspunzi la penalizare, tragi totalul și îți închei tura. În rest, poți trage o carte în loc să joci; și această tragere încheie tura.")
                Text("4 sare tura adversarului: în doi jucători, mai joci o dată. Nu blochează o penalizare de tras.")
                Text("Asul și jokerii pot fi jucați peste orice culoare și cer alegerea unei culori noi. Asul nu poate răspunde la o penalizare de tras.")
                Text("«Macao!» se anunță automat la o singură carte. Poți încheia cu orice carte, inclusiv una specială; victoria este imediată.")
                Text("Când pachetul se termină, cărțile de pe masă se amestecă, păstrând cartea de deasupra. Dacă nu sunt suficiente cărți, tragi doar câte sunt disponibile.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("AM ÎNȚELES") } },
    )
}
