package app.writer.common

import app.writer.model.AppLanguage
import app.writer.model.Applicant
import app.writer.model.Recipient
import app.writer.model.TemplateDef

/**
 * Fills `{{placeholders}}` in template text. Anything the user has not provided yet becomes a
 * visible `[blank]` such as `[आपका नाम]`, so a half-filled letter still reads sensibly and the
 * app can count how many blanks are left.
 */
object TemplateFiller {
    private val PLACEHOLDER = Regex("\\{\\{([a-z0-9_]+)\\}\\}")
    private val BLANK = Regex("\\[[^\\[\\]\\n]{1,60}\\]")

    fun fill(text: String, values: Map<String, String>, blankLabel: (String) -> String): String =
        PLACEHOLDER.replace(text) { match ->
            val key = match.groupValues[1]
            val value = values[key]?.trim().orEmpty()
            if (value.isNotEmpty()) value else "[" + blankLabel(key) + "]"
        }

    fun countBlanks(text: String): Int = BLANK.findAll(text).count()

    /** Start and end offsets (end exclusive) of every `[blank]` in [text]. */
    fun blankRanges(text: String): List<IntRange> =
        BLANK.findAll(text).map { it.range.first until it.range.last + 1 }.toList()

    /** Values for the standard placeholder keys plus the template's own fields. */
    fun valuesFor(
        applicant: Applicant,
        recipient: Recipient,
        dateText: String,
        fieldValues: Map<String, String>,
        language: AppLanguage,
    ): Map<String, String> = buildMap {
        put("name", applicant.name)
        put("father", applicant.parent)
        put("address", AddressFormatter.full(applicant, language))
        put("village", applicant.village)
        put("post", applicant.postOffice)
        put("block", applicant.block)
        put("district", applicant.district)
        put("state", applicant.state)
        put("pin", applicant.pin)
        put("mobile", applicant.mobile)
        put("date", dateText)
        put("recipient_district", recipient.district)
        put("recipient_office", recipient.office)
        put("recipient_designation", recipient.designation)
        putAll(fieldValues)
    }

    fun blankLabelFor(template: TemplateDef, language: AppLanguage): (String) -> String = { key ->
        standardLabel(key, language)
            ?: template.fields.firstOrNull { it.key == key }?.label?.get(language)
            ?: key
    }

    fun standardLabel(key: String, language: AppLanguage): String? {
        val hi = language == AppLanguage.HI
        return when (key) {
            "name" -> if (hi) "आपका नाम" else "your name"
            "father" -> if (hi) "पिता/माता का नाम" else "parent's name"
            "address" -> if (hi) "आपका पता" else "your address"
            "village" -> if (hi) "गाँव" else "village"
            "post" -> if (hi) "डाकघर" else "post office"
            "block" -> if (hi) "ब्लॉक" else "block"
            "district" -> if (hi) "जनपद" else "district"
            "state" -> if (hi) "राज्य" else "state"
            "pin" -> if (hi) "पिन कोड" else "PIN code"
            "mobile" -> if (hi) "मोबाइल नंबर" else "mobile number"
            "date" -> if (hi) "दिनांक" else "date"
            "recipient_district" -> if (hi) "जनपद का नाम" else "district name"
            "recipient_office" -> if (hi) "कार्यालय" else "office"
            "recipient_designation" -> if (hi) "पदनाम" else "designation"
            else -> null
        }
    }
}

object AddressFormatter {
    /** Combines the address line, village, post office, block, district, state and PIN into one line. */
    fun full(a: Applicant, language: AppLanguage): String {
        val hi = language == AppLanguage.HI
        val parts = mutableListOf<String>()
        a.address.trim().takeIf { it.isNotEmpty() }?.let(parts::add)
        a.village.trim().takeIf { it.isNotEmpty() }?.let { parts += if (hi) "ग्राम $it" else "Village $it" }
        a.postOffice.trim().takeIf { it.isNotEmpty() }?.let { parts += if (hi) "डाकघर $it" else "Post Office $it" }
        a.block.trim().takeIf { it.isNotEmpty() }?.let { parts += if (hi) "ब्लॉक $it" else "Block $it" }
        a.district.trim().takeIf { it.isNotEmpty() }?.let { parts += if (hi) "जनपद $it" else "District $it" }
        val state = a.state.trim()
        val pin = a.pin.trim()
        when {
            state.isNotEmpty() && pin.isNotEmpty() -> parts += "$state - $pin"
            state.isNotEmpty() -> parts += state
            pin.isNotEmpty() -> parts += if (hi) "पिन $pin" else "PIN $pin"
        }
        return parts.joinToString(", ")
    }
}
