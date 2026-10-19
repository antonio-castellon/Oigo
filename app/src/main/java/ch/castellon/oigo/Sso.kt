package ch.castellon.oigo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Device-code sign-in that stays inside the app, the same way Grok Usage keeps
 * the account session in its own WebView. Chrome's cookies are invisible here.
 * A real cookie on grok.com or accounts.x.ai means the person is already in,
 * and the poll keeps going until xAI returns the token the ear can use.
 */
object Sso {
    private const val TAG = "XaiOAuth"
    private val generation = AtomicInteger(0)
    private val running = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private val sessionUrls = arrayOf(
        "https://grok.com",
        "https://grok.com/dashboard",
        "https://accounts.x.ai",
        "https://accounts.spacex.ai",
    )

    /** True once a bearer token is stored. */
    val signedIn = MutableStateFlow(false)

    /** Page of the sign-in that is still open. Empty when there is nothing to show. */
    val page = MutableStateFlow("")

    /** True when the WebView already holds a grok or xAI session cookie. */
    val browserKnown = MutableStateFlow(false)

    /** True when the last device-code request failed before a page existed. */
    val failed = MutableStateFlow(false)

    fun refresh(context: Context) {
        val store = Store(context.applicationContext)
        signedIn.value = store.bearer().isNotBlank()
        browserKnown.value = cookiesLookSignedIn()
        if (page.value.isBlank()) page.value = store.pendingUrl
    }

    fun request(context: Context, onFail: () -> Unit, onCode: (XaiOAuth.DeviceCode) -> Unit) {
        val app = context.applicationContext
        Thread {
            val code = XaiOAuth.requestDeviceCode()
            if (code == null) {
                failed.value = true
                main.post(onFail)
                return@Thread
            }
            failed.value = false
            begin(app, code)
            main.post { onCode(code) }
        }.start()
    }

    fun begin(context: Context, code: XaiOAuth.DeviceCode) {
        val app = context.applicationContext
        val store = Store(app)
        val url = code.verificationUriComplete ?: code.verificationUri
        store.pendingDevice = code.deviceCode
        store.pendingInterval = code.interval
        store.pendingUntil = System.currentTimeMillis() + code.expiresIn.coerceAtLeast(60) * 1000L
        store.pendingUrl = url
        page.value = url
        generation.incrementAndGet()
        kick(app)
    }

    /** Continues a sign-in that was still waiting when the screen went away. */
    fun resume(context: Context) {
        val app = context.applicationContext
        val store = Store(app)
        browserKnown.value = cookiesLookSignedIn()
        if (store.bearer().isNotBlank()) {
            store.clearPending()
            page.value = ""
            signedIn.value = true
            return
        }
        if (store.pendingUrl.isNotBlank()) page.value = store.pendingUrl
        if (store.pendingDevice.isBlank()) return
        if (generation.get() == 0) generation.incrementAndGet()
        kick(app)
    }

    /**
     * Same moment Grok Usage calls onSessionConfirmed: the account home is open,
     * or the cookie jar already holds a real session cookie.
     */
    fun notePage(url: String?) {
        if (isXaiAccountHome(url) || cookiesLookSignedIn()) browserKnown.value = true
    }

    /** Same rule as Grok Usage: a Cloudflare-only cookie is not a session. */
    fun cookiesLookSignedIn(): Boolean {
        val jar = runCatching { CookieManager.getInstance() }.getOrNull() ?: return false
        return sessionUrls.any { url ->
            val header = runCatching { jar.getCookie(url) }.getOrNull()
            cookieLooksSignedIn(header)
        }
    }

    private fun kick(app: Context) {
        if (!running.compareAndSet(false, true)) return
        val gen = generation.get()
        Thread {
            try {
                val store = Store(app)
                val code = store.pendingDevice
                val left = ((store.pendingUntil - System.currentTimeMillis()) / 1000L).toInt()
                if (code.isBlank() || left <= 0) {
                    if (gen == generation.get()) {
                        store.clearPending()
                        page.value = ""
                        if (store.bearer().isBlank()) failed.value = true
                    }
                    return@Thread
                }
                Log.i(TAG, "sso poll for ${left}s")
                val tokens = try {
                    XaiOAuth.pollForTokens(code, store.pendingInterval, left)
                } catch (e: Exception) {
                    Log.e(TAG, "sso poll failed", e)
                    null
                }
                if (gen != generation.get()) return@Thread
                if (tokens == null) {
                    Log.w(TAG, "sso no token")
                    store.clearPending()
                    page.value = ""
                    failed.value = true
                    return@Thread
                }
                store.saveOauth(
                    tokens.accessToken,
                    tokens.refreshToken,
                    System.currentTimeMillis() + tokens.expiresIn.coerceAtLeast(60) * 1000L,
                )
                store.authMode = Store.AUTH_OAUTH
                store.clearPending()
                page.value = ""
                failed.value = false
                signedIn.value = store.bearer().isNotBlank()
                Log.i(TAG, "sso saved ${signedIn.value}")
            } finally {
                running.set(false)
                if (gen != generation.get() && Store(app).pendingDevice.isNotBlank()) kick(app)
            }
        }.start()
    }
}

