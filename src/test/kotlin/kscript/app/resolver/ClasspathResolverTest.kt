package kscript.app.resolver

import assertk.assertThat
import assertk.assertions.endsWith
import io.mockk.mockk
import kscript.app.appdir.AppDir
import kscript.app.model.Config
import kscript.app.model.Dependency
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

class ClasspathResolverTest {
    private val config = Config.builder().build()
    private val appDir = AppDir(Paths.get("~/.kscript"))
    private lateinit var dependencyResolver: DependencyResolver
    private lateinit var classpathResolver: ClasspathResolver

    @BeforeEach
    fun setUp() {
        dependencyResolver = mockk()
        classpathResolver =  ClasspathResolver(config, appDir, dependencyResolver)
    }

    @Test
    fun `Resolve classpath`() {
        assertThat(
            classpathResolver.resolve(setOf(Dependency("log4j:log4j:1.2.14")), emptySet()).replace('\\', '/')
        ).endsWith(".m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar")

        assertThrows<RuntimeException> {
            classpathResolver.resolve(setOf(Dependency("log4j:log4j:9.8.76")), emptySet())
        }
    }
}
