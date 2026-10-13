package ch.castellon.oigo

/**
 * Turns the typed name into tokens of the zh-en wake model.
 * This is a written pronunciation, not a recording of the person.
 * Every token here exists in that model's tokens.txt. An unknown token
 * makes the spotter refuse the whole file.
 */
object Phoneme {
    fun nameOf(raw: String): String {
        val words = Inbox.fold(raw).split(" ").filter { it.isNotBlank() }.take(3)
        return words.joinToString(" ").take(32).ifBlank { "grok" }
    }

    /** Two lines the settings screen can show. */
    fun spell(raw: String): String {
        val name = nameOf(raw)
        return phones(name).joinToString(" ") + "\n" + letters(name).joinToString(" ")
    }

    fun wakeLines(hey: Boolean, raw: String): String {
        val name = nameOf(raw)
        // These exact lines answered in 1.0.35. A shorter "grok" alone would wake on the TV.
        if (name == "grok") return if (hey) CLASSIC_HEY else CLASSIC_HOLA
        val said = phones(name)
        val written = letters(name)
        val greet = if (hey) {
            listOf(listOf("HH", "EY1"), listOf("EY1"))
        } else {
            listOf(listOf("OW1", "L", "AA1"), listOf("OW1", "L", "AH0"), listOf("AA1", "L", "AA1"))
        }
        val greetLetters = if (hey) listOf(listOf("h", "e", "i")) else listOf(listOf("o", "l", "a"), listOf("ou", "l", "a"))
        val lines = linkedSetOf<String>()
        lines += keyword(said, "2.5", "0.10")
        lines += keyword(written, "2.0", "0.10")
        for (prefix in greet) lines += keyword(prefix + said, "2.0", "0.12")
        for (prefix in greetLetters) lines += keyword(prefix + written, "2.0", "0.15")
        return lines.filter { it.isNotBlank() }.joinToString("")
    }

    fun answerLines(raw: String): String {
        val name = nameOf(raw)
        if (name == "grok") return CLASSIC_ANSWER
        val said = phones(name)
        val written = letters(name)
        val lines = linkedSetOf<String>()
        if (said.isNotEmpty()) {
            lines += keyword(said + listOf("S", "IY1"), "2.2", "0.14", "GROK_YES")
            lines += keyword(said + listOf("N", "OW1"), "2.2", "0.14", "GROK_NO")
        }
        if (written.isNotEmpty()) {
            lines += keyword(written + listOf("s", "i"), "2.0", "0.16", "GROK_YES")
            lines += keyword(written + listOf("n", "o"), "2.0", "0.16", "GROK_NO")
        }
        return lines.filter { it.isNotBlank() }.joinToString("")
    }

    private fun keyword(tokens: List<String>, score: String, gate: String, tag: String = "WAKE"): String {
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") + " :$score #$gate @$tag\n"
    }

    /** Spanish-leaning phones. A silent h is dropped. c before e or i is S. */
    private fun phones(name: String): List<String> {
        val out = ArrayList<String>()
        val text = name.lowercase()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == ' ') {
                i++
                continue
            }
            val next = text.getOrNull(i + 1)
            when {
                c == 'c' && next == 'h' -> { out += "CH"; i += 2 }
                c == 'l' && next == 'l' -> { out += "Y"; i += 2 }
                c == 'r' && next == 'r' -> { out += "R"; i += 2 }
                c == 'q' && next == 'u' -> { out += "K"; i += 2 }
                c == 'c' && (next == 'e' || next == 'i') -> { out += "S"; i++ }
                c == 'g' && (next == 'e' || next == 'i') -> { out += "HH"; i++ }
                c == 'h' -> i++
                c == 'x' -> { out += "K"; out += "S"; i++ }
                else -> {
                    ONE[c]?.let { out += it }
                    i++
                }
            }
        }
        return out
    }

    /** One model token per letter. These tokens are the pinyin letters of the same model. */
    private fun letters(name: String): List<String> {
        val out = ArrayList<String>()
        for (c in name.lowercase()) {
            if (c == ' ') continue
            LETTER[c]?.let { out += it }
        }
        return out
    }

    private val ONE = mapOf(
        'a' to "AA1",
        'e' to "EH1",
        'i' to "IY1",
        'o' to "OW1",
        'u' to "UW1",
        'b' to "B",
        'c' to "K",
        'd' to "D",
        'f' to "F",
        'g' to "G",
        'j' to "HH",
        'k' to "K",
        'l' to "L",
        'm' to "M",
        'n' to "N",
        'p' to "P",
        'q' to "K",
        'r' to "R",
        's' to "S",
        't' to "T",
        'v' to "B",
        'w' to "W",
        'y' to "Y",
        'z' to "S",
    )
    private val LETTER = mapOf(
        'a' to "a",
        'b' to "b",
        'c' to "c",
        'd' to "d",
        'e' to "e",
        'f' to "f",
        'g' to "g",
        'h' to "h",
        'i' to "i",
        'j' to "j",
        'k' to "k",
        'l' to "l",
        'm' to "m",
        'n' to "n",
        'o' to "o",
        'p' to "p",
        'q' to "k",
        'r' to "r",
        's' to "s",
        't' to "t",
        'u' to "u",
        'v' to "b",
        'w' to "w",
        'x' to "x",
        'y' to "y",
        'z' to "z",
    )

    /** The lines that answered in 1.0.35. */
    private const val CLASSIC_HOLA =
        "OW1 L AA1 G R OW1 K :2.5 #0.10 @HOLA_GROK\n" +
            "OW1 L AA1 G R AA1 K :2.5 #0.10 @HOLA_GROK\n" +
            "OW1 L AA1 G R AO1 K :2.0 #0.12 @HOLA_GROK\n" +
            "OW1 L AH0 G R OW1 K :2.0 #0.12 @HOLA_GROK\n" +
            "HH OW1 L AA1 G R OW1 K :2.0 #0.15 @HOLA_GROK\n" +
            "HH OW1 L AA1 G R AA1 K :2.0 #0.15 @HOLA_GROK\n" +
            "o l a g r o k :2.0 #0.18 @HOLA_GROK\n" +
            "ou l a g r ou k :2.0 #0.18 @HOLA_GROK\n"
    private const val CLASSIC_HEY =
        "HH EY1 G R OW1 K :2.0 #0.12 @HEY_GROK\n" +
            "HH EY1 G R AA1 K :2.0 #0.12 @HEY_GROK\n" +
            "EY1 G R OW1 K :2.0 #0.12 @HEY_GROK\n"
    private const val CLASSIC_ANSWER =
        "G R OW1 K S IY1 :2.2 #0.14 @GROK_YES\n" +
            "G R AA1 K S IY1 :2.2 #0.14 @GROK_YES\n" +
            "G R OW1 K S IH1 :2.0 #0.16 @GROK_YES\n" +
            "g r o k s i :2.0 #0.16 @GROK_YES\n" +
            "G R OW1 K Y EH1 S :2.0 #0.16 @GROK_YES\n" +
            "G R OW1 K W IY1 :2.0 #0.16 @GROK_YES\n" +
            "G R OW1 K Y AA1 :2.0 #0.16 @GROK_YES\n" +
            "G R OW1 K N OW1 :2.2 #0.14 @GROK_NO\n" +
            "G R AA1 K N OW1 :2.2 #0.14 @GROK_NO\n" +
            "g r o k n o :2.0 #0.16 @GROK_NO\n" +
            "G R OW1 K N AO1 N :2.0 #0.16 @GROK_NO\n" +
            "G R OW1 K N AY1 N :2.0 #0.16 @GROK_NO\n"
}
