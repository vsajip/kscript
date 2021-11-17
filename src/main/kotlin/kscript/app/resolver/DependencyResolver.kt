package kscript.app.resolver

import kotlinx.coroutines.runBlocking
import kscript.app.appdir.AppDir
import kscript.app.model.Config
import kscript.app.model.Repository
import kscript.app.util.Logger.errorMsg
import kscript.app.util.Logger.infoMsg
import kscript.app.util.quit
import java.io.File
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.dependencies.maven.MavenRepositoryCoordinates

class DependencyResolver(private val config: Config, private val appDir: AppDir) {

    fun resolveClasspath(dependencyIds: Set<String>, repositories: Set<Repository>): String {
        // if no dependencies were provided we stop here
        if (dependencyIds.isEmpty()) {
            return ""
        }

        val dependenciesHash = dependencyIds.toList().sorted().joinToString(config.classPathSeparator)


        // Use cached classpath from previous run if present
        val cache = appDir.dependencyCache.read().lines().filter { it.isNotBlank() }
            .associateBy({ it.split(" ")[0] }, { it.split(" ")[1] })

        if (cache.containsKey(dependenciesHash)) {
            val cachedCP = cache.getValue(dependenciesHash)

            // Make sure that local dependencies have not been wiped since resolving them (like by deleting .m2) (see #146)
            if (cachedCP.split(config.classPathSeparator).all { File(it).exists() }) {
                return cachedCP
            }

            infoMsg("Detected missing dependencies in cache.")
        }


        infoMsg("Resolving dependencies...")

        try {
            val artifacts = resolveDependenciesViaKotlin(dependencyIds, repositories)
            val classPath = artifacts.map { it.absolutePath }.joinToString(config.classPathSeparator)

            infoMsg("Dependencies resolved")

            // Add classpath to cache
            appDir.dependencyCache.append("$dependenciesHash $classPath\n")

            // Print the classpath
            return classPath
        } catch (e: Exception) {
            infoMsg("Exception during dependency resolution... $e")
            throw e
        }
    }


    private fun resolveDependenciesViaKotlin(depIds: Set<String>, customRepos: Set<Repository>): List<File> {


        // validate dependencies
        depIds.map { depIdToArtifact(it) }

        val extRepos = customRepos //+ MavenRepo("jcenter", "https://jcenter.bintray.com")

        val repoCoords = extRepos.map { MavenRepositoryCoordinates(it.url, it.user, it.password, null, null) }

        val mvnResolver = MavenDependenciesResolver().apply {
            repoCoords.map { addRepository(it) }
        }

        val resolver =
            CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver(), mvnResolver)

        val resolvedDependencies = runBlocking {
            depIds.map {
                infoMsg("Resolving $it...")
                resolver.resolve(it)
            }.map { it.valueOrThrow() }
        }.flatten()

        return resolvedDependencies
    }


    private fun depIdToArtifact(depId: String) {
        val regex = Regex("^([^:]*):([^:]*):([^:@]*)(:(.*))?(@(.*))?\$")
        val matchResult = regex.find(depId)

        if (matchResult == null) {
            errorMsg("Invalid dependency locator: '${depId}'. Expected format is groupId:artifactId:version[:classifier][@type]")
            quit(1)
        }
    }

    fun formatVersion(version: String): String {
        // replace + with open version range for maven
        return version.let { it ->
            if (it.endsWith("+")) {
                "[${it.dropLast(1)},)"
            } else {
                it
            }
        }
    }

}
