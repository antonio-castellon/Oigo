package dev.castellon.grok

import kotlinx.coroutines.flow.MutableStateFlow

/** What the screen shows. The service writes it. The activity only reads it. */
object EarState {
    val listening = MutableStateFlow(false)
    val status = MutableStateFlow("")
}
