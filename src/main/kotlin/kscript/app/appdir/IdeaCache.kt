package kscript.app.appdir

import kscript.app.util.Logger
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.toPath

interface ProjectItem {
    val relativePath: String
}

data class FileItem(override val relativePath: String, val content: String) : ProjectItem
data class SymLinkItem(override val relativePath: String, val source: URI) : ProjectItem

class IdeaCache(private val path: Path, private val uriCache: UriCache) {

    fun ideaPath(code: String): Path? {
        val hash = DigestUtils.md5Hex(code)
        val directory = File(path.toFile(), "idea_$hash")
        return if (directory.exists()) directory.toPath() else null
    }

    fun ideaDir(code: String, projectItems: List<ProjectItem>): Path {
        val hash = DigestUtils.md5Hex(code)

        val directory = File(path.toFile(), "idea_$hash")

        if (directory.exists()) {
            return directory.toPath()
        }

        directory.mkdirs()

        for (projectItem in projectItems) {
            val file = path.resolve(directory.path).resolve(projectItem.relativePath).toFile()

            if (file.exists()) {
                continue
            }

            when (projectItem) {
                is FileItem -> file.writeText(projectItem.content)
                is SymLinkItem -> {
                    val target = projectItem.source.toPath().toFile()
                    val isSymlinked = createSymLink(file, target)

                    if (!isSymlinked) {
                        Logger.warnMsg("Failed to create symbolic link to script. Copying instead...")
                        target.copyTo(file)
                    }
                }
            }
        }

        return directory.toPath().toAbsolutePath()
    }

    fun clear() {
        path.toFile().listFiles()?.forEach { it.delete() }
    }

    private fun createSymLink(link: File, target: File): Boolean {
        return try {
            Files.createSymbolicLink(link.toPath(), target.absoluteFile.toPath())
            true
        } catch (e: IOException) {
            false
        }
    }
}
