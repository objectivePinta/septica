package ro.septica.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import ro.septica.game.Suit
import ro.septica.solitaire.SolitaireEngine
import ro.septica.solitaire.SolitaireSource
import ro.septica.solitaire.SolitaireState
import ro.septica.solitaire.SolitaireTarget

@Composable
internal fun SolitaireGame(onMenu: () -> Unit, paused: Boolean) {
    val tokens = LocalGameThemeTokens.current
    var state by remember { mutableStateOf(SolitaireEngine.newGame(Random.nextInt())) }
    var selection by remember { mutableStateOf<SolitaireSource?>(null) }
    var dialogPaused by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Alege o carte, apoi coloana sau fundația destinație.") }
    val blocked = paused || dialogPaused
    val board = state.board

    fun act(action: () -> SolitaireState) {
        if (blocked) return
        try {
            state = action()
            selection = null
            message = "Alege o carte, apoi destinația. Poți trage și din pachet."
        } catch (error: IllegalArgumentException) {
            message = error.message ?: "Mutare nepermisă."
        }
    }
    fun select(source: SolitaireSource) {
        if (blocked || board.won) return
        if (selection == source) {
            selection = null
            message = "Selecție anulată."
            return
        }
        try {
            val cards = SolitaireEngine.selectedCards(board, source)
            selection = source
            message = "Selectat: ${cards.first().accessibilityLabel} (${cards.size} cărți). Alege destinația."
        } catch (error: IllegalArgumentException) {
            message = error.message ?: "Selecție nepermisă."
        }
    }
    fun target(destination: SolitaireTarget) {
        val source = selection
        if (source == null) {
            if (!blocked) message = "Selectează mai întâi o carte vizibilă."
        } else act { SolitaireEngine.move(state, source, destination) }
    }

    GameFrame(
        title = "Solitaire · Klondike",
        onMenu = onMenu,
        onRestart = {
            state = SolitaireEngine.newGame(Random.nextInt())
            selection = null
            message = "Joc nou. Alege o carte sau trage din pachet."
        },
        onPauseChange = { dialogPaused = it },
        rules = "52 de cărți, fără jokeri. Trage câte o carte; numai ultima carte trasă poate fi mutată. " +
            "Când pachetul este gol, reia cărțile trase, în aceeași ordine, de câte ori dorești. " +
            "În coloane, așază cărțile descrescător, alternând roșu și negru. Asul are valoarea 1. " +
            "Poți muta o secvență vizibilă validă; numai regele poate ocupa o coloană goală. " +
            "Cartea ascunsă rămasă la vârful coloanei se întoarce automat. " +
            "Pe fiecare fundație construiește aceeași suită de la as la rege, câte o carte. " +
            "Poți readuce cartea de sus a unei fundații într-o coloană. Câștigi cu toate cele 52 de cărți " +
            "în fundații. Amestecările nu sunt garantat rezolvabile. Anularea restaurează inclusiv " +
            "întoarcerile și reluarea pachetului. Atinge o sursă, apoi antetul destinației. " +
            "Atinge din nou sursa pentru a deselecta. Derulează orizontal pentru toate cele șapte coloane.",
    ) {
        Text(
            if (board.won) "Felicitări! Ai câștigat! Poți anula ultima mutare sau începe un joc nou." else message,
            color = tokens.onSurface,
        )
        CoachPanel(board, !blocked && !board.won,
            analyze = { ro.septica.coach.solitaireAdvice(ro.septica.coach.SolitaireView.from(board)) })
        Text(
            "Pachet: ${board.stock.size} · Trase: ${board.waste.size} · Fundații: ${board.foundations.sumOf { it.size }}/52",
            color = tokens.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !blocked && !board.won && (board.stock.isNotEmpty() || board.waste.isNotEmpty()),
                onClick = { act { SolitaireEngine.draw(state) } },
            ) { Text(if (board.stock.isEmpty()) "Reia pachetul" else "Trage") }
        }
        OutlinedButton(
            enabled = !blocked && state.history.isNotEmpty(),
            onClick = { act { SolitaireEngine.undo(state) } },
        ) { Text("Anulează mutarea") }
        Text("Ultima carte trasă", color = tokens.onSurface)
        board.waste.lastOrNull()?.let { card ->
            StandardCardView(card, selected = selection == SolitaireSource.Waste,
                onClick = if (blocked || board.won) null else { { select(SolitaireSource.Waste) } })
        } ?: Text("Nicio carte trasă", color = tokens.onSurface)
        Text("Fundații · atinge antetul pentru a depune", color = tokens.onSurface)
        LazyRow(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(4) { index ->
                val suit = Suit.entries[index]
                Column(Modifier.width(88.dp)) {
                    TextButton(enabled = !blocked && !board.won,
                        onClick = { target(SolitaireTarget.Foundation(index)) }) {
                        Text("${suit.symbol} ${board.foundations[index].size}/13",
                            modifier = Modifier.semantics { contentDescription = "Fundația ${suit.romanianName}" })
                    }
                    board.foundations[index].lastOrNull()?.let { card ->
                        StandardCardView(card, selected = selection == SolitaireSource.Foundation(index),
                            onClick = if (blocked || board.won) null else { {
                                if (selection == null || selection == SolitaireSource.Foundation(index)) {
                                    select(SolitaireSource.Foundation(index))
                                } else target(SolitaireTarget.Foundation(index))
                            } })
                    } ?: OutlinedButton(enabled = !blocked && !board.won,
                        onClick = { target(SolitaireTarget.Foundation(index)) }) { Text("As ${suit.symbol}") }
                }
            }
        }
        Text("Coloane · derulează lateral ↔ · atinge antetul pentru a muta", color = tokens.onSurface)
        val tallest = board.tableau.maxOf { pile ->
            64 + pile.count { !it.faceUp } * 20 + pile.count { it.faceUp } * 52
        }.coerceAtLeast(128)
        LazyRow(Modifier.fillMaxWidth().height(tallest.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(7) { column ->
                val pile = board.tableau[column]
                Column(Modifier.width(88.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(enabled = !blocked && !board.won,
                        onClick = { target(SolitaireTarget.Tableau(column)) }) {
                        Text(if (pile.isEmpty()) "Coloana ${column + 1} goală" else "Coloana ${column + 1}")
                    }
                    pile.forEachIndexed { index, entry ->
                        if (!entry.faceUp) {
                            Surface(Modifier.fillMaxWidth().height(18.dp).semantics {
                                contentDescription = "Carte ascunsă"
                            }, color = MaterialTheme.colorScheme.secondaryContainer) { Text("▧") }
                        } else {
                            val source = SolitaireSource.Tableau(column, index)
                            val selected = (selection as? SolitaireSource.Tableau)?.let {
                                it.column == column && index >= it.index
                            } == true
                            Surface(
                                onClick = {
                                    val current = selection
                                    if (current == null || current is SolitaireSource.Tableau && current.column == column) {
                                        select(source)
                                    } else target(SolitaireTarget.Tableau(column))
                                },
                                enabled = !blocked && !board.won,
                                modifier = Modifier.fillMaxWidth().height(50.dp).semantics {
                                    contentDescription = "${entry.card.accessibilityLabel}, coloana ${column + 1}" +
                                        if (selected) ", selectată" else ""
                                },
                                color = if (selected) tokens.secondary else tokens.onSurface,
                                border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            ) {
                                Text(
                                    "${entry.card.rank.symbol} ${entry.card.suit.symbol}",
                                    color = if (entry.card.suit.isRed) androidx.compose.ui.graphics.Color(0xFFB71C1C)
                                    else androidx.compose.ui.graphics.Color(0xFF172020),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
