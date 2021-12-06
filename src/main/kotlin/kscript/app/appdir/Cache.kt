package kscript.app.appdir

import kscript.app.creator.JarArtifact
import kscript.app.model.ScriptType
import kscript.app.util.Logger.devMsg
import kscript.app.util.ScriptUtils
import org.apache.commons.codec.digest.DigestUtils
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class UriItem(
    val content: String,
    val scriptType: ScriptType,
    val fileName: String,
    val uri: URI, //Real one from Web, not the cached file
    val contextUri: URI, //Real one from Web, not the cached file
    val path: Path //Path to local file
)

class Cache(private val path: Path) {
    fun getOrCreateIdeaProject(digest: String, creator: (Path) -> Path): Path {
        return directoryCache(path.resolve("idea_$digest"), creator)
    }

    fun getOrCreatePackage(digest: String, creator: (Path) -> Path): Path {
        return directoryCache(path.resolve("package_$digest"), creator)
    }

    fun getOrCreateJar(digest: String, creator: (Path) -> JarArtifact): JarArtifact {
        val directory = path.resolve("jar_$digest")
        val cachedJarArtifact = directory.resolve("jarArtifact.descriptor")

        return if (directory.exists()) {
            val jarArtifactLines = cachedJarArtifact.readText().lines()
            JarArtifact(Paths.get(jarArtifactLines[0]), jarArtifactLines[1])
        } else {
            directory.createDirectories()
            val jarArtifact = creator(directory)
            cachedJarArtifact.writeText("${jarArtifact.path}\n${jarArtifact.execClassName}")
            jarArtifact
        }
    }

    fun getOrCreateUriItem(uri: URI): UriItem {
        val digest = DigestUtils.md5Hex(uri.toString())

        if (uri.scheme == "file") {
            val content = uri.toURL().readText()
            val scriptType = ScriptUtils.resolveScriptType(uri) ?: ScriptUtils.resolveScriptType(content)
            val fileName = ScriptUtils.extractFileName(uri)
            val contextUri = uri.resolve(".")
            return UriItem(content, scriptType, fileName, uri, contextUri, Paths.get(uri))
        }

        val directory = path.resolve("uri_$digest").createDirectories()
        val descriptorFile = directory.resolve("uri.descriptor")
        val contentFile = directory.resolve("uri.content")

        if (descriptorFile.exists() && contentFile.exists()) {
            //Cache hit
            val descriptor = descriptorFile.readText().lines()

            devMsg("Descriptor: $descriptor")

            val scriptType = ScriptType.valueOf(descriptor[0])
            val fileName = descriptor[1]
            val cachedUri = URI.create(descriptor[2])
            val contextUri = URI.create(descriptor[3])
            val content = contentFile.readText()

            return UriItem(content, scriptType, fileName, cachedUri, contextUri, contentFile)
        }

        //Otherwise, resolve web file and cache it...
        val resolvedUri = ScriptUtils.resolveRedirects(uri.toURL()).toURI()
        val content = resolvedUri.toURL().readText()
        val scriptType = ScriptUtils.resolveScriptType(resolvedUri) ?: ScriptUtils.resolveScriptType(content)
        val fileName = ScriptUtils.extractFileName(resolvedUri)
        val contextUri = resolvedUri.resolve(".")

        descriptorFile.writeText("$scriptType\n$fileName\n$resolvedUri\n$contextUri")
        contentFile.writeText(content)

        return UriItem(content, scriptType, fileName, resolvedUri, contextUri, contentFile)
    }

    private fun directoryCache(path: Path, creator: (Path) -> Path): Path {
        return if (path.exists()) {
            path
        } else {
            path.createDirectories()
            creator(path)
        }
    }
}
