package kscript.app

import kscript.app.appdir.AppDir
import kscript.app.code.Templates
import kscript.app.creator.*
import kscript.app.model.Config
import kscript.app.model.ScriptType
import kscript.app.parser.Parser
import kscript.app.resolver.*
import kscript.app.util.Logger
import org.docopt.DocOptWrapper

class KscriptHandler(private val config: Config, private val docopt: DocOptWrapper) {

    @OptIn(ExperimentalStdlibApi::class)
    fun handle(userArgs: List<String>) {
        Logger.silentMode = docopt.getBoolean("silent")
        Logger.devMode = docopt.getBoolean("development")

        // create kscript dir if it does not yet exist
        val appDir = AppDir(config.kscriptDir)

        // optionally clear up the jar cache
        if (docopt.getBoolean("clear-cache")) {
            Logger.info("Cleaning up cache...")
            appDir.clearCache()
            return
        }

        val enableSupportApi = docopt.getBoolean("text")

        val preambles = buildList {
            if (enableSupportApi) {
                add(Templates.textProcessingPreamble)
            }

            add(config.customPreamble)
        }

        val sectionResolver = SectionResolver(Parser(), appDir.cache, config)
        val scriptResolver = ScriptResolver(sectionResolver, appDir.cache, config.kotlinOptsEnvVariable)

        if (docopt.getBoolean("add-bootstrap-header")) {
            val script = scriptResolver.resolve(docopt.getString("script"), maxResolutionLevel = 0)
            BootstrapCreator().create(script)
            return
        }

        val script = scriptResolver.resolve(docopt.getString("script"), preambles)
        val resolvedDependencies = DependencyResolver(script.repositories).resolve(script.dependencies)
        val executor = Executor(CommandResolver(config, script), config)

        //  Create temporary dev environment
        if (docopt.getBoolean("idea")) {
            val projectPath = IdeaProjectCreator(appDir.cache).create(script, userArgs)
            executor.runIdea(projectPath)
            return
        }

        //  Optionally enter interactive mode
        if (docopt.getBoolean("interactive")) {
            executor.runInteractiveRepl(resolvedDependencies)
            return
        }

        // Even if we just need and support the //ENTRY directive in case of kt-class
        // files, we extract it here to fail if it was used in kts files.
        if (script.entryPoint != null && script.scriptType == ScriptType.KTS) {
            throw IllegalStateException("@Entry directive is just supported for kt class files")
        }

        val jar = JarCreator(appDir.cache, executor).create(script, resolvedDependencies)

        //if requested try to package the into a standalone binary
        if (docopt.getBoolean("package")) {
            PackageCreator(appDir.cache, executor).packageKscript(script, jar)
            return
        }

        executor.executeKotlin(jar, resolvedDependencies, userArgs)
    }
}
