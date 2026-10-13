package ch.castellon.oigo

import kotlinx.coroutines.flow.MutableStateFlow

/** What the screen shows. The service writes it. The activity only reads it. */
object EarState {
    val listening = MutableStateFlow(false)
    val status = MutableStateFlow("")
    val inConversation = MutableStateFlow(false)
    val reply = MutableStateFlow("")
    /** Boosted microphone level, about 0.12 when speech is loud enough. */
    val mic = MutableStateFlow(0f)
}
