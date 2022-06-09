package kscript.app.util

import kscript.app.model.OsType

private const val alphaChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

object OsPathExt {
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

    private val windowsDrive = Regex("^([a-zA-Z]:).*$")

    private const val fileRegexString = "[a-zA-Z\\d !\$#%&'()+,\\-.=@\\[\\]^{}]+"
    private const val rootPosixRegexString = "(/|\\.\\.|\\.)"
    private const val rootWindowsRegexString = "([a-zA-Z]:|\\.\\.|\\.)"

    fun create(path: String, osType: OsType): OsPath {
        require(path.isNotEmpty()) {
            "Path must not be empty"
        }

        val pathSeparatorCharacter: Char
        val isAbsolute: Boolean

        if (osType.isUnixLike()) {
            pathSeparatorCharacter = '/'
            isAbsolute = path.startsWith(pathSeparatorCharacter)
        } else {
            //Windows
            pathSeparatorCharacter = '\\'
            isAbsolute = (path.length >= 2 && path[1] == ':' && alphaChars.contains(path[0]))
        }

        var rootElementSize = 0

        val pathParts = if (isAbsolute) {
            rootElementSize = if (osType.isUnixLike()) 1 else 3

            validatePathParts(path, buildList {
                add(path.substring(0, rootElementSize))
                addAll(path.substring(rootElementSize).split(pathSeparatorCharacter))
            })
        } else {
            validatePathParts(path, path.split(pathSeparatorCharacter))
        }

        require(pathParts.isNotEmpty())

        val forbiddenCharacter = path.substring(rootElementSize).find { forbiddenCharacters.contains(it) }

        if (forbiddenCharacter != null) {
            throw IllegalArgumentException("Invalid characters in path: '$forbiddenCharacter'")
        }

        val pathType = if (isAbsolute) PathType.ABSOLUTE else PathType.RELATIVE

        return OsPath(osType, pathType, normalize(pathParts, pathType), pathSeparatorCharacter)
    }

    private fun validatePathParts(path: String, pathParts: List<String>): List<String> {
        //Only last element can be empty
        val newList = if (pathParts.last().isBlank()) pathParts.subList(0, pathParts.size - 1) else pathParts

        require(newList.find { it.isEmpty() } == null) {
            "Duplicated path separators or empty path names in '$path'"
        }

        return newList
    }

    private fun normalize(pathParts: List<String>, pathType: PathType): List<String> {
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
                    throw IllegalArgumentException("Invalid path: after normalization it goes beyond root element.")
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

    fun stringPath(parts: List<String>, pathSeparator: Char): String {
        return parts[0] + parts.subList(1, parts.size).joinToString(pathSeparator.toString()) { it }
    }
}

fun OsPath.stringPath() = OsPathExt.stringPath(pathParts, pathSeparator)
