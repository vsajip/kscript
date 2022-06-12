package kscript.app.resolver

import kscript.app.appdir.Cache
import kscript.app.model.Content
import kscript.app.model.OsConfig
import kscript.app.util.*
import kscript.app.util.UriUtils.isUrl
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.io.path.readText

class InputOutputResolver(private val osConfig: OsConfig, private val cache: Cache) {
    fun resolveContent(osPath: OsPath): Content {
        val uri = resolveUri(osPath)
        val content = osPath.toNativePath().readText()
        val (fileName, scriptType) = ScriptUtils.extractScriptFileDetails(uri)
        val contextUri = uri.resolve(".")
        return Content(
            content,
            scriptType ?: ScriptUtils.resolveScriptType(content),
            fileName,
            uri,
            contextUri,
            osPath.toNativePath()
        )
    }

    fun resolveContent(uri: URI): Content {
        if (!isUrl(uri)) {
            return resolveContent(OsPath.createOrThrow(osConfig.nativeOsType, Paths.get(uri).toString()))
        }

        return cache.getOrCreateUriItem(uri) { sourceUrl, contentFile ->
            val resolvedUri = UriUtils.resolveRedirects(sourceUrl.toURL()).toURI()
            val content = resolvedUri.toURL().readText()
            val (fileName, scriptType) = ScriptUtils.extractScriptFileDetails(resolvedUri)
            val contextUri = resolvedUri.resolve(".")

            Content(
                content,
                scriptType ?: ScriptUtils.resolveScriptType(content),
                fileName,
                resolvedUri,
                contextUri,
                contentFile
            )
        }
    }

    fun resolveContentUsingInputStream(osPath: OsPath): Content {
        val resolvedUri = resolveUri(osPath)
        val contextUri = resolvedUri.resolve(".")
        val content = FileInputStream(osPath.toNativeOsPath().stringPath()).bufferedReader().readText()
        val scriptType = ScriptUtils.resolveScriptType(content)

        return Content(content, scriptType, osPath.pathParts.last(), resolvedUri, contextUri, osPath.toNativePath())
    }

    fun resolveUriRelativeToRoot(path: String): URI {
        return resolveUri(
            OsPath.createOrThrow(
                osConfig.nativeOsType, File(".").toPath().toAbsolutePath().root!!.resolve(path).pathString
            )
        )
    }

    fun resolveUriRelativeToHomeDir(path: String): URI = File(osConfig.userHomeDir.resolve(path).stringPath()).toURI()

    fun resolveCurrentDir(): URI = File(".").toURI()

    fun tryToCreateFilePath(path: String): OsPath? = OsPath.create(osConfig.osType, path)

    fun isReadable(osPath: OsPath): Boolean = File(osPath.toNativeOsPath().stringPath()).canRead()

    private fun resolveUri(osPath: OsPath): URI = File(osPath.toNativeOsPath().stringPath()).toURI()
}
