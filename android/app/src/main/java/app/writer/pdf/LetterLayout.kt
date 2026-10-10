package app.writer.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import app.writer.common.LetterText
import app.writer.model.AppLanguage
import app.writer.model.BodyAlign
import app.writer.model.LetterStyle
import app.writer.model.TextSpan
import app.writer.model.WriterDocument

/** A run of lines from one text block, placed at a position on a page. All units are PDF points. */
class PageSlice(
    val layout: StaticLayout,
    val startLine: Int,
    val endLine: Int,
    val x: Float,
    val y: Float,
    val width: Float,
)

class LaidOutPage(val slices: List<PageSlice>)

class LaidOutDocument(
    val widthPt: Float,
    val heightPt: Float,
    val pages: List<LaidOutPage>,
    /** True if a bundled font could not be loaded and the system font was used instead. */
    val usedFallbackFonts: Boolean,
) {
    val pageCount: Int get() = pages.size
}

/**
 * The letter layout engine. It turns a [WriterDocument] into real laid-out text on paper-sized
 * pages. The on-screen preview and the PDF both draw the result of this one function, so they
 * cannot disagree about line breaks or page breaks.
 *
 * Rules: margins from the document; automatic wrapping; paragraph spacing; no single line left
 * alone at the top or bottom of a page; the subject and salutation are kept with the text that
 * follows; the closing and signature stay together and bring at least two lines of the letter
 * with them, so a signature never sits alone on a page.
 */
class LetterEngine(context: Context) {
    private val fonts = LetterFonts(context.applicationContext)

    private class Block(
        val layout: StaticLayout,
        val x: Float,
        val width: Float,
        val spaceBefore: Float,
        val keepTogether: Boolean,
        val keepWithNext: Boolean,
        val pullPrevious: Int,
        val pullable: Boolean,
    )

    private class Placed(
        val layout: StaticLayout,
        var start: Int,
        var end: Int,
        val x: Float,
        var y: Float,
        val width: Float,
        val pullable: Boolean,
    )

    fun layout(doc: WriterDocument): LaidOutDocument {
        val f = doc.formatting
        val pt = 72f / 25.4f
        val pageW = f.paper.widthMm * pt
        val pageH = f.paper.heightMm * pt
        val left = f.margins.leftMm * pt
        val right = f.margins.rightMm * pt
        val top = f.margins.topMm * pt
        val bottom = pageH - f.margins.bottomMm * pt
        val contentW = (pageW - left - right).coerceAtLeast(60f)

        val hindi = doc.language == AppLanguage.HI
        val defaultScript = if (hindi) Script.DEVANAGARI else Script.LATIN
        val size = f.fontSizePt
        val lineGap = size * f.lineSpacing

        fun layoutOf(text: CharSequence, width: Float, align: Layout.Alignment, justify: Boolean): StaticLayout {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                color = Color.BLACK
                typeface = fonts.typeface(defaultScript, f.font, bold = false, italic = false)
            }
            val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
                .setAlignment(align)
                .setLineSpacing(0f, f.lineSpacing)
                .setIncludePad(true)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            if (justify) builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
            if (Build.VERSION.SDK_INT >= 28) builder.setUseLineSpacingFromFallbacks(true)
            return builder.build()
        }

        val blocks = mutableListOf<Block>()
        fun add(
            text: String,
            spans: List<TextSpan> = emptyList(),
            bold: Boolean = false,
            align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
            justify: Boolean = false,
            spaceBefore: Float = 0f,
            widthFraction: Float = 1f,
            keepTogether: Boolean = false,
            keepWithNext: Boolean = false,
            pullPrevious: Int = 0,
            pullable: Boolean = false,
        ) {
            val styled = StyledText.build(text, spans, bold, fonts, f.font, defaultScript)
            val width = contentW * widthFraction
            blocks += Block(
                layout = layoutOf(styled, width, align, justify),
                x = left + contentW - width,
                width = width,
                spaceBefore = spaceBefore,
                keepTogether = keepTogether,
                keepWithNext = keepWithNext,
                pullPrevious = pullPrevious,
                pullable = pullable,
            )
        }

        // --- top of the letter ---
        if (doc.style == LetterStyle.PERSONAL) {
            val topLines = LetterText.personalTopLines(doc)
            if (topLines.isNotEmpty()) {
                add(topLines.joinToString("\n"), align = Layout.Alignment.ALIGN_OPPOSITE, widthFraction = 0.6f, keepTogether = true)
            }
        }
        val recipient = LetterText.recipientLines(doc)
        if (recipient.isNotEmpty()) {
            add(LetterText.recipientHeading(doc.language), spaceBefore = if (blocks.isEmpty()) 0f else lineGap, keepWithNext = true)
            add(recipient.joinToString("\n"), spaceBefore = lineGap * 0.5f, keepTogether = true)
        }
        val subject = LetterText.subjectLine(doc)
        if (subject.isNotEmpty()) {
            add(subject, bold = true, spaceBefore = lineGap, keepTogether = true, keepWithNext = true)
        }
        val salutation = doc.salutation.trim()
        if (salutation.isNotEmpty()) {
            add(salutation, spaceBefore = lineGap, keepWithNext = true)
        }

