package kscript.app.model

import kscript.app.util.guessKotlinHome
import java.nio.file.Path
import java.nio.file.Paths

class ConfigBuilder internal constructor () {
    private var selfName: String = System.getenv("CUSTOM_KSCRIPT_NAME") ?: "kscript"
    private var kscriptDir: Path =
        Paths.get(System.getenv("KSCRIPT_DIR") ?: (System.getProperty("user.home")!! + "/.kscript"))
    private var customPreamble: String = System.getenv("CUSTOM_KSCRIPT_PREAMBLE") ?: ""
    private var intellijCommand: String = System.getenv("KSCRIPT_IDEA_COMMAND") ?: "idea"
    private var gradleCommand: String = System.getenv("KSCRIPT_GRADLE_COMMAND") ?: "gradle"
    private var kotlinHome: Path? = (System.getenv("KOTLIN_HOME") ?: guessKotlinHome())?.let { Paths.get(it) }
    private var classPathSeparator: String =
        if (System.getProperty("os.name").lowercase().contains("windows")) ";" else ":"

    fun build(): Config {
        return Config(
            selfName, kscriptDir, customPreamble, intellijCommand, gradleCommand, kotlinHome, classPathSeparator
        )
    }
}
