package ch.castellon.oigo

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A short ring of safe events. The person sends it from Settings.
 * Nothing leaves the phone until they publish the GitHub issue or the email.
 * The report has no key, no message text, no phone numbers, and no names.
 */
object Trace {
    const val ISSUES = "https://github.com/antonio-castellon/Grok_Android/issues"
    const val SITE = "https://castellon.ch/"
    private const val MAIL = "antonio@castellon.ch"
    private const val GMAIL = "com.google.android.gm"
    private const val CAP = 100
    private const val KEY = "trace_log"
    private val lines = ArrayDeque<String>()
    private val lock = Any()
    private var app: Context? = null
    private var loaded = false
    private val safeWord = setOf(
        "si", "no", "yes", "oui", "ja", "vale", "non", "nein",
        "grok", "grock", "hola", "hey", "llamar", "llama", "call", "appelle", "ruf",
        "gracias", "thanks", "merci", "danke", "grazie", "a",
    )

    fun attach(context: Context) {
        val base = context.applicationContext
        synchronized(lock) {
            app = base
            if (loaded) return
            loaded = true
            val saved = base.getSharedPreferences("grok", Context.MODE_PRIVATE)
                .getString(KEY, "")
                .orEmpty()
            saved.lineSequence().filter { it.isNotBlank() }.forEach { lines.addLast(it.take(180)) }
            while (lines.size > CAP) lines.removeFirst()
        }
    }

    fun event(line: String) {
        val row = "${stamp()} ${sanitize(line)}"
        val snapshot: String
        synchronized(lock) {
            lines.addLast(row)
            while (lines.size > CAP) lines.removeFirst()
            snapshot = lines.joinToString("\n")
        }
        app?.getSharedPreferences("grok", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY, snapshot.takeLast(12_000))
            ?.apply()
    }

    /** Keeps a command word. A name or any other word becomes "other". */
    fun word(kind: String, folded: String) {
        val first = folded.trim().substringBefore(' ')
        val shown = if (first in safeWord) first else "other"
        event("$kind $shown")
    }

    fun send(context: Context) {
        attach(context)
        event("trace send")
        val report = report(context)
        val subject = "GROK-VOICE-DEBUG ${Store(context).installId()}"
        val email = emailIntent(context, subject, report)
        val gmail = Intent(email).setPackage(GMAIL)
        if (resolves(context, gmail)) {
            context.startActivity(gmail)
            return
        }
        if (resolves(context, email)) {
            val chooser = Intent.createChooser(email, context.getString(R.string.trace_chooser))
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooser)
            return
        }
        context.startActivity(mailto(subject, report))
    }

    private fun report(context: Context): String {
        val store = Store(context)
        val info = try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
        val version = info?.versionName ?: "?"
        val code = if (info != null && Build.VERSION.SDK_INT >= 28) info.longVersionCode.toString() else "?"
        val listener = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        )?.contains(context.packageName) == true
        fun granted(permission: String): Boolean {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        val events = synchronized(lock) { lines.joinToString("\n") }
        val notice = if (Inbox.pending != null) "waiting" else "none"
        val key = if (store.apiKey.isBlank()) "missing" else "present"
        return buildString {
            appendLine("GROK-VOICE-DEBUG")
            appendLine("id: ${store.installId()}")
            appendLine("app: $version ($code)")
            appendLine("android: ${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("lang: ${store.language} locale: ${store.locale().toLanguageTag()}")
            appendLine("wake: ${store.wake}")
            appendLine(
                "armed: ${store.armed} listening: ${EarState.listening.value} conversation: ${EarState.inConversation.value}",
            )
            appendLine("calls: ${store.calls} whatsapp: ${store.whatsapp} telegram: ${store.telegram}")
            appendLine("volume: ${store.voiceVolume} quiet: ${store.quietMinutes} hours: ${store.contextHours}")
            appendLine("model: ${store.model} key: $key")
            appendLine(
                "mic: ${granted(Manifest.permission.RECORD_AUDIO)} " +
                    "contacts: ${granted(Manifest.permission.READ_CONTACTS)} " +
                    "phone: ${granted(Manifest.permission.CALL_PHONE)} listener: $listener",
            )
            appendLine("notice: $notice")
            appendLine()
            append(events.ifBlank { "(no events yet)" })
        }
    }

    private fun emailIntent(context: Context, subject: String, report: String): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(MAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, report)
        }
        try {
            val dir = File(context.cacheDir, "trace").apply { mkdirs() }
            val file = File(dir, "grok-voice-debug.txt")
            file.writeText(report)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.clipData = ClipData.newRawUri("trace", uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.grantUriPermission(GMAIL, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val handlers = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, 0)
            }
            for (handler in handlers) {
                context.grantUriPermission(
                    handler.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        } catch (_: Exception) {
            // The mail body still carries the report when the file cannot be shared.
        }
        return intent
    }

    private fun mailto(subject: String, report: String): Intent {
        val uri = Uri.parse("mailto:$MAIL").buildUpon()
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", report)
            .build()
        return Intent(Intent.ACTION_SENDTO, uri)
    }

    private fun resolves(context: Context, intent: Intent): Boolean {
        val found = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0)
        }
        return found.isNotEmpty()
    }

    private fun sanitize(line: String): String {
        return line
            .replace(Regex("[\\r\\n\\t]"), " ")
            .replace(Regex("\\d{6,}"), "#")
            .replace(Regex("(?i)xai-[A-Za-z0-9_\\-]+"), "xai-#")
            .trim()
            .take(140)
    }

    private fun stamp(): String {
        return SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date())
    }
}
