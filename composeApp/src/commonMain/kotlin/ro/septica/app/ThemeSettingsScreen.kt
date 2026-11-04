package ro.septica.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ThemeSettingsScreen(
    selectedTheme: GameTheme,
    persistenceError: String?,
    onSelectTheme: (GameTheme) -> Unit,
    onBack: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Setări", style = MaterialTheme.typography.headlineMedium)
            Text("Alege paleta pentru toate ecranele jocului.", color = tokens.onSurfaceVariant)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = tokens.surface.copy(alpha = 0.94f),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GameTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = theme == selectedTheme,
                            onClick = { onSelectTheme(theme) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(theme.displayName, style = MaterialTheme.typography.titleSmall)
                                    if (theme == selectedTheme) {
                                        Text("Selectată", color = tokens.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                        )
                    }
                }
            }
            if (persistenceError != null) {
                Text(
                    persistenceError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Înapoi")
            }
        }
    }
}
