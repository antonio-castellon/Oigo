package ch.castellon.oigo

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActivity : LocalizedActivity() {
    private lateinit var store: Store
    private var callsOn by mutableStateOf(false)
    private var whatsappOn by mutableStateOf(false)
    private var telegramOn by mutableStateOf(false)
    private var traceNote by mutableStateOf("")

    private val callPerm = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val ok = granted[Manifest.permission.CALL_PHONE] == true &&
            granted[Manifest.permission.READ_CONTACTS] == true
        store.calls = ok
        callsOn = ok
    }

    private val contactPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        store.whatsapp = ok
        whatsappOn = ok
        if (ok && !listenerOn()) openListener()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized && (store.telegram || store.whatsapp)) MessageListener.rescan(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        Trace.attach(this)
        callsOn = store.calls
        whatsappOn = store.whatsapp
        telegramOn = store.telegram
        enableEdgeToEdge()
        setContent { GrokTheme { Page() } }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun Page() {
        val ink = Color(0xFFE7F0EA)
        val paper = Color(0xFF101418)
        var wake by remember { mutableStateOf(store.wake) }
        var repeats by remember { mutableIntStateOf(store.alertRepeats) }
        var minutes by remember { mutableIntStateOf(store.alertMinutes) }
        var quiet by remember { mutableFloatStateOf(store.quietMinutes) }
        var contextHours by remember { mutableIntStateOf(store.contextHours) }
        var volume by remember { mutableIntStateOf(store.voiceVolume) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(paper)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings_title), color = ink, fontSize = 28.sp)
            Text(stringResource(R.string.no_recording), color = ink, fontSize = 16.sp)
            Text(stringResource(R.string.phoneme_note), color = ink, fontSize = 16.sp)
            Text(stringResource(R.string.language), color = ink, fontSize = 18.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LangChip("system", stringResource(R.string.lang_system))
                LangChip("es", "ES")
                LangChip("en", "EN")
                LangChip("fr", "FR")
                LangChip("de", "DE")
                LangChip("it", "IT")
            }
            Text(stringResource(R.string.wake_phrase), color = ink, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = wake == Store.WAKE_HOLA,
                    onClick = {
                        wake = Store.WAKE_HOLA
                        store.wake = wake
                    },
                    label = { Text(stringResource(R.string.wake_hola)) },
                )
                FilterChip(
                    selected = wake == Store.WAKE_HEY,
                    onClick = {
                        wake = Store.WAKE_HEY
                        store.wake = wake
                    },
                    label = { Text(stringResource(R.string.wake_hey)) },
                )
            }
            var key by mutableStateOf(store.apiKey)
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it
                    store.apiKey = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.api_key)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Text(stringResource(R.string.api_key_help), color = ink, fontSize = 15.sp)
            var model by mutableStateOf(store.model)
            OutlinedTextField(
                value = model,
                onValueChange = {
                    model = it
                    store.model = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.model)) },
                singleLine = true,
            )
            Stepper(stringResource(R.string.voice_volume), "$volume%", {
                store.voiceVolume = volume - 10
                volume = store.voiceVolume
                VoiceLevel.apply(this@SettingsActivity, volume)
            }, {
                store.voiceVolume = volume + 10
                volume = store.voiceVolume
                VoiceLevel.apply(this@SettingsActivity, volume)
            })
            Text(stringResource(R.string.alerts_title), color = ink, fontSize = 18.sp)
            Stepper(stringResource(R.string.alert_repeats), repeats.toString(), {
                store.alertRepeats = repeats - 1
                repeats = store.alertRepeats
            }, {
                store.alertRepeats = repeats + 1
                repeats = store.alertRepeats
            })
            Stepper(stringResource(R.string.alert_minutes), minutes.toString(), {
                store.alertMinutes = minutes - 1
                minutes = store.alertMinutes
            }, {
                store.alertMinutes = minutes + 1
                minutes = store.alertMinutes
            })
            Stepper(
                stringResource(R.string.quiet_minutes),
                String.format("%.1f", quiet),
                {
                    store.quietMinutes = quiet - 0.5f
                    quiet = store.quietMinutes
                },
                {
                    store.quietMinutes = quiet + 0.5f
                    quiet = store.quietMinutes
                },
            )
            Stepper(stringResource(R.string.context_hours), contextHours.toString(), {
                store.contextHours = contextHours - 1
                contextHours = store.contextHours
            }, {
                store.contextHours = contextHours + 1
                contextHours = store.contextHours
            })
            Text(stringResource(R.string.context_help), color = ink, fontSize = 15.sp)
            Text(stringResource(R.string.extras_title), color = ink, fontSize = 18.sp)
            Text(stringResource(R.string.extras_help), color = ink, fontSize = 15.sp)
            Toggle(stringResource(R.string.calls), callsOn) { on ->
                if (!on) {
                    store.calls = false
                    callsOn = false
                } else {
                    callPerm.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS))
                }
            }
            Toggle(stringResource(R.string.whatsapp), whatsappOn) { on ->
                if (!on) {
                    store.whatsapp = false
                    whatsappOn = false
                } else {
                    contactPerm.launch(Manifest.permission.READ_CONTACTS)
                }
            }
            Toggle(stringResource(R.string.telegram), telegramOn) { on ->
                store.telegram = on
                telegramOn = on
                if (!on) return@Toggle
                if (!listenerOn()) openListener() else MessageListener.rescan(this@SettingsActivity)
            }
            Text(stringResource(R.string.notification_access), color = ink, fontSize = 15.sp)
            Button(onClick = ::openListener, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_listener))
            }
            Text(stringResource(R.string.battery_help), color = ink, fontSize = 15.sp)
            Button(onClick = ::openBattery, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_battery))
            }
            Button(onClick = ::stopListening, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.stop))
            }
            Text(stringResource(R.string.trace_help), color = ink, fontSize = 15.sp)
            Button(onClick = ::sendTraces, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.trace_button))
            }
            Text(
                text = stringResource(R.string.trace_id, remember { store.installId() }),
                color = ink,
                fontSize = 15.sp,
            )
            if (traceNote.isNotBlank()) {
                Text(traceNote, color = ink, fontSize = 15.sp)
            }
            Text(
                text = stringResource(R.string.app_version, appVersion()),
                color = ink,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(onClick = { openLink(Trace.ISSUES) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.issues_link))
            }
            Button(onClick = { openLink(Trace.SITE) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.company_link))
            }
            Text(
                text = stringResource(R.string.author_name),
                color = ink,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }

    private fun appVersion(): String = try {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

    @Composable
    private fun LangChip(tag: String, label: String) {
        FilterChip(
            selected = store.language == tag,
            onClick = {
                store.language = tag
                recreate()
            },
            label = { Text(label) },
        )
    }

    @Composable
    private fun Stepper(label: String, value: String, down: () -> Unit, up: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = Color(0xFFE7F0EA), modifier = Modifier.weight(1f))
            TextButton(onClick = down) { Text("−") }
            Text(value, color = Color(0xFFE7F0EA))
            TextButton(onClick = up) { Text("+") }
        }
    }

    @Composable
    private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = Color(0xFFE7F0EA), fontSize = 18.sp)
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }

    private fun listenerOn(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(packageName)
    }

    private fun openListener() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun sendTraces() {
        traceNote = ""
        try {
            Trace.send(this)
        } catch (_: Exception) {
            traceNote = getString(R.string.trace_failed)
        }
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            traceNote = getString(R.string.trace_failed)
        }
    }

    private fun openBattery() {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName"),
        )
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun stopListening() {
        ListenService.stop(this)
        finish()
    }
}
