package kscript.app.appdir

import kscript.app.model.Script
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class ProjectCache(private val path: Path) {
    fun findOrCreate(script: Script): Path {
        val hash = calculateHash(script)
        val directory = path.resolve("project_$hash")
        return if (directory.exists()) directory else directory.createDirectories()
    }

    fun clear() {
        FileUtils.cleanDirectory(path.toFile())
    }

    private fun calculateHash(script: Script): String {
        val text =
            script.resolvedCode + script.repositories.joinToString("\n") + script.dependencies.joinToString("\n") + script.compilerOpts.joinToString(
                "\n"
            ) + script.kotlinOpts.joinToString(
                "\n"
            ) + (script.entryPoint ?: "")
        return DigestUtils.md5Hex(text)
    }
}
