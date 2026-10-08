package ch.castellon.oigo

/** Messages seen in notifications since the option was turned on. Not the full chat history. */
object Inbox {
    data class Note(val channel: String, val who: String, val text: String, val at: Long)

    private val notes = ArrayDeque<Note>()
    var pending: Note? = null
    private var asked = false

    /** True when a notice is waiting and the voice has not asked about it yet. */
    @Synchronized
    fun hasUnasked(): Boolean = pending != null && !asked

    @Synchronized
    fun markAsked() {
        asked = true
    }

    @Synchronized
    fun add(channel: String, who: String, text: String): Note? {
        val note = Note(channel, who.ifBlank { channel }, text.trim(), System.currentTimeMillis())
        val previous = notes.lastOrNull()
        if (previous != null && previous.who == note.who && previous.text == note.text && note.at - previous.at < 15_000) {
            return null
        }
        notes.addLast(note)
        while (notes.size > 200) notes.removeFirst()
        pending = note
        asked = false
        return note
    }

    @Synchronized
    fun takePending(): Note? {
        val note = pending
        pending = null
        return note
    }

    @Synchronized
    fun dropPending() {
        pending = null
    }

    @Synchronized
    fun last(who: String, count: Int): List<Note> {
        val needle = fold(who)
        if (needle.isBlank()) return emptyList()
        return notes.filter { fold(it.who).contains(needle) || needle.contains(fold(it.who)) }
            .takeLast(count.coerceIn(1, 20))
    }

    fun fold(text: String): String {
        val builder = StringBuilder()
        text.lowercase().forEach { char ->
            val plain = when (char) {
                'á', 'à', 'ä' -> 'a'
                'é', 'è', 'ë' -> 'e'
                'í', 'ì', 'ï' -> 'i'
                'ó', 'ò', 'ö' -> 'o'
                'ú', 'ù', 'ü' -> 'u'
                'ñ' -> 'n'
                else -> char
            }
            if (plain.isLetterOrDigit() || plain == ' ') builder.append(plain)
        }
        return builder.toString().replace(Regex("\\s+"), " ").trim()
    }
}