internal fun cookieLooksSignedIn(header: String?): Boolean {
    if (header.isNullOrBlank()) return false
    return header.split(';').any { part ->
        val name = part.substringBefore('=').trim().lowercase()
        name.isNotEmpty() && name != "__cf_bm" && name != "_cfuvid" && !name.startsWith("__cf")
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SsoWeb(url: String, modifier: Modifier = Modifier) {
    val opened = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    if (url.isBlank()) return
    AndroidView(
        modifier = modifier.fillMaxWidth().height(520.dp),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.parseColor("#101418"))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(true)
                val cookies = CookieManager.getInstance()
                cookies.setAcceptCookie(true)
                cookies.setAcceptThirdPartyCookies(this, true)
                webChromeClient = SsoChrome()
                webViewClient = ssoClient(reloadInto = null)
                loadUrl(url)
            }
        },
        update = { view ->
            if (opened.value != url) {
                opened.value = url
                if (view.url != url) view.loadUrl(url)
            }
        },
    )
}

/** Login popups stay in this app's cookie jar, as they do in Grok Usage. */
private class SsoChrome : WebChromeClient() {
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message,
    ): Boolean {
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val child = WebView(view.context)
        child.settings.javaScriptEnabled = true
        child.settings.domStorageEnabled = true
        child.settings.setSupportMultipleWindows(true)
        child.settings.javaScriptCanOpenWindowsAutomatically = true
        CookieManager.getInstance().setAcceptThirdPartyCookies(child, true)
        child.webViewClient = ssoClient(reloadInto = view)
        child.webChromeClient = this
        (view.parent as? ViewGroup)?.addView(
            child,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        transport.webView = child
        resultMsg.sendToTarget()
        return true
    }
}

private fun ssoClient(reloadInto: WebView?): WebViewClient = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val next = request.url?.toString().orEmpty()
        Sso.notePage(next)
        val scheme = request.url?.scheme?.lowercase().orEmpty()
        if (scheme != "http" && scheme != "https") return true
        val host = request.url?.host?.lowercase().orEmpty()
        if (host.isNotEmpty() && !ssoHostAllowed(host)) return true
        if (reloadInto != null && request.isForMainFrame && (isXaiAccountHome(next) || isGrokReturn(next))) {
            reloadInto.loadUrl(next)
            view.post {
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
            }
            return true
        }
        return false
    }

    override fun onPageFinished(view: WebView?, finished: String?) {
        CookieManager.getInstance().flush()
        Sso.notePage(finished)
    }
}

/** Account home, not the sign-in or callback page. Same check as Grok Usage. */
internal fun isXaiAccountHome(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val host = url.substringAfter("://", "")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .lowercase()
    if (host != "accounts.x.ai" && host != "accounts.spacex.ai") return false
    val path = url.substringAfter("://", "")
        .substringAfter('/', "")
        .substringBefore('?')
        .substringBefore('#')
        .lowercase()
    if (path.contains("sign-in") || path.contains("signin") || path.contains("login") ||
        path.contains("sign-up") || path.contains("signup") || path.contains("callback") ||
        path.contains("oauth")
    ) {
        return false
    }
    return true
}

internal fun isGrokReturn(url: String): Boolean {
    val host = url.substringAfter("://", "")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .lowercase()
    if (host != "grok.com" && !host.endsWith(".grok.com")) return false
    val path = "/" + url.substringAfter("://", "").substringAfter('/', "").substringBefore('?').substringBefore('#')
    return path == "/" || path.startsWith("/dashboard") || path.startsWith("/chat")
}

internal fun ssoHostAllowed(host: String): Boolean {
    val normalized = host.lowercase()
    return ssoHosts.any { suffix -> normalized == suffix || normalized.endsWith(".$suffix") }
}

private val ssoHosts = listOf(
    "x.ai",
    "spacex.ai",
    "spacex.com",
    "grok.com",
    "x.com",
    "twitter.com",
    "google.com",
    "googleapis.com",
    "gstatic.com",
    "googleusercontent.com",
    "withgoogle.com",
    "recaptcha.net",
    "apple.com",
    "icloud.com",
    "workos.com",
    "cursor.com",
    "cursor.sh",
)
