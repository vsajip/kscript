package kscript.app.parser

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import kscript.app.model.Dependency
import kscript.app.model.Import
import kscript.app.parser.LineParser.parseDependency
import kscript.app.parser.LineParser.parseImport
import org.junit.jupiter.api.Test

class LineProcessorTest {
    @Test
    fun `Import processing`() {
        assertThat(parseImport("import com.script.test1")).isNotNull().let {
            it.prop(Import::importName).isEqualTo("com.script.test1")
        }

        assertThat(parseImport("      import com.script.test2            ")).isNotNull().let {
            it.prop(Import::importName).isEqualTo("com.script.test2")
            it.prop(Import::code).isEqualTo("      import com.script.test2            ")
        }
    }

    @Test
    fun `Dependency processing`() {
        assertThat(parseDependency("@file:DependsOn(\"org.javamoney:moneta:pom:1.3\")")).isNotNull().let {
            it.prop(Dependency::dependencies).isEqualTo(listOf("org.javamoney:moneta:pom:1.3"))
        }

        assertThat(parseDependency("@file:DependsOn(\"org.javamoney:moneta:pom:1.3\", \"log4j:log4j:1.2.14\", \"com.offbytwo:docopt:0.6.0.20150202\")")).isNotNull().let {
            it.prop(Dependency::dependencies).isEqualTo(listOf("org.javamoney:moneta:pom:1.3", "log4j:log4j:1.2.14", "com.offbytwo:docopt:0.6.0.20150202"))
        }

        assertThat(parseDependency("@file:DependsOnMaven(\"org.javamoney:moneta:pom:1.3\")")).isNotNull().let {
            it.prop(Dependency::dependencies).isEqualTo(listOf("org.javamoney:moneta:pom:1.3"))
        }

        assertThat(parseDependency("@file:DependsOnMaven(\"org.javamoney:moneta:pom:1.3\", \"log4j:log4j:1.2.14\", \"com.offbytwo:docopt:0.6.0.20150202\")")).isNotNull().let {
            it.prop(Dependency::dependencies).isEqualTo(listOf("org.javamoney:moneta:pom:1.3", "log4j:log4j:1.2.14", "com.offbytwo:docopt:0.6.0.20150202"))
        }

        assertThat(parseDependency("//DEPS org.javamoney:moneta:pom:1.3")).isNotNull().let {
            it.prop(Dependency::dependencies).isEqualTo(listOf("org.javamoney:moneta:pom:1.3"))
        }

        assertThat(parseDependency("//DEPS org.javamoney:moneta:pom:1.3, log4j:log4j:1.2.14, com.offbytwo:docopt:0.6.0.20150202")).isNotNull().let {
            it.prop(Dependency::dependencies).isEqualTo(listOf("org.javamoney:moneta:pom:1.3", "log4j:log4j:1.2.14", "com.offbytwo:docopt:0.6.0.20150202"))
        }
    }
}
