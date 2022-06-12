package kscript.app.appdir

import kscript.app.util.OsPath
import kscript.app.util.toNativePath
import org.apache.commons.io.FileUtils
import kotlin.io.path.createDirectories

class AppDir(osPath: OsPath) {
    private val cachePath = osPath.resolve("cache").toNativePath()

    init {
        cachePath.createDirectories()
    }

    val cache = Cache(cachePath)

    fun clearCache() {
        FileUtils.cleanDirectory(cachePath.toFile())
    }
}
