package kscript.app.code

import kscript.app.model.PackageName
import kscript.app.model.ScriptType
import org.intellij.lang.annotations.Language

object Templates {
    @Language("sh")
    val bootstrapHeader = """
        #!/bin/bash
        
        //usr/bin/env echo '
        /**** BOOTSTRAP kscript ****\'>/dev/null
        command -v kscript >/dev/null 2>&1 || source /dev/stdin <<< "${'$'}(curl -L https://git.io/fpF1K)"
        exec kscript $0 "$@"
        \*** IMPORTANT: Any code including imports and annotations must come after this line ***/
        
        """.trimIndent()

    val textProcessingPreamble = """
        //DEPS com.github.holgerbrandl:kscript-support-api:1.2.5

        import kscript.text.*
        val lines = resolveArgFile(args)
        
        """.trimIndent()

    val executeHeader = """
        #!/usr/bin/env bash
        exec java -jar ${'$'}0 "${'$'}@"
        """.trimIndent()

    fun wrapperForScript(packageName: PackageName, className: String): String {
        val classReference = packageName.value + "." + className

        return """
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
    }

    fun runConfig(rootScriptName: String, rootScriptType: ScriptType, userArgs: List<String>): String {
        val rootFileName = rootScriptName + rootScriptType.extension
        val userArgsString = userArgs.joinToString(" ") { it }

        if (rootScriptType == ScriptType.KT) {
            return """  |<component name="ProjectRunConfigurationManager">
                        |  <configuration default="false" name="$rootFileName" type="JetRunConfigurationType" nameIsGenerated="true">
                        |    <module name="idea" />
                        |    <option name="MAIN_CLASS_NAME" value="${rootScriptName}Kt" />
                        |    <option name="PROGRAM_PARAMETERS" value="$userArgsString" />
                        |    <shortenClasspath name="NONE" />
                        |    <method v="2">
                        |      <option name="Make" enabled="true" />
                        |    </method>
                        |  </configuration>
                        |</component>
                        |""".trimMargin()
        }

        return """  |<component name="ProjectRunConfigurationManager">
                    |  <configuration default="false" name="$rootFileName" type="KotlinStandaloneScriptRunConfigurationType" nameIsGenerated="true">
                    |    <module name="idea" />
                    |    <option name="PROGRAM_PARAMETERS" value="$userArgsString" />
                    |    <shortenClasspath name="NONE" />
                    |    <option name="filePath" value="${'$'}PROJECT_DIR${'$'}/src/$rootFileName" />
                    |    <method v="2">
                    |      <option name="Make" enabled="true" />
                    |    </method>
                    |  </configuration>
                    |</component>
                    |""".trimMargin()
    }

    fun usageOptions(selfName: String, version: String) = """
        $selfName - Enhanced scripting support for Kotlin on *nix-based systems.

        Usage:
         $selfName [options] <script> [<script_args>]...
         $selfName --clear-cache

        The <script> can be a script file (*kts), a script URL, - for stdin, a *.kt source file with a main method, or some kotlin code.

        Use '--clear-cache' to wipe cached script jars and urls

        Options:
         -i --interactive        Create interactive shell with dependencies as declared in script
         -t --text               Enable stdin support API for more streamlined text processing
         --idea                  Open script in temporary Intellij session
         -s --silent             Suppress status logging
         -d --development        Enable logging of exception stack trace and additional log messages
         --package               Package script and dependencies into self-dependent binary
         --add-bootstrap-header  Prepend bash header that installs kscript if necessary


        Copyright : 2022 Holger Brandl
        License   : MIT
        Version   : v${version}
        Website   : https://github.com/holgerbrandl/kscript
        """.trimIndent().trim()
}
