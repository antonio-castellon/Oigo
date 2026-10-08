package ch.castellon.oigo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object Notifier {
    const val LISTEN = "listen"
    const val ALERT = "alert"
    const val LISTEN_ID = 42
    const val ALERT_ID = 43
    const val REMIND_ID = 44
    const val REMINDER_ID = 45
    const val NOTICE_ID = 46
    private const val REMINDER = "reminder"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val listen = NotificationChannel(LISTEN, context.getString(R.string.channel_listen), NotificationManager.IMPORTANCE_LOW)
        val alert = NotificationChannel(ALERT, context.getString(R.string.channel_alert), NotificationManager.IMPORTANCE_HIGH)
        val reminder = NotificationChannel(REMINDER, context.getString(R.string.channel_reminder), NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(listen)
        manager.createNotificationChannel(alert)
        manager.createNotificationChannel(reminder)
    }

    fun listening(context: Context, text: String): Notification {
        return base(context, LISTEN, text)
            .addAction(0, context.getString(R.string.stop_listening), stopIntent(context))
            .build()
    }

    fun alert(context: Context, text: String): Notification {
        return base(context, ALERT, text).build()
    }

    fun reminder(context: Context, text: String): Notification {
        return base(context, REMINDER, text).build()
    }

    fun remind(context: Context) {
        ensure(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(REMIND_ID, base(context, ALERT, context.getString(R.string.boot_remind)).build())
    }

    private fun base(context: Context, channel: String, text: String): NotificationCompat.Builder {
        val open = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(channel == LISTEN)
            .setOnlyAlertOnce(true)
    }

    private fun stopIntent(context: Context): PendingIntent {
        val intent = Intent(context, ListenService::class.java).setAction(ListenService.ACTION_STOP)
        return PendingIntent.getService(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
