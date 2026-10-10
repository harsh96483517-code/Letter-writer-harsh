package app.writer.database

import androidx.room.TypeConverter
import app.writer.model.TextSpan
import org.json.JSONArray
import org.json.JSONObject

/** Room converters for the two list/map columns of a document. */
class Converters {
    @TypeConverter
    fun spansToString(spans: List<TextSpan>): String {
        val array = JSONArray()
        spans.forEach { span ->
            array.put(
                JSONObject()
                    .put("s", span.start)
                    .put("e", span.end)
                    .put("b", span.bold)
                    .put("i", span.italic),
            )
        }
        return array.toString()
    }

    @TypeConverter
    fun stringToSpans(value: String): List<TextSpan> = runCatching {
        val array = JSONArray(value)
        List(array.length()) { index ->
            val o = array.getJSONObject(index)
            TextSpan(o.getInt("s"), o.getInt("e"), o.optBoolean("b"), o.optBoolean("i"))
        }
    }.getOrDefault(emptyList())

    @TypeConverter
    fun mapToString(map: Map<String, String>): String {
        val obj = JSONObject()
        map.forEach { (key, value) -> obj.put(key, value) }
        return obj.toString()
    }

    @TypeConverter
    fun stringToMap(value: String): Map<String, String> = runCatching {
        val obj = JSONObject(value)
        buildMap {
            obj.keys().forEach { key -> put(key, obj.optString(key)) }
        }
    }.getOrDefault(emptyMap())
}
