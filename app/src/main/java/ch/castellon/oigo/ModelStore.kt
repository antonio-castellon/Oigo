package ch.castellon.oigo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * The phoneme model is downloaded once. Nothing is recorded from the person.
 * The wake line is a prepared phoneme string, not a voice print.
 *
 * Download and install run in one job owned by the app, not by the listening service.
 * Stopping or starting the ear only waits on that job; it never cancels or restarts it.
 */
object ModelStore {
    private const val URL_MODEL =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/kws-models/sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20.tar.bz2"
    internal const val ENCODER = "encoder-epoch-13-avg-2-chunk-16-left-64.onnx"
    internal const val DECODER = "decoder-epoch-13-avg-2-chunk-16-left-64.onnx"
    internal const val JOINER = "joiner-epoch-13-avg-2-chunk-16-left-64.onnx"
    private val NEEDED = listOf(ENCODER, DECODER, JOINER, "tokens.txt")

    /** What the model job is doing, for the screen and the notification. */
    data class Step(val installing: Boolean, val percent: Int)

    /** Null while no download or install is running. */
    val step = MutableStateFlow<Step?>(null)

    private val worker = Executors.newSingleThreadExecutor { Thread(it, "oigo-model") }
    private var job: Future<*>? = null

    data class Ready(val dir: File, val encoder: File, val decoder: File, val joiner: File, val tokens: File)

    private fun root(context: Context) = File(context.filesDir, "kws")

    /** Starts the model job, or joins the one already running. Null when the model is already there. */
    @Synchronized
    fun fetch(context: Context): Future<*>? {
        val root = root(context.applicationContext)
        job?.let { if (!it.isDone) return it }
        if (complete(root)) return null
        return worker.submit { install(root) }.also { job = it }
    }

    /** The files the ear needs. Call only after [fetch] returned null or its job finished. */
    fun ready(context: Context, keywords: String): Ready {
        val root = root(context)
        if (!complete(root)) error("modelo incompleto")
        val found = Ready(
            dir = root,
            encoder = find(root, ENCODER) ?: error("falta el encoder"),
            decoder = find(root, DECODER) ?: error("falta el decoder"),
            joiner = find(root, JOINER) ?: error("falta el joiner"),
            tokens = find(root, "tokens.txt") ?: error("faltan los tokens"),
        )
        File(found.dir, "keywords.txt").writeText(keywords.trim() + "\n", Charsets.UTF_8)
        return found
    }

    /** A downloaded pack is installed, never fetched again. Only a missing or broken pack is downloaded. */
    private fun install(root: File) {
        try {
            root.mkdirs()
            val pack = File(root, "model.tar.bz2")
            val onInstall = { percent: Int -> step.value = Step(installing = true, percent = percent) }
            if (pack.isFile) {
                onInstall(0)
                unpack(pack, root, onInstall)
            }
            if (!complete(root)) {
                step.value = Step(installing = false, percent = 0)
                download(pack) { step.value = Step(installing = false, percent = it) }
                onInstall(0)
                if (!unpack(pack, root, onInstall)) error("modelo incompleto")
            }
            pack.delete()
        } finally {
            step.value = null
        }
    }

    private fun find(root: File, name: String): File? {
        val direct = File(root, name)
        if (direct.isFile) return direct
        return root.walkTopDown().firstOrNull { it.isFile && it.name == name }
    }

    /** True only when every piece is present. A lone encoder is a download that was cut off. */
    internal fun complete(dir: File): Boolean {
        return File(dir, ENCODER).length() > 10_000_000L &&
            File(dir, DECODER).length() > 500_000L &&
            File(dir, JOINER).length() > 200_000L &&
            File(dir, "tokens.txt").length() > 200L
    }

    /** A body shorter than the announced length is a cut download, never a pack. */
    internal fun fullyRead(read: Long, total: Long): Boolean = read > 0 && (total <= 0 || read == total)

    /** Install progress from the compressed bytes read. 100 is kept for the moment the files are in place. */
    internal fun installPercent(seen: Long, size: Long): Int =
        if (size <= 0) 0 else ((seen * 100) / size).toInt().coerceIn(0, 99)

    private fun download(pack: File, onProgress: (Int) -> Unit) {
        val part = File(pack.parentFile, "model.tar.bz2.part")
        part.delete()
        pack.delete()
        val connection = URL(URL_MODEL).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 20000
        connection.readTimeout = 120000
        connection.setRequestProperty("Accept-Encoding", "identity")
        var read = 0L
        var total = -1L
        try {
            connection.inputStream.use { input ->
                total = connection.contentLengthLong
                FileOutputStream(part).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) onProgress(((read * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        if (!fullyRead(read, total)) {
            part.delete()
            error("descarga cortada $read/$total")
        }
        if (!part.renameTo(pack)) part.copyTo(pack, overwrite = true).also { part.delete() }
    }

    /**
     * Unpacks the four needed files. Skipped entries are still read, or the next file is lost.
     * The pack is read through a buffer: bzip2 pulls single bytes, and 33 MB of single reads
     * from a bare file stream kept the screen at 100% for minutes.
     * A broken pack is deleted, so the next start downloads it again instead of looping.
     */
    internal fun unpack(pack: File, root: File, onPercent: (Int) -> Unit = {}): Boolean {
        if (!pack.isFile || pack.length() < 1_000_000L) return false
        val incoming = File(root, "incoming")
        incoming.deleteRecursively()
        incoming.mkdirs()
        try {
            val counted = Counted(pack.inputStream(), pack.length(), onPercent)
            BZip2CompressorInputStream(counted.buffered(64 * 1024)).use { bz ->
                TarArchiveInputStream(bz).use { tar ->
                    while (true) {
                        val entry = tar.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val name = File(entry.name).name
                        if (name !in NEEDED) {
                            discard(tar)
                            continue
                        }
                        FileOutputStream(File(incoming, name)).use { tar.copyTo(it, 64 * 1024) }
                    }
                }
            }
            if (!complete(incoming)) {
                pack.delete()
                return false
            }
            for (name in NEEDED) {
                val from = File(incoming, name)
                val to = File(root, name)
                if (to.exists()) to.delete()
                if (!from.renameTo(to)) from.copyTo(to, overwrite = true)
            }
            val done = complete(root)
            if (done) onPercent(100)
            return done
        } catch (_: Exception) {
            pack.delete()
            return false
        } finally {
            incoming.deleteRecursively()
        }
    }

    private fun discard(tar: TarArchiveInputStream) {
        val buffer = ByteArray(64 * 1024)
        while (tar.read(buffer) >= 0) {
        }
    }

    /** Counts the compressed bytes going into bzip2 and reports each new percent once. */
    private class Counted(input: InputStream, private val size: Long, private val onPercent: (Int) -> Unit) :
        FilterInputStream(input) {
        private var seen = 0L
        private var shown = -1

        override fun read(): Int = super.read().also { if (it >= 0) step(1) }

        override fun read(b: ByteArray, off: Int, len: Int): Int =
            super.read(b, off, len).also { if (it > 0) step(it.toLong()) }

        private fun step(n: Long) {
            seen += n
            val percent = installPercent(seen, size)
            if (percent != shown) {
                shown = percent
                onPercent(percent)
            }
        }
    }
}