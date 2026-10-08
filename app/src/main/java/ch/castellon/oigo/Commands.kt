package ch.castellon.oigo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone as ContactPhone
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local orders. A cloud reply may also arrive as "CMD ...".
 * Sending a message opens the other app with a draft. It does not send by itself.
 */
object Commands {
    fun fromCloud(context: Context, store: Store, line: String): String? {
        val text = line.trim()
        if (!text.startsWith("CMD ")) return null
        val body = text.removePrefix("CMD ").trim()
        return when {
            body.startsWith("call ") -> call(context, store, body.removePrefix("call ").trim())
            body.startsWith("read ") -> {
                val rest = body.removePrefix("read ").trim()
                val count = rest.takeWhile { it.isDigit() }.toIntOrNull() ?: 3
                val who = rest.dropWhile { it.isDigit() }.trim()
                read(context, store, count, who)
            }
            body.startsWith("send whatsapp ") -> {
                val rest = body.removePrefix("send whatsapp ").trim()
                val parts = rest.split("::", limit = 2)
                val who = parts[0].trim()
                val message = parts.getOrElse(1) { "" }.trim()
                sendWhatsapp(context, store, who, message)
            }
            body.startsWith("send telegram") -> context.getString(R.string.telegram_send_later)
            else -> null
        }
    }

    /** A call, a send, or a read. Used before ordinary talk, and just after a message. */
    fun handles(heard: String): Boolean {
        val folded = Inbox.fold(heard.trim())
        if (callName(folded) != null) return true
        if (readRequest(folded) != null) return true
        if (whatsappRequest(heard) != null) return true
        return folded.startsWith("envia un telegram") || folded.startsWith("manda un telegram")
    }

    /** Null means this phrase is ordinary talk and may go to Grok. */
    fun local(context: Context, store: Store, heard: String): String? {
        val text = heard.trim()
        val folded = Inbox.fold(text)
        callName(folded)?.let { return call(context, store, it) }
        readRequest(folded)?.let { return read(context, store, it.first, it.second) }
        whatsappRequest(text)?.let { return sendWhatsapp(context, store, it.first, it.second) }
        if (folded.startsWith("envia un telegram") || folded.startsWith("manda un telegram")) {
            return context.getString(R.string.telegram_send_later)
        }
        return null
    }

    private fun call(context: Context, store: Store, name: String): String {
        if (!store.calls) return context.getString(R.string.option_off)
        if (!granted(context, Manifest.permission.CALL_PHONE) || !granted(context, Manifest.permission.READ_CONTACTS)) {
            return context.getString(R.string.need_call_permission)
        }
        val phones = phonesFor(context, name)
        val groups = phones.groupBy { Inbox.fold(it.person) }
        val distinct = phones.map { digits(it.number) }.filter { it.isNotEmpty() }.distinct().size
        if (groups.isEmpty()) {
            Log.i(TAG, "call people=0 numbers=0 pick=missing")
            Trace.event("call people=0 numbers=0 pick=missing")
            return context.getString(R.string.call_missing, name)
        }
        if (groups.size > 1) {
            Log.i(TAG, "call people=${groups.size} numbers=$distinct pick=many")
            Trace.event("call people=${groups.size} numbers=$distinct pick=many")
            val listed = enumerate(context, groups.values.map { fullName(it) })
            return context.getString(R.string.call_many, name, listed)
        }
        val mine = groups.values.first()
        val chosen = choose(mine)
        Log.i(TAG, "call people=1 numbers=$distinct pick=${chosen?.first ?: "none"}")
        Trace.event("call people=1 numbers=$distinct pick=${chosen?.first ?: "none"}")
        if (chosen == null) return context.getString(R.string.call_no_main, fullName(mine))
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${chosen.second}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        callPlaced.set(true)
        return context.getString(R.string.call_done, fullName(mine))
    }

