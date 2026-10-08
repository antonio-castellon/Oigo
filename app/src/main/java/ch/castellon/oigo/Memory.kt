package ch.castellon.oigo

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.text.format.DateFormat
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Date

/**
 * Notes and alarms stay on the phone. The chat does not.
 * A closed or expired chat throws its lines away. These remain until the person deletes them.
 */
object Memory {
    private const val PREF = "grok"
    private const val KEY = "memory"
    private const val MAX_NOTES = 12
    private const val MAX_ALARMS = 8
    private const val MAX_CHARS = 120
    private const val LATE_MS = 15 * 60_000L
    private val lock = Any()

    /** Short block added to a question. Empty when nothing is kept. */
    fun block(context: Context): String = synchronized(lock) {
        val root = read(context)
        val notes = root.optJSONArray("notes") ?: JSONArray()
        val alarms = root.optJSONArray("alarms") ?: JSONArray()
        if (notes.length() == 0 && alarms.length() == 0) return ""
        val lines = StringBuilder(
            "Notes and alarms kept on this phone. They are not the chat. " +
                "Use one only when it matters. Do not recite them unless asked. Do not invent one.\n",
        )
        for (i in 0 until notes.length()) lines.append("- ").append(notes.optString(i)).append('\n')
        for (i in 0 until alarms.length()) {
            val item = alarms.optJSONObject(i) ?: continue
            lines.append("- alarm ").append(clockText(context, item.optLong("at")))
                .append(" ").append(item.optString("text")).append('\n')
        }
        lines.toString().trim()
    }

    /** Spoken reply when the phrase saves, lists, or deletes. Null means ordinary talk. */
    fun handle(context: Context, ui: Context, raw: String): String? = synchronized(lock) {
        val folded = Inbox.fold(raw.replace('-', ' '))
        if (isList(folded)) return speakList(context, ui)
        if (isClearNotes(folded)) return clearNotes(context, ui)
        if (isClearAlarms(folded)) return clearAlarms(context, ui)
        forgetNeedle(folded)?.let { return forgetMatching(context, ui, it) }
        when (val alarm = alarmTry(raw, folded)) {
            is AlarmTry.Set -> return proposeAlarm(context, ui, alarm.at, alarm.label)
            AlarmTry.NeedTime -> return ui.getString(R.string.alarm_need_time)
            null -> Unit
        }
        noteText(folded)?.let { return proposeNote(context, ui, rawNote(raw) ?: it) }
        null
    }

    fun waiting(): Boolean = synchronized(lock) { draft != null }

    fun drop() = synchronized(lock) { draft = null }

    /** Yes saves the repeated alarm or note. No throws it away. */
    fun answer(context: Context, ui: Context, yes: Boolean): String = synchronized(lock) {
        val item = draft ?: return ui.getString(R.string.kept_no)
        draft = null
        if (!yes) return ui.getString(R.string.kept_no)
        return if (item.alarm) saveAlarm(context, ui, item.at, item.text) else saveNote(context, ui, item.text)
    }

    /** After a reboot, future alarms are armed again. One that just passed still rings. */
    fun reschedule(context: Context) = synchronized(lock) {
        val root = read(context)
        val alarms = root.optJSONArray("alarms") ?: return
        val now = System.currentTimeMillis()
        val keep = JSONArray()
        for (i in 0 until alarms.length()) {
            val item = alarms.optJSONObject(i) ?: continue
            val at = item.optLong("at")
            val whenMs = if (at <= now && now - at <= LATE_MS) now + 5_000 else at
            if (whenMs <= now) continue
            item.put("at", whenMs)
            keep.put(item)
            arm(context, item.optInt("id"), whenMs)
        }
        root.put("alarms", keep)
        write(context, root)
    }

    /** Removes the alarm and returns the words to speak. */
    fun take(context: Context, id: Int): String? = synchronized(lock) {
        val root = read(context)
        val alarms = root.optJSONArray("alarms") ?: return null
        val keep = JSONArray()
        var text: String? = null
        for (i in 0 until alarms.length()) {
            val item = alarms.optJSONObject(i) ?: continue
            if (item.optInt("id") == id) text = item.optString("text")
            else keep.put(item)
        }
        root.put("alarms", keep)
        write(context, root)
        text?.trim()?.ifBlank { null }
    }

