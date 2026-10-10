package app.writer.storage

import app.writer.model.Bilingual
import app.writer.model.FieldDef
import app.writer.model.LetterStyle
import app.writer.model.RecipientDef
import app.writer.model.TemplateDef
import org.json.JSONArray
import org.json.JSONObject

/** Reads template JSON (see tools/TEMPLATES.md). Pure Kotlin + org.json, so it is unit-tested on the JVM. */
object TemplateParser {

    fun parse(json: String): List<TemplateDef> {
        val array = JSONArray(json)
        return List(array.length()) { parseTemplate(array.getJSONObject(it)) }
    }

    private fun parseTemplate(o: JSONObject): TemplateDef {
        val recipient = o.optJSONObject("recipient")?.let { r ->
            val hi = r.getJSONObject("hi")
            val en = r.getJSONObject("en")
            RecipientDef(
                designation = Bilingual(hi.getString("designation"), en.getString("designation")),
                office = Bilingual(hi.optString("office"), en.optString("office")),
            )
        }
        val fields = o.optJSONArray("fields")?.let { array ->
            List(array.length()) { index ->
                val f = array.getJSONObject(index)
                FieldDef(
                    key = f.getString("key"),
                    label = Bilingual(f.getString("labelHi"), f.getString("labelEn")),
                    hint = Bilingual(f.optString("hintHi"), f.optString("hintEn")),
                    multiline = f.optBoolean("multiline"),
                    required = f.optBoolean("required"),
                )
            }
        } ?: emptyList()
        val keywords = o.optJSONArray("keywords")?.let { array -> List(array.length()) { array.getString(it) } } ?: emptyList()
        return TemplateDef(
            id = o.getString("id"),
            category = o.getString("category"),
            style = if (o.optString("style") == "personal") LetterStyle.PERSONAL else LetterStyle.APPLICATION,
            title = Bilingual(o.getString("titleHi"), o.getString("titleEn")),
            description = Bilingual(o.optString("descHi"), o.optString("descEn")),
            keywords = keywords,
            recipient = recipient,
            salutation = bilingual(o, "salutation"),
            subject = bilingual(o, "subject"),
            body = bilingual(o, "body"),
            closing = bilingual(o, "closing"),
            fields = fields,
        )
    }

    private fun bilingual(o: JSONObject, key: String): Bilingual {
        val b = o.optJSONObject(key) ?: return Bilingual.Empty
        return Bilingual(b.optString("hi"), b.optString("en"))
    }
}