    /** True once, after this turn really handed a call to the phone. */
    fun takeCallPlaced(): Boolean = callPlaced.getAndSet(false)

    /**
     * The catch ear often hears only the name, because "llamar a" was said
     * before speech recognition opened. Dial when that name is one contact.
     * Null when it is not a name, or nobody in the address book matches.
     */
    fun dialKnown(context: Context, store: Store, name: String): String? {
        var folded = Inbox.fold(name).trim()
        if (folded.startsWith("a ")) folded = folded.removePrefix("a ").trim()
        val words = folded.split(" ").filter { it.isNotBlank() }
        if (words.size !in 1..8) return null
        val small = setOf("de", "la", "las", "los", "del", "y", "e", "da", "do", "di", "van", "von")
        if (words.any { word -> word.any { !it.isLetter() } || (word.length < 2 && word !in small) }) return null
        if (!store.calls) return null
        val spoken = call(context, store, folded)
        if (spoken == context.getString(R.string.call_missing, folded)) return null
        return spoken
    }

    private fun read(context: Context, store: Store, count: Int, who: String): String {
        if (!store.whatsapp && !store.telegram) return context.getString(R.string.option_off)
        val found = Inbox.last(who, count)
        if (found.isEmpty()) return context.getString(R.string.messages_none, who)
        return found.joinToString(". ") { note ->
            if (note.text.isBlank()) context.getString(R.string.message_hidden, note.who)
            else context.getString(R.string.message_body, note.who, note.text)
        }
    }

