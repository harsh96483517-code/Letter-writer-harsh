package app.writer.design

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.writer.model.ThemeMode

val LocalGlassColors = staticCompositionLocalOf { DarkGlassColors }

/** Shortcut: `Glass.colors.primary`. */
object Glass {
    val colors: GlassColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current
}

@Composable
fun resolveDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun WriterTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkGlassColors else LightGlassColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            secondary = colors.purple,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.sheet,
            onSurface = colors.textPrimary,
            error = colors.error,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            secondary = colors.purple,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.sheet,
            onSurface = colors.textPrimary,
            error = colors.error,
        )
    }
    SystemBarsEffect(darkTheme)
    CompositionLocalProvider(LocalGlassColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Keeps status/navigation bar icons readable in whichever theme is active. */
@Composable
private fun SystemBarsEffect(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
