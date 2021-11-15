package kscript.app.appdir

import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class JarCache(private val path: Path) {
    fun calculateJarFile(code: String) : File {
        val hash = DigestUtils.md5Hex(code)
        return path.resolve("jar$hash.jar").normalize().toAbsolutePath().toFile()
    }

    fun clear() {
        path.toFile().listFiles()?.forEach { it.delete() }
    }
}
