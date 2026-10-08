package dev.castellon.grok

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Sees WhatsApp and Telegram notices only after the person turns the option on
 * and grants notification access. The message text is stored. It is not spoken
 * until the person agrees.
 */
class MessageListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val store = Store(this)
        val channel = when (sbn.packageName) {
            "com.whatsapp", "com.whatsapp.w4b" -> if (store.whatsapp) "WhatsApp" else return
            "org.telegram.messenger", "org.telegram.messenger.web" -> if (store.telegram) "Telegram" else return
            else -> return
        }
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = notification.extras ?: return
        val who = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        if (who.isBlank() || who.equals(channel, ignoreCase = true)) return
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: ""
        val note = Inbox.add(channel, who, text)
        ListenService.live?.onIncoming(note)
    }
}
