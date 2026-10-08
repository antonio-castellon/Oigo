package ch.castellon.oigo

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/** Android's own voice. One engine per service. */
class Speaker(context: Context, private val locale: Locale) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false
    private var waiting: Pair<String, () -> Unit>? = null
    private var turn = 0
    private var pendingDone: Runnable? = null

    fun start() {
        engine = TextToSpeech(appContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                ready = false
                val pending = waiting
                waiting = null
                pending?.second?.invoke()
                return@TextToSpeech
            }
            engine?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            val lang = engine?.setLanguage(locale)
            Log.i(TAG, "tts ready lang=$lang")
            Trace.event("tts ready")
            ready = true
            waiting?.let { (text, done) ->
                waiting = null
                say(text, done)
            }
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
        val mine = ++turn
        var finished = false
        fun finish() {
            if (finished || turn != mine) return
            finished = true
            pendingDone?.let { main.removeCallbacks(it) }
            pendingDone = null
            if (Looper.myLooper() == Looper.getMainLooper()) done() else main.post(done)
        }
        val timeout = Runnable {
            Log.i(TAG, "tts timeout")
            Trace.event("tts timeout")
            engine?.stop()
            finish()
        }
        pendingDone = timeout
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.i(TAG, "tts start")
                Trace.event("tts start")
            }

            override fun onDone(utteranceId: String?) {
                main.post { finish() }
            }

            @Deprecated("Use the error code overload")
            override fun onError(utteranceId: String?) {
                main.post { finish() }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.i(TAG, "tts error $errorCode")
                Trace.event("tts error $errorCode")
                main.post { finish() }
            }
        })
        val code = engine?.speak(line, TextToSpeech.QUEUE_FLUSH, null, "grok-$mine") ?: TextToSpeech.ERROR
        Log.i(TAG, "tts speak code=$code chars=${line.length}")
        Trace.event("tts speak code=$code chars=${line.length}")
        if (code == TextToSpeech.ERROR) {
            finish()
            return
        }
        val waitMs = (line.length * 90L).coerceIn(2500L, 20000L)
        main.postDelayed(timeout, waitMs)
    }

    fun stop() {
        turn++
        pendingDone?.let { main.removeCallbacks(it) }
        pendingDone = null
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
    }

    private companion object {
        const val TAG = "GrokEar"
    }
}

/** Media volume for the voice, and a hush so the recognizer beep stays silent. */
internal object VoiceLevel {
    fun apply(context: Context, percent: Int) {
        val audio = audio(context)
        unmute(audio)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val index = (max * percent.coerceIn(0, 100) / 100).coerceIn(0, max)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, index, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
    }

    fun hush(context: Context) {
        val audio = audio(context)
        mute(audio, AudioManager.STREAM_SYSTEM)
        mute(audio, AudioManager.STREAM_NOTIFICATION)
        if (android.os.Build.VERSION.SDK_INT >= 26) mute(audio, 11)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
    }

    private fun unmute(audio: AudioManager) {
        unmuteStream(audio, AudioManager.STREAM_SYSTEM)
        unmuteStream(audio, AudioManager.STREAM_NOTIFICATION)
        if (android.os.Build.VERSION.SDK_INT >= 26) unmuteStream(audio, 11)
    }

    private fun mute(audio: AudioManager, stream: Int) {
        try {
            @Suppress("DEPRECATION")
            audio.setStreamMute(stream, true)
        } catch (_: Exception) {
        }
    }

    private fun unmuteStream(audio: AudioManager, stream: Int) {
        try {
            @Suppress("DEPRECATION")
            audio.setStreamMute(stream, false)
        } catch (_: Exception) {
        }
    }

    private fun audio(context: Context): AudioManager {
        return context.getSystemService(AudioManager::class.java)
    }
}
