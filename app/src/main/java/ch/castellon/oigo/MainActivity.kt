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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
        setContent { GrokTheme { Home(hasKey, ::listen, ::openSettings) } }
    }

    override fun onResume() {
        super.onResume()
        hasKey = Store(this).apiKey.isNotBlank()
        if (!hasKey && EarState.listening.value) ListenService.stop(this)
    }

    private fun listen() {
        if (!hasKey) return
        if (EarState.listening.value) {
            ListenService.stop(this)
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

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}

@Composable
private fun Home(hasKey: Boolean, onListen: () -> Unit, onSettings: () -> Unit) {
    val listening by EarState.listening.collectAsState()
    val status by EarState.status.collectAsState()
    val inConversation by EarState.inConversation.collectAsState()
    val reply by EarState.reply.collectAsState()
    val idle = stringResource(R.string.status_idle)
    val ink = Color(0xFFE7F0EA)
    val paper = Color(0xFF101418)
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
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!hasKey) {
                Text(
                    text = stringResource(R.string.need_key_banner),
                    color = ink,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 24.dp),
                )
            }
            Button(
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
            if (hasKey) {
                Text(
                    text = status.ifBlank { idle },
                    color = ink,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 28.dp),
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
    }
}
