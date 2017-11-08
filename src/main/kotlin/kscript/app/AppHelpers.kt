package kscript.app

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
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

/** see discussion on https://github.com/holgerbrandl/kscript/issues/15*/
fun guessKotlinHome(): String? {
    return evalBash("KOTLIN_RUNNER=1 JAVACMD=echo kotlinc").stdout.run {
        "kotlin.home=([^\\s]*)".toRegex()
                .find(this)?.groups?.get(1)?.value
    }
}


fun createTmpScript(scriptText: String): File {
    return File(SCRIPT_TEMP_DIR, "scriptlet.${md5(scriptText)}.kts").apply {
        writeText(scriptText)
    }
}


fun fetchFromURL(scriptURL: String): File? {
    val urlHash = md5(scriptURL)
    val urlCache = File(KSCRIPT_CACHE_DIR, "/url_cache_${urlHash}.kts")

    if (!urlCache.isFile) {
        urlCache.writeText(URL(scriptURL).readText())
    }

    return urlCache
}


fun md5(byteProvider: () -> ByteArray): String {
    // from https://stackoverflow.com/questions/304268/getting-a-files-md5-checksum-in-java
    val md = MessageDigest.getInstance("MD5")
    md.update(byteProvider())

    // disabled to support java9 which dropeed DataTypeConverter
    //    md.update(byteProvider())
    //    val digestInHex = DatatypeConverter.printHexBinary(md.digest()).toLowerCase()

    val digestInHex = bytesToHex(md.digest()).toLowerCase()

    return digestInHex.substring(0, 16)
}

fun md5(msg: String) = md5 { msg.toByteArray() }


fun md5(file: File) = md5 { Files.readAllBytes(Paths.get(file.toURI())) }


// from https://github.com/frontporch/pikitis/blob/master/src/test/kotlin/repacker.tests.kt
private fun bytesToHex(buffer: ByteArray): String {
    val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    val len = buffer.count()
    val result = StringBuffer(len * 2)
    var ix = 0
    while (ix < len) {
        val octet = buffer[ix].toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
        ix++
    }
    return result.toString()
}


fun numLines(str: String) = str.split("\r\n|\r|\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size


fun info(msg: String) = System.err.println(msg)


fun launchIdeaWithKscriptlet(scriptFile: File, dependencies: List<String>): String {
    System.err.println("Setting up idea project from ${scriptFile}")

    //    val tmpProjectDir = createTempDir("edit_kscript", suffix="")
    //            .run { File(this, "kscript_tmp_project") }
    //            .apply { mkdir() }


    //  fixme use tmp instead of cachdir. Fails for now because idea gradle import does not seem to like tmp
    val tmpProjectDir = KSCRIPT_CACHE_DIR
            .run { File(this, "kscript_tmp_project__${scriptFile.name}") }
            .apply { mkdir() }
    //    val tmpProjectDir = File("/Users/brandl/Desktop/")
    //            .run { File(this, "kscript_tmp_project") }
    //            .apply { mkdir() }

    val stringifiedDeps = dependencies.map { "    compile \"$it\"" }.joinToString("\n")

    val gradleScript = """
group 'com.github.holgerbrandl.kscript.editor'
version '0.1-SNAPSHOT'

apply plugin: 'java'
apply plugin: 'kotlin'


dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib:${'$'}kotlin_version"
$stringifiedDeps
}

repositories {
    mavenCentral()
    jcenter()
}

sourceSets {
    main {
        java {
            srcDirs 'src'
        }
    }
}

buildscript {
    ext.kotlin_version = '1.1.4'

    repositories {
        jcenter()
    }

    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version"
    }
}
"""

    File(tmpProjectDir, "build.gradle").writeText(gradleScript)

    // also copy script reource in
    File(tmpProjectDir, "src").apply {
        mkdir()
        scriptFile.copyTo(File(this, scriptFile.name))

    }

    return "idea ${tmpProjectDir.absolutePath}"
}