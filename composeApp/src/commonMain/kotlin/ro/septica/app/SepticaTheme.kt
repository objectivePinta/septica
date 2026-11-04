package ro.septica.app

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

internal fun GameTheme.toColorScheme(): ColorScheme = tokens.toColorScheme(isLight = this == GameTheme.MINIMAL)

private fun GameThemeTokens.toColorScheme(isLight: Boolean): ColorScheme {
    val scheme = if (isLight) {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = secondary,
            onTertiary = onSecondary,
            tertiaryContainer = secondaryContainer,
            onTertiaryContainer = onSecondaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outline,
            surfaceTint = primary,
        )
    } else {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = secondary,
            onTertiary = onSecondary,
            tertiaryContainer = secondaryContainer,
            onTertiaryContainer = onSecondaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outline,
            surfaceTint = primary,
        )
    }

    return scheme
}

@Composable
internal fun SepticaTheme(theme: GameTheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGameThemeTokens provides theme.tokens) {
        MaterialTheme(
            colorScheme = theme.toColorScheme(),
            typography = theme.tokens.typography,
            content = content,
        )
    }
}
