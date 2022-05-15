package kscript.app.resolver

import io.mockk.mockk
import kscript.app.appdir.Cache
import kscript.app.creator.JarArtifact
import kscript.app.model.Config
import kscript.app.model.OsType
import kscript.app.parser.Parser
import org.junit.jupiter.api.Test
import java.nio.file.Paths

internal class CommandResolverTest {
//    private val testHome = Paths.get("build/tmp/script_resolver_test")
//    private val config =
//        Config.builder().apply { osType = OsType.LINUX.osName; homeDir = testHome.resolve("home") }.build()
//    private val cache = mockk<Cache>()
//    private val contentResolver = ContentResolver(cache)
//    private val sectionResolver = SectionResolver(Parser(), contentResolver, config)
//    private val scriptResolver = ScriptResolver(sectionResolver, contentResolver)
//    private val commandResolver = CommandResolver(config, scriptResolver.resolve("println(\"Kotlin rocks\")"))

    @Test
    fun executeKotlin() {
        val jarArtifact = JarArtifact(Paths.get("/home/vagrant/test.jar"), "Main_Scriplet")
        val dependencies = setOf(Paths.get("/home/vagrant/deps/test1.jar"), Paths.get("/home/vagrant/deps/test2.jar"))
        //println(commandResolver.executeKotlin(jarArtifact, dependencies, emptyList()))
    }
}
