package kscript.app.resolver

import kscript.app.appdir.AppDir
import kscript.app.model.Config
import kscript.app.model.Dependency
import kscript.app.model.Repository
import kscript.app.util.Logger
import java.io.File

class ClasspathResolver(private val config: Config, private val appDir: AppDir, private val dependencyResolver: DependencyResolver) {
    fun resolve(dependencyIds: Set<Dependency>, repositories: Set<Repository>): String {
        // if no dependencies were provided we stop here
        if (dependencyIds.isEmpty()) {
            return ""
        }

        val dependenciesHash = dependencyIds.toList().sortedBy { it.value }.joinToString(config.classPathSeparator)

        // Use cached classpath from previous run if present
        val cache = appDir.dependencyCache.read().lines().filter { it.isNotBlank() }
            .associateBy({ it.split(" ")[0] }, { it.split(" ")[1] })

        if (cache.containsKey(dependenciesHash)) {
            val cachedCP = cache.getValue(dependenciesHash)

            // Make sure that local dependencies have not been wiped since resolving them (like by deleting .m2) (see #146)
            if (cachedCP.split(config.classPathSeparator).all { File(it).exists() }) {
                return cachedCP
            }

            Logger.infoMsg("Detected missing dependencies in cache.")
        }


        Logger.infoMsg("Resolving dependencies...")

        try {
            val artifacts = dependencyResolver.resolve(dependencyIds, repositories)
            val classPath = artifacts.joinToString(config.classPathSeparator) { it.absolutePath }

            Logger.infoMsg("Dependencies resolved")

            // Add classpath to cache
            appDir.dependencyCache.append("$dependenciesHash $classPath\n")

            // Print the classpath
            return classPath
        } catch (e: Exception) {
            Logger.infoMsg("Exception during dependency resolution... $e")
            throw e
        }
    }
}
