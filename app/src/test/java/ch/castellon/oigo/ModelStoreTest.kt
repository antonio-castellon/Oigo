package ch.castellon.oigo

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random

class ModelStoreTest {
    @get:Rule
    val temp = TemporaryFolder()

    /** Same layout as the real archive: one folder, extra files before and between the needed ones. */
    private fun pack(into: File): File {
        val dir = "sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20/"
        val random = Random(1)
        val entries = listOf(
            dir + "en.phone" to random.nextBytes(300_000),
            dir + ModelStore.DECODER to random.nextBytes(760_000),
            dir + ModelStore.ENCODER to random.nextBytes(10_500_000),
            dir + "test_wavs/en_0.wav" to random.nextBytes(200_000),
            dir + ModelStore.JOINER to random.nextBytes(340_000),
            dir + "tokens.txt" to ByteArray(2_000) { 'a'.code.toByte() },
        )
        val file = File(into, "model.tar.bz2")
        TarArchiveOutputStream(BZip2CompressorOutputStream(file.outputStream().buffered())).use { tar ->
            for ((name, bytes) in entries) {
                val entry = TarArchiveEntry(name)
                entry.size = bytes.size.toLong()
                tar.putArchiveEntry(entry)
                tar.write(bytes)
                tar.closeArchiveEntry()
            }
        }
        return file
    }

    @Test
    fun fullPackInstallsWithProgressUpTo100() {
        val root = temp.newFolder("kws")
        val pack = pack(root)
        val seen = mutableListOf<Int>()
        assertTrue(ModelStore.unpack(pack, root) { seen += it })
        assertTrue(ModelStore.complete(root))
        assertFalse(File(root, "en.phone").exists())
        assertFalse(File(root, "incoming").exists())
        assertEquals(100, seen.last())
        assertEquals(seen.sorted(), seen)
    }

    @Test
    fun cutPackIsDeletedAndNeverReady() {
        val root = temp.newFolder("kws")
        val pack = pack(root)
        val half = pack.readBytes().copyOf((pack.length() / 2).toInt())
        pack.writeBytes(half)
        assertFalse(ModelStore.unpack(pack, root))
        assertFalse(pack.exists())
        assertFalse(ModelStore.complete(root))
        assertFalse(File(root, ModelStore.ENCODER).exists())
    }

    @Test
    fun cutDownloadIsNotAPack() {
        assertTrue(ModelStore.fullyRead(32_885_699, 32_885_699))
        assertTrue(ModelStore.fullyRead(1_000, -1))
        assertFalse(ModelStore.fullyRead(32_000_000, 32_885_699))
        assertFalse(ModelStore.fullyRead(0, -1))
    }

    @Test
    fun installPercentStopsAt99UntilFilesAreInPlace() {
        assertEquals(0, ModelStore.installPercent(0, 100))
        assertEquals(50, ModelStore.installPercent(50, 100))
        assertEquals(99, ModelStore.installPercent(100, 100))
        assertEquals(0, ModelStore.installPercent(10, 0))
    }
}