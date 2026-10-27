package ro.septica.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
internal fun CardFace(
    rankSymbol: String,
    suitSymbol: String,
    isRed: Boolean,
    accessibilityLabel: String,
    selected: Boolean = false,
    muted: Boolean = false,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val selection by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 420f),
        label = "cardSelection",
    )
    val textMeasurer = rememberTextMeasurer()
    val shape = RoundedCornerShape(percent = 9)
    val ink = if (isRed) Color(0xFF8E2339) else Color(0xFF182C35)
    val gold = SepticaColors.Gold
    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = -6.dp.toPx() * selection
                scaleX = 1f + .025f * selection
                scaleY = scaleX
                alpha = if (muted) .56f else 1f
            }
            .shadow(if (selected) 10.dp else 4.dp, shape)
            .clip(shape)
            .drawWithCache {
                val paper = Brush.linearGradient(
                    listOf(
                        Color(0xFFFFFEF8),
                        if (selected) Color(0xFFFFF1CC) else Color(0xFFF7F1E5),
                        Color(0xFFE9DFC9),
                    ),
                    end = Offset(size.width, size.height),
                )
                val rank = textMeasurer.measure(
                    rankSymbol,
                    TextStyle(
                        color = ink,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    ),
                    softWrap = false,
                    maxLines = 1,
                )
                val number = rankSymbol.toIntOrNull()
                val joker = rankSymbol.startsWith("+") ||
                    rankSymbol.equals("JOKER", ignoreCase = true) || suitSymbol == "★"
                onDrawBehind {
                    if (size.minDimension <= 0f) return@onDrawBehind
                    val w = size.width
                    val h = size.height
                    drawRect(paper)
                    drawCardFrame(
                        if (selected) gold else Color(0xFFB9A581),
                        selected,
                    )
                    drawRankCorner(rank, suitSymbol, ink, compact)
                    rotate(180f) { drawRankCorner(rank, suitSymbol, ink, compact) }
                    when {
                        joker -> drawJokerEmblem(ink, gold)
                        number != null && number in 2..10 -> {
                            val pipSize = min(w * .17f, h * .145f)
                            CardEngravings.pips.getValue(number).forEach { point ->
                                val at = Offset(w * point.x, h * point.y)
                                if (point.y > .5f) {
                                    rotate(180f, at) {
                                        drawSuitGlyph(suitSymbol, ink, at, pipSize)
                                    }
                                } else {
                                    drawSuitGlyph(suitSymbol, ink, at, pipSize)
                                }
                            }
                        }
                        rankSymbol == "J" || rankSymbol == "Q" || rankSymbol == "K" -> {
                            drawRoyalEmblem(suitSymbol, ink, gold, rankSymbol)
                        }
                        else -> {
                            drawMedallion(gold.copy(alpha = .65f), .31f)
                            drawSuitGlyph(suitSymbol, ink, center, w * .43f)
                            drawSuitGlyph("♦", gold, Offset(w * .5f, h * .27f), w * .06f)
                            drawSuitGlyph("♦", gold, Offset(w * .5f, h * .73f), w * .06f)
                        }
                    }
                }
            }
            .semantics {
                contentDescription = accessibilityLabel
                this.selected = selected
            }
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier,
            ),
    )
}

@Composable
internal fun CardBack(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(percent = 9)
    val gold = SepticaColors.PaleGold
    Box(
        modifier = modifier
            .shadow(3.dp, shape)
            .clip(shape)
            .semantics { contentDescription = "Carte cu fața în jos" }
            .drawWithCache {
                val enamel = Brush.linearGradient(
                    listOf(Color(0xFF143F4A), Color(0xFF0B2233), Color(0xFF19535A)),
                    end = Offset(size.width, size.height),
                )
                onDrawBehind {
                    if (size.minDimension <= 0f) return@onDrawBehind
                    val w = size.width
                    val h = size.height
                    drawRect(enamel)
                    clipRect(w * .09f, h * .065f, w * .91f, h * .935f) {
                        // Two fixed families of diagonals create a woven, symmetric lattice.
                        for (index in -12..12) {
                            val x = index * w / 8f
                            drawLine(
                                gold.copy(alpha = .19f),
                                Offset(x, 0f),
                                Offset(x + w, h),
                                w * .006f,
                            )
                            drawLine(
                                gold.copy(alpha = .19f),
                                Offset(x, 0f),
                                Offset(x - w, h),
                                w * .006f,
                            )
                        }
                    }
                    drawCardFrame(gold, false)
                    drawOval(
                        Color(0xFF102F3E),
                        Offset(w * .18f, h * .265f),
                        Size(w * .64f, h * .47f),
                    )
                    drawMedallion(gold, .31f)
                    drawMedallion(gold.copy(alpha = .45f), .27f)
                    drawSuitGlyph("♠", gold, Offset(w * .5f, h * .39f), w * .16f)
                    drawSuitGlyph("♣", gold, Offset(w * .5f, h * .61f), w * .16f)
                    drawSuitGlyph("♥", gold, Offset(w * .35f, h * .5f), w * .15f)
                    drawSuitGlyph("♦", gold, Offset(w * .65f, h * .5f), w * .15f)
                    drawSuitGlyph("♦", gold, center, w * .065f)
                    drawSuitGlyph("♦", gold, Offset(w * .5f, h * .14f), w * .11f)
                    drawSuitGlyph("♦", gold, Offset(w * .5f, h * .86f), w * .11f)
                }
            },
    )
}

