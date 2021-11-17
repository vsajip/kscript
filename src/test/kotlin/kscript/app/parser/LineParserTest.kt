package kscript.app.parser

import assertk.assertThat
import assertk.assertions.*
import kscript.app.model.Dependency
import kscript.app.model.Import
import kscript.app.parser.LineParser.parseDependency
import kscript.app.parser.LineParser.parseImport
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class LineParserTest {
    @Test
    fun `Import processing`() {
        assertThat(parseImport("import com.script.test1")).containsExactlyInAnyOrder(Import("com.script.test1"))
        assertThat(parseImport("      import com.script.test2            ")).containsExactlyInAnyOrder(Import("com.script.test2"))
    }

    @ParameterizedTest
    @MethodSource("staticDependencies")
    fun `Dependency processing - common scenarios - static dependencies`(list: List<String>) {
        val listWithQuotes = list.joinToString(", ") { "\"$it\"" }
        val listWithoutQuotes = list.joinToString(", ")

        for (line in listOf(
            "@file:DependsOn($listWithQuotes)",
            "@file:DependsOn($listWithQuotes) //Comment",
            "@file:DependsOnMaven($listWithQuotes)",
            "      @file:DependsOnMaven($listWithQuotes)    ",
            "//DEPS $listWithoutQuotes",
            "    //DEPS $listWithoutQuotes",
        )) {
            println("Case: '$line'")
            assertThat(parseDependency(line)).containsExactlyInAnyOrder(*list.map { Dependency(it) }.toTypedArray())
        }
    }


    @ParameterizedTest
    @MethodSource("dynamicDependencies")
    fun `Dependency processing - common scenarios - dynamic dependencies`(list: List<String>) {
        val listWithQuotes = list.joinToString(", ") { "\"$it\"" }
        val listWithoutQuotes = list.joinToString(", ")

        for (line in listOf(
            "@file:DependsOn($listWithQuotes)",
            "@file:DependsOn($listWithQuotes) //Comment",
            "@file:DependsOnMaven($listWithQuotes)",
            "      @file:DependsOnMaven($listWithQuotes)    ",
        )) {
            println("Case: '$line'")
            assertThat(parseDependency(line)).containsExactlyInAnyOrder(*list.map { Dependency(it) }.toTypedArray())
        }
    }

    @ParameterizedTest
    @MethodSource("invalidDependencies")
    fun `Dependency processing - common scenarios - invalid dependencies`(list: List<String>) {
        val listWithQuotes = list.joinToString(", ") { "\"$it\"" }
        val listWithoutQuotes = list.joinToString(", ")

        for (line in listOf(
            "@file:DependsOn($listWithQuotes)",
            "@file:DependsOn($listWithQuotes) //Comment",
            "@file:DependsOnMaven($listWithQuotes)",
            "      @file:DependsOnMaven($listWithQuotes)    ",
            "//DEPS $listWithoutQuotes",
            "    //DEPS $listWithoutQuotes",
        )) {
            println("Case: '$line'")
            assertThat { parseDependency(line) }.isFailure().messageContains("$line\nInvalid dependency locator: 'log4j")
        }
    }

    companion object {
        @JvmStatic
        fun staticDependencies(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf("org.javamoney:moneta:pom:1.3")), Arguments.of(
                listOf(
                    "org.javamoney:moneta:pom:1.3", "log4j:log4j:1.2.14", "com.offbytwo:docopt:0.6.0.20150202"
                )
            )
        )

        @JvmStatic
        fun dynamicDependencies(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf("log4j:log4j:[1.2,)", "com.offbytwo:docopt:[0.6,)"))
        )

        @JvmStatic
        fun invalidDependencies(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf("log4j:1.0")),
            Arguments.of(listOf("com.offbytwo:docopt:0.6", "log4j:1.0")),
            Arguments.of(listOf("log4j:::1.0", "com.offbytwo:docopt:0.6", "log4j:1.0")),

        )
    }
}
