package app.writer.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateText {
    private val DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)
    private val LIST_HI = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("hi", "IN"))
    private val LIST_EN = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    /** The date written on a letter, e.g. 10/10/2026. */
    fun letterDate(date: LocalDate = LocalDate.now()): String = date.format(DISPLAY)

    /** Date shown in file lists, e.g. "10 Oct 2026" or "10 अक्टू 2026". */
    fun listDate(epochMillis: Long, hindi: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return date.format(if (hindi) LIST_HI else LIST_EN)
    }
}
