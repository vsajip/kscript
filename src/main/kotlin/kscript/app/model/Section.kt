package kscript.app.model

import java.net.URI

enum class SourceType { FILE, HTTP, STD_INPUT, OTHER_FILE, PARAMETER }
enum class ScriptType(val extension: String) { KT(".kt"), KTS(".kts") }

abstract class Section(open val code: String)

data class Include(override val code: String, val include: String) : Section(code)
data class Package(override val code: String, val packageName: String) : Section(code)
data class Import(override val code: String, val importName: String) : Section(code)
data class Dependency(override val code: String, val dependencies: List<String>) : Section(code)
data class KotlinOpts(override val code: String, val kotlinOpts: List<String>) : Section(code)
data class CompilerOpts(override val code: String, val compileOpts: List<String>) : Section(code)
data class Entry(override val code: String, val entry: String) : Section(code)
data class SheBang(override val code: String) : Section(code)
data class Code(override val code: String) : Section(code)
data class Repository(
    override val code: String, val id: String, val url: String, val user: String = "", val password: String = ""
) : Section(code)

//URI to another, resolved, single script
//This class replaces include in sections
data class ScriptSource(
    override val code: String,
    val sourceType: SourceType,
    val scriptType: ScriptType,
    val sourceUri: URI?,
    val sourceContextUri: URI
) : Section(code)
