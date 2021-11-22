package kscript.app.resolver

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import kscript.app.model.Dependency
import kscript.app.model.Repository
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.invariantSeparatorsPathString

class DependencyResolverTest {
    private val repositoryPathString =
        Paths.get(System.getProperty("user.home") + "/.m2/repository").toAbsolutePath().invariantSeparatorsPathString

    private val dependencyResolver = DependencyResolver(
        setOf(
            Repository(
                "imagej-releases", "http://maven.imagej.net/content/repositories/releases"
            )
        )
    )

    @ParameterizedTest
    @ValueSource(strings = ["log4j:log4j:1.2.14", "net.clearvolume:cleargl:jar:2.0.1"])
    fun `Resolve dependencies`(dependencyString: String) {
        val dependency = Dependency(dependencyString)
        assertThat(dependencyResolver.resolve(setOf(dependency))).contains(calculateArtifactPath(dependency, true))
    }

    @Test
    fun `Non-existing dependency`() {
        assertThat { dependencyResolver.resolve(setOf(Dependency("log4j:log4j:9.8.76"))) }.isFailure().isInstanceOf(
            IllegalStateException::class.java
        )
    }

    private fun calculateArtifactPath(dependency: Dependency, cleanupFirst: Boolean = false): Path {
        val parts = dependency.value.split(":")
        require(parts.size == 3 || parts.size == 4)

        val group = parts[0].replace('.', '/')
        val artifact = parts[1]
        val extension = if (parts.size == 4) parts[2] else "jar"
        val version = parts[parts.size - 1]


        val calculatedPath = Paths.get("$repositoryPathString/$group/$artifact/$version/$artifact-$version.$extension")

        if (cleanupFirst) {
            val cleanupPath = Paths.get("$repositoryPathString/$group")
            FileUtils.cleanDirectory(cleanupPath.toFile())
        }

        return calculatedPath
    }
}
