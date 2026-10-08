package dev.castellon.grok

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Android's own voice. One engine per service. */
class Speaker(context: Context, private val locale: Locale) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false
    private var waiting: Pair<String, () -> Unit>? = null

    fun start() {
        engine = TextToSpeech(appContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                ready = false
                val pending = waiting
                waiting = null
                pending?.second?.invoke()
                return@TextToSpeech
            }
            engine?.language = locale
            ready = true
            waiting?.let { (text, done) -> say(text, done) }
        }
    }

    fun say(text: String, done: () -> Unit) {
        val line = text.trim()
        if (!ready || engine == null) {
            waiting = line to done
            if (engine == null) done()
            return
        }
        if (line.isEmpty()) {
            done()
            return
        }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                main.post(done)
            }
            override fun onError(utteranceId: String?) {
                main.post(done)
            }
        })
        engine?.speak(line, TextToSpeech.QUEUE_FLUSH, null, "grok")
    }

    fun stop() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
    }
}
