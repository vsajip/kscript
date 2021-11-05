package kscript.app

import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

class Cache(private val path: Path) {
    fun fetch(url: URL): String {
        val hash = md5(url.toString())
        val cachedFile = path.resolve("uri_cache_${hash}").toFile()

        if (cachedFile.exists()) {
            return cachedFile.readText()
        }

        val urlContent = url.readText()
        cachedFile.writeText(urlContent)

        return urlContent
    }

    fun clear() {
        path.toFile().listFiles()?.forEach { it.delete() }
    }
}

class AppDir(path: Path) {
    private val cacheDir = path.resolve("cache")

    init {
        Files.createDirectories(cacheDir)
    }

    val cache = Cache(cacheDir)
}
