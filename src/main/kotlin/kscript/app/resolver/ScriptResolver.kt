package kscript.app.resolver

import kscript.app.appdir.AppDir
import kscript.app.model.*
import kscript.app.model.Annotation
import kscript.app.parser.ParseError
import kscript.app.parser.Parser
import kscript.app.util.Logger
import kscript.app.util.quit
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class ScriptResolver(private val parser: Parser, private val appDir: AppDir) {
    private val kotlinExtensions = listOf("kts", "kt")
    private val scripletName = "Scriplet"

    //from input
    fun createFromInput(string: String, preambles: List<String> = emptyList()): Script {
        //Is it stdin?
        if (string == "-" || string == "/dev/stdin") {
            // we need to keep track of the scripts dir or the working dir in case of stdin script to correctly resolve includes
            val includeContext = File(".").toURI()
            val scriptText = prependPreambles(preambles, generateSequence { readLine() }.joinToString("\n"))
            return Script(
                SourceType.STD_INPUT,
                resolveScriptType(scriptText),
                null,
                includeContext,
                scripletName,
                resolveSections(scriptText, includeContext)
            )
        }

        //Is it a URL?
        if (isUrl(string)) {
            val url = URL(string)
            url.file
            val resolvedUri = resolveRedirects(url).toURI()
            val includeContext = resolvedUri.resolve(".")
            val scriptText = prependPreambles(preambles, readUri(resolvedUri))
            return Script(
                SourceType.HTTP,
                resolveScriptType(resolvedUri),
                resolvedUri,
                includeContext,
                getFileNameWithoutExtension(resolvedUri),
                resolveSections(scriptText, includeContext)
            )
        }

        val file = File(string)
        if (file.canRead()) {
            val uri = file.toURI()
            val includeContext = uri.resolve(".")

            if (kotlinExtensions.contains(file.extension)) {
                //Regular file
                val scriptText = prependPreambles(preambles, readUri(uri))
                return Script(
                    SourceType.FILE,
                    resolveScriptType(uri),
                    uri,
                    includeContext,
                    file.nameWithoutExtension,
                    resolveSections(scriptText, includeContext)
                )
            } else {
                //If script input is a process substitution file handle we can not use for content reading:
                //FileInputStream(this).bufferedReader().use{ readText() } nor readText()
                val scriptText = prependPreambles(preambles, FileInputStream(file).bufferedReader().readText())
                return Script(
                    SourceType.OTHER_FILE,
                    resolveScriptType(scriptText),
                    uri,
                    includeContext,
                    scripletName,
                    resolveSections(scriptText, includeContext)
                )
            }
        }

        if (kotlinExtensions.contains(file.extension)) {
            Logger.errorMsg("Could not read script from '$string'")
            quit(1)
        }

        //As a last resort we assume that input is a Kotlin program...
        val includeContext = File(".").toURI()
        val scriptText = prependPreambles(preambles, string)
        return Script(
            SourceType.PARAMETER,
            resolveScriptType(scriptText),
            null,
            includeContext,
            scripletName,
            resolveSections(scriptText, includeContext)
        )
    }

    fun getFileNameWithoutExtension(path: URI): String {
        val filename = getFileName(path)!!
        val idx = filename.lastIndexOf('.')
        return filename.substring(0, idx)
    }

    fun getFileName(path: URI): String? {
        return getFileName(path.normalize().path)
    }

    fun getFileName(path: String): String? {
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

    private fun resolveSections(scriptText: String, includeContext: URI): List<Section> {
        val sections = mutableListOf<Section>()

        for (section in parser.parse(scriptText)) {
            val resolvedAnnotations = mutableListOf<Annotation>()

            for (annotation in section.annotation) {
                resolvedAnnotations += if (annotation is Include) {
                    resolveInclude(annotation, includeContext)
                } else {
                    annotation
                }
            }

            sections += Section(section.code, resolvedAnnotations)
        }

        return sections
    }

    private fun resolveInclude(include: Include, includeContext: URI): Annotation {
        val uri = URI.create(include.value)
        return ScriptSource(
            if (isUrl(include.value)) SourceType.HTTP else SourceType.FILE,
            resolveScriptType(uri),
            uri,
            includeContext
        )
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

    fun resolveUri(scriptPath: URI, include: String): URI {
        val result = when {
            include.startsWith("/") -> File(include).toURI()
            include.startsWith("~/") -> File(System.getenv("HOME")!! + include.substring(1)).toURI()
            else -> scriptPath.resolve(URI(include.removePrefix("./")))
        }

        return result.normalize()
    }


    fun resolve(script: Script): ResolvedScript {
        val unifiedCode = mutableListOf<String>()
        val dependencies = mutableSetOf<Dependency>()
        val repositories = mutableSetOf<Repository>()
        val kotlinOpts = mutableSetOf<KotlinOpt>()
        val compilerOpts = mutableSetOf<CompilerOpt>()
        val imports = mutableSetOf<Import>()
        val scriptSources = mutableSetOf<ScriptSource>()

        var packageName: Package? = null
        var entry: Entry? = null

        for (section in script.sections) {
            for (annotation in section.annotation) {
                when (annotation) {
                    is ScriptSource -> {

                    }

                    is Import -> {
                        imports.add(annotation)
                    }

                    is Package -> {
                        //TODO: Only top level packages should be used here
                        if (packageName == null) {
                            packageName = annotation
                        }
                    }

                    is SheBang -> {
                    }

                    is Dependency -> {
                        dependencies.add(annotation)
                    }

                    is KotlinOpt -> {
                        kotlinOpts.add(annotation)
                    }

                    is CompilerOpt -> {
                        compilerOpts.add(annotation)
                    }

                    is Entry -> {
                        if (entry == null) {
                            entry = annotation
                        } else {
                            throw ParseError(section.code, "Duplicated Entry point.")
                        }
                    }

                    is Repository -> {
                        repositories.add(annotation)
                    }

                    is Code -> {
                        unifiedCode.add(section.code)
                    }
                }
            }
        }

        val code = StringBuilder().apply {
            if (packageName != null) {
                append("package ${packageName.value}\n\n")
            }

            imports.forEach {
                append("import ${it.value}\n")
            }

            unifiedCode.forEach {
                append("$it\n")
            }
        }.toString()

        return ResolvedScript(
            code, packageName, entry, scriptSources, dependencies, repositories, kotlinOpts, compilerOpts
        )
    }

//    fun create(uri: URI): List<Section> {
//        try {
//            if (isRegularFile(uri)) {
//                return Parser.parse(uri.toURL().readText())
//            }
//
//            return Parser.parse(appDir.urlCache.code(uri.toURL()))
//        } catch (e: Exception) {
//            Logger.errorMsg("Failed to resolve include with URI: '${uri}'", e)
//            quit(1)
//        }
//    }
}
