package kscript.app

import kscript.app.ShellUtils.requireInPath
import org.docopt.DocOptWrapper
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.net.URL
import java.net.UnknownHostException
import kotlin.system.exitProcess


/**
 * A kscript - Scripting enhancements for Kotlin
 *
 * For details and license see https://github.com/holgerbrandl/kscript
 *
 * @author Holger Brandl
 */

const val KSCRIPT_VERSION = "2.2.1"

val selfName = System.getenv("CUSTOM_KSCRIPT_NAME") ?: "kscript"

val USAGE = """
$selfName - Enhanced scripting support for Kotlin on *nix-based systems.

Usage:
 $selfName [options] <script> [<script_args>]...
 $selfName --clear-cache
 $selfName --self-update

The <script> can be a  script file (*kts), a script URL, - for stdin, a *.kt source file with a main method, or some kotlin code.

Use '--clear-cache' to wipe cached script jars and urls
Use '--self-update' to update kscript to the latest version

Options:
 -i --interactive        Create interactive shell with dependencies as declared in script
 -t --text               Enable stdin support API for more streamlined text processing
 --idea                  Open script in temporary Intellij session
 -s --silent             Suppress status logging to stderr

Copyright : 2017 Holger Brandl
License   : MIT
Version   : v$KSCRIPT_VERSION
Website   : https://github.com/holgerbrandl/kscript
""".trim()

val KSCRIPT_CACHE_DIR = File(System.getenv("HOME")!!, ".kscript")
val SCRIPT_TEMP_DIR = createTempDir()

