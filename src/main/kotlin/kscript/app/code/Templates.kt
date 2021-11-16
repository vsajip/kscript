package kscript.app.code

import kscript.app.model.Repository
import org.intellij.lang.annotations.Language
import java.io.File

object Templates {
    @Language("sh")
    val bootstrapHeader = """
        #!/bin/bash
        
        //usr/bin/env echo '
        /**** BOOTSTRAP kscript ****\'>/dev/null
        command -v kscript >/dev/null 2>&1 || curl -L "https://git.io/fpF1K" | bash 1>&2
        exec kscript $0 "$@"
        \*** IMPORTANT: Any code including imports and annotations must come after this line ***/
        
    """.trimIndent()

    val textProcessingPreamble = """
        //DEPS com.github.holgerbrandl:kscript-support-api:1.2.5

        import kscript.text.*
        val lines = resolveArgFile(args)
        
        """.trimIndent()

    private fun createGradleRepositoryCredentials(repository: Repository): String {
        if (repository.user.isNotBlank() && repository.password.isNotBlank()) {
            return """
                credentials {
                    username = "${repository.user}"
                    password = "${repository.password}"
                }
            """.trimIndent()
        }

        return ""
    }

    private fun createGradleDependenciesSection(dependencies: Set<String>) = dependencies.joinToString("\n") {
        """
        implementation \"$it\"
        """.trimIndent()
    }

    private fun createGradleRepositoriesSection(repositories: Set<Repository>) = repositories.joinToString("\n") {
        """ 
        maven {
            url "${it.url}"
            ${createGradleRepositoryCredentials(it)}
        }
        """.trimIndent()
    }

    fun createGradlePackageScript(
        repositories: Set<Repository>,
        dependencies: Set<String>,
        filePaths: String,
        wrapperClassName: String,
        appName: String,
        jvmOptions: String
    ): String {
        val kotlinVersion = KotlinVersion.CURRENT
        val extendedDependencies = setOf(
            "org.jetbrains.kotlin:kotlin-stdlib", "org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion"
        ) + dependencies

        return """     
        plugins {
            id "org.jetbrains.kotlin.jvm" version "$kotlinVersion"
            id "it.gianluz.capsule" version "1.0.3"
        }

        repositories {
            mavenLocal()
            mavenCentral()
            ${createGradleRepositoriesSection(repositories)}
        }

        dependencies {
            ${createGradleDependenciesSection(extendedDependencies)}

            // https://stackoverflow.com/questions/20700053/how-to-add-local-jar-file-dependency-to-build-gradle-file
            implementation files('$filePaths')
        }

        task simpleCapsule(type: FatCapsule){
            applicationClass '$wrapperClassName'

            archiveName '$appName'

            // http://www.capsule.io/user-guide/#really-executable-capsules
            reallyExecutable

            capsuleManifest {
                jvmArgs = [$jvmOptions]
                //args = []
                //systemProperties['java.awt.headless'] = true
            }
        }
        """.trimIndent()
    }


    fun createGradleIdeaScript(
        repositories: Set<Repository>, dependencies: Set<String>, kotlinOptions: String
    ): String {
        val kotlinVersion = KotlinVersion.CURRENT
        val extendedDependencies = setOf(
            "org.jetbrains.kotlin:kotlin-stdlib", "org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion"
        ) + dependencies

        return """
        plugins {
            id("org.jetbrains.kotlin.jvm") version "$kotlinVersion"
        }

        repositories {
            mavenLocal()
            mavenCentral()
            ${createGradleRepositoriesSection(repositories)}
        }

        dependencies {
            ${createGradleDependenciesSection(extendedDependencies)}
        }

        sourceSets.getByName("main").java.srcDirs("src")
        sourceSets.getByName("test").java.srcDirs("src")

        $kotlinOptions
    """.trimIndent()
    }

    fun runConfig(scriptFile: File, tmpProjectDir: File, userArgs: List<String>): String {
        val body = if (scriptFile.extension == "kt") {
            """
        <configuration name="${scriptFile.name.substringBeforeLast(".")}" type="JetRunConfigurationType">
            <module name="${tmpProjectDir.name}.main" />
            <option name="VM_PARAMETERS" value="" />
            <option name="PROGRAM_PARAMETERS" value="" />
            <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
            <option name="ALTERNATIVE_JRE_PATH" />
            <option name="PASS_PARENT_ENVS" value="true" />
            <option name="MAIN_CLASS_NAME" value="${scriptFile.name.substringBeforeLast(".").capitalize()}Kt" />
            <option name="WORKING_DIRECTORY" value="" />
            <method v="2">
                <option name="Make" enabled="true" />
            </method>
            </configuration>
            """.trimIndent()
        } else {
            """  
        <configuration default="false" name="Main" type="BashConfigurationType" factoryName="Bash">
            <option name="INTERPRETER_OPTIONS" value="" />
            <option name="INTERPRETER_PATH" value="kscript" />
            <option name="PROJECT_INTERPRETER" value="false" />
            <option name="WORKING_DIRECTORY" value="" />
            <option name="PARENT_ENVS" value="true" />
            <option name="SCRIPT_NAME" value="${'$'}PROJECT_DIR${'$'}/src/${scriptFile.name}" />
            <option name="PARAMETERS" value="${userArgs.joinToString(" ")}" />
            <module name="" />
            <method v="2" />
         </configuration>
         """.trimIndent()
        }
        return """
            <component name="ProjectRunConfigurationManager">
            $body
            </component>
        """.trimIndent()
    }

    fun kotlinOptions(jvmTargetOption: String?) = if (jvmTargetOption != null) {
        """
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions { 
                jvmTarget = "$jvmTargetOption"
            }
        }
        """.trimIndent()
    } else {
        ""
    }

    fun usage(selfName: String, version: String) = """
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
         -s --silent             Suppress status logging to stderr
         --package               Package script and dependencies into self-dependent binary
         --add-bootstrap-header  Prepend bash header that installs kscript if necessary


        Copyright : 2021 Holger Brandl
        License   : MIT
        Version   : v${version}
        Website   : https://github.com/holgerbrandl/kscript
        """.trimIndent().trim()
}
