package ch.castellon.oigo

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.Locale
import java.util.UUID

/** Local choices. API key and OAuth tokens stay in the keystore when the device allows it. */
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

    /** The word said instead of grok. Stored as typed. Matching uses [spokenName]. */
    var name: String
        get() = plain.getString(NAME, "grok") ?: "grok"
        set(value) = plain.edit().putString(NAME, value.take(32)).apply()

    var debug: Boolean
        get() = plain.getBoolean(DEBUG, false)
        set(value) = plain.edit().putBoolean(DEBUG, value).apply()

    fun spokenName(): String = Phoneme.nameOf(name)

    fun displayName(): String = name.trim().ifBlank { "Grok" }

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

    /** Answer to the xAI disclosure: null until asked. Nothing goes to xAI unless it is true. */
    var xaiConsent: Boolean?
        get() = if (plain.contains(XAI_CONSENT)) plain.getBoolean(XAI_CONSENT, false) else null
        set(value) = plain.edit().putBoolean(XAI_CONSENT, value == true).apply()

    var model: String
        get() = plain.getString(MODEL, "grok-4.3") ?: "grok-4.3"
        set(value) = plain.edit().putString(MODEL, value.ifBlank { "grok-4.3" }).apply()

    /** "api_key" or "oauth". */
    var authMode: String
        get() = plain.getString(AUTH_MODE, AUTH_API) ?: AUTH_API
        set(value) = plain.edit().putString(AUTH_MODE, if (value == AUTH_OAUTH) AUTH_OAUTH else AUTH_API).apply()

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

    var oauthAccess: String
        get() = secret.getString(OAUTH_ACCESS, "") ?: ""
        set(value) = secret.edit().putString(OAUTH_ACCESS, value).apply()

    var oauthRefresh: String
        get() = secret.getString(OAUTH_REFRESH, "") ?: ""
        set(value) = secret.edit().putString(OAUTH_REFRESH, value).apply()

    var oauthExpiresAt: Long
        get() = secret.getLong(OAUTH_EXPIRES, 0L)
        set(value) = secret.edit().putLong(OAUTH_EXPIRES, value).apply()

    /** Writes the three token fields together, before the process can be killed. */
    fun saveOauth(access: String, refresh: String, expiresAt: Long) {
        secret.edit()
            .putString(OAUTH_ACCESS, access)
            .putString(OAUTH_REFRESH, refresh)
            .putLong(OAUTH_EXPIRES, expiresAt)
            .commit()
    }

    var pendingDevice: String
        get() = plain.getString(PENDING_DEVICE, "") ?: ""
        set(value) {
            plain.edit().putString(PENDING_DEVICE, value).commit()
        }

    var pendingInterval: Int
        get() = plain.getInt(PENDING_INTERVAL, 5).coerceAtLeast(1)
        set(value) {
            plain.edit().putInt(PENDING_INTERVAL, value.coerceAtLeast(1)).commit()
        }

    var pendingUntil: Long
        get() = plain.getLong(PENDING_UNTIL, 0L)
        set(value) {
            plain.edit().putLong(PENDING_UNTIL, value).commit()
        }

    var pendingUrl: String
        get() = plain.getString(PENDING_URL, "") ?: ""
        set(value) {
            plain.edit().putString(PENDING_URL, value).commit()
        }

    fun clearPending() {
        plain.edit()
            .remove(PENDING_DEVICE)
            .remove(PENDING_INTERVAL)
            .remove(PENDING_UNTIL)
            .remove(PENDING_URL)
            .commit()
    }

    /** Returns a usable bearer token, refreshing OAuth if needed. Empty if none. */
    fun bearer(): String {
        if (authMode == AUTH_OAUTH) {
            val access = oauthAccess
            if (access.isBlank()) return ""
            if (System.currentTimeMillis() < oauthExpiresAt - 60_000) return access
            val refresh = oauthRefresh
            if (refresh.isBlank() || xaiConsent != true) return access
            val fresh = XaiOAuth.refresh(refresh)
            if (fresh != null) {
                oauthAccess = fresh.accessToken
                oauthRefresh = fresh.refreshToken
                oauthExpiresAt = System.currentTimeMillis() + fresh.expiresIn * 1000L
                return fresh.accessToken
            }
            return access
        }
        return apiKey
    }

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

    /** Phonemes of hola/hey plus the chosen name. Not a recording. */
    fun wakeLines(): String = Phoneme.wakeLines(wake == WAKE_HEY, name)

    /** Yes and no after the name. Loaded only while a notice is waiting. */
    fun answerLines(): String = Phoneme.answerLines(name)

    /** Wake words plus yes and no. Yes and no are only for a waiting message. */
    fun keywordsText(): String = wakeLines() + answerLines()

    fun wakeWords(): List<String> {
        val who = spokenName()
        return if (wake == WAKE_HEY) {
            listOf("hey $who", "ok $who", "okay $who")
        } else {
            listOf("hola $who")
        }
    }

    companion object {
        const val WAKE_HOLA = "hola"
        const val WAKE_HEY = "hey"
        const val AUTH_API = "api_key"
        const val AUTH_OAUTH = "oauth"

        private const val ARMED = "armed"
        private const val WAKE = "wake"
        private const val NAME = "name"
        private const val DEBUG = "debug"
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
        private const val XAI_CONSENT = "xai_consent"
        private const val AUTH_MODE = "auth_mode"
        private const val KEY = "api_key"
        private const val OAUTH_ACCESS = "oauth_access"
        private const val OAUTH_REFRESH = "oauth_refresh"
        private const val OAUTH_EXPIRES = "oauth_expires"
        private const val PENDING_DEVICE = "oauth_pending_device"
        private const val PENDING_INTERVAL = "oauth_pending_interval"
        private const val PENDING_UNTIL = "oauth_pending_until"
        private const val PENDING_URL = "oauth_pending_url"
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
