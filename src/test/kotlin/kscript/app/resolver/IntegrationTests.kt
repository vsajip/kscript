package kscript.app.resolver

import org.junit.jupiter.api.Test

class IntegrationTests {

    @Test
    fun `it should run kscript and resolve dependencies`(){
        kscript.app.main(arrayOf("log4j_dep.kts"))
    }
}