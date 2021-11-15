package kscript.app.resolver

import kscript.app.appdir.AppDir
import kscript.app.isRegularFile
import kscript.app.model.*
import kscript.app.quit
import kscript.app.util.Logger
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class ScriptResolver(private val parser: Parser, private val appDir: AppDir) {
    private val kotlinExtensions = listOf("kts", "kt")

    //from input
    fun createFromInput(string: String, preambles: List<String> = emptyList()): Script {
        //Is it stdin?
        if (string == "-" || string == "/dev/stdin") {
            // we need to keep track of the scripts dir or the working dir in case of stdin script to correctly resolve includes
            val includeContext = File(".").toURI()
            val scriptText = prependPreambles(preambles, generateSequence { readLine() }.joinToString("\n"))
            return Script(
                SourceType.STD_INPUT,
                resolveScriptType(scriptText),
                null,
                includeContext,
                resolveSections(scriptText, includeContext)
            )
        }

        //Is it a URL?
        if (isUrl(string)) {
            val url = URL(string)
            val resolvedUri = resolveRedirects(url).toURI()
            val includeContext = resolvedUri.resolve(".")
            val scriptText = prependPreambles(preambles, resolveUri(resolvedUri))
            return Script(
                SourceType.HTTP,
                resolveScriptType(resolvedUri),
                resolvedUri,
                includeContext,
                resolveSections(scriptText, includeContext)
            )
        }

        val file = File(string)
        if (file.canRead()) {
            val uri = file.toURI()
            val includeContext = uri.resolve(".")

            if (kotlinExtensions.contains(file.extension)) {
                //Regular file
                val scriptText = prependPreambles(preambles, resolveUri(uri))
                return Script(
                    SourceType.FILE,
                    resolveScriptType(uri),
                    uri,
                    includeContext,
                    resolveSections(scriptText, includeContext)
                )
            } else {
                //If script input is a process substitution file handle we can not use for content reading:
                //FileInputStream(this).bufferedReader().use{ readText() } nor readText()
                val scriptText = prependPreambles(preambles, FileInputStream(file).bufferedReader().readText())
                return Script(
                    SourceType.OTHER_FILE,
                    resolveScriptType(scriptText),
                    uri,
                    includeContext,
                    resolveSections(scriptText, includeContext)
                )
            }
        }

        if (kotlinExtensions.contains(file.extension)) {
            Logger.errorMsg("Could not read script from '$string'")
            quit(1)
        }

        //As a last resort we assume that input is a Kotlin program...
        val includeContext = File(".").toURI()
        val scriptText = prependPreambles(preambles, string)
        return Script(
            SourceType.PARAMETER,
            resolveScriptType(scriptText),
            null,
            includeContext,
            resolveSections(scriptText, includeContext)
        )
    }

    private fun prependPreambles(preambles: List<String>, string: String): String {
        return preambles.joinToString("\n") + string
    }

    private fun resolveSections(scriptText: String, includeContext: URI): List<Section> {
        val sections = mutableListOf<Section>()

        for (section in parser.parse(scriptText)) {
            sections += if (section is Include) {
                resolveInclude(section, includeContext)
            } else {
                section
            }
        }

        return sections
    }

    private fun resolveInclude(include: Include, includeContext: URI): Section {
        return ScriptSource(include.code, ....)
    }

    private fun isUrl(string: String): Boolean {
        val normalizedString = string.toLowerCase().trim()
        return normalizedString.startsWith("http://") || normalizedString.startsWith("https://")
    }

    private fun resolveRedirects(url: URL): URL {
        val con: HttpURLConnection = url.openConnection() as HttpURLConnection
        con.instanceFollowRedirects = false
        con.connect()

        if (con.responseCode == HttpURLConnection.HTTP_MOVED_PERM || con.responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            val redirectUrl = URL(con.getHeaderField("Location"))
            return resolveRedirects(redirectUrl)
        }

        return url
    }

    private fun resolveScriptType(uri: URI): ScriptType {
        val path = uri.path.toLowerCase()

        when {
            path.endsWith(".kt") -> return ScriptType.KT
            path.endsWith(".kts") -> return ScriptType.KTS
        }

        //Try to guess the type by reading the code...
        val code = resolveUri(uri)
        return resolveScriptType(code)
    }

    private fun resolveScriptType(code: String): ScriptType {
        return if (code.contains("fun main")) ScriptType.KT else ScriptType.KTS
    }

    fun resolveUri(scriptPath: URI, include: String): URI {
        val result = when {
            include.startsWith("/") -> File(include).toURI()
            include.startsWith("~/") -> File(System.getenv("HOME")!! + include.substring(1)).toURI()
            else -> scriptPath.resolve(URI(include.removePrefix("./")))
        }

        return result.normalize()
    }


    fun unifyScripts(script: Script): UnifiedScript {
        //Resolve all ScriptSources with Script
        val resolvedSources = resolveScriptSources(resolvedIncludes)

        return script.copy(sections = resolvedSources)
    }

    private fun resolveScriptSources(sections: List<Section>): List<Section> {
        val result = mutableListOf<Section>()

        for (section in sections) {
            if (section is ScriptSource) {
                result += resolver.resolve(section)
            } else {
                result += section
            }
        }

        return result
    }

    fun create(uri: URI): List<Section> {
        try {
            if (isRegularFile(uri)) {
                return Parser.parse(uri.toURL().readText())
            }

            return Parser.parse(appDir.urlCache.code(uri.toURL()))
        } catch (e: Exception) {
            Logger.errorMsg("Failed to resolve include with URI: '${uri}'", e)
            quit(1)
        }
    }

    private fun resolveUri(resolvedUri: URI): String {
        TODO("Not yet implemented")
    }

    fun resolveSources(script: Script): ResolvedScript {
        TODO("Not yet implemented")
    }
}
