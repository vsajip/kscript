package kscript.app.resolver

import kscript.app.appdir.AppDir
import kscript.app.model.*
import kscript.app.model.Annotation
import kscript.app.parser.Parser
import kscript.app.util.isRegularFile
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Path

class ScriptResolver(private val parser: Parser, private val appDir: AppDir, private val config: Config) {
    private val kotlinExtensions = listOf("kts", "kt")
    private val scripletName = "Scriplet"

    //from input
    //level Int.Max -   full resolution
    //level 0       -   do not resolve includes in base file and any other
    //level 1 to n  -   resolve includes up to respective level (1 is a base script)
    fun resolveFromInput(
        string: String, preambles: List<String> = emptyList(), maxResolutionLevel: Int = Int.MAX_VALUE
    ): Pair<Script, ResolvedScript> {
        val resolutionContext = ResolutionContext(maxResolutionLevel)

        //Is it stdin?
        if (string == "-" || string == "/dev/stdin") {
            // we need to keep track of the scripts dir or the working dir in case of stdin script to correctly resolve includes
            val includeContext = File(".").toURI()
            val scriptText = prependPreambles(preambles, generateSequence { readLine() }.joinToString("\n"))
            val sections = resolveSections(scriptText, includeContext, true, 0, resolutionContext)
            val script = Script(
                SourceType.STD_INPUT, resolveScriptType(scriptText), null, includeContext, scripletName, sections
            )
            val resolvedScript = createResolvedScript(resolutionContext)
            return Pair(script, resolvedScript)
        }

        //Is it a URL?
        if (isUrl(string)) {
            val url = URL(string)
            val resolvedUri = resolveRedirects(url).toURI()
            val includeContext = resolvedUri.resolve(".")
            val scriptText = prependPreambles(preambles, readUri(resolvedUri))
            val sections = resolveSections(scriptText, includeContext, false, 0, resolutionContext)
            val script = Script(
                SourceType.HTTP,
                resolveScriptType(resolvedUri),
                resolvedUri,
                includeContext,
                getFileNameWithoutExtension(resolvedUri),
                sections
            )
            val resolvedScript = createResolvedScript(resolutionContext)
            return Pair(script, resolvedScript)
        }

        val file = File(string)
        if (file.canRead()) {
            val uri = file.toURI()
            val includeContext = uri.resolve(".")

            if (kotlinExtensions.contains(file.extension)) {
                //Regular file
                val scriptText = prependPreambles(preambles, readUri(uri))
                val sections = resolveSections(scriptText, includeContext, true, 0, resolutionContext)
                val script = Script(
                    SourceType.FILE, resolveScriptType(uri), uri, includeContext, file.nameWithoutExtension, sections
                )
                val resolvedScript = createResolvedScript(resolutionContext)
                return Pair(script, resolvedScript)

            } else {
                //If script input is a process substitution file handle we can not use for content reading:
                //FileInputStream(this).bufferedReader().use{ readText() } nor readText()
                val scriptText = prependPreambles(preambles, FileInputStream(file).bufferedReader().readText())
                val sections = resolveSections(scriptText, includeContext, true, 0, resolutionContext)
                val script = Script(
                    SourceType.OTHER_FILE, resolveScriptType(scriptText), uri, includeContext, scripletName, sections
                )
                val resolvedScript = createResolvedScript(resolutionContext)
                return Pair(script, resolvedScript)
            }
        }

        if (kotlinExtensions.contains(file.extension)) {
            throw IllegalStateException("Could not read script from '$string'")
        }

        //As a last resort we assume that input is a Kotlin program...
        val includeContext = File(".").toURI()
        val scriptText = prependPreambles(preambles, string)
        val sections = resolveSections(scriptText, includeContext, true, 0, resolutionContext)
        val script = Script(
            SourceType.PARAMETER, resolveScriptType(scriptText), null, includeContext, scripletName, sections
        )
        val resolvedScript = createResolvedScript(resolutionContext)
        return Pair(script, resolvedScript)
    }

    private fun createResolvedScript(resolutionContext: ResolutionContext): ResolvedScript {
        val code = StringBuilder().apply {
            if (resolutionContext.packageName != null) {
                append("package ${resolutionContext.packageName!!.value}\n\n")
            }

            resolutionContext.imports.forEach {
                append("import ${it.value}\n")
            }

            resolutionContext.code.forEach {
                append("$it\n")
            }
        }.toString()


        return ResolvedScript(
            code,
            resolutionContext.packageName,
            resolutionContext.entry,
            resolutionContext.scripts,
            resolutionContext.includes,
            resolutionContext.dependencies,
            resolutionContext.repositories,
            resolutionContext.kotlinOpts,
            resolutionContext.compilerOpts
        )
    }

