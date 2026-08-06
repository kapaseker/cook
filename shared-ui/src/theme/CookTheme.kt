package theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import page.settings.biz.scaledDensity
import androidx.compose.material3.lightColorScheme

private val CookColorScheme = lightColorScheme(
    primary = Color(0xFF99462A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD97757),
    onPrimaryContainer = Color(0xFF541400),
    secondary = Color(0xFF486459),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E7D9),
    onSecondaryContainer = Color(0xFF314C42),
    tertiary = Color(0xFF7C5800),
    onTertiary = Color.White,
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF1E1B18),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF1E1B18),
    surfaceVariant = Color(0xFFE9E1DC),
    onSurfaceVariant = Color(0xFF55433D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF2ED),
    surfaceContainer = Color(0xFFF5ECE7),
    surfaceContainerHigh = Color(0xFFEFE6E2),
    surfaceContainerHighest = Color(0xFFE9E1DC),
    outline = Color(0xFF88726C),
    outlineVariant = Color(0xFFDBC1B9),
)

private val CookTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.64).sp,
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = Typography().titleLarge.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
    ),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 18.sp, lineHeight = 28.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = Typography().labelLarge.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.14.sp,
    ),
    labelSmall = Typography().labelSmall.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
    ),
)

/** Applies the Material theme with independent text and UI scales. */
@Composable
fun CookTheme(
    textScale: Float,
    uiScale: Float,
    content: @Composable () -> Unit,
) {
    val systemDensity = LocalDensity.current

    CompositionLocalProvider(
        LocalDensity provides scaledDensity(systemDensity, textScale, uiScale),
    ) {
        MaterialTheme(
            colorScheme = CookColorScheme,
            typography = CookTypography,
            content = content,
        )
    }
}
