package kscript.app.model

import kscript.app.util.OsPath
import kscript.app.util.ShellUtils

class ConfigBuilder internal constructor() {
    var osType: String? = null
    var selfName: String? = null
    var kscriptDir: OsPath? = null
    var customPreamble: String? = null
    var intellijCommand: String? = null
    var gradleCommand: String? = null
    var kotlinHome: OsPath? = null
    var homeDir: OsPath? = null
    var providedKotlinOpts: String? = null
    var repositoryUrl: String? = null
    var repositoryUser: String? = null
    var repositoryPassword: String? = null

    fun build(): Config {
        //Java resolved env variables paths are always in native format; All paths should be stored in Config as native,
        //and then converted as needed to shell format.

        val osType = OsType.findOrThrow(requireNotNull(osType))

        val selfName = selfName ?: System.getenv("KSCRIPT_NAME") ?: "kscript"
        val kscriptDir = kscriptDir ?: OsPath.createOrThrow(
            OsType.native, System.getenv("KSCRIPT_DIR") ?: (System.getProperty("user.home")!! + "/.kscript")
        )
        val customPreamble = customPreamble ?: System.getenv("KSCRIPT_PREAMBLE") ?: ""
        val intellijCommand = intellijCommand ?: System.getenv("KSCRIPT_COMMAND_IDEA") ?: "idea"
        val gradleCommand = gradleCommand ?: System.getenv("KSCRIPT_COMMAND_GRADLE") ?: "gradle"

        val kotlinHome = kotlinHome ?: (System.getenv("KOTLIN_HOME") ?: ShellUtils.guessKotlinHome(osType))?.let {
            OsPath.createOrThrow(OsType.native, it)
        } ?: throw IllegalStateException("KOTLIN_HOME is not set and could not be inferred from context.")

        val homeDir = homeDir ?: OsPath.createOrThrow(OsType.native, System.getProperty("user.home")!!)
        val providedKotlinOpts = providedKotlinOpts ?: System.getenv("KSCRIPT_KOTLIN_OPTS") ?: ""
        val repositoryUrl = repositoryUrl ?: System.getenv("KSCRIPT_REPOSITORY_URL") ?: ""
        val repositoryUser = repositoryUser ?: System.getenv("KSCRIPT_REPOSITORY_USER") ?: ""
        val repositoryPassword = repositoryPassword ?: System.getenv("KSCRIPT_REPOSITORY_PASSWORD") ?: ""

        val osConfig = OsConfig(
            osType, selfName, intellijCommand, gradleCommand, homeDir, kscriptDir, kotlinHome
        )

        val scriptingConfig = ScriptingConfig(
            customPreamble, providedKotlinOpts, repositoryUrl, repositoryUser, repositoryPassword
        )

        return Config(
            osConfig, scriptingConfig
        )
    }
}
