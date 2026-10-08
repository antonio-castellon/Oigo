package ch.castellon.oigo

import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Sees WhatsApp and Telegram notices only after the person turns the option on
 * and grants notification access. The message text is stored. It is not spoken
 * until the person agrees. The question is asked even when no conversation is open.
 */
class MessageListener : NotificationListenerService() {
    private val seen = ArrayDeque<String>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        live = this
        scanActive()
    }

    override fun onListenerDisconnected() {
        if (live == this) live = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Trace.attach(this)
        consider(sbn, posted = true)
    }

    /** A notice already in the shade is not posted again. Look at the ones still there. */
    fun scanActive() {
        Trace.attach(this)
        val active = try {
            activeNotifications
        } catch (_: Exception) {
            null
        } ?: return
        for (sbn in active) consider(sbn, posted = false)
    }

    private fun consider(sbn: StatusBarNotification, posted: Boolean) {
        val store = Store(this)
        val channel = when (sbn.packageName) {
            "com.whatsapp", "com.whatsapp.w4b" -> {
                if (store.whatsapp) "WhatsApp" else {
                    if (posted) Trace.event("notice whatsapp off")
                    return
                }
            }
            "org.telegram.messenger", "org.telegram.messenger.web" -> {
                if (store.telegram) "Telegram" else {
                    if (posted) Trace.event("notice telegram off")
                    return
                }
            }
            else -> return
        }
        val notification = sbn.notification ?: return
        if (!posted && sbn.key in seen) return
        val extras = notification.extras ?: return
        val text = body(extras)
        val summary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        if (text.isBlank()) {
            Log.i(TAG, "notice skip empty")
            Trace.event("notice $channel skip empty summary=$summary")
            return
        }
        val who = sender(extras, channel).ifBlank { channel }
        Log.i(TAG, "notice chars=${text.length}")
        Trace.event("notice $channel chars=${text.length} summary=$summary")
        remember(sbn.key)
        val note = Inbox.add(channel, who, text) ?: return
        val ear = ListenService.live
        if (ear != null && ear.isRunning()) {
            Log.i(TAG, "notice ask listening")
            Trace.event("notice ask listening")
            ear.onIncoming(note)
        } else {
            Log.i(TAG, "notice ask idle")
            Trace.event("notice ask idle")
            MessageNotice.start(this, note.who)
        }
    }

    private fun remember(key: String) {
        if (key in seen) return
        seen.addLast(key)
        while (seen.size > 100) seen.removeFirst()
    }

    private fun sender(extras: Bundle, channel: String): String {
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        if (title.isNotBlank() && !title.equals(channel, ignoreCase = true)) return title
        val conversation = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim().orEmpty()
        if (conversation.isNotBlank() && !conversation.equals(channel, ignoreCase = true)) return conversation
        val last = lastMessage(extras) ?: return ""
        val person = last.getCharSequence("sender")?.toString()?.trim().orEmpty()
        if (person.isNotBlank() && !person.equals(channel, ignoreCase = true)) return person
        return ""
    }

    private fun body(extras: Bundle): String {
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        if (big.isNotBlank()) return big
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (text.isNotBlank()) return text
        return lastMessage(extras)?.getCharSequence("text")?.toString()?.trim().orEmpty()
    }

    private fun lastMessage(extras: Bundle): Bundle? {
        val messages = if (Build.VERSION.SDK_INT >= 33) {
            extras.getParcelableArray(Notification.EXTRA_MESSAGES, Bundle::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        } ?: return null
        return messages.lastOrNull() as? Bundle
    }

    companion object {
        private const val TAG = "GrokEar"
        @Volatile private var live: MessageListener? = null

        fun rescan(context: Context) {
            val ear = live
            if (ear != null) {
                ear.scanActive()
                return
            }
            requestRebind(ComponentName(context, MessageListener::class.java))
        }
    }
}

/** Speaks that a message arrived while the microphone is off. It does not open the microphone. */
class MessageNotice : Service() {
    private var speaker: Speaker? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val store = Store(this)
        val ui = store.localized(this)
        val text = intent?.getStringExtra(EXTRA_TEXT)?.trim().orEmpty()
        if (text.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        Notifier.ensure(ui)
        val notification = Notifier.alert(ui, ui.getString(R.string.message_waiting))
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    Notifier.NOTICE_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(Notifier.NOTICE_ID, notification)
            }
        } catch (error: Exception) {
            Log.i(TAG, "message notice foreground ${error.javaClass.simpleName}")
            Trace.event("message notice foreground ${error.javaClass.simpleName}")
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
        private const val TAG = "GrokEar"
        private const val EXTRA_TEXT = "text"

        fun start(context: Context, who: String) {
            val ui = Store(context).localized(context)
            val text = ui.getString(R.string.message_ask_idle, who)
            val intent = Intent(context, MessageNotice::class.java).putExtra(EXTRA_TEXT, text)
            try {
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
                else context.startService(intent)
            } catch (error: Exception) {
                Log.i(TAG, "message notice start ${error.javaClass.simpleName}")
                Trace.event("message notice start ${error.javaClass.simpleName}")
            }
        }
    }
}
