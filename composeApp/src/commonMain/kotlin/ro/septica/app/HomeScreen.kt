package ro.septica.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role

@Composable
internal fun HomeScreen(
    state: HomeState,
    activeGame: GameDestination?,
    onPlayNow: () -> Unit,
    onContinue: () -> Unit,
    onOpenGame: (GameDestination) -> Unit,
    onToggleFavorite: (GameDestination) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val tokens = LocalGameThemeTokens.current
    TableBackdrop(contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                "BUN VENIT LA MASĂ",
                color = tokens.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text("Jocurile tale, pe îndelete.", style = MaterialTheme.typography.headlineMedium)
            Button(
                onClick = onPlayNow,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Joacă acum", style = MaterialTheme.typography.titleMedium)
            }
            if (activeGame != null) {
                OutlinedButton(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("Continuă jocul · ${activeGame.title}")
                }
            } else {
                Text(
                    "Nu ai o partidă în desfășurare. Începe un joc nou când ești gata.",
                    color = tokens.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            HomeSection("Jocuri recente") {
                if (state.recentGames.isEmpty()) {
                    Text(
                        "Jocurile începute vor apărea aici.",
                        color = tokens.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    state.recentGames.forEach { game ->
                        HomeGameRow(
                            game = game,
                            isFavorite = state.favoriteGame == game,
                            onOpen = { onOpenGame(game) },
                            onToggleFavorite = { onToggleFavorite(game) },
                        )
                    }
                }
            }

            HomeSection("Joc preferat") {
                val favorite = state.favoriteGame
                if (favorite == null) {
                    Text(
                        "Alege o inimă pe un joc ca să-l găsești mai ușor.",
                        color = tokens.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    HomeGameRow(
                        game = favorite,
                        isFavorite = true,
                        onOpen = { onOpenGame(favorite) },
                        onToggleFavorite = { onToggleFavorite(favorite) },
                    )
                }
            }

            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Setări și teme")
            }
        }
    }
}

@Composable
private fun HomeSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val tokens = LocalGameThemeTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = tokens.onSurface)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = tokens.surface.copy(alpha = 0.94f),
            contentColor = tokens.onSurface,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun HomeGameRow(
    game: GameDestination,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val tokens = LocalGameThemeTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = tokens.surfaceVariant.copy(alpha = 0.52f),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onOpen, role = Role.Button)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(game.title, style = MaterialTheme.typography.titleMedium)
                Text(game.description, color = tokens.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onToggleFavorite, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (isFavorite) "★ Favorit" else "♡ Adaugă")
            }
        }
    }
}