    private fun sendWhatsapp(context: Context, store: Store, who: String, message: String): String {
        if (!store.whatsapp) return context.getString(R.string.option_off)
        if (!granted(context, Manifest.permission.READ_CONTACTS)) {
            return context.getString(R.string.need_call_permission)
        }
        val phone = phonesFor(context, who).firstOrNull()?.number
            ?: return context.getString(R.string.call_missing, who)
        val digits = phone.filter { it.isDigit() }
        val uri = Uri.parse("https://wa.me/$digits?text=" + Uri.encode(message))
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            context.getString(R.string.send_ready, who)
        } catch (_: Exception) {
            context.getString(R.string.whatsapp_missing)
        }
    }

    private fun callName(folded: String): String? {
        val patterns = listOf(
            Regex("""^(?:puedes |podrias |haz una |por favor )?llamar a (.+)$"""),
            Regex("""^(?:puedes |podrias )?llama a (.+)$"""),
            Regex("""\bllamar a (.+)$"""),
            Regex("""\bllama a (.+)$"""),
            Regex("""^call (.+)$"""),
            Regex("""^appelle (.+)$"""),
            Regex("""^ruf (.+?) an$"""),
        )
        return patterns.firstNotNullOfOrNull { it.find(folded)?.groupValues?.get(1)?.trim() }
    }

    private fun readRequest(folded: String): Pair<Int, String>? {
        val patterns = listOf(
            Regex("""^lee los ultimos (\d+) mensajes de (.+)$"""),
            Regex("""^lee los mensajes de (.+)$"""),
            Regex("""^read the last (\d+) messages from (.+)$"""),
            Regex("""^lis les (\d+) derniers messages de (.+)$"""),
        )
        patterns.forEach { pattern ->
            val match = pattern.find(folded) ?: return@forEach
            val groups = match.groupValues
            if (groups.size == 3 && groups[1].all { it.isDigit() }) {
                return groups[1].toInt() to groups[2].trim()
            }
            return 3 to groups.last().trim()
        }
        return null
    }

    private fun whatsappRequest(raw: String): Pair<String, String>? {
        val folded = Inbox.fold(raw)
        val match = Regex("""^(?:envia|manda)(?: un)? whatsapp a ([^:]+?)(?: diciendo | que diga | )(.*)$""")
            .find(folded) ?: return null
        val who = match.groupValues[1].trim()
        val message = raw.substringAfter(who, "").trim().ifBlank { match.groupValues[2].trim() }
        if (who.isBlank()) return null
        return who to message
    }

    private fun granted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private data class PhoneHit(
        val person: String,
        val number: String,
        val main: Boolean,
        val preferred: Boolean,
    )

    /** Main or Principal label first, then the contact's default number, then the only number. */
    private fun choose(mine: List<PhoneHit>): Pair<String, String>? {
        val rows = mine.filter { digits(it.number).isNotEmpty() }.distinctBy { digits(it.number) }
        val mains = rows.filter { it.main }
        val hit = when {
            mains.size == 1 -> mains.first()
            mains.size > 1 -> mains.firstOrNull { it.preferred } ?: mains.first()
            else -> {
                val preferred = rows.filter { it.preferred }
                when {
                    preferred.isNotEmpty() -> preferred.first()
                    rows.size == 1 -> rows.first()
                    else -> return null
                }
            }
        }
        val kind = when {
            hit.main -> "main"
            hit.preferred -> "preferred"
            else -> "only"
        }
        return kind to hit.number
    }

    private fun fullName(hits: List<PhoneHit>): String {
        return hits.maxBy { it.person.length }.person.trim()
    }

    private fun enumerate(context: Context, names: List<String>): String {
        val order = context.resources.getStringArray(R.array.call_order)
        val lines = names.take(order.size).mapIndexed { index, who ->
            order[index].replace("%s", who)
        }
        val more = if (names.size > order.size) " " + context.getString(R.string.call_more) else ""
        return lines.joinToString(" ") + more
    }

    private fun digits(number: String): String = number.filter { it.isDigit() }

    private fun phonesFor(context: Context, name: String): List<PhoneHit> {
        val needle = Inbox.fold(name).split(" ").filter { it.isNotBlank() }
        if (needle.isEmpty()) return emptyList()
        val projection = arrayOf(
            ContactPhone.DISPLAY_NAME,
            ContactPhone.NUMBER,
            ContactPhone.TYPE,
            ContactPhone.LABEL,
            ContactPhone.IS_PRIMARY,
            ContactPhone.IS_SUPER_PRIMARY,
        )
        val found = mutableListOf<PhoneHit>()
        context.contentResolver.query(ContactPhone.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val nameCol = cursor.getColumnIndex(ContactPhone.DISPLAY_NAME)
            val numberCol = cursor.getColumnIndex(ContactPhone.NUMBER)
            val typeCol = cursor.getColumnIndex(ContactPhone.TYPE)
            val labelCol = cursor.getColumnIndex(ContactPhone.LABEL)
            val primaryCol = cursor.getColumnIndex(ContactPhone.IS_PRIMARY)
            val superCol = cursor.getColumnIndex(ContactPhone.IS_SUPER_PRIMARY)
            while (cursor.moveToNext()) {
                val person = cursor.getString(nameCol) ?: continue
                val words = Inbox.fold(person).split(" ").filter { it.isNotBlank() }
                if (needle.all { wanted -> words.any { it == wanted } }) {
                    val number = cursor.getString(numberCol) ?: continue
                    val type = if (typeCol >= 0 && !cursor.isNull(typeCol)) cursor.getInt(typeCol) else 0
                    val label = if (labelCol >= 0) Inbox.fold(cursor.getString(labelCol) ?: "") else ""
                    val main = type == ContactPhone.TYPE_MAIN || label == "main" || label == "principal"
                    val preferred = flagged(cursor, superCol) || flagged(cursor, primaryCol)
                    found += PhoneHit(person, number, main, preferred)
                }
            }
        }
        return found
    }

    private fun flagged(cursor: Cursor, column: Int): Boolean {
        return column >= 0 && !cursor.isNull(column) && cursor.getInt(column) != 0
    }

    private const val TAG = "GrokEar"
    private val callPlaced = AtomicBoolean(false)
}
