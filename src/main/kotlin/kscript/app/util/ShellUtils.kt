package kscript.app.util

import java.io.File
import kotlin.system.exitProcess

object ShellUtils {
    fun evalBash(cmd: String, wd: File? = null): ProcessResult {
        return ProcessRunner.runProcess("bash", "-c", cmd, wd = wd)
    }

    fun isInPath(tool: String) = evalBash("which $tool").stdout.trim().isNotBlank()

    // see discussion on https://github.com/holgerbrandl/kscript/issues/15
    fun guessKotlinHome(): String? {
        val kotlinHome = evalBash("KOTLIN_RUNNER=1 JAVACMD=echo kotlinc").stdout.run {
            "kotlin.home=([^\\s]*)".toRegex().find(this)?.groups?.get(1)?.value
        }

        return kotlinHome
    }

    fun quit(status: Int): Nothing {
        print(if (status == 0) "true" else "false")
        exitProcess(status)
    }
}
