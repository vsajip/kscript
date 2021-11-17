package kscript.app.parser

import kscript.app.model.*
import kscript.app.util.Logger
import kscript.app.util.quit

object LineParser {
    fun parseSheBang(line: String): Section? {
        if (line.startsWith("#!/")) {
            return SheBang(line)
        }
        return null
    }

    fun parseInclude(line: String): Section? {
        val fileInclude = "@file:Include"
        val include = "//INCLUDE "

        line.trim().let {
            return when {
                it.startsWith(fileInclude) -> Include(
                    line, extractQuotedValueInParenthesis(it.substring(fileInclude.length))
                )
                it.startsWith(include) -> Include(line, extractValue(it.substring(include.length)))
                else -> null
            }
        }
    }

    fun parseDependency(line: String): Section? {
        val fileDependsOn = "@file:DependsOn"
        val fileDependsOnMaven = "@file:DependsOnMaven"
        val depends = "//DEPS "

        line.trim().let {
            return when {
                it.startsWith(fileDependsOnMaven) -> Dependency(
                    line, extractQuotedValuesInParenthesis(it.substring(fileDependsOnMaven.length))
                )
                it.startsWith(fileDependsOn) -> Dependency(
                    line, extractQuotedValuesInParenthesis(it.substring(fileDependsOn.length))
                )
                it.startsWith(depends) -> Dependency(line, extractValues(it.substring(depends.length)))
                else -> null
            }
        }
    }

    fun parseEntry(line: String): Section? {
        val fileEntry = "@file:EntryPoint"
        val entry = "//ENTRY "

        line.trim().let {
            return when {
                it.startsWith(fileEntry) -> Entry(line, extractQuotedValueInParenthesis(it.substring(fileEntry.length)))
                it.startsWith(entry) -> Entry(line, extractValue(it.substring(entry.length)))
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
                    val value = dropEnclosing(it.substring(fileMavenRepository.length), "(", ")")
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
        val fileKotlinOpts = "@file:KotlinOpts"
        val kotlinOpts = "//KOTLIN_OPTS "

        line.trim().let {
            return when {
                it.startsWith(fileKotlinOpts) -> KotlinOpts(
                    line, extractQuotedValuesInParenthesis(it.substring(fileKotlinOpts.length))
                )
                it.startsWith(kotlinOpts) -> KotlinOpts(line, extractValues(it.substring(kotlinOpts.length)))
                else -> null
            }
        }
    }

    fun parseCompilerOpts(line: String): Section? {
        val fileCompilerOpts = "@file:CompilerOpts"
        val compilerOpts = "//COMPILER_OPTS "

        line.trim().let {
            return when {
                it.startsWith(fileCompilerOpts) -> CompilerOpts(
                    line, extractQuotedValuesInParenthesis(it.substring(fileCompilerOpts.length))
                )
                it.startsWith(compilerOpts) -> CompilerOpts(line, extractValues(it.substring(compilerOpts.length)))
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

    private fun extractQuotedValueInParenthesis(string: String): String {
        val result = extractQuotedValuesInParenthesis(string)

        if (result.size != 1) {
            throw ParseError(string, "Expected single value, but get ${result.size}")
        }

        return result[0]
    }

    private fun extractQuotedValuesInParenthesis(string: String): List<String> {
        // https://stackoverflow.com/questions/171480/regex-grabbing-values-between-quotation-marks

        if (!string.startsWith("(")) {
            throw ParseError(string, "Missing parenthesis")
        }

        val annotationArgs = """(["'])(\\?.*?)\1""".toRegex()
            .findAll(string.drop(1)).toList().map {
                it.groupValues[2]
            }

        return annotationArgs
    }

    private fun extractValue(string: String, prefix: String = "", suffix: String = prefix): String {
        val result = extractValues(string, prefix, suffix)

        if (result.size != 1) {
            throw ParseError(string, "Expected single value, but get ${result.size}")
        }

        return result[0]
    }

    private fun extractValues(string: String, prefix: String = "", suffix: String = prefix): List<String> {
        string.trim().let {
            return it.split("[ ;,]+".toRegex()).map(String::trim)
        }
    }

    private fun dropEnclosing(string: String, prefix: String, suffix: String = prefix): String {
        string.trim().let {
            if (it.startsWith(prefix) && it.endsWith(suffix)) {
                return it.substring(prefix.length, it.length - suffix.length)
            }

            throw ParseError(string, "Value '$string' is not delimited with '$prefix' and '$suffix'")
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
