package kscript.app.util

import kscript.app.model.OsType
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess

object ShellUtils {
    fun evalBash(osType: OsType, cmd: String, wd: File? = null): ProcessResult {
        if (osType == OsType.WINDOWS) {
            return ProcessRunner.runProcess("cmd", "/c", cmd, wd = wd)
        }

        return ProcessRunner.runProcess("bash", "-c", cmd, wd = wd)
    }

    fun isInPath(osType: OsType, tool: String) = evalBash(osType, "${if (osType == OsType.WINDOWS) "where" else "which"} $tool").stdout.trim().isNotBlank()

    // see discussion on https://github.com/holgerbrandl/kscript/issues/15
    fun guessKotlinHome(osType: OsType): String? {
        val kotlinHome = evalBash(osType, "KOTLIN_RUNNER=1 JAVACMD=echo kotlinc").stdout.run {
            "kotlin.home=([^\\s]*)".toRegex().find(this)?.groups?.get(1)?.value
        }

        if (osType == OsType.MSYS) {
            return FileUtils.shellToNativePath(osType, kotlinHome)?.absolutePathString()
        }

        return kotlinHome
    }

    fun quit(status: Int): Nothing {
        print(if (status == 0) "true" else "false")
        exitProcess(status)
    }
}
