package app.writer.features.launch

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.writer.core.tr
import app.writer.design.Accents
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterShapes
import app.writer.design.WriterType
import app.writer.design.components.GlassButton
import app.writer.design.components.GlassButtonStyle
import app.writer.design.components.GlassText
import app.writer.design.components.glassPanel
import app.writer.design.motionMs
import app.writer.design.rememberReducedMotion
import kotlinx.coroutines.launch

private class Slide(
    val icon: ImageVector,
    val titleHi: String,
    val titleEn: String,
    val descHi: String,
    val descEn: String,
)

private val slides = listOf(
    Slide(
        Icons.Rounded.EditNote,
        "समझदारी से लिखें", "Write Smarter",
        "कुछ ही मिनटों में पेशेवर पत्र और आवेदन तैयार करें।", "Create professional letters and applications in minutes.",
    ),
    Slide(
        Icons.Rounded.Description,
        "तैयार टेम्पलेट्स", "Ready-to-Use Templates",
        "हिंदी और English के उपयोगी दस्तावेज़ टेम्पलेट्स में से चुनें।", "Choose from useful Hindi and English document templates.",
    ),
    Slide(
        Icons.Rounded.PictureAsPdf,
        "PDF बनाएं और प्रिंट करें", "Export and Print",
        "अपने दस्तावेज़ PDF में सेव करें और A4 फ़ॉर्मेट में प्रिंट करें।", "Save your documents as PDFs and print them in A4 format.",
    ),
)

/** Three-slide first-launch introduction. Skippable at any time; shown once. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val c = Glass.colors
    val pager = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == slides.lastIndex

    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = Spacing.x2),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!last) {
                GlassButton(
                    text = tr("छोड़ें", "Skip"),
                    onClick = onFinish,
                    style = GlassButtonStyle.Secondary,
                    modifier = Modifier.width(112.dp),
                )
            }
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { index ->
            val slide = slides[index]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.x4),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .glassPanel(shape = WriterShapes.cardLarge, elevated = true),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = slide.icon,
                        contentDescription = null,
                        tint = c.electric,
                        modifier = Modifier.size(84.dp),
                    )
                }
                GlassText(
                    text = tr(slide.titleHi, slide.titleEn),
                    style = WriterType.display,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.x4),
                )
                GlassText(
                    text = tr(slide.descHi, slide.descEn),
                    style = WriterType.body,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.x1_5),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.x2, vertical = Spacing.x2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            PageDots(count = slides.size, selected = pager.currentPage)
            GlassButton(
                text = if (last) tr("शुरू करें", "Get Started") else tr("आगे", "Next"),
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                onClick = {
                    if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
            )
        }
    }
}

@Composable
private fun PageDots(count: Int, selected: Int) {
    val c = Glass.colors
    val reduced = rememberReducedMotion()
    val position = tr("स्लाइड ${selected + 1} / $count", "Slide ${selected + 1} of $count")
    Row(
        modifier = Modifier.semantics { contentDescription = position },
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == selected
            val width by animateDpAsState(if (active) 28.dp else 8.dp, tween(motionMs(reduced, 220)), label = "dotWidth")
            val color by animateColorAsState(if (active) c.electric else c.textMuted.copy(alpha = 0.5f), tween(motionMs(reduced, 220)), label = "dotColor")
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(WriterShapes.pill)
                    .background(color),
            )
        }
    }
}
