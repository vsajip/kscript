package kscript.app.util

import kscript.app.creator.JarArtifact
import kscript.app.model.Config
import kscript.app.resolver.CommandResolver
import kscript.app.util.Logger.devMsg
import java.nio.file.Path
import java.lang.ProcessBuilder.Redirect
import java.util.concurrent.TimeUnit

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

    fun executeCommand(command: String) {
        if (!config.executeCommands) {
            println(command)
        }
        else {
            // A bit of a kludge here to remove quotes. Needs to be made more bulletproof
            val cmd = command.trim().split("\\s+".toRegex()).map {
                var s = it
                if (s[0] == '\"' && s[s.length - 1] == '\"') {
                    s = s.substring(1, it.length - 1)
                }
                s
            }
            ProcessBuilder(cmd)
                .redirectOutput(Redirect.INHERIT)
                .redirectError(Redirect.INHERIT)
                .start().waitFor(60, TimeUnit.MINUTES)
        }
    }

    fun executeKotlin(jarArtifact: JarArtifact, dependencies: Set<Path>, userArgs: List<String>) {
        if (config.kotlinHome == null && !ShellUtils.isInPath(config.osType, "kotlin") ) {
            throw IllegalStateException("KOTLIN_HOME is not set and could not be inferred from context, and kotlin is not in PATH")
        }

        val command = commandResolver.executeKotlin(jarArtifact, dependencies, userArgs)
        devMsg("Kotlin execute command: $command")
        executeCommand(command)
    }

    fun runInteractiveRepl(dependencies: Set<Path>) {
        Logger.infoMsg("Creating REPL")
        val command = commandResolver.interactiveKotlinRepl(dependencies)
        devMsg("REPL Kotlin command: $command")
        executeCommand(command)
    }

    fun runIdea(projectPath: Path) {
        if (!ShellUtils.isInPath(config.osType, config.intellijCommand)) {
            throw IllegalStateException("Could not find '${config.intellijCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_IDEA_COMMAND' env property")
        }

        if (!ShellUtils.isInPath(config.osType, config.gradleCommand)) {
            throw IllegalStateException(
                "Could not find '${config.gradleCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_GRADLE_COMMAND' env property"
            )
        }

        // Create gradle wrapper
        ProcessRunner.runProcess("${config.gradleCommand} wrapper", wd = projectPath.toFile())

        val command = commandResolver.executeIdea(projectPath)
        devMsg("Idea execute command: $command")
        executeCommand(command)
    }

    fun createPackage(projectPath: Path) {
        if (!ShellUtils.isInPath(config.osType, config.gradleCommand)) {
            throw IllegalStateException("gradle is required to package kscripts")
        }

        val command = commandResolver.createPackage(projectPath)
        devMsg("Create package command: $command")

        val result = ShellUtils.evalBash(config.osType, command)
        if (result.exitCode != 0) {
            throw IllegalStateException("Packaging for path: '$projectPath' failed:$result")
        }
    }
}
