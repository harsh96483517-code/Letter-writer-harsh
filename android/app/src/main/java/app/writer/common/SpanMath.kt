package app.writer.common

import app.writer.model.TextSpan

/**
 * Keeps bold/italic runs attached to the right characters while the user types, deletes or pastes.
 * All offsets are UTF-16 indexes, matching Compose text fields and Android spans.
 */
object SpanMath {

    /**
     * Adjusts [spans] after [oldText] became [newText]. The edit is found by trimming the common
     * prefix and suffix, which is exact for typing, deleting and pasting a single contiguous piece.
     */
    fun applyEdit(spans: List<TextSpan>, oldText: String, newText: String): List<TextSpan> {
        if (oldText == newText || spans.isEmpty()) return spans
        var prefix = 0
        val maxPrefix = minOf(oldText.length, newText.length)
        while (prefix < maxPrefix && oldText[prefix] == newText[prefix]) prefix++
        var suffix = 0
        val maxSuffix = minOf(oldText.length, newText.length) - prefix
        while (suffix < maxSuffix && oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]) suffix++

        val removedEnd = oldText.length - suffix          // old range [prefix, removedEnd) was replaced
        val insertedLength = newText.length - suffix - prefix
        val delta = insertedLength - (removedEnd - prefix)

        val result = mutableListOf<TextSpan>()
        for (span in spans) {
            var start = span.start
            var end = span.end
            // Map both ends through the edit.
            start = mapOffset(start, prefix, removedEnd, insertedLength, delta, isStart = true)
            end = mapOffset(end, prefix, removedEnd, insertedLength, delta, isStart = false)
            // Text typed right after a bold run stays plain; typing inside a run extends it naturally.
            if (end > start) result += span.copy(start = start, end = end)
        }
        return normalize(result, newText.length)
    }

    private fun mapOffset(offset: Int, editStart: Int, removedEnd: Int, insertedLength: Int, delta: Int, isStart: Boolean): Int = when {
        offset <= editStart -> offset
        offset >= removedEnd -> offset + delta
        // The offset sat inside the replaced region.
        else -> if (isStart) editStart + insertedLength else editStart
    }

    /** Sets or clears bold/italic over [start, end), splitting existing runs as needed. */
    fun toggle(spans: List<TextSpan>, start: Int, end: Int, bold: Boolean?, italic: Boolean?, textLength: Int): List<TextSpan> {
        val from = start.coerceIn(0, textLength)
        val to = end.coerceIn(0, textLength)
        if (to <= from) return spans
        // Work on a per-character style table; texts are letters, so this is small and exact.
        val boldAt = BooleanArray(textLength)
        val italicAt = BooleanArray(textLength)
        for (span in spans) for (i in span.start.coerceAtLeast(0) until span.end.coerceAtMost(textLength)) {
            if (span.bold) boldAt[i] = true
            if (span.italic) italicAt[i] = true
        }
        if (bold != null) for (i in from until to) boldAt[i] = bold
        if (italic != null) for (i in from until to) italicAt[i] = italic
        return fromTable(boldAt, italicAt)
    }

    /** True when every character in [start, end) is bold (or italic). */
    fun isAll(spans: List<TextSpan>, start: Int, end: Int, italic: Boolean): Boolean {
        if (end <= start) return false
        for (i in start until end) {
            val covered = spans.any { s -> i >= s.start && i < s.end && (if (italic) s.italic else s.bold) }
            if (!covered) return false
        }
        return true
    }

    private fun fromTable(boldAt: BooleanArray, italicAt: BooleanArray): List<TextSpan> {
        val out = mutableListOf<TextSpan>()
        var i = 0
        while (i < boldAt.size) {
            if (!boldAt[i] && !italicAt[i]) {
                i++
                continue
            }
            val b = boldAt[i]
            val it = italicAt[i]
            var j = i + 1
            while (j < boldAt.size && boldAt[j] == b && italicAt[j] == it) j++
            out += TextSpan(i, j, b, it)
            i = j
        }
        return out
    }

    /** Drops empty or out-of-range runs and merges neighbours with identical style. */
    fun normalize(spans: List<TextSpan>, textLength: Int): List<TextSpan> {
        val clean = spans
            .map { it.copy(start = it.start.coerceIn(0, textLength), end = it.end.coerceIn(0, textLength)) }
            .filter { it.end > it.start && (it.bold || it.italic) }
            .sortedBy { it.start }
        if (clean.isEmpty()) return clean
        val merged = mutableListOf(clean.first())
        for (span in clean.drop(1)) {
            val last = merged.last()
            if (span.start <= last.end && span.bold == last.bold && span.italic == last.italic) {
                merged[merged.lastIndex] = last.copy(end = maxOf(last.end, span.end))
            } else {
                merged += span
            }
        }
        return merged
    }
}
