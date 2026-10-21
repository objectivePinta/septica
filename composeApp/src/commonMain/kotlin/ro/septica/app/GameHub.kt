package ro.septica.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SepticaApp() {
    SepticaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            GameNavigation()
        }
    }
}

@Composable
private fun GameNavigation() {
    var destination by remember { mutableStateOf<GameDestination?>(null) }
    var confirmExit by remember { mutableStateOf(false) }

    when (destination) {
        null -> GamePicker(onSelect = { destination = it })
        GameDestination.SEPTICA -> SepticaGame(onMenu = { confirmExit = true }, paused = confirmExit)
        GameDestination.MACAO -> MacaoGame(onMenu = { confirmExit = true }, paused = confirmExit)
        GameDestination.TABINET -> TabinetGame(onMenu = { confirmExit = true }, paused = confirmExit)
        GameDestination.WAR -> WarGame(onMenu = { confirmExit = true }, paused = confirmExit)
        GameDestination.OLD_MAID -> OldMaidGame(onMenu = { confirmExit = true }, paused = confirmExit)
        GameDestination.BLACKJACK -> BlackjackGame(onMenu = { confirmExit = true }, paused = confirmExit)
        GameDestination.WHIST -> WhistGame(onMenu = { confirmExit = true }, paused = confirmExit)
        GameDestination.RENTZ -> RentzGame(onMenu = { confirmExit = true }, paused = confirmExit)
        GameDestination.SOLITAIRE -> SolitaireGame(onMenu = { confirmExit = true }, paused = confirmExit)
        GameDestination.SIXTY_SIX -> SixtySixGame(onMenu = { confirmExit = true }, paused = confirmExit)
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Înapoi la jocuri?") },
            text = { Text("Partida curentă nu va fi păstrată.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    destination = null
                }) { Text("ÎNAPOI LA JOCURI") }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("CONTINUĂ PARTIDA") }
            },
        )
    }
}

@Composable
private fun GamePicker(onSelect: (GameDestination) -> Unit) {
    var query by remember { mutableStateOf("") }
    val games = matchingGames(query)
    TableBackdrop(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(280.dp),
            modifier = Modifier.widthIn(max = 1_000.dp).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "heading", span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = SepticaColors.Panel.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, SepticaColors.Gold.copy(alpha = 0.30f)),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "BUN VENIT ÎN CLUB",
                            color = SepticaColors.Gold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("La masa\nde joc.", style = MaterialTheme.typography.displaySmall)
                                Text("Cărți clasice.\nO experiență nouă.", color = SepticaColors.Muted)
                            }
                            CatalogArtwork(
                                GameDestination.SEPTICA,
                                modifier = Modifier.width(122.dp).height(130.dp),
                                paintedBackground = false,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CatalogBadge("10 JOCURI")
                            CatalogBadge("OFFLINE")
                            CatalogBadge("ASISTENT LOCAL")
                        }
                    }
                }
            }
            item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Caută un joc") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = if (query.isNotEmpty()) {
                        { TextButton(onClick = { query = "" }) { Text("Șterge") } }
                    } else null,
                )
            }
            items(games, key = { it.name }) { game ->
                GameOption(game = game, onClick = { onSelect(game) })
            }
            if (games.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text("Niciun joc găsit. Încearcă alt nume.", color = SepticaColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun GameOption(game: GameDestination, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = SepticaColors.Panel,
        border = BorderStroke(1.dp, game.artColor().copy(alpha = 0.70f)),
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            CatalogArtwork(game, modifier = Modifier.fillMaxWidth().height(152.dp))
            Column(Modifier.padding(20.dp)) {
                Text(game.title, style = MaterialTheme.typography.headlineSmall, color = SepticaColors.PaleGold)
                Spacer(Modifier.height(10.dp))
                Text(game.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(game.players, style = MaterialTheme.typography.bodySmall, color = SepticaColors.Muted)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                    Text("INTRĂ LA MASĂ  →")
                }
            }
        }
    }
}

@Composable
private fun CatalogBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = SepticaColors.DeepGreen.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, SepticaColors.Gold.copy(alpha = 0.18f)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            color = SepticaColors.PaleGold,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        )
    }
}
