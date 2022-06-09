package kscript.app.util

import kscript.app.model.OsType

private const val alphaChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

object OsPathUtils {
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

    fun create(path: String, osType: OsType): OsPath {
        require(path.isNotEmpty()) {
            "Path must not be empty"
        }

        val pathSeparatorCharacter = if (osType.isUnixLike()) {
            '/'
        } else {
            '\\'
        }

        val pathParts = path.split(pathSeparatorCharacter).toMutableList()

        //Validate root element of path and find out if it is absolute or relative
        val rootElementSize: Int
        val isAbsolute: Boolean

        when {
            pathParts[0] == ".." || pathParts[0] == "." -> {
                isAbsolute = false
                rootElementSize = pathParts[0].length
            }
            osType.isUnixLike() && path.startsWith("/") -> {
                //After split first element is empty for absolute paths on Linux; assigning correct value below
                pathParts[0] = "/"
                isAbsolute = true
                rootElementSize = 1
            }
            osType.isWindowsLike() && pathParts[0].length == 2 && pathParts[0][1] == ':' && alphaChars.contains(
                pathParts[0][0]
            ) -> {
                pathParts[0] = pathParts[0] + pathSeparatorCharacter
                isAbsolute = true
                rootElementSize = 3
            }
            else -> throw IllegalArgumentException("Invalid root element of path: '${pathParts[0]}'")
        }

        //Last path element can be empty (trailing path separator); removing it
        if (pathParts.last().isBlank()) {
            pathParts.removeAt(pathParts.size - 1)
        }

        //Make sure that there are no "empty parts" in path
        require(pathParts.find { it.isEmpty() } == null) {
            "Duplicated path separators or empty path names in '$path'"
        }

        val osSpecificForbiddenCharacters =
            mutableSetOf(if (osType.isUnixLike()) '\\' else '/').plus(forbiddenCharacters)
        val forbiddenCharacter = path.substring(rootElementSize).find { osSpecificForbiddenCharacters.contains(it) }

        require(forbiddenCharacter == null) {
            "Invalid characters in path: '$forbiddenCharacter'"
        }

        val pathType = if (isAbsolute) PathType.ABSOLUTE else PathType.RELATIVE

        return OsPath(osType, pathType, normalize(path, pathParts, pathType), pathSeparatorCharacter)
    }

    private fun normalize(path: String, pathParts: List<String>, pathType: PathType): List<String> {
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
