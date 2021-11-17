package kscript.app.resolver

import kscript.app.appdir.AppDir
import kscript.app.model.Dependency
import kscript.app.util.Logger
import java.io.File

class ClasspathResolver(
    private val classpathSeparator: String,
    private val appDir: AppDir,
    private val dependencyResolver: DependencyResolver
) {
    fun resolve(dependencyIds: Set<Dependency>): String {
        // if no dependencies were provided we stop here
        if (dependencyIds.isEmpty()) {
            return ""
        }

        val dependenciesHash = dependencyIds.toList().sortedBy { it.value }.joinToString(classpathSeparator)

        // Use cached classpath from previous run if present
        val cache = appDir.dependencyCache.read().lines().filter { it.isNotBlank() }
            .associateBy({ it.split(" ")[0] }, { it.split(" ")[1] })

        if (cache.containsKey(dependenciesHash)) {
            val cachedCP = cache.getValue(dependenciesHash)

            // Make sure that local dependencies have not been wiped since resolving them (like by deleting .m2) (see #146)
            if (cachedCP.split(classpathSeparator).all { File(it).exists() }) {
                return cachedCP
            }

            Logger.infoMsg("Detected missing dependencies in cache.")
        }


        Logger.infoMsg("Resolving dependencies...")

        try {
            val artifacts = dependencyResolver.resolve(dependencyIds)
            val classPath = artifacts.joinToString(classpathSeparator) { it.absolutePath }

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
