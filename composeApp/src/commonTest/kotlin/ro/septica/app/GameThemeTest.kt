package ro.septica.app

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameThemeTest {
    @Test
    fun exposesTheFiveThemesInApprovedOrder() {
        assertEquals(
            listOf(
                "classic_romanian" to "Clasic românesc",
                "night" to "Noapte",
                "retro" to "Retro",
                "minimal" to "Minimal",
                "high_contrast" to "Contrast ridicat",
            ),
            GameTheme.entries.map { it.storageId to it.displayName },
        )
    }

    @Test
    fun missingOrUnknownStorageIdUsesClassicRomanian() {
        assertEquals(GameTheme.CLASSIC_ROMANIAN, GameTheme.fromStorageId(null))
        assertEquals(GameTheme.CLASSIC_ROMANIAN, GameTheme.fromStorageId("unrecognized"))
    }

    @Test
    fun allPalettesMeetTextAndControlContrastTargets() {
        GameTheme.entries.forEach { theme ->
            val tokens = theme.tokens
            listOf(
                tokens.onBackground to tokens.background,
                tokens.onBackground to tokens.table,
                tokens.onBackground to tokens.felt,
                tokens.onSurface to tokens.surface,
                tokens.onSurfaceVariant to tokens.surfaceVariant,
                tokens.onPrimary to tokens.primary,
                tokens.onPrimaryContainer to tokens.primaryContainer,
                tokens.onSecondary to tokens.secondary,
                tokens.onSecondaryContainer to tokens.secondaryContainer,
                tokens.onError to tokens.error,
                tokens.onErrorContainer to tokens.errorContainer,
            ).forEach { (foreground, background) ->
                assertTrue(
                    contrastRatio(foreground, background) >= 4.5,
                    "${theme.displayName} does not meet normal text contrast on $background",
                )
            }

            assertTrue(
                contrastRatio(tokens.outline, tokens.surface) >= 3.0,
                "${theme.displayName} does not meet control-boundary contrast on ${tokens.surface}",
            )
        }
    }
}

private fun contrastRatio(first: Color, second: Color): Double {
    val firstLuminance = relativeLuminance(first)
    val secondLuminance = relativeLuminance(second)
    val lighter = maxOf(firstLuminance, secondLuminance)
    val darker = minOf(firstLuminance, secondLuminance)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(color: Color): Double {
    fun linearize(channel: Float): Double {
        val value = channel.toDouble()
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    return 0.2126 * linearize(color.red) +
        0.7152 * linearize(color.green) +
        0.0722 * linearize(color.blue)
}
