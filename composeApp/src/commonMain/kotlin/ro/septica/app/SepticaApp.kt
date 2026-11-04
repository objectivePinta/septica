package ro.septica.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import ro.septica.game.GamePhase
import ro.septica.game.GameState
import ro.septica.game.SepticaPlayer as Player
import ro.septica.game.SepticaEngine
import ro.septica.game.SepticaRules

@Composable
internal fun SepticaGame(onMenu: () -> Unit, paused: Boolean) {
    val engine = remember { SepticaEngine() }
    var gameNumber by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf(engine.newGame()) }
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    var showRules by remember { mutableStateOf(false) }
    var showSetup by remember { mutableStateOf(true) }
    var playerCount by remember { mutableIntStateOf(2) }

    LaunchedEffect(
        gameNumber,
        state.phase,
        state.turn,
        state.table.size,
        showRules,
        showSetup,
        paused
    ) {
        if (showRules || showSetup || paused) return@LaunchedEffect
        when {
            state.phase == GamePhase.TRICK_REVEAL -> {
                delay(1_150)
                selectedCardId = null
                state = engine.completeTrick(state)
            }

            state.phase == GamePhase.PLAYING && state.turn != Player.HUMAN -> {
                delay(if (state.table.isEmpty()) 700 else 850)
                val player = state.turn ?: return@LaunchedEffect
                val card = engine.chooseComputerCard(state)
                state = if (card == null) {
                    engine.pass(state, player)
                } else {
                    engine.playCard(state, player, card.id)
                }
            }
        }
    }

    fun startNewGame() {
        val starter = Player.entries[gameNumber % playerCount]
        gameNumber++
        state = engine.newGame(starter, playerCount)
        selectedCardId = null
        showSetup = false
    }

    val legalCardIds = remember(state) {
        engine.legalCards(state, Player.HUMAN).mapTo(mutableSetOf()) { it.id }
    }
    val humanCanPass = engine.canPass(state, Player.HUMAN)

    GameScreen(
        coachContent = {
            CoachPanel(
                state,
                !paused && !showRules && !showSetup && state.phase == GamePhase.PLAYING && state.turn == Player.HUMAN,
                analyze = {
                    ro.septica.coach.SepticaCoach.advise(
                        ro.septica.coach.SepticaView.from(
                            state
                        )
                    )
                },
                onSelectCard = { selectedCardId = it })
        },
        state = state,
        statusText = statusText(state),
        selectedCardId = selectedCardId,
        legalCardIds = legalCardIds,
        humanCanPass = humanCanPass,
        onCardSelected = { cardId ->
            selectedCardId = if (selectedCardId == cardId) null else cardId
        },
        onPlaySelected = {
            selectedCardId?.let { cardId ->
                state = engine.playCard(state, Player.HUMAN, cardId)
                selectedCardId = null
            }
        },
        onPass = {
            state = engine.pass(state, Player.HUMAN)
            selectedCardId = null
        },
        onNewGame = {
            playerCount = state.players.size
            showSetup = true
        },
        onShowRules = { showRules = true },
        onMenu = onMenu,
    )

    if (showRules) {
        RulesDialog(onDismiss = { showRules = false })
    }
    if (showSetup) {
        AlertDialog(
            onDismissRequest = { showSetup = false },
            title = { Text("Șeptică · joc nou") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Alege numărul total de jucători, inclusiv tu. Fiecare joacă pentru el, fără echipe.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (2..4).forEach { count ->
                            FilterChip(
                                selected = playerCount == count,
                                onClick = { playerCount = count },
                                label = { Text("$count") },
                            )
                        }
                    }
                    Text(
                        "La masă: " + Player.entries.take(playerCount)
                            .joinToString { it.label } + ".")
                    Text("Adversari automați: ${playerCount - 1}.")
                    if (playerCount == 3) {
                        Text("30 de cărți: fără 8 de cupă și 8 de caro. Ceilalți doi optari taie ca șeptarii.")
                    }
                }
            },
            confirmButton = { TextButton(onClick = ::startNewGame) { Text("JOACĂ") } },
            dismissButton = { TextButton(onClick = { showSetup = false }) { Text("ANULEAZĂ") } },
        )
    }
}

internal fun statusText(state: GameState): String =
    when (state.phase) {
        GamePhase.FINISHED -> when {
            state.gameWinner == Player.HUMAN && state.scoreFor(Player.HUMAN) == SepticaRules.TOTAL_POINT_CARDS ->
                "l-ai batut la fundu gol"

            state.gameWinner?.let { state.scoreFor(it) == SepticaRules.TOTAL_POINT_CARDS } == true ->
                "ai fost batut la fundu gol"

            state.gameWinner == Player.HUMAN -> "Ai câștigat partida!"
            state.gameWinner != null -> "${state.gameWinner?.label} a câștigat partida"
            else -> "Egalitate — " + state.gameWinners.joinToString {
                "${it.label}: ${
                    state.scoreFor(
                        it
                    )
                }"
            }
        }

        GamePhase.TRICK_REVEAL -> {
            val points = state.trickPoints
            val pointsText = when (points) {
                0 -> ""
                1 -> " · +1 punct"
                else -> " · +$points puncte"
            }
            if (state.trickWinner == Player.HUMAN) {
                "Ai luat mâna$pointsText"
            } else {
                "${state.trickWinner?.label} a luat mâna$pointsText"
            }
        }

        GamePhase.PLAYING -> when {
            state.table.isEmpty() && state.turn == Player.HUMAN ->
                "Deschizi mâna · alege orice carte"

            state.table.isEmpty() ->
                "${state.turn?.label} deschide mâna…"

            state.turn == Player.HUMAN && state.trickLeader == Player.HUMAN ->
                "Ai fost tăiat · continuă cu ${state.targetRank?.symbol}, 7${if (state.players.size == 3) " sau 8" else ""}"

            state.turn == Player.HUMAN ->
                "Rândul tău · poți pune orice carte"

            state.turn == state.trickLeader ->
                "${state.turn?.label} decide dacă mai taie…"

            else ->
                "${state.turn?.label} răspunde…"
        }
    }
