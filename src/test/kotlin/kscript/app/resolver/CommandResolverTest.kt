package kscript.app.resolver

import kscript.app.model.*
import kscript.app.util.OsPath
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems

internal class CommandResolverTest {
    @Test
    fun compileKotlin() {
        val config = createConfig(OsType.LINUX)
        val commandResolver = CommandResolver(config)
        val jarPath = config.osConfig.userHomeDir.resolve("/cache/somefile.jar")
        val depPaths = setOf(
            config.osConfig.userHomeDir.resolve("/.m2/somepath/dep1.jar"),
            config.osConfig.userHomeDir.resolve("/.m2/somepath/dep2.jar"),
            config.osConfig.userHomeDir.resolve("/.m2/somepath/dep3.jar")
        )

        val filePaths = setOf(
            config.osConfig.userHomeDir.resolve("/source/somepath/dep1.jar"),
            config.osConfig.userHomeDir.resolve("/source/somepath/dep2.jar")
        )

        val compilerOpts = setOf(CompilerOpt("-abc"), CompilerOpt("-def"), CompilerOpt("--clear"))

//        for( i in 0 until jarPath.toAbsolutePath().nameCount) {
//            println(jarPath.getName(i))
//        }

        println("home: " + config.osConfig.userHomeDir)
        println("jarFile: " + jarPath)

//        assertThat(commandResolver.compileKotlin(jarPath, depPaths, filePaths, compilerOpts)).isEqualTo(
//            "C:\\home\\my workspace\\Code\\kotlin\\bin\\kotlinc -abc -def --clear -classpath \"C:\\.m2\\somepath\\dep1.jar:C:\\.m2\\somepath\\dep2.jar:C:\\.m2\\somepath\\dep3.jar\" -d 'C:\\cache\\somefile.jar' 'C:\\source\\somepath\\dep1.jar' 'C:\\source\\somepath\\dep2.jar'")
    }

    private fun createConfig(osType: OsType): Config {
        val homeDir = when (osType) {
            OsType.LINUX, OsType.MAC, OsType.FREEBSD -> OsPath.createOrThrow(osType, "/home/my workspace/Code")
            OsType.WINDOWS -> OsPath.createOrThrow(osType, "C:\\My Workspace\\Code")
            OsType.CYGWIN -> OsPath.createOrThrow(osType, "/cygdrive/c/my workspace/Code")
            OsType.MSYS -> OsPath.createOrThrow(osType, "/c/my workspace/Code")
        }

        //homeDir.fileSystem

        FileSystems.getDefault()

        val classPathSeparator = if (osType.isWindowsLike() || osType.isPosixHostedOnWindows()) ';' else ':'
        val hostPathSeparatorChar = if (osType.isPosixLike()) ':' else ';'

        val osConfig = OsConfig(
            osType,
            if (osType.isPosixHostedOnWindows()) OsType.WINDOWS else osType,
            "kscript",
            "idea",
            "gradle",
            homeDir,
            homeDir.resolve(".kscript/"),
            homeDir.resolve("kotlin/"),
            classPathSeparator,
            hostPathSeparatorChar
        )

        val scriptingConfig = ScriptingConfig("", "", "", "", "")

        return Config(osConfig, scriptingConfig)
    }
}
