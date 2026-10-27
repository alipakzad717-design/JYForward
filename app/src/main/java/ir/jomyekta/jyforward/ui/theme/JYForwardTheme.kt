package ir.jomyekta.jyforward.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import ir.jomyekta.jyforward.data.local.AppLanguageMode
import ir.jomyekta.jyforward.data.local.AppThemeMode
import java.util.Locale

private val LightColors = lightColorScheme(
    primary = Color(0xFF0D47A1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00201C),
    background = Color(0xFFF8FAFC),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color(0xFF002F6C),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFFB2DFDB),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B)
)

@Composable
fun JYForwardTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    languageMode: AppLanguageMode = AppLanguageMode.SYSTEM,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) DarkColors else LightColors

    // Determine effective RTL/LTR direction based on selected or system locale
    val isPersian = when (languageMode) {
        AppLanguageMode.PERSIAN -> true
        AppLanguageMode.ENGLISH -> false
        AppLanguageMode.SYSTEM -> Locale.getDefault().language.startsWith("fa")
    }
    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * fontScale.coerceIn(0.85f, 1.35f)
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}
