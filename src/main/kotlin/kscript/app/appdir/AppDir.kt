package kscript.app.appdir

import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path

class AppDir(path: Path) {
    private val urlCachePath = path.resolve("url")
    private val projectCachePath = path.resolve("project")

    init {
        Files.createDirectories(urlCachePath)
        Files.createDirectories(projectCachePath)
    }

    val uriCache = UriCache(urlCachePath)
    val projectCache = ProjectCache(projectCachePath)

    fun clearCaches() {
        uriCache.clear()
        projectCache.clear()
    }
}
