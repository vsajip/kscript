package kscript.app

import kscript.app.Logger.errorMsg
import java.io.File
import java.net.URI
import java.net.URL

/**
 * @author Holger Brandl
 * @author Ilan Pillemer
 * @author Marcin Kuszczak
 */

const val PACKAGE_STATEMENT_PREFIX = "package "
const val IMPORT_STATEMENT_PREFIX = "import " // todo make more solid by using operator including regex

data class IncludeResult(val scriptFile: Script, val includes: List<URL> = emptyList())

/** Resolve include declarations in a script file. Resolved script will be put into another temporary script */
fun resolveIncludes(scriptSource: ScriptSource): IncludeResult {
    val includes = mutableListOf<URI>()
    val lines = resolve(scriptSource.sourceType != SourceType.HTTP, scriptSource.codeText, scriptSource.includeContext, includes)
    val script = Script(lines, if (scriptSource.scriptType == ScriptType.KTS) "kts" else "kt")

    return IncludeResult(script.consolidateStructure(), includes.map { it.toURL() })
}

private fun resolve(allowFileReferences: Boolean, codeText: String, includeContext: URI, includes: MutableList<URI>): List<String> {
    val lines = codeText.lines()
    val result = mutableListOf<String>()

    for (line in lines) {
        if (isIncludeDirective(line)) {
            val include = extractTarget(line)
            val includeUri = resolveUri(includeContext, include)

            if (!allowFileReferences && isRegularFile(includeUri)) {
                errorMsg("References to local filesystem from remote scripts are not allowed.\nIn script: ; Reference: $includeUri")
                quit(1)
            }

            // test if include was processed already (aka include duplication, see #151)
            if (includes.map { it.path }.contains(includeUri.path)) {
                // include was already resolved, so we just continue
                continue
            }

            includes.add(includeUri)


            val resolvedLines = resolve(allowFileReferences && isRegularFile(includeUri), readLines(includeUri), includeUri.resolve("."), includes)
            result.addAll(resolvedLines)
            continue
        }

        result.add(line)
    }

    return result
}

private fun readLines(uri: URI): String {
    try {
        if (isRegularFile(uri)) {
            return uri.toURL().readText()
        }

        val urlHash = md5(uri.toString())
        val urlCache = File(KSCRIPT_DIR, "/url_cache_${urlHash}")

        if (urlCache.exists()) {
            return urlCache.readText()
        }

        val urlContent = uri.toURL().readText()
        urlCache.writeText(urlContent)

        return urlContent
    } catch (e: Exception) {
        errorMsg("Failed to resolve include with URI: '${uri}'")
        System.err.println(e.message?.lines()!!.map { it.prependIndent("[kscript] [ERROR] ") })
        quit(1)
    }
}

private fun resolveUri(scriptPath: URI, include: String): URI {
    val result = when {
        include.startsWith("/") -> File(include).toURI()
        include.startsWith("~/") -> File(System.getenv("HOME")!! + include.substring(1)).toURI()
        else -> scriptPath.resolve(URI(include.removePrefix("./")))
    }

    return result.normalize()
}

internal fun isIncludeDirective(line: String) = line.startsWith("//INCLUDE") || line.startsWith(INCLUDE_ANNOT_PREFIX)

internal fun extractTarget(incDirective: String) = when {
    incDirective.startsWith(INCLUDE_ANNOT_PREFIX) -> incDirective.replaceFirst(INCLUDE_ANNOT_PREFIX, "")
        .split(")")[0].trim(' ', '"')
    else -> incDirective.split("[ ]+".toRegex()).last()
}

private const val INCLUDE_ANNOT_PREFIX = "@file:Include("

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
        //System.err.println(resolveIncludes(File(args[0]).toURI()).scriptFile.readText())
    }
}
