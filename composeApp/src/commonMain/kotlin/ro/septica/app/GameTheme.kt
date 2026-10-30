package ro.septica.app

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class GameThemeTokens(
    val background: Color,
    val onBackground: Color,
    val table: Color,
    val felt: Color,
    val surface: Color,
    val onSurface: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val typography: Typography = gameTypography,
    val spacing: GameSpacingTokens = GameSpacingTokens(),
    val shapes: GameShapeTokens = GameShapeTokens(),
    val motion: GameMotionTokens = GameMotionTokens(),
)

internal data class GameSpacingTokens(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
)

internal data class GameShapeTokens(
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
)

internal data class GameMotionTokens(
    val shortMillis: Int = 120,
    val mediumMillis: Int = 220,
    val longMillis: Int = 360,
)

private val gameTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.5.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.3.sp,
    ),
)

private val classicRomanianTokens = GameThemeTokens(
    background = Color(0xFF07131A),
    onBackground = Color(0xFFF0F3F4),
    table = Color(0xFF123D38),
    felt = Color(0xFF287064),
    surface = Color(0xFF19343A),
    onSurface = Color(0xFFF0F3F4),
    primary = Color(0xFFEBC27D),
    onPrimary = Color(0xFF172537),
    primaryContainer = Color(0xFF664812),
    onPrimaryContainer = Color(0xFFFFE2AE),
    secondary = Color(0xFFFFE2AE),
    onSecondary = Color(0xFF172537),
    secondaryContainer = Color(0xFF254A4B),
    onSecondaryContainer = Color(0xFFF0F3F4),
    surfaceVariant = Color(0xFF203A40),
    onSurfaceVariant = Color(0xFFB1C8CC),
    outline = Color(0xFF8AABB0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFFAE344A),
    onErrorContainer = Color(0xFFFFF9EE),
)

private val nightTokens = GameThemeTokens(
    background = Color(0xFF080B18),
    onBackground = Color(0xFFF4F7FF),
    table = Color(0xFF101A33),
    felt = Color(0xFF281E50),
    surface = Color(0xFF151A2C),
    onSurface = Color(0xFFF4F7FF),
    primary = Color(0xFF5ED8E8),
    onPrimary = Color(0xFF06202B),
    primaryContainer = Color(0xFF124255),
    onPrimaryContainer = Color(0xFFBAF4FB),
    secondary = Color(0xFFB9A5FF),
    onSecondary = Color(0xFF211641),
    secondaryContainer = Color(0xFF34265E),
    onSecondaryContainer = Color(0xFFE6DEFF),
    surfaceVariant = Color(0xFF2D3149),
    onSurfaceVariant = Color(0xFFC1CAE5),
    outline = Color(0xFF8797C6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF7A1B3A),
    onErrorContainer = Color(0xFFFFD9E4),
)

private val retroTokens = GameThemeTokens(
    background = Color(0xFF261811),
    onBackground = Color(0xFFFFF4DC),
    table = Color(0xFF4A2D20),
    felt = Color(0xFF68452F),
    surface = Color(0xFF3A271F),
    onSurface = Color(0xFFFFF4DC),
    primary = Color(0xFFD89A5A),
    onPrimary = Color(0xFF2C1C16),
    primaryContainer = Color(0xFF70472E),
    onPrimaryContainer = Color(0xFFFFE1BE),
    secondary = Color(0xFFE7C5A3),
    onSecondary = Color(0xFF2C1C16),
    secondaryContainer = Color(0xFF624454),
    onSecondaryContainer = Color(0xFFFFF4DC),
    surfaceVariant = Color(0xFF533A2F),
    onSurfaceVariant = Color(0xFFD8C0AE),
    outline = Color(0xFFAD8B75),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F0015),
    errorContainer = Color(0xFF8C2638),
    onErrorContainer = Color(0xFFFFE1E5),
)

private val minimalTokens = GameThemeTokens(
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF182231),
    table = Color(0xFFDCE4EF),
    felt = Color(0xFFE9EDF3),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF182231),
    primary = Color(0xFF2457C5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF16336F),
    secondary = Color(0xFF6A4BC4),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9E1FF),
    onSecondaryContainer = Color(0xFF2F1B63),
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = Color(0xFF4F5B6E),
    outline = Color(0xFF68778D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val highContrastTokens = GameThemeTokens(
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    table = Color(0xFF050505),
    felt = Color(0xFF141414),
    surface = Color(0xFF050505),
    onSurface = Color(0xFFFFFFFF),
    primary = Color(0xFFFFFF00),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF514800),
    onPrimaryContainer = Color(0xFFFFFF00),
    secondary = Color(0xFF00FFFF),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF004F4F),
    onSecondaryContainer = Color(0xFF00FFFF),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFFFFFFF),
    outline = Color(0xFFFFFFFF),
    error = Color(0xFFFF5C5C),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF5A0000),
    onErrorContainer = Color(0xFFFFFFFF),
)

internal enum class GameTheme(
    val storageId: String,
    val displayName: String,
) {
    CLASSIC_ROMANIAN("classic_romanian", "Clasic românesc"),
    NIGHT("night", "Noapte"),
    RETRO("retro", "Retro"),
    MINIMAL("minimal", "Minimal"),
    HIGH_CONTRAST("high_contrast", "Contrast ridicat"),
    ;

    val tokens: GameThemeTokens
        get() = when (this) {
            CLASSIC_ROMANIAN -> classicRomanianTokens
            NIGHT -> nightTokens
            RETRO -> retroTokens
            MINIMAL -> minimalTokens
            HIGH_CONTRAST -> highContrastTokens
        }

    companion object {
        fun fromStorageId(storageId: String?): GameTheme =
            entries.firstOrNull { it.storageId == storageId } ?: CLASSIC_ROMANIAN
    }
}

internal val LocalGameThemeTokens = staticCompositionLocalOf { GameTheme.CLASSIC_ROMANIAN.tokens }
