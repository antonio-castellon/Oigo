package dev.castellon.grok

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

object Alerts {
    const val ACTION = "dev.castellon.grok.ALERT"
    private const val REQUEST = 17

    fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.cancel(pending(context))
        Store(context).alertIndex = 0
    }

    fun scheduleNext(context: Context, index: Int, delayMs: Long) {
        val store = Store(context)
        store.alertIndex = index
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = pending(context)
        val whenMs = SystemClock.elapsedRealtime() + delayMs
        try {
            alarm.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, whenMs, intent)
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, whenMs, intent)
        }
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, AlertReceiver::class.java).setAction(ACTION)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST, intent, flags)
    }
}

class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Store(context).armed || EarState.listening.value) return
        val start = Intent(context, AlertService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(start)
        } else {
            context.startService(start)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = Store(context)
        if (!store.armed) return
        store.armed = false
        Notifier.remind(Store(context).localized(context))
    }
}
