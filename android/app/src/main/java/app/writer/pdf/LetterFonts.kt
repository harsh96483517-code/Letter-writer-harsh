package app.writer.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import app.writer.R
import app.writer.model.FontChoice
import java.util.concurrent.ConcurrentHashMap

enum class Script { LATIN, DEVANAGARI }

/**
 * The bundled Noto fonts (SIL Open Font License, see assets/licenses). Devanagari and Latin live
 * in separate font files, so the engine picks a font per run of text. If a font resource cannot be
 * loaded the platform's own serif/sans is used instead and [usedFallback] is set so the UI can say so.
 */
class LetterFonts(private val context: Context) {
    @Volatile
    var usedFallback: Boolean = false
        private set

    private data class Key(val script: Script, val font: FontChoice, val bold: Boolean, val italic: Boolean)

    private val cache = ConcurrentHashMap<Key, Typeface>()

    fun typeface(script: Script, font: FontChoice, bold: Boolean, italic: Boolean): Typeface =
        cache.getOrPut(Key(script, font, bold, italic)) {
            val style = when {
                bold && italic -> Typeface.BOLD_ITALIC
                bold -> Typeface.BOLD
                italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            val family = runCatching { ResourcesCompat.getFont(context, familyRes(script, font)) }.getOrNull()
            if (family != null) {
                Typeface.create(family, style)
            } else {
                usedFallback = true
                Typeface.create(if (font == FontChoice.SERIF) Typeface.SERIF else Typeface.SANS_SERIF, style)
            }
        }

    /** True if [typeface] has a glyph for the character, used for punctuation that only one font has. */
    fun hasGlyph(typeface: Typeface, codePoint: Int): Boolean {
        val paint = Paint().apply { this.typeface = typeface }
        return paint.hasGlyph(String(Character.toChars(codePoint)))
    }

    private fun familyRes(script: Script, font: FontChoice): Int = when (script) {
        Script.LATIN -> if (font == FontChoice.SERIF) R.font.noto_serif_family else R.font.noto_sans_family
        Script.DEVANAGARI ->
            if (font == FontChoice.SERIF) R.font.noto_serif_devanagari_family else R.font.noto_sans_devanagari_family
    }
}
