package ch.castellon.oigo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val scheme = darkColorScheme(
    primary = Color(0xFFE7F0EA),
    onPrimary = Color(0xFF102018),
    background = Color(0xFF101418),
    surface = Color(0xFF1A2128),
    onBackground = Color(0xFFE7F0EA),
    onSurface = Color(0xFFE7F0EA),
)

@Composable
fun GrokTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
