package kscript.app

import kscript.app.ShellUtils.isInPath
import kscript.app.appdir.AppDir
import kscript.app.code.Templates
import kscript.app.model.Config
import kscript.app.model.ResolvedScript
import kscript.app.util.Logger.errorMsg
import kscript.app.util.Logger.infoMsg
import java.io.File
import java.io.IOException
import java.nio.file.Files

class IdeaProjectCreator(private val appDir: AppDir) {

    fun createProject(scriptFile: File, resolvedScript: ResolvedScript, userArgs: List<String>, config: Config): String {
        if (!isInPath(config.intellijCommand)) {
            errorMsg("Could not find '${config.intellijCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_IDEA_COMMAND' env property")
            quit(1)
        }

        infoMsg("Setting up idea project...")

        val tmpProjectDir = appDir.projectCache.projectDir()

        //Symlink script resource in
        File(tmpProjectDir, "src").run {
            mkdir()
            createSymLink(File(this, scriptFile.name), scriptFile)
        }

        File(tmpProjectDir, ".idea/runConfigurations/Main.xml").writeText(
            Templates.runConfig(scriptFile, tmpProjectDir, userArgs)
        )

        val opts = resolvedScript.compilerOpts.toList()

        var jvmTargetOption: String? = null
        for (i in opts.indices) {
            if (i > 0 && opts[i - 1] == "-jvm-target") {
                jvmTargetOption = opts[i]
            }
        }

        val kotlinOptions = Templates.kotlinOptions(jvmTargetOption)
        val gradleScript =
            Templates.createGradleIdeaScript(resolvedScript.repositories, resolvedScript.dependencies, kotlinOptions)

        File(tmpProjectDir, "build.gradle.kts").writeText(gradleScript)

        val projectPath = tmpProjectDir.absolutePath

        // Create gradle wrapper
        if (!isInPath(config.gradleCommand)) {
            errorMsg(
                "Could not find '${config.gradleCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_GRADLE_COMMAND' env property"
            )
            quit(1)
        }

        runProcess("${config.gradleCommand} wrapper", wd = tmpProjectDir)

        infoMsg("Project set up at $projectPath")

        return "${config.intellijCommand} \"$projectPath\""
    }

    private fun createSymLink(link: File, target: File) {
        try {
            Files.createSymbolicLink(link.toPath(), target.absoluteFile.toPath())
        } catch (e: IOException) {
            errorMsg("Failed to create symbolic link to script. Copying instead...")
            target.copyTo(link)
        }
    }
}
