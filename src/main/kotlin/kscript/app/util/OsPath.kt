package kscript.app.util

import kscript.app.model.OsType
import java.nio.file.Path
import kotlin.io.path.absolutePathString

enum class PathType {
    ABSOLUTE, RELATIVE
}

//Path representation for different OSes
class OsPath internal constructor(
    val osType: OsType, val pathType: PathType, val pathParts: List<String>, val pathSeparator: Char
) {
    //functionality:
    //* relative + relative
    //* absolute + relative
    //* throw relative + absolute
    fun resolve(path: OsPath): OsPath = TODO()

    //Not all conversions make sense: only Windows to CygWin and Msys and vice versa
    fun convert(osType: OsType): OsPath {
        if (this.osType == osType || (this.osType.isUnixLike() && osType.isUnixLike())) {
            return this
        }

        osType.isUnixLike()




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

        require(this.osType == osType || this.osType == OsType.WINDOWS && osType.isUnixHostedOnWindows()) {
            "You can convert only Windows paths into Cygwin or Msys paths."
        }

        if (this.osType == osType) {
            return this
        }

        when (osType) {

        }

        TODO()
    }

    fun stringPath(): String =
        pathParts.joinToString(pathSeparator.toString()) { it }

    companion object {
        fun create(osType: OsType, path: String): OsPath = OsPathUtils.create(path, osType)
    }
}
