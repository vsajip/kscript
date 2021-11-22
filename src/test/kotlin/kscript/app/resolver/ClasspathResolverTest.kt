package kscript.app.resolver

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import kscript.app.appdir.AppDir
import kscript.app.model.Dependency
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Paths

class ClasspathResolverTest {
    private val log4jDep = Dependency("log4j:log4j:1.2.14")
    private val log4jDepNonExistingVersion = Dependency("log4j:log4j:9.8.76")

    private lateinit var dependencyResolver: DependencyResolver

    @BeforeEach
    fun setUp() {
        dependencyResolver = mockk()
        val appDir = mockk<AppDir>()
        val fileMock = mockk<File>()
        every { fileMock.absolutePath } returns "~/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar"
        every { dependencyResolver.resolve(setOf(log4jDep)) } returns setOf(fileMock.toPath())
        every { dependencyResolver.resolve(setOf(log4jDepNonExistingVersion)) } throws RuntimeException()

//        classpathResolver = ClasspathResolver(":", appDir, dependencyResolver)
    }

    @Test
    fun `Resolve classpath`() {
//        assertThat(
//            classpathResolver.resolve(setOf(log4jDep))
//        ).isEqualTo("~/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar")
//
//        assertThrows<RuntimeException> {
//            classpathResolver.resolve(setOf(Dependency("log4j:log4j:9.8.76")))
//        }
    }
}
