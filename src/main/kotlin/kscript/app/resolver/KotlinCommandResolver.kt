package kscript.app.resolver

import kscript.app.model.CompilerOpt
import kscript.app.model.Dependency
import kscript.app.model.KotlinOpt
import kscript.app.model.ResolvedScript
import java.io.File

class KotlinCommandResolver(private val resolvedScript: ResolvedScript) {

    fun compile(compilerOpts: Set<CompilerOpt>): String {

        return ""
    }

    fun execute(userArgs: List<String>): String {
//        val kotlinOptsStr = resolveKotlinOpts(kotlinOpts)
//        val userArgsStr = resolveUserArgs(userArgs)
//
//        return "kotlin $kotlinOptsStr -classpath \"$extClassPath\" $execClassName $userArgsStr"
        return ""
    }

    fun interactive(): String {
//        return "kotlinc ${resolvedScript.compilerOpts} ${resolvedScript.kotlinOpts} $optionalCpArg"
        return ""
    }

    private fun resolveKotlinOpts(kotlinOpts: Set<KotlinOpt>) = kotlinOpts.joinToString(" ") { it.value }
    private fun resolveCompilerOpts(compilerOpts: Set<CompilerOpt>) = compilerOpts.joinToString(" ") { it.value }
    private fun resolveUserArgs(userArgs: List<String>) =
        userArgs.joinToString(" ") { "\"${it.replace("\"", "\\\"")}\"" }
    private fun resolveClasspath(jarFiles: List<File>) {

    }
}
