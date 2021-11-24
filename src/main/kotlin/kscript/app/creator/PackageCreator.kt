package kscript.app.creator

import kscript.app.appdir.Cache
import kscript.app.code.GradleTemplates
import kscript.app.model.Script
import kscript.app.util.FileUtils
import kscript.app.util.Logger.infoMsg
import kscript.app.util.ScriptUtils.dropExtension
import java.io.File
import java.nio.file.Paths

class PackageCreator(private val cache: Cache, private val executor: Executor) {
    /**
     * Create and use a temporary gradle project to package the compiled script using capsule.
     * See https://github.com/puniverse/capsule
     */
    fun packageKscript(script: Script, jarArtifact: JarArtifact) {
        val appName = script.scriptName.dropExtension()

        infoMsg("Packaging script '$appName' into standalone executable...")

        val projectDir = cache.findOrCreatePackage(script.digest)

//        val jvmOptions =
//            script.kotlinOpts.map { it.value }
//                .filter { it.startsWith("-J") }
//                .map { it.removePrefix("-J") }
//                .joinToString(", ") { '"' + it + '"' }

        // https://shekhargulati.com/2015/09/10/gradle-tip-using-gradle-plugin-from-local-maven-repository/
//        val gradleScript = GradleTemplates.createGradlePackageScript(
//            script.repositories, script.dependencies, jarArtifact.path.toString(), // should be invariantSeparatorChars
//            jarArtifact.execClassName, appName, jvmOptions
//        )

        val pckgedJar = File(Paths.get("").toAbsolutePath().toFile(), appName).absoluteFile

        // create exec_header to allow for direction execution (see http://www.capsule.io/user-guide/#really-executable-capsules)
        // from https://github.com/puniverse/capsule/blob/master/capsule-util/src/main/resources/capsule/execheader.sh
        //FileUtils.createFile(projectDir.resolve("exec_header.sh"), Templates.executeHeader)

        FileUtils.createFile(projectDir.resolve("build.gradle.kts"), GradleTemplates.createGradlePackageScript(script, jarArtifact))

        executor.createPackage(projectDir)

        //pckgedJar.delete()
        //File(tmpProjectDir, "build/libs/${appName}").copyTo(pckgedJar, true).setExecutable(true)

        infoMsg("Finished packaging into $pckgedJar")
    }
}
