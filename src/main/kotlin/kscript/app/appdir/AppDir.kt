package kscript.app.appdir

import kscript.app.appdir.CacheSubDir
import kscript.app.appdir.TempSubDir
import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path

class AppDir(path: Path) {
    private val cacheSubDir = path.resolve("cache")
    private val tempSubDir = path.resolve("tmp")

    init {
        Files.createDirectories(cacheSubDir)

        Files.createDirectories(tempSubDir)
        FileUtils.cleanDirectory(tempSubDir.toFile())
    }

    val cache = CacheSubDir(cacheSubDir)
    val temp = TempSubDir(tempSubDir)
}
