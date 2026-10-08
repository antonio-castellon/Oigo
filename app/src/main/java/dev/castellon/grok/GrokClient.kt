package dev.castellon.grok

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One open chat. It forgets itself when the talk closes, so a month of history is never resent. */
class GrokClient {
    private val turns = ArrayDeque<JSONObject>()

    fun clear() = turns.clear()

    fun ask(store: Store, text: String, languageName: String): Answer {
        val key = store.apiKey
        if (key.isBlank()) return Answer.NeedKey
        val system = """
            You are the voice of a home assistant. Answer in $languageName.
            Speak in one or two short sentences. If the person asks for an explanation, you may use up to 80 words.
            No markdown, no lists, no emoji.
            If, and only if, the person asks to call someone, to read recent messages, or to send a WhatsApp message, answer with exactly one line and nothing else:
            CMD call <name>
            CMD read <n> <name>
            CMD send whatsapp <name> :: <text>
            Do not invent a command for an ordinary question.
        """.trimIndent()
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", system))
        turns.forEach { messages.put(it) }
        messages.put(JSONObject().put("role", "user").put("content", text))
        val body = JSONObject()
            .put("model", store.model.ifBlank { "grok-4.7" })
            .put("temperature", 0.3)
            .put("messages", messages)
        val connection = URL("https://api.x.ai/v1/chat/completions").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 20000
        connection.readTimeout = 45000
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $key")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        val payload = body.toString().toByteArray(Charsets.UTF_8)
        return try {
            connection.outputStream.use { it.write(payload) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code == 401 || code == 402 || code == 429) return Answer.Fuel
            if (code !in 200..299) return Answer.Failed
            val content = JSONObject(raw)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
            turns.addLast(JSONObject().put("role", "user").put("content", text))
            turns.addLast(JSONObject().put("role", "assistant").put("content", content))
            while (turns.size > 8) turns.removeFirst()
            Answer.Text(strip(content))
        } catch (_: Exception) {
            Answer.Failed
        } finally {
            connection.disconnect()
        }
    }

    private fun strip(text: String): String {
        return text.replace(Regex("[*`#>]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    sealed class Answer {
        data class Text(val value: String) : Answer()
        data object NeedKey : Answer()
        data object Fuel : Answer()
        data object Failed : Answer()
    }
}
