package kscript.app.util

import kscript.app.creator.JarArtifact
import kscript.app.model.Config
import kscript.app.resolver.CommandResolver
import kscript.app.util.Logger.devMsg
import kscript.app.util.Logger.warnMsg
import java.nio.file.Path

class Executor(private val commandResolver: CommandResolver, private val config: Config) {
    fun compileKotlin(jar: Path, dependencies: Set<Path>, filePaths: Set<Path>) {
        if (config.kotlinHome == null && !ShellUtils.isInPath(config.osType, "kotlinc")) {
            throw IllegalStateException("${"kotlinc"} is not in PATH")
        }

        val command = commandResolver.compileKotlin(jar, dependencies, filePaths)

        devMsg("JAR compile command: $command")

        val scriptCompileResult = ShellUtils.evalBash(config.osType, command)

        if (scriptCompileResult.exitCode != 0) {
            throw IllegalStateException("Compilation of scriplet failed:\n$scriptCompileResult")
        }
    }

    fun executeKotlin(jarArtifact: JarArtifact, dependencies: Set<Path>, userArgs: List<String>) {
        if (config.kotlinHome == null && !ShellUtils.isInPath(config.osType, "kotlin") ) {
            throw IllegalStateException("KOTLIN_HOME is not set and could not be inferred from context, and kotlin is not in PATH")
        }

        val command = commandResolver.executeKotlin(jarArtifact, dependencies, userArgs)
        devMsg("Kotlin execute command: $command")
        println(command)
    }

    fun runInteractiveRepl(dependencies: Set<Path>) {
        Logger.infoMsg("Creating REPL")
        val command = commandResolver.interactiveKotlinRepl(dependencies)
        devMsg("REPL Kotlin command: $command")
        println(command)
    }

    fun runIdea(projectPath: Path) {
        if (ShellUtils.isInPath(config.osType, config.gradleCommand)) {
            // Create gradle wrapper
            ProcessRunner.runProcess("${config.gradleCommand} wrapper", wd = projectPath.toFile())
        } else {
            warnMsg("Could not find '${config.gradleCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_COMMAND_GRADLE' env property")
        }

        if (ShellUtils.isInPath(config.osType, config.intellijCommand)) {
            val command = commandResolver.executeIdea(projectPath)
            devMsg("Idea execute command: $command")
            println(command)
        } else {
            warnMsg("Could not find '${config.intellijCommand}' in your PATH. You should set the command used to launch your intellij as 'KSCRIPT_COMMAND_IDEA' env property")
        }
    }

    fun createPackage(projectPath: Path) {
        if (!ShellUtils.isInPath(config.osType, config.gradleCommand)) {
            throw IllegalStateException("gradle is required to package scripts")
        }

        val command = commandResolver.createPackage(projectPath)
        devMsg("Create package command: $command")

        val result = ShellUtils.evalBash(config.osType, command)
        if (result.exitCode != 0) {
            throw IllegalStateException("Packaging for path: '$projectPath' failed:$result")
        }
    }
}
