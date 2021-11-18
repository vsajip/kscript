package kscript.app.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class LoggerTest {
    @Test
    fun `test error message for exception`() {
        assertThat(Logger.errorMsg(IllegalArgumentException("Test message\nSecond line"))).isEqualTo("[kscript] [ERROR] Test message\n[kscript] [ERROR] Second line")
    }
}
