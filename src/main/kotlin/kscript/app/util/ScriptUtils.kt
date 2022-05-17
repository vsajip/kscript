package kscript.app.util

import kscript.app.model.*
import kscript.app.resolver.ResolutionContext
import org.apache.commons.codec.digest.DigestUtils
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

    fun isUrl(uri: URI) = uri.scheme.equals("http") || uri.scheme.equals("https")

    fun isRegularFile(uri: URI) = uri.scheme.startsWith("file")

    fun String.dropExtension(): String {
        val name = extractFileName(this)

        if (name.indexOf(".") > 0) {
            return name.substring(0, name.lastIndexOf("."))
        }

        return name
    }

    fun extractFileName(uri: URI): String {
        return extractFileName(uri.normalize().path)
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

    private fun extractFileName(path: String): String {
        var filename = path

        val idx = path.lastIndexOf("/")
        if (idx >= 0) {
            filename = path.substring(idx + 1, path.length)
        }

        val idxOfDot = filename.lastIndexOf('.')
        if (idxOfDot > 0) {
            filename = filename.substring(0, idxOfDot)
        }

        return filename
    }

    fun resolveCode(packageName: PackageName?, importNames: Set<ImportName>, scriptNode: ScriptNode): String {
        val sortedImports = importNames.sortedBy { it.value }.toList()
        val sb = StringBuilder()

        if (packageName != null) {
            sb.append("package ${packageName.value}\n\n")
        }

        sortedImports.forEach {
            sb.append("import ${it.value}\n")
        }

        resolveSimpleCode(sb, scriptNode)

        return sb.toString()
    }

    private fun resolveSimpleCode(sb: StringBuilder, scriptNode: ScriptNode, lastLineIsEmpty: Boolean = false) {
        var isLastLineEmpty = lastLineIsEmpty

        for (section in scriptNode.sections) {
            val scriptNodes = section.scriptAnnotations.filterIsInstance<ScriptNode>()

            if (scriptNodes.isNotEmpty()) {
                val subNode = scriptNodes.single()
                resolveSimpleCode(sb, subNode, isLastLineEmpty)
                continue
            }

            val droppedAnnotations = section.scriptAnnotations.filter { it !is Code }
            if (droppedAnnotations.isNotEmpty()) {
                continue
            }

            if (section.code.isNotEmpty() || (!isLastLineEmpty && section.code.isEmpty())) {
                sb.append(section.code).append('\n')
            }

            isLastLineEmpty = section.code.isEmpty()
        }
    }

    fun calculateHash(code: String, resolutionContext: ResolutionContext): String {
        val text =
            code + resolutionContext.repositories.joinToString("\n") + resolutionContext.dependencies.joinToString("\n") + resolutionContext.compilerOpts.joinToString(
                "\n"
            ) + resolutionContext.kotlinOpts.joinToString(
                "\n"
            ) + (resolutionContext.entryPoint ?: "")
        return DigestUtils.md5Hex(text)
    }
}
