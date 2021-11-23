package kscript.app.resolver

import kscript.app.appdir.UriCache
import kscript.app.model.*
import kscript.app.parser.LineParser.extractValues
import kscript.app.util.ScriptUtils
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.net.URL

class ScriptResolver(
    private val sectionResolver: SectionResolver,
    private val uriCache: UriCache,
    private val kotlinOptsEnvVariable: String = ""
) {
    private val kotlinExtensions = listOf("kts", "kt")
    private val scripletName = "scriplet"

    //level parameter - for how many levels should include be resolved
    //level 0       -   do not resolve includes in base file and any other embedded
    //level 1 to n  -   resolve includes up to respective level (1 is a base script)
    //level Int.Max -   full resolution (default)
    fun resolve(
        string: String, preambles: List<String> = emptyList(), maxResolutionLevel: Int = Int.MAX_VALUE
    ): Script {
        //Is it stdin?
        if (string == "-" || string == "/dev/stdin") {
            // we need to keep track of the scripts dir or the working dir in case of stdin script to correctly resolve includes
            val scriptText = ScriptUtils.prependPreambles(preambles, generateSequence { readLine() }.joinToString("\n"))

            return createScript(
                ScriptSource.STD_INPUT,
                ScriptUtils.resolveScriptType(scriptText),
                null,
                File(".").toURI(),
                scripletName,
                scriptText,
                true,
                maxResolutionLevel
            )
        }

        //Is it a URL?
        if (ScriptUtils.isUrl(string)) {
            val uriItem = uriCache.readUri(URL(string).toURI())
            val scriptText = ScriptUtils.prependPreambles(preambles, uriItem.content)

            return createScript(
                ScriptSource.HTTP,
                uriItem.scriptType,
                uriItem.uri,
                uriItem.contextUri,
                uriItem.fileName,
                scriptText,
                false,
                maxResolutionLevel
            )
        }

        val file = File(string)
        if (file.canRead()) {
            if (kotlinExtensions.contains(file.extension)) {
                //Regular file
                val uriItem = uriCache.readUri(file.toURI())
                val scriptText = ScriptUtils.prependPreambles(preambles, uriItem.content)

                return createScript(
                    ScriptSource.FILE,
                    uriItem.scriptType,
                    uriItem.uri,
                    uriItem.contextUri,
                    uriItem.fileName,
                    scriptText,
                    true,
                    maxResolutionLevel
                )
            } else {
                //If script input is a process substitution file handle we can not use for content reading:
                //FileInputStream(this).bufferedReader().use{ readText() } nor readText()
                val uri = file.toURI()
                val includeContext = uri.resolve(".")

                val scriptText =
                    ScriptUtils.prependPreambles(preambles, FileInputStream(file).bufferedReader().readText())

                return createScript(
                    ScriptSource.OTHER_FILE,
                    ScriptUtils.resolveScriptType(scriptText),
                    uri,
                    includeContext,
                    scripletName,
                    scriptText,
                    true,
                    maxResolutionLevel
                )
            }
        }

        if (kotlinExtensions.contains(file.extension)) {
            throw IllegalStateException("Could not read script from '$string'")
        }

        //As a last resort we assume that input is a Kotlin program...
        val scriptText = ScriptUtils.prependPreambles(preambles, string)
        return createScript(
            ScriptSource.PARAMETER,
            ScriptUtils.resolveScriptType(scriptText),
            null,
            File(".").toURI(),
            scripletName,
            scriptText,
            true,
            maxResolutionLevel
        )
    }

    private fun createScript(
        scriptSource: ScriptSource,
        scriptType: ScriptType,
        sourceUri: URI?,
        sourceContextUri: URI,
        scriptName: String,
        scriptText: String,
        allowLocalReferences: Boolean,
        maxResolutionLevel: Int
    ): Script {
        val level = 0
        val resolutionContext = ResolutionContext()
        val sections = sectionResolver.resolve(
            scriptText,
            sourceContextUri,
            allowLocalReferences,
            level,
            maxResolutionLevel,
            resolutionContext
        )

        val scriptNode = ScriptNode(level, scriptSource, scriptType, sourceUri, sourceContextUri, scriptName, sections)
        val code = ScriptUtils.resolveCode(resolutionContext.packageName, resolutionContext.importNames, scriptNode)

        if (kotlinOptsEnvVariable.isNotBlank()) {
            extractValues(kotlinOptsEnvVariable).map { KotlinOpt(it) }.forEach {
                resolutionContext.kotlinOpts.add(it)
            }
        }

        return Script(
            scriptSource,
            scriptType,
            sourceUri,
            sourceContextUri,
            scriptName,
            code,
            resolutionContext.packageName,
            resolutionContext.entry,
            resolutionContext.importNames,
            resolutionContext.includes,
            resolutionContext.dependencies,
            resolutionContext.repositories,
            resolutionContext.kotlinOpts,
            resolutionContext.compilerOpts,
            resolutionContext.scriptNodes,
            scriptNode
        )
    }
}
