package kscript.app.appdir

import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path

class AppDir(path: Path) {
    private val urlCachePath = path.resolve("url")
    private val jarCachePath = path.resolve("jar")
    private val ideaCachePath = path.resolve("idea")
    private val projectCachePath = path.resolve("project")
    private val dependencyCachePath = path.resolve("dependency_cache.txt")

    init {
        Files.createDirectories(urlCachePath)
        Files.createDirectories(jarCachePath)
        FileUtils.cleanDirectory(jarCachePath.toFile())
        Files.createDirectories(ideaCachePath)
        Files.createDirectories(projectCachePath)
        dependencyCachePath.toFile().createNewFile()
    }

    val uriCache = UriCache(urlCachePath)
    val projectCache = ProjectCache(projectCachePath)
    val ideaCache = IdeaCache(ideaCachePath, uriCache)
    val jarCache = JarCache(jarCachePath)
    val dependencyCache = DependencyCache(dependencyCachePath)

    fun clearCaches() {
        uriCache.clear()
        ideaCache.clear()
        jarCache.clear()
        Files.deleteIfExists(dependencyCachePath)
    }
}
