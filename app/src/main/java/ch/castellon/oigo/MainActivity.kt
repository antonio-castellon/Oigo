package ch.castellon.oigo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

open class LocalizedActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Store(newBase).localized(newBase))
    }
}

/** One button. Settings sit below the status bar so a shade swipe misses them. */
class MainActivity : LocalizedActivity() {
    private var hasKey by mutableStateOf(false)
    private var called by mutableStateOf("Grok")
    private var authMode by mutableStateOf(Store.AUTH_API)
    private var ssoStatus by mutableStateOf("")
    private var consent by mutableStateOf<Boolean?>(null)
    private var disclosure by mutableStateOf(false)
    private var afterConsent: (() -> Unit)? = null

    private val askMic = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (!hasKey) return@registerForActivityResult
        if (granted[Manifest.permission.RECORD_AUDIO] == true) {
            ListenService.start(this)
        } else {
            EarState.status.value = getString(R.string.status_need_mic)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val liveSignIn by Sso.signedIn.collectAsState()
            LaunchedEffect(liveSignIn) {
                if (liveSignIn) hasKey = true
            }
            GrokTheme {
                Home(hasKey || liveSignIn, called, authMode, ssoStatus, consent, ::listen, ::openSettings, ::startSso) { askConsent {} }
                if (disclosure) XaiDisclosure(::answerConsent, ::closeConsent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val store = Store(this)
        Sso.refresh(this)
        Sso.resume(this)
        hasKey = store.bearer().isNotBlank()
        called = store.displayName()
        authMode = store.authMode
        consent = store.xaiConsent
        if (consent == null) disclosure = true
        EarState.debug.value = Store(this).debug
        if (!hasKey && EarState.listening.value) ListenService.stop(this)
    }

    private fun listen() {
        if (Sso.signedIn.value) hasKey = true
        if (!hasKey) return
        if (EarState.listening.value) {
            ListenService.stop(this)
            return
        }
        if (consent != true) {
            askConsent(::listen)
            return
        }
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) needed += Manifest.permission.POST_NOTIFICATIONS
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty() || missing == listOf(Manifest.permission.POST_NOTIFICATIONS)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                ListenService.start(this)
                return
            }
        }
        askMic.launch(missing.toTypedArray())
    }

    /** Shows the disclosure. [then] runs only if the person accepts. */
    private fun askConsent(then: () -> Unit) {
        afterConsent = then
        disclosure = true
    }

    private fun answerConsent(accepted: Boolean) {
        Consent.answer(this, accepted)
        consent = accepted
        val then = afterConsent
        closeConsent()
        if (accepted) then?.invoke()
    }

    private fun closeConsent() {
        disclosure = false
        afterConsent = null
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun startSso() {
        if (authMode != Store.AUTH_OAUTH) return
        if (consent != true) {
            askConsent(::startSso)
            return
        }
        ssoStatus = getString(R.string.sso_status_waiting)
        Sso.request(
            this,
            onFail = { ssoStatus = getString(R.string.sso_status_fail) },
            onCode = { code ->
                ssoStatus = getString(R.string.sso_code_prompt, code.verificationUri, code.userCode)
            },
        )
    }
}

