package kscript.app.resolver

import kscript.app.appdir.AppDir
import kscript.app.model.*
import kscript.app.parser.ParseError
import kscript.app.parser.Parser
import kscript.app.util.quit
import kscript.app.util.Logger
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
            sections += if (section is Include) {
                resolveInclude(section, includeContext)
            } else {
                section
            }
        }

        return sections
    }

    private fun resolveInclude(include: Include, includeContext: URI): Section {
        val uri = URI.create(include.include)
        return ScriptSource(
            include.code,
            if (isUrl(include.include)) SourceType.HTTP else SourceType.FILE,
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
        val dependencies = mutableSetOf<String>()
        val repositories = mutableSetOf<Repository>()
        val kotlinOpts = mutableSetOf<String>()
        val compilerOpts = mutableSetOf<String>()
        val imports = mutableSetOf<String>()
        val scriptSources = mutableSetOf<ScriptSource>()

        var packageName: String? = null
        var entry: String? = null

        for (section in script.sections) {

            when(section) {
                is ScriptSource -> {

                }

                is Import -> {
                   imports.add(section.importName)
                }

                is Package -> {
                    //TODO: Only top level packages should be used here
                    if (packageName == null) {
                        packageName = section.packageName
                    }
                }

                is SheBang -> {}

                is Dependency -> {
                    dependencies.addAll(section.dependencies)
                }

                is KotlinOpts -> {
                    kotlinOpts.addAll(section.kotlinOpts)
                }

                is CompilerOpts -> {
                    compilerOpts.addAll(section.compileOpts)
                }

                is Entry -> {
                    if (entry == null ) {
                        entry = section.entry
                    } else {
                        throw ParseError(section.code, "Duplicated Entry point.")
                    }
                }

                is Repository -> {
                    repositories.add(section)
                }

                is Code -> {
                    unifiedCode.add(section.code)
                }
            }
        }

        val code = StringBuilder().apply {
            if (packageName != null) {
                append("package $packageName\n\n")
            }

            imports.forEach {
                append("import $it\n")
            }

            unifiedCode.forEach {
                append("$it\n")
            }
        }.toString()


        return ResolvedScript(code, packageName, entry, scriptSources, dependencies, repositories, kotlinOpts, compilerOpts)
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
