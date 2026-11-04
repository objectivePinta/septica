package ro.septica.app

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SepticaApp() {
    val themeLoad = remember { loadGameTheme(createThemePreferenceStore()) }
    var selectedTheme by remember { mutableStateOf(themeLoad.theme) }
    var persistenceError by remember { mutableStateOf(themeLoad.errorMessage) }
    SepticaTheme(selectedTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            GameNavigation(
                selectedTheme = selectedTheme,
                persistenceError = persistenceError,
                onSelectTheme = { theme ->
                    selectedTheme = theme
                    persistenceError = saveGameTheme(createThemePreferenceStore(), theme)
                },
            )
        }
    }
}

@Composable
private fun GameNavigation(
    selectedTheme: GameTheme,
    persistenceError: String?,
    onSelectTheme: (GameTheme) -> Unit,
) {
    var route by remember { mutableStateOf(AppRoute.HOME) }
    var homeState by remember { mutableStateOf(HomeState()) }
    var activeGame by remember { mutableStateOf<GameDestination?>(null) }
    var session by remember { mutableStateOf<SepticaGameSession?>(null) }
    var pendingGame by remember { mutableStateOf<GameDestination?>(null) }
    var confirmReplacement by remember { mutableStateOf(false) }
    var confirmReturnHome by remember { mutableStateOf(false) }

    fun startGame(game: GameDestination) {
        homeState = homeState.recordLaunch(game)
        activeGame = game
        session = SepticaGameSession()
        route = AppRoute.GAME
        pendingGame = null
    }

    when (route) {
        AppRoute.HOME -> HomeScreen(
            state = homeState,
            activeGame = activeGame,
            onPlayNow = { route = AppRoute.CATALOG },
            onContinue = { if (session != null) route = AppRoute.GAME },
            onOpenGame = { game ->
                when {
                    activeGame == game && session != null -> route = AppRoute.GAME
                    activeGame != null -> {
                        pendingGame = game
                        confirmReplacement = true
                    }
                    else -> startGame(game)
                }
            },
            onToggleFavorite = { homeState = homeState.toggleFavorite(it) },
            onOpenSettings = { route = AppRoute.SETTINGS },
        )
        AppRoute.CATALOG -> GamePicker(
            onSelect = { game ->
                when {
                    activeGame == game && session != null -> route = AppRoute.GAME
                    activeGame != null -> {
                        pendingGame = game
                        confirmReplacement = true
                    }
                    else -> startGame(game)
                }
            },
        )
        AppRoute.GAME -> {
            val activeSession = session
            if (activeSession != null) {
                SepticaGame(
                    session = activeSession,
                    onMenu = { confirmReturnHome = true },
                    paused = confirmReturnHome,
                )
            }
        }
        AppRoute.SETTINGS -> ThemeSettingsScreen(
            selectedTheme = selectedTheme,
            persistenceError = persistenceError,
            onSelectTheme = onSelectTheme,
            onBack = { route = AppRoute.HOME },
        )
    }

    if (confirmReturnHome) {
        AlertDialog(
            onDismissRequest = { confirmReturnHome = false },
            title = { Text("Înapoi la pagina principală?") },
            text = { Text("Partida rămâne disponibilă pentru continuare cât timp aplicația este deschisă.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReturnHome = false
                    route = AppRoute.HOME
                }) { Text("ÎNAPOI ACASĂ") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReturnHome = false }) { Text("CONTINUĂ PARTIDA") }
            },
        )
    }

    if (confirmReplacement) {
        AlertDialog(
            onDismissRequest = {
                confirmReplacement = false
                pendingGame = null
            },
            title = { Text("Începi o partidă nouă?") },
            text = { Text("Partida curentă va fi înlocuită.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReplacement = false
                    pendingGame?.let(::startGame)
                }) { Text("PARTIDĂ NOUĂ") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmReplacement = false
                    pendingGame = null
                }) { Text("ANULEAZĂ") }
            },
        )
    }
}

private enum class AppRoute { HOME, CATALOG, GAME, SETTINGS }

@Composable
private fun GamePicker(onSelect: (GameDestination) -> Unit) {
    val tokens = LocalGameThemeTokens.current
    TableBackdrop(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(20.dp),
            shape = RoundedCornerShape(32.dp),
            color = tokens.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, tokens.primary.copy(alpha = 0.28f)),
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "BUN VENIT LA MASĂ",
                    color = tokens.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 2.2.sp,
                )
                CatalogArtwork(
                    GameDestination.SEPTICA,
                    modifier = Modifier.width(188.dp).height(176.dp),
                    paintedBackground = true,
                )
                Text(
                    "ȘEPTICĂ",
                    style = MaterialTheme.typography.displaySmall,
                    color = tokens.onSurface,
                )
                Text(
                    "Taie cu un 7. Adună așii și decarii.\nJoacă o partidă relaxată, offline.",
                    color = tokens.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CatalogBadge("2–4 JUCĂTORI")
                    CatalogBadge("OFFLINE")
                }
                Button(
                    onClick = { onSelect(GameDestination.SEPTICA) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("JOACĂ ȘEPTICĂ", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Text(
                    "Fără cont. Fără reclame.",
                    color = tokens.onSurfaceVariant.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun CatalogBadge(text: String) {
    val tokens = LocalGameThemeTokens.current
    Surface(
        shape = RoundedCornerShape(50),
        color = tokens.background.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, tokens.primary.copy(alpha = 0.18f)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            color = tokens.secondary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        )
    }
}
