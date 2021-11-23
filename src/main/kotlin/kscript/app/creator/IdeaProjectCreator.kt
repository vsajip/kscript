package kscript.app.creator

import kscript.app.appdir.ProjectCache
import kscript.app.appdir.UriCache
import kscript.app.code.GradleTemplates
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.util.FileUtils
import kscript.app.util.Logger.infoMsg
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class IdeaProjectCreator(private val projectCache: ProjectCache, private val uriCache: UriCache) {

    fun create(script: Script, userArgs: List<String>): Path {
        val projectPath = projectCache.findOrCreate(script)

        if (hasIdeaFiles(projectPath)) {
            return projectPath
        }

        infoMsg("Setting up idea project...")

        for (scriptNode in script.scriptNodes) {
            val sourceUri = scriptNode.sourceUri
            val path = projectPath.resolve("src/${scriptNode.scriptName}")

            if (sourceUri == null) {
                FileUtils.createFile(path, scriptNode.sections.joinToString("\n") { it.code })
            } else {
                FileUtils.symLinkOrCopy(path, Paths.get(uriCache.readUri(sourceUri).uri))
            }
        }

        FileUtils.createFile(
            projectPath.resolve(".idea/runConfigurations/Main.xml"),
            Templates.runConfig(script.rootNode.scriptName, userArgs)
        )

        FileUtils.createFile(
            projectPath.resolve("build.gradle.kts"), GradleTemplates.createGradleIdeaScript(script)
        )

        infoMsg("Project set up at $projectPath")

        return projectPath
    }

    private fun hasIdeaFiles(projectPath: Path) = projectPath.resolve("src").exists()
}
