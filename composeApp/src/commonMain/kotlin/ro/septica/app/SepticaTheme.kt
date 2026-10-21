package ro.septica.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal object SepticaColors {
    val DeepGreen = Color(0xFF080F19)
    val TableGreen = Color(0xFF123732)
    val Felt = Color(0xFF215B50)
    val Panel = Color(0xFF142B30)
    val Gold = Color(0xFFE8BD76)
    val PaleGold = Color(0xFFFFDFA5)
    val Cream = Color(0xFFFFF9EE)
    val Ink = Color(0xFF172537)
    val Burgundy = Color(0xFFAE344A)
    val SoftWhite = Color(0xFFF0F3F4)
    val Muted = Color(0xFFA7BDC2)
}

private val SepticaColorScheme = darkColorScheme(
    primary = SepticaColors.Gold,
    onPrimary = SepticaColors.Ink,
    primaryContainer = Color(0xFF664812),
    onPrimaryContainer = SepticaColors.PaleGold,
    secondary = SepticaColors.PaleGold,
    secondaryContainer = Color(0xFF254A4B),
    background = SepticaColors.DeepGreen,
    onBackground = SepticaColors.SoftWhite,
    surface = SepticaColors.Panel,
    onSurface = SepticaColors.SoftWhite,
    surfaceVariant = Color(0xFF203A40),
    onSurfaceVariant = SepticaColors.Muted,
    outline = Color(0xFF668385),
    error = Color(0xFFFFB4AB),
)

private val SepticaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 34.sp,
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

@Composable
internal fun SepticaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SepticaColorScheme,
        typography = SepticaTypography,
        content = content,
    )
}
