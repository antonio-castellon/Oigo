package ch.castellon.oigo

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Local SuperGrok / X Premium+ OAuth via device code (RFC 8628).
 * Uses the public client_id shared by Grok CLI and community tools.
 * Tokens stay only on the device. Unofficial path; may return 403 on some accounts.
 */
object XaiOAuth {
    private const val TAG = "XaiOAuth"
    const val CLIENT_ID = "b1a00492-073a-47ea-816f-4c329264a828"
    private const val SCOPE = "openid profile email offline_access grok-cli:access api:access"
    private const val DEVICE_CODE_URL = "https://auth.x.ai/oauth2/device/code"
    private const val TOKEN_URL = "https://auth.x.ai/oauth2/token"
    private const val GRANT_DEVICE = "urn:ietf:params:oauth:grant-type:device_code"

    data class DeviceCode(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val verificationUriComplete: String?,
        val expiresIn: Int,
        val interval: Int,
    )

    data class Tokens(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Int,
    )

    fun requestDeviceCode(): DeviceCode? {
        try {
            val body = "client_id=${enc(CLIENT_ID)}&scope=${enc(SCOPE)}&referrer=grok-build"
            val (code, raw) = post(DEVICE_CODE_URL, body)
            if (code !in 200..299) {
                Log.w(TAG, "device code $code ${raw.take(200)}")
                return null
            }
            val json = JSONObject(raw)
            return DeviceCode(
                deviceCode = json.getString("device_code"),
                userCode = json.getString("user_code"),
                verificationUri = json.getString("verification_uri"),
                verificationUriComplete = json.optString("verification_uri_complete").takeIf { it.isNotBlank() },
                expiresIn = json.optInt("expires_in", 1800),
                interval = json.optInt("interval", 5),
            )
        } catch (e: Exception) {
            Log.e(TAG, "device code failed", e)
            return null
        }
    }

    fun pollForTokens(deviceCode: String, intervalSec: Int, timeoutSec: Int): Tokens? {
        val deadline = System.currentTimeMillis() + timeoutSec * 1000L
        var wait = intervalSec.coerceAtLeast(1)
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(wait * 1000L)
            val body = "grant_type=${enc(GRANT_DEVICE)}&device_code=${enc(deviceCode)}&client_id=${enc(CLIENT_ID)}"
            val (code, raw) = post(TOKEN_URL, body)
            if (code in 200..299) {
                val json = JSONObject(raw)
                return Tokens(
                    accessToken = json.getString("access_token"),
                    refreshToken = json.optString("refresh_token"),
                    expiresIn = json.optInt("expires_in", 3600),
                )
            }
            val err = try { JSONObject(raw).optString("error") } catch (_: Exception) { "" }
            when (err) {
                "authorization_pending" -> continue
                "slow_down" -> wait += 5
                else -> {
                    Log.w(TAG, "poll $code $err")
                    return null
                }
            }
        }
        return null
    }

    fun refresh(refreshToken: String): Tokens? {
        try {
            val body = "grant_type=refresh_token&refresh_token=${enc(refreshToken)}&client_id=${enc(CLIENT_ID)}"
            val (code, raw) = post(TOKEN_URL, body)
            if (code !in 200..299) {
                Log.w(TAG, "refresh $code ${raw.take(200)}")
                return null
            }
            val json = JSONObject(raw)
            return Tokens(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token").ifBlank { refreshToken },
                expiresIn = json.optInt("expires_in", 3600),
            )
        } catch (e: Exception) {
            Log.e(TAG, "refresh failed", e)
            return null
        }
    }

    private fun post(url: String, form: String): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("x-grok-client-surface", "ui")
            conn.outputStream.use { it.write(form.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            code to raw
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
