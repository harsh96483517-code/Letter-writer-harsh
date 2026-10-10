package app.writer.pdf

import android.graphics.pdf.PdfDocument
import app.writer.model.WriterDocument
import java.io.File
import java.io.OutputStream
import kotlin.math.roundToInt

data class PdfResult(val pageCount: Int, val usedFallbackFonts: Boolean)

/**
 * Writes a document as a real PDF: text is drawn as text (not a screenshot), so it stays sharp,
 * selectable and searchable. Needs no network.
 */
class PdfExporter(private val engine: LetterEngine) {

    /** Runs on the caller's thread; call it from a background dispatcher. [checkCancelled] may throw. */
    fun write(doc: WriterDocument, out: OutputStream, checkCancelled: () -> Unit = {}): PdfResult {
        val laid = engine.layout(doc)
        val pdf = PdfDocument()
        try {
            for (index in laid.pages.indices) {
                checkCancelled()
                val info = PdfDocument.PageInfo.Builder(laid.widthPt.roundToInt(), laid.heightPt.roundToInt(), index + 1).create()
                val page = pdf.startPage(info)
                PageRenderer.draw(page.canvas, laid, index)
                pdf.finishPage(page)
            }
            checkCancelled()
            pdf.writeTo(out)
            out.flush()
        } finally {
            pdf.close()
        }
        return PdfResult(laid.pageCount, laid.usedFallbackFonts)
    }

    /** Writes to a temporary file first so a failed export never leaves a half-written PDF behind. */
    fun writeToFile(doc: WriterDocument, target: File, checkCancelled: () -> Unit = {}): PdfResult {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".part")
        try {
            val result = temp.outputStream().buffered().use { write(doc, it, checkCancelled) }
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) throw java.io.IOException("Could not move the finished PDF into place")
            return result
        } finally {
            if (temp.exists()) temp.delete()
        }
    }
}
