package kscript.app.util

import kscript.app.model.OsType
import kscript.app.model.PathType

//Path representation for different OSes
class OsPath private constructor(
    val osType: OsType, val pathType: PathType, val pathParts: List<String>, val pathSeparator: Char
) {
    fun resolve(vararg pathParts: String): OsPath {
        return resolve(create(osType, pathParts.joinToString(pathSeparator.toString())))
    }

    //functionality:
    //* relative + relative
    //* absolute + relative
    //* throw relative + absolute
    fun resolve(path: OsPath): OsPath {
        require(osType == path.osType) {
            "Paths from different OS's: '${this.osType.name}' path can not be resolved with '${path.osType.name}' path"
        }

        require(!(pathType == PathType.RELATIVE && path.pathType == PathType.ABSOLUTE)) {
            "Can not resolve relative path '${stringPath()}' using absolute path '${path.stringPath()}'"
        }

        val newPath = stringPath() + pathSeparator + path.stringPath()
        val newPathParts = buildList {
            addAll(pathParts)
            addAll(path.pathParts)
        }

        return OsPath(osType, pathType, normalize(newPath, newPathParts, pathType), pathSeparator)
    }

    //Not all conversions make sense: only Windows to CygWin and Msys and vice versa
    fun convert(targetOsType: OsType): OsPath {
        if (this.osType == targetOsType || (this.osType.isPosixLike() && targetOsType.isPosixLike()) || (this.osType.isWindowsLike() && targetOsType.isWindowsLike())) {
            return OsPath(targetOsType, pathType, pathParts, pathSeparator)
        }

        val toPosix = osType.isWindowsLike() && targetOsType.isPosixHostedOnWindows()
        val fromPosix = osType.isPosixHostedOnWindows() && targetOsType.isWindowsLike()

        require(toPosix || fromPosix) {
            "Only conversion between Windows and Posix hosted on Windows paths are supported"
        }

        val newParts = mutableListOf<String>()

        when {
            toPosix -> {
                val drive: String

                if (pathType == PathType.ABSOLUTE) {
                    drive = pathParts[0][0].lowercase()

                    newParts.add("/")

                    if (targetOsType == OsType.CYGWIN) {
                        newParts.add("cygdrive")
                        newParts.add(drive)
                    } else {
                        newParts.add(drive)
                    }

                    newParts.addAll(pathParts.subList(1, pathParts.size))
                } else {
                    newParts.addAll(pathParts)
                }
            }
            fromPosix -> {
                if (pathType == PathType.ABSOLUTE) {
                    if (osType == OsType.CYGWIN) {
                        newParts.add(pathParts[2] + ":")
                        newParts.addAll(pathParts.subList(3, pathParts.size))
                    } else {
                        newParts.add(pathParts[1] + ":")
                        newParts.addAll(pathParts.subList(2, pathParts.size))
                    }
                } else {
                    newParts.addAll(pathParts)
                }
            }
            else -> throw IllegalArgumentException("Invalid conversion: ${pathType.name} to ${targetOsType.name}")
        }

        return OsPath(targetOsType, pathType, newParts, resolvePathSeparator(targetOsType))
    }

    fun stringPath(): String {
        if (osType.isPosixLike() && pathParts[0] == "/") {
            return "/" + pathParts.subList(1, pathParts.size).joinToString(pathSeparator.toString()) { it }
        }

        return pathParts.joinToString(pathSeparator.toString()) { it }
    }

    override fun toString(): String = stringPath()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OsPath

        if (osType != other.osType) return false
        if (pathType != other.pathType) return false
        if (pathParts != other.pathParts) return false
        if (pathSeparator != other.pathSeparator) return false

        return true
    }

    override fun hashCode(): Int {
        var result = osType.hashCode()
        result = 31 * result + pathType.hashCode()
        result = 31 * result + pathParts.hashCode()
        result = 31 * result + pathSeparator.hashCode()
        return result
    }

    companion object {
        //https://stackoverflow.com/questions/1976007/what-characters-are-forbidden-in-windows-and-linux-directory-names
        //The rule here is more strict than necessary, but it is at least good practice to follow such a rule.
        private val forbiddenCharacters = buildSet {
            add('<')
            add('>')
            add(':')
            add('"')
            add('|')
            add('?')
            add('*')
            for (i in 0 until 32) {
                add(i.toChar())
            }
        }

        private const val alphaChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

        fun resolvePathSeparator(osType: OsType) = if (osType.isPosixLike()) {
            '/'
        } else {
            '\\'
        }


        //Relaxed validation:
        //1. It doesn't matter if there is '/' or '\' used as path separator - both are treated he same
        //2. Duplicated or trailing slashes '/' and backslashes '\' are just ignored
        fun create(osType: OsType, vararg pathParts: String): OsPath {
            require(pathParts.isNotEmpty() && pathParts.none { it.isBlank() }) {
                "Path parts must not be empty"
            }

            val pathSeparatorCharacter = resolvePathSeparator(osType)

            val path = pathParts.joinToString(pathSeparatorCharacter.toString())
            val pathPartsResolved = path.split('/', '\\').toMutableList()

            //Validate root element of path and find out if it is absolute or relative
            val rootElementSize: Int
            val isAbsolute: Boolean

            when {
                pathPartsResolved[0] == ".." || pathPartsResolved[0] == "." -> {
                    isAbsolute = false
                    rootElementSize = pathPartsResolved[0].length
                }
                osType.isPosixLike() && path.startsWith("/") -> {
                    //After split first element is empty for absolute paths on Linux; assigning correct value below
                    pathPartsResolved[0] = "/"
                    isAbsolute = true
                    rootElementSize = 1
                }
                osType.isWindowsLike() && pathPartsResolved[0].length == 2 && pathPartsResolved[0][1] == ':' && alphaChars.contains(
                    pathPartsResolved[0][0]
                ) -> {
                    isAbsolute = true
                    rootElementSize = 2
                }
                else -> {
                    //This is relative path, but without preceding '..' or '.'
                    pathPartsResolved.add(0, ".")
                    isAbsolute = false
                    rootElementSize = 1
                }
            }

            //Remove empty path parts - there were duplicated or trailing slashes / backslashes in initial path
            pathPartsResolved.removeAll { it.isEmpty() }

            val forbiddenCharacter = path.substring(rootElementSize).find { forbiddenCharacters.contains(it) }

            require(forbiddenCharacter == null) {
                "Invalid character '$forbiddenCharacter' in path '$path'"
            }

            val pathType = if (isAbsolute) PathType.ABSOLUTE else PathType.RELATIVE

            return OsPath(osType, pathType, normalize(path, pathPartsResolved, pathType), pathSeparatorCharacter)
        }

        fun normalize(path: String, pathParts: List<String>, pathType: PathType): List<String> {
            //Relative:
            // ./../ --> ../
            // ./a/../ --> ./
            // ./a/ --> ./a
            // ../a --> ../a
            // ../../a --> ../../a

            //Absolute:
            // /../ --> invalid (above root)
            // /a/../ --> /

            val newParts = mutableListOf<String>()

            newParts.add(pathParts[0])
            var index = 1

            while (index < pathParts.size) {
                if (pathParts[index] == ".") {
                    //Just skip . without adding it to newParts
                } else if (pathParts[index] == "..") {
                    if (pathType == PathType.ABSOLUTE && newParts.size == 1) {
                        throw IllegalArgumentException("Path after normalization goes beyond root element: '$path'")
                    }
                    if (newParts.size > 0) {
                        when (newParts.last()) {
                            "." -> {
                                //It's the first element - other dots should be already removed before
                                newParts.removeAt(newParts.size - 1)
                                newParts.add("..")
                            }
                            ".." -> {
                                newParts.add("..")
                            }
                            else -> {
                                newParts.removeAt(newParts.size - 1)
                            }
                        }
                    } else {
                        newParts.add("..")
                    }
                } else {
                    newParts.add(pathParts[index])
                }

                index += 1
            }

            return newParts
        }
    }
}
