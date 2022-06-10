package kscript.app.creator

import kscript.app.code.GradleTemplates
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.util.Executor
import kscript.app.util.FileUtils
import kscript.app.util.Logger.infoMsg
import kscript.app.util.ScriptUtils.dropExtension
import java.nio.file.Path
import java.nio.file.Paths
import java.io.File

class PackageCreator(private val executor: Executor) {
    /**
     * Create and use a temporary gradle project to package the compiled script using capsule.
     * See https://github.com/puniverse/capsule
     */
    fun packageKscript(basePath: Path, script: Script, jarArtifact: JarArtifact): Path {
        val baseName = script.scriptName.dropExtension()

        infoMsg("Packaging script '${script.scriptName}' into standalone executable...")

        // create exec_header to allow for direction execution (see http://www.capsule.io/user-guide/#really-executable-capsules)
        // from https://github.com/puniverse/capsule/blob/master/capsule-util/src/main/resources/capsule/execheader.sh
        var hdr = Templates.executeHeader
        val parts = mutableListOf("")

        for (opt in script.kotlinOpts) {
            val s = opt.value
            if (s.startsWith("-J")) {
                parts.add(s.substring(2))
            }
        }
        val opts = parts.joinToString(" ").trim()
        hdr = hdr.replace("java -jar", "java $opts -jar")
        FileUtils.createFile(basePath.resolve("exec_header.sh"), hdr)
        FileUtils.createFile(
            basePath.resolve("build.gradle.kts"), GradleTemplates.createGradlePackageScript(script, jarArtifact)
        )

        executor.createPackage(basePath)

        val ef = basePath.resolve("build/libs/$baseName").toFile()
        ef.setExecutable(true)

        infoMsg("Finished packaging '${script.scriptName}'; executable path: ${basePath}/build/libs/$baseName")

        // The tests seem to expect the end product to be in the current directory. Let's copy it for now.
        val wd = Paths.get(File("").getAbsolutePath()).resolve(baseName).toFile()
        infoMsg("Copied $ef to $wd")
        ef.copyTo(wd)
        wd.setExecutable(true)

        return basePath
    }
}
