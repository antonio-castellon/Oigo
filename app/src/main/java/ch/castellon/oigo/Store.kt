package ch.castellon.oigo

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.Locale
import java.util.UUID

/** Local choices. The API key stays in the keystore when the device allows it. */
class Store(context: Context) {
    private val app = context.applicationContext
    private val plain: SharedPreferences =
        app.getSharedPreferences("grok", Context.MODE_PRIVATE)
    private val secret: SharedPreferences = openSecret(app) ?: plain

    var armed: Boolean
        get() = plain.getBoolean(ARMED, false)
        set(value) = plain.edit().putBoolean(ARMED, value).apply()

    var wake: String
        get() = plain.getString(WAKE, WAKE_HOLA) ?: WAKE_HOLA
        set(value) = plain.edit().putString(WAKE, value).apply()

    var language: String
        get() = plain.getString(LANGUAGE, "system") ?: "system"
        set(value) = plain.edit().putString(LANGUAGE, value).apply()

    var alertRepeats: Int
        get() = plain.getInt(REPEATS, 3).coerceIn(1, 10)
        set(value) = plain.edit().putInt(REPEATS, value.coerceIn(1, 10)).apply()

    var alertMinutes: Int
        get() = plain.getInt(MINUTES, 5).coerceIn(1, 60)
        set(value) = plain.edit().putInt(MINUTES, value.coerceIn(1, 60)).apply()

    var quietMinutes: Float
        get() = plain.getFloat(QUIET, 1f).coerceIn(0.2f, 30f)
        set(value) = plain.edit().putFloat(QUIET, value.coerceIn(0.2f, 30f)).apply()

    /**
     * Hours the compacted daytime chat stays on the phone.
     * A new key, so an old saved minute count is not read as hours.
     * Notes and alarms are separate.
     */
    var contextHours: Int
        get() = plain.getInt(CONTEXT_HOURS, 12).coerceIn(1, 24)
        set(value) = plain.edit().putInt(CONTEXT_HOURS, value.coerceIn(1, 24)).apply()

    /** Percent of the media volume used when the app speaks. */
    var voiceVolume: Int
        get() = plain.getInt(VOICE, 80).coerceIn(0, 100)
        set(value) = plain.edit().putInt(VOICE, value.coerceIn(0, 100)).apply()

    var calls: Boolean
        get() = plain.getBoolean(CALLS, false)
        set(value) = plain.edit().putBoolean(CALLS, value).apply()

    var whatsapp: Boolean
        get() = plain.getBoolean(WHATSAPP, false)
        set(value) = plain.edit().putBoolean(WHATSAPP, value).apply()

    var telegram: Boolean
        get() = plain.getBoolean(TELEGRAM, false)
        set(value) = plain.edit().putBoolean(TELEGRAM, value).apply()

    var model: String
        get() = plain.getString(MODEL, "grok-4.3") ?: "grok-4.3"
        set(value) = plain.edit().putString(MODEL, value.ifBlank { "grok-4.3" }).apply()

    /** Stable id for a trace. It is not a phone number and not an account. */
    fun installId(): String {
        val saved = plain.getString(INSTALL, "").orEmpty()
        if (saved.isNotBlank()) return saved
        val created = UUID.randomUUID().toString()
        plain.edit().putString(INSTALL, created).apply()
        return created
    }

    var apiKey: String
        get() = secret.getString(KEY, "") ?: ""
        set(value) = secret.edit().putString(KEY, value.trim()).apply()

    var alertIndex: Int
        get() = plain.getInt(ALERT_INDEX, 0)
        set(value) = plain.edit().putInt(ALERT_INDEX, value).apply()

    fun locale(): Locale {
        val tag = when (language) {
            "es" -> "es"
            "en" -> "en"
            "fr" -> "fr"
            "de" -> "de"
            "it" -> "it"
            else -> return Locale.getDefault()
        }
        return Locale.forLanguageTag(tag)
    }

    fun localized(base: Context): Context {
        if (language == "system") return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale())
        return base.createConfigurationContext(config)
    }

    /** Several pronunciations, one tag. Spanish “hola” has a silent h. */
    fun keywordsText(): String = (if (wake == WAKE_HEY) HEY_LINES else HOLA_LINES) + ANSWER_LINES

    fun wakeWords(): List<String> = if (wake == WAKE_HEY) {
        listOf("hey grok", "ok grok", "okay grok")
    } else {
        listOf("hola grok")
    }

    companion object {
        const val WAKE_HOLA = "hola"
        const val WAKE_HEY = "hey"

        // Phones and pinyin of the zh-en model. Not a recording of the person.
        // :score raises the path. #threshold is how sure the model must be.
        // A line that starts with HH misses Spanish “hola”, because that h is silent.
        private const val HOLA_LINES =
            "OW1 L AA1 G R OW1 K :2.5 #0.10 @HOLA_GROK\n" +
                "OW1 L AA1 G R AA1 K :2.5 #0.10 @HOLA_GROK\n" +
                "OW1 L AA1 G R AO1 K :2.0 #0.12 @HOLA_GROK\n" +
                "OW1 L AH0 G R OW1 K :2.0 #0.12 @HOLA_GROK\n" +
                "HH OW1 L AA1 G R OW1 K :2.0 #0.15 @HOLA_GROK\n" +
                "HH OW1 L AA1 G R AA1 K :2.0 #0.15 @HOLA_GROK\n" +
                "o l a g r o k :2.0 #0.18 @HOLA_GROK\n" +
                "ou l a g r ou k :2.0 #0.18 @HOLA_GROK\n"
        private const val HEY_LINES =
            "HH EY1 G R OW1 K :2.0 #0.12 @HEY_GROK\n" +
                "HH EY1 G R AA1 K :2.0 #0.12 @HEY_GROK\n" +
                "EY1 G R OW1 K :2.0 #0.12 @HEY_GROK\n"
        private const val ANSWER_LINES =
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

        private const val ARMED = "armed"
        private const val WAKE = "wake"
        private const val LANGUAGE = "language"
        private const val REPEATS = "repeats"
        private const val MINUTES = "minutes"
        private const val QUIET = "quiet"
        private const val CONTEXT_HOURS = "context_hours"
        private const val VOICE = "voice_volume"
        private const val CALLS = "calls"
        private const val WHATSAPP = "whatsapp"
        private const val TELEGRAM = "telegram"
        private const val MODEL = "model"
        private const val KEY = "api_key"
        private const val ALERT_INDEX = "alert_index"
        private const val INSTALL = "install_id"

        private fun openSecret(context: Context): SharedPreferences? = try {
            val alias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "grok.keys",
                alias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            null
        }
    }
}