private fun DrawScope.drawCardFrame(color: Color, selected: Boolean) {
    val w = size.width
    val h = size.height
    val edge = w * .016f
    drawRoundRect(
        color,
        Offset(edge, edge),
        Size(w - edge * 2, h - edge * 2),
        CornerRadius(w * .08f),
        style = Stroke(w * if (selected) .035f else .012f),
    )
    val inset = w * .07f
    drawRoundRect(
        color.copy(alpha = .45f),
        Offset(inset, inset),
        Size(w - inset * 2, h - inset * 2),
        CornerRadius(w * .025f),
        style = Stroke(w * .006f),
    )
    for (index in 0..1) {
        rotate(index * 180f) {
            drawLine(color.copy(alpha = .65f), Offset(w * .38f, inset), Offset(w * .62f, inset), w * .013f)
            drawSuitGlyph("♦", color, Offset(w * .5f, inset), w * .045f)
        }
    }
}

private fun DrawScope.drawRankCorner(
    rank: TextLayoutResult,
    suit: String,
    ink: Color,
    compact: Boolean,
) {
    val w = size.width
    val h = size.height
    val width = w * .25f
    val height = min(h * .155f, w * if (compact) .25f else .27f)
    val factor = min(width / rank.size.width.coerceAtLeast(1), height / rank.size.height.coerceAtLeast(1))
    val x = w * .09f + (width - rank.size.width * factor) / 2f
    withTransform({
        translate(x, h * .055f)
        scale(factor, factor, Offset.Zero)
    }) {
        drawText(rank)
    }
    drawSuitGlyph(suit, ink, Offset(w * .215f, h * .055f + height + w * .075f), w * .135f)
}

private fun DrawScope.drawMedallion(color: Color, radius: Float) {
    val rx = size.width * radius
    val ry = size.height * radius * .73f
    drawOval(
        color,
        Offset(center.x - rx, center.y - ry),
        Size(rx * 2, ry * 2),
        style = Stroke(size.width * .009f),
    )
}

private fun DrawScope.drawRoyalEmblem(suit: String, ink: Color, gold: Color, rank: String) {
    val w = size.width
    val h = size.height
    drawOval(gold.copy(alpha = .10f), Offset(w * .2f, h * .27f), Size(w * .6f, h * .46f))
    drawMedallion(gold, .31f)
    drawMedallion(gold.copy(alpha = .5f), .275f)
    withTransform({
        translate(w * .5f, h * .42f)
        scale(w * .4f, w * .4f, Offset.Zero)
    }) {
        drawPath(CardEngravings.crown, gold.copy(alpha = .16f))
        drawPath(CardEngravings.crown, ink, style = Stroke(.035f))
    }
    drawSuitGlyph(suit, ink, Offset(w * .5f, h * .57f), w * .24f)
    drawSuitGlyph(if (rank == "Q") "♦" else "★", gold, Offset(w * .5f, h * .34f), w * .085f)
    drawLine(gold, Offset(w * .37f, h * .66f), Offset(w * .63f, h * .66f), w * .012f)
    if (rank == "K") {
        drawLine(ink, Offset(w * .5f, h * .29f), Offset(w * .5f, h * .33f), w * .015f)
        drawLine(ink, Offset(w * .475f, h * .305f), Offset(w * .525f, h * .305f), w * .015f)
    }
}

private fun DrawScope.drawJokerEmblem(ink: Color, gold: Color) {
    drawMedallion(gold, .32f)
    rotate(22.5f) { drawSuitGlyph("♦", gold.copy(alpha = .22f), center, size.width * .66f) }
    drawSuitGlyph("★", gold, center, size.width * .49f)
    drawSuitGlyph("♦", ink, center, size.width * .18f)
    drawSuitGlyph("♦", ink, Offset(center.x, size.height * .27f), size.width * .075f)
    drawSuitGlyph("♦", ink, Offset(center.x, size.height * .73f), size.width * .075f)
}

