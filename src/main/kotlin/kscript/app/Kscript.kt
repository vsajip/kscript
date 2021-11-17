package kscript.app

import kscript.app.util.ShellUtils.isInPath
import kscript.app.appdir.AppDir
import kscript.app.code.Templates
import kscript.app.creator.IdeaProjectCreator
import kscript.app.creator.PackageCreator
import kscript.app.model.Config
import kscript.app.model.ScriptType
import kscript.app.model.SourceType
import kscript.app.parser.Parser
import kscript.app.resolver.DependencyResolver
import kscript.app.resolver.ScriptResolver
import kscript.app.util.Logger
import kscript.app.util.Logger.errorMsg
import kscript.app.util.Logger.info
import kscript.app.util.Logger.infoMsg
import kscript.app.util.VersionChecker.versionCheck
import kscript.app.util.evalBash
import kscript.app.util.guessKotlinHome
import kscript.app.util.quit
import org.docopt.DocOptWrapper
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess


/**
 * A kscript - Scripting enhancements for Kotlin
 *
 * For details and license see https://github.com/holgerbrandl/kscript
 *
 * @author Holger Brandl
 * @author Marcin Kuszczak
 */

const val KSCRIPT_VERSION = "3.1.0"

@OptIn(ExperimentalStdlibApi::class)
fun main(args: Array<String>) {
    val config = Config(
        System.getenv("CUSTOM_KSCRIPT_NAME") ?: "kscript",
        Paths.get(System.getenv("KSCRIPT_DIR") ?: (System.getProperty("user.home")!! + "/.kscript")),
        System.getenv("CUSTOM_KSCRIPT_PREAMBLE") ?: "",
        System.getenv("KSCRIPT_IDEA_COMMAND") ?: "idea",
        System.getenv("KSCRIPT_GRADLE_COMMAND") ?: "gradle",
        (System.getenv("KOTLIN_HOME") ?: guessKotlinHome())?.let { Paths.get(it) },
        if (System.getProperty("os.name").lowercase().contains("windows")) ";" else ":"
    )

    // skip org.docopt for version and help to allow for lazy version-check
    val usage = Templates.usage(config.selfName, KSCRIPT_VERSION)

    if (args.size == 1 && listOf("--help", "-h", "--version", "-v").contains(args[0])) {
        info(usage)
        versionCheck(KSCRIPT_VERSION)
        val systemInfo = evalBash("kotlin -version").stdout
        info("Kotlin    : " + systemInfo.split('(')[0].removePrefix("Kotlin version").trim())
        info("Java      : " + systemInfo.split('(')[1].split('-', ')')[0].trim())
        quit(0)
    }

    // note: with current implementation we still don't support `kscript -1` where "-1" is a valid kotlin expression
    val userArgs = args.dropWhile { it.startsWith("-") && it != "-" }.drop(1)
    val kscriptArgs = args.take(args.size - userArgs.size)

    val docopt = DocOptWrapper(kscriptArgs, usage)

    Logger.silentMode = docopt.getBoolean("silent")

    // create kscript dir if it does not yet exist
    val appDir = AppDir(config.kscriptDir)

    // optionally clear up the jar cache
    if (docopt.getBoolean("clear-cache")) {
        info("Cleaning up cache...")
        appDir.clear()
        quit(0)
    }

    val enableSupportApi = docopt.getBoolean("text")

    val preambles = buildList {
        if (enableSupportApi) {
            add(Templates.textProcessingPreamble)
        }

        add(config.customPreamble)
    }

    val scriptResolver = ScriptResolver(Parser(), appDir)
    val script = scriptResolver.createFromInput(docopt.getString("script"), preambles)

    if (docopt.getBoolean("add-bootstrap-header")) {
        if (script.sourceType != SourceType.FILE) {
            errorMsg("Can not add bootstrap header to resources, which are not regular Kotlin files.")
            quit(1)
        }

        val scriptLines = script.sections.map { it.code }.dropWhile {
            it.startsWith("#!/") && it != "#!/bin/bash"
        }

        val bootstrapHeader = Templates.bootstrapHeader.lines()

        if (scriptLines.getOrNull(0) == bootstrapHeader[0] && scriptLines.any { "command -v kscript >/dev/null 2>&1 || " in it }) {
            val lastHeaderLine = bootstrapHeader.findLast { it.isNotBlank() }!!
            val preexistingHeader = scriptLines.dropLastWhile { it != lastHeaderLine }.joinToString("\n")
            errorMsg("Bootstrap header already detected:\n\n$preexistingHeader\n\nYou can remove it to force the re-generation")
            quit(1)
        }

        File(script.sourceUri!!).writeText((bootstrapHeader + scriptLines).joinToString("\n"))
        infoMsg("${script.sourceUri} updated")
        quit(0)
    }

    val resolvedScript = scriptResolver.resolve(script)
    val projectDir = appDir.projectCache.projectDir(resolvedScript.code)

    //  Create temporary dev environment
    if (docopt.getBoolean("idea")) {
        val scriptFile = appDir.urlCache.scriplet(resolvedScript.code, script.scriptType.extension).toFile()
        val ideaProjectCreator = IdeaProjectCreator(appDir)
        println(ideaProjectCreator.createProject(scriptFile, resolvedScript, userArgs, config))
        exitProcess(0)
    }

    val classpath = try {
        DependencyResolver(config, appDir).resolveClasspath(resolvedScript.dependencies, resolvedScript.repositories)
    } catch (e: Exception) {
        // Probably a wrapped Nullpointer from 'DefaultRepositorySystem.resolveDependencies()', this however is probably a connection problem.
        errorMsg("Failed while connecting to the server. Check the connection (http/https, port, proxy, credentials, etc.) of your maven dependency locators. If you suspect this is a bug, you can create an issue on https://github.com/holgerbrandl/kscript")
        errorMsg("Exception: $e")
        quit(1)
    }

    val optionalCpArg = if (classpath.isNotEmpty()) "-classpath '${classpath}'" else ""

    //  Optionally enter interactive mode
    if (docopt.getBoolean("interactive")) {
        val scriptFile = appDir.urlCache.scriplet(resolvedScript.code, script.scriptType.extension).toFile()
        infoMsg("Creating REPL from $scriptFile")
        println("kotlinc ${resolvedScript.compilerOpts} ${resolvedScript.kotlinOpts} $optionalCpArg")

        exitProcess(0)
    }

    // Even if we just need and support the //ENTRY directive in case of kt-class
    // files, we extract it here to fail if it was used in kts files.
    val entryDirective = resolvedScript.entryPoint

    if (entryDirective != null && script.scriptType == ScriptType.KTS) {
        errorMsg("@Entry directive is just supported for kt class files")
        quit(1)
    }

    // Capitalize first letter and get rid of dashes (since this is what kotlin compiler is doing for the wrapper to create a valid java class name)
    // For valid characters see https://stackoverflow.com/questions/4814040/allowed-characters-in-filename
    val className = script.scriptName.replace("[^A-Za-z0-9]".toRegex(), "_").replaceFirstChar { it.titlecase() }
        // also make sure that it is a valid identifier by avoiding an initial digit (to stay in sync with what the kotlin script compiler will do as well)
        .let { if ("^[0-9]".toRegex().containsMatchIn(it)) "_$it" else it }


    val scriptFile = File(projectDir, className + script.scriptType.extension)
    scriptFile.writeText(resolvedScript.code)

    // Define the entrypoint for the scriptlet jar
    val packageName = if (resolvedScript.packageName != null) resolvedScript.packageName + "." else ""
    val execClassName = if (script.scriptType == ScriptType.KTS) {
        "Main_${className}"
    } else {
        // extract package from kt-file
        """${packageName}${entryDirective ?: "${className}Kt"}"""
    }

    val jarFile = projectDir.resolve("jar/scriplet.jar")

    if (!jarFile.isFile) {
        if (!isInPath("kotlinc")) {
            errorMsg("${"kotlinc"} is not in PATH")
            quit(1)
        }

        val filesToCompile = mutableListOf<File>()
        filesToCompile.add(scriptFile)

        // create main-wrapper for kts scripts
        if (script.scriptType == ScriptType.KTS) {
            val classReference = packageName + className

            val code = """
            class Main_${className}{
                companion object {
                    @JvmStatic
                    fun main(args: Array<String>) {
                        val script = Main_${className}::class.java.classLoader.loadClass("$classReference")
                        script.getDeclaredConstructor(Array<String>::class.java).newInstance(args);
                    }
                }
            }
            """.trimIndent()

            val mainKotlin = File(projectDir, "$execClassName.kt")
            mainKotlin.writeText(code)
            filesToCompile.add(mainKotlin)
        }

        val fileArguments = filesToCompile.joinToString(" ") { "'${it.absolutePath}'" }
        val compilerOpts = resolvedScript.compilerOpts.joinToString(" ")

        val scriptCompileResult =
            evalBash("kotlinc $compilerOpts $optionalCpArg -d '${jarFile.absolutePath}' $fileArguments")

        if (scriptCompileResult.exitCode != 0) {
            errorMsg("compilation of '${scriptFile.name}' failed\n$scriptCompileResult.stderr")
            quit(1)
        }
    }

    // print the final command to be run by eval+exec
    val joinedUserArgs = userArgs.joinToString(" ") { "\"${it.replace("\"", "\\\"")}\"" }

    //if requested try to package the into a standalone binary
    if (docopt.getBoolean("package")) {
        val binaryName = if (scriptFile.run { canRead() && listOf("kts", "kt").contains(extension) }) {
            scriptFile.nameWithoutExtension
        } else {
            "k" + scriptFile.nameWithoutExtension
        }

        PackageCreator(appDir).packageKscript(resolvedScript, jarFile, execClassName, binaryName)

        quit(0)
    }

    if (config.kotlinHome == null) {
        errorMsg("KOTLIN_HOME is not set and could not be inferred from context")
        quit(1)
    }

    var extClassPath =
        "${jarFile}${config.classPathSeparator}${config.kotlinHome}${File.separatorChar}lib${File.separatorChar}kotlin-script-runtime.jar"

    if (classpath.isNotEmpty()) extClassPath += config.classPathSeparator + classpath

    val kotlinOpts = resolvedScript.kotlinOpts.joinToString(" ")

    val kotlinCommand = "kotlin $kotlinOpts -classpath \"$extClassPath\" $execClassName $joinedUserArgs"
    infoMsg(kotlinCommand)

    println(kotlinCommand)
}
