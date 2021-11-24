package kscript.app.creator

import kscript.app.appdir.Cache
import kscript.app.code.GradleTemplates
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.util.FileUtils
import kscript.app.util.Logger.infoMsg
import java.nio.file.Path
import kotlin.io.path.exists

class IdeaProjectCreator(private val cache: Cache) {

    fun create(script: Script, userArgs: List<String>): Path {
        val projectPath = cache.findOrCreateProject(script.digest)

        if (hasIdeaFiles(projectPath)) {
            return projectPath
        }

        infoMsg("Setting up idea project...")
        infoMsg("Files: ${script.scriptNodes.size}")

        for (scriptNode in script.scriptNodes) {
            val sourceUri = scriptNode.sourceUri
            val path = projectPath.resolve("src/${scriptNode.scriptName}.${scriptNode.scriptType.extension}")

            if (sourceUri == null) {
                infoMsg("Creating file: $path")
                FileUtils.createFile(path, scriptNode.sections.joinToString("\n") { it.code })
            } else {
                infoMsg("Creating symlink: $path")
                FileUtils.symLinkOrCopy(path, cache.readUri(sourceUri).path)
            }
        }

        FileUtils.createFile(
            projectPath.resolve(".idea/runConfigurations/Main.xml"),
            Templates.runConfig(script.rootNode.scriptName, userArgs)
        )

        FileUtils.createFile(
            projectPath.resolve("build.gradle.kts"), GradleTemplates.createGradleScript(script)
        )

        infoMsg("Project set up at $projectPath")

        return projectPath
    }

    private fun hasIdeaFiles(projectPath: Path) = projectPath.resolve("src").exists()
}
