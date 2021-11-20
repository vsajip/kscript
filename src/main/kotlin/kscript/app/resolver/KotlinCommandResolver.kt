package kscript.app.resolver

import kscript.app.model.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute

class KotlinCommandResolver(
    private val config: Config,
    private val script: Script,
    private val classpathResolver: ClasspathResolver
) {
    fun compile(jarPath: Path, filePaths: List<Path>): String {
        val compilerOptsStr = resolveCompilerOpts(script.compilerOpts)
        val classpath = classpathResolver.resolve(script.dependencies)
        val files = filePaths.joinToString(" ") { it.absolute().toString() }

        return "kotlinc $compilerOptsStr $classpath -d '${jarPath.absolute()}' $files"
    }

    fun execute(jarPath: Path, execClassName: String, userArgs: List<String>): String {
        val kotlinOptsStr = resolveKotlinOpts(script.kotlinOpts)
        val userArgsStr = resolveUserArgs(userArgs)
        val scriptRuntime =
            Paths.get("${config.kotlinHome}${config.separatorChar}lib${config.separatorChar}kotlin-script-runtime.jar")
        val classpath = classpathResolver.resolve(script.dependencies, jarPath, scriptRuntime)

        return "kotlin $kotlinOptsStr $classpath $execClassName $userArgsStr"
    }

    fun interactive(): String {
        val compilerOptsStr = resolveCompilerOpts(script.compilerOpts)
        val kotlinOptsStr = resolveKotlinOpts(script.kotlinOpts)
        val classpath = classpathResolver.resolve(script.dependencies)

        return "kotlinc $compilerOptsStr $kotlinOptsStr $classpath"
    }

    private fun resolveKotlinOpts(kotlinOpts: Set<KotlinOpt>) = kotlinOpts.joinToString(" ") { it.value }
    private fun resolveCompilerOpts(compilerOpts: Set<CompilerOpt>) = compilerOpts.joinToString(" ") { it.value }
    private fun resolveUserArgs(userArgs: List<String>) =
        userArgs.joinToString(" ") { "\"${it.replace("\"", "\\\"")}\"" }
}
