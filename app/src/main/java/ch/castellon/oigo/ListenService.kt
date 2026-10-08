package ch.castellon.oigo

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
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The big button starts this service. Phonemes watch the room. Android's own
 * recognizer writes the question only after the phrase, or while a talk is open.
 */
class ListenService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val work = Executors.newSingleThreadExecutor()
    private lateinit var grok: GrokClient
    private val alive = AtomicBoolean(false)
    @Volatile private var mode = WAKE
    private val again = Runnable {
        if (!alive.get()) return@Runnable
        when {
            messageEar -> listenForAnswer()
            followEar -> listenFollow()
            awaitingAnswer() -> hear()
            else -> wake()
        }
    }
    /** Tone, then the ear, after the message question. The mic stays shut while the voice talks. */
    private val chimeRunnable = Runnable {
        if (!alive.get() || mode != TALK) return@Runnable
        chime()
        main.removeCallbacks(afterAskRunnable)
        main.postDelayed(afterAskRunnable, 280)
    }
    private val afterAskRunnable = Runnable {
        if (!alive.get() || mode != TALK) return@Runnable
        hearMessage()
    }
    private val answerTimeout = Runnable {
        if (!alive.get() || !messageEar) return@Runnable
        Log.i(TAG, "message ear timeout")
        Trace.event("message ear timeout")
        messageEar = false
        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }
        if (open) hear() else wake()
    }
    /** After a message is read, a few minutes of speech recognition for a call or another order. */
    private val followTimeout = Runnable {
        if (!alive.get() || !followEar) return@Runnable
        Log.i(TAG, "command ear timeout")
        Trace.event("command ear timeout")
        followEar = false
        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }
        wake()
    }
    /** Speech during the key-phrase ear. The phoneme model misses a spoken order, so this catches it. */
    private val catchTimeout = Runnable {
        if (!alive.get() || !catchEar) return@Runnable
        Log.i(TAG, "catch timeout")
        Trace.event("catch timeout")
        catchEar = false
        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }
        wake()
    }
    @Volatile private var open = false
    private var handed = false
    private var record: AudioRecord? = null
    private var spotter: KeywordSpotter? = null
    private var stream: OnlineStream? = null
    private var recognizer: SpeechRecognizer? = null
    private var speaker: Speaker? = null
    private var ear: Thread? = null
    @Volatile private var earOn = false
    /** A few seconds of speech recognition after the message question. Not a conversation. */
    @Volatile private var messageEar = false
    private var messageEarUntil = 0L
    /** Closed chat, just after a message was read. Local orders work here. Not a conversation. */
    @Volatile private var followEar = false
    private var followUntil = 0L
    /** A short listen grabbed because someone spoke while the key-phrase ear was on. */
    @Volatile private var catchEar = false
    private var catchReadyAt = 0L
    private var chime: android.media.ToneGenerator? = null
    private val quiet = Runnable {
        open = false
        EarState.reply.value = ""
        Memory.drop()
        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }
        wake()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        grok = GrokClient(applicationContext)
        live = this
        Trace.attach(this)
        Notifier.ensure(this)
        val store = Store(this)
        speaker = Speaker(localized(), store.locale()).also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Store(this).armed = false
            Alerts.cancel(this)
            handed = true
            EarState.listening.value = false
            EarState.status.value = ""
            EarState.inConversation.value = false
            EarState.reply.value = ""
            Memory.drop()
            Trace.event("listen stop")
            stopSelf()
            return START_NOT_STICKY
        }
        Store(this).armed = true
        Trace.event("listen start")
        Alerts.cancel(this)
        MessageListener.rescan(this)
        val text = phrase(R.string.status_preparing)
        try {
            goForeground(text)
        } catch (_: Exception) {
            lose()
            return START_NOT_STICKY
        }
        EarState.listening.value = true
        EarState.status.value = text
        alive.set(true)
        if (spotter != null && stream?.ptr != 0L) {
            main.post { if (open) hear() else wake() }
        } else {
            work.execute { prepare() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        live = null
        alive.set(false)
        messageEar = false
        followEar = false
        catchEar = false
        main.removeCallbacks(quiet)
        main.removeCallbacks(again)
        main.removeCallbacks(answerTimeout)
        main.removeCallbacks(followTimeout)
        main.removeCallbacks(catchTimeout)
        main.removeCallbacks(chimeRunnable)
        main.removeCallbacks(afterAskRunnable)
        releaseChime()
        closeMic()
        stopEar()
        stream?.release()
        spotter?.release()
        stream = null
        spotter = null
        recognizer?.destroy()
        speaker?.stop()
        VoiceLevel.apply(this, Store(this).voiceVolume)
        EarState.listening.value = false
        EarState.inConversation.value = false
        EarState.reply.value = ""
        Memory.drop()
        if (!handed && Store(this).armed) lose()
        super.onDestroy()
    }

    /** The center button. If the model is ready, the microphone opens again. */
    fun reactivate() {
        handed = false
        alive.set(true)
        Alerts.cancel(this)
        if (spotter == null) {
            val text = phrase(R.string.status_preparing)
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

    fun isRunning(): Boolean = alive.get()

    /** Asks about a stored message. Conversation does not have to be open. */
    fun onIncoming(note: Inbox.Note) {
        main.post {
            if (!alive.get()) return@post
            val voice = speaker ?: return@post
            main.removeCallbacks(chimeRunnable)
            main.removeCallbacks(afterAskRunnable)
            Inbox.markAsked()
            closeMic()
            mode = TALK
            VoiceLevel.apply(this, Store(this).voiceVolume)
            voice.say(phrase(R.string.message_ask, note.who)) {
                afterAsk()
            }
        }
    }

    private fun prepare() {
        val store = Store(this)
        try {
            val model = ModelStore.ready(this, store.keywordsText()) { percent ->
                val line = phrase(R.string.status_downloading, percent)
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
                maxActivePaths = 8,
                keywordsFile = java.io.File(model.dir, "keywords.txt").absolutePath,
                keywordsScore = 2.0f,
                keywordsThreshold = 0.10f,
                numTrailingBlanks = 1,
            )
            closeMic()
            stopEar()
            stream?.release()
            spotter?.release()
            stream = null
            spotter = null
            spotter = KeywordSpotter(config = config)
            stream = spotter?.createStream()
            Log.i(TAG, "kws ready")
            Trace.event("kws ready")
            main.post { wake() }
        } catch (error: Exception) {
            Log.e(TAG, "prepare failed", error)
            Trace.event("prepare failed ${error.javaClass.simpleName}")
            main.post {
                EarState.status.value = phrase(R.string.status_model_failed)
                lose()
            }
        }
    }

    private fun wake() {
        if (!alive.get()) return
        messageEar = false
        followEar = false
        catchEar = false
        main.removeCallbacks(answerTimeout)
        main.removeCallbacks(followTimeout)
        main.removeCallbacks(catchTimeout)
        main.removeCallbacks(chimeRunnable)
        main.removeCallbacks(afterAskRunnable)
        releaseChime()
        val waiting = Inbox.pending
        if (waiting != null && Inbox.hasUnasked()) {
            onIncoming(waiting)
            return
        }
        main.removeCallbacks(again)
        VoiceLevel.apply(this, Store(this).voiceVolume)
        mode = WAKE
        EarState.inConversation.value = open
        val line = phrase(R.string.status_listening)
        EarState.status.value = line
        EarState.listening.value = true
        goForeground(line)
        if (!openMic()) {
            lose()
            return
        }
        if (ear?.isAlive != true) {
            earOn = true
            ear = Thread({ loop() }, "grok-ear").also { it.start() }
        }
    }

    /** The native stream must not be deleted while this thread is inside it. */
    private fun stopEar() {
        earOn = false
        val thread = ear
        ear = null
        thread?.interrupt()
        if (thread != null && thread != Thread.currentThread() && thread.isAlive) {
            try {
                thread.join(1500)
            } catch (_: InterruptedException) {
            }
        }
    }

    /** Phoneme ear only. Opening speech recognition on room noise plays a beep each time. */
    private fun loop() {
        val buffer = ShortArray(1600)
        val floats = FloatArray(1600)
        var samples = 0
        var energy = 0.0
        while (earOn && alive.get() && !Thread.currentThread().isInterrupted) {
            if (mode != WAKE) {
                try {
                    Thread.sleep(40)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }
            val mic = record ?: continue
            val n = try {
                mic.read(buffer, 0, buffer.size)
            } catch (_: Exception) {
                continue
            }
            if (n <= 0) continue
            var sum = 0.0
            for (i in 0 until n) {
                val sample = buffer[i] / 32768.0
                floats[i] = sample.toFloat()
                sum += sample * sample
            }
            energy += sum
            samples += n
            val heard = stream ?: continue
            val engine = spotter ?: continue
            if (heard.ptr == 0L) continue
            heard.acceptWaveform(floats.copyOf(n), 16000)
            while (earOn && engine.isReady(heard)) engine.decode(heard)
            val keyword = engine.getResult(heard).keyword
            if (samples >= 32000) {
                val rms = kotlin.math.sqrt(energy / samples)
                Log.i(TAG, "mic rms=${String.format(Locale.US, "%.4f", rms)}")
                samples = 0
                energy = 0.0
            }
            if (keyword.isNotBlank()) {
                Log.i(TAG, "wake $keyword")
                Trace.event(
                    when {
                        keyword.contains("GROK_YES") -> "wake yes"
                        keyword.contains("GROK_NO") -> "wake no"
                        else -> "wake phrase"
                    },
                )
                engine.reset(heard)
                main.post {
                    when {
                        keyword.contains("GROK_YES") -> heardAnswer(true)
                        keyword.contains("GROK_NO") -> heardAnswer(false)
                        else -> heardWake()
                    }
                }
            }
        }
    }

    /**
     * Yes or no for a waiting message. The phrase is heard by the same ear that
     * waits for hola grok. It does not open a conversation.
     */
    private fun heardAnswer(yes: Boolean) {
        if (!alive.get() || mode != WAKE) return
        if (Inbox.pending == null) return
        closeMic()
        mode = TALK
        val spoken = spokenMessage(yes)
        if (spoken.isNullOrBlank()) {
            wake()
            return
        }
        EarState.inConversation.value = open
        EarState.reply.value = spoken
        VoiceLevel.apply(this, Store(this).voiceVolume)
        val voice = speaker
        if (voice == null) wake() else voice.say(spoken) { if (open) hear() else hearFollow() }
    }

    private fun heardWake() {
        if (!alive.get() || mode != WAKE) return
        open = true
        EarState.inConversation.value = true
        closeMic()
        mode = TALK
        val hello = phrase(R.string.hello)
        EarState.reply.value = hello
        EarState.status.value = phrase(R.string.status_your_turn)
        VoiceLevel.apply(this, Store(this).voiceVolume)
        val voice = speaker
        if (voice == null) {
            hear()
        } else {
            voice.say(hello) { hear() }
        }
    }

    /**
     * The question itself says "grok, sí", so the microphone stays closed until
     * the voice has finished and a short tone has played. Then speech recognition
     * listens for a few seconds. That does not open a conversation. A missed
     * answer leaves the notice waiting, and the phoneme ear can still catch a
     * later "grok, sí".
     */
    private fun afterAsk() {
        if (!alive.get()) return
        main.removeCallbacks(chimeRunnable)
        main.removeCallbacks(afterAskRunnable)
        // Let the speaker tail die, then the tone. The ear opens after the tone.
        main.postDelayed(chimeRunnable, 180)
    }

    private fun hearMessage() {
        if (!alive.get()) return
        if (Inbox.pending == null) {
            messageEar = false
            if (open) hear() else wake()
            return
        }
        messageEar = true
        messageEarUntil = android.os.SystemClock.elapsedRealtime() + 22_000L
        main.removeCallbacks(answerTimeout)
        main.postDelayed(answerTimeout, 22_000L)
        Log.i(TAG, "message ear")
        Trace.event("message ear")
        listenForAnswer()
    }

    private fun listenForAnswer() {
        if (!alive.get() || !messageEar) return
        if (Inbox.pending == null || android.os.SystemClock.elapsedRealtime() >= messageEarUntil) {
            messageEar = false
            main.removeCallbacks(answerTimeout)
            if (open) hear() else wake()
            return
        }
        releaseChime()
        EarState.inConversation.value = open
        startStt(phrase(R.string.status_message_yes))
    }

    private fun chime() {
        releaseChime()
        val tone = try {
            android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 90)
        } catch (error: Exception) {
            Log.i(TAG, "chime failed ${error.javaClass.simpleName}")
            null
        } ?: return
        chime = tone
        try {
            tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {
        }
    }

    private fun releaseChime() {
        val tone = chime ?: return
        chime = null
        try {
            tone.release()
        } catch (_: Exception) {
        }
    }

    private fun hear() {
        if (!alive.get()) return
        startStt(phrase(R.string.status_your_turn))
    }

    private fun startStt(status: String) {
        if (!alive.get()) return
        mode = HEAR
        main.removeCallbacks(quiet)
        main.removeCallbacks(again)
        closeMic()
        EarState.status.value = status
        val ear = recognizer() ?: run {
            lose()
            return
        }
        try {
            ear.cancel()
        } catch (_: Exception) {
        }
        ear.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Store(this).locale().toLanguageTag())
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
        VoiceLevel.hush(this)
        try {
            Log.i(TAG, "stt start ${Store(this).locale().toLanguageTag()}")
            Trace.event("stt start ${Store(this).locale().toLanguageTag()}")
            ear.startListening(intent)
        } catch (error: Exception) {
            Log.e(TAG, "stt start failed", error)
            Trace.event("stt start failed ${error.javaClass.simpleName}")
            VoiceLevel.apply(this, Store(this).voiceVolume)
            main.removeCallbacks(again)
            main.postDelayed(again, 800)
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
            if (mode != HEAR) return
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            Log.i(TAG, "stt '${text.take(80)}'")
            val heardWords = Inbox.fold(text).split(" ").count { it.isNotBlank() }
            Trace.event("stt chars=${text.length} words=$heardWords")
            when {
                messageEar -> onMessagePhrase(text)
                followEar && !open -> onCommandPhrase(text)
                catchEar && !open -> onCatchPhrase(text)
                else -> onPhrase(text)
            }
        }
        override fun onError(error: Int) {
            Log.i(TAG, "stt error $error")
            Trace.event("stt error $error")
            if (!alive.get() || mode != HEAR) return
            if (catchEar && android.os.SystemClock.elapsedRealtime() < catchReadyAt) return
            if (catchEar) {
                catchEar = false
                main.removeCallbacks(catchTimeout)
                wake()
                return
            }
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                lose()
                return
            }
            if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                try {
                    recognizer?.destroy()
                } catch (_: Exception) {
                }
                recognizer = null
            }
            main.removeCallbacks(again)
            main.postDelayed(again, 800)
        }
    }

    /**
     * Yes or no only. Spanish recognition often drops the word grok and leaves
     * "sí", so a short yes or no is enough in this window. Anything else keeps
     * the notice. A closed chat stays closed.
     */
    private fun onMessagePhrase(raw: String) {
        if (!alive.get() || !messageEar) return
        val question = questionForGrok(raw)
        val folded = Inbox.fold(if (question.isNullOrBlank()) raw else question)
        if (!agrees(folded) && !declines(folded)) {
            if (open && !question.isNullOrBlank()) {
                messageEar = false
                main.removeCallbacks(answerTimeout)
                onPhrase(raw)
                return
            }
            Log.i(TAG, "stt skip ${Inbox.fold(raw).trim().substringBefore(' ')}")
            Trace.word("stt skip", Inbox.fold(raw))
            listenForAnswer()
            return
        }
        Log.i(TAG, "message choice ${folded.substringBefore(' ')}")
        Trace.word("message choice", folded)
        messageEar = false
        main.removeCallbacks(answerTimeout)
        mode = TALK
        val spoken = spokenMessage(agrees(folded))
        if (spoken.isNullOrBlank()) {
            if (open) hear() else wake()
            return
        }
        EarState.inConversation.value = open
        EarState.reply.value = spoken
        VoiceLevel.apply(this, Store(this).voiceVolume)
        val voice = speaker
        if (voice == null) {
            if (open) hear() else hearFollow()
        } else {
            voice.say(spoken) { if (open) hear() else hearFollow() }
        }
    }

    /** Three minutes after the message. A call does not need hola grok and does not open the chat. */
    private fun hearFollow() {
        if (!alive.get()) return
        followEar = true
        followUntil = android.os.SystemClock.elapsedRealtime() + 180_000L
        main.removeCallbacks(followTimeout)
        main.postDelayed(followTimeout, 180_000L)
        Log.i(TAG, "command ear")
        Trace.event("command ear")
        listenFollow()
    }

    private fun listenFollow() {
        if (!alive.get() || !followEar) return
        if (android.os.SystemClock.elapsedRealtime() >= followUntil) {
            followEar = false
            main.removeCallbacks(followTimeout)
            wake()
            return
        }
        EarState.inConversation.value = open
        startStt(phrase(R.string.status_your_turn))
    }

    private fun onCommandPhrase(raw: String) {
        if (!alive.get() || !followEar) return
        val question = questionForGrok(raw)
        val text = (if (question.isNullOrBlank()) raw else question).trim()
        if (text.isBlank()) {
            listenFollow()
            return
        }
        val addressed = !question.isNullOrBlank()
        if (!addressed && !Commands.handles(text)) {
            Log.i(TAG, "stt skip ${Inbox.fold(raw).trim().substringBefore(' ')}")
            Trace.word("stt skip", Inbox.fold(raw))
            listenFollow()
            return
        }
        Log.i(TAG, "command ${Inbox.fold(text).substringBefore(' ')}")
        Trace.word("command", Inbox.fold(text))
        followEar = false
        main.removeCallbacks(followTimeout)
        mode = TALK
        ask(text)
    }

    /**
     * Someone spoke over the key-phrase ear. That ear does not understand
     * "llamar", so speech recognition takes the rest of the phrase.
     */
    private fun catchSpeech() {
        if (!alive.get() || mode != WAKE || messageEar || followEar || open) return
        catchEar = true
        catchReadyAt = android.os.SystemClock.elapsedRealtime() + 500
        main.removeCallbacks(catchTimeout)
        main.postDelayed(catchTimeout, 8_000L)
        Log.i(TAG, "catch ear")
        Trace.event("catch ear")
        EarState.inConversation.value = false
        startStt(phrase(R.string.status_your_turn))
    }

    private fun onCatchPhrase(raw: String) {
        if (!alive.get() || !catchEar) return
        catchEar = false
        main.removeCallbacks(catchTimeout)
        val foldedAll = Inbox.fold(raw)
        if (foldedAll == "hola grok" || foldedAll == "hey grok" ||
            foldedAll.startsWith("hola grok ") || foldedAll.startsWith("hey grok ")
        ) {
            mode = WAKE
            heardWake()
            return
        }
        val question = questionForGrok(raw)
        val text = (if (question.isNullOrBlank()) raw else question).trim()
        if (Inbox.pending != null) {
            val choice = Inbox.fold(text)
            if (agrees(choice) || declines(choice)) {
                mode = TALK
                val spoken = spokenMessage(agrees(choice))
                if (spoken.isNullOrBlank()) {
                    wake()
                    return
                }
                EarState.inConversation.value = false
                EarState.reply.value = spoken
                VoiceLevel.apply(this, Store(this).voiceVolume)
                val voice = speaker
                if (voice == null) hearFollow() else voice.say(spoken) { hearFollow() }
                return
            }
        }
        if (text.isNotBlank() && (!question.isNullOrBlank() || Commands.handles(text))) {
            Log.i(TAG, "catch ${Inbox.fold(text).substringBefore(' ')}")
            Trace.word("catch", Inbox.fold(text))
            mode = TALK
            ask(text)
            return
        }
        // The order starts while the key-phrase ear is still up, so often only the name remains.
        val heardName = text
        work.execute {
            val spoken = Commands.dialKnown(localized(), Store(this), heardName)
            main.post {
                if (!alive.get()) return@post
                if (spoken.isNullOrBlank()) {
                    Log.i(TAG, "catch skip ${Inbox.fold(heardName).trim().substringBefore(' ')}")
                    Trace.word("catch skip", Inbox.fold(heardName))
                    wake()
                    return@post
                }
                Log.i(TAG, "catch name")
                Trace.event("catch name")
                mode = TALK
                EarState.reply.value = spoken
                VoiceLevel.apply(this, Store(this).voiceVolume)
                val voice = speaker
                if (voice == null) finishCallLine { wake() } else voice.say(spoken) { finishCallLine { wake() } }
            }
        }
    }

    private fun ask(text: String) {
        work.execute {
            val spoken = decide(text)
            main.post {
                if (!alive.get()) return@post
                if (spoken.isNullOrBlank()) {
                    next()
                } else {
                    mode = TALK
                    EarState.reply.value = spoken
                    VoiceLevel.apply(this, Store(this).voiceVolume)
                    val voice = speaker
                    if (voice == null) finishCallLine { next() } else voice.say(spoken) { finishCallLine { next() } }
                }
            }
        }
    }

    private fun onPhrase(raw: String) {
        // No voice print. In a conversation only a phrase that starts with "grok" is for us.
        val question = questionForGrok(raw)
        if (question.isNullOrBlank()) {
            Log.i(TAG, "stt skip ${Inbox.fold(raw).trim().substringBefore(' ')}")
            Trace.word("stt skip", Inbox.fold(raw))
            if (!alive.get()) return
            if (awaitingAnswer()) hear() else wake()
            return
        }
        ask(question)
    }

    private fun decide(text: String): String? {
        val store = Store(localized())
        val ui = localized()
        val folded = Inbox.fold(text)
        if (Inbox.pending != null) {
            if (agrees(folded)) return spokenMessage(true)
            if (declines(folded)) return spokenMessage(false)
            Log.i(TAG, "pending waits")
            Trace.event("pending waits")
        }
        if (Memory.waiting()) {
            if (folded in YES) return Memory.answer(this, ui, true)
            if (folded in NO) return Memory.answer(this, ui, false)
            Memory.drop()
        }
        if (open && folded in GOODBYE && folded.split(" ").size <= 4) {
            open = false
            EarState.inConversation.value = false
            EarState.reply.value = ""
            Memory.drop()
            return ui.getString(R.string.bye)
        }
        val useful = text.trim()
        if (useful.isBlank()) return null
        Memory.handle(this, ui, useful)?.let { return it }
        if (forgetsChat(folded)) {
            grok.clear()
            return ui.getString(R.string.chat_cleared)
        }
        Commands.local(ui, store, useful)?.let { return it }
        if (!open) open = true
        EarState.status.value = ui.getString(R.string.status_thinking)
        return when (val answer = grok.ask(store, useful, languageName(store), Memory.block(this))) {
            is GrokClient.Answer.Text -> Commands.fromCloud(ui, store, answer.value) ?: answer.value
            GrokClient.Answer.NeedKey -> ui.getString(R.string.need_key)
            GrokClient.Answer.Fuel -> ui.getString(R.string.fuel)
            GrokClient.Answer.Failed -> ui.getString(R.string.grok_failed)
        }
    }

    /** The "Calling …" line is only the hand-off. Once it has been said, the screen drops it. */
    private fun finishCallLine(then: () -> Unit) {
        if (Commands.takeCallPlaced()) EarState.reply.value = ""
        then()
    }

    /** Only an open conversation keeps the speech recognizer. A waiting message uses a short window. */
    private fun awaitingAnswer(): Boolean = open

    /** Speaks the waiting notice, or says it will not be read. Null if it is already gone. */
    private fun spokenMessage(yes: Boolean): String? {
        val ui = localized()
        if (!yes) {
            Inbox.dropPending()
            return ui.getString(R.string.no_message)
        }
        val note = Inbox.takePending()
        Log.i(TAG, "message yes chars=${note?.text?.length ?: -1}")
        Trace.event("message yes chars=${note?.text?.length ?: -1}")
        return when {
            note == null -> null
            note.text.isBlank() -> ui.getString(R.string.message_hidden, note.who)
            else -> ui.getString(R.string.message_body, note.who, note.text)
        }
    }

    private fun agrees(folded: String): Boolean = wordChoice(folded, YES)

    private fun declines(folded: String): Boolean = wordChoice(folded, NO)

    private fun wordChoice(folded: String, words: Set<String>): Boolean {
        if (folded in words) return true
        val parts = folded.split(" ").filter { it.isNotBlank() }
        return parts.size in 1..4 && parts.first() in words
    }

    private fun next() {
        if (!alive.get()) return
        if (awaitingAnswer()) {
            if (open) {
                val wait = (Store(this).quietMinutes * 60_000f).toLong()
                main.removeCallbacks(quiet)
                main.postDelayed(quiet, wait)
            } else {
                main.removeCallbacks(quiet)
            }
            hear()
        } else {
            main.removeCallbacks(quiet)
            wake()
        }
    }

    private fun recognizer(): SpeechRecognizer? {
        recognizer?.let { return it }
        recognizer = try {
            SpeechRecognizer.createSpeechRecognizer(this)
        } catch (error: Exception) {
            Log.e(TAG, "stt create failed", error)
            Trace.event("stt create failed ${error.javaClass.simpleName}")
            null
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
        EarState.inConversation.value = false
        EarState.reply.value = ""
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

    /** Screen and spoken lines follow the language chosen in the app. */
    private fun phrase(id: Int, vararg args: Any): String = localized().getString(id, *args)

    private fun languageName(store: Store): String = when (store.language) {
        "es" -> "Spanish"
        "en" -> "English"
        "fr" -> "French"
        "de" -> "German"
        "it" -> "Italian"
        else -> store.locale().getDisplayLanguage(java.util.Locale.ENGLISH)
    }

    /** Drops the spoken lines. Notes and alarms stay. */
    private fun forgetsChat(folded: String): Boolean {
        if (folded in FORGET_CHAT) return true
        return folded.startsWith("olvida la charla") ||
            folded.startsWith("borra la charla") ||
            folded.startsWith("borra la conversacion") ||
            folded.startsWith("empieza de cero") ||
            folded.startsWith("forget the chat") ||
            folded.startsWith("start over")
    }

    /** The words after a leading "grok". Null when the phrase is not addressed to the app. */
    private fun questionForGrok(raw: String): String? {
        val first = Inbox.fold(raw).trim().substringBefore(' ')
        if (first != "grok" && first != "grock") return null
        return raw.trim().split(Regex("\\s+")).drop(1).joinToString(" ")
            .trim()
            .trimStart(':', ',', ';', '-', ' ')
            .trim()
    }

    companion object {
        const val ACTION_STOP = "ch.castellon.oigo.STOP"
        private const val TAG = "GrokEar"
        private const val WAKE = 0
        private const val HEAR = 1
        private const val TALK = 2

        @Volatile
        var live: ListenService? = null

        private val YES = setOf("si", "sí", "yes", "oui", "ja", "sì", "vale")
        private val NO = setOf("no", "non", "nein")
        private val FORGET_CHAT = setOf(
            "olvida", "olvidalo", "olvida la charla", "olvida la conversacion",
            "borra la charla", "borra la conversacion", "empieza de cero", "empieza de nuevo",
            "nuevo tema", "forget", "forget it", "forget the chat", "start over",
            "oublie", "oublie la conversation", "vergiss", "vergiss das gesprach",
            "dimentica", "dimentica la conversazione",
        )
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

        /** Second press of the center button. No spoken warning: the user turned it off. */
        fun stop(context: Context) {
            Store(context).armed = false
            Alerts.cancel(context)
            EarState.listening.value = false
            EarState.status.value = ""
            EarState.inConversation.value = false
            EarState.reply.value = ""
            if (live != null) {
                context.startService(
                    Intent(context, ListenService::class.java).setAction(ACTION_STOP),
                )
            }
        }
    }
}
