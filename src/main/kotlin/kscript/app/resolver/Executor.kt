package kscript.app.resolver

import kscript.app.creator.JarArtifact
import kscript.app.model.Config
import kscript.app.util.Logger
import kscript.app.util.ProcessRunner
import kscript.app.util.ShellUtils
import java.nio.file.Path

class Executor(private val commandResolver: CommandResolver, private val config: Config) {
    fun compileKotlin(jar: Path, dependencies: Set<Path>, filePaths: Set<Path>) {
        val command = commandResolver.compileKotlin(jar, dependencies, filePaths)

        Logger.infoMsg("Jar compile: $command")

        val scriptCompileResult = ShellUtils.evalBash(command)

        if (scriptCompileResult.exitCode != 0) {
            throw IllegalStateException("compilation of scriplet failed\n$scriptCompileResult.stderr")
        }
    }

    fun executeKotlin(jarArtifact: JarArtifact, dependencies: Set<Path>, userArgs: List<String>) {
        if (config.kotlinHome == null) {
            throw IllegalStateException("KOTLIN_HOME is not set and could not be inferred from context")
        }

        val command = commandResolver.executeKotlin(jarArtifact, dependencies, userArgs)
        Logger.infoMsg("Execute command: $command")
        println(command)
    }

    fun runInteractiveRepl(dependencies: Set<Path>) {
        Logger.infoMsg("Creating REPL")
        val command = commandResolver.interactiveKotlinRepl(dependencies)
        Logger.infoMsg("REPL command: $command")
        println(command)
    }

    fun runIdea(projectPath: Path) {
        if (!ShellUtils.isInPath(config.intellijCommand)) {
            throw IllegalStateException("Could not find '${config.intellijCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_IDEA_COMMAND' env property")
        }

        if (!ShellUtils.isInPath(config.gradleCommand)) {
            throw IllegalStateException(
                "Could not find '${config.gradleCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_GRADLE_COMMAND' env property"
            )
        }

        // Create gradle wrapper
        ProcessRunner.runProcess("${config.gradleCommand} wrapper", wd = projectPath.toFile())

        val command = commandResolver.executeIdea(projectPath)
        Logger.infoMsg("Execute idea: $command")
        println(command)
    }
}
