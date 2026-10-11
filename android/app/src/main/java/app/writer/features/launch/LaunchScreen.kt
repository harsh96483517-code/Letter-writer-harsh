package app.writer.features.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.writer.core.tr
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterType
import app.writer.design.components.GlassText
import app.writer.design.components.WriterLogo
import app.writer.design.motionMs
import app.writer.design.rememberReducedMotion
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * Short launch screen (about a second): the logo fades in with a slight scale-up while a soft
 * blue-purple light sweeps across. It never blocks on anything slow; [ready] only holds it
 * until settings have been read, so the right theme and language show from the first frame.
 */
@Composable
fun LaunchScreen(
    ready: Boolean,
    onFinished: () -> Unit,
) {
    val c = Glass.colors
    val reduced = rememberReducedMotion()
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.9f) }
    val sweep = remember { Animatable(0f) }

    LaunchedEffect(reduced) {
        coroutineScope {
            val a = async { alpha.animateTo(1f, tween(motionMs(reduced, 520), easing = FastOutSlowInEasing)) }
            val s = async { scale.animateTo(1f, tween(motionMs(reduced, 620), easing = FastOutSlowInEasing)) }
            val w = async {
                delay(motionMs(reduced, 150).toLong())
                sweep.animateTo(1f, tween(motionMs(reduced, 900), easing = FastOutSlowInEasing))
            }
            a.await(); s.await(); w.await()
        }
    }
    LaunchedEffect(ready, reduced) {
        if (ready) {
            delay(motionMs(reduced, 1050).toLong())
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Soft light band travelling from left to right behind the logo.
                val x = size.width * (-0.3f + 1.6f * sweep.value)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, c.primary.copy(alpha = 0.22f), c.purple.copy(alpha = 0.18f), Color.Transparent),
                        start = Offset(x - size.width * 0.35f, 0f),
                        end = Offset(x + size.width * 0.35f, size.height * 0.45f),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value),
        ) {
            WriterLogo(size = 112.dp)
            GlassText(text = "Writer", style = WriterType.display)
            GlassText(
                text = tr("लिखें • आवेदन करें • परिणाम पाएं", "Write • Apply • Get Results"),
                style = WriterType.bodySmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
