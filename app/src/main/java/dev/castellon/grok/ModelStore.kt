package dev.castellon.grok

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
        val encoder = find(root, ENCODER)
        if (encoder == null) {
            download(root, onProgress)
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

    private fun download(root: File, onProgress: (Int) -> Unit) {
        val pack = File(root, "model.tar.bz2")
        val connection = URL(URL_MODEL).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 20000
        connection.readTimeout = 120000
        connection.inputStream.use { input ->
            val total = connection.contentLengthLong
            FileOutputStream(pack).use { output ->
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
        BZip2CompressorInputStream(pack.inputStream()).use { bz ->
            TarArchiveInputStream(bz).use { tar ->
                while (true) {
                    val entry = tar.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val name = File(entry.name).name
                    if (name != ENCODER && name != DECODER && name != JOINER && name != "tokens.txt") continue
                    val out = File(root, name)
                    FileOutputStream(out).use { tar.copyTo(it) }
                }
            }
        }
        pack.delete()
    }
}
