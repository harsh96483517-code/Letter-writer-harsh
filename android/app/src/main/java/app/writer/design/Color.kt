package app.writer.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Every color the Liquid Glass design system uses, for one theme. */
@Immutable
data class GlassColors(
    val isDark: Boolean,
    val background: Color,
    val backgroundSecondary: Color,
    /** Default translucent surface. */
    val glass: Color,
    /** Raised translucent surface. */
    val glassElevated: Color,
    val glassBorder: Color,
    /** Mostly opaque surface for sheets, dialogs, menus and the floating navigation bar. */
    val sheet: Color,
    val primary: Color,
    val electric: Color,
    val indigo: Color,
    val purple: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val onAccent: Color,
) {
    /** Blue → indigo → purple. Used for the most important actions only. */
    val accentBrush: Brush
        get() = Brush.linearGradient(listOf(primary, indigo, purple))
}

val DarkGlassColors = GlassColors(
    isDark = true,
    background = Color(0xFF050816),
    backgroundSecondary = Color(0xFF0A1022),
    glass = Color(0xFFFFFFFF).copy(alpha = 0.06f),
    glassElevated = Color(0xFFFFFFFF).copy(alpha = 0.10f),
    glassBorder = Color(0xFFFFFFFF).copy(alpha = 0.14f),
    sheet = Color(0xFF0E1630),
    primary = Color(0xFF3478F6),
    electric = Color(0xFF48A5FF),
    indigo = Color(0xFF5E5CE6),
    purple = Color(0xFF8557FF),
    textPrimary = Color(0xFFF7F8FF),
    textSecondary = Color(0xFFA5AEC4),
    textMuted = Color(0xFF737E97),
    success = Color(0xFF37D6A0),
    warning = Color(0xFFFFBF69),
    error = Color(0xFFFF647C),
    onAccent = Color.White,
)

val LightGlassColors = GlassColors(
    isDark = false,
    background = Color(0xFFF4F6FC),
    backgroundSecondary = Color(0xFFE9EEFB),
    glass = Color(0xFFFFFFFF).copy(alpha = 0.72f),
    glassElevated = Color(0xFFFFFFFF).copy(alpha = 0.90f),
    glassBorder = Color(0xFF1E2D50).copy(alpha = 0.10f),
    sheet = Color(0xFFFCFDFF),
    primary = Color(0xFF3F7FF7),
    electric = Color(0xFF2D7DE8),
    indigo = Color(0xFF6270F5),
    purple = Color(0xFF8660F7),
    textPrimary = Color(0xFF151B2C),
    textSecondary = Color(0xFF647089),
    textMuted = Color(0xFF7B86A0),
    success = Color(0xFF14A97A),
    warning = Color(0xFFD98A1F),
    error = Color(0xFFE23D5A),
    onAccent = Color.White,
)

/** Restrained accent colors for categories and file types. */
object Accents {
    val Blue = Color(0xFF3478F6)
    val Green = Color(0xFF2FBF8F)
    val Purple = Color(0xFF8557FF)
    val Orange = Color(0xFFFF9A3D)
    val Pink = Color(0xFFE8579B)
    val Teal = Color(0xFF22B8CF)
    val Red = Color(0xFFFF647C)
    val Amber = Color(0xFFF2B33D)
}
