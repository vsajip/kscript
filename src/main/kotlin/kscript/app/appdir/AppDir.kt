package kscript.app.appdir

import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path

class AppDir(path: Path) {
    private val urlCachePath = path.resolve("url")
    private val jarCachePath = path.resolve("jar")
    private val projectCachePath = path.resolve("project")
    private val dependencyCachePath = path.resolve("dependency_cache.txt")

    init {
        Files.createDirectories(urlCachePath)
        Files.createDirectories(jarCachePath)
        FileUtils.cleanDirectory(jarCachePath.toFile())
        Files.createDirectories(projectCachePath)
        dependencyCachePath.toFile().createNewFile()
    }

    val uriCache = UriCache(urlCachePath)
    val jarCache = JarCache(jarCachePath)
    val projectCache = ProjectCache(projectCachePath)
    val dependencyCache = DependencyCache(dependencyCachePath)

    fun clear() {
        uriCache.clear()
        jarCache.clear()
        projectCache.clear()
        Files.deleteIfExists(dependencyCachePath)
    }
}
