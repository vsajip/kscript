package kscript.app.appdir

import kscript.app.util.OsPath
import kscript.app.util.path
import org.apache.commons.io.FileUtils
import kotlin.io.path.createDirectories

class AppDir(path: OsPath) {
    private val cachePath = path.resolve("cache").path()

    init {
        cachePath.createDirectories()
    }

    val cache = Cache(cachePath)

    fun clearCache() {
        FileUtils.cleanDirectory(cachePath.toFile())
    }
}
