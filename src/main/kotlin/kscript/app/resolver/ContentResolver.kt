package kscript.app.resolver

import kscript.app.appdir.Cache
import kscript.app.model.Content
import kscript.app.util.OsHandler
import kscript.app.util.ScriptUtils
import kscript.app.util.ScriptUtils.isUrl
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class ContentResolver(private val osHandler: OsHandler, private val cache: Cache) {

    fun resolve(filePath: Path): Content {
        val uri = filePath.toUri()
        val content = filePath.readText()
        val scriptType = ScriptUtils.resolveScriptType(uri) ?: ScriptUtils.resolveScriptType(content)
        val fileName = ScriptUtils.extractFileName(uri)
        val contextUri = uri.resolve(".")
        return Content(content, scriptType, fileName, uri, contextUri, filePath)
    }

    fun resolve(url: URL): Content {
        return cache.getOrCreateUriItem(url) { sourceUrl, contentFile ->
            val resolvedUri = ScriptUtils.resolveRedirects(sourceUrl).toURI()
            val content = resolvedUri.toURL().readText()
            val scriptType = ScriptUtils.resolveScriptType(resolvedUri) ?: ScriptUtils.resolveScriptType(content)
            val fileName = ScriptUtils.extractFileName(resolvedUri)
            val contextUri = resolvedUri.resolve(".")

            Content(content, scriptType, fileName, resolvedUri, contextUri, contentFile)
        }
    }

    fun resolve(uri: URI): Content {
        return if (isUrl(uri)) {
            resolve(uri.toURL())
        } else {
            resolve(Paths.get(uri))
        }
    }
}
