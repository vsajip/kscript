package kscript.app.parser

import kscript.app.model.*
import kscript.app.util.FileUtils

object LineParser {
    private val sheBang = listOf(SheBang)

    fun parseSheBang(line: String): List<ScriptAnnotation> {
        if (line.startsWith("#!/")) {
            return sheBang
        }
        return emptyList()
    }

    fun parseInclude(line: String): List<ScriptAnnotation> {
        val fileInclude = "@file:Include"
        val include = "//INCLUDE "

        line.trim().let {
            return when {
                it.startsWith(fileInclude) -> listOf(
                    Include(extractQuotedValueInParenthesis(it.substring(fileInclude.length)))
                )
                it.startsWith(include) -> listOf(Include(extractValue(it.substring(include.length))))
                else -> emptyList()
            }
        }
    }

    private fun validateDependency(dependency: String): String {
        // First check for a file that exists (potentially a jar)
        if (FileUtils.isPossibleJarPath(dependency)) {
            return dependency
        }
        val regex = Regex("^([^:]*):([^:]*):([^:@]*)(:(.*))?(@(.*))?\$")
        regex.find(dependency) ?: throw ParseException(
            "Invalid dependency locator: '${dependency}'. Expected an absolute path to a jar or a string with the format groupId:artifactId:version[:classifier][@type]"
        )
        return dependency
    }

    fun parseDependency(line: String): List<ScriptAnnotation> {
        val fileDependsOn = "@file:DependsOn"
        val fileDependsOnMaven = "@file:DependsOnMaven"
        val depends = "//DEPS "

        line.trim().let { s ->
            val dependencies = when {
                s.startsWith(fileDependsOnMaven) -> extractQuotedValuesInParenthesis(s.substring(fileDependsOnMaven.length))
                s.startsWith(fileDependsOn) -> extractQuotedValuesInParenthesis(s.substring(fileDependsOn.length))
                s.startsWith(depends) -> extractValues(s.substring(depends.length))
                else -> emptyList()
            }

            return dependencies.map {
                val validated = validateDependency(it)
                Dependency(validated)
            }
        }
    }

    fun parseEntry(line: String): List<ScriptAnnotation> {
        val fileEntry = "@file:EntryPoint"
        val entry = "//ENTRY "

        line.trim().let {
            return when {
                it.startsWith(fileEntry) -> listOf(
                    Entry(extractQuotedValueInParenthesis(it.substring(fileEntry.length)))
                )
                it.startsWith(entry) -> listOf(Entry(extractValue(it.substring(entry.length))))
                else -> emptyList()
            }
        }
    }

    fun parseRepository(line: String): List<ScriptAnnotation> {
        //Format:
        // @file:MavenRepository("imagej", "http://maven.imagej.net/content/repositories/releases/")
        // @file:MavenRepository("imagej", "http://maven.imagej.net/content/repositories/releases/", user="user", password="pass")

        val fileMavenRepository = "@file:MavenRepository"

        line.trim().let {
            return when {
                it.startsWith(fileMavenRepository) -> {
                    val value = it.substring(fileMavenRepository.length).substringBeforeLast(")")


                    val repository = value.split(",").map { it.trim(' ', '"', '(') }.let { annotationParams ->
                        val keyValSep = "[ ]*=[ ]*\"".toRegex()

                        val namedArgs = annotationParams.filter { it.contains(keyValSep) }.map { keyVal ->
                            keyVal.split(keyValSep).map { it.trim(' ', '\"') }.let { it.first() to it.last() }
                        }.toMap()

                        if (annotationParams.size < 2) {
                            throw ParseException(
                                "Missing ${2 - annotationParams.size} of the required arguments for @file:MavenRepository(id, url)"
                            )
                        }

                        Repository(
                            namedArgs.getOrDefault("id", annotationParams[0]),
                            namedArgs.getOrDefault("url", annotationParams[1]),
                            namedArgs.getOrDefault("user", annotationParams.getOrNull(2) ?: ""),
                            namedArgs.getOrDefault("password", annotationParams.getOrNull(3) ?: "")
                        )
                    }
                    return listOf(repository)
                }
                else -> emptyList()
            }
        }
    }

    fun parseKotlinOpts(line: String): List<ScriptAnnotation> {
        val fileKotlinOpts = "@file:KotlinOpts"
        val kotlinOpts = "//KOTLIN_OPTS "

        line.trim().let {
            return when {
                it.startsWith(fileKotlinOpts) -> extractQuotedValuesInParenthesis(it.substring(fileKotlinOpts.length)).map {
                    KotlinOpt(it)
                }

                it.startsWith(kotlinOpts) -> extractValues(it.substring(kotlinOpts.length)).map { KotlinOpt(it) }
                else -> emptyList()
            }
        }
    }

    fun parseCompilerOpts(line: String): List<ScriptAnnotation> {
        val fileCompilerOpts = "@file:CompilerOpts"
        val compilerOpts = "//COMPILER_OPTS "

        line.trim().let {
            return when {
                it.startsWith(fileCompilerOpts) -> extractQuotedValuesInParenthesis(it.substring(fileCompilerOpts.length)).map {
                    CompilerOpt(it)
                }

                it.startsWith(compilerOpts) -> extractValues(it.substring(compilerOpts.length)).map {
                    CompilerOpt(it)
                }
                else -> emptyList()
            }
        }
    }

    fun parsePackage(line: String): List<ScriptAnnotation> {
        val packagePrefix = "package "

        line.trim().let {
            if (it.startsWith(packagePrefix)) {
                return listOf(PackageName(it.substring(packagePrefix.length)))
            }
            return emptyList()
        }
    }

    fun parseImport(line: String): List<ScriptAnnotation> {
        val importPrefix = "import "

        line.trim().let {
            if (it.startsWith(importPrefix)) {
                return listOf(ImportName(it.substring(importPrefix.length)))
            }
            return emptyList()
        }
    }

    private fun extractQuotedValueInParenthesis(string: String): String {
        val result = extractQuotedValuesInParenthesis(string)

        if (result.size != 1) {
            throw ParseException("Expected single value, but get ${result.size}")
        }

        return result[0]
    }

    private fun extractQuotedValuesInParenthesis(string: String): List<String> {
        // https://stackoverflow.com/questions/171480/regex-grabbing-values-between-quotation-marks

        if (!string.startsWith("(")) {
            throw ParseException("Missing parenthesis")
        }

        val annotationArgs = """(["'])(\\?.*?)\1""".toRegex().findAll(string.drop(1)).toList().map {
            it.groupValues[2].trim()
        }

        // fail if any argument is a comma separated list of artifacts (see #101)
        annotationArgs.filter { it.contains(",[^)]".toRegex()) }.let {
            if (it.isNotEmpty()) {
                throw ParseException(
                    "Artifact locators must be provided as separate annotation arguments and not as comma-separated list: $it"
                )
            }
        }

        return annotationArgs
    }

    private fun extractValue(string: String): String {
        val result = extractValues(string)

        if (result.size != 1) {
            throw ParseException("Expected single value, but get ${result.size}")
        }

        return result[0]
    }

    fun extractValues(string: String): List<String> {
        string.trim().let {
            return it.split(",(?=(?:[^']*'[^']*')*[^']*\$)".toRegex()).map(String::trim).filter(String::isNotBlank)
        }
    }
}
