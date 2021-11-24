package kscript.app.appdir

import org.apache.commons.io.FileUtils
import java.nio.file.Path
import kotlin.io.path.createDirectories

class AppDir(path: Path) {
    private val cachePath = path.resolve("cache")

    init {
        cachePath.createDirectories()
    }

    val cache = Cache(cachePath)

    fun clearCache() {
        FileUtils.cleanDirectory(cachePath.toFile())
    }
}
