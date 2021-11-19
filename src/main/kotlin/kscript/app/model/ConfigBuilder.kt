package kscript.app.model

import kscript.app.util.guessKotlinHome
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class ConfigBuilder internal constructor() {
    var selfName: String = System.getenv("CUSTOM_KSCRIPT_NAME") ?: "kscript"
    var kscriptDir: Path =
        Paths.get(System.getenv("KSCRIPT_DIR") ?: (System.getProperty("user.home")!! + "/.kscript"))
    var customPreamble: String = System.getenv("CUSTOM_KSCRIPT_PREAMBLE") ?: ""
    var intellijCommand: String = System.getenv("KSCRIPT_IDEA_COMMAND") ?: "idea"
    var gradleCommand: String = System.getenv("KSCRIPT_GRADLE_COMMAND") ?: "gradle"
    var kotlinHome: Path? = (System.getenv("KOTLIN_HOME") ?: guessKotlinHome())?.let { Paths.get(it) }
    var classPathSeparator: String =
        if (System.getProperty("os.name").lowercase().contains("windows")) ";" else ":"
    var separatorChar: Char = File.separatorChar
    var homeDir: Path = Paths.get(System.getProperty("user.home")!!)

    fun build(): Config {
        return Config(
            selfName,
            kscriptDir,
            customPreamble,
            intellijCommand,
            gradleCommand,
            kotlinHome,
            classPathSeparator,
            separatorChar,
            homeDir
        )
    }
}
