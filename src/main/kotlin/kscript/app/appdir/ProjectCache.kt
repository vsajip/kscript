package kscript.app.appdir

import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.nio.file.Path

class ProjectCache(private val path: Path) {
    fun projectDir(): File {
        val directory = File(path.toFile(), "kscript_project_${System.currentTimeMillis()}")
        directory.mkdirs()

        return directory
    }

    fun projectDir(code: String): File {
        val hash = DigestUtils.md5Hex(code)
        val directory = File(path.toFile(), "project_$hash")
        directory.mkdirs()

        return directory
    }

    fun clear() {
        path.toFile().listFiles()?.forEach { it.delete() }
    }
}