fun main(args: Array<String>) {
    // skip org.docopt for version and help to allow for lazy version-check
    if (args.size == 1 && listOf("--help", "-h", "--version", "-v").contains(args[0])) {
        info(USAGE)
        versionCheck()
        quit(0)
    }

    // note: with current impt we still don't support `kscript -1` where "-1" is a valid kotlin expression
    val userArgs = args.dropWhile { it.startsWith("-") }.drop(1)
    val kscriptArgs = args.take(args.size - userArgs.size)

    val docopt = DocOptWrapper(kscriptArgs, USAGE)
    val loggingEnabled = !docopt.getBoolean("silent")


    // create cache dir if it does not yet exist
    if (!KSCRIPT_CACHE_DIR.isDirectory) {
        KSCRIPT_CACHE_DIR.mkdir()
    }

    // optionally clear up the jar cache
    if (docopt.getBoolean("clear-cache")) {
        info("Cleaning up cache...")
        KSCRIPT_CACHE_DIR.listFiles().forEach { it.delete() }
        //        evalBash("rm -f ${KSCRIPT_CACHE_DIR}/*")
        quit(0)
    }

    // optionally self-update kscript ot the newest version
    // (if not local copy is not being maintained by sdkman)
    if (docopt.getBoolean(("self-update"))) {
        if (true || evalBash("which kscript | grep .sdkman").stdout.isNotBlank()) {
            info("Installing latest version of kscript...")
            //            println("sdkman_auto_answer=true && sdk install kscript")

            // create update script
            val updateScript = File(KSCRIPT_CACHE_DIR, "self_update.sh").apply {
                writeText("""
                #!/usr/bin/env bash
                export SDKMAN_DIR="${"$"}{HOME}/.sdkman"
                source "${"$"}{SDKMAN_DIR}/bin/sdkman-init.sh"
                sdkman_auto_answer=true && sdk install kscript
                """.trimIndent())
                setExecutable(true)
            }

            println(updateScript.absolutePath)
        } else {
            info("Self-update is currently just supported via sdkman.")
            info("Please download a new release from https://github.com/holgerbrandl/kscript")
            // todo port sdkman-indpendent self-update
        }

        quit(0)
    }


    // Resolve the script resource argument into an actual file
    val scriptResource = docopt.getString("script")
    val scriptFile = prepareScript(scriptResource, enableSupportApi = docopt.getBoolean("text"))


    val scriptText = scriptFile.readLines()

    // Make sure that dependencies declarations are well formatted
    if (scriptText.any { it.startsWith("// DEPS") }) {
        error("Dependencies must be declared by using the line prefix //DEPS")
    }

    // Find all //DEPS directives and concatenate their values
    val dependencies = collectDependencies(scriptText)
    val customRepos = collectRepos(scriptText)


    //  Create temopary dev environment
    if (docopt.getBoolean("idea")) {
        println(launchIdeaWithKscriptlet(scriptFile, dependencies, customRepos))
        exitProcess(0)
    }


    val classpath = resolveDependencies(dependencies, customRepos, loggingEnabled)

    // Extract kotlin arguments
    val kotlinOpts = collectRuntimeOptions(scriptText)


    //  Optionally enter interactive mode
    if (docopt.getBoolean("interactive")) {
        System.err.println("Creating REPL from ${scriptFile}")
        //        System.err.println("kotlinc ${kotlinOpts} -classpath '${classpath}'")

        println("kotlinc ${kotlinOpts} -classpath ${classpath}")
        exitProcess(0)
    }

    val scriptFileExt = scriptFile.extension
    val scriptCheckSum = md5(scriptFile)


    // Even if we just need and support the //ENTRY directive in case of kt-class
    // files, we extract it here to fail if it was used in kts files.
    val entryDirective = findEntryPoint(scriptText)

    errorIf(entryDirective != null && scriptFileExt == "kts") {
        "@Entry directive is just supported for kt class files"
    }


    val jarFile = if (scriptFile.nameWithoutExtension.endsWith(scriptCheckSum)) {
        File(KSCRIPT_CACHE_DIR, scriptFile.nameWithoutExtension + ".jar")
    } else {
        File(KSCRIPT_CACHE_DIR, scriptFile.nameWithoutExtension + "." + scriptCheckSum + ".jar")
    }

    // Capitalize first letter and get rid of dashes (since this is what kotlin compiler is doing for the wrapper to create a valid java class name)
    val className = scriptFile.nameWithoutExtension
        .replace("[.-]".toRegex(), "_")
        .capitalize()


    // Define the entrypoint for the scriptlet jar
    val execClassName = if (scriptFileExt == "kts") {
        "Main_${className}"
    } else {
        // extract package from kt-file
        val pckg = scriptText.find { it.startsWith("package ") }
            ?.split("[ ]+".toRegex())?.get(1)?.run { this + "." }

        """${pckg ?: ""}${entryDirective ?: "${className}Kt"}"""
    }


    // infer KOTLIN_HOME if not set
    @Suppress("LocalVariableName")
    val KOTLIN_HOME = System.getenv("KOTLIN_HOME") ?: guessKotlinHome()
    errorIf(KOTLIN_HOME == null) {
        "KOTLIN_HOME is not set and could not be inferred from context"
    }


    // If scriplet jar ist not cached yet, build it
    if (!jarFile.isFile) {
        // disabled logging because it seems too much
        // if(loggingEnabled) System.err.print("[kscript] Building script jar...")

        // disabled because a user might have same-named scripts for different projects
        // // remove previous (now outdated) cache jars
        // KSCRIPT_CACHE_DIR.listFiles({
        //     file -> file.name.startsWith(scriptFile.nameWithoutExtension) && file.extension=="jar"
        // }).forEach { it.delete() }


        requireInPath("kotlinc")


        // create main-wrapper for kts scripts

        val wrapperSrcArg = if (scriptFileExt == "kts") {
            val mainKotlin = File(createTempDir("kscript"), execClassName + ".kt")
            mainKotlin.writeText("""
            class Main_${className}{
                companion object {
                    @JvmStatic
                    fun main(args: Array<String>) {
                        val script = Main_${className}::class.java.classLoader.loadClass("${className}")
                        script.getDeclaredConstructor(Array<String>::class.java).newInstance(args);
                    }
                }
            }
            """.trimIndent())

            //            // compile the wrapper
            //            with(evalBash("kotlinc '${mainKotlin}'", wd = mainKotlin.parentFile)) {
            //                errorIf(exitCode != 0) { "Compilation of script-wrapper failed:$stderr" }
            //            }
            //
            //            // update the jar to include main-wrapper
            //            // requireInPath("jar") // disabled because it's another process invocation
            //            val jarUpdateCmd = "jar uf '${jarFile.absoluteFile}' ${mainKotlin.nameWithoutExtension}*.class"
            //            with(evalBash(jarUpdateCmd, wd = mainKotlin.parentFile)) {
            //                errorIf(exitCode != 0) { "Update of script jar with wrapper class failed\n${stderr}" }
            //            }

            //            if(loggingEnabled) System.err.println("Done")
            "'${mainKotlin.absolutePath}'"
        } else {
            ""
        }

        val scriptCompileResult = evalBash("kotlinc -classpath '$classpath' -d '${jarFile.absolutePath}' '${scriptFile.absolutePath}' ${wrapperSrcArg}")
        with(scriptCompileResult) {
            errorIf(exitCode != 0) { "compilation of '$scriptResource' failed\n$stderr" }
        }
    }


    // print the final command to be run by exec
    //    val joinedUserArgs = args.drop(1 + args.indexOfFirst { it == scriptResource }).joinToString(" ")
    val joinedUserArgs = userArgs.joinToString(" ")

    println("kotlin ${kotlinOpts} -classpath ${jarFile}${CP_SEPARATOR_CHAR}${KOTLIN_HOME}${File.separatorChar}lib${File.separatorChar}kotlin-script-runtime.jar${CP_SEPARATOR_CHAR}${classpath} ${execClassName} ${joinedUserArgs} ")
}

