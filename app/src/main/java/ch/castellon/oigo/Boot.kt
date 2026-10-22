package ch.castellon.oigo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Opt-in. After a reboot it only re-arms saved alarms and posts a notification.
 * It never starts a service: the tap opens the app, which starts listening.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = Store(context)
        if (!acts(intent.action, store.bootStart)) return
        Memory.reschedule(context)
        Notifier.bootReady(store.localized(context))
    }

    companion object {
        fun acts(action: String?, enabled: Boolean) = enabled && action == Intent.ACTION_BOOT_COMPLETED
    }
}