package kscript.app

import kscript.app.util.Logger
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.util.function.Consumer
import kotlin.system.exitProcess


data class ProcessResult(val command: String, val exitCode: Int, val stdout: String, val stderr: String) {

    override fun toString(): String {
        return """
            Exit Code   : ${exitCode}Comand      : ${command}
            Stdout      : ${stdout}
            Stderr      : """.trimIndent() + "\n" + stderr
    }
}

fun evalBash(
    cmd: String,
    wd: File? = null,
    stdoutConsumer: Consumer<String> = StringBuilderConsumer(),
    stderrConsumer: Consumer<String> = StringBuilderConsumer()
): ProcessResult {
    return runProcess(
        "bash", "-c", cmd, wd = wd, stderrConsumer = stderrConsumer, stdoutConsumer = stdoutConsumer
    )
}


fun runProcess(cmd: String, wd: File? = null): ProcessResult {
    val parts = cmd.split("\\s".toRegex())
    return runProcess(*parts.toTypedArray(), wd = wd)
}

fun runProcess(
    vararg cmd: String,
    wd: File? = null,
    stdoutConsumer: Consumer<String> = StringBuilderConsumer(),
    stderrConsumer: Consumer<String> = StringBuilderConsumer()
): ProcessResult {

    try {
        // simplify with https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
        val proc = ProcessBuilder(cmd.asList()).directory(wd).
            // see https://youtrack.jetbrains.com/issue/KT-20785
        apply { environment()["KOTLIN_RUNNER"] = "" }.start()


        // we need to gobble the streams to prevent that the internal pipes hit their respecitive buffer limits, which
        // would lock the sub-process execution (see see https://github.com/holgerbrandl/kscript/issues/55
        // https://stackoverflow.com/questions/14165517/processbuilder-forwarding-stdout-and-stderr-of-started-processes-without-blocki
        val stdoutGobbler = StreamGobbler(proc.inputStream, stdoutConsumer).apply { start() }
        val stderrGobbler = StreamGobbler(proc.errorStream, stderrConsumer).apply { start() }

        val exitVal = proc.waitFor()

        // we need to wait for the gobbler threads or we may loose some output (e.g. in case of short-lived processes
        stderrGobbler.join()
        stdoutGobbler.join()

        return ProcessResult(cmd.joinToString(" "), exitVal, stdoutConsumer.toString(), stderrConsumer.toString())

    } catch (t: Throwable) {
        throw RuntimeException(t)
    }
}


internal class StreamGobbler(private val inputStream: InputStream, private val consumeInputLine: Consumer<String>) :
    Thread() {

    override fun run() {
        BufferedReader(InputStreamReader(inputStream)).lines().forEach(consumeInputLine)
    }
}

internal open class StringBuilderConsumer : Consumer<String> {
    val sb = StringBuilder()

    override fun accept(t: String) {
        sb.appendLine(t)
    }

    override fun toString(): String {
        return sb.toString()
    }
}


object ShellUtils {
    fun isInPath(tool: String) = evalBash("which $tool").stdout.trim().isNotBlank()
}

fun quit(status: Int): Nothing {
    print(if (status == 0) "true" else "false")
    exitProcess(status)
}

/** see discussion on https://github.com/holgerbrandl/kscript/issues/15*/
fun guessKotlinHome(): String? {
    val kotlinHome = evalBash("KOTLIN_RUNNER=1 JAVACMD=echo kotlinc").stdout.run {
        "kotlin.home=([^\\s]*)".toRegex().find(this)?.groups?.get(1)?.value
    }

    return kotlinHome
}

fun isRegularFile(uri: URI) = uri.scheme.startsWith("file")
