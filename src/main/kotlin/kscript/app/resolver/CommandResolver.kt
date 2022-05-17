package kscript.app.resolver

import kscript.app.creator.JarArtifact
import kscript.app.model.CompilerOpt
import kscript.app.model.Config
import kscript.app.model.KotlinOpt
import kscript.app.model.Script
import kscript.app.util.FileUtils.nativeToShellPath
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

class CommandResolver(private val config: Config, private val script: Script) {
    //Syntax for different OS-es:
    //LINUX:    /usr/local/sdkman/..../kotlin  -classpath "/home/vagrant/workspace/Kod/Repos/kscript/test:/home/vagrant/.kscript/cache/jar_2ccd53e06b0355d3573a4ae8698398fe/scriplet.jar:/usr/local/sdkman/candidates/kotlin/1.6.21/lib/kotlin-script-runtime.jar" Main_Scriplet
    //GIT-BASH: /c/Users/Admin/.sdkman/candidates/kotlin/current/bin/kotlin  -classpath "C:\Users\Admin;C:\Users\Admin\.kscript\cache\jar_2ccd53e06b0355d3573a4ae8698398fe\scriplet.jar;C:\Users\Admin\.sdkman\candidates\kotlin\current\lib\kotlin-script-runtime.jar" Main_Scriplet
    //CYGWIN:   /home/Admin/.sdkman/candidates/kotlin/current/bin/kotlin  -classpath "C:\Users\Admin;C:\Users\Admin\.kscript\cache\jar_2ccd53e06b0355d3573a4ae8698398fe\scriplet.jar;C:\Users\Admin\.sdkman\candidates\kotlin\current\lib\kotlin-script-runtime.jar" Main_Scriplet
    //WINDOWS:  C:\Users\Admin\.sdkman\candidates\kotlin\current\bin\kotlin  -classpath "C:\Users\Admin;C:\Users\Admin\.kscript\cache\jar_2ccd53e06b0355d3573a4ae8698398fe\scriplet.jar;C:\Users\Admin\.sdkman\candidates\kotlin\current\lib\kotlin-script-runtime.jar" Main_Scriplet
    //MACOS:


    //<command_path>kotlinc -classpath "p1:p2"
    //OS Conversion matrix
    //              command_path    command_quoting     classpath_path  classpath_separator     classpath_quoting       files_path      files_quoting   main_class_quoting   @arg_file
    //LINUX         native          no                  native          :                       "                       ?                                                    no
    //GIT-BASH      shell           no                  native          ;                       "                       ?                                                    no
    //CYGWIN        shell           no                  native          ;                       "                                                                            no
    //WINDOWS       native          no                  native          ;                       "                                                                            yes
    //MACOS         ?               ?                   ?               ?                       ?                                                                            no


    //Path conversion (Cygwin/mingw): cygpath -u "c:\Users\Admin"; /cygdrive/c/ - Cygwin; /c/ - Mingw
    //uname --> CYGWIN_NT-10.0 or MINGW64_NT-10.0-19043
    //How to find if mingw/cyg/win (second part): https://stackoverflow.com/questions/40877323/quickly-find-if-java-was-launched-from-windows-cmd-or-cygwin-terminal

    fun compileKotlin(jar: Path, dependencies: Set<Path>, filePaths: Set<Path>): String {
        val compilerOptsStr = resolveCompilerOpts(script.compilerOpts)
        val classpath = resolveClasspath(dependencies)
        val files = filePaths.joinToString(" ") { "\"${it.absolutePathString()}\"" }
        val kotlinc = resolveKotlinBinary("kotlinc")

        return "$kotlinc $compilerOptsStr $classpath -d \"${jar.absolutePathString()}\" $files"
    }

    fun executeKotlin(jarArtifact: JarArtifact, dependencies: Set<Path>, userArgs: List<String>): String {
        val kotlinOptsStr = resolveKotlinOpts(script.kotlinOpts)
        val userArgsStr = resolveUserArgs(userArgs)
        val scriptRuntime =
            Paths.get("${config.kotlinHome}${config.hostPathSeparatorChar}lib${config.hostPathSeparatorChar}kotlin-script-runtime.jar")

        val dependenciesSet = buildSet<Path> {
            addAll(dependencies)
            add(jarArtifact.path)
            add(scriptRuntime)
        }

        val classpath = resolveClasspath(dependenciesSet)
        val kotlin = resolveKotlinBinary("kotlin")

        return "$kotlin $kotlinOptsStr $classpath ${jarArtifact.execClassName} $userArgsStr"
    }

    fun interactiveKotlinRepl(dependencies: Set<Path>): String {
        val compilerOptsStr = resolveCompilerOpts(script.compilerOpts)
        val kotlinOptsStr = resolveKotlinOpts(script.kotlinOpts)
        val classpath = resolveClasspath(dependencies)
        val kotlinc = resolveKotlinBinary("kotlinc")

        return "$kotlinc $compilerOptsStr $kotlinOptsStr $classpath"
    }

    fun executeIdea(projectPath: Path): String {
        return "${config.intellijCommand} \"$projectPath\""
    }

    fun createPackage(projectPath: Path): String {
        return "cd '${projectPath}' && ${config.gradleCommand} simpleCapsule"
    }

    private fun resolveKotlinOpts(kotlinOpts: Set<KotlinOpt>) = kotlinOpts.joinToString(" ") { it.value }

    private fun resolveCompilerOpts(compilerOpts: Set<CompilerOpt>) = compilerOpts.joinToString(" ") { it.value }

    private fun resolveUserArgs(userArgs: List<String>) =
        userArgs.joinToString(" ") { "\"${it.replace("\"", "\\\"")}\"" }

    private fun resolveClasspath(dependencies: Set<Path>) =
        if (dependencies.isEmpty()) "" else "-classpath \"" + dependencies.joinToString(config.classPathSeparator.toString()) {
            it.absolutePathString()
        } + "\""

    private fun resolveKotlinBinary(binary: String) = if (config.kotlinHome != null) nativeToShellPath(
        config.osType,
        (config.kotlinHome / "bin" / binary)
    ) else binary
}
