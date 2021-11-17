package kscript.app.resolver

import kscript.app.model.Dependency
import kscript.app.model.Repository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DependencyResolverTest {
    private val dependencyResolver = DependencyResolver()

    @Test
    fun `Resolve dependencies`() {

        dependencyResolver.resolve(setOf(Dependency("log4j:log4j:1.2.14")), emptySet())

        //TODO clear local file first
        dependencyResolver.resolve(setOf(Dependency("net.clearvolume:cleargl:jar:2.0.1")), setOf(Repository("imagej-releases","http://maven.imagej.net/content/repositories/releases")))


        assertThrows<RuntimeException> {
            dependencyResolver.resolve(setOf(Dependency("log4j:log4j:9.8.76")), emptySet())
        }
    }
}
