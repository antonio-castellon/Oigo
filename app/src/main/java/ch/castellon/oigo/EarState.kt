package ch.castellon.oigo

import kotlinx.coroutines.flow.MutableStateFlow

/** What the screen shows. The service writes it. The activity only reads it. */
object EarState {
    val listening = MutableStateFlow(false)
    val status = MutableStateFlow("")
    val inConversation = MutableStateFlow(false)
    val reply = MutableStateFlow("")
    /** Raw microphone level. Speech on this phone sits near 0.01. */
    val mic = MutableStateFlow(0f)
    val debug = MutableStateFlow(false)

    /** One heard phrase. Green on screen when it was sent to Grok. */
    data class HeardLine(val text: String, val sent: Boolean)

    /** Last heard phrases, newest last. Shown only while debug is on. */
    val lines = MutableStateFlow<List<HeardLine>>(emptyList())
}
