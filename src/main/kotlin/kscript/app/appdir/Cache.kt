package kscript.app.appdir

import kscript.app.creator.JarArtifact
import kscript.app.model.Content
import kscript.app.model.ScriptType
import kscript.app.util.*
import org.apache.commons.codec.digest.DigestUtils
import java.net.URI

class Cache(private val path: OsPath) {
    fun getOrCreateIdeaProject(digest: String, creator: (OsPath) -> OsPath): OsPath {
        return directoryCache(path.resolve("idea_$digest"), creator)
    }

    fun getOrCreatePackage(digest: String, creator: (OsPath) -> OsPath): OsPath {
        return directoryCache(path.resolve("package_$digest"), creator)
    }

    fun getOrCreateJar(digest: String, creator: (OsPath) -> JarArtifact): JarArtifact {
        val directory = path.resolve("jar_$digest")
        val cachedJarArtifact = directory.resolve("jarArtifact.descriptor")

        return if (cachedJarArtifact.exists()) {
            val jarArtifactLines = cachedJarArtifact.readText().lines()
            JarArtifact(OsPath.createOrThrow(path.nativeType, jarArtifactLines[0]), jarArtifactLines[1])
        } else {
            directory.createDirectories()
            val jarArtifact = creator(directory)
            cachedJarArtifact.writeText("${jarArtifact.path}\n${jarArtifact.execClassName}")
            jarArtifact
        }
    }

    fun getOrCreateUriItem(uri: URI, creator: (URI, OsPath) -> Content): Content {
        val digest = DigestUtils.md5Hex(uri.toString())

        val directory = path.resolve("uri_$digest")
        val descriptorFile = directory.resolve("uri.descriptor")
        val contentFile = directory.resolve("uri.content")

        if (descriptorFile.exists() && contentFile.exists()) {
            //Cache hit
            val descriptor = descriptorFile.readText().lines()
            val scriptType = ScriptType.valueOf(descriptor[0])
            val fileName = descriptor[1]
            val cachedUri = URI.create(descriptor[2])
            val contextUri = URI.create(descriptor[3])
            val content = contentFile.readText()

            return Content(content, scriptType, fileName, cachedUri, contextUri, contentFile)
        }

        //Cache miss
        val content = creator(uri, contentFile)

        directory.createDirectories()
        descriptorFile.writeText("${content.scriptType}\n${content.fileName}\n${content.uri}\n${content.contextUri}")
        contentFile.writeText(content.text)

        return content
    }

    fun getOrCreateDependencies(digest: String, creator: () -> Set<OsPath>): Set<OsPath> {
        val directory = path.resolve("dependencies_$digest")
        val contentFile = directory.resolve("dependencies.content")

        if (directory.exists()) {
            val dependencies =
                contentFile.readText()
                    .lines()
                    .filter { it.isNotEmpty() }
                    .map { OsPath.createOrThrow(path.nativeType, it) }
                    .toSet()

            //Recheck cached paths - if there are missing artifacts skip the cached values
            if (dependencies.all { it.exists() }) {
                return dependencies
            }
        }

        val dependencies = creator()
        directory.createDirectories()
        contentFile.writeText(dependencies.joinToString("\n") { it.toString() })

        return dependencies
    }

    private fun directoryCache(path: OsPath, creator: (OsPath) -> OsPath): OsPath {
        return if (path.exists()) {
            path
        } else {
            path.createDirectories()
            creator(path)
        }
    }
}
