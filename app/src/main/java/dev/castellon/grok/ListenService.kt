package dev.castellon.grok

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The big button starts this service. Phonemes watch the room. Android's own
 * recognizer writes the question only after the phrase, or while a talk is open.
 */
class ListenService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val work = Executors.newSingleThreadExecutor()
    private val grok = GrokClient()
    private val alive = AtomicBoolean(false)
    @Volatile private var mode = WAKE
    @Volatile private var open = false
    private var handed = false
    private var record: AudioRecord? = null
    private var spotter: KeywordSpotter? = null
    private var stream: OnlineStream? = null
    private var recognizer: SpeechRecognizer? = null
    private var speaker: Speaker? = null
    private var ear: Thread? = null
    private val quiet = Runnable {
        open = false
        grok.clear()
        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }
        wake()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        live = this
        Notifier.ensure(this)
        val store = Store(this)
        speaker = Speaker(localized(), store.locale()).also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Store(this).armed = false
            Alerts.cancel(this)
            handed = true
            stopSelf()
            return START_NOT_STICKY
        }
        Store(this).armed = true
        Alerts.cancel(this)
        val text = getString(R.string.status_preparing)
        try {
            goForeground(text)
        } catch (_: Exception) {
            lose()
            return START_NOT_STICKY
        }
        EarState.listening.value = true
        EarState.status.value = text
        alive.set(true)
        work.execute { prepare() }
        return START_STICKY
    }

    override fun onDestroy() {
        live = null
        alive.set(false)
        main.removeCallbacks(quiet)
        closeMic()
        stream?.release()
        spotter?.release()
        recognizer?.destroy()
        speaker?.stop()
        ear?.interrupt()
        EarState.listening.value = false
        if (!handed && Store(this).armed) lose()
        super.onDestroy()
    }

    /** The center button. If the model is ready, the microphone opens again. */
    fun reactivate() {
        handed = false
        alive.set(true)
        Alerts.cancel(this)
        if (spotter == null) {
            val text = getString(R.string.status_preparing)
            try {
                goForeground(text)
            } catch (_: Exception) {
                lose()
                return
            }
            EarState.listening.value = true
            EarState.status.value = text
            work.execute { prepare() }
        } else {
            main.post { if (open) hear() else wake() }
        }
    }

    fun onIncoming(note: Inbox.Note) {
        main.post {
            if (!alive.get()) return@post
            closeMic()
            mode = TALK
            speaker?.say(getString(R.string.message_ask, note.who)) { hear() }
        }
    }

    private fun prepare() {
        val store = Store(this)
        try {
            val model = ModelStore.ready(this, store.keywordLine()) { percent ->
                val line = getString(R.string.status_downloading, percent)
                main.post {
                    EarState.status.value = line
                    goForeground(line)
                }
            }
            val config = KeywordSpotterConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = model.encoder.absolutePath,
                        decoder = model.decoder.absolutePath,
                        joiner = model.joiner.absolutePath,
                    ),
                    tokens = model.tokens.absolutePath,
                    numThreads = 1,
                    provider = "cpu",
                    modelType = "zipformer2",
                ),
                keywordsFile = java.io.File(model.dir, "keywords.txt").absolutePath,
                keywordsScore = 1.0f,
                keywordsThreshold = 0.25f,
                numTrailingBlanks = 1,
            )
            spotter?.release()
            stream?.release()
            spotter = KeywordSpotter(config = config)
            stream = spotter?.createStream()
            main.post { wake() }
        } catch (_: Exception) {
            main.post {
                EarState.status.value = getString(R.string.status_model_failed)
                lose()
            }
        }
    }

    private fun wake() {
        if (!alive.get()) return
        mode = WAKE
        val line = getString(if (open) R.string.status_talk else R.string.status_listening)
        EarState.status.value = line
        EarState.listening.value = true
        goForeground(line)
        if (!openMic()) {
            lose()
            return
        }
        if (ear?.isAlive != true) {
            ear = Thread({ loop() }, "grok-ear").also { it.start() }
        }
    }

    private fun loop() {
        val buffer = ShortArray(1600)
        val floats = FloatArray(1600)
        while (alive.get() && !Thread.currentThread().isInterrupted) {
            if (mode != WAKE) {
                Thread.sleep(40)
                continue
            }
            val mic = record ?: continue
            val n = try {
                mic.read(buffer, 0, buffer.size)
            } catch (_: Exception) {
                continue
            }
            if (n <= 0) continue
            for (i in 0 until n) floats[i] = buffer[i] / 32768.0f
            val ear = stream ?: continue
            val engine = spotter ?: continue
            ear.acceptWaveform(floats.copyOf(n), 16000)
            while (engine.isReady(ear)) engine.decode(ear)
            val keyword = engine.getResult(ear).keyword
            if (keyword.isNotBlank()) {
                engine.reset(ear)
                main.post { heardWake() }
            }
        }
    }

    private fun heardWake() {
        if (!alive.get() || mode != WAKE) return
        open = true
        closeMic()
        hear()
    }

    private fun hear() {
        if (!alive.get()) return
        mode = HEAR
        main.removeCallbacks(quiet)
        closeMic()
        val ear = recognizer() ?: run {
            lose()
            return
        }
        ear.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Store(this).locale().toLanguageTag())
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
        try {
            ear.startListening(intent)
        } catch (_: Exception) {
            wake()
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
        override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
        override fun onResults(results: android.os.Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            onPhrase(text)
        }
        override fun onError(error: Int) {
            if (!alive.get()) return
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                lose()
                return
            }
            main.postDelayed({ if (!alive.get()) return@postDelayed; if (open) hear() else wake() }, 500)
        }
    }

    private fun onPhrase(raw: String) {
        val store = Store(this)
        val stripped = stripWake(raw, store.wakeWords())
        work.execute {
            val spoken = decide(stripped)
            main.post {
                if (!alive.get()) return@post
                if (spoken.isNullOrBlank()) {
                    next()
                } else {
                    mode = TALK
                    speaker?.say(spoken) { next() }
                }
            }
        }
    }

    private fun decide(text: String): String? {
        val store = Store(localized())
        val ui = localized()
        val folded = Inbox.fold(text)
        if (Inbox.pending != null) {
            if (folded in YES) {
                val note = Inbox.takePending()
                return when {
                    note == null -> null
                    note.text.isBlank() -> ui.getString(R.string.message_hidden, note.who)
                    else -> ui.getString(R.string.message_body, note.who, note.text)
                }
            }
            if (folded in NO) {
                Inbox.dropPending()
                return ui.getString(R.string.no_message)
            }
            Inbox.dropPending()
        }
        if (open && folded in GOODBYE && folded.split(" ").size <= 4) {
            open = false
            grok.clear()
            return ui.getString(R.string.bye)
        }
        val useful = text.trim()
        if (useful.isBlank()) return null
        Commands.local(ui, store, useful)?.let { return it }
        if (!open) open = true
        EarState.status.value = ui.getString(R.string.status_thinking)
        return when (val answer = grok.ask(store, useful, languageName(store))) {
            is GrokClient.Answer.Text -> Commands.fromCloud(ui, store, answer.value) ?: answer.value
            GrokClient.Answer.NeedKey -> ui.getString(R.string.need_key)
            GrokClient.Answer.Fuel -> ui.getString(R.string.fuel)
            GrokClient.Answer.Failed -> ui.getString(R.string.grok_failed)
        }
    }

    private fun next() {
        if (!alive.get()) return
        if (open) {
            val wait = (Store(this).quietMinutes * 60_000f).toLong()
            main.removeCallbacks(quiet)
            main.postDelayed(quiet, wait)
            hear()
        } else {
            main.removeCallbacks(quiet)
            wake()
        }
    }

    private fun recognizer(): SpeechRecognizer? {
        recognizer?.let { return it }
        recognizer = try {
            if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            } else {
                SpeechRecognizer.createSpeechRecognizer(this)
            }
        } catch (_: Exception) {
            try {
                SpeechRecognizer.createSpeechRecognizer(this)
            } catch (_: Exception) {
                null
            }
        }
        return recognizer
    }

    private fun openMic(): Boolean {
        closeMic()
        val rate = 16000
        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val min = AudioRecord.getMinBufferSize(rate, channel, encoding)
        if (min <= 0) return false
        val mic = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            rate,
            channel,
            encoding,
            min * 2,
        )
        if (mic.state != AudioRecord.STATE_INITIALIZED) {
            mic.release()
            return false
        }
        mic.startRecording()
        record = mic
        return true
    }

    private fun closeMic() {
        val mic = record ?: return
        record = null
        try {
            mic.stop()
        } catch (_: Exception) {
        }
        mic.release()
    }

    private fun lose() {
        if (handed) return
        handed = true
        alive.set(false)
        EarState.listening.value = false
        val store = Store(this)
        if (store.armed) {
            store.alertIndex = 0
            try {
                startForegroundService(Intent(this, AlertService::class.java))
            } catch (_: Exception) {
            }
        }
        stopSelf()
    }

    private fun goForeground(text: String) {
        val notification = Notifier.listening(this, text)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(Notifier.LISTEN_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(Notifier.LISTEN_ID, notification)
        }
    }

    private fun localized() = Store(this).localized(this)

    private fun languageName(store: Store): String = when (store.language) {
        "es" -> "Spanish"
        "en" -> "English"
        "fr" -> "French"
        "de" -> "German"
        "it" -> "Italian"
        else -> store.locale().getDisplayLanguage(java.util.Locale.ENGLISH)
    }

    private fun stripWake(text: String, wakes: List<String>): String {
        val folded = Inbox.fold(text)
        val words = text.trim().split(Regex("\\s+"))
        for (wake in wakes) {
            if (folded.startsWith(wake)) {
                val drop = wake.split(" ").size
                return words.drop(drop).joinToString(" ")
            }
        }
        return text.trim()
    }

    companion object {
        const val ACTION_STOP = "dev.castellon.grok.STOP"
        private const val WAKE = 0
        private const val HEAR = 1
        private const val TALK = 2

        @Volatile
        var live: ListenService? = null

        private val YES = setOf("si", "sí", "yes", "oui", "ja", "sì", "vale")
        private val NO = setOf("no", "non", "nein")
        private val GOODBYE = setOf(
            "gracias", "vale", "adios", "hasta luego", "bye", "goodbye",
            "au revoir", "tschuss", "ciao", "nada gracias", "ok gracias", "de rien", "danke",
        )

        fun start(context: Context) {
            Store(context).armed = true
            val running = live
            if (running != null) {
                running.reactivate()
                return
            }
            androidx.core.content.ContextCompat.startForegroundService(
                context,
                Intent(context, ListenService::class.java),
            )
        }
    }
}