        // --- body: one block per paragraph, with its slice of the bold/italic runs ---
        val body = doc.body.trimEnd()
        val align = when (f.bodyAlign) {
            BodyAlign.LEFT, BodyAlign.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
            BodyAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            BodyAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }
        if (body.isNotEmpty()) {
            var offset = 0
            body.split("\n").forEachIndexed { index, paragraph ->
                val runs = doc.bodySpans.mapNotNull { s ->
                    val a = maxOf(s.start, offset)
                    val b = minOf(s.end, offset + paragraph.length)
                    if (b > a) TextSpan(a - offset, b - offset, s.bold, s.italic) else null
                }
                add(
                    text = paragraph,
                    spans = runs,
                    align = align,
                    justify = f.bodyAlign == BodyAlign.JUSTIFY,
                    spaceBefore = if (index == 0) lineGap else lineGap * 0.6f,
                    pullable = true,
                )
                offset += paragraph.length + 1
            }
        }

        // --- closing and signature, right side, kept together ---
        val closing = doc.closing.trim()
        val signature = LetterText.signatureLines(doc)
        val closingLines = mutableListOf<String>()
        if (closing.isNotEmpty()) {
            closingLines += closing
            if (signature.isNotEmpty()) closingLines += "" // room for a handwritten signature
        }
        closingLines += signature
        if (closingLines.isNotEmpty()) {
            add(
                closingLines.joinToString("\n"),
                align = Layout.Alignment.ALIGN_OPPOSITE,
                spaceBefore = lineGap * 1.5f,
                widthFraction = 0.6f,
                keepTogether = true,
                pullPrevious = 2,
            )
        }

        val pages = paginate(blocks, top, bottom)
        return LaidOutDocument(
            widthPt = pageW,
            heightPt = pageH,
            pages = pages.map { page ->
                LaidOutPage(page.map { PageSlice(it.layout, it.start, it.end, it.x, it.y, it.width) })
            },
            usedFallbackFonts = fonts.usedFallback,
        )
    }

    private fun linesHeight(l: StaticLayout, from: Int, to: Int): Float =
        (l.getLineBottom(to - 1) - l.getLineTop(from)).toFloat()

    private fun paginate(blocks: List<Block>, top: Float, bottom: Float): List<List<Placed>> {
        val eps = 0.5f
        val pageHeight = bottom - top
        val pages = mutableListOf<MutableList<Placed>>()
        var page = mutableListOf<Placed>()
        pages += page
        var y = top

        fun newPage() {
            page = mutableListOf()
            pages += page
            y = top
        }

        /** Takes the last [count] lines off the previous slice so they can move to the next page. */
        fun pullLines(count: Int): Placed? {
            val last = page.lastOrNull() ?: return null
            if (!last.pullable || last.end - last.start < count + 2) return null
            val moved = Placed(last.layout, last.end - count, last.end, last.x, top, last.width, true)
            last.end -= count
            return moved
        }

        for ((index, block) in blocks.withIndex()) {
            val layout = block.layout
            val lineCount = layout.lineCount
            val total = linesHeight(layout, 0, lineCount)
            val keepTogether = block.keepTogether && total <= pageHeight
            var need = if (keepTogether) total else linesHeight(layout, 0, minOf(lineCount, 2))
            if (block.keepWithNext) {
                blocks.getOrNull(index + 1)?.let { next ->
                    need += next.spaceBefore + linesHeight(next.layout, 0, minOf(next.layout.lineCount, 2))
                }
            }
            if (page.isNotEmpty() && y + block.spaceBefore + need > bottom + eps) {
                val moved = if (block.pullPrevious > 0) pullLines(block.pullPrevious) else null
                newPage()
                if (moved != null) {
                    moved.y = top
                    page += moved
                    y = top + linesHeight(moved.layout, moved.start, moved.end)
                }
            }

            var start = 0
            while (start < lineCount) {
                val first = page.isEmpty()
                val gap = if (first || start > 0) 0f else block.spaceBefore
                val available = bottom - y - gap
                var end = start
                while (end < lineCount && linesHeight(layout, start, end + 1) <= available + eps) end++
                if (end >= lineCount) {
                    y += gap
                    page += Placed(layout, start, lineCount, block.x, y, block.width, block.pullable)
                    y += linesHeight(layout, start, lineCount)
                    break
                }
                var take = end - start
                if (take < 2 && !first) {
                    newPage()
                    continue
                }
                if (lineCount - end == 1 && take > 2) take -= 1 // never strand a single last line
                if (take < 1) take = 1
                y += gap
                page += Placed(layout, start, start + take, block.x, y, block.width, block.pullable)
                start += take
                newPage()
            }
        }
        if (pages.size > 1 && pages.last().isEmpty()) pages.removeAt(pages.lastIndex)
        return pages
    }
}