internal fun DrawScope.drawSuitGlyph(symbol: String, color: Color, center: Offset, glyphSize: Float) {
    val path = when (symbol) {
        "♥" -> CardEngravings.heart
        "♦" -> CardEngravings.diamond
        "♣" -> CardEngravings.club
        "♠" -> CardEngravings.spade
        else -> CardEngravings.star
    }
    withTransform({
        translate(center.x, center.y)
        scale(glyphSize, glyphSize, Offset.Zero)
    }) {
        drawPath(path, color)
    }
}

// Unit-space engravings are shared by every card; drawing never rebuilds paths.
private object CardEngravings {
    val heart = Path().apply {
        moveTo(0f, .48f)
        cubicTo(-.12f, .30f, -.5f, .02f, -.5f, -.20f)
        cubicTo(-.5f, -.52f, -.14f, -.59f, 0f, -.30f)
        cubicTo(.14f, -.59f, .5f, -.52f, .5f, -.20f)
        cubicTo(.5f, .02f, .12f, .30f, 0f, .48f)
        close()
    }
    val diamond = Path().apply {
        moveTo(0f, -.5f)
        lineTo(.35f, 0f)
        lineTo(0f, .5f)
        lineTo(-.35f, 0f)
        close()
    }
    val spade = Path().apply {
        moveTo(0f, -.5f)
        cubicTo(.14f, -.27f, .48f, -.04f, .48f, .15f)
        cubicTo(.48f, .42f, .16f, .44f, .06f, .22f)
        cubicTo(.07f, .36f, .11f, .43f, .21f, .5f)
        lineTo(-.21f, .5f)
        cubicTo(-.11f, .43f, -.07f, .36f, -.06f, .22f)
        cubicTo(-.16f, .44f, -.48f, .42f, -.48f, .15f)
        cubicTo(-.48f, -.04f, -.14f, -.27f, 0f, -.5f)
        close()
    }
    val club = Path().apply {
        moveTo(-.09f, -.03f)
        cubicTo(-.39f, -.32f, -.23f, -.5f, 0f, -.5f)
        cubicTo(.23f, -.5f, .39f, -.32f, .09f, -.03f)
        cubicTo(.44f, -.22f, .62f, .13f, .39f, .31f)
        cubicTo(.23f, .43f, .09f, .29f, .05f, .18f)
        cubicTo(.05f, .35f, .12f, .45f, .21f, .5f)
        lineTo(-.21f, .5f)
        cubicTo(-.12f, .45f, -.05f, .35f, -.05f, .18f)
        cubicTo(-.09f, .29f, -.23f, .43f, -.39f, .31f)
        cubicTo(-.62f, .13f, -.44f, -.22f, -.09f, -.03f)
        close()
    }
    val star = Path().apply {
        moveTo(0f, -.5f)
        lineTo(.12f, -.16f)
        lineTo(.48f, -.15f)
        lineTo(.20f, .07f)
        lineTo(.30f, .43f)
        lineTo(0f, .22f)
        lineTo(-.30f, .43f)
        lineTo(-.20f, .07f)
        lineTo(-.48f, -.15f)
        lineTo(-.12f, -.16f)
        close()
    }
    val crown = Path().apply {
        moveTo(-.40f, .20f)
        lineTo(-.5f, -.28f)
        lineTo(-.23f, -.08f)
        lineTo(0f, -.42f)
        lineTo(.23f, -.08f)
        lineTo(.5f, -.28f)
        lineTo(.40f, .20f)
        close()
        moveTo(-.39f, .30f)
        lineTo(.39f, .30f)
        moveTo(-.22f, -.06f)
        lineTo(-.17f, .20f)
        moveTo(.22f, -.06f)
        lineTo(.17f, .20f)
    }
    val pips: Map<Int, List<Offset>> = (2..10).associateWith { count ->
        buildList {
            if (count <= 3) {
                add(Offset(.5f, .26f))
                add(Offset(.5f, .74f))
                if (count == 3) add(Offset(.5f, .5f))
            } else {
                val rows = when (count) {
                    in 4..5 -> listOf(.27f, .73f)
                    in 6..8 -> listOf(.27f, .5f, .73f)
                    else -> listOf(.26f, .42f, .58f, .74f)
                }
                rows.forEach { y ->
                    add(Offset(.38f, y))
                    add(Offset(.62f, y))
                }
                when (count) {
                    5, 9 -> add(Offset(.5f, .5f))
                    7 -> add(Offset(.5f, .385f))
                    8 -> {
                        add(Offset(.5f, .385f))
                        add(Offset(.5f, .615f))
                    }
                    10 -> {
                        add(Offset(.5f, .34f))
                        add(Offset(.5f, .66f))
                    }
                }
            }
        }
    }
}