data class MavenRepo(val id: String, val url: String)

fun collectRepos(scriptText: List<String>): List<MavenRepo> {
    val dependsOnMavenPrefix = "^@file:MavenRepository[(]".toRegex()
    // only supported annotation format for now

    // @file:MavenRepository("imagej", "http://maven.imagej.net/content/repositories/releases/")
    return scriptText
        .filter { it.contains(dependsOnMavenPrefix) }
        .map { it.replaceFirst(dependsOnMavenPrefix, "").split(")")[0] }
        .map { it.split(",").map { it.trim(' ', '"', '(') }.let { MavenRepo(it[0], it[1]) } }

    // todo add credential support https://stackoverflow.com/questions/36282168/how-to-add-custom-maven-repository-to-gradle
}


fun isKscriptAnnotation(line: String) =
    listOf("DependsOn", "KotlinOpts", "Include", "EntryPoint", "MavenRepository", "DependsOnMaven")
        .any { line.contains("^@file:${it}[(]".toRegex()) }


fun collectRuntimeOptions(scriptText: List<String>): String {
    val koptsPrefix = "//KOTLIN_OPTS "

    var kotlinOpts = scriptText.
        filter { it.startsWith(koptsPrefix) }.
        map { it.replaceFirst(koptsPrefix, "").trim() }

    //support for @file:KotlinOpts see #47
    val annotatonPrefix = "^@file:KotlinOpts[(]".toRegex()
    kotlinOpts += scriptText
        .filter { it.contains(annotatonPrefix) }
        .map { it.replaceFirst(annotatonPrefix, "").split(")")[0] }
        .map { it.trim(' ', '"') }


    // Append $KSCRIPT_KOTLIN_OPTS if defined in the parent environment
    System.getenv()["KSCRIPT_KOTLIN_OPTS"]?.run {
        kotlinOpts = kotlinOpts + this
    }

    return kotlinOpts.joinToString(" ")
}


//
// Entry directive
//


private val DEPS_COMMENT_PREFIX = "//DEPS "
private val DEPS_ANNOT_PREFIX = "^@file:DependsOn[(]".toRegex()
private val DEPSMAVEN_ANNOT_PREFIX = "^@file:DependsOnMaven[(]".toRegex()


private fun extractDependencies(line: String) = when {
    line.contains(DEPS_ANNOT_PREFIX) -> line
        .replaceFirst(DEPS_ANNOT_PREFIX, "")
        .split(")")[0].split(",")
        .map { it.trim(' ', '"') }

    line.contains(DEPSMAVEN_ANNOT_PREFIX) -> line
        .replaceFirst(DEPSMAVEN_ANNOT_PREFIX, "")
        .split(")")[0].trim(' ', '"').let { listOf(it) }

    line.startsWith(DEPS_COMMENT_PREFIX) ->
        line.split("[ ;,]+".toRegex()).drop(1).map(String::trim)

    else ->
        throw IllegalArgumentException("can not extract entry point from non-directive")
}


internal fun isDependDeclare(line: String) =
    line.startsWith(DEPS_COMMENT_PREFIX) || line.contains(DEPS_ANNOT_PREFIX) || line.contains(DEPSMAVEN_ANNOT_PREFIX)


fun collectDependencies(scriptText: List<String>): List<String> {
    val dependencies = scriptText.filter {
        isDependDeclare(it)
    }.flatMap {
        extractDependencies(it)
    }.toMutableList()


    // if annotations are used add dependency on kscript-annotations
    if (scriptText.any { isKscriptAnnotation(it) }) {
        dependencies += "com.github.holgerbrandl:kscript-annotations:1.1"
    }

    return dependencies.distinct()
}


//
// Entry directive
//

private val ENTRY_ANNOT_PREFIX = "^@file:EntryPoint[(]".toRegex()
private const val ENTRY_COMMENT_PREFIX = "//ENTRY "


internal fun isEntryPointDirective(line: String) =
    line.startsWith(ENTRY_COMMENT_PREFIX) || line.contains(ENTRY_ANNOT_PREFIX)


