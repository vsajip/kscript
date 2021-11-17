package kscript.app.parser

import kscript.app.model.*
import kscript.app.model.Annotation
import kscript.app.util.Logger
import kscript.app.util.quit

object LineParser {
    fun parseSheBang(line: String): List<Annotation> {
        if (line.startsWith("#!/")) {
            return listOf(SheBang)
        }
        return emptyList()
    }

    fun parseInclude(line: String): List<Annotation> {
        val fileInclude = "@file:Include"
        val include = "//INCLUDE "

        line.trim().let {
            return when {
                it.startsWith(fileInclude) -> listOf(
                    Include(
                        extractQuotedValueInParenthesis(it.substring(fileInclude.length))
                    )
                )
                it.startsWith(include) -> listOf(Include(extractValue(it.substring(include.length))))
                else -> emptyList()
            }
        }
    }

    private fun validateDependency(line: String, dependency: String): String {
        val regex = Regex("^([^:]*):([^:]*):([^:@]*)(:(.*))?(@(.*))?\$")
        regex.find(dependency) ?: throw ParseError(line,
            "Invalid dependency locator: '${dependency}'. Expected format is groupId:artifactId:version[:classifier][@type]"
        )
        return dependency
    }


    fun parseDependency(line: String): List<Annotation> {
        val fileDependsOn = "@file:DependsOn"
        val fileDependsOnMaven = "@file:DependsOnMaven"
        val depends = "//DEPS "

        line.trim().let { s ->
            val dependencies = when {
                s.startsWith(fileDependsOnMaven) -> extractQuotedValuesInParenthesis(
                    s.substring(fileDependsOnMaven.length)
                )
                s.startsWith(fileDependsOn) -> extractQuotedValuesInParenthesis(s.substring(fileDependsOn.length))
                s.startsWith(depends) -> extractValues(s.substring(depends.length))
                else -> emptyList()
            }

            return dependencies.map { Dependency(validateDependency(line, it)) }
        }
    }

    fun parseEntry(line: String): List<Annotation> {
        val fileEntry = "@file:EntryPoint"
        val entry = "//ENTRY "

        line.trim().let {
            return when {
                it.startsWith(fileEntry) -> listOf(Entry(extractQuotedValueInParenthesis(it.substring(fileEntry.length))))
                it.startsWith(entry) -> listOf(Entry(extractValue(it.substring(entry.length))))
                else -> emptyList()
            }
        }
    }

    fun parseRepository(line: String): List<Annotation> {
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
                            namedArgs.getOrDefault("id", annotationParams[0]),
                            decodeEnv(namedArgs.getOrDefault("url", annotationParams[1])),
                            decodeEnv(namedArgs.getOrDefault("user", annotationParams.getOrNull(2) ?: "")),
                            decodeEnv(namedArgs.getOrDefault("password", annotationParams.getOrNull(3) ?: ""))
                        )
                    }
                    return listOf(repository)
                }
                else -> emptyList()
            }
        }
    }

    fun parseKotlinOpts(line: String): List<Annotation> {
        val fileKotlinOpts = "@file:KotlinOpts"
        val kotlinOpts = "//KOTLIN_OPTS "

        line.trim().let {
            return when {
                it.startsWith(fileKotlinOpts) -> extractQuotedValuesInParenthesis(it.substring(fileKotlinOpts.length)).map {
                    KotlinOpt(
                        it
                    )
                }

                it.startsWith(kotlinOpts) -> extractValues(it.substring(kotlinOpts.length)).map { KotlinOpt(it) }
                else -> emptyList()
            }
        }
    }

    fun parseCompilerOpts(line: String): List<Annotation> {
        val fileCompilerOpts = "@file:CompilerOpts"
        val compilerOpts = "//COMPILER_OPTS "

        line.trim().let {
            return when {
                it.startsWith(fileCompilerOpts) -> extractQuotedValuesInParenthesis(it.substring(fileCompilerOpts.length)).map {
                    CompilerOpt(
                        it
                    )
                }

                it.startsWith(compilerOpts) -> extractValues(it.substring(compilerOpts.length)).map { CompilerOpt(it) }
                else -> emptyList()
            }
        }
    }

    fun parsePackage(line: String): List<Annotation> {
        val packagePrefix = "package "

        line.trim().let {
            if (it.startsWith(packagePrefix)) {
                return listOf(Package(it.substring(packagePrefix.length)))
            }
            return emptyList()
        }
    }

    fun parseImport(line: String): List<Annotation> {
        val importPrefix = "import "

        line.trim().let {
            if (it.startsWith(importPrefix)) {
                return listOf(Import(it.substring(importPrefix.length)))
            }
            return emptyList()
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

        val annotationArgs = """(["'])(\\?.*?)\1""".toRegex().findAll(string.drop(1)).toList().map {
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
