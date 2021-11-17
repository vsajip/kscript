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

class DependencyResolver {
    fun resolve(depIds: Set<Dependency>, customRepos: Set<Repository>): List<File> {
        val mvnResolver = MavenDependenciesResolver().apply {
            customRepos.map {
                val options = mutableMapOf<String, String>()

                if (it.password.isNotBlank() && it.user.isNotBlank()) {
                    options[DependenciesResolverOptionsName.USERNAME] = it.user
                    options[DependenciesResolverOptionsName.PASSWORD] = it.password
                }

                addRepository(RepositoryCoordinates(it.url), makeExternalDependenciesResolverOptions(options))
            }
        }

        val resolver =
            CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver(), mvnResolver)

        val resolvedDependencies = runBlocking {
            depIds.map {
                infoMsg("Resolving $it...")
                resolver.resolve(it.value)
            }.map { it.valueOrThrow() }
        }.flatten()

        return resolvedDependencies
    }
}
