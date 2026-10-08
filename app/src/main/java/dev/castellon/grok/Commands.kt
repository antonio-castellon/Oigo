package dev.castellon.grok

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

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
        val people = phones.map { it.person }.distinct()
        if (people.isEmpty()) return context.getString(R.string.call_missing, name)
        if (people.size > 1) return context.getString(R.string.call_many, name)
        val numbers = phones.map { it.number }.distinct()
        val chosen = when {
            numbers.size == 1 -> numbers.first()
            else -> phones.firstOrNull { it.mobile }?.number
        }
        if (chosen == null || numbers.size > 1 && phones.count { it.mobile } != 1) {
            return context.getString(R.string.call_many, name)
        }
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$chosen"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return context.getString(R.string.call_done, people.first())
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

    private data class PhoneHit(val person: String, val number: String, val mobile: Boolean)

    private fun phonesFor(context: Context, name: String): List<PhoneHit> {
        val needle = Inbox.fold(name).split(" ").filter { it.isNotBlank() }
        if (needle.isEmpty()) return emptyList()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
        )
        val found = mutableListOf<PhoneHit>()
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            while (cursor.moveToNext()) {
                val person = cursor.getString(nameCol) ?: continue
                val folded = Inbox.fold(person)
                if (needle.all { folded.contains(it) }) {
                    val number = cursor.getString(numberCol) ?: continue
                    val type = if (typeCol >= 0) cursor.getInt(typeCol) else 0
                    found += PhoneHit(
                        person,
                        number,
                        type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                    )
                }
            }
        }
        return found
    }
}