    private fun speakList(context: Context, ui: Context): String {
        val root = read(context)
        val notes = root.optJSONArray("notes") ?: JSONArray()
        val alarms = root.optJSONArray("alarms") ?: JSONArray()
        if (notes.length() == 0 && alarms.length() == 0) return ui.getString(R.string.notes_empty)
        val chunks = mutableListOf<String>()
        if (notes.length() > 0) {
            chunks += ui.getString(R.string.notes_header)
            for (i in 0 until notes.length()) chunks += notes.optString(i)
        }
        if (alarms.length() > 0) {
            chunks += ui.getString(R.string.alarms_header)
            for (i in 0 until alarms.length()) {
                val item = alarms.optJSONObject(i) ?: continue
                chunks += ui.getString(
                    R.string.alarm_line,
                    clockText(context, item.optLong("at")),
                    item.optString("text"),
                )
            }
        }
        return chunks.joinToString(". ")
    }

    private fun proposeAlarm(context: Context, ui: Context, at: Long, label: String): String {
        val alarms = read(context).optJSONArray("alarms") ?: JSONArray()
        if (alarms.length() >= MAX_ALARMS) return ui.getString(R.string.alarms_full)
        val text = squash(label).ifBlank { ui.getString(R.string.alarm_bare) }
        draft = Draft(true, at, text)
        return ui.getString(R.string.alarm_confirm, clockText(context, at), text)
    }

    private fun proposeNote(context: Context, ui: Context, text: String): String {
        val clean = squash(text)
        if (clean.isBlank()) return ui.getString(R.string.note_missing)
        val notes = read(context).optJSONArray("notes") ?: JSONArray()
        val folded = Inbox.fold(clean)
        for (i in 0 until notes.length()) {
            if (Inbox.fold(notes.optString(i)) == folded) return ui.getString(R.string.note_exists)
        }
        if (notes.length() >= MAX_NOTES) return ui.getString(R.string.notes_full)
        draft = Draft(false, 0L, clean)
        return ui.getString(R.string.note_confirm, clean)
    }

    private fun saveNote(context: Context, ui: Context, text: String): String {
        val clean = squash(text)
        if (clean.isBlank()) return ui.getString(R.string.note_missing)
        val root = read(context)
        val notes = root.optJSONArray("notes") ?: JSONArray()
        val folded = Inbox.fold(clean)
        for (i in 0 until notes.length()) {
            if (Inbox.fold(notes.optString(i)) == folded) return ui.getString(R.string.note_exists)
        }
        if (notes.length() >= MAX_NOTES) return ui.getString(R.string.notes_full)
        notes.put(clean)
        root.put("notes", notes)
        write(context, root)
        return ui.getString(R.string.note_saved)
    }

    private fun clearNotes(context: Context, ui: Context): String {
        val root = read(context)
        root.put("notes", JSONArray())
        write(context, root)
        return ui.getString(R.string.notes_cleared)
    }

    private fun forgetMatching(context: Context, ui: Context, needle: String): String {
        val root = read(context)
        val notes = root.optJSONArray("notes") ?: JSONArray()
        val keep = JSONArray()
        var removed = 0
        val want = Inbox.fold(needle)
        for (i in 0 until notes.length()) {
            val note = notes.optString(i)
            val folded = Inbox.fold(note)
            val hit = folded.contains(want) || (folded.length >= 8 && want.contains(folded))
            if (hit) removed += 1 else keep.put(note)
        }
        root.put("notes", keep)
        write(context, root)
        return if (removed > 0) ui.getString(R.string.note_forgotten) else ui.getString(R.string.note_missing)
    }

    private fun saveAlarm(context: Context, ui: Context, at: Long, label: String): String {
        val root = read(context)
        val alarms = root.optJSONArray("alarms") ?: JSONArray()
        if (alarms.length() >= MAX_ALARMS) return ui.getString(R.string.alarms_full)
        var id = 1
        for (i in 0 until alarms.length()) {
            id = maxOf(id, (alarms.optJSONObject(i)?.optInt("id") ?: 0) + 1)
        }
        val text = squash(label).ifBlank { ui.getString(R.string.alarm_bare) }
        val item = JSONObject().put("id", id).put("at", at).put("text", text)
        alarms.put(item)
        root.put("alarms", alarms)
        write(context, root)
        val exact = arm(context, id, at)
        val time = clockText(context, at)
        return if (exact) ui.getString(R.string.alarm_set, time)
        else ui.getString(R.string.alarm_set_loose, time)
    }

    private fun clearAlarms(context: Context, ui: Context): String {
        val root = read(context)
        val alarms = root.optJSONArray("alarms") ?: JSONArray()
        for (i in 0 until alarms.length()) {
            val id = alarms.optJSONObject(i)?.optInt("id") ?: continue
            context.getSystemService(AlarmManager::class.java).cancel(pending(context, id))
        }
        root.put("alarms", JSONArray())
        write(context, root)
        return ui.getString(R.string.alarms_cleared)
    }

