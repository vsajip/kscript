package kscript.app.creator

import kscript.app.appdir.FileItem
import kscript.app.appdir.ProjectCache
import kscript.app.appdir.ProjectItem
import kscript.app.appdir.SymLinkItem
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.model.ScriptType
import kscript.app.util.Logger.infoMsg
import kscript.app.util.ScriptUtils.dropExtension
import kscript.app.util.ShellUtils
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists

class IdeaProjectCreator(private val projectCache: ProjectCache) {

    fun create(script: Script, userArgs: List<String>): Path {
        val projectPath = projectCache.findOrCreate(script)

        if (hasIdeaFiles(projectPath)) {
            return projectPath
        }

        infoMsg("Setting up project...")

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

        infoMsg("Project set up at $projectPath")

        projectCache.addFiles(script, projectItems)

        return projectPath
    }

    private fun hasIdeaFiles(projectPath: Path) = projectPath.resolve("src").exists()
}
