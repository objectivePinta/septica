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
            color = SepticaColors.Panel.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, SepticaColors.Gold.copy(alpha = 0.28f)),
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "BUN VENIT LA MASĂ",
                    color = SepticaColors.Gold,
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
                    color = SepticaColors.Cream,
                )
                Text(
                    "Taie cu un 7. Adună așii și decarii.\nJoacă o partidă relaxată, offline.",
                    color = SepticaColors.Muted,
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
                    color = SepticaColors.Muted.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                )
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