    private fun arm(context: Context, id: Int, whenMs: Long): Boolean {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = pending(context, id)
        return try {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, intent)
            true
        } catch (_: SecurityException) {
            Log.i(TAG, "alarm inexact id=$id")
            Trace.attach(context)
            Trace.event("alarm inexact")
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, intent)
            false
        }
    }

    private fun pending(context: Context, id: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context,
            1000 + id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmTry(raw: String, folded: String): AlarmTry? {
        if (!wantsAlarm(folded)) return null
        duration(raw)?.let { return it }
        clock(raw)?.let { return it }
        if (noteText(folded) != null) return null
        return AlarmTry.NeedTime
    }

    private fun duration(raw: String): AlarmTry.Set? {
        val minutes = Regex("""(?i)\b(?:en|dentro de)\s+(\d+)\s+minutos?\b""").find(raw)
        if (minutes != null) {
            val count = minutes.groupValues[1].toInt().coerceIn(1, 24 * 60)
            return AlarmTry.Set(System.currentTimeMillis() + count * 60_000L, labelAround(raw, minutes))
        }
        val hours = Regex("""(?i)\b(?:en|dentro de)\s+(\d+)\s+horas?\b""").find(raw)
        if (hours != null) {
            val count = hours.groupValues[1].toInt().coerceIn(1, 48)
            return AlarmTry.Set(System.currentTimeMillis() + count * 3_600_000L, labelAround(raw, hours))
        }
        val half = Regex("""(?i)\b(?:en|dentro de)\s+media\s+hora\b""").find(raw) ?: return null
        return AlarmTry.Set(System.currentTimeMillis() + 30 * 60_000L, labelAround(raw, half))
    }

    private fun clock(raw: String): AlarmTry.Set? {
        val match = Regex(
            """(?i)\b(?:a las|at|um|for|à|a)\s+(\d{1,2}|una|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce)(?:\s*[:.]\s*(\d{2})|\s+y\s+media|\s+y\s+cuarto|\s+menos\s+cuarto)?(?:\s+de\s+la\s+(mañana|manana|tarde|noche))?""",
        ).find(raw) ?: return null
        val word = match.groupValues[1].lowercase()
        var hour = word.toIntOrNull() ?: HOURS[word] ?: return null
        var minute = match.groupValues[2].toIntOrNull() ?: 0
        val said = match.value.lowercase()
        if ("y media" in said) minute = 30
        if ("y cuarto" in said) minute = 15
        if ("menos cuarto" in said) {
            minute = 45
            hour -= 1
        }
        val part = match.groupValues[3].lowercase()
        hour = when (part) {
            "tarde" -> if (hour in 1..11) hour + 12 else hour
            "noche" -> when {
                hour == 12 -> 0
                hour in 1..11 -> hour + 12
                else -> hour
            }
            "mañana", "manana" -> if (hour == 12) 0 else hour
            else -> hour
        }
        if (hour !in 0..23 || minute !in 0..59) return null
        return AlarmTry.Set(next(hour, minute), labelAround(raw, match))
    }

    private fun next(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        if (calendar.timeInMillis <= System.currentTimeMillis() + 30_000) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun labelAround(raw: String, match: MatchResult): String {
        val after = tidy(raw.substring(match.range.last + 1))
        if (after.isNotBlank()) return after
        val before = raw.substring(0, match.range.first)
        return tidy(before.replace(VERB, "").trim())
    }

    private fun tidy(text: String): String {
        return squash(text).replace(Regex("(?i)^(de|para|que|to|about)\\s+"), "").trim()
    }

    private fun squash(text: String): String = text.replace(Regex("\\s+"), " ").trim().take(MAX_CHARS)

    private fun wantsAlarm(folded: String): Boolean {
        return folded.startsWith("avisame") ||
            folded.startsWith("pon una alarma") ||
            folded.startsWith("pon alarma") ||
            folded.startsWith("crea una alarma") ||
            folded.startsWith("recuerdame") ||
            folded.startsWith("wake me") ||
            folded.startsWith("set an alarm") ||
            folded.startsWith("rappelle moi") ||
            folded.startsWith("weck mich")
    }

    private fun rawNote(raw: String): String? {
        val match = Regex(
            """(?i)^(?:recuerda que|acu[eé]rdate de|acu[eé]rdate que|apunta que|recu[eé]rdame que|remember that|remember to|note that|souviens-toi que|souviens toi que|souviens-toi de|souviens toi de|rappelle-moi de|rappelle moi de|merke dir dass|merke dir|ricorda che|ricordami che)\s+(.+)$""",
        ).find(raw.trim()) ?: return null
        return match.groupValues[1].trim().takeIf { it.isNotBlank() }
    }

    private fun noteText(folded: String): String? {
        val match = Regex(
            """^(?:recuerda que|acuerdate de|acuerdate que|apunta que|recuerdame que|remember that|remember to|note that|souviens toi que|souviens toi de|rappelle moi de|merke dir dass|merke dir|ricorda che|ricordami che) (.+)$""",
        ).find(folded) ?: return null
        return match.groupValues[1].trim().takeIf { it.isNotBlank() }
    }

    private fun forgetNeedle(folded: String): String? {
        val match = Regex(
            """^(?:olvida que|borra el recuerdo|borra que|forget that|oublie que|vergiss dass) (.+)$""",
        ).find(folded) ?: return null
        return match.groupValues[1].trim().takeIf { it.length >= 3 }
    }

    private fun isList(folded: String): Boolean {
        return folded == "que recuerdas" ||
            folded.startsWith("que recuerdas") ||
            folded.startsWith("que tienes apuntado") ||
            folded.startsWith("que alarmas") ||
            folded.startsWith("what do you remember") ||
            folded.startsWith("de quoi tu te souviens") ||
            folded.startsWith("woran erinnerst du dich") ||
            folded.startsWith("cosa ricordi")
    }

    private fun isClearNotes(folded: String): Boolean {
        return folded == "borra los recuerdos" ||
            folded.startsWith("borra los recuerdos") ||
            folded == "olvida los recuerdos" ||
            folded.startsWith("olvida los recuerdos") ||
            folded == "borra todo lo apuntado" ||
            folded == "olvida todo lo apuntado" ||
            folded == "forget the notes" ||
            folded == "forget my notes" ||
            folded == "oublie les notes" ||
            folded == "vergiss die notizen"
    }

    private fun isClearAlarms(folded: String): Boolean {
        return folded == "cancela las alarmas" ||
            folded.startsWith("cancela las alarmas") ||
            folded == "borra las alarmas" ||
            folded.startsWith("borra las alarmas") ||
            folded == "quita las alarmas" ||
            folded == "cancel the alarms" ||
            folded == "annule les alarmes" ||
            folded == "vergiss die wecker"
    }

    private fun clockText(context: Context, whenMs: Long): String {
        return DateFormat.getTimeFormat(context).format(Date(whenMs))
    }

    private fun read(context: Context): JSONObject {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        return try {
            if (raw.isBlank()) JSONObject() else JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun write(context: Context, root: JSONObject) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, root.toString()).commit()
    }

    private data class Draft(val alarm: Boolean, val at: Long, val text: String)

    private var draft: Draft? = null

    private sealed class AlarmTry {
        data class Set(val at: Long, val label: String) : AlarmTry()
        data object NeedTime : AlarmTry()
    }

    private val HOURS = mapOf(
        "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "seis" to 6,
        "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12,
    )
    private val VERB = Regex(
        """(?i)^(avísame|avisame|pon una alarma|pon alarma|crea una alarma|recuérdame|recuerdame|wake me|set an alarm|rappelle-moi|rappelle moi|weck mich)\s*""",
    )

    private const val TAG = "GrokEar"
    const val EXTRA_ID = "id"
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(Memory.EXTRA_ID, -1)
        if (id < 0) return
        val text = Memory.take(context, id) ?: return
        val start = Intent(context, ReminderService::class.java).putExtra(ReminderService.EXTRA_TEXT, text)
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(start)
            else context.startService(start)
        } catch (_: Exception) {
            Log.i("GrokEar", "reminder start failed")
            Trace.attach(context)
            Trace.event("reminder start failed")
        }
    }
}

/** Speaks one saved alarm. It does not open the microphone. */
class ReminderService : Service() {
    private var speaker: Speaker? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val store = Store(this)
        val ui = store.localized(this)
        val text = intent?.getStringExtra(EXTRA_TEXT)?.trim().orEmpty().ifBlank {
            ui.getString(R.string.alarm_bare)
        }
        Notifier.ensure(ui)
        val notification = Notifier.reminder(this, text)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    Notifier.REMINDER_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(Notifier.REMINDER_ID, notification)
            }
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        VoiceLevel.apply(this, store.voiceVolume)
        speaker = Speaker(ui, store.locale()).also { it.start() }
        speaker?.say(text) {
            speaker?.stop()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        speaker?.stop()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TEXT = "text"
    }
}
