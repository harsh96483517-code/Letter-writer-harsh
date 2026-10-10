package app.writer.pdf

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import app.writer.model.FontChoice
import app.writer.model.TextSpan

/** Applies one typeface to a run of text, for both measuring and drawing. */
internal class RunTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(paint: TextPaint) {
        paint.typeface = typeface
    }

    override fun updateMeasureState(paint: TextPaint) {
        paint.typeface = typeface
    }
}

/** Turns plain text plus bold/italic runs into text that carries the right font for every character. */
internal object StyledText {

    fun scriptOf(codePoint: Int): Script? = when {
        codePoint in 0x0900..0x097F || codePoint in 0xA8E0..0xA8FF || codePoint in 0x1CD0..0x1CFF -> Script.DEVANAGARI
        // Joiners keep whatever font surrounds them.
        codePoint == 0x200C || codePoint == 0x200D -> null
        Character.isLetter(codePoint) || Character.getType(codePoint) == Character.NON_SPACING_MARK.toInt() -> Script.LATIN
        else -> null
    }

    fun build(
        text: String,
        spans: List<TextSpan>,
        baseBold: Boolean,
        fonts: LetterFonts,
        font: FontChoice,
        defaultScript: Script,
    ): SpannableStringBuilder {
        val out = SpannableStringBuilder(text)
        if (text.isEmpty()) return out

        val scripts = Array(text.length) { defaultScript }
        var current = defaultScript
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val width = Character.charCount(cp)
            val strong = scriptOf(cp)
            if (strong != null) {
                current = strong
            } else if (!Character.isWhitespace(cp) && cp != 0x200C && cp != 0x200D) {
                // Digits, punctuation and symbols stay in the surrounding font when it has them.
                val currentFace = fonts.typeface(current, font, bold = false, italic = false)
                if (!fonts.hasGlyph(currentFace, cp)) {
                    val other = if (current == Script.LATIN) Script.DEVANAGARI else Script.LATIN
                    if (fonts.hasGlyph(fonts.typeface(other, font, bold = false, italic = false), cp)) current = other
                }
            }
            for (k in 0 until width) scripts[i + k] = current
            i += width
        }

        val boldAt = BooleanArray(text.length) { baseBold }
        val italicAt = BooleanArray(text.length)
        for (span in spans) {
            for (k in span.start.coerceAtLeast(0) until span.end.coerceAtMost(text.length)) {
                if (span.bold) boldAt[k] = true
                if (span.italic) italicAt[k] = true
            }
        }

        var start = 0
        while (start < text.length) {
            var end = start + 1
            while (
                end < text.length &&
                scripts[end] == scripts[start] &&
                boldAt[end] == boldAt[start] &&
                italicAt[end] == italicAt[start]
            ) end++
            out.setSpan(
                RunTypefaceSpan(fonts.typeface(scripts[start], font, boldAt[start], italicAt[start])),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            start = end
        }
        return out
    }
}
