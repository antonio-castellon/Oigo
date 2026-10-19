package ch.castellon.oigo

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

/** Prominent disclosure for Google Play. [GrokClient.ask] sends nothing to xAI until it is accepted. */
object Consent {
    const val PRIVACY_URL = "https://www.castellon.ch/oigo.privacy.html"

    /** Saves the answer. A refusal also stops listening, so the microphone is not left open for nothing. */
    fun answer(context: Context, accepted: Boolean) {
        Store(context).xaiConsent = accepted
        if (!accepted && EarState.listening.value) ListenService.stop(context)
    }
}

/** Accept or decline saves the answer. Back or a tap outside only closes it. */
@Composable
fun XaiDisclosure(onAnswer: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val body = stringResource(R.string.xai_consent_text)
    val link = stringResource(R.string.xai_consent_link)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.xai_consent_title)) },
        text = {
            Text(
                buildAnnotatedString {
                    append(body)
                    append("\n\n")
                    withLink(
                        LinkAnnotation.Url(
                            Consent.PRIVACY_URL,
                            TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
                        ),
                    ) { append(link) }
                },
            )
        },
        confirmButton = {
            TextButton(onClick = { onAnswer(true) }) { Text(stringResource(R.string.xai_consent_accept)) }
        },
        dismissButton = {
            TextButton(onClick = { onAnswer(false) }) { Text(stringResource(R.string.xai_consent_decline)) }
        },
    )
}
