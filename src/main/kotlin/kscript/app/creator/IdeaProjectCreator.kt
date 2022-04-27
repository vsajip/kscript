package kscript.app.creator

import kscript.app.code.GradleTemplates
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.util.FileUtils
import kscript.app.util.Logger.devMsg
import kscript.app.util.Logger.infoMsg
import java.net.URI
import java.nio.file.Path

class IdeaProjectCreator {
    fun create(basePath: Path, script: Script, userArgs: List<String>, uriLocalPathProvider: (URI) -> Path): Path {
        infoMsg("Setting up idea project...")

        for (scriptNode in script.scriptNodes) {
            val sourceUri = scriptNode.sourceUri
            val path = basePath.resolve("src/${scriptNode.scriptName}")

            if (sourceUri == null) {
                FileUtils.createFile(path, scriptNode.sections.joinToString("\n") { it.code })
            } else {
                val targetPath = uriLocalPathProvider(sourceUri)
                devMsg("link: $path, target: $targetPath")
                FileUtils.symLinkOrCopy(path, targetPath)
            }
        }

        FileUtils.createFile(
            basePath.resolve(".idea/runConfigurations/Main.xml"),
            Templates.runConfig(script.rootNode.scriptName, userArgs)
        )

        FileUtils.createFile(
            basePath.resolve("build.gradle.kts"), GradleTemplates.createGradleIdeaScript(script)
        )

        return basePath
    }
}
