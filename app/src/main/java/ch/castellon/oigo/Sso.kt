package ch.castellon.oigo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
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

    /** The 8-character device code. The sign-in page fills it in from the address. */
    val userCode = MutableStateFlow("")

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
        userCode.value = code.userCode.ifBlank { codeIn(url) }
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
            hidePage()
            signedIn.value = true
            return
        }
        if (store.pendingUrl.isNotBlank()) {
            page.value = store.pendingUrl
            if (userCode.value.isBlank()) userCode.value = codeIn(store.pendingUrl)
        }
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
                        hidePage()
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
                    hidePage()
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
                hidePage()
                failed.value = false
                signedIn.value = store.bearer().isNotBlank()
                Log.i(TAG, "sso saved ${signedIn.value}")
            } finally {
                running.set(false)
                if (gen != generation.get() && Store(app).pendingDevice.isNotBlank()) kick(app)
            }
        }.start()
    }

    private fun hidePage() {
        page.value = ""
        userCode.value = ""
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
        modifier = modifier,
        factory = { context -> SsoHost(context) },
        update = { host ->
            if (opened.value != url) {
                opened.value = url
                host.page.tag = url
                loadWhenLaidOut(host.page, url)
            }
        },
    )
}

/**
 * One full-screen frame. X login opens a second window; Compose would leave
 * that window at zero size, so the popup lives here and fills the frame.
 */
@SuppressLint("SetJavaScriptEnabled")
private class SsoHost(context: Context) : FrameLayout(context) {
    val page: WebView = WebView(context)
    private var popup: WebView? = null

    init {
        setBackgroundColor(AndroidColor.WHITE)
        prepare(page)
        page.webViewClient = ssoClient(reloadInto = null, host = this)
        page.webChromeClient = SsoChrome(this)
        addView(page, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun showPopup(child: WebView) {
        popup?.let { closePopup(it) }
        popup = child
        addView(child, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        child.bringToFront()
    }

    fun closePopup(child: WebView) {
        child.post {
            removeView(child)
            if (popup === child) popup = null
            child.destroy()
        }
    }
}

private class SsoChrome(private val host: SsoHost) : WebChromeClient() {
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message,
    ): Boolean {
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val child = WebView(view.context)
        child.setBackgroundColor(AndroidColor.WHITE)
        prepare(child)
        child.settings.userAgentString = view.settings.userAgentString
        child.webViewClient = ssoClient(reloadInto = host.page, host = host)
        child.webChromeClient = this
        host.showPopup(child)
        transport.webView = child
        resultMsg.sendToTarget()
        return true
    }

    override fun onCloseWindow(window: WebView) {
        if (window !== host.page) host.closePopup(window)
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun prepare(web: WebView) {
    web.setBackgroundColor(AndroidColor.WHITE)
    web.settings.javaScriptEnabled = true
    web.settings.domStorageEnabled = true
    web.settings.loadsImagesAutomatically = true
    web.settings.useWideViewPort = true
    web.settings.loadWithOverviewMode = false
    web.settings.builtInZoomControls = false
    web.settings.displayZoomControls = false
    web.settings.setSupportZoom(false)
    web.settings.userAgentString = mobileUserAgent(web.settings.userAgentString)
    web.settings.javaScriptCanOpenWindowsAutomatically = true
    web.settings.setSupportMultipleWindows(true)
    web.settings.mediaPlaybackRequiresUserGesture = true
    web.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    web.settings.cacheMode = WebSettings.LOAD_DEFAULT
    hideAppIdentity(web.settings)
    val cookies = CookieManager.getInstance()
    cookies.setAcceptCookie(true)
    cookies.setAcceptThirdPartyCookies(web, true)
    web.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )
}

private fun ssoClient(reloadInto: WebView?, host: SsoHost): WebViewClient = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val next = request.url?.toString().orEmpty()
        Sso.notePage(next)
        if (reloadInto != null && request.isForMainFrame && (isXaiAccountHome(next) || isGrokReturn(next))) {
            reloadInto.loadUrl(next)
            host.closePopup(view)
            return true
        }
        val uri = request.url ?: return true
        return consumeSsoNavigation(view, uri)
    }

    override fun onPageFinished(view: WebView?, finished: String?) {
        CookieManager.getInstance().flush()
        Sso.notePage(finished)
    }
}

private fun loadWhenLaidOut(web: WebView, url: String, attempt: Int = 0) {
    web.post {
        if (web.tag != url) return@post
        if ((web.height <= 0 || web.parent == null) && attempt < 40) {
            loadWhenLaidOut(web, url, attempt + 1)
            return@post
        }
        if (web.url != url) web.loadUrl(url)
    }
}

/** Same mobile identity Grok Usage sends, so X draws a phone form. */
private fun mobileUserAgent(raw: String?): String {
    val base = raw?.takeIf { it.isNotBlank() }
        ?: "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    val chrome = Regex("Chrome/[\\d.]+").find(base)?.value ?: "Chrome/131.0.0.0"
    return "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) $chrome Mobile Safari/537.36"
}

private fun hideAppIdentity(settings: WebSettings) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
    }
}

private fun consumeSsoNavigation(view: WebView, uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase() ?: return true
    if (scheme == "intent") {
        val parsed = runCatching { Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME) }.getOrNull()
        val fallback = parsed?.getStringExtra("browser_fallback_url")
        val data = parsed?.data
        val target = when {
            data?.scheme == "https" && data.host?.let { ssoHostAllowed(it) } == true -> data
            !fallback.isNullOrBlank() -> Uri.parse(fallback)
            else -> null
        }
        if (target != null && (target.scheme == "https" || target.scheme == "http") && ssoHostAllowed(target.host.orEmpty())) {
            view.loadUrl(target.toString())
        }
        return true
    }
    if (scheme != "http" && scheme != "https") return true
    val host = uri.host ?: return true
    return !ssoHostAllowed(host)
}

internal fun codeIn(url: String): String {
    val query = url.substringAfter('?', missingDelimiterValue = "").substringBefore('#')
    for (part in query.split('&')) {
        if (part.substringBefore('=') == "user_code") {
            return runCatching {
                java.net.URLDecoder.decode(part.substringAfter('='), "UTF-8")
            }.getOrDefault("")
        }
    }
    return ""
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
)
