package kscript.app.util

import kscript.app.model.ScriptType
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object ScriptUtils {
    fun resolveRedirects(url: URL): URL {
        val con: HttpURLConnection = url.openConnection() as HttpURLConnection
        con.instanceFollowRedirects = false
        con.connect()

        if (con.responseCode == HttpURLConnection.HTTP_MOVED_PERM || con.responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            val redirectUrl = URL(con.getHeaderField("Location"))
            return resolveRedirects(redirectUrl)
        }

        return url
    }

    fun isUrl(string: String): Boolean {
        val normalizedString = string.lowercase().trim()
        return normalizedString.startsWith("http://") || normalizedString.startsWith("https://")
    }

    fun isRegularFile(uri: URI) = uri.scheme.startsWith("file")

    fun String.dropExtension(): String {
        val name = extractFileName(this)

        if (name.indexOf(".") > 0) {
            return name.substring(0, name.lastIndexOf("."));
        }

        return name
    }

    fun extractFileName(uri: URI): String {
        return extractFileName(uri.normalize().path)
    }

    private fun extractFileName(path: String): String {
        val idx = path.lastIndexOf("/")
        var filename = path
        if (idx >= 0) {
            filename = path.substring(idx + 1, path.length)
        }
        return filename
    }

    fun prependPreambles(preambles: List<String>, string: String): String {
        return preambles.joinToString("\n") + string
    }

    fun resolveScriptType(uri: URI): ScriptType? {
        val path = uri.path.lowercase()

        when {
            path.endsWith(".kt") -> return ScriptType.KT
            path.endsWith(".kts") -> return ScriptType.KTS
        }

        return null
    }

    fun resolveScriptType(code: String): ScriptType {
        return if (code.contains("fun main")) ScriptType.KT else ScriptType.KTS
    }
}
