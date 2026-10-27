package ro.septica.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ro.septica.game.Player
import ro.septica.tabinet.TabinetCard
import ro.septica.tabinet.TabinetEngine
import ro.septica.tabinet.TabinetState

@Composable
internal fun TabinetGame(onMenu: () -> Unit, paused: Boolean) {
    val engine = remember { TabinetEngine() }
    var state by remember { mutableStateOf(engine.newGame()) }
    var gameNumber by remember { mutableIntStateOf(0) }
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    var selectedTableIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showRules by remember { mutableStateOf(false) }
    var confirmRestart by remember { mutableStateOf(false) }
    var hintMessage by remember { mutableStateOf<String?>(null) }

    val humanTurn = !state.finished && state.turn == Player.HUMAN
    val selectedCard = state.humanHand.find { it.id == selectedCardId }
    val selectedTable = state.table.filter { it.id in selectedTableIds }
    val validCapture = remember(selectedCard, selectedTable) {
        selectedCard != null && engine.canCapture(selectedCard, selectedTable)
    }

    LaunchedEffect(state, gameNumber, showRules, confirmRestart, paused) {
        if (!state.finished && state.turn == Player.COMPUTER &&
            !showRules && !confirmRestart && !paused
        ) {
            delay(1_100)
            val move = engine.chooseComputerMove(state)
            state = engine.playCard(state, Player.COMPUTER, move.cardId, move.capturedIds)
        }
    }

    fun clearSelection() {
        selectedCardId = null
        selectedTableIds = emptySet()
        hintMessage = null
    }

    fun restart() {
        gameNumber++
        state = engine.newGame()
        clearSelection()
        confirmRestart = false
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(SepticaColors.TableGreen, SepticaColors.DeepGreen)),
        ).windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val gridWidth = maxWidth.coerceAtMost(720.dp) - 56.dp
        val columns = ((gridWidth + 8.dp) / 80.dp).toInt().coerceAtLeast(1)
        val rows = ((state.table.size + columns - 1) / columns).coerceAtLeast(1)
        val tableHeight = minOf(
            (rows * 130 + 12).dp,
            (maxHeight * 0.30f).coerceIn(142.dp, 330.dp),
        )
        Column(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "TABINET",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showRules = true }) { Text("Reguli") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onMenu) { Text("‹ Jocuri") }
                TextButton(onClick = { confirmRestart = true }) { Text("Joc nou") }
            }
            TabinetScoreboard(state)
            CoachPanel(state, humanTurn && !paused && !showRules && !confirmRestart,
                analyze = { ro.septica.coach.tabinetAdvice(state.humanHand, state.table, state.humanCaptured.size + state.computerCaptured.size) },
                onSelectCard = { selectedCardId = it; selectedTableIds = emptySet(); hintMessage = null })
            Text(
                text = state.notice,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (state.finished) {
                TabinetResult(state, onReplay = ::restart)
            } else {
                Text(
                    if (humanTurn) "Rândul tău" else "Adversarul joacă…",
                    style = MaterialTheme.typography.titleMedium,
                    color = SepticaColors.PaleGold,
                )
                Text(
                    "Pe masă: ${state.table.size} cărți · selectate: ${selectedTable.size}",
                    color = SepticaColors.Muted,
                )
                Surface(shape = RoundedCornerShape(18.dp), color = SepticaColors.Panel) {
                    if (state.table.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(tableHeight).padding(18.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Masa este goală", style = MaterialTheme.typography.headlineSmall)
                            Text("Următoarea carte rămâne pe masă.", color = SepticaColors.Muted)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(72.dp),
                            modifier = Modifier.fillMaxWidth().height(tableHeight),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.table, key = { it.id }) { card ->
                                TabinetCardView(
                                    card = card,
                                    selected = card.id in selectedTableIds,
                                    compact = true,
                                    onClick = if (humanTurn && selectedCard != null) {
                                        {
                                            selectedTableIds = if (card.id in selectedTableIds) {
                                                selectedTableIds - card.id
                                            } else selectedTableIds + card.id
                                            hintMessage = null
                                        }
                                    } else null,
                                )
                            }
                        }
                    }
                }
                Text(
                    text = when {
                        !humanTurn -> "Așteaptă mutarea adversarului."
                        selectedCard == null -> "1. Alege o carte din mână."
                        selectedTableIds.isEmpty() ->
                            "2. Selectează cărțile de capturat sau pune cartea pe masă."
                        validCapture -> "Captură validă: ${selectedTable.size} cărți."
                        else -> "Selecție invalidă. Fiecare grup trebuie să aibă valoarea cărții tale."
                    },
                    color = if (humanTurn && selectedTableIds.isNotEmpty() && !validCapture) {
                        MaterialTheme.colorScheme.error
                    } else SepticaColors.PaleGold,
                )
                hintMessage?.let { Text(it, color = SepticaColors.PaleGold) }
                Text(
                    "Mâna ta: ${state.humanHand.size} cărți · glisează pentru toate",
                    color = SepticaColors.Muted,
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(152.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 12.dp),
                ) {
                    items(state.humanHand, key = { it.id }) { card ->
                        TabinetCardView(
                            card = card,
                            selected = card.id == selectedCardId,
                            modifier = Modifier.width(80.dp),
                            onClick = if (humanTurn) {
                                {
                                    val newId = if (selectedCardId == card.id) null else card.id
                                    clearSelection()
                                    selectedCardId = newId
                                }
                            } else null,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val card = selectedCard ?: return@OutlinedButton
                            selectedTableIds = engine.suggestCapture(state, Player.HUMAN, card.id).toSet()
                            hintMessage = if (selectedTableIds.isEmpty()) {
                                "Nu există o captură cu această carte."
                            } else null
                        },
                        enabled = humanTurn && selectedCard != null,
                        modifier = Modifier.weight(1f),
                    ) { Text("SUGESTIE") }
                    OutlinedButton(
                        onClick = { selectedTableIds = emptySet(); hintMessage = null },
                        enabled = humanTurn && selectedTableIds.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { Text("DESELECTEAZĂ") }
                }
                Button(
                    onClick = {
                        val card = selectedCard ?: return@Button
                        state = engine.playCard(state, Player.HUMAN, card.id, selectedTableIds.toList())
                        clearSelection()
                    },
                    enabled = humanTurn && selectedCard != null &&
                        (selectedTableIds.isEmpty() || validCapture),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            selectedCard == null -> "ALEGE O CARTE"
                            selectedTableIds.isEmpty() -> "PUNE PE MASĂ"
                            else -> "CAPTUREAZĂ"
                        },
                    )
                }
            }
        }
    }

    if (showRules) TabinetRulesDialog(onDismiss = { showRules = false })
    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Începi o partidă nouă?") },
            text = { Text("Partida curentă nu va fi păstrată.") },
            confirmButton = { TextButton(onClick = ::restart) { Text("JOC NOU") } },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("ANULEAZĂ") } },
        )
    }
}

