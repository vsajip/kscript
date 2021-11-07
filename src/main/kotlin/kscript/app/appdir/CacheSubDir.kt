package kscript.app.appdir

import kscript.app.md5
import org.apache.commons.codec.digest.DigestUtils.md5Hex
import java.io.File
import java.net.URL
import java.nio.file.Path

class CacheSubDir(private val path: Path) {
    fun code(url: URL): String {
        val hash = md5Hex(url.toString())
        val cachedFile = path.resolve("url_cache_$hash").toFile()

        if (cachedFile.exists()) {
            return cachedFile.readText()
        }

        val urlContent = url.readText()
        cachedFile.writeText(urlContent)

        return urlContent
    }

    fun scriplet(code: String, extension: String) : Path {
        val hash = md5Hex(code)
        val cachedFile = path.resolve("scriplet_$hash.$extension").toFile()

        if (cachedFile.exists()) {
            return cachedFile.toPath()
        }

        cachedFile.writeText(code)
        return cachedFile.toPath()
    }

    fun clear() {
        path.toFile().listFiles()?.forEach { it.delete() }
    }
}
