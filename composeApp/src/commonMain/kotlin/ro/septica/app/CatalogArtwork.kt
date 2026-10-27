package ro.septica.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

private data class PreviewCard(val rank: String, val suit: String, val red: Boolean)

internal fun GameDestination.artColor(): Color = when (this) {
    GameDestination.SEPTICA -> Color(0xFF22665B)
}

private fun GameDestination.previewCards(): List<PreviewCard> = when (this) {
    GameDestination.SEPTICA -> listOf(PreviewCard("A", "♠", false), PreviewCard("7", "♥", true), PreviewCard("10", "♦", true))
}

@Composable
internal fun CatalogArtwork(
    game: GameDestination,
    modifier: Modifier = Modifier,
    paintedBackground: Boolean = true,
) {
    val cards = game.previewCards()
    BoxWithConstraints(
        modifier = modifier.clearAndSetSemantics {}.then(
            if (paintedBackground) Modifier.background(
                Brush.linearGradient(listOf(game.artColor(), SepticaColors.DeepGreen)),
            ) else Modifier,
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (paintedBackground) {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width / 2, size.height * 0.62f)
                drawCircle(
                    game.artColor().copy(alpha = 0.65f),
                    radius = size.height * 0.50f,
                    center = center,
                )
                for (index in 0..2) {
                    val inset = 12.dp.toPx() + index * 5.dp.toPx()
                    drawOval(
                        SepticaColors.Gold.copy(alpha = if (index == 0) 0.28f else 0.09f),
                        topLeft = Offset(inset, inset),
                        size = Size((size.width - inset * 2).coerceAtLeast(0f), (size.height - inset * 2).coerceAtLeast(0f)),
                        style = Stroke(0.8.dp.toPx()),
                    )
                }
                repeat(7) { index ->
                    val x = size.width * (index + 1) / 8
                    drawCircle(SepticaColors.Cream.copy(alpha = 0.12f), 1.2.dp.toPx(), Offset(x, 16.dp.toPx()))
                }
            }
        }
        val scale = (maxWidth.value / 280f).coerceIn(0.65f, 1f)
        cards.forEachIndexed { index, card ->
            val distance = index - (cards.size - 1) / 2f
            CardFace(
                rankSymbol = card.rank,
                suitSymbol = card.suit,
                isRed = card.red,
                accessibilityLabel = "",
                compact = scale < 0.8f,
                modifier = Modifier
                    .width((78 * scale).dp)
                    .height((113 * scale).dp)
                    .offset(x = (distance * 43 * scale).dp, y = (kotlin.math.abs(distance) * 5 * scale).dp)
                    .graphicsLayer { rotationZ = distance * 15f },
            )
        }
    }
}
