package kscript.app.creator

import kscript.app.appdir.ProjectCache
import kscript.app.model.Script
import kscript.app.model.ScriptType
import kscript.app.resolver.CommandResolver
import kscript.app.util.Logger.infoMsg
import kscript.app.util.ScriptUtils.dropExtension
import kscript.app.util.ShellUtils
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists

data class JarArtifact(val path: Path, val execClassName: String)

class JarCreator(private val projectCache: ProjectCache, private val commandResolver: CommandResolver) {

    fun create(script: Script, resolvedDependencies: Set<Path>): JarArtifact {
        val jarPath = projectCache.findOrCreate(script).resolve("jar-dir")

        // Capitalize first letter and get rid of dashes (since this is what kotlin compiler is doing for the wrapper to create a valid java class name)
        // For valid characters see https://stackoverflow.com/questions/4814040/allowed-characters-in-filename
        val className =
            script.scriptName.dropExtension().replace("[^A-Za-z0-9]".toRegex(), "_").replaceFirstChar { it.titlecase() }
                // also make sure that it is a valid identifier by avoiding an initial digit (to stay in sync with what the kotlin script compiler will do as well)
                .let { if ("^[0-9]".toRegex().containsMatchIn(it)) "_$it" else it }


        val scriptFile = jarPath.resolve("/" + className + script.scriptType.extension).toFile()
        scriptFile.writeText(script.resolvedCode)

        // Define the entrypoint for the scriptlet jar
        val packageName = if (script.packageName != null) script.packageName.value + "." else ""
        val execClassName = if (script.scriptType == ScriptType.KTS) {
            "Main_${className}"
        } else {
            // extract package from kt-file
            """${packageName}${script.entryPoint?.value ?: "${className}Kt"}"""
        }

        val jarFile = jarPath.resolve("scriplet.jar")

        if (jarFile.exists()) {
            return JarArtifact(jarPath, execClassName)
        }

        if (!ShellUtils.isInPath("kotlinc")) {
            throw IllegalStateException("${"kotlinc"} is not in PATH")
        }

        val filesToCompile = mutableSetOf<Path>()
        filesToCompile.add(scriptFile.toPath())

        // create main-wrapper for kts scripts
        if (script.scriptType == ScriptType.KTS) {
            val classReference = packageName + className

            val code = """
            class Main_${className}{
                companion object {
                    @JvmStatic
                    fun main(args: Array<String>) {
                        val script = Main_${className}::class.java.classLoader.loadClass("$classReference")
                        script.getDeclaredConstructor(Array<String>::class.java).newInstance(args);
                    }
                }
            }
            """.trimIndent()

            val mainKotlin = File(jarPath.toFile(), "$execClassName.kt")
            mainKotlin.writeText(code)
            filesToCompile.add(mainKotlin.toPath())
        }

        val command = commandResolver.compileKotlin(jarPath, resolvedDependencies, filesToCompile)

        infoMsg("Jar compile: $command")

        val scriptCompileResult = ShellUtils.evalBash(command)

        if (scriptCompileResult.exitCode != 0) {
            throw IllegalStateException("compilation of '${scriptFile.name}' failed\n$scriptCompileResult.stderr")
        }

        return JarArtifact(jarFile, execClassName)
    }
}
