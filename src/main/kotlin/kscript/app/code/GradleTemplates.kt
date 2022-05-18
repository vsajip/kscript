package kscript.app.code

import kscript.app.creator.JarArtifact
import kscript.app.model.CompilerOpt
import kscript.app.model.Dependency
import kscript.app.model.Repository
import kscript.app.model.Script
import kscript.app.util.ScriptUtils.dropExtension

object GradleTemplates {
    fun createGradleIdeaScript(script: Script): String {
        val kotlinVersion = KotlinVersion.CURRENT
        val extendedDependencies = setOf(
            Dependency("org.jetbrains.kotlin:kotlin-stdlib"),
            Dependency("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion"),
            Dependency("com.github.holgerbrandl:kscript-annotations:1.4"),
        ) + script.dependencies

        return """
            |plugins {
            |    id("org.jetbrains.kotlin.jvm") version "$kotlinVersion"
            |}
            |
            |repositories {
            |    mavenLocal()
            |    mavenCentral()
            |${createGradleRepositoriesSection(script.repositories).prependIndent()}
            |}
            |
            |dependencies {
            |${createGradleDependenciesSection(extendedDependencies).prependIndent()}
            |}
            |
            |sourceSets.getByName("main").java.srcDirs("src")
            |sourceSets.getByName("test").java.srcDirs("src")
            |
            |${createCompilerOptionsSection(script.compilerOpts)}
            |""".trimMargin()
    }

    //Capsule: https://github.com/ngyewch/gradle-capsule-plugin
    fun createGradlePackageScript(script: Script, jarArtifact: JarArtifact): String {
        val kotlinOptions = createCompilerOptionsSection(script.compilerOpts)

        val kotlinVersion = KotlinVersion.CURRENT
        val extendedDependencies = setOf(
            Dependency("org.jetbrains.kotlin:kotlin-stdlib"),
            Dependency("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
        ) + script.dependencies

        val capsuleApp = jarArtifact.execClassName

        return """
        plugins {
            id("org.jetbrains.kotlin.jvm") version "$kotlinVersion"
            id("it.gianluz.capsule") version "1.0.3"
            application
        }

        repositories {
            mavenLocal()
            mavenCentral()
            ${createGradleRepositoriesSection(script.repositories).prependIndent()}
        }
        
        tasks.create<us.kirchmeier.capsule.task.FatCapsule>("simpleCapsule") {
            applicationClass("$capsuleApp")
            archiveFileName.set("${script.scriptName.dropExtension()}")

            // https://github.com/danthegoodman/gradle-capsule-plugin/blob/master/DOCUMENTATION.md#really-executable-capsules
            reallyExecutable

            capsuleManifest.apply {
                applicationClass = "$capsuleApp"
                application = "${script.scriptName.dropExtension()}"
                applicationScript = "exec_header.sh"
                jvmArgs = listOf()
            }
        }
        
        dependencies {
            implementation(files("${jarArtifact.path.parent.resolve("scriplet.jar")}"))
            ${createGradleDependenciesSection(extendedDependencies).prependIndent()}
        }

        $kotlinOptions
        """.trimIndent()
    }

    private fun createGradleRepositoryCredentials(repository: Repository): String {
        if (repository.user.isNotBlank() && repository.password.isNotBlank()) {
            return """|credentials {
                      |    username = "${repository.user}"
                      |    password = "${repository.password}"
                      |}""".trimMargin()
        }

        return ""
    }

    private fun createGradleDependenciesSection(dependencies: Set<Dependency>) = dependencies.joinToString("\n") {
        "implementation(\"${it.value}\")"
    }

    private fun createGradleRepositoriesSection(repositories: Set<Repository>) = repositories.joinToString("\n") {
        """|maven {
           |    url = uri("${it.url}")
           |${createGradleRepositoryCredentials(it).prependIndent()}
           |}
        """.trimMargin()
    }

    private fun createCompilerOptionsSection(compilerOpts: Set<CompilerOpt>): String {
        if (compilerOpts.isEmpty()) {
            return ""
        }

        var jvmTarget = ""
        val freeCompilerArgs = mutableListOf<String>()

        for (opt in compilerOpts) {
            when {
                opt.value.startsWith("-jvm-target") -> {
                    jvmTarget = "jvmTarget = \"" + opt.value.drop(11).trim() + "\""
                }
                else -> {
                    freeCompilerArgs.add(opt.value)
                }
            }
        }

        return """|tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                  |    kotlinOptions {
                  |        $jvmTarget
                  |        freeCompilerArgs = listOf(${freeCompilerArgs.joinToString(", ") { "\"$it\"" }})
                  |    }
                  |}""".trimMargin()
    }
}
