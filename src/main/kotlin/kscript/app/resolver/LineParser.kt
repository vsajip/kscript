package kscript.app.resolver

import kscript.app.model.*
import kscript.app.quit
import kscript.app.util.Logger

object LineParser {
    fun parseSheBang(line: String): Section? {
        if (line.startsWith("#!/")) {
            return SheBang(line)
        }
        return null
    }

    fun parseInclude(line: String): Section? {
        //Regex("^@file:Include\\(\"(.+)\"\\)$|^//INCLUDE\\s+(.+)$")
        val fileInclude = "@file:Include"
        val include = "//INCLUDE "

        line.trim().let {
            return when {
                it.startsWith(fileInclude) -> Include(line, it.substring(fileInclude.length))
                it.startsWith(include) -> Include(line, it.substring(include.length))
                else -> null
            }
        }
    }

    fun parseDependency(line: String): Section? {
        //Regex("^@file:DependsOn\\s*\\((.+)\\)\\s*$|^@file:DependsOnMaven\\s*\\((.+)\\)\\s*$|^//DEPS\\s+(.+)\\s*$")
        val fileDependsOn = "@file:DependsOn"
        val fileDependsOnMaven = "@file:DependsOnMaven"
        val depends = "//DEPS "

        line.trim().let {
            return when {
                it.startsWith(fileDependsOn) -> Dependency(
                    line, extractValues(it.substring(fileDependsOn.length), "\"") ?: throw ParseError(
                        line, "Value quoting error."
                    )
                )
                it.startsWith(fileDependsOnMaven) -> Dependency(
                    line, extractValues(it.substring(fileDependsOnMaven.length), "\"") ?: throw ParseError(
                        line, "Value quoting error."
                    )
                )
                it.startsWith(depends) -> Dependency(
                    line, extractValues(it.substring(depends.length)) ?: throw ParseError(
                        line, "Value quoting error."
                    )
                )
                else -> null
            }
        }
    }

    fun parseEntry(line: String): Section? {
        //Regex("^@file:EntryPoint\\(\"(.+)\"\\)")
        val fileEntry = "@file:EntryPoint"

        line.trim().let {
            return when {
                it.startsWith(fileEntry) -> Entry(line, it.substring(fileEntry.length))
                else -> null
            }
        }
    }

    fun parseRepository(line: String): Section? {
        //Format:
        // @file:MavenRepository("imagej", "http://maven.imagej.net/content/repositories/releases/")
        // @file:MavenRepository("imagej", "http://maven.imagej.net/content/repositories/releases/", user="user", password="pass")

        val fileMavenRepository = "@file:MavenRepository"

        line.trim().let {
            return when {
                it.startsWith(fileMavenRepository) -> {
                    val value = dropEnclosing(it.substring(fileMavenRepository.length), "(", ")") ?: throw ParseError(
                        line, "No (matching) parenthesis."
                    )

                    val repository = value.split(",").map { it.trim(' ', '"', '(') }.let { annotationParams ->
                        val keyValSep = "[ ]*=[ ]*\"".toRegex()

                        val namedArgs = annotationParams.filter { it.contains(keyValSep) }.map { keyVal ->
                            keyVal.split(keyValSep).map { it.trim(' ', '\"') }.let { it.first() to it.last() }
                        }.toMap()

                        if (annotationParams.size < 2) {
                            throw ParseError(
                                line,
                                "Missing ${2 - annotationParams.size} of the required arguments for @file:MavenRepository(id, url)"
                            )
                        }

                        Repository(
                            line,
                            namedArgs.getOrDefault("id", annotationParams[0]),
                            decodeEnv(namedArgs.getOrDefault("url", annotationParams[1])),
                            decodeEnv(namedArgs.getOrDefault("user", annotationParams.getOrNull(2) ?: "")),
                            decodeEnv(namedArgs.getOrDefault("password", annotationParams.getOrNull(3) ?: ""))
                        )
                    }
                    return repository
                }
                else -> null
            }
        }
    }

    fun parseKotlinOpts(line: String): Section? {
        //Regex("^@file:KotlinOpts(\"(.*)\")$|^//KOTLIN_OPTS\\s+(.*)$")
        val fileKotlinOpts = "@file:KotlinOpts"
        val kotlinOpts = "//KOTLIN_OPTS "

        line.trim().let {
            return when {
                it.startsWith(fileKotlinOpts) -> KotlinOpts(
                    line, extractValues(it.substring(fileKotlinOpts.length), "\"") ?: throw ParseError(
                        line, "Value quoting error."
                    )
                )
                it.startsWith(kotlinOpts) -> KotlinOpts(
                    line, extractValues(it.substring(kotlinOpts.length)) ?: throw ParseError(
                        line, "Value quoting error."
                    )
                )
                else -> null
            }
        }
    }

    fun parseCompilerOpts(line: String): Section? {
        //Regex("^@file:CompilerOpts(\"(.*)\")$|$//COMPILER_OPTS\\s+(.*)$")
        val fileCompilerOpts = "@file:CompilerOpts"
        val compilerOpts = "//COMPILER_OPTS "

        line.trim().let {
            return when {
                it.startsWith(fileCompilerOpts) -> CompilerOpts(
                    line, extractValues(it.substring(fileCompilerOpts.length), "\"") ?: throw ParseError(
                        line, "Value quoting error."
                    )
                )
                it.startsWith(compilerOpts) -> CompilerOpts(
                    line, extractValues(it.substring(compilerOpts.length)) ?: throw ParseError(
                        line, "Value quoting error."
                    )
                )
                else -> null
            }
        }
    }

    fun parsePackage(line: String): Section? {
        val packagePrefix = "package "

        line.trim().let {
            if (it.startsWith(packagePrefix)) {
                return Package(line, it.substring(packagePrefix.length))
            }
            return null
        }
    }

    fun parseImport(line: String): Section? {
        val importPrefix = "import "

        line.trim().let {
            if (it.startsWith(importPrefix)) {
                return Import(line, it.substring(importPrefix.length))
            }
            return null
        }
    }

    private fun extractValues(string: String, prefix: String = "", suffix: String = prefix): List<String>? {
        string.trim().let {
            return string.split(',').map { it.trim() }.map { dropEnclosing(it, prefix, suffix) ?: return null }
        }
    }

    private fun dropEnclosing(string: String, prefix: String, suffix: String = prefix): String? {
        string.trim().let {
            if (it.startsWith(prefix) && it.endsWith(suffix)) {
                return it.substring(prefix.length, it.length - suffix.length)
            }
            return null
        }
    }

    //TODO: externalize System.getenv below
    private fun decodeEnv(value: String): String {
        return if (value.startsWith("{{") && value.endsWith("}}")) {
            val envKey = value.substring(2, value.length - 2)
            val envValue = System.getenv()[envKey]
            if (null == envValue) {
                Logger.errorMsg("Could not resolve environment variable {{$envKey}} in maven repository credentials")
                quit(1)
            }
            envValue
        } else {
            value
        }
    }
}
