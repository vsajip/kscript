package kscript.app

import java.io.File
import java.net.URI

class ScriptNewResolver(private val scriptSource: ScriptSource, private val preambles: String) {
    private val includes = mutableListOf<URI>()
    private val lines = mutableListOf<String>()
    private val dependencies = mutableListOf<String>()
    private val customRepositories = mutableListOf<String>()
    private val kotlinOpts = mutableListOf<String>()
    private val compilerOpts = mutableListOf<String>()

    private val imports = mutableSetOf<String>()

    fun resolve(): ScriptNew {
        val codeText = preambles + "\n" + scriptSource.codeText

        resolveInternal(scriptSource.sourceType != SourceType.HTTP, codeText, scriptSource.includeContext)

        return ScriptNew(
            scriptSource.scriptType, lines, includes, dependencies, customRepositories, kotlinOpts, compilerOpts
        )
    }

    /** Resolve include declarations in a script file. Resolved script will be put into another temporary script */
//    fun resolveIncludes(scriptSource: ScriptSource): IncludeResult {
//        val includes = mutableListOf<URI>()
//        val lines = resolveInternal(
//            scriptSource.sourceType != SourceType.HTTP,
//            scriptSource.codeText,
//            scriptSource.includeContext,
//            includes
//        )
//        val script = Script(lines, if (scriptSource.scriptType == ScriptType.KTS) "kts" else "kt")
//
//        return IncludeResult(script.consolidateStructure(), includes.map { it.toURL() })
//    }

    private fun resolveInternal(allowFileReferences: Boolean, codeText: String, includeContext: URI) {
        val codeTextAsLines = codeText.lines()

        for (line in codeTextAsLines) {

            val skip = when {
                isIncludeDirective(line) -> processInclude(line, allowFileReferences, includeContext)
                else -> false
            }

            if (skip) {
                continue
            }

            lines.add(line)
        }
    }

    private fun processInclude(line: String, allowFileReferences: Boolean, includeContext: URI): Boolean {
        val include = extractTarget(line)
        val includeUri = resolveUri(includeContext, include)

        if (!allowFileReferences && isRegularFile(includeUri)) {
            Logger.errorMsg("References to local filesystem from remote scripts are not allowed.\nIn script: ; Reference: $includeUri")
            quit(1)
        }

        // test if include was processed already (aka include duplication, see #151)
        if (includes.map { it.path }.contains(includeUri.path)) {
            // include was already resolved, so we just continue
            return true
        }

        includes.add(includeUri)


        resolveInternal(
            allowFileReferences && isRegularFile(includeUri),
            readLines(includeUri),
            includeUri.resolve("."),
        )
        return true
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
            Logger.errorMsg("Failed to resolve include with URI: '${uri}'")
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

//    internal fun isIncludeDirective(line: String) =
//        line.startsWith("//INCLUDE") || line.startsWith(INCLUDE_ANNOT_PREFIX)
//
//    internal fun extractTarget(incDirective: String) = when {
//        incDirective.startsWith(INCLUDE_ANNOT_PREFIX) -> incDirective.replaceFirst(INCLUDE_ANNOT_PREFIX, "")
//            .split(")")[0].trim(' ', '"')
//        else -> incDirective.split("[ ]+".toRegex()).last()
//    }

    private val INCLUDE_ANNOT_PREFIX = "@file:Include("
}
