package ro.septica.app

import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals

class SepticaThemeTest {
    @Test
    fun minimalUsesLightSchemeAndRemainingThemesUseDarkSchemes() {
        GameTheme.entries.forEach { theme ->
            val colorScheme = theme.toColorScheme()
            val usesLightScheme = colorScheme.inverseSurface.luminance() < colorScheme.background.luminance()

            assertEquals(
                theme == GameTheme.MINIMAL,
                usesLightScheme,
                "${theme.displayName} uses the wrong Material color scheme mode",
            )
        }
    }
}
