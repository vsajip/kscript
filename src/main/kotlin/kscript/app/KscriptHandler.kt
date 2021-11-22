package kscript.app

import kscript.app.appdir.AppDir
import kscript.app.code.Templates
import kscript.app.creator.IdeaProjectCreator
import kscript.app.creator.JarCreator
import kscript.app.creator.PackageCreator
import kscript.app.model.Config
import kscript.app.model.ScriptSource
import kscript.app.model.ScriptType
import kscript.app.parser.Parser
import kscript.app.resolver.CommandResolver
import kscript.app.resolver.DependencyResolver
import kscript.app.resolver.ScriptResolver
import kscript.app.resolver.SectionResolver
import kscript.app.util.Logger
import kscript.app.util.Logger.infoMsg
import kscript.app.util.ProcessRunner
import kscript.app.util.ShellUtils
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
            appDir.clearCaches()
            return
        }

        val enableSupportApi = docopt.getBoolean("text")

        val preambles = buildList {
            if (enableSupportApi) {
                add(Templates.textProcessingPreamble)
            }

            add(config.customPreamble)
        }

        val sectionResolver = SectionResolver(Parser(), appDir.uriCache, config)
        val scriptResolver = ScriptResolver(sectionResolver, appDir.uriCache, config.kotlinOptsEnvVariable)

        if (docopt.getBoolean("add-bootstrap-header")) {
            val script = scriptResolver.resolve(
                docopt.getString("script"), maxResolutionLevel = 0
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
            infoMsg("${script.sourceUri} updated")
            return
        }

        val script = scriptResolver.resolve(docopt.getString("script"), preambles)
        val resolvedDependencies = DependencyResolver(script.repositories).resolve(script.dependencies)
        val commandResolver = CommandResolver(config, script)

        //  Create temporary dev environment
        if (docopt.getBoolean("idea")) {
            if (!ShellUtils.isInPath(config.intellijCommand)) {
                throw IllegalStateException("Could not find '${config.intellijCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_IDEA_COMMAND' env property")
            }

            if (!ShellUtils.isInPath(config.gradleCommand)) {
                throw IllegalStateException(
                    "Could not find '${config.gradleCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_GRADLE_COMMAND' env property"
                )
            }

            val projectPath = IdeaProjectCreator(appDir.projectCache).create(script, userArgs)

            // Create gradle wrapper
            ProcessRunner.runProcess("${config.gradleCommand} wrapper", wd = projectPath.toFile())

            val command = commandResolver.executeIdea(projectPath)
            infoMsg("Execute idea: $command")
            println(command)
            return
        }

        //  Optionally enter interactive mode
        if (docopt.getBoolean("interactive")) {
            infoMsg("Creating REPL from ${script.scriptName}")
            val command = commandResolver.interactiveRepl(resolvedDependencies)
            infoMsg("REPL command: $command")
            println(command)
            return
        }

        // Even if we just need and support the //ENTRY directive in case of kt-class
        // files, we extract it here to fail if it was used in kts files.
        if (script.entryPoint != null && script.scriptType == ScriptType.KTS) {
            throw IllegalStateException("@Entry directive is just supported for kt class files")
        }

        val jar = JarCreator(appDir.projectCache, commandResolver).create(script, resolvedDependencies)

        //if requested try to package the into a standalone binary
        if (docopt.getBoolean("package")) {
            PackageCreator(appDir.projectCache, config).packageKscript(script, jar)
            return
        }

        if (config.kotlinHome == null) {
            throw IllegalStateException("KOTLIN_HOME is not set and could not be inferred from context")
        }

        val command = commandResolver.executeKotlin(jar, resolvedDependencies, userArgs)
        infoMsg("Execute command: $command")
        println(command)
    }
}
