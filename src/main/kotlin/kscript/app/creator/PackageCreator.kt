package kscript.app.creator

import kscript.app.appdir.ProjectCache
import kscript.app.code.GradleTemplates
import kscript.app.code.Templates
import kscript.app.model.Config
import kscript.app.model.Script
import kscript.app.util.Logger.infoMsg
import kscript.app.util.ScriptUtils.dropExtension
import kscript.app.util.ShellUtils.evalBash
import kscript.app.util.ShellUtils.isInPath
import java.io.File
import java.nio.file.Paths

class PackageCreator(private val projectCache: ProjectCache, private val config: Config) {
    /**
     * Create and use a temporary gradle project to package the compiled script using capsule.
     * See https://github.com/puniverse/capsule
     */
    fun packageKscript(script: Script, jarArtifact: JarArtifact) {
        if (!isInPath(config.gradleCommand)) {
            throw IllegalStateException("gradle is required to package kscripts")
        }

        val appName = script.scriptName.dropExtension()


        infoMsg("Packaging script '$appName' into standalone executable...")

        val tmpProjectDir = projectCache.findOrCreate(script).toFile()

        val jvmOptions = script.kotlinOpts.map { it.value }.filter { it.startsWith("-J") }.map { it.removePrefix("-J") }
            .joinToString(", ") { '"' + it + '"' }

        // https://shekhargulati.com/2015/09/10/gradle-tip-using-gradle-plugin-from-local-maven-repository/
        val gradleScript = GradleTemplates.createGradlePackageScript(
            script.repositories,
            script.dependencies,
            jarArtifact.path.toString(), // should be invariantSeparatorChars
            jarArtifact.execClassName,
            appName,
            jvmOptions
        )

        val pckgedJar = File(Paths.get("").toAbsolutePath().toFile(), appName).absoluteFile


        // create exec_header to allow for direction execution (see http://www.capsule.io/user-guide/#really-executable-capsules)
        // from https://github.com/puniverse/capsule/blob/master/capsule-util/src/main/resources/capsule/execheader.sh
        File(tmpProjectDir, "exec_header.sh").writeText(
            """#!/usr/bin/env bash
            exec java -jar ${'$'}0 "${'$'}@"
            """
        )

        File(tmpProjectDir, "build.gradle").writeText(gradleScript)

        val packageResult = evalBash("cd '${tmpProjectDir}' && gradle simpleCapsule")

        with(packageResult) {
            if (exitCode != 0) {
                throw IllegalStateException("packaging of '$appName' failed:\n$packageResult")
            }
            Unit
        }

        pckgedJar.delete()
        File(tmpProjectDir, "build/libs/${appName}").copyTo(pckgedJar, true).setExecutable(true)

        infoMsg("Finished packaging into $pckgedJar")
    }
}
