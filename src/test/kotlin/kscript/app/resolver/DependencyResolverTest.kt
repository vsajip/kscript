package kscript.app.resolver

import assertk.assertThat
import assertk.assertions.endsWith
import kscript.app.appdir.AppDir
import kscript.app.model.Config
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

class DependencyResolverTest {
    @Test
    fun `Resolve classpath`() {
        val config = Config(
            "kscript",
            Paths.get("~/.kscript"),
            "",
            "idea",
            "gradle",
            null,
            if (System.getProperty("os.name").lowercase().contains("windows")) ";" else ":"
        )

        val appDir = AppDir(Paths.get("~/.kscript"))

        val dependencyResolver = DependencyResolver(config, appDir)

        assertThat(
            dependencyResolver.resolveClasspath(setOf("log4j:log4j:1.2.14"), emptySet()).replace('\\', '/')
        ).endsWith(".m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar")

        assertThrows<RuntimeException> {
            dependencyResolver.resolveClasspath(setOf("log4j:log4j:9.8.76"), emptySet())
        }
    }
}