@Composable
private fun Home(
    hasKey: Boolean,
    called: String,
    authMode: String,
    ssoStatus: String,
    consent: Boolean?,
    onListen: () -> Unit,
    onSettings: () -> Unit,
    onStartSso: () -> Unit,
    onReviewConsent: () -> Unit,
) {
    val listening by EarState.listening.collectAsState()
    val status by EarState.status.collectAsState()
    val inConversation by EarState.inConversation.collectAsState()
    val reply by EarState.reply.collectAsState()
    val mic by EarState.mic.collectAsState()
    val debugOn by EarState.debug.collectAsState()
    val lines by EarState.lines.collectAsState()
    val modelStep by ModelStore.step.collectAsState()
    val keyBanner by EarState.keyBanner.collectAsState()
    val idle = stringResource(R.string.status_idle, called)
    val ink = Color(0xFFE7F0EA)
    val paper = Color(0xFF101418)
    val signInPage by Sso.page.collectAsState()
    val userCode by Sso.userCode.collectAsState()
    val browserKnown by Sso.browserKnown.collectAsState()
    val signInFailed by Sso.failed.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(paper),
    ) {
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 32.dp)
                .size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = ink,
                modifier = Modifier.size(22.dp),
            )
        }
        if (!hasKey && signInPage.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (userCode.isNotBlank()) {
                    Text(
                        text = userCode,
                        color = ink,
                        fontSize = 48.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 8.dp),
                    )
                }
                SsoWeb(
                    signInPage,
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
        if (hasKey || signInPage.isBlank()) Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = if (debugOn && lines.isNotEmpty()) 132.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (consent == false) {
                Text(
                    text = stringResource(R.string.xai_consent_declined),
                    color = ink,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp),
                )
                TextButton(onClick = onReviewConsent, modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(stringResource(R.string.xai_consent_review))
                }
            }
            if (hasKey && keyBanner != 0) {
                Text(
                    text = stringResource(keyBanner),
                    color = Color(0xFFE8833A),
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 12.dp),
                )
            }
            if (!hasKey && signInPage.isBlank()) {
                Text(
                    text = stringResource(
                        if (authMode == Store.AUTH_OAUTH && browserKnown) R.string.sso_status_waiting
                        else if (authMode == Store.AUTH_OAUTH) R.string.need_sso_banner
                        else R.string.need_key_banner
                    ),
                    color = ink,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 12.dp),
                )
                if (authMode == Store.AUTH_OAUTH) {
                    Button(
                        onClick = onStartSso,
                        modifier = Modifier.padding(bottom = 16.dp),
                    ) {
                        Text(stringResource(R.string.sso_sign_in))
                    }
                    if (ssoStatus.isNotBlank()) {
                        Text(
                            text = ssoStatus,
                            color = ink,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                        )
                    }
                }
                if (signInFailed && ssoStatus.isBlank()) {
                    Text(
                        text = stringResource(R.string.sso_status_fail),
                        color = ink,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                    )
                }
            }
            if (hasKey || signInPage.isBlank()) Button(
                onClick = onListen,
                enabled = hasKey,
                modifier = Modifier.size(240.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (listening) Color(0xFF1B8F45) else Color(0xFFC62828),
                    contentColor = Color(0xFFF4FFF7),
                    disabledContainerColor = Color(0xFF2C3532),
                    disabledContentColor = Color(0xFF8E9B94),
                ),
            ) {
                Text(
                    text = stringResource(if (listening) R.string.listen_off else R.string.listen_button),
                    fontSize = if (listening) 26.sp else 32.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                    maxLines = 2,
                )
            }
            if (hasKey && listening) {
                val loud = (mic / 0.03f).coerceIn(0f, 1f)
                val (micWords, micColor) = when {
                    mic < 0.003f -> stringResource(R.string.mic_quiet) to Color(0xFF8E9B94)
                    mic < 0.008f -> stringResource(R.string.mic_low) to Color(0xFFE6A817)
                    else -> stringResource(R.string.mic_hot) to Color(0xFF3DDC84)
                }
                Text(
                    text = micWords,
                    color = micColor,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 22.dp),
                )
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .width(240.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFF2C3532)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(loud)
                            .height(18.dp)
                            .background(micColor),
                    )
                }
            }
            val step = modelStep
            if (hasKey) {
                Text(
                    text = if (step != null) {
                        stringResource(
                            if (step.installing) R.string.status_installing else R.string.status_downloading,
                            step.percent,
                        )
                    } else {
                        status.ifBlank { idle }
                    },
                    color = ink,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 28.dp),
                )
            }
            if (hasKey && step != null) {
                LinearProgressIndicator(
                    progress = { step.percent / 100f },
                    color = Color(0xFF3DDC84),
                    trackColor = Color(0xFF2C3532),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(240.dp)
                        .height(8.dp),
                )
            }
            if (hasKey && inConversation) {
                Text(
                    text = stringResource(R.string.in_conversation),
                    color = Color(0xFF3DDC84),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp),
                )
            }
            if (hasKey && reply.isNotBlank()) {
                Text(
                    text = reply,
                    color = ink,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp),
                )
            }
        }
        if (debugOn && lines.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .fillMaxWidth()
                    .heightIn(max = 124.dp)
                    .clipToBounds(),
            ) {
                // Newest on top, always in view. Long phrases wrap under their own text, not under the bullet.
                lines.forEach { line ->
                    val color = if (line.sent) Color(0xFF1B8F45) else Color(0xFFC62828)
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                        Text(text = "•", color = color, fontSize = 13.sp, lineHeight = 16.sp)
                        Text(
                            text = line.text,
                            color = color,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f).padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
