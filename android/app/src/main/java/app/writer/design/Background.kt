package app.writer.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The layered backdrop that gives glass something to refract: a vertical base gradient plus
 * three soft radial light pools. It is drawn once and never animates, so it costs almost
 * nothing on mid-range phones (no real-time backdrop blur anywhere in the app).
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = Glass.colors
    val boost = if (c.isDark) 1f else 0.75f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.background, c.backgroundSecondary)))
            .drawBehind {
                fun pool(color: Color, cx: Float, cy: Float, r: Float, alpha: Float) {
                    val center = Offset(size.width * cx, size.height * cy)
                    val radius = size.width * r
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = alpha * boost), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
                pool(c.primary, 0.10f, 0.04f, 0.95f, 0.30f)
                pool(c.purple, 0.98f, 0.38f, 0.85f, 0.22f)
                pool(c.electric, 0.15f, 0.96f, 0.95f, 0.16f)
            },
        content = content,
    )
}
