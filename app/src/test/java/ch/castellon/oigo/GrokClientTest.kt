package ch.castellon.oigo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrokClientTest {
    @Test
    fun effortFollowsTheModel() {
        assertEquals("none", reasoningEffort("grok-4.3", lookup = false))
        assertEquals("low", reasoningEffort("grok-4.3", lookup = true))
        assertEquals("low", reasoningEffort("grok-4.7", lookup = false))
        assertEquals("low", reasoningEffort("grok-4.7", lookup = true))
        assertNull(reasoningEffort("grok-5", lookup = false))
        assertNull(reasoningEffort("grok-4-fast", lookup = true))
    }

    @Test
    fun longRouteIsGrok47HighWithin30Seconds() {
        val long = routeFor("grok-4.3", lookup = false, long = true)
        assertEquals("grok-4.7", long.model)
        assertEquals("high", long.effort)
        assertTrue(long.long)
        assertTrue(long.connectMs + long.readMs <= 30_000)
    }

    @Test
    fun normalRouteKeepsSettingsModelAndTimeouts() {
        val plain = routeFor("  ", lookup = false, long = false)
        assertEquals("grok-4.3", plain.model)
        assertEquals("none", plain.effort)
        assertEquals(20_000, plain.connectMs)
        assertEquals(35_000, plain.readMs)
        assertFalse(plain.long)
        val typed = routeFor("grok-4.7", lookup = false, long = false)
        assertEquals("grok-4.7", typed.model)
        assertEquals("low", typed.effort)
        assertNull(routeFor("grok-5", lookup = true, long = false).effort)
    }

    @Test
    fun choiceIsParsedPerLanguage() {
        assertTrue(wantsLong("La larga", "es"))
        assertTrue(wantsLong("completa, por favor", "es"))
        assertFalse(wantsLong("corta", "es"))
        assertFalse(wantsLong("breve", "es"))
        assertTrue(wantsLong("The long one", "en"))
        assertFalse(wantsLong("short please", "en"))
        assertTrue(wantsLong("Die ausführliche", "de"))
        assertTrue(wantsLong("lang", "de"))
        assertFalse(wantsLong("kurz", "de"))
        assertTrue(wantsLong("La longue", "fr"))
        assertTrue(wantsLong("complète", "fr"))
        assertFalse(wantsLong("courte", "fr"))
        assertTrue(wantsLong("lunga", "it"))
        assertFalse(wantsLong("breve", "it"))
    }

    @Test
    fun silenceTimeoutOrUnknownIsShort() {
        assertFalse(wantsLong(null, "es"))
        assertFalse(wantsLong("", "es"))
        assertFalse(wantsLong("no sé", "es"))
        assertFalse(wantsLong("corta o larga", "es"))
        assertFalse(wantsLong("long", "es"))
    }

    @Test
    fun longFormTopicsAreHistoryStoriesAndReading() {
        assertTrue(isLongForm("Cuéntame la historia de Roma"))
        assertTrue(isLongForm("Cuéntame un cuento"))
        assertTrue(isLongForm("Tell me a story"))
        assertTrue(isLongForm("Léeme algo de Cervantes"))
        assertTrue(isLongForm("Erzähl mir die Geschichte der Römer"))
        assertFalse(isLongForm("Qué tiempo hace mañana"))
        assertFalse(isLongForm("Lee mis mensajes de WhatsApp"))
        assertFalse(isLongForm("Hola, qué tal"))
    }

    @Test
    fun refusedKeysAreClassified() {
        val wrong = """{"code":"invalid-argument","error":"Incorrect API key provided: xa***. You can obtain an API key from https://console.x.ai."}"""
        assertEquals(GrokClient.Answer.BadKey, keyProblem(400, wrong, sso = false))
        assertEquals(GrokClient.Answer.BadKey, keyProblem(401, "", sso = false))
        assertEquals(GrokClient.Answer.BadKey, keyProblem(403, "", sso = false))
        assertEquals(GrokClient.Answer.SessionExpired, keyProblem(401, "", sso = true))
        assertEquals(GrokClient.Answer.SessionExpired, keyProblem(403, "", sso = true))
    }

    @Test
    fun otherErrorsAreNotKeyProblems() {
        val reasoning = """{"code":"invalid-argument","error":"Model grok-4.7 does not support reasoning effort none"}"""
        assertNull(keyProblem(400, reasoning, sso = false))
        assertNull(keyProblem(402, "", sso = false))
        assertNull(keyProblem(429, "", sso = false))
        assertNull(keyProblem(500, "API key service down", sso = false))
        assertNull(keyProblem(200, "", sso = false))
    }
}
