package kscript.app.appdir

import kscript.app.model.ScriptType
import kscript.app.util.ScriptUtils
import org.apache.commons.codec.digest.DigestUtils.md5Hex
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeText

data class UriItem(
    val content: String,
    val scriptType: ScriptType,
    val fileName: String,
    val uri: URI, //Real one from Web, not the cached file
    val contextUri: URI, //Real one from Web, not the cached file
    val path: Path //Path to local file
)

class UriCache(private val path: Path) {
    fun readUri(uri: URI): UriItem {
        val hash = md5Hex(uri.toString())

        val descriptorFile = File(path.resolve("$hash.descriptor").toUri())

        if (descriptorFile.exists()) {
            //Cache hit
            val descriptor = descriptorFile.readText().split(" ")
            val scriptType = ScriptType.valueOf(descriptor[0])
            val fileName = descriptor[1]
            val cachedUri = URI.create(descriptor[2])
            val contextUri = URI.create(descriptor[3])
            val content = path.resolve("$hash.content").toUri().toURL().readText()

            return UriItem(content, scriptType, fileName, cachedUri, contextUri, path.resolve("$hash.content"))
        }

        if (uri.scheme == "file") {
            val content = uri.toURL().readText()
            val scriptType = ScriptUtils.resolveScriptType(uri) ?: ScriptUtils.resolveScriptType(content)
            val fileName = ScriptUtils.extractFileName(uri)
            val contextUri = uri.resolve(".")
            return UriItem(content, scriptType, fileName, uri, contextUri, Paths.get(uri))
        }

        //Otherwise, resolve web file and cache it...
        val resolvedUri = ScriptUtils.resolveRedirects(uri.toURL()).toURI()
        val content = resolvedUri.toURL().readText()
        val scriptType = ScriptUtils.resolveScriptType(resolvedUri) ?: ScriptUtils.resolveScriptType(content)
        val fileName = ScriptUtils.extractFileName(resolvedUri)
        val contextUri = resolvedUri.resolve(".")

        descriptorFile.writeText("$scriptType $fileName $resolvedUri $contextUri")
        path.resolve("$hash.content").writeText(content)

        return UriItem(content, scriptType, fileName, resolvedUri, contextUri, path.resolve("$hash.content"))
    }

    fun clear() {
        FileUtils.cleanDirectory(path.toFile())
    }
}
