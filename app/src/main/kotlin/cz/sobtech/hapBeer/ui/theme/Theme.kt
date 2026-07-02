package cz.sobtech.hapBeer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BeerAmber,
    onPrimary = BeerOnBackground,
    secondary = BeerWood,
    onSecondary = BeerFoam,
    tertiary = BeerFoam,
    onTertiary = BeerOnBackground,
    background = BeerBackground,
    onBackground = BeerOnBackground,
    surface = BeerSurface,
    onSurface = BeerOnBackground,
    surfaceVariant = BeerSurfaceVariant,
    onSurfaceVariant = BeerOnSurfaceVariant,
    error = Color(0xFFB00020),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BeerAmberLight,
    onPrimary = Color(0xFF1C1410),
    secondary = BeerWoodLight,
    onSecondary = Color(0xFF1C1410),
    tertiary = BeerFoamDark,
    onTertiary = Color(0xFF1C1410),
    background = BeerBackgroundDark,
    onBackground = BeerOnBackgroundDark,
    surface = BeerSurfaceDark,
    onSurface = BeerOnBackgroundDark,
    surfaceVariant = BeerSurfaceVariantDark,
    onSurfaceVariant = BeerOnSurfaceVariantDark,
    error = Color(0xFFCF6679),
    onError = Color(0xFF1C1410)
)

private val BeerShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun HapBeerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = BeerShapes,
        content = content
    )
}
