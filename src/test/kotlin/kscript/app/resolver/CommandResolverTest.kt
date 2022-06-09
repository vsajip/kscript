package kscript.app.resolver

import assertk.assertThat
import assertk.assertions.isEqualTo
import kscript.app.creator.JarArtifact
import kscript.app.model.CompilerOpt
import kscript.app.model.Config
import kscript.app.model.OsType
import org.junit.jupiter.api.Test
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Paths

internal class CommandResolverTest {
    @Test
    fun compileKotlin() {
        val config = createConfig(OsType.LINUX)
        val commandResolver = CommandResolver(config)
        val jarPath = config.homeDir.resolve("/cache/somefile.jar")
        val depPaths = setOf(
            config.homeDir.resolve("/.m2/somepath/dep1.jar"),
            config.homeDir.resolve("/.m2/somepath/dep2.jar"),
            config.homeDir.resolve("/.m2/somepath/dep3.jar")
        )

        val filePaths = setOf(
            config.homeDir.resolve("/source/somepath/dep1.jar"), config.homeDir.resolve("/source/somepath/dep2.jar")
        )

        val compilerOpts = setOf(CompilerOpt("-abc"), CompilerOpt("-def"), CompilerOpt("--clear"))

        for( i in 0 until jarPath.toAbsolutePath().nameCount) {
            println(jarPath.getName(i))
        }

        println("home: " + config.homeDir)
        println("jarFile: " + jarPath)

        assertThat(commandResolver.compileKotlin(jarPath, depPaths, filePaths, compilerOpts)).isEqualTo(
            "C:\\home\\my workspace\\Code\\kotlin\\bin\\kotlinc -abc -def --clear -classpath \"C:\\.m2\\somepath\\dep1.jar:C:\\.m2\\somepath\\dep2.jar:C:\\.m2\\somepath\\dep3.jar\" -d 'C:\\cache\\somefile.jar' 'C:\\source\\somepath\\dep1.jar' 'C:\\source\\somepath\\dep2.jar'")
    }

    private fun createConfig(osType: OsType): Config {
        val homeDir = when (osType) {
            OsType.LINUX, OsType.DARWIN, OsType.FREEBSD -> Paths.get("/home/my workspace/Code")
            OsType.WINDOWS -> Paths.get("C:\\My Workspace\\Code")
            OsType.CYGWIN -> Paths.get("/cygdrive/c/my workspace/Code")
            OsType.MSYS -> Paths.get("/c/my workspace/Code")
        }

        homeDir.fileSystem

        FileSystems.getDefault()



        val classPathSeparator = if (osType.isWindowsLike() || osType.isUnixHostedOnWindows()) ';' else ':'
        val hostPathSeparatorChar = if (osType.isUnixLike()) ':' else ';'

        return Config(
            osType,
            "kscript",
            homeDir.resolve(".kscript/"),
            "",
            "idea",
            "gradle",
            homeDir.resolve("kotlin/"),
            classPathSeparator,
            hostPathSeparatorChar,
            homeDir,
            "",
            "",
            "",
            "",
        )
    }
}
