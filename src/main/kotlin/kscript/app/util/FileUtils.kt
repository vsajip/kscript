package kscript.app.util

import kscript.app.model.OsType
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

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

    fun nativeToShellPath(osType: OsType, path: Path): String {
        val pathString = path.absolutePathString()

        return when (osType) {
            OsType.LINUX, OsType.DARWIN, OsType.WINDOWS, OsType.FREEBSD -> pathString
            OsType.CYGWIN, OsType.MSYS -> {
                val match =
                    Regex("^([A-Za-z]):\\\\(.*)").find(pathString)
                        ?: throw IllegalStateException("Can not resolve path: $pathString")
                var (extractedDrive, extractedPath) = match.destructured

                extractedPath = extractedPath.replace('\\', '/')

                if (osType == OsType.CYGWIN) {
                    "/cygdrive/${extractedDrive.lowercase()}/$extractedPath"
                } else {
                    "/${extractedDrive.lowercase()}/$extractedPath"
                }
            }
        }
    }

    //I handle only absolute path with drives !!!
    fun shellToNativePath(osType: OsType, path: String?): Path? {
        path ?: return null

        if (osType.isWindowsLike() || osType.isUnixLike()) {
            return Paths.get(path)
        }

        if (osType == OsType.MSYS) {
            val drive = path[1]
            val converted = path.drop(2).replace('/', '\\')

            return Paths.get("$drive:$converted")
        }

        //osType == OsType.CYGWIN
        TODO()
    }

    fun isPossibleJarPath(path: String): Boolean {
        // Checks could be changed to e.g. look for a .jar extension, allow relative paths etc.
        val f = File(path)

        return f.exists() && !f.isDirectory && f.isAbsolute
    }
}
