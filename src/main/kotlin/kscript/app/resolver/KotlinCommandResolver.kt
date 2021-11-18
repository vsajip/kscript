package kscript.app.resolver

import kscript.app.model.CompilerOpt
import kscript.app.model.Config
import kscript.app.model.KotlinOpt
import kscript.app.model.ResolvedScript
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute

class KotlinCommandResolver(
    private val config: Config,
    private val resolvedScript: ResolvedScript,
    private val classpathResolver: ClasspathResolver
) {
    fun compile(compileOpts: Set<CompilerOpt>, outputJarPath: Path, filePaths: List<Path>): String {
        val compilerOptsStr = resolveCompilerOpts(resolvedScript.compilerOpts)
        val classpath = classpathResolver.resolve(resolvedScript.dependencies)
        val files = filePaths.joinToString(" ") { it.absolute().toString() }

        return "kotlinc $compilerOptsStr $classpath -d '${outputJarPath.absolute()}' $files"
    }

    fun execute(jarPath: Path, execClassName: String, userArgs: List<String>): String {
        val kotlinOptsStr = resolveKotlinOpts(resolvedScript.kotlinOpts)
        val userArgsStr = resolveUserArgs(userArgs)
        val scriptRuntime =
            Paths.get("${config.kotlinHome}${config.separatorChar}lib${config.separatorChar}kotlin-script-runtime.jar")
        val classpath = classpathResolver.resolve(resolvedScript.dependencies, jarPath, scriptRuntime)

        return "kotlin $kotlinOptsStr $classpath $execClassName $userArgsStr"
    }

    fun interactive(): String {
        val compilerOptsStr = resolveCompilerOpts(resolvedScript.compilerOpts)
        val kotlinOptsStr = resolveKotlinOpts(resolvedScript.kotlinOpts)
        val classpath = classpathResolver.resolve(resolvedScript.dependencies)

        return "kotlinc $compilerOptsStr $kotlinOptsStr $classpath"
    }

    private fun resolveKotlinOpts(kotlinOpts: Set<KotlinOpt>) = kotlinOpts.joinToString(" ") { it.value }
    private fun resolveCompilerOpts(compilerOpts: Set<CompilerOpt>) = compilerOpts.joinToString(" ") { it.value }
    private fun resolveUserArgs(userArgs: List<String>) =
        userArgs.joinToString(" ") { "\"${it.replace("\"", "\\\"")}\"" }
}
