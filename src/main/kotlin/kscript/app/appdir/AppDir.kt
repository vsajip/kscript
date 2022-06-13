package kscript.app.appdir

import kscript.app.util.OsPath
import kscript.app.util.createDirectories
import kscript.app.util.toNativeFile
import org.apache.commons.io.FileUtils

class AppDir(osPath: OsPath) {
    private val cachePath = osPath.resolve("cache")

    init {
        cachePath.createDirectories()
    }

    val cache = Cache(cachePath)

    fun clearCache() {
        FileUtils.cleanDirectory(cachePath.toNativeFile())
    }
}
