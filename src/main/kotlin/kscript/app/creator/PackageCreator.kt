package kscript.app.creator

import kscript.app.code.GradleTemplates
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.util.Executor
import kscript.app.util.FileUtils
import kscript.app.util.Logger.infoMsg
import kscript.app.util.OsPath
import kscript.app.util.toNativeFile

class PackageCreator(private val executor: Executor) {
    /**
     * Create and use a temporary gradle project to package the compiled script using capsule.
     * See https://github.com/puniverse/capsule
     */
    fun packageKscript(basePath: OsPath, script: Script, jarArtifact: JarArtifact): OsPath {
        infoMsg("Packaging script '${script.scriptName}' into standalone executable...")

        // create exec_header to allow for direction execution (see http://www.capsule.io/user-guide/#really-executable-capsules)
        // from https://github.com/puniverse/capsule/blob/master/capsule-util/src/main/resources/capsule/execheader.sh
        FileUtils.createFile(basePath.resolve("exec_header.sh"), Templates.executeHeader)
        FileUtils.createFile(
            basePath.resolve("build.gradle.kts"), GradleTemplates.createGradlePackageScript(script, jarArtifact)
        )

        executor.createPackage(basePath)

        basePath.resolve("build/libs/appName").toNativeFile().setExecutable(true)

        infoMsg("Finished packaging '${script.scriptName}'; executable path: ${basePath}/build/libs/")

        return basePath
    }
}
