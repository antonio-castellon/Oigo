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
        val pieces = piecesOf(line)
        if (pieces.isEmpty()) {
            done()
            return
        }
        val mine = ++turn
        var spokenLast = pieces.lastIndex
        var finished = false
        var started = false
        fun finish() {
            if (finished || turn != mine) return
            finished = true
            pendingDone?.let { main.removeCallbacks(it) }
            pendingDone = null
            if (Looper.myLooper() == Looper.getMainLooper()) done() else main.post(done)
        }
        // Only if the engine never begins. Once it starts, onDone of the last piece is the end.
        val startGuard = Runnable {
            if (!started && turn == mine) {
                Log.i(TAG, "tts never started")
                Trace.event("tts never started")
                finish()
            }
        }
        pendingDone = startGuard
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (!utteranceId.orEmpty().startsWith("grok-$mine")) return
                started = true
                pendingDone?.let { main.removeCallbacks(it) }
                pendingDone = null
                Log.i(TAG, "tts start")
                Trace.event("tts start")
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == "grok-$mine-$spokenLast") main.post { finish() }
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
        var queued = 0
        for (index in pieces.indices) {
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val code = engine?.speak(pieces[index], mode, null, "grok-$mine-$index") ?: TextToSpeech.ERROR
            Log.i(TAG, "tts speak code=$code chunk=$index chars=${pieces[index].length}")
            Trace.event("tts speak code=$code chars=${pieces[index].length}")
            if (code == TextToSpeech.ERROR) {
                if (queued == 0) {
                    finish()
                    return
                }
                spokenLast = index - 1
                break
            }
            queued++
            spokenLast = index
        }
        main.postDelayed(startGuard, 8_000L)
    }

    /** One clause at a time, in order. The voice ends when the last piece reports onDone. */
    private fun piecesOf(text: String): List<String> = speechPieces(text)

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

/**
 * Clauses for the voice. A comma, a stop, or a new line ends a piece, so the
 * engine is not handed two sentences, and a word is not cut in half.
 * A comma or a dot between digits stays, as in 1,5 or 3.14.
 */
internal fun speechPieces(text: String): List<String> {
    val source = text.replace("\r\n", "\n").replace('\r', '\n').trim()
    if (source.isEmpty()) return emptyList()
    val out = ArrayList<String>()
    val buf = StringBuilder()
    var i = 0
    while (i < source.length) {
        val c = source[i]
        if (c == '\n') {
            emitClause(out, buf)
            i++
            continue
        }
        buf.append(c)
        if (clauseBreak(source, i)) {
            while (i + 1 < source.length && source[i + 1] in CLOSING) {
                i++
                buf.append(source[i])
            }
            emitClause(out, buf)
        }
        i++
    }
    emitClause(out, buf)
    return out.flatMap { fitClause(it) }
}

private fun emitClause(out: MutableList<String>, buf: StringBuilder) {
    val piece = buf.toString().trim()
    buf.clear()
    if (piece.isNotEmpty()) out += piece
}

private fun clauseBreak(text: String, i: Int): Boolean {
    val c = text[i]
    val prev = if (i > 0) text[i - 1] else ' '
    val next = if (i + 1 < text.length) text[i + 1] else ' '
    if (c == ',' || c == ';') return !(prev.isDigit() && next.isDigit())
    if (c == '!' || c == '?' || c == '…') return true
    if (c != '.') return false
    if (next == '.') return false
    if (prev.isDigit() && next.isDigit()) return false
    return true
}

/** A clause with no punctuation still goes out whole words, never mid-word. */
private fun fitClause(piece: String): List<String> {
    if (piece.length <= CLAUSE_LIMIT) return listOf(piece)
    val out = ArrayList<String>()
    var rest = piece
    while (rest.length > CLAUSE_LIMIT) {
        val window = rest.substring(0, CLAUSE_LIMIT)
        val space = window.lastIndexOf(' ')
        if (space < 1) {
            val next = rest.indexOf(' ')
            if (next < 0) {
                out += rest
                return out
            }
            out += rest.substring(0, next)
            rest = rest.substring(next + 1).trim()
            continue
        }
        out += rest.substring(0, space).trim()
        rest = rest.substring(space + 1).trim()
    }
    if (rest.isNotEmpty()) out += rest
    return out
}

private const val CLAUSE_LIMIT = 160
private const val CLOSING = "\"'»”’)"

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
