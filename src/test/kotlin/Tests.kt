import io.kotlintest.matchers.shouldBe
import kscript.app.collectDependencies
import kscript.app.collectRuntimeOptions
import org.junit.Test

/**
 * @author Holger Brandl
 */

class Tests {

    // "comma separated dependencies should be parsed correctly"
    @Test
    fun directiveDependencyCollect() {
        val lines = listOf(
                "//DEPS de.mpicbg.scicomp.joblist:joblist-kotlin:1.1, de.mpicbg.scicomp:kutils:0.7",
                "//DEPS  log4j:log4j:1.2.14"
        )

        val expected = listOf(
                "de.mpicbg.scicomp.joblist:joblist-kotlin:1.1",
                "de.mpicbg.scicomp:kutils:0.7",
                "log4j:log4j:1.2.14"
        )

        collectDependencies(lines) shouldBe expected
    }

    @Test
    fun mixedDependencyCollect() {
        val lines = listOf(
                "//DEPS de.mpicbg.scicomp.joblist:joblist-kotlin:1.1, de.mpicbg.scicomp:kutils:0.7",
                """@file:DependsOn("log4j:log4j:1.2.14")"""
        )

        val expected = listOf(
                "de.mpicbg.scicomp.joblist:joblist-kotlin:1.1",
                "de.mpicbg.scicomp:kutils:0.7",
                "log4j:log4j:1.2.14",
                "com.github.holgerbrandl:kscript-annotations:1.0"
        )

        collectDependencies(lines) shouldBe expected
    }



    // combine kotlin opts spread over multiple lines
    @Test
    fun optsCollect() {
        val lines = listOf(
                "//KOTLIN_OPTS -foo 3 'some file.txt'",
                "//KOTLIN_OPTS  --bar"
        )

        collectRuntimeOptions(lines) shouldBe "-foo 3 'some file.txt' --bar"
    }

    @Test
    fun annotOptsCollect() {
        val lines = listOf(
                "//KOTLIN_OPTS -foo 3 'some file.txt'",
                """@file:KotlinOpts("--bar")"""
        )

        collectRuntimeOptions(lines) shouldBe "-foo 3 'some file.txt' --bar"
    }
}