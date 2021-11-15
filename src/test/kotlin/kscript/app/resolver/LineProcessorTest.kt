package kscript.app.resolver

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import kscript.app.model.Import
import kscript.app.resolver.LineParser.parseImport
import org.junit.jupiter.api.Test

class LineProcessorTest {
    @Test
    fun `Import processing`() {
//        assertThat(parseImport("import com.script.test2")).isNotNull().isDataClassEqualTo(Import::class.java).let {
//            it.prop(Import::importName).isEqualTo("com.script.test2")
//        }

        //leading and trailing spaces are not important
        assertThat(parseImport("      import com.script.test1            ")).let {
            it.prop(Import::importName).isEqualTo("com.script.test1")
        }
    }

    @Test
    fun `Repository processing`() {

    }
}
