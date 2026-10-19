package ch.castellon.oigo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechPiecesTest {
    @Test
    fun splitsOnCommaPeriodAndParagraph() {
        val pieces = speechPieces("El rey salió al jardín. Allí encontró un pájaro, que cantaba.\n\nLuego volvió.")
        assertEquals(
            listOf(
                "El rey salió al jardín.",
                "Allí encontró un pájaro,",
                "que cantaba.",
                "Luego volvió.",
            ),
            pieces,
        )
    }

    @Test
    fun keepsDigitsAndEllipsisTogether() {
        assertEquals(listOf("Son las 3.14,", "más o menos."), speechPieces("Son las 3.14, más o menos."))
        assertEquals(listOf("Cuesta 1,5 euros."), speechPieces("Cuesta 1,5 euros."))
        assertEquals(listOf("Espera..."), speechPieces("Espera..."))
        assertEquals(listOf("¿Qué tal?"), speechPieces("¿Qué tal?"))
    }

    @Test
    fun longClauseBreaksOnWords() {
        val words = (1..40).joinToString(" ") { "palabra" }
        val pieces = speechPieces(words)
        assertTrue(pieces.size > 1)
        pieces.forEach { piece ->
            assertFalse(piece.endsWith("palabr"))
            assertTrue(piece.split(' ').all { it == "palabra" })
        }
        assertEquals(words, pieces.joinToString(" "))
    }
}
