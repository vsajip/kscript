package kscript.app.resolver

import kscript.app.appdir.UriCache
import kscript.app.model.*
import kscript.app.parser.Parser
import kscript.app.util.ScriptUtils
import kscript.app.util.ScriptUtils.dropExtension
import java.io.File
import java.net.URI
import java.nio.file.Path


class SectionResolver(private val parser: Parser, private val uriCache: UriCache, private val homeDir: Path) {
    fun resolve(
        scriptText: String,
        includeContext: URI,
        allowLocalReferences: Boolean,
        currentLevel: Int,
        resolutionContext: ResolutionContext
    ): List<Section> {
        val sections = parser.parse(scriptText)
        val resultingSections = mutableListOf<Section>()

        for (section in sections) {
            val resultingScriptAnnotations = mutableListOf<ScriptAnnotation>()

            for (annotation in section.scriptAnnotations) {
                resultingScriptAnnotations += resolveAnnotation(
                    section.code, annotation, includeContext, allowLocalReferences, currentLevel, resolutionContext
                )
            }

            resultingSections += Section(section.code, resultingScriptAnnotations)
        }

        return resultingSections
    }


    private fun resolveAnnotation(
        code: String,
        scriptAnnotation: ScriptAnnotation,
        includeContext: URI,
        allowLocalReferences: Boolean,
        currentLevel: Int,
        resolutionContext: ResolutionContext
    ): ScriptAnnotation {
        var resolvedScriptAnnotation = scriptAnnotation

        when (scriptAnnotation) {
            is Include -> if (currentLevel < resolutionContext.maxResolutionLevel) {
                val uri = resolveInclude(includeContext, scriptAnnotation.value, homeDir)
                val scriptSource = if (ScriptUtils.isRegularFile(uri)) ScriptSource.FILE else ScriptSource.HTTP

                if (scriptSource == ScriptSource.FILE && !allowLocalReferences) {
                    throw IllegalStateException("References to local files from remote scripts are disallowed.")
                }

                val uriItem = uriCache.readUri(uri)

                val newSections = resolve(
                    uriItem.content,
                    uriItem.contextUri,
                    allowLocalReferences && scriptSource == ScriptSource.FILE,
                    currentLevel + 1,
                    resolutionContext
                )

                val scriptNode = ScriptNode(
                    currentLevel + 1,
                    scriptSource,
                    uriItem.scriptType,
                    uri,
                    uriItem.contextUri,
                    ScriptUtils.extractFileName(uri).dropExtension(),
                    newSections
                )

                resolutionContext.scriptNodes.add(scriptNode)

                //Replace Include annotation with Script annotation
                resolvedScriptAnnotation = scriptNode
            } else {
                resolutionContext.includes.add(scriptAnnotation)
            }

            is Package -> {
                if (resolutionContext.packageName == null || (resolutionContext.packageName != null && resolutionContext.packageLevel > currentLevel)) {
                    resolutionContext.packageName = scriptAnnotation
                    resolutionContext.packageLevel = currentLevel
                }
            }

            is Entry -> {
                if (resolutionContext.entry == null || (resolutionContext.entry != null && resolutionContext.entryLevel > currentLevel)) {
                    resolutionContext.entry = scriptAnnotation
                    resolutionContext.entryLevel = currentLevel
                }
            }

            is SheBang -> {}
            is Import -> resolutionContext.imports.add(scriptAnnotation)
            is Dependency -> resolutionContext.dependencies.add(scriptAnnotation)
            is KotlinOpt -> resolutionContext.kotlinOpts.add(scriptAnnotation)
            is CompilerOpt -> resolutionContext.compilerOpts.add(scriptAnnotation)
            is Repository -> resolutionContext.repositories.add(scriptAnnotation)
            is Code -> resolutionContext.code.add(code)
        }

        return resolvedScriptAnnotation
    }

    private fun resolveInclude(includeContext: URI, include: String, homeDir: Path): URI {
        val result = when {
            include.startsWith("/") -> File(include).toURI()
            include.startsWith("~/") -> File(homeDir.toAbsolutePath().toString() + include.substring(1)).toURI()
            else -> includeContext.resolve(URI(include.removePrefix("./")))
        }

        return result.normalize()
    }
}