@Composable
private fun TabinetCardView(
    card: TabinetCard,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CardFace(
            rankSymbol = card.rank.symbol,
            suitSymbol = card.suit.symbol,
            isRed = card.suit.isRed,
            accessibilityLabel = "${card.accessibilityLabel}, valoare ${card.rank.values.joinToString(" sau ")}",
            selected = selected,
            compact = compact,
            onClick = onClick,
            modifier = Modifier.widthIn(max = 84.dp).fillMaxWidth().height(if (compact) 96.dp else 112.dp),
        )
        Text(
            card.rank.values.joinToString("/"),
            style = MaterialTheme.typography.labelSmall,
            color = SepticaColors.Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TabinetScoreboard(state: TabinetState) {
    Surface(shape = RoundedCornerShape(18.dp), color = SepticaColors.Panel) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("TU · ${state.humanScore} puncte", color = SepticaColors.PaleGold)
                    Text(
                        "${state.humanCaptured.size} cărți · ${state.humanSweeps} table",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("ADVERSAR · ${state.computerScore} puncte", color = SepticaColors.PaleGold)
                    Text(
                        "${state.computerCaptured.size} cărți · ${state.computerSweeps} table",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                "Pachet: ${state.deck.size} · În mâna adversarului: ${state.computerHand.size}",
                color = SepticaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TabinetResult(state: TabinetState, onReplay: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            when (state.gameWinner) {
                Player.HUMAN -> "Ai câștigat!"
                Player.COMPUTER -> "Adversarul a câștigat."
                null -> "Egalitate!"
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        Text("Scor final: ${state.humanScore} – ${state.computerScore}")
        Text(
            "Tu: ${state.humanCardPoints} din cărți + " +
                "${state.humanSweeps} din table + ${state.humanMajorityBonus} pentru majoritate.",
        )
        Text(
            "Adversar: ${state.computerCardPoints} din cărți + " +
                "${state.computerSweeps} din table + ${state.computerMajorityBonus} pentru majoritate.",
        )
        Button(onClick = onReplay, modifier = Modifier.fillMaxWidth()) { Text("JOACĂ DIN NOU") }
    }
}

@Composable
private fun TabinetRulesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tabinet · varianta în doi") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("52 de cărți, fără jokeri. Începi cu șase cărți în mână și patru pe masă. Când ambele mâini sunt goale, se împart din nou câte șase; masa rămâne.")
                Text("La fiecare tură joci o singură carte. Alegi cartea din mână, apoi cărțile de pe masă pe care vrei să le iei.")
                Text("Valorile pentru captură: 2–10 ca pe carte, J=12, Q=13, K=14, As=1 sau 11. Culorile nu contează la captură.")
                Text("Poți lua o carte de aceeași valoare sau mai multe grupuri cu suma egală cu valoarea cărții jucate. Exemplu: cu 9 poți lua 9, 2+7 și 3+6. O carte nu poate fi folosită în două grupuri.")
                Text("Fiecare As de pe masă poate conta ca 1 sau 11. Cu un As din mână poți lua și grupuri de 1, și grupuri de 11 în aceeași tură.")
                Text("Poți captura doar o parte din cărțile potrivite sau lăsa cartea pe masă fără captură. «Sugestie» selectează o captură posibilă pentru cartea aleasă.")
                Text("Fiecare As, K, Q, J și 10 capturat aduce 1 punct. 10♦ aduce 2 puncte în total, iar 2♣ aduce 1 punct. Sunt 22 de puncte în cărți.")
                Text("Golirea mesei aduce o tablă: +1 punct. Tablele se adună și nu se anulează. Ultima mutare a partidei nu aduce tablă.")
                Text("La final, cărțile rămase pe masă merg la ultimul care a capturat, fără bonus de tablă. Dacă nu a capturat nimeni, le ia adversarul, care este împărțitorul.")
                Text("Cine a capturat mai multe cărți primește +3 puncte. La 26–26 nu se acordă bonus. Partida se încheie după toate cele 52 de cărți; câștigă scorul mai mare.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("AM ÎNȚELES") } },
    )
}
