package kscript.app

import kscript.app.util.Logger.errorMsg
import kscript.app.util.Logger.infoMsg
import kscript.app.ShellUtils.isInPath
import kscript.app.code.Templates
import kscript.app.model.UnifiedScript
import kscript.app.util.Logger
import java.io.File
import java.nio.file.Paths

class PackageCreator {
    /**
     * Create and use a temporary gradle project to package the compiled script using capsule.
     * See https://github.com/puniverse/capsule
     */
    fun packageKscript(
        unifiedScript: UnifiedScript, scriptJar: File, wrapperClassName: String, appName: String
    ) {
        if (!isInPath("gradle")) {
            errorMsg("gradle is required to package kscripts".toString())
            quit(1)
        }

        infoMsg("Packaging script '$appName' into standalone executable...")


        val tmpProjectDir =
            KSCRIPT_DIR.run { File(this, "kscript_tmp_project__${scriptJar.name}_${System.currentTimeMillis()}") }
                .apply { mkdir() }

        val jvmOptions = unifiedScript.kotlinOpts.filter { it.startsWith("-J") }.map { it.removePrefix("-J") }
            .joinToString(", ") { '"' + it + '"' }

        // https://shekhargulati.com/2015/09/10/gradle-tip-using-gradle-plugin-from-local-maven-repository/

        val gradleScript = Templates.createGradlePackageScript(
            unifiedScript.repositories,
            unifiedScript.dependencies,
            scriptJar.invariantSeparatorsPath,
            wrapperClassName,
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

        val pckgResult = evalBash("cd '${tmpProjectDir}' && gradle simpleCapsule")

        with(pckgResult) {
            if (exitCode != 0) {
                Logger.errorMsg("packaging of '$appName' failed:\n$pckgResult".toString())
                quit(1)
            }
            Unit
        }

        pckgedJar.delete()
        File(tmpProjectDir, "build/libs/${appName}").copyTo(pckgedJar, true).setExecutable(true)

        infoMsg("Finished packaging into $pckgedJar")
    }

}
