package dev.castellon.grok

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.Locale

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
        get() = plain.getString(MODEL, "grok-4.7") ?: "grok-4.7"
        set(value) = plain.edit().putString(MODEL, value.ifBlank { "grok-4.7" }).apply()

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

    fun keywordLine(): String = if (wake == WAKE_HEY) HEY_LINE else HOLA_LINE

    fun wakeWords(): List<String> = if (wake == WAKE_HEY) {
        listOf("hey grok", "ok grok", "okay grok")
    } else {
        listOf("hola grok")
    }

    companion object {
        const val WAKE_HOLA = "hola"
        const val WAKE_HEY = "hey"

        // Phones of the zh-en keyword model. "Hola grok" is written by hand in that set.
        // It is not a recording of the person.
        private const val HOLA_LINE = "HH OW1 L AA1 G R OW1 K @HOLA_GROK"
        private const val HEY_LINE = "HH EY1 G R OW1 K @HEY_GROK"

        private const val ARMED = "armed"
        private const val WAKE = "wake"
        private const val LANGUAGE = "language"
        private const val REPEATS = "repeats"
        private const val MINUTES = "minutes"
        private const val QUIET = "quiet"
        private const val CALLS = "calls"
        private const val WHATSAPP = "whatsapp"
        private const val TELEGRAM = "telegram"
        private const val MODEL = "model"
        private const val KEY = "api_key"
        private const val ALERT_INDEX = "alert_index"

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
