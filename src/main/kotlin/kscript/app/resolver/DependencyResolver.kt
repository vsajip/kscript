package kscript.app.resolver

import kotlinx.coroutines.runBlocking
import kscript.app.model.Dependency
import kscript.app.model.Repository
import kscript.app.util.Logger.infoMsg
import java.io.File
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.impl.DependenciesResolverOptionsName
import kotlin.script.experimental.dependencies.impl.makeExternalDependenciesResolverOptions
import kotlin.script.experimental.dependencies.impl.set
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver

class DependencyResolver(private val customRepos: Set<Repository>) {
    private val mvnResolver = MavenDependenciesResolver().apply {
        customRepos.map {
            val options = mutableMapOf<String, String>()

            if (it.password.isNotBlank() && it.user.isNotBlank()) {
                options[DependenciesResolverOptionsName.USERNAME] = it.user
                options[DependenciesResolverOptionsName.PASSWORD] = it.password
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
                infoMsg(it.reports.toString())
                it.valueOrThrow()
            }
        }.flatten()

        return resolvedDependencies
    }
}
