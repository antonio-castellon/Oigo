package ch.castellon.oigo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}