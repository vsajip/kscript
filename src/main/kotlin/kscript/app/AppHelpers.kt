package kscript.app

import java.io.File
import kotlin.system.exitProcess

data class ProcessResult(val command: String, val exitCode: Int, val stdout: String, val stderr: String) {

    override fun toString(): String {
        return """
            Exit Code   : ${exitCode}Comand      : ${command}
            Stdout      : ${stdout}
            Stderr      : """.trimIndent() + "\n" + stderr
    }
}

fun evalBash(cmd: String, wd: File? = null): ProcessResult = runProcess("bash", "-c", cmd, wd = wd)


fun runProcess(cmd: String, wd: File? = null): ProcessResult {
    val parts = cmd.split("\\s".toRegex())
    return runProcess(cmd = *parts.toTypedArray(), wd = wd)
}

fun runProcess(vararg cmd: String, wd: File? = null): ProcessResult {

    try {
        // simplify with https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
        val proc = ProcessBuilder(cmd.asList()). //.inheritIO();
                directory(wd).
                redirectOutput(ProcessBuilder.Redirect.PIPE).
                redirectError(ProcessBuilder.Redirect.PIPE).
                // see https://youtrack.jetbrains.com/issue/KT-20785
                apply { environment()["KOTLIN_RUNNER"] = "" }.
                start();


        val exitVal = proc.waitFor()

        return ProcessResult(cmd.joinToString(" "), exitVal,
                proc.inputStream.bufferedReader().readText(),
                proc.errorStream.bufferedReader().readText()
        )

    } catch (t: Throwable) {
        throw RuntimeException(t)
    }
}


object ShellUtils {

    fun isInPath(tool: String) = evalBash("which $tool").stdout.trim().isNotBlank()

    fun requireInPath(tool: String) = errorIf(!isInPath(tool)) { "$tool is not in PATH" }

}

fun errorMsg(msg: String) {
    System.err.println("[ERROR] " + msg)
}

fun errorIf(value: Boolean, lazyMessage: () -> Any) {
    if (value) {
        errorMsg(lazyMessage().toString())
        quit(1)
    }
}

fun quit(status: Int): Nothing {
    print(if (status == 0) "true" else "false")
    exitProcess(status)
}