internal fun findEntryPoint(scriptText: List<String>): String? {
    return scriptText.find { isEntryPointDirective(it) }?.let { extractEntryPoint(it) }
}

private fun extractEntryPoint(line: String) = when {
    line.contains(ENTRY_ANNOT_PREFIX) ->
        line
            .replaceFirst(ENTRY_ANNOT_PREFIX, "")
            .split(")")[0].trim(' ', '"')
    line.startsWith(ENTRY_COMMENT_PREFIX) ->
        line.split("[ ]+".toRegex()).last()
    else ->
        throw IllegalArgumentException("can not extract entry point from non-directive")
}


/** Determine the latest version by checking github repo and print info if newer version is available. */
private fun versionCheck() {

    //    val latestVersion = fetchFromURL("https://git.io/v9R73")?.useLines {
    //    val kscriptRawReleaseURL= "https://git.io/v9R73"
    // todo use the actual kscript.app.Kscript.kt here to infer version
    val kscriptRawReleaseURL = "https://raw.githubusercontent.com/holgerbrandl/kscript/releases/kscript"

    val latestVersion = try {
        BufferedReader(InputStreamReader(URL(kscriptRawReleaseURL).openStream())).useLines {
            it.first { it.startsWith("KSCRIPT_VERSION") }.split("=")[1]
        }
    } catch (e: UnknownHostException) {
        return // skip version check here, since the use has no connection to the internet at the moment
    }

    fun padVersion(version: String) = java.lang.String.format("%03d%03d%03d", *version.split(".").map { Integer.valueOf(it) }.toTypedArray())

    if (padVersion(latestVersion) > padVersion(KSCRIPT_VERSION)) {
        info("""A new version (v${latestVersion}) of kscript is available. Use 'kscript --self-update' to update your local kscript installation""")
    }
}

fun prepareScript(scriptResource: String, enableSupportApi: Boolean): File {
    var scriptFile: File?

    // map script argument to script file
    scriptFile = with(File(scriptResource)) {
        if (!canRead()) {
            // not a file so let's keep the script-file undefined here
            null
        } else if (listOf("kts", "kt").contains(extension)) {
            // script input is a regular script or clas file
            this
        } else {
            // if we can "just" read from script resource create tmp file
            // i.e. script input is process substitution file handle
            // not FileInputStream(this).bufferedReader().use{ readText()} does not work nor does this.readText
            createTmpScript(FileInputStream(this).bufferedReader().readText())
        }
    }

    // support stdin
    if (scriptResource == "-" || scriptResource == "/dev/stdin") {
        val scriptText = generateSequence() { readLine() }.joinToString("\n").trim()
        scriptFile = createTmpScript(scriptText)
    }


    // Support URLs as script files
    if (scriptResource.startsWith("http://") || scriptResource.startsWith("https://")) {
        scriptFile = fetchFromURL(scriptResource)
    }


    // Support for support process substitution and direct script arguments
    if (scriptFile == null && !scriptResource.endsWith(".kts") && !scriptResource.endsWith(".kt")) {
        val scriptText = if (File(scriptResource).canRead()) {
            File(scriptResource).readText().trim()
        } else {
            // the last resort is to assume the input to be a kotlin program
            scriptResource.trim()
        }

        scriptFile = createTmpScript(scriptText)
    }

    // just proceed if the script file is a regular file at this point
    errorIf(scriptFile == null || !scriptFile.canRead()) {
        "Could not read script argument '$scriptResource'"
    }

    val extension = scriptFile!!.extension

    // note script file must be not null at this point

    // include preamble for custom interpreters (see https://github.com/holgerbrandl/kscript/issues/67)
    System.getenv("CUSTOM_KSCRIPT_PREAMBLE")?.let { interpPreamble ->
        scriptFile = Script(scriptFile!!).prependWith(interpPreamble).createTmpScript()
    }

    // prefix with text-processing preamble if kscript-support api is enabled
    if (enableSupportApi) {
        val textProcPreamble = """
            //DEPS com.github.holgerbrandl:kscript-support:1.2.4

            import kscript.text.*
            val lines = resolveArgFile(args)

            """.trimIndent()

        scriptFile = Script(scriptFile!!).prependWith(textProcPreamble).createTmpScript()
    }

    //    System.err.println("[kscript] temp script file is ${scriptFile}")
    //    System.err.println("[kscript] temp script file is \n${Script(scriptFile!!)}")

    // support //INCLUDE directive (see https://github.com/holgerbrandl/kscript/issues/34)
    scriptFile = resolveIncludes(scriptFile!!)

    return scriptFile!!
}
