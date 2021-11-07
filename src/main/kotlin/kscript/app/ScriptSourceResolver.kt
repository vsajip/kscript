package kscript.app

import kscript.app.appdir.AppDir
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.*


class ScriptSourceResolver(private val appDir: AppDir) {
    private val kotlinExtensions = listOf("kts", "kt")

//    fun resolveFromInclude(include: String): ScriptSource {
//
//    }

    fun resolvePreambles(scriptSource: ScriptSource, customPreamble: String?, enableSupportApi: Boolean): ScriptSource {
        // include preamble for custom interpreters (see https://github.com/holgerbrandl/kscript/issues/67)
        var codeText = scriptSource.codeText

        customPreamble?.let { interpPreamble ->
            codeText = interpPreamble + "\n" + codeText
        }

        // prefix with text-processing preamble if kscript-support api is enabled
        if (enableSupportApi) {
            val textProcPreamble = """
            //DEPS com.github.holgerbrandl:kscript-support-api:1.2.5

            import kscript.text.*
            val lines = resolveArgFile(args)

            """.trimIndent()

            codeText = textProcPreamble + "\n" + codeText
        }

        return scriptSource.copy(codeText = codeText)
    }

    fun resolveFromInput(string: String): ScriptSource {
        //Is it stdin?
        if (string == "-" || string == "/dev/stdin") {
            // we need to keep track of the scripts dir or the working dir in case of stdin script to correctly resolve includes
            val includeContext = File(".").toURI()
            val scriptText = generateSequence { readLine() }.joinToString("\n")
            return ScriptSource(
                SourceType.STD_INPUT,
                resolveScriptType(scriptText),
                includeContext,
                null,
                scriptText
            )
        }

        //Is it a URL?
        if (isUrl(string)) {
            val url = URL(string)
            val resolvedUri = resolveRedirects(url).toURI()
            val includeContext = resolvedUri.resolve(".")
            return ScriptSource(
                SourceType.HTTP,
                resolveScriptType(resolvedUri),
                includeContext,
                resolvedUri,
                appDir.cache.code(url)
            )
        }

        val file = File(string)
        if (file.canRead()) {
            val uri = file.toURI()
            val includeContext = uri.resolve(".")

            if (kotlinExtensions.contains(file.extension)) {
                //Regular file
                return ScriptSource(
                    SourceType.FILE,
                    resolveScriptType(uri),
                    includeContext,
                    uri,
                    uri.toURL().readText()
                )
            } else {
                //If script input is a process substitution file handle we can not use for content reading:
                //FileInputStream(this).bufferedReader().use{ readText() } nor readText()
                val scriptText = FileInputStream(file).bufferedReader().readText()
                return ScriptSource(
                    SourceType.OTHER_FILE,
                    resolveScriptType(scriptText),
                    includeContext,
                    uri,
                    scriptText
                )
            }
        }

        errorIf(kotlinExtensions.contains(file.extension)) {
            "Could not read script from '$string'"
        }

        //As a last resort we assume that input is a Kotlin program...
        val includeContext = File(".").toURI()
        return ScriptSource(
            SourceType.PARAMETER, resolveScriptType(string), includeContext, null, string
        )
    }

    private fun isUrl(string: String): Boolean {
        val normalizedString = string.lowercase(Locale.getDefault()).trim()
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
        val path = uri.path.lowercase(Locale.getDefault())

        when {
            path.endsWith(".kt") -> return ScriptType.KT
            path.endsWith(".kts") -> return ScriptType.KTS
        }

        //Try to guess the type by reading the code...
        val code = appDir.cache.code(uri.toURL())
        return resolveScriptType(code)
    }

    private fun resolveScriptType(code: String): ScriptType {
        return if (code.contains("fun main")) ScriptType.KT else ScriptType.KTS
    }
}
