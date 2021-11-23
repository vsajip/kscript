package kscript.app.code

import kscript.app.model.Dependency
import kscript.app.model.Repository
import kscript.app.model.Script

object GradleTemplates {
    fun createGradleIdeaScript(script: Script): String {
        val kotlinOptions = kotlinOptions(script)

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
            ${createGradleRepositoriesSection(script.repositories)}
        }

        dependencies {
            ${createGradleDependenciesSection(extendedDependencies)}
        }

        sourceSets.getByName("main").java.srcDirs("src")
        sourceSets.getByName("test").java.srcDirs("src")

        $kotlinOptions
    """.trimIndent()
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
        """
        implementation "${it.value}"
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
        dependencies: Set<Dependency>,
        filePaths: String,
        wrapperClassName: String,
        appName: String,
        jvmOptions: String
    ): String {
        val kotlinVersion = KotlinVersion.CURRENT
        val extendedDependencies = setOf(
            Dependency("org.jetbrains.kotlin:kotlin-stdlib"),
            Dependency("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
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

    private fun kotlinOptions(script: Script): String {
        val opts = script.compilerOpts.map { it.value }

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
