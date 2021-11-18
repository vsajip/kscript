package kscript.app.parser

import kscript.app.model.*
import kscript.app.model.Annotation
import kscript.app.util.Logger
import kscript.app.util.quit

object LineParser {
    private val sheBang = listOf(SheBang)

    fun parseSheBang(line: String): List<Annotation> {
        if (line.startsWith("#!/")) {
            return sheBang
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
                        extractQuotedValueInParenthesis(line, it.substring(fileInclude.length))
                    )
                )
                it.startsWith(include) -> listOf(Include(extractValue(line, it.substring(include.length))))
                else -> emptyList()
            }
        }
    }

    private fun validateDependency(line: String, dependency: String): String {
        val regex = Regex("^([^:]*):([^:]*):([^:@]*)(:(.*))?(@(.*))?\$")
        regex.find(dependency) ?: throw ParseException(
            line,
            "Invalid dependency locator: '${dependency}'. Expected format is groupId:artifactId:version[:classifier][@type]"
        )
        return dependency
    }

    fun formatVersion(version: String): String {
        // replace + with open version range for maven
        return version.let { it ->
            if (it.endsWith("+")) {
                "[${it.dropLast(1)},)"
            } else {
                it
            }
        }
    }

    fun parseDependency(line: String): List<Annotation> {
        val fileDependsOn = "@file:DependsOn"
        val fileDependsOnMaven = "@file:DependsOnMaven"
        val depends = "//DEPS "

        line.trim().let { s ->
            val dependencies = when {
                s.startsWith(fileDependsOnMaven) -> extractQuotedValuesInParenthesis(line, s.substring(fileDependsOnMaven.length))
                s.startsWith(fileDependsOn) -> extractQuotedValuesInParenthesis(line, s.substring(fileDependsOn.length))
                s.startsWith(depends) -> extractValues(line, s.substring(depends.length))
                else -> emptyList()
            }

            return dependencies.map {
                val validated = validateDependency(line, it)
                Dependency(validated)
            }
        }
    }

    fun parseEntry(line: String): List<Annotation> {
        val fileEntry = "@file:EntryPoint"
        val entry = "//ENTRY "

        line.trim().let {
            return when {
                it.startsWith(fileEntry) -> listOf(Entry(extractQuotedValueInParenthesis(line, it.substring(fileEntry.length))))
                it.startsWith(entry) -> listOf(Entry(extractValue(line, it.substring(entry.length))))
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
                    val value = dropEnclosing(line, it.substring(fileMavenRepository.length), "(", ")")
                    val repository = value.split(",").map { it.trim(' ', '"', '(') }.let { annotationParams ->
                        val keyValSep = "[ ]*=[ ]*\"".toRegex()

                        val namedArgs = annotationParams.filter { it.contains(keyValSep) }.map { keyVal ->
                            keyVal.split(keyValSep).map { it.trim(' ', '\"') }.let { it.first() to it.last() }
                        }.toMap()

                        if (annotationParams.size < 2) {
                            throw ParseException(
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
                it.startsWith(fileKotlinOpts) -> extractQuotedValuesInParenthesis(line, it.substring(fileKotlinOpts.length)).map {
                    KotlinOpt(
                        it
                    )
                }

                it.startsWith(kotlinOpts) -> extractValues(line, it.substring(kotlinOpts.length)).map { KotlinOpt(it) }
                else -> emptyList()
            }
        }
    }

    fun parseCompilerOpts(line: String): List<Annotation> {
        val fileCompilerOpts = "@file:CompilerOpts"
        val compilerOpts = "//COMPILER_OPTS "

        line.trim().let {
            return when {
                it.startsWith(fileCompilerOpts) -> extractQuotedValuesInParenthesis(line, it.substring(fileCompilerOpts.length)).map {
                    CompilerOpt(
                        it
                    )
                }

                it.startsWith(compilerOpts) -> extractValues(line, it.substring(compilerOpts.length)).map { CompilerOpt(it) }
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

    private fun extractQuotedValueInParenthesis(line: String, string: String): String {
        val result = extractQuotedValuesInParenthesis(line, string)

        if (result.size != 1) {
            throw ParseException(line, "Expected single value, but get ${result.size}")
        }

        return result[0]
    }

    private fun extractQuotedValuesInParenthesis(line: String, string: String): List<String> {
        // https://stackoverflow.com/questions/171480/regex-grabbing-values-between-quotation-marks

        if (!string.startsWith("(")) {
            throw ParseException(line, "Missing parenthesis")
        }

        val annotationArgs = """(["'])(\\?.*?)\1""".toRegex().findAll(string.drop(1)).toList().map {
            it.groupValues[2]
        }

        // fail if any argument is a comma separated list of artifacts (see #101)
        annotationArgs.filter { it.contains(",[^)]".toRegex()) }.let {
            if (it.isNotEmpty()) {
                throw ParseException(line, "Artifact locators must be provided as separate annotation arguments and not as comma-separated list: $it")
            }
        }

        return annotationArgs
    }

    private fun extractValue(line: String, string: String, prefix: String = "", suffix: String = prefix): String {
        val result = extractValues(line, string, prefix, suffix)

        if (result.size != 1) {
            throw ParseException(line, "Expected single value, but get ${result.size}")
        }

        return result[0]
    }

    private fun extractValues(line: String, string: String, prefix: String = "", suffix: String = prefix): List<String> {
        string.trim().let {
            return it.split("[ ;,]+".toRegex()).map(String::trim)
        }
    }

    private fun dropEnclosing(line: String, string: String, prefix: String, suffix: String = prefix): String {
        string.trim().let {
            if (it.startsWith(prefix) && it.endsWith(suffix)) {
                return it.substring(prefix.length, it.length - suffix.length)
            }

            throw ParseException(line, "Value '$string' is not delimited with '$prefix' and '$suffix'")
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
