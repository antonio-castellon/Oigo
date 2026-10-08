package ch.castellon.oigo

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * One daytime chat on the phone. xAI stores nothing: each question is sent alone.
 * The last few spoken lines go with it. Older lines are folded here into a short
 * summary, kept on the phone for the chosen hours. Search pages are not kept.
 * Notes and alarms live elsewhere and are not cleared with this chat.
 */
class GrokClient(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("grok", Context.MODE_PRIVATE)
    private val lock = Any()
    private val turns = ArrayDeque<Turn>()
    private val bits = ArrayDeque<Bit>()
    private var generation = 0

    init {
        load()
    }

    fun clear() = synchronized(lock) {
        generation++
        turns.clear()
        bits.clear()
        save()
    }

    fun ask(store: Store, text: String, languageName: String, kept: String = ""): Answer {
        val key = store.apiKey
        if (key.isBlank()) return Answer.NeedKey
        if (store.model == "grok-4.7") store.model = "grok-4.3"
        val model = store.model.ifBlank { "grok-4.3" }
        val earlier: String
        val prior: List<Turn>
        val gen: Int
        val bitCount: Int
        synchronized(lock) {
            if (expire(store.contextHours)) save()
            earlier = digest()
            prior = turns.toList()
            gen = generation
            bitCount = bits.size
        }
        val lookup = needsLookup(text)
        val memory = if (kept.isBlank()) "" else "\n$kept"
        val day = if (earlier.isBlank()) {
            ""
        } else {
            "\nEarlier today, in short: $earlier. Use that only as background. The recent lines are the live talk."
        }
        val system = """
            You are the voice of a home assistant. Answer in $languageName.
            Speak in one or two short sentences. If the person asks for an explanation, you may use up to 80 words.
            No markdown, no lists, no emoji, no links.
            ${if (lookup) "Search before you answer. Dates, history, weather, news, prices and who someone is must come from the search, not from memory." else "This turn has no web search. Answer the chat, the story, or the tone. Do not invent a date, a price, or a historical fact."}$memory$day
            If, and only if, the person asks to call someone, to read recent messages, or to send a WhatsApp message, answer with exactly one line and nothing else:
            CMD call <name>
            CMD read <n> <name>
            CMD send whatsapp <name> :: <text>
            Do not invent a command for an ordinary question.
        """.trimIndent()
        val input = JSONArray()
        prior.forEach { input.put(JSONObject().put("role", it.role).put("content", it.content)) }
        input.put(JSONObject().put("role", "user").put("content", text))
        val body = JSONObject()
            .put("model", model)
            .put("store", false)
            .put("instructions", system)
            .put("input", input)
            .put("include", JSONArray().put("no_inline_citations"))
        if (model == "grok-4.3") {
            body.put("reasoning", JSONObject().put("effort", if (lookup) "low" else "none"))
        }
        if (lookup) {
            val tools = JSONArray().put(JSONObject().put("type", "web_search"))
            if (needsX(text)) tools.put(JSONObject().put("type", "x_search"))
            body.put("tools", tools)
            body.put("tool_choice", "required")
        }
        val connection = URL("https://api.x.ai/v1/responses").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 20000
        connection.readTimeout = 90000
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $key")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        val payload = body.toString().toByteArray(Charsets.UTF_8)
        return try {
            connection.outputStream.use { it.write(payload) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code == 401 || code == 402 || code == 429) {
                Trace.event("api $code fuel")
                return Answer.Fuel
            }
            if (code !in 200..299) {
                Log.i(TAG, "api $code ${raw.take(180).replace(Regex("\\s+"), " ")}")
                Trace.event("api $code failed")
                return Answer.Failed
            }
            val root = JSONObject(raw)
            val content = spokenText(root)
            Log.i(
                TAG,
                "api $code ${root.optString("status")} model=$model search=$lookup turns=${prior.size} bits=$bitCount digest=${earlier.length} chars=${content.length}",
            )
            Trace.event("api $code model=$model search=$lookup turns=${prior.size} chars=${content.length}")
            if (content.isBlank()) {
                Log.i(TAG, "api empty search=$lookup")
                Trace.event("api empty search=$lookup")
                return Answer.Failed
            }
            synchronized(lock) {
                if (gen == generation) remember(text, content, System.currentTimeMillis())
            }
            Answer.Text(strip(content))
        } catch (error: Exception) {
            Log.e(TAG, "api failed", error)
            Trace.event("api failed ${error.javaClass.simpleName}")
            Answer.Failed
        } finally {
            connection.disconnect()
        }
    }

    /** Drops lines older than the daytime window. Caller holds [lock]. */
    private fun expire(hours: Int): Boolean {
        val cutoff = System.currentTimeMillis() - hours.coerceIn(1, 24) * 3_600_000L
        var dropped = false
        while (bits.firstOrNull()?.let { it.at < cutoff } == true) {
            bits.removeFirst()
            dropped = true
        }
        while (turns.firstOrNull()?.let { it.at < cutoff } == true) {
            turns.removeFirst()
            dropped = true
        }
        return dropped
    }

    /** Keeps the newest lines whole and folds the rest into a short day summary. Caller holds [lock]. */
    private fun remember(user: String, assistant: String, at: Long) {
        turns.addLast(Turn("user", clip(user, TURN_CAP), at))
        turns.addLast(Turn("assistant", clip(assistant, TURN_CAP), at))
        while (turns.size > KEEP) {
            val first = turns.removeFirst()
            val second = turns.firstOrNull()
            val reply = if (first.role == "user" && second?.role == "assistant") turns.removeFirst() else null
            val line = if (reply == null) {
                "Person: ${clip(first.content, CLIP)}"
            } else {
                "Person: ${clip(first.content, CLIP)} Assistant: ${clip(reply.content, CLIP)}"
            }
            bits.addLast(Bit(first.at, line))
        }
        while (bits.isNotEmpty() && bits.sumOf { it.text.length + 1 } > DIGEST) bits.removeFirst()
        save()
    }

    /** Caller holds [lock]. */
    private fun digest(): String = bits.joinToString(" ") { it.text }

    private fun clip(text: String, limit: Int): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        return if (clean.length <= limit) clean else clean.take(limit - 3).trimEnd() + "..."
    }

    /** Caller holds [lock]. */
    private fun save() {
        val root = JSONObject()
        val packedBits = JSONArray()
        bits.forEach { packedBits.put(JSONObject().put("at", it.at).put("text", it.text)) }
        val packedTurns = JSONArray()
        turns.forEach {
            packedTurns.put(JSONObject().put("role", it.role).put("content", it.content).put("at", it.at))
        }
        root.put("bits", packedBits).put("turns", packedTurns)
        prefs.edit().putString(CHAT_DAY, root.toString()).commit()
    }

    private fun load() {
        val raw = prefs.getString(CHAT_DAY, null) ?: return
        try {
            val root = JSONObject(raw)
            val packedBits = root.optJSONArray("bits") ?: JSONArray()
            for (i in 0 until packedBits.length()) {
                val item = packedBits.optJSONObject(i) ?: continue
                val text = item.optString("text")
                if (text.isBlank()) continue
                bits.addLast(Bit(item.optLong("at"), text))
            }
            val packedTurns = root.optJSONArray("turns") ?: JSONArray()
            for (i in 0 until packedTurns.length()) {
                val item = packedTurns.optJSONObject(i) ?: continue
                val role = item.optString("role")
                val content = item.optString("content")
                if ((role != "user" && role != "assistant") || content.isBlank()) continue
                turns.addLast(Turn(role, content, item.optLong("at")))
            }
            while (bits.isNotEmpty() && bits.sumOf { it.text.length + 1 } > DIGEST) bits.removeFirst()
            while (turns.size > KEEP) turns.removeFirst()
        } catch (_: Exception) {
            bits.clear()
            turns.clear()
        }
    }

    /** Facts, including history, search. Chat, stories and tone do not. */
    private fun needsLookup(text: String): Boolean {
        val folded = Inbox.fold(text)
        if (FACT.containsMatchIn(folded)) return true
        if (CHAT.containsMatchIn(folded)) return false
        return true
    }

    private fun needsX(text: String): Boolean = SOCIAL.containsMatchIn(Inbox.fold(text))

    private fun spokenText(root: JSONObject): String {
        val direct = root.optString("output_text")
        if (direct.isNotBlank()) return direct.trim()
        val output = root.optJSONArray("output") ?: return ""
        val parts = StringBuilder()
        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            if (item.optString("type") != "message") continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val block = content.optJSONObject(j) ?: continue
                if (block.optString("type") == "output_text") parts.append(block.optString("text"))
            }
        }
        return parts.toString().trim()
    }

    private fun strip(text: String): String {
        return text.replace(Regex("\\[\\[\\d+]]\\([^)]*\\)"), " ")
            .replace(Regex("\\[[^\\]]+]\\([^)]*\\)"), " ")
            .replace(Regex("[*`#>]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private data class Turn(val role: String, val content: String, val at: Long)

    private data class Bit(val at: Long, val text: String)

    private companion object {
        const val TAG = "GrokEar"
        const val CHAT_DAY = "chat_day"
        const val KEEP = 6
        const val CLIP = 80
        const val TURN_CAP = 480
        const val DIGEST = 4000
        val FACT: Regex = Regex(
            "\\b(1\\d{3}|20\\d{2})\\b|" +
                "\\b(tiempo|clima|temperatura|lluvia|weather|forecast|meteo|wetter)\\b|" +
                "\\b(noticia|noticias|news|precio|precios|price)\\b|" +
                "\\b(quien|cuando|donde|cuanto|cuantos|que paso|que ocurrio|who|when|where)\\b|" +
                "\\b(historia de|history of|historico|historica|historicos|historicas|guerra|batalla|revolucion|imperio|tratado|biografia|siglo)\\b|" +
                "\\b(capital|poblacion|presidente|rey|reina)\\b|" +
                "\\b(nacio|murio|fundo|invento|descubrio)\\b|" +
                "\\b(que ano|en que ano|que fecha|en que fecha)\\b",
        )
        val CHAT: Regex = Regex(
            "\\b(hola|buenos dias|buenas tardes|buenas noches|que tal|como estas|hello|hi|bonjour|hallo)\\b|" +
                "\\b(cuento|chiste|broma|story|joke|conte|blague|witz|una historia|un relato)\\b|" +
                "\\b(no quiero|no me cuentes|callate|basta|mas corto|mas breve)\\b|" +
                "\\b(gracias|adios|bye|danke)\\b|" +
                "\\b(no tiene voz|sin voz|mas alto|repite)\\b",
        )
        val SOCIAL: Regex = Regex("\\b(twitter|noticia|noticias|que se dice|ultima hora|tendencia)\\b|\\bx\\b")
    }

    sealed class Answer {
        data class Text(val value: String) : Answer()
        data object NeedKey : Answer()
        data object Fuel : Answer()
        data object Failed : Answer()
    }
}
