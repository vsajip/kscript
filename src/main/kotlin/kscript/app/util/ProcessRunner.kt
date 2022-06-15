package kscript.app.util

import kscript.app.util.Logger.devMsg
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.function.Consumer
import java.util.regex.Pattern

data class ProcessResult(val command: String, val exitCode: Int, val stdout: String, val stderr: String) {
    override fun toString(): String {
        return """
                Command     : $command
                Exit Code   : $exitCode   
                Stdout      : $stdout
                Stderr      : 
                """.trimIndent() + "\n" + stderr
    }
}

object ProcessRunner {
    //Pattern for splitting command by space, but still keeping spaces inside quotes " or '
    private val pattern = Pattern.compile("([^\"']\\S*|[\"|'].+?[\"|'])\\s*")

    fun runProcess(cmd: String, wd: File? = null): ProcessResult {
        val command = mutableListOf<String>()

        val matcher = pattern.matcher(cmd)
        while (matcher.find()) {
            command.add(matcher.group(1))
        }

        return runProcess(command, wd = wd)
    }

    fun runProcess(vararg cmd: String, wd: File? = null): ProcessResult {
        return runProcess(cmd.asList(), wd)
    }

    fun runProcess(
        cmd: List<String>,
        wd: File? = null,
        stdoutConsumer: Consumer<String> = StringBuilderConsumer(),
        stderrConsumer: Consumer<String> = StringBuilderConsumer()
    ): ProcessResult {
        try {
            // simplify with https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
            val proc = ProcessBuilder(cmd).directory(wd).
                // see https://youtrack.jetbrains.com/issue/KT-20785
            apply { environment()["KOTLIN_RUNNER"] = "" }.start()

            // we need to gobble the streams to prevent that the internal pipes hit their respective buffer limits, which
            // would lock the sub-process execution (see see https://github.com/holgerbrandl/kscript/issues/55
            // https://stackoverflow.com/questions/14165517/processbuilder-forwarding-stdout-and-stderr-of-started-processes-without-blocki
            val stdoutGobbler = StreamGobbler(proc.inputStream, stdoutConsumer).apply { start() }
            val stderrGobbler = StreamGobbler(proc.errorStream, stderrConsumer).apply { start() }

            val exitVal = proc.waitFor()

            // we need to wait for the gobbler threads, or we may lose some output (e.g. in case of short-lived processes
            stderrGobbler.join()
            stdoutGobbler.join()

            return ProcessResult(cmd.joinToString(" "), exitVal, stdoutConsumer.toString(), stderrConsumer.toString())
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }
}

private class StreamGobbler(private val inputStream: InputStream, private val consumeInputLine: Consumer<String>) :
    Thread() {

    override fun run() {
        BufferedReader(InputStreamReader(inputStream)).lines().forEach(consumeInputLine)
    }
}

private open class StringBuilderConsumer : Consumer<String> {
    private val sb = StringBuilder()

    override fun accept(t: String) {
        sb.appendLine(t)
    }

    override fun toString(): String {
        return sb.toString()
    }
}
