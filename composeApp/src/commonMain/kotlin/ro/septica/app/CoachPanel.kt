package ro.septica.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ro.septica.coach.Advice

@Composable
internal fun CoachPanel(
    stateKey: Any,
    enabled: Boolean,
    analyze: () -> Advice,
    onSelectCard: ((String) -> Unit)? = null,
) {
    val tokens = LocalGameThemeTokens.current
    // A changed position or a modal/lifecycle pause invalidates old advice immediately.
    var advice by remember(stateKey, enabled) { mutableStateOf<Advice?>(null) }
    TextButton(
        onClick = { advice = analyze() },
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = tokens.secondary),
    ) {
        Text(if (enabled) "Sfat tactic · local" else "Sfat tactic · indisponibil", style = MaterialTheme.typography.labelLarge)
    }
    advice?.let { current ->
        AlertDialog(
            onDismissRequest = { advice = null },
            containerColor = tokens.surface,
            titleContentColor = tokens.secondary,
            title = { Text(current.title) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(current.message)
                    Text("Algoritmi deterministici și, la Șeptică, simulări locale. Fără LLM, chatbot sau acces la cărți ascunse. Recomandările nu garantează câștigul.",
                        style = MaterialTheme.typography.bodySmall, color = tokens.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { advice = null }) { Text("Am înțeles") } },
            dismissButton = {
                if (onSelectCard != null && current.recommendedCardId != null) {
                    TextButton(onClick = {
                        onSelectCard(current.recommendedCardId)
                        advice = null
                    }) { Text("Selectează cartea") }
                }
            },
        )
    }
}
