package ro.septica.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun TableBackdrop(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().background(SepticaColors.DeepGreen),
        contentAlignment = contentAlignment,
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                Brush.radialGradient(
                    listOf(SepticaColors.Felt.copy(alpha = 0.78f), Color.Transparent),
                    center = Offset(size.width * 0.35f, size.height * 0.12f),
                    radius = maxOf(size.width, size.height) * 0.95f,
                ),
            )
            drawOval(
                color = SepticaColors.Gold.copy(alpha = 0.10f),
                topLeft = Offset(-size.width * 0.4f, size.height * 0.16f),
                size = Size(size.width * 1.8f, size.height * 0.80f),
                style = Stroke(1.dp.toPx()),
            )
            drawOval(
                color = SepticaColors.Gold.copy(alpha = 0.045f),
                topLeft = Offset(-size.width * 0.43f, size.height * 0.145f),
                size = Size(size.width * 1.86f, size.height * 0.83f),
                style = Stroke(1.dp.toPx()),
            )
            val step = 32.dp.toPx()
            val columns = (size.width / step).toInt().coerceAtMost(50)
            val rows = (size.height / step).toInt().coerceAtMost(60)
            for (column in 0..columns) {
                for (row in 0..rows) {
                    drawCircle(
                        SepticaColors.Cream.copy(alpha = 0.035f),
                        radius = 0.65.dp.toPx(),
                        center = Offset(column * step + if (row % 2 == 0) 0f else step / 2, row * step),
                    )
                }
            }
        }
        content()
    }
}
