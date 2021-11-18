package kscript.app.resolver

import kotlinx.coroutines.runBlocking
import kscript.app.model.Dependency
import kscript.app.model.Repository
import kscript.app.util.Logger.infoMsg
import java.io.File
import kotlin.collections.set
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.impl.makeExternalDependenciesResolverOptions
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver

class DependencyResolver(private val customRepos: Set<Repository>) {
    private val mvnResolver = MavenDependenciesResolver().apply {
        addRepository(RepositoryCoordinates("https://repo.maven.apache.org/maven2"))

        customRepos.map {
            val options = mutableMapOf<String, String>()

            if (it.password.isNotBlank() && it.user.isNotBlank()) {
                options["username"] = it.user
                options["password"] = it.password
            }

            infoMsg("Adding repository: $it")

            //Adding custom repository removes MavenCentral, so it must be re-added below
            addRepository(RepositoryCoordinates(it.url), makeExternalDependenciesResolverOptions(options))
        }
    }

    private val resolver =
        CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver(), mvnResolver)

    fun resolve(depIds: Set<Dependency>): List<File> {
        val resolvedDependencies = runBlocking {
            depIds.map {
                infoMsg("Resolving ${it.value}...")
                resolver.resolve(it.value)
            }.map {
                it.valueOr {
                    // Probably a wrapped Nullpointer from 'DefaultRepositorySystem.resolveDependencies()', this however is probably a connection problem.

                    throw IllegalStateException(
                        "Failed while connecting to the server. Check the connection (http/https, port, proxy, credentials, etc.) of your maven dependency locators. If you suspect this is a bug, you can create an issue on https://github.com/holgerbrandl/kscript" + it.reports.joinToString(
                            "\n"
                        ) { it.exception?.toString() ?: it.message },
                        it.reports.find { it.exception != null }?.exception
                    )
                }
            }
        }.flatten()

        return resolvedDependencies
    }
}
