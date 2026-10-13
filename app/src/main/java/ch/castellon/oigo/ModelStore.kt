package ch.castellon.oigo

import android.content.Context
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * The phoneme model is downloaded once. Nothing is recorded from the person.
 * The wake line is a prepared phoneme string, not a voice print.
 */
object ModelStore {
    private const val URL_MODEL =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/kws-models/sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20.tar.bz2"
    private const val ENCODER = "encoder-epoch-13-avg-2-chunk-16-left-64.onnx"
    private const val DECODER = "decoder-epoch-13-avg-2-chunk-16-left-64.onnx"
    private const val JOINER = "joiner-epoch-13-avg-2-chunk-16-left-64.onnx"

    data class Ready(val dir: File, val encoder: File, val decoder: File, val joiner: File, val tokens: File)

    fun ready(context: Context, keywords: String, onProgress: (Int) -> Unit): Ready {
        val root = File(context.filesDir, "kws")
        root.mkdirs()
        if (!complete(root)) {
            val pack = File(root, "model.tar.bz2")
            if (!unpack(pack, root)) {
                download(pack, onProgress)
                if (!unpack(pack, root)) error("modelo incompleto")
            }
            pack.delete()
        }
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

    private fun find(root: File, name: String): File? {
        val direct = File(root, name)
        if (direct.isFile) return direct
        return root.walkTopDown().firstOrNull { it.isFile && it.name == name }
    }

    /** True only when every piece is present. A lone encoder is a download that was cut off. */
    private fun complete(dir: File): Boolean {
        return File(dir, ENCODER).length() > 10_000_000L &&
            File(dir, DECODER).length() > 500_000L &&
            File(dir, JOINER).length() > 200_000L &&
            File(dir, "tokens.txt").length() > 200L
    }

    private fun download(pack: File, onProgress: (Int) -> Unit) {
        val part = File(pack.parentFile, "model.tar.bz2.part")
        part.delete()
        val connection = URL(URL_MODEL).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 20000
        connection.readTimeout = 120000
        connection.inputStream.use { input ->
            val total = connection.contentLengthLong
            FileOutputStream(part).use { output ->
                val buffer = ByteArray(64 * 1024)
                var read = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    output.write(buffer, 0, n)
                    read += n
                    if (total > 0) onProgress(((read * 100) / total).toInt().coerceIn(0, 100))
                }
            }
        }
        connection.disconnect()
        if (pack.exists()) pack.delete()
        if (!part.renameTo(pack)) part.copyTo(pack, overwrite = true).also { part.delete() }
    }

    /** Unpacks the four needed files. Skipped entries are still read, or the next file is lost. */
    private fun unpack(pack: File, root: File): Boolean {
        if (!pack.isFile || pack.length() < 1_000_000L) return false
        val incoming = File(root, "incoming")
        incoming.deleteRecursively()
        incoming.mkdirs()
        try {
            BZip2CompressorInputStream(pack.inputStream()).use { bz ->
                TarArchiveInputStream(bz).use { tar ->
                    while (true) {
                        val entry = tar.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val name = File(entry.name).name
                        val wanted = name == ENCODER || name == DECODER || name == JOINER || name == "tokens.txt"
                        if (!wanted) {
                            discard(tar)
                            continue
                        }
                        FileOutputStream(File(incoming, name)).use { tar.copyTo(it) }
                    }
                }
            }
            if (!complete(incoming)) return false
            for (name in listOf(ENCODER, DECODER, JOINER, "tokens.txt")) {
                val from = File(incoming, name)
                val to = File(root, name)
                if (to.exists()) to.delete()
                if (!from.renameTo(to)) from.copyTo(to, overwrite = true)
            }
            return complete(root)
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
}
