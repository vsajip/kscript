package kscript.app.creator

import kscript.app.appdir.FileItem
import kscript.app.appdir.IdeaCache
import kscript.app.appdir.ProjectItem
import kscript.app.appdir.SymLinkItem
import kscript.app.code.Templates
import kscript.app.model.Config
import kscript.app.model.Script
import kscript.app.util.Logger.infoMsg
import kscript.app.util.ProcessRunner.runProcess
import kscript.app.util.ShellUtils
import java.nio.file.Path

class IdeaProjectCreator(private val config: Config, private val ideaCache: IdeaCache) {
    fun createProject(script: Script, userArgs: List<String>): String {
        if (!ShellUtils.isInPath(config.intellijCommand)) {
            throw IllegalStateException("Could not find '${config.intellijCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_IDEA_COMMAND' env property")
        }

        if (!ShellUtils.isInPath(config.gradleCommand)) {
            throw IllegalStateException(
                "Could not find '${config.gradleCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_GRADLE_COMMAND' env property"
            )
        }

        var ideaPath = ideaCache.ideaPath(script.resolvedCode)
        if ( ideaPath != null) {
            return createCommand(ideaPath)
        }

        infoMsg("Setting up idea project...")

        val projectItems = mutableListOf<ProjectItem>()
        for (scriptNode in script.scriptNodes) {
            val sourceUri = scriptNode.sourceUri
            val path = "src/${scriptNode.scriptName}"

            if (sourceUri == null) {
                projectItems.add(FileItem(path, scriptNode.sections.joinToString("\n") { it.code }))
            } else {
                projectItems.add(SymLinkItem(path, sourceUri))
            }
        }

        projectItems.add(
            FileItem(
                ".idea/runConfigurations/Main.xml", Templates.runConfig(script.rootNode.scriptName, userArgs)
            )
        )

        val opts = script.compilerOpts.map { it.value }

        var jvmTargetOption: String? = null
        for (i in opts.indices) {
            if (i > 0 && opts[i - 1] == "-jvm-target") {
                jvmTargetOption = opts[i]
            }
        }

        val kotlinOptions = Templates.kotlinOptions(jvmTargetOption)
        val gradleScript = Templates.createGradleIdeaScript(
            script.repositories, script.dependencies, kotlinOptions
        )

        projectItems.add(FileItem("build.gradle.kts", gradleScript))

        ideaPath = ideaCache.ideaDir(script.resolvedCode, projectItems)

        // Create gradle wrapper
        runProcess("${config.gradleCommand} wrapper", wd = ideaPath.toFile())

        infoMsg("Project set up at $ideaPath")

        return createCommand(ideaPath)
    }

    private fun createCommand(ideaPath: Path) = "${config.intellijCommand} \"$ideaPath\""
}
