package kscript.app.appdir

import java.io.File
import java.nio.file.Path

class ProjectCache(private val path: Path) {
    fun projectDir(): File {
        val directory = File(path.toFile(), "kscript_project_${System.currentTimeMillis()}")
        directory.mkdirs()

        return directory
    }

    fun clear() {
        path.toFile().listFiles()?.forEach { it.delete() }
    }
}
