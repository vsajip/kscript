package kscript.app.util

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

object FileUtils {
    fun createFile(path: Path, content: String): Path {
        createDirsIfNeeded(path)
        path.writeText(content)
        return path
    }

    fun symLinkOrCopy(link: Path, target: Path): Path {
        createDirsIfNeeded(link)

        val isSymlinked = createSymLink(link, target)

        if (!isSymlinked) {
            Logger.warnMsg("Failed to create symbolic link to script. Copying instead...")
            target.copyTo(link)
        }

        return link
    }

    fun createDirsIfNeeded(path: Path) {
        val dir = path.parent

        if (!dir.exists()) {
            dir.createDirectories()
        }
    }

    fun createSymLink(link: Path, target: Path): Boolean {
        return try {
            Files.createSymbolicLink(link, target)
            true
        } catch (e: IOException) {
            false
        }
    }
}
