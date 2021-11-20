package kscript.app

import kscript.app.appdir.AppDir
import kscript.app.code.Templates
import kscript.app.creator.IdeaProjectCreator
import kscript.app.creator.PackageCreator
import kscript.app.model.Config
import kscript.app.model.ScriptType
import kscript.app.model.ScriptSource
import kscript.app.parser.Parser
import kscript.app.resolver.ClasspathResolver
import kscript.app.resolver.DependencyResolver
import kscript.app.resolver.KotlinCommandResolver
import kscript.app.resolver.ScriptResolver
import kscript.app.util.Logger
import kscript.app.util.ShellUtils
import kscript.app.util.evalBash
import org.docopt.DocOptWrapper
import java.io.File

class KscriptHandler(private val config: Config, private val docopt: DocOptWrapper) {

    @OptIn(ExperimentalStdlibApi::class)
    fun handle(userArgs: List<String>) {
        Logger.silentMode = docopt.getBoolean("silent")

        // create kscript dir if it does not yet exist
        val appDir = AppDir(config.kscriptDir)

        // optionally clear up the jar cache
        if (docopt.getBoolean("clear-cache")) {
            Logger.info("Cleaning up cache...")
            appDir.clear()
            return
        }

        val enableSupportApi = docopt.getBoolean("text")

        val preambles = buildList {
            if (enableSupportApi) {
                add(Templates.textProcessingPreamble)
            }

            add(config.customPreamble)
        }

        val scriptResolver = ScriptResolver(Parser(), appDir, config)

        if (docopt.getBoolean("add-bootstrap-header")) {
            val script= scriptResolver.resolveFromInput(
                docopt.getString("script"),
                maxResolutionLevel = 0
            )

            if (script.scriptSource != ScriptSource.FILE) {
                throw IllegalStateException("Can not add bootstrap header to resources, which are not regular Kotlin files.")
            }

            val scriptLines = script.rootNode.sections.map { it.code }.dropWhile {
                it.startsWith("#!/") && it != "#!/bin/bash"
            }

            val bootstrapHeader = Templates.bootstrapHeader.lines()

            if (scriptLines.getOrNull(0) == bootstrapHeader[0] && scriptLines.any { "command -v kscript >/dev/null 2>&1 || " in it }) {
                val lastHeaderLine = bootstrapHeader.findLast { it.isNotBlank() }!!
                val preexistingHeader = scriptLines.dropLastWhile { it != lastHeaderLine }.joinToString("\n")
                throw IllegalStateException("Bootstrap header already detected:\n\n$preexistingHeader\n\nYou can remove it to force the re-generation")
            }

            File(script.sourceUri!!).writeText((bootstrapHeader + scriptLines).joinToString("\n"))
            Logger.infoMsg("${script.sourceUri} updated")
            return
        }

        val script = scriptResolver.resolveFromInput(docopt.getString("script"), preambles)
        val projectDir = appDir.projectCache.projectDir(script.code)

        //  Create temporary dev environment
        if (docopt.getBoolean("idea")) {
            val ideaProjectCreator = IdeaProjectCreator(config, appDir)
            println(ideaProjectCreator.createProject(script, userArgs))
            return
        }

        val dependencyResolver = DependencyResolver(script.repositories)
        val classpathResolver = ClasspathResolver(config.classPathSeparator, appDir, dependencyResolver)
        val kotlinCommandResolver = KotlinCommandResolver(config, script, classpathResolver)

        val classpath = classpathResolver.resolve(script.dependencies)

        val optionalCpArg = if (classpath.isNotEmpty()) "-classpath '${classpath}'" else ""

        //  Optionally enter interactive mode
        if (docopt.getBoolean("interactive")) {
            Logger.infoMsg("Creating REPL from ${script.scriptName}")
            println("kotlinc ${script.compilerOpts} ${script.kotlinOpts} $optionalCpArg")
            return
        }

        // Even if we just need and support the //ENTRY directive in case of kt-class
        // files, we extract it here to fail if it was used in kts files.
        val entryDirective = script.entryPoint

        if (entryDirective != null && script.scriptType == ScriptType.KTS) {
            throw IllegalStateException("@Entry directive is just supported for kt class files")
        }

        // Capitalize first letter and get rid of dashes (since this is what kotlin compiler is doing for the wrapper to create a valid java class name)
        // For valid characters see https://stackoverflow.com/questions/4814040/allowed-characters-in-filename
        val className = script.scriptName.replace("[^A-Za-z0-9]".toRegex(), "_").replaceFirstChar { it.titlecase() }
            // also make sure that it is a valid identifier by avoiding an initial digit (to stay in sync with what the kotlin script compiler will do as well)
            .let { if ("^[0-9]".toRegex().containsMatchIn(it)) "_$it" else it }


        val scriptFile = File(projectDir, className + script.scriptType.extension)
        scriptFile.writeText(script.code)

        // Define the entrypoint for the scriptlet jar
        val packageName = if (script.packageName != null) script.packageName.value + "." else ""
        val execClassName = if (script.scriptType == ScriptType.KTS) {
            "Main_${className}"
        } else {
            // extract package from kt-file
            """${packageName}${entryDirective?.value ?: "${className}Kt"}"""
        }

        val jarFile = projectDir.resolve("jar/scriplet.jar")

        if (!jarFile.isFile) {
            if (!ShellUtils.isInPath("kotlinc")) {
                throw IllegalStateException("${"kotlinc"} is not in PATH")
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
            val compilerOpts = script.compilerOpts.joinToString(" ") { it.value }

            val scriptCompileResult =
                evalBash("kotlinc $compilerOpts $optionalCpArg -d '${jarFile.absolutePath}' $fileArguments")

            if (scriptCompileResult.exitCode != 0) {
                throw IllegalStateException("compilation of '${scriptFile.name}' failed\n$scriptCompileResult.stderr")
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

            PackageCreator(appDir).packageKscript(script, jarFile, execClassName, binaryName)
            return
        }

        if (config.kotlinHome == null) {
            throw IllegalStateException("KOTLIN_HOME is not set and could not be inferred from context")
        }

        var extClassPath =
            "${jarFile}${config.classPathSeparator}${config.kotlinHome}${File.separatorChar}lib${File.separatorChar}kotlin-script-runtime.jar"

        if (classpath.isNotEmpty()) extClassPath += config.classPathSeparator + classpath

        val kotlinOpts = script.kotlinOpts.joinToString(" ") { it.value }

        val kotlinCommandString = "kotlin $kotlinOpts -classpath \"$extClassPath\" $execClassName $joinedUserArgs"

        println(kotlinCommandString)
    }
}
