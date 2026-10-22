package ch.castellon.oigo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootReceiverTest {
    @Test
    fun actsOnlyWhenEnabledAndOnBoot() {
        assertTrue(BootReceiver.acts("android.intent.action.BOOT_COMPLETED", true))
        assertFalse(BootReceiver.acts("android.intent.action.BOOT_COMPLETED", false))
        assertFalse(BootReceiver.acts("android.intent.action.LOCKED_BOOT_COMPLETED", true))
        assertFalse(BootReceiver.acts(null, true))
    }
}