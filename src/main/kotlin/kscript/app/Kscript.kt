package kscript.app

import kscript.app.code.Templates
import kscript.app.model.Config
import kscript.app.util.Logger
import kscript.app.util.Logger.errorMsg
import kscript.app.util.ShellUtils.evalBash
import kscript.app.util.ShellUtils.quit
import kscript.app.util.VersionChecker
import org.docopt.DocOptWrapper

/**
 * A kscript - Scripting enhancements for Kotlin
 *
 * For details and license see https://github.com/holgerbrandl/kscript
 *
 * @author Holger Brandl
 * @author Marcin Kuszczak
 */

const val KSCRIPT_VERSION = "4.0.4"

fun main(args: Array<String>) {
    try {
        val config = Config.builder().apply { osType = System.getenv("OSTYPE") }.build()
        val remainingArgs = args

        // skip org.docopt for version and help to allow for lazy version-check
        val usage = Templates.usageOptions(config.selfName, KSCRIPT_VERSION)

        if (remainingArgs.size == 1 && listOf("--help", "-h", "--version", "-v").contains(remainingArgs[0])) {
            Logger.info(usage)
            VersionChecker.versionCheck(KSCRIPT_VERSION)
            val systemInfo = evalBash(config.osType, "kotlin -version").stdout
            Logger.info("Kotlin    : " + systemInfo.split('(')[0].removePrefix("Kotlin version").trim())
            Logger.info("Java      : " + systemInfo.split('(')[1].split('-', ')')[0].trim())
            return
        }

        // note: with current implementation we still don't support `kscript -1` where "-1" is a valid kotlin expression
        val userArgs = remainingArgs.dropWhile { it.startsWith("-") && it != "-" }.drop(1)
        val kscriptArgs = remainingArgs.take(remainingArgs.size - userArgs.size)

        val docopt = DocOptWrapper(kscriptArgs, usage)

        KscriptHandler(config, docopt).handle(userArgs)
    } catch (e: Exception) {
        errorMsg(e)
        quit(1)
    }
}
