package kscript.app.appdir

import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path

class AppDir(path: Path) {
    private val urlCachePath = path.resolve("url")
    private val jarCachePath = path.resolve("jar")
    private val projectCachePath = path.resolve("project")

    init {
        Files.createDirectories(urlCachePath)
        Files.createDirectories(jarCachePath)
        FileUtils.cleanDirectory(jarCachePath.toFile())
        Files.createDirectories(projectCachePath)
    }

    val urlCache = UrlCache(urlCachePath)
    val jarCache = JarCache(jarCachePath)
    val projectCache = ProjectCache(projectCachePath)

    fun clear() {
        urlCache.clear()
        jarCache.clear()
        projectCache.clear()
    }
}