    private fun resolveSections(
        scriptText: String,
        includeContext: URI,
        allowLocalReferences: Boolean,
        currentLevel: Int,
        resolutionContext: ResolutionContext
    ): List<Section> {
        val sections = parser.parse(scriptText)

        val resultingSections = mutableListOf<Section>()

        for (section in sections) {
            val resultingAnnotations = mutableListOf<Annotation>()

            for (annotation in section.annotations) {

                when (annotation) {
                    is Include -> if (currentLevel < resolutionContext.maxResolutionLevel) {
                        val uri = resolveInclude(includeContext, annotation.value, config.homeDir)
                        val sourceType = if (isRegularFile(uri)) SourceType.FILE else SourceType.HTTP

                        if (sourceType == SourceType.FILE && !allowLocalReferences) {
                            throw IllegalStateException("References to local files from remote scripts are disallowed.")
                        }

                        val scriptType = resolveScriptType(uri)
                        val newScriptText = readUri(uri)
                        val newIncludeContext = uri.resolve(".")
                        val newSections = resolveSections(
                            newScriptText,
                            newIncludeContext,
                            allowLocalReferences && sourceType == SourceType.FILE,
                            currentLevel + 1,
                            resolutionContext
                        )
                        val script = Script(
                            sourceType,
                            scriptType,
                            uri,
                            newIncludeContext,
                            getFileNameWithoutExtension(uri),
                            newSections
                        )

                        resolutionContext.scripts.add(script)

                        //Replace Include annotation with Script annotation
                        resultingAnnotations += script
                        continue
                    } else {
                        resolutionContext.includes.add(annotation)
                    }

                    is Package -> {
                        if (resolutionContext.packageName == null || (resolutionContext.packageName != null && resolutionContext.packageLevel > currentLevel)) {
                            resolutionContext.packageName = annotation
                            resolutionContext.packageLevel = currentLevel
                        }
                    }

                    is Entry -> {
                        if (resolutionContext.entry == null || (resolutionContext.entry != null && resolutionContext.entryLevel > currentLevel)) {
                            resolutionContext.entry = annotation
                            resolutionContext.entryLevel = currentLevel
                        }
                    }

                    is SheBang -> {}
                    is Import -> resolutionContext.imports.add(annotation)
                    is Dependency -> resolutionContext.dependencies.add(annotation)
                    is KotlinOpt -> resolutionContext.kotlinOpts.add(annotation)
                    is CompilerOpt -> resolutionContext.compilerOpts.add(annotation)
                    is Repository -> resolutionContext.repositories.add(annotation)
                    is Code -> resolutionContext.code.add(section.code)
                }

                resultingAnnotations += annotation
            }

            resultingSections += Section(section.code, resultingAnnotations)
        }

        return resultingSections
    }

    private fun resolveInclude(includeContext: URI, include: String, homeDir: Path): URI {
        val result = when {
            include.startsWith("/") -> File(include).toURI()
            include.startsWith("~/") -> File(homeDir.toAbsolutePath().toString() + include.substring(1)).toURI()
            else -> includeContext.resolve(URI(include.removePrefix("./")))
        }

        return result.normalize()
    }

    private fun getFileNameWithoutExtension(path: URI): String {
        val filename = getFileName(path)!!
        val idx = filename.lastIndexOf('.')
        return filename.substring(0, idx)
    }

    private fun getFileName(path: URI): String? {
        return getFileName(path.normalize().path)
    }

    private fun getFileName(path: String): String? {
        val idx = path.lastIndexOf("/")
        var filename = path
        if (idx >= 0) {
            filename = path.substring(idx + 1, path.length)
        }
        return filename
    }

    private fun readUri(resolvedUri: URI): String {
        return resolvedUri.toURL().readText()
    }

    private fun prependPreambles(preambles: List<String>, string: String): String {
        return preambles.joinToString("\n") + string
    }

    private fun isUrl(string: String): Boolean {
        val normalizedString = string.lowercase().trim()
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
        val path = uri.path.lowercase()

        when {
            path.endsWith(".kt") -> return ScriptType.KT
            path.endsWith(".kts") -> return ScriptType.KTS
        }

        //Try to guess the type by reading the code...
        val code = readUri(uri)
        return resolveScriptType(code)
    }

    private fun resolveScriptType(code: String): ScriptType {
        return if (code.contains("fun main")) ScriptType.KT else ScriptType.KTS
    }
}
