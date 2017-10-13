package kscript.app

import org.docopt.Docopt
import java.io.File
import kotlin.system.exitProcess

data class ProcessResult(val command: String, val exitCode: Int, val stdout: String, val stderr: String) {

    override fun toString(): String {

        return """
                Exit Code   : $exitCode
                Comand      : $command
                Stdout      : ${stdout}
                Stderr      : ${stderr}""".trimIndent()
    }
}

fun evalBash(cmd: String, wd: File? = null): ProcessResult = runProcess("bash", "-c", cmd, wd = wd)


fun runProcess(cmd:String, wd: File? = null): ProcessResult {
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

    fun requireInPath(tool: String) = require(isInPath(tool)) { "$tool is not in PATH" }

}

fun errorIf(value: Boolean, lazyMessage: () -> Any) {
    if (value) exitOne(lazyMessage)
}

fun exitOne(lazyMessage: () -> Any): Unit {
    System.err.println("[ERROR] " + lazyMessage().toString())
    exitProcess(1)
}


/** Simple Kotlin facade for org.docopt.Docopt.Docopt(java.lang.String) .*/
class DocOpt(args: Array<String>, val usage: String) {

    val parsedArgs = Docopt(usage).parse(args.toList())

    private val myDO by lazy {
        parsedArgs.map {
            it.key.removePrefix("--").replace("[<>]".toRegex(), "") to it.value
        }.toMap()
    }

    fun getString(key: String) = myDO[key]!!.toString()
    fun getStrings(key: String) = (myDO[key]!! as List<*>).map { it as String }

    fun getFile(key: String) = File(getString(key))
    fun getFiles(key: String) = getStrings(key).map { File(it) }


    fun getInt(key: String) = myDO[key]!!.toString().toInt()

    fun getNumber(key: String) = myDO[key]!!.toString().toFloat()

    fun getBoolean(key: String) = myDO[key]!!.toString().toBoolean()

    override fun toString(): String {
        return parsedArgs.toString()
    }
}

internal inline fun warning(value: Boolean, lazyMessage: () -> Any): Unit {
    if (!value) System.err.println(lazyMessage())
}
