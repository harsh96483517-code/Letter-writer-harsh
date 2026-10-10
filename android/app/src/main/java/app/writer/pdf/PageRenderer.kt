package app.writer.pdf

import android.graphics.Canvas

/** Draws one laid-out page. Used by both the on-screen preview and the PDF writer. */
object PageRenderer {
    fun draw(canvas: Canvas, document: LaidOutDocument, pageIndex: Int) {
        val page = document.pages.getOrNull(pageIndex) ?: return
        for (slice in page.slices) {
            val layout = slice.layout
            val top = layout.getLineTop(slice.startLine).toFloat()
            val bottom = layout.getLineBottom(slice.endLine - 1).toFloat()
            canvas.save()
            canvas.translate(slice.x, slice.y - top)
            // A little sideways slack so italic overhangs and wide matras are not shaved off.
            canvas.clipRect(-4f, top, slice.width + 4f, bottom)
            layout.draw(canvas)
            canvas.restore()
        }
    }
}
