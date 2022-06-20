package kscript.app.code

import kscript.app.creator.JarArtifact
import kscript.app.model.CompilerOpt
import kscript.app.model.Dependency
import kscript.app.model.Repository
import kscript.app.model.Script
import kscript.app.util.ScriptUtils.dropExtension

object GradleTemplates {
    fun createGradleIdeaScript(script: Script): String {
        val kotlinOptions = kotlinOptions(script.compilerOpts)

        val kotlinVersion = KotlinVersion.CURRENT
        val extendedDependencies = setOf(
            Dependency("org.jetbrains.kotlin:kotlin-stdlib"),
            Dependency("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
        ) + script.dependencies

        return """
        plugins {
            id("org.jetbrains.kotlin.jvm") version "$kotlinVersion"
        }

        repositories {
            mavenLocal()
            mavenCentral()
            ${createGradleRepositoriesSection(script.repositories).prependIndent()}
        }

        dependencies {
            ${createGradleDependenciesSection(extendedDependencies).prependIndent()}
        }

        sourceSets.getByName("main").java.srcDirs("src")
        sourceSets.getByName("test").java.srcDirs("src")

        $kotlinOptions
        """.trimIndent()
    }

    fun hangingIndents(s: String, n: Int): String {
        val parts = s.split('\n')
        val indent = String.format("%${n}s", " ")
        val result = StringBuilder()
        val psize = parts.size

        if (psize == 1) {
            return s
        }
        for ((i, p) in parts.withIndex()) {
            if (i == 0) {
                result.append(p)
            }
            else {
                result.append(indent)
                result.append(p)
            }
            if (i < (psize - 1)) {
                result.append('\n')
            }
        }
        return result.toString()
    }

    fun createGradlePackageScript(script: Script, jarArtifact: JarArtifact): String {
        val kotlinOptions = kotlinOptions(script.compilerOpts)
        val kotlinVersion = KotlinVersion.CURRENT
        val extendedDependencies = setOf(
            Dependency("org.jetbrains.kotlin:kotlin-stdlib"),
            Dependency("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
        ) + script.dependencies

        val capsuleApp = jarArtifact.execClassName
        val baseName = script.scriptName.dropExtension()

        return """
        import java.io.*
        import java.lang.System
        import java.nio.file.Files
        import java.nio.file.Paths

        plugins {
            id("org.jetbrains.kotlin.jvm") version "$kotlinVersion"
            application
        }

        repositories {
            mavenLocal()
            mavenCentral()
            ${createGradleRepositoriesSection(script.repositories).prependIndent("")}
        }

        tasks.jar {
            manifest {
                attributes["Main-Class"] = "$capsuleApp"
            }
            baseName = "$baseName"
            configurations["compileClasspath"].forEach { file: File ->
                from(zipTree(file.absoluteFile))
            }
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        tasks.register("makeScript") {
            dependsOn(":jar")
            doLast {
                val headerDir = layout.projectDirectory.toString()
                val jarFileName = layout.buildDirectory.file("libs/$baseName.jar").get().toString()
                val outFileName = layout.buildDirectory.file("libs/$baseName").get().toString()
                val lineSeparator = System.getProperty("line.separator").encodeToByteArray()
                val headerPath = Paths.get(headerDir).resolve("exec_header.sh")
                val headerBytes = Files.readAllBytes(headerPath)
                val jarBytes = Files.readAllBytes(Paths.get(jarFileName))
                val outFile = Paths.get(outFileName).toFile()
                val fileStream = FileOutputStream(outFile)

                fileStream.write(headerBytes)
                fileStream.write(lineSeparator)
                fileStream.write(jarBytes)
                fileStream.close()
            }
        }

        dependencies {
            implementation(files("${jarArtifact.path.parent.resolve("scriplet.jar")}"))
            ${hangingIndents(createGradleDependenciesSection(extendedDependencies), 12)}
        }

        $kotlinOptions
        """.trimStart('\n').trimIndent()
    }

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

    private fun createGradleDependenciesSection(dependencies: Set<Dependency>) = dependencies.joinToString("\n") {
        "implementation(\"${it.value}\")"
    }

    private fun createGradleRepositoriesSection(repositories: Set<Repository>) = repositories.joinToString("\n") {
        """
        maven {
            url "${it.url}"
            ${createGradleRepositoryCredentials(it).prependIndent()}
        }
        """.trimIndent()
    }

    private fun kotlinOptions(compilerOpts: Set<CompilerOpt>): String {
        val opts = compilerOpts.map { it.value }

        var jvmTargetOption: String? = null
        for (i in opts.indices) {
            if (i > 0 && opts[i - 1] == "-jvm-target") {
                jvmTargetOption = opts[i]
            }
        }

        val kotlinOpts = if (jvmTargetOption != null) {
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

        return kotlinOpts
    }
}
