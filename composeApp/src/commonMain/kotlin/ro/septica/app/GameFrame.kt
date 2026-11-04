package ro.septica.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import ro.septica.cards.StandardCard

@Composable
internal fun GameFrame(
    title: String,
    onMenu: () -> Unit,
    onRestart: () -> Unit,
    rules: String,
    onPauseChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalGameThemeTokens.current
    var showRules by remember { mutableStateOf(false) }
    var showRestart by remember { mutableStateOf(false) }

    fun dismiss() {
        showRules = false
        showRestart = false
        onPauseChange(false)
    }

    TableBackdrop(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "LA MASA DE JOC  /  OFFLINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = tokens.primary,
            )
            Text(title, style = MaterialTheme.typography.displaySmall, color = tokens.onSurface)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onMenu) { Text("‹ Jocuri") }
                TextButton(onClick = { showRules = true; onPauseChange(true) }) { Text("Reguli") }
                TextButton(onClick = { showRestart = true; onPauseChange(true) }) { Text("Joc nou") }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = tokens.surface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, tokens.primary.copy(alpha = 0.18f)),
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content,
                )
            }
            Text(
                "Joacă în ritmul tău. Fără bani reali.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.onSurfaceVariant,
            )
        }
    }
    if (showRules) {
        AlertDialog(
            onDismissRequest = ::dismiss,
            title = { Text("$title · reguli") },
            text = {
                Text(rules, modifier = Modifier.verticalScroll(rememberScrollState()))
            },
            confirmButton = { TextButton(onClick = ::dismiss) { Text("AM ÎNȚELES") } },
        )
    }
    if (showRestart) {
        AlertDialog(
            onDismissRequest = ::dismiss,
            title = { Text("Începi un joc nou?") },
            text = { Text("Progresul curent nu va fi păstrat.") },
            confirmButton = {
                TextButton(onClick = { dismiss(); onRestart() }) { Text("JOC NOU") }
            },
            dismissButton = { TextButton(onClick = ::dismiss) { Text("ANULEAZĂ") } },
        )
    }
}

@Composable
internal fun StandardCardView(
    card: StandardCard,
    modifier: Modifier = Modifier.width(72.dp).height(104.dp),
    selected: Boolean = false,
    muted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    CardFace(
        rankSymbol = card.rank.symbol,
        suitSymbol = card.suit.symbol,
        isRed = card.suit.isRed,
        accessibilityLabel = card.accessibilityLabel,
        selected = selected,
        muted = muted,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
internal fun StandardHand(
    cards: List<StandardCard>,
    selectedId: String? = null,
    legalIds: Set<String> = emptySet(),
    onSelect: ((String) -> Unit)? = null,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(122.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        items(cards, key = { it.id }) { card ->
            StandardCardView(
                card = card,
                selected = card.id == selectedId,
                muted = onSelect != null && card.id !in legalIds,
                onClick = if (onSelect != null && card.id in legalIds) {
                    { onSelect(card.id) }
                } else null,
            )
        }
    }
}
