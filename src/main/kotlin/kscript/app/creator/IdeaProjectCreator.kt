package kscript.app.creator

import kscript.app.appdir.Cache
import kscript.app.code.GradleTemplates
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.util.FileUtils
import kscript.app.util.Logger.devMsg
import kscript.app.util.Logger.infoMsg
import java.nio.file.Path
import kotlin.io.path.exists

class IdeaProjectCreator(private val cache: Cache) {

    fun create(script: Script, userArgs: List<String>): Path {
        val projectPath = cache.findOrCreateIdea(script.digest)

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
                devMsg("link: $path, target: ${cache.readUri(sourceUri).path}")
                FileUtils.symLinkOrCopy(path, cache.readUri(sourceUri).path)
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
