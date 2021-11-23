package kscript.app.creator

import kscript.app.appdir.ProjectCache
import kscript.app.appdir.UriCache
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.util.Logger
import kscript.app.util.Logger.infoMsg
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class IdeaProjectCreator(private val projectCache: ProjectCache, private val uriCache: UriCache) {

    fun create(script: Script, userArgs: List<String>): Path {
        val projectPath = projectCache.findOrCreate(script)

        if (hasIdeaFiles(projectPath)) {
            return projectPath
        }

        infoMsg("Setting up project...")

        for (scriptNode in script.scriptNodes) {
            val sourceUri = scriptNode.sourceUri
            val path = "src/${scriptNode.scriptName}"

            if (sourceUri == null) {
                projectPath.resolve(path).toFile().writeText(scriptNode.sections.joinToString("\n") { it.code })
            } else {
                val file = projectPath.resolve(path).toFile()
                val target = Paths.get(uriCache.readUri(sourceUri).uri).toFile()
                val isSymlinked = createSymLink(file, target)

                if (!isSymlinked) {
                    Logger.warnMsg("Failed to create symbolic link to script. Copying instead...")
                    target.copyTo(file)
                }
            }
        }

        projectPath.resolve(".idea/runConfigurations/Main.xml")
            .toFile()
            .writeText(Templates.runConfig(script.rootNode.scriptName, userArgs))

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

        projectPath.resolve("build.gradle.kts").toFile().writeText(gradleScript)

        infoMsg("Project set up at $projectPath")

        return projectPath
    }

    private fun hasIdeaFiles(projectPath: Path) = projectPath.resolve("src").exists()

    private fun createSymLink(link: File, target: File): Boolean {
        return try {
            Files.createSymbolicLink(link.toPath(), target.absoluteFile.toPath())
            true
        } catch (e: IOException) {
            false
        }
    }
}
