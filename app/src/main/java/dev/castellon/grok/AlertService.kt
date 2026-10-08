package dev.castellon.grok

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** Speaks that the microphone is not listening. It does not open the microphone. */
class AlertService : Service() {
    private var speaker: Speaker? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val store = Store(this)
        val ui = store.localized(this)
        Notifier.ensure(ui)
        val text = ui.getString(R.string.mic_stopped)
        val notification = Notifier.alert(this, text)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    Notifier.ALERT_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(Notifier.ALERT_ID, notification)
            }
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!store.armed || EarState.listening.value) {
            stopSelf()
            return START_NOT_STICKY
        }
        val index = store.alertIndex
        speaker = Speaker(ui, store.locale()).also { it.start() }
        speaker?.say(text) {
            val next = index + 1
            if (next < store.alertRepeats && store.armed && !EarState.listening.value) {
                Alerts.scheduleNext(this, next, store.alertMinutes * 60_000L)
            } else {
                Alerts.cancel(this)
            }
            speaker?.stop()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        speaker?.stop()
        super.onDestroy()
    }
}
