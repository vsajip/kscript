package kscript.app.resolver

import assertk.assertThat
import assertk.assertions.isEqualTo
import kscript.app.model.*
import kscript.app.util.OsPath
import org.junit.jupiter.api.Test

internal class CommandResolverTest {
    @Test
    fun compileKotlin() {
        val config = createConfig(OsType.WINDOWS)
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

        val compilerOpts = setOf(CompilerOpt("-abc"), CompilerOpt("-def"), CompilerOpt("--experimental"))

//        assertThat(commandResolver.compileKotlin(jarPath, depPaths, filePaths, compilerOpts)).isEqualTo(
//            "C:\\home\\my workspace\\Code\\kotlin\\bin\\kotlinc -abc -def --clear -classpath \"C:\\.m2\\somepath\\dep1.jar:C:\\.m2\\somepath\\dep2.jar:C:\\.m2\\somepath\\dep3.jar\" -d 'C:\\cache\\somefile.jar' 'C:\\source\\somepath\\dep1.jar' 'C:\\source\\somepath\\dep2.jar'"
//        )
    }

    private fun createConfig(osType: OsType): Config {
        val homeDir = when (osType) {
            OsType.LINUX, OsType.MAC, OsType.FREEBSD -> OsPath.createOrThrow(osType, "/home/my workspace/Code")
            OsType.WINDOWS -> OsPath.createOrThrow(osType, "C:\\My Workspace\\Code")
            OsType.CYGWIN -> OsPath.createOrThrow(osType, "/cygdrive/c/my workspace/Code")
            OsType.MSYS -> OsPath.createOrThrow(osType, "/c/my workspace/Code")
        }

        val osConfig = OsConfig(
            osType,
            "kscript",
            "idea",
            "gradle",
            homeDir,
            homeDir.resolve("./.kscript/"),
            homeDir.resolve("./kotlin/"),
        )

        val scriptingConfig = ScriptingConfig("", "", "", "", "")

        return Config(osConfig, scriptingConfig)
    }
}
