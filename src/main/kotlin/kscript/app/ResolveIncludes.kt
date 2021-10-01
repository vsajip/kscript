package kscript.app

import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.net.URL

/**
 * @author Holger Brandl
 * @author Ilan Pillemer
 * @author Marcin Kuszczak
 */

const val PACKAGE_STATEMENT_PREFIX = "package "
const val IMPORT_STATEMENT_PREFIX = "import " // todo make more solid by using operator including regex

data class IncludeResult(val scriptFile: File, val includes: List<URL> = emptyList())

/** Resolve include declarations in a script file. Resolved script will be put into another temporary script */
fun resolveIncludes(file: File): IncludeResult {
    val includes = mutableListOf<URI>()
    val lines = resolve(file.toURI(), includes)
    val script = Script(lines)

    return IncludeResult(script.consolidateStructure().createTmpScript(), includes.map { it.toURL() })
}

private fun resolve(scriptUri: URI, includes: MutableList<URI>): List<String> {
    val lines = readLinesOrThrow(scriptUri)
    val scriptDir = scriptUri.resolve(".")
    val result = mutableListOf<String>()

    for (line in lines) {
        if (isIncludeDirective(line)) {
            val include = extractIncludeTarget(line)

            val includeURI = when {
                isUrl(include) -> URL(include).toURI()
                include.startsWith("/") -> File(include).toURI()
                include.startsWith("~/") -> File(System.getenv("HOME")!! + include.substring(1)).toURI()
                else -> scriptDir.resolve(URI(include.removePrefix("./")))
            }

            // test if include was processed already (aka include duplication, see #151)
            if (includes.map { it.path }.contains(includeURI.path)) {
                // include was already resolved, so we return just continue
                continue
            }

            includes.add(includeURI)

            val resolvedLines = resolve(includeURI, includes)
            result.addAll(resolvedLines)
            continue
        }

        result.add(line)
    }

    return result
}

private fun readLinesOrThrow(uri: URI): List<String> {
    try {
        return uri.toURL().readText().lines()
    } catch (e: FileNotFoundException) {
        errorMsg("Failed to resolve include with URI: '${uri}'")
        System.err.println(e.message?.lines()!!.map { it.prependIndent("[kscript] [ERROR] ") })
        quit(1)
    }
}

internal fun isUrl(s: String) = s.startsWith("http://") || s.startsWith("https://")

private const val INCLUDE_ANNOT_PREFIX = "@file:Include("

internal fun isIncludeDirective(line: String) = line.startsWith("//INCLUDE") || line.startsWith(INCLUDE_ANNOT_PREFIX)

internal fun extractIncludeTarget(incDirective: String) = when {
    incDirective.startsWith(INCLUDE_ANNOT_PREFIX) -> incDirective.replaceFirst(INCLUDE_ANNOT_PREFIX, "")
        .split(")")[0].trim(' ', '"')
    else -> incDirective.split("[ ]+".toRegex()).last()
}

/**
 * Basic launcher used for testing
 *
 *
 * Usage Example:
 * ```
 * cd $KSCRIPT_HOME
 * ./gradlew assemble
 * resolve_inc() { kotlin -classpath build/libs/kscript.jar kscript.app.ResolveIncludes "$@";}
 * resolve_inc /Users/brandl/projects/kotlin/kscript/test/resources/includes/include_variations.kts
 * cat $(resolve_inc /Users/brandl/projects/kotlin/kscript/test/resources/includes/include_variations.kts 2>&1)
 * ```
 */
object ResolveIncludes {
    @JvmStatic
    fun main(args: Array<String>) {
        System.err.println(resolveIncludes(File(args[0])).scriptFile.readText())
    }
}
